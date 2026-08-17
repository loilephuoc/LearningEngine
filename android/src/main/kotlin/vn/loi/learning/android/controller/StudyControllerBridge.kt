package vn.loi.learning.android.controller

import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.domain.study.memory.model.ReviewRating
import java.util.concurrent.atomic.AtomicReference

/**
 * Interface implemented by active Study ViewModel/controller to receive controller actions.
 */
interface StudyControllerTarget {
    fun currentContext(): ControllerContext
    suspend fun revealAnswer(): Boolean
    suspend fun continueCurrentMode(): Boolean = false
    suspend fun rate(rating: ReviewRating): Boolean
    suspend fun next(): Boolean
    suspend fun previous(): Boolean
    fun replayAudio(): Boolean
    fun loopPrimaryAudio(): Boolean = false
    fun loopExampleEnglish(): Boolean = false
    fun primaryEnglishAudio(): String? = null
    fun primaryVietnameseAudio(): String? = null
    fun exampleEnglishAudio(): String? = null
    fun exampleVietnameseAudio(): String? = null
    fun describeState(): String = "TargetActive"
    suspend fun awaitIdle() {}
}

/**
 * Metadata for currently active Study background/controller audio playback.
 */
data class ActiveStudyPlayback(
    val itemKey: String?,
    val role: String?,
    val isLooping: Boolean,
    val reason: StudyAudioReason,
    val startedWhileForeground: Boolean
)

/**
 * Thread-safe bridge connecting controller input dispatches to the currently active Study target,
 * audio playback authorities (primary EN/VI, examples, loops), continue actions, and AutoPlay starter.
 */
object StudyControllerBridge {

    private val targetRef = AtomicReference<StudyControllerTarget?>(null)
    private val audioReplayerRef = AtomicReference<(() -> Boolean)?>(null)
    private val primaryEnglishPlayerRef = AtomicReference<(() -> Boolean)?>(null)
    private val primaryVietnamesePlayerRef = AtomicReference<(() -> Boolean)?>(null)
    private val exampleEnglishPlayerRef = AtomicReference<(() -> Boolean)?>(null)
    private val exampleVietnamesePlayerRef = AtomicReference<(() -> Boolean)?>(null)
    private val continueCurrentModeRef = AtomicReference<(() -> Boolean)?>(null)
    private val startAutoPlayRef = AtomicReference<(() -> Boolean)?>(null)
    private val backgroundAudioControllerRef = AtomicReference<AndroidAudioController?>(null)
    private val isForegroundRef = java.util.concurrent.atomic.AtomicBoolean(true)
    private val currentPlaybackRef = AtomicReference<ActiveStudyPlayback?>(null)
    private val consumedAutoplayKeys = mutableSetOf<String>()

    val activeTarget: StudyControllerTarget?
        get() = targetRef.get()

    val currentPlayback: ActiveStudyPlayback?
        get() = currentPlaybackRef.get()

    fun onActivityForegroundChanged(isForeground: Boolean) {
        val previous = isForegroundRef.getAndSet(isForeground)
        if (previous && !isForeground) {
            val playback = currentPlaybackRef.get()
            if (playback != null && playback.startedWhileForeground && playback.reason == StudyAudioReason.MANUAL_LOOP) {
                stopAudio(StudyAudioReason.MANUAL_LOOP)
            }
        }
    }

    fun register(target: StudyControllerTarget) {
        targetRef.set(target)
    }

    fun unregister(target: StudyControllerTarget) {
        targetRef.compareAndSet(target, null)
    }

    fun registerBackgroundAudioController(controller: AndroidAudioController) {
        backgroundAudioControllerRef.set(controller)
    }

    fun unregisterBackgroundAudioController(controller: AndroidAudioController) {
        backgroundAudioControllerRef.compareAndSet(controller, null)
    }

    fun claimAutoplay(itemKey: String?, role: String): Boolean {
        if (itemKey.isNullOrBlank()) return false
        return consumedAutoplayKeys.add("${itemKey}_${role}")
    }

    fun playAudio(
        itemKey: String?,
        path: String?,
        role: String?,
        isLooping: Boolean,
        reason: StudyAudioReason
    ): Boolean {
        if (path.isNullOrBlank()) return false
        val controller = backgroundAudioControllerRef.get() ?: return false
        val fileName = path.substringAfterLast('/').substringAfterLast('\\')
        val wasForeground = isForegroundRef.get()
        val playback = ActiveStudyPlayback(
            itemKey = itemKey,
            role = role,
            isLooping = isLooping,
            reason = reason,
            startedWhileForeground = wasForeground
        )
        currentPlaybackRef.set(playback)
        ControllerDiagnosticsHolder.setActiveStudyPlayback(playback)
        ControllerDiagnosticsHolder.recordAudioTrace(
            StudyAudioTraceEntry(
                kind = StudyAudioEventKind.AUDIO_START,
                itemKey = itemKey,
                role = role,
                path = fileName,
                isLooping = isLooping,
                reason = reason
            )
        )
        controller.replay(path, isLooping = isLooping)
        return true
    }

    fun stopAudio(reason: StudyAudioReason) {
        currentPlaybackRef.set(null)
        ControllerDiagnosticsHolder.setActiveStudyPlayback(null)
        val controller = backgroundAudioControllerRef.get()
        if (controller != null) {
            ControllerDiagnosticsHolder.recordAudioTrace(
                StudyAudioTraceEntry(
                    kind = StudyAudioEventKind.AUDIO_STOP,
                    itemKey = null,
                    role = null,
                    path = null,
                    isLooping = false,
                    reason = reason
                )
            )
            controller.stop()
        }
    }

    fun playAudioPath(path: String?): Boolean {
        return playAudio(null, path, null, isLooping = false, reason = StudyAudioReason.ITEM_ENTRY)
    }

    fun registerAudioReplayer(replayer: () -> Boolean) {
        audioReplayerRef.set(replayer)
    }

    fun unregisterAudioReplayer(replayer: () -> Boolean) {
        audioReplayerRef.compareAndSet(replayer, null)
    }

    fun registerPrimaryEnglishPlayer(player: () -> Boolean) {
        primaryEnglishPlayerRef.set(player)
    }

    fun unregisterPrimaryEnglishPlayer(player: () -> Boolean) {
        primaryEnglishPlayerRef.compareAndSet(player, null)
    }

    fun registerPrimaryVietnamesePlayer(player: () -> Boolean) {
        primaryVietnamesePlayerRef.set(player)
    }

    fun unregisterPrimaryVietnamesePlayer(player: () -> Boolean) {
        primaryVietnamesePlayerRef.compareAndSet(player, null)
    }

    fun registerExampleEnglishPlayer(player: () -> Boolean) {
        exampleEnglishPlayerRef.set(player)
    }

    fun unregisterExampleEnglishPlayer(player: () -> Boolean) {
        exampleEnglishPlayerRef.compareAndSet(player, null)
    }

    fun registerExampleVietnamesePlayer(player: () -> Boolean) {
        exampleVietnamesePlayerRef.set(player)
    }

    fun unregisterExampleVietnamesePlayer(player: () -> Boolean) {
        exampleVietnamesePlayerRef.compareAndSet(player, null)
    }

    fun registerContinueCurrentMode(action: () -> Boolean) {
        continueCurrentModeRef.set(action)
    }

    fun unregisterContinueCurrentMode(action: () -> Boolean) {
        continueCurrentModeRef.compareAndSet(action, null)
    }

    fun registerStartAutoPlay(starter: () -> Boolean) {
        startAutoPlayRef.set(starter)
    }

    fun unregisterStartAutoPlay(starter: () -> Boolean) {
        startAutoPlayRef.compareAndSet(starter, null)
    }

    fun currentContext(): ControllerContext {
        return activeTarget?.currentContext() ?: ControllerContext.GLOBAL
    }

    fun replayAudio(): Boolean {
        val replayer = audioReplayerRef.get()
        if (replayer != null && replayer()) return true
        val target = activeTarget ?: return false
        val audioPath = target.primaryEnglishAudio() ?: target.primaryVietnameseAudio()
        if (playAudio(null, audioPath, "PRIMARY", isLooping = false, reason = StudyAudioReason.MANUAL_PLAY)) return true
        return target.replayAudio()
    }

    fun playPrimaryEnglish(): Boolean {
        val player = primaryEnglishPlayerRef.get()
        if (player != null && player()) return true
        val target = activeTarget ?: return false
        if (playAudio(null, target.primaryEnglishAudio(), "PRIMARY_EN", isLooping = false, reason = StudyAudioReason.MANUAL_PLAY)) return true
        return target.replayAudio()
    }

    fun playPrimaryVietnamese(): Boolean {
        val player = primaryVietnamesePlayerRef.get()
        if (player != null && player()) return true
        val target = activeTarget ?: return false
        return playAudio(null, target.primaryVietnameseAudio(), "PRIMARY_VI", isLooping = false, reason = StudyAudioReason.MANUAL_PLAY)
    }

    fun playExampleEnglish(): Boolean {
        val player = exampleEnglishPlayerRef.get()
        if (player != null && player()) return true
        val target = activeTarget ?: return false
        return playAudio(null, target.exampleEnglishAudio(), "EXAMPLE_EN", isLooping = false, reason = StudyAudioReason.MANUAL_PLAY)
    }

    fun playExampleVietnamese(): Boolean {
        val player = exampleVietnamesePlayerRef.get()
        if (player != null && player()) return true
        val target = activeTarget ?: return false
        return playAudio(null, target.exampleVietnameseAudio(), "EXAMPLE_VI", isLooping = false, reason = StudyAudioReason.MANUAL_PLAY)
    }

    fun loopPrimaryAudio(): Boolean {
        val target = activeTarget ?: return false
        val audioPath = target.primaryEnglishAudio() ?: target.primaryVietnameseAudio()
        if (playAudio(null, audioPath, "EXPECTED_ANSWER", isLooping = true, reason = StudyAudioReason.MANUAL_LOOP)) return true
        return target.loopPrimaryAudio()
    }

    fun loopExampleEnglish(): Boolean {
        val target = activeTarget ?: return false
        val audioPath = target.exampleEnglishAudio()
        if (playAudio(null, audioPath, "EXAMPLE_EN", isLooping = true, reason = StudyAudioReason.MANUAL_LOOP)) return true
        return target.loopExampleEnglish()
    }

    suspend fun revealAnswer(): Boolean {
        return activeTarget?.revealAnswer() ?: false
    }

    suspend fun continueCurrentMode(): Boolean {
        val target = activeTarget
        if (target != null) {
            val result = target.continueCurrentMode()
            if (result) return true
        }
        return continueCurrentModeRef.get()?.invoke() ?: false
    }

    suspend fun rate(rating: ReviewRating): Boolean {
        return activeTarget?.rate(rating) ?: false
    }

    suspend fun next(): Boolean {
        return activeTarget?.next() ?: false
    }

    suspend fun previous(): Boolean {
        return activeTarget?.previous() ?: false
    }

    fun startAutoPlay(): Boolean {
        return startAutoPlayRef.get()?.invoke() ?: false
    }

    suspend fun awaitIdle() {
        activeTarget?.awaitIdle()
    }

    fun describeState(): String {
        return activeTarget?.describeState() ?: "NoActiveStudyTarget"
    }

    fun clear() {
        targetRef.set(null)
        audioReplayerRef.set(null)
        primaryEnglishPlayerRef.set(null)
        primaryVietnamesePlayerRef.set(null)
        exampleEnglishPlayerRef.set(null)
        exampleVietnamesePlayerRef.set(null)
        continueCurrentModeRef.set(null)
        startAutoPlayRef.set(null)
        backgroundAudioControllerRef.set(null)
        currentPlaybackRef.set(null)
        isForegroundRef.set(true)
        consumedAutoplayKeys.clear()
    }
}
