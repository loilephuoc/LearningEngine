package vn.loi.learning.android.reminder

import android.content.Context
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.LearningEngineAudioPolicy
import vn.loi.learning.android.recording.QuickVoiceRecorderController

fun interface AndroidVocabularyReminderDelayScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): AutoCloseable
}

class CoroutineAndroidVocabularyReminderDelayScheduler(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : AndroidVocabularyReminderDelayScheduler, AutoCloseable {
    override fun schedule(delayMillis: Long, action: () -> Unit): AutoCloseable {
        val job = scope.launch {
            delay(delayMillis)
            action()
        }
        return AutoCloseable { job.cancel() }
    }

    override fun close() {
        // Scope supervisor job lifecycle handled if needed
    }
}

class AndroidVocabularyReminderRuntime(
    private val preferencesController: AndroidVocabularyReminderPreferencesController,
    private val selector: AndroidVocabularyReminderCandidateSelector,
    private val notificationHelper: AndroidVocabularyReminderNotificationHelper,
    private val audioController: AndroidAudioController? = null,
    private val overlayPresenter: AndroidVocabularyReminderOverlayPresenter? = null,
    private val deviceStateProvider: AndroidVocabularyReminderDeviceStateProvider? = null,
    private val resolveMedia: (String) -> String? = { null },
    private val delayScheduler: AndroidVocabularyReminderDelayScheduler = CoroutineAndroidVocabularyReminderDelayScheduler(),
    private val nowProvider: () -> Instant = Instant::now,
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault
) : AutoCloseable {

    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scheduledTask: AutoCloseable? = null
    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var isReviewScreenActive = false

    val settings: AndroidVocabularyReminderSettings
        get() = preferencesController.current()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        runtimeScope.launch {
            preferencesController.settings.collectLatest {
                reschedule()
            }
        }
    }

    fun setReviewScreenActive(active: Boolean) {
        isReviewScreenActive = active
        if (active) {
            overlayPresenter?.hide()
        }
    }

    fun preview(draft: AndroidVocabularyReminderDraft): AndroidVocabularyReminderActionResult {
        val validation = draft.validate(preferencesController.current().pausedUntil)
        if (validation is AndroidVocabularyReminderDraftValidation.Invalid) {
            return AndroidVocabularyReminderActionResult.Failure(validation.message)
        }
        val settings = (validation as AndroidVocabularyReminderDraftValidation.Valid).settings.copy(enabled = true, pausedUntil = null)
        return when (val result = selector.select(settings)) {
            is AndroidVocabularyCandidateSelectionResult.Selected -> {
                val previewDuration = maxOf(settings.displayDurationMillis, 15_000L)
                var visualPresented = false

                val canOverlay = settings.overlayPopupEnabled &&
                    (deviceStateProvider?.isOverlayPermissionGranted() == true) &&
                    (overlayPresenter != null)

                if (canOverlay) {
                    val shown = overlayPresenter.show(
                        candidate = result.candidate,
                        mode = settings.selectionMode,
                        displayDurationMillis = previewDuration
                    )
                    if (shown) {
                        visualPresented = true
                    }
                }

                if (!visualPresented) {
                    val posted = notificationHelper.postReminderNotification(
                        result.candidate,
                        settings.selectionMode,
                        previewDuration
                    )
                    if (posted) {
                        visualPresented = true
                    }
                }

                if (visualPresented) {
                    if (settings.autoPlayPronunciation) {
                        playCandidateAudioIfPermitted(result.candidate)
                    }
                    AndroidVocabularyReminderActionResult.Success
                } else {
                    AndroidVocabularyReminderActionResult.Failure("Notification permission required.")
                }
            }
            is AndroidVocabularyCandidateSelectionResult.NoCandidate -> {
                AndroidVocabularyReminderActionResult.Failure(result.reason.userFacingMessage())
            }
        }
    }

    fun pause30Minutes() = preferencesController.pause30Minutes()
    fun pauseOneHour() = preferencesController.pauseOneHour()
    fun pauseToday() = preferencesController.pauseToday()
    fun resumeNow() = preferencesController.resumeNow()

    @Synchronized
    private fun reschedule() {
        scheduledTask?.close()
        scheduledTask = null
        val current = preferencesController.current()
        if (!closed.get() && started.get() && current.enabled) {
            scheduledTask = delayScheduler.schedule(current.intervalMillis, ::tick)
        }
    }

    @Synchronized
    private fun tick() {
        scheduledTask = null
        if (closed.get() || !started.get()) return
        val current = preferencesController.current()
        if (!current.enabled) return

        try {
            dispatchIfEligible(current)
        } catch (t: Throwable) {
            android.util.Log.e(TAG, "tick exception in dispatchIfEligible", t)
        }

        if (!closed.get() && started.get() && current.enabled) {
            reschedule()
        }
    }

    private fun dispatchIfEligible(settings: AndroidVocabularyReminderSettings) {
        if (settings.selectedPackageId == null) {
            android.util.Log.d(TAG, "dispatchIfEligible skipped: selectedPackageId is null")
            return
        }
        if (!isActiveAt(settings, nowProvider(), zoneProvider())) {
            android.util.Log.d(TAG, "dispatchIfEligible skipped: not active at current time")
            return
        }
        if (isReviewScreenActive) {
            android.util.Log.d(TAG, "dispatchIfEligible skipped: isReviewScreenActive=true")
            return
        }

        when (val result = selector.select(settings)) {
            is AndroidVocabularyCandidateSelectionResult.Selected -> {
                val candidate = result.candidate
                var visualPresented = false

                val isPermissionGranted = deviceStateProvider?.isOverlayPermissionGranted() ?: false
                val isInteractive = deviceStateProvider?.isScreenInteractive() ?: true
                val isLocked = deviceStateProvider?.isDeviceLocked() ?: false

                val canOverlay = settings.overlayPopupEnabled &&
                    isPermissionGranted &&
                    isInteractive &&
                    !isLocked &&
                    !isReviewScreenActive &&
                    overlayPresenter != null

                android.util.Log.i(
                    TAG,
                    "Scheduled dispatch: overlayPopupEnabled=${settings.overlayPopupEnabled}, permissionGranted=$isPermissionGranted, isInteractive=$isInteractive, isLocked=$isLocked, canOverlay=$canOverlay"
                )

                if (canOverlay && overlayPresenter != null) {
                    val shown = overlayPresenter.show(
                        candidate = candidate,
                        mode = settings.selectionMode,
                        displayDurationMillis = settings.displayDurationMillis
                    )
                    if (shown) {
                        visualPresented = true
                    }
                }

                if (!visualPresented) {
                    val fallbackReason = when {
                        !settings.overlayPopupEnabled -> "OVERLAY_DISABLED"
                        !isPermissionGranted -> "PERMISSION_MISSING"
                        !isInteractive -> "SCREEN_NOT_INTERACTIVE"
                        isLocked -> "DEVICE_LOCKED"
                        isReviewScreenActive -> "REVIEW_SCREEN_ACTIVE"
                        overlayPresenter == null -> "OVERLAY_PRESENTER_NULL"
                        else -> "OVERLAY_SHOW_FAILED"
                    }
                    android.util.Log.w(TAG, "Fallback notification invoked: $fallbackReason")
                    val posted = notificationHelper.postReminderNotification(
                        candidate,
                        settings.selectionMode,
                        settings.displayDurationMillis
                    )
                    if (posted) {
                        visualPresented = true
                    }
                }

                if (visualPresented && settings.autoPlayPronunciation) {
                    playCandidateAudioIfPermitted(candidate)
                }
            }
            is AndroidVocabularyCandidateSelectionResult.NoCandidate -> {
                android.util.Log.i(TAG, "dispatchIfEligible: no candidate available (${result.reason})")
            }
        }
    }
    private fun playCandidateAudioIfPermitted(candidate: AndroidVocabularyCandidate) {
        if (candidate.primaryAudioReference == null) return
        val recordingState = QuickVoiceRecorderController.state.value
        if (recordingState is vn.loi.learning.android.recording.QuickVoiceRecorderState.Recording ||
            recordingState is vn.loi.learning.android.recording.QuickVoiceRecorderState.Starting
        ) return
        if (LearningEngineAudioPolicy.isMuted.value) return

        val audioPath = resolveMedia(candidate.primaryAudioReference) ?: return
        audioController?.replay(audioPath)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            scheduledTask?.close()
            scheduledTask = null
            overlayPresenter?.shutdown()
            notificationHelper.cancelNotification()
        }
    }

    companion object {
        private const val TAG = "VocabularyReminderOverlay"

        fun isActiveAt(
            settings: AndroidVocabularyReminderSettings,
            instant: Instant,
            zoneId: ZoneId
        ): Boolean {
            if (!settings.enabled) return false
            val pausedUntil = settings.pausedUntil
            if (pausedUntil != null && instant.isBefore(pausedUntil)) return false

            val localTime = instant.atZone(zoneId).toLocalTime()
            val start = settings.activeStart
            val end = settings.activeEnd

            return if (start == end) {
                true // 24-hour active window
            } else if (start.isBefore(end)) {
                !localTime.isBefore(start) && !localTime.isAfter(end)
            } else {
                // Overnight window e.g. 22:00 -> 07:00
                !localTime.isBefore(start) || !localTime.isAfter(end)
            }
        }
    }
}

private fun AndroidVocabularyCandidateSelectionResult.Reason.userFacingMessage() = when (this) {
    AndroidVocabularyCandidateSelectionResult.Reason.DISABLED -> "Reminder is disabled."
    AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_NOT_SELECTED -> "Select a package first."
    AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE -> "Selected package is unavailable."
    AndroidVocabularyCandidateSelectionResult.Reason.PACKAGE_EMPTY -> "Selected package has no content."
    AndroidVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE -> "No eligible vocabulary items found for this mode."
}
