package vn.loi.learning.desktop.notification

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.study.JavaSoundLearningContentAudioPlayer
import vn.loi.learning.desktop.ui.study.LearningContentAudioPlayer

interface DesktopVocabularyReminderAudioLifecycle : AutoCloseable {
    fun start(audioReference: String?)
    fun startLoop(audioReference: String?) = start(audioReference)
    fun stop()
    fun listen(listener: (Boolean) -> Unit): AutoCloseable = AutoCloseable {}
    override fun close()
}

object NoOpDesktopVocabularyReminderAudioLifecycle : DesktopVocabularyReminderAudioLifecycle {
    override fun start(audioReference: String?) = Unit
    override fun stop() = Unit
    override fun close() = Unit
}

class DefaultDesktopVocabularyReminderAudioLifecycle(
    private val mediaStorage: ContentMediaStorage,
    private val player: LearningContentAudioPlayer = JavaSoundLearningContentAudioPlayer()
) : DesktopVocabularyReminderAudioLifecycle {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = AtomicLong()
    @Volatile private var loopPath: java.nio.file.Path? = null
    @Volatile private var closed = false

    override fun listen(listener: (Boolean) -> Unit): AutoCloseable =
        player.listen { state ->
            listener(
                state is vn.loi.learning.desktop.ui.study.LearningContentAudioState.Starting ||
                    state is vn.loi.learning.desktop.ui.study.LearningContentAudioState.Playing
            )
        }

    private val loopRegistration = player.listen { state ->
        if (state is vn.loi.learning.desktop.ui.study.LearningContentAudioState.Completed &&
            !closed && loopPath == state.path) {
            player.runCatching { play(state.path) }
        }
    }

    override fun start(audioReference: String?) {
        loopPath = null
        val token = generation.incrementAndGet()
        player.runCatching { stop() }
        if (closed || audioReference.isNullOrBlank()) return
        scope.launch {
            val path = runCatching { mediaStorage.resolve(audioReference) }.getOrNull() ?: return@launch
            if (!closed && generation.get() == token) player.runCatching { play(path) }
        }
    }

    override fun startLoop(audioReference: String?) {
        val token = generation.incrementAndGet()
        player.runCatching { stop() }
        if (closed || audioReference.isNullOrBlank()) return
        scope.launch {
            val path = runCatching { mediaStorage.resolve(audioReference) }.getOrNull() ?: return@launch
            if (!closed && generation.get() == token) {
                loopPath = path.toAbsolutePath().normalize()
                player.runCatching { play(path) }
            }
        }
    }

    override fun stop() {
        loopPath = null
        generation.incrementAndGet()
        player.runCatching { stop() }
    }

    override fun close() {
        if (closed) return
        closed = true
        generation.incrementAndGet()
        scope.cancel()
        loopRegistration.runCatching { close() }
        player.runCatching { stop() }
        player.runCatching { close() }
    }
}
