package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LearningContentAudioState {
    data object Idle : LearningContentAudioState
    data class Starting(val path: Path) : LearningContentAudioState
    data class Playing(val path: Path) : LearningContentAudioState
    data class Failed(val path: Path, val message: String) : LearningContentAudioState
}

fun interface LearningContentAudioStateListener {
    fun onStateChanged(state: LearningContentAudioState)
}

interface LearningContentAudioPlayer : AutoCloseable {
    val state: LearningContentAudioState
    fun play(path: Path)
    fun stop()
    fun listen(listener: LearningContentAudioStateListener): AutoCloseable
}

internal fun interface DesktopAudioOutputFactory {
    fun open(format: AudioFormat): DesktopAudioOutput
}

internal interface DesktopAudioOutput : AutoCloseable {
    fun start()
    fun write(bytes: ByteArray, offset: Int, length: Int)
    fun drain()
    fun stop()
}

internal object SystemDesktopAudioOutputFactory : DesktopAudioOutputFactory {
    override fun open(format: AudioFormat): DesktopAudioOutput {
        val info = DataLine.Info(SourceDataLine::class.java, format)
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.open(format)
        return SourceDataLineOutput(line)
    }
}

private class SourceDataLineOutput(
    private val line: SourceDataLine
) : DesktopAudioOutput {
    override fun start() = line.start()
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        line.write(bytes, offset, length)
    }
    override fun drain() = line.drain()
    override fun stop() {
        line.stop()
        line.flush()
    }
    override fun close() = line.close()
}

/** Opens WAV/AIFF/AU and MP3 SPI streams as PCM suitable for the system output line. */
internal object DesktopAudioDecoder {
    fun open(path: Path): AudioInputStream {
        require(Files.isRegularFile(path)) { "Audio file is unavailable." }
        val encoded = AudioSystem.getAudioInputStream(path.toFile())
        val source = encoded.format
        if (source.encoding == AudioFormat.Encoding.PCM_SIGNED) return encoded

        val decoded = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            source.sampleRate,
            16,
            source.channels,
            source.channels * 2,
            source.sampleRate,
            false
        )
        return try {
            AudioSystem.getAudioInputStream(decoded, encoded)
        } catch (failure: Exception) {
            encoded.close()
            throw failure
        }
    }
}

/**
 * Real Desktop adapter. Decoding and system-line writes run off the Compose event thread.
 * Generation checks make completion/failure callbacks from a superseded clip inert.
 */
class JavaSoundLearningContentAudioPlayer internal constructor(
    private val outputFactory: DesktopAudioOutputFactory = SystemDesktopAudioOutputFactory
) : LearningContentAudioPlayer {
    private val generation = AtomicLong()
    private val listeners = CopyOnWriteArraySet<LearningContentAudioStateListener>()
    private val lock = Any()
    private var output: DesktopAudioOutput? = null

    @Volatile
    override var state: LearningContentAudioState = LearningContentAudioState.Idle
        private set

    override fun play(path: Path) {
        val normalized = path.toAbsolutePath().normalize()
        val token = generation.incrementAndGet()
        closeOutput()
        publish(LearningContentAudioState.Starting(normalized))

        thread(name = "learning-content-audio", isDaemon = true) {
            runCatching {
                DesktopAudioDecoder.open(normalized).use { stream ->
                    val nextOutput = outputFactory.open(stream.format)
                    synchronized(lock) {
                        if (generation.get() != token) {
                            nextOutput.close()
                            return@use
                        }
                        output = nextOutput
                    }
                    nextOutput.start()
                    publishIfCurrent(token, LearningContentAudioState.Playing(normalized))
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (generation.get() == token) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        nextOutput.write(buffer, 0, count)
                    }
                    if (generation.get() == token) nextOutput.drain()
                }
            }.onFailure { failure ->
                publishIfCurrent(
                    token,
                    LearningContentAudioState.Failed(
                        normalized,
                        failure.message?.takeIf(String::isNotBlank)
                            ?: "Audio could not be played."
                    )
                )
            }
            synchronized(lock) {
                val active = output
                if (active != null && generation.get() == token) {
                    active.runCatching { close() }
                    output = null
                }
            }
            if (generation.get() == token && state !is LearningContentAudioState.Failed) {
                publish(LearningContentAudioState.Idle)
            }
        }
    }

    override fun stop() {
        generation.incrementAndGet()
        closeOutput()
        publish(LearningContentAudioState.Idle)
    }

    override fun listen(listener: LearningContentAudioStateListener): AutoCloseable {
        listeners += listener
        listener.onStateChanged(state)
        return AutoCloseable { listeners -= listener }
    }

    override fun close() {
        stop()
        listeners.clear()
    }

    private fun publishIfCurrent(token: Long, next: LearningContentAudioState) {
        if (generation.get() == token) publish(next)
    }

    private fun publish(next: LearningContentAudioState) {
        state = next
        listeners.forEach { it.onStateChanged(next) }
    }

    private fun closeOutput() {
        synchronized(lock) {
            output?.runCatching {
                stop()
                close()
            }
            output = null
        }
    }
}

class LearningContentAudioController(
    private val player: LearningContentAudioPlayer
) : AutoCloseable {
    private var primaryAudio: Path? = null
    private var boundScene: LearningScene? = null

    val state: LearningContentAudioState
        get() = player.state

    var loopDelaySeconds: Double = 0.5
    var activeLoopPath: Path? by mutableStateOf(null)
        private set

    private var loopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var listenerRegistration: AutoCloseable? = null

    init {
        listenerRegistration = player.listen { state ->
            if (state is LearningContentAudioState.Idle && activeLoopPath != null) {
                val currentPath = activeLoopPath
                if (currentPath != null && loopJob?.isActive == true) {
                    scope.launch {
                        val delayMillis = (loopDelaySeconds.coerceIn(0.0, 10.0) * 1000).toLong()
                        if (delayMillis > 0) delay(delayMillis)
                        if (activeLoopPath == currentPath && loopJob?.isActive == true) {
                            player.play(currentPath)
                        }
                    }
                }
            }
        }
    }

    fun bind(scene: LearningScene?) {
        stopLoop()
        player.stop()
        boundScene = scene
        primaryAudio = scene
            ?.blocks
            ?.filterIsInstance<PresentedLearningBlock.Audio>()
            ?.firstOrNull()
            ?.path
    }

    fun playOnce(path: Path) {
        stopLoop()
        player.play(path)
    }

    fun toggleLoop(path: Path, delaySeconds: Double = loopDelaySeconds) {
        if (activeLoopPath == path) {
            stopLoop()
        } else {
            startLoop(path, delaySeconds)
        }
    }

    fun startLoop(path: Path, delaySeconds: Double = loopDelaySeconds) {
        stopLoop()
        this.loopDelaySeconds = delaySeconds
        activeLoopPath = path
        loopJob = scope.launch {
            player.play(path)
        }
    }

    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
        activeLoopPath = null
    }

    fun toggle(path: Path) {
        stopLoop()
        when (val current = player.state) {
            is LearningContentAudioState.Starting ->
                if (current.path == path) player.stop() else player.play(path)
            is LearningContentAudioState.Playing ->
                if (current.path == path) player.stop() else player.play(path)
            else -> player.play(path)
        }
    }

    fun replayPrimary(): Boolean {
        stopLoop()
        val path = primaryAudio ?: return false
        player.play(path)
        return true
    }

    fun stop() {
        stopLoop()
        player.stop()
    }

    fun listen(listener: LearningContentAudioStateListener) = player.listen(listener)

    override fun close() {
        stopLoop()
        listenerRegistration?.close()
        scope.cancel()
        player.close()
    }
}
