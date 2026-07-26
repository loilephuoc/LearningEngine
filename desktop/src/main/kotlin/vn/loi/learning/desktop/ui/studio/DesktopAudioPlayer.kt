package vn.loi.learning.desktop.ui.studio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DesktopAudioPlayer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AudioPlayer {

    override var state by mutableStateOf<AudioPlayerState>(AudioPlayerState.Idle)
        private set

    private var activeLine: SourceDataLine? = null
    private var playbackJob: Job? = null

    @Volatile
    private var isStopRequested = false
    private var totalDurationMs: Long = 0L

    override fun load(path: Path) {
        stop()
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            state = AudioPlayerState.Error(path, "Missing file")
            return
        }
        state = AudioPlayerState.Idle
    }

    override fun play(path: Path) {
        stop()

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            state = AudioPlayerState.Error(path, "Missing file")
            return
        }

        state = AudioPlayerState.Loading(path)
        isStopRequested = false

        playbackJob = scope.launch(Dispatchers.IO) {
            var line: SourceDataLine? = null
            try {
                val rawInputStream = BufferedInputStream(Files.newInputStream(path))
                val audioInputStream = AudioSystem.getAudioInputStream(rawInputStream)
                val baseFormat = audioInputStream.format

                val frameLength = audioInputStream.frameLength
                if (frameLength > 0 && baseFormat.frameRate > 0) {
                    totalDurationMs = ((frameLength / baseFormat.frameRate) * 1000).toLong()
                } else {
                    totalDurationMs = 0L
                }

                val sampleRate = if (baseFormat.sampleRate > 0) baseFormat.sampleRate else 44100f
                val channels = if (baseFormat.channels > 0) baseFormat.channels else 2

                val targetFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    channels,
                    channels * 2,
                    sampleRate,
                    false
                )

                val pcmStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream)
                val info = DataLine.Info(SourceDataLine::class.java, targetFormat)

                if (!AudioSystem.isLineSupported(info)) {
                    withContext(Dispatchers.Main) {
                        state = AudioPlayerState.Error(path, "Device unavailable")
                    }
                    return@launch
                }

                line = AudioSystem.getLine(info) as SourceDataLine
                line.open(targetFormat)
                line.start()
                activeLine = line

                withContext(Dispatchers.Main) {
                    state = AudioPlayerState.Playing(path)
                }

                val buffer = ByteArray(4096)
                var bytesRead = 0

                while (!isStopRequested && pcmStream.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                    if (bytesRead > 0) {
                        line.write(buffer, 0, bytesRead)
                    }
                }

                if (!isStopRequested) {
                    line.drain()
                }

                withContext(Dispatchers.Main) {
                    if (state is AudioPlayerState.Playing && (state as AudioPlayerState.Playing).path == path) {
                        state = AudioPlayerState.Idle
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    state = AudioPlayerState.Error(path, "Cannot play audio")
                }
            } finally {
                try {
                    line?.stop()
                    line?.close()
                } catch (_: Exception) {
                }
                activeLine = null
            }
        }
    }

    override fun pause() {
        stop()
    }

    override fun stop() {
        isStopRequested = true
        playbackJob?.cancel()
        playbackJob = null
        try {
            activeLine?.stop()
            activeLine?.flush()
            activeLine?.close()
        } catch (_: Exception) {
        } finally {
            activeLine = null
        }
        state = AudioPlayerState.Idle
    }

    override fun release() {
        stop()
    }

    override fun positionMs(): Long {
        return activeLine?.microsecondPosition?.let { it / 1000 } ?: 0L
    }

    override fun durationMs(): Long {
        return totalDurationMs
    }
}
