package vn.loi.learning.android.reminder

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AndroidVocabularyReminderRuntime(
    private val preferencesController: AndroidVocabularyReminderPreferencesController,
    private val selector: AndroidVocabularyReminderCandidateSelector,
    private val notificationHelper: AndroidVocabularyReminderNotificationHelper,
    private val audioController: vn.loi.learning.android.media.AndroidAudioController? = null,
    private val overlayPresenter: AndroidVocabularyReminderOverlayPresenter? = null,
    private val deviceStateProvider: AndroidVocabularyReminderDeviceStateProvider? = null,
    private val resolveMedia: (String) -> String? = { null }
) : AutoCloseable {

    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var isReviewScreenActive = false

    val settings: AndroidVocabularyReminderSettings
        get() = preferencesController.current()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        // Single Owner Contract: AndroidLockScreenVocabularyCoordinator owns recurring UnlockedReminderTimer.
        // AndroidVocabularyReminderRuntime no longer runs a duplicate recurring timer.
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

    private fun playCandidateAudioIfPermitted(candidate: AndroidVocabularyCandidate) {
        val rawRef = candidate.primaryAudioReference ?: return
        val resolvedPath = resolveMedia(rawRef) ?: return
        val file = File(resolvedPath)
        if (!file.exists() || !file.canRead()) return

        try {
            val mediaPlayer = MediaPlayer()
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            mediaPlayer.setAudioAttributes(attributes)
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.setOnPreparedListener { mp ->
                mp.start()
            }
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.setOnErrorListener { mp, _, _ ->
                mp.release()
                true
            }
            mediaPlayer.prepareAsync()
        } catch (t: Throwable) {
            android.util.Log.e(TAG, "Failed to play preview audio", t)
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
    }

    companion object {
        private const val TAG = "VocabularyReminderRuntime"

        fun isActiveAt(
            settings: AndroidVocabularyReminderSettings,
            now: Instant,
            zone: ZoneId
        ): Boolean {
            if (!settings.enabled) return false
            val pausedUntil = settings.pausedUntil
            if (pausedUntil != null && now.isBefore(pausedUntil)) return false

            val localTime = now.atZone(zone).toLocalTime()
            val start = settings.activeStart
            val end = settings.activeEnd

            return if (start <= end) {
                !localTime.isBefore(start) && !localTime.isAfter(end)
            } else {
                // Overnight window, e.g. 22:00 -> 07:00
                !localTime.isBefore(start) || !localTime.isAfter(end)
            }
        }
    }
}
