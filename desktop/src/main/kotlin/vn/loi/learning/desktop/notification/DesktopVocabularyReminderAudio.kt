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
    fun start(audioReference: String?, onCompleted: (() -> Unit)? = null) = start(audioReference)
    fun startLoop(audioReference: String?) = start(audioReference)
    fun stop()
    fun listen(listener: (Boolean) -> Unit): AutoCloseable = AutoCloseable {}
    override fun close()
}

object NoOpDesktopVocabularyReminderAudioLifecycle : DesktopVocabularyReminderAudioLifecycle {
    override fun start(audioReference: String?) = Unit
    override fun start(audioReference: String?, onCompleted: (() -> Unit)?) = Unit
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
    @Volatile private var activeCompletionCallback: Pair<Long, () -> Unit>? = null
    @Volatile private var closed = false

    override fun listen(listener: (Boolean) -> Unit): AutoCloseable =
        player.listen { state ->
            listener(
                state is vn.loi.learning.desktop.ui.study.LearningContentAudioState.Starting ||
                    state is vn.loi.learning.desktop.ui.study.LearningContentAudioState.Playing
            )
        }

    private val loopRegistration = player.listen { state ->
        if (state is vn.loi.learning.desktop.ui.study.LearningContentAudioState.Completed && !closed) {
            if (loopPath == state.path) {
                player.runCatching { play(state.path) }
            } else {
                val cb = activeCompletionCallback
                if (cb != null) {
                    activeCompletionCallback = null
                    cb.second.invoke()
                }
            }
        } else if (state is vn.loi.learning.desktop.ui.study.LearningContentAudioState.Failed && !closed) {
            val cb = activeCompletionCallback
            if (cb != null) {
                activeCompletionCallback = null
                cb.second.invoke()
            }
        }
    }

    override fun start(audioReference: String?) {
        start(audioReference, onCompleted = null)
    }

    override fun start(audioReference: String?, onCompleted: (() -> Unit)?) {
        loopPath = null
        val token = generation.incrementAndGet()
        activeCompletionCallback = onCompleted?.let { token to it }
        player.runCatching { stop() }
        if (closed || audioReference.isNullOrBlank()) {
            val cb = activeCompletionCallback
            if (cb?.first == token) {
                activeCompletionCallback = null
                cb.second.invoke()
            }
            return
        }
        scope.launch {
            val path = runCatching { mediaStorage.resolve(audioReference) }.getOrNull()
            if (path == null) {
                if (generation.get() == token) {
                    val cb = activeCompletionCallback
                    if (cb?.first == token) {
                        activeCompletionCallback = null
                        cb.second.invoke()
                    }
                }
                return@launch
            }
            if (!closed && generation.get() == token) {
                player.runCatching { play(path) }.onFailure {
                    if (generation.get() == token) {
                        val cb = activeCompletionCallback
                        if (cb?.first == token) {
                            activeCompletionCallback = null
                            cb.second.invoke()
                        }
                    }
                }
            }
        }
    }

    override fun startLoop(audioReference: String?) {
        val token = generation.incrementAndGet()
        activeCompletionCallback = null
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
        activeCompletionCallback = null
        generation.incrementAndGet()
        player.runCatching { stop() }
    }

    override fun close() {
        if (closed) return
        closed = true
        activeCompletionCallback = null
        generation.incrementAndGet()
        scope.cancel()
        loopRegistration.runCatching { close() }
        player.runCatching { stop() }
        player.runCatching { close() }
    }
}
