package vn.loi.learning.android.reminder

import android.graphics.Bitmap
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

enum class VocabularyPresentationDeviceState {
    SCREEN_OFF,
    LOCKED_SCREEN_ON,
    UNLOCKED_SCREEN_ON
}

enum class AndroidVocabularyReminderSelectionMode {
    AGAIN_HARD,
    DUE,
    RANDOM_LEARNED,
    RANDOM_ALL,
    MARKED_DIFFICULT
}

enum class AndroidVocabularyReminderIntervalUnit {
    SECONDS,
    MINUTES
}

sealed interface ReminderQuickPauseAction {
    data class ForDuration(val duration: java.time.Duration) : ReminderQuickPauseAction
    data object Indefinitely : ReminderQuickPauseAction
}

sealed interface UnlockedReminderPauseState {
    data object Active : UnlockedReminderPauseState
    data class PausedUntil(val epochMillis: Long) : UnlockedReminderPauseState
    data object PausedIndefinitely : UnlockedReminderPauseState
}

data class AndroidVocabularyReminderSettings(
    val enabled: Boolean = false,
    val selectedPackageId: String? = null,
    val selectionMode: AndroidVocabularyReminderSelectionMode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD,
    val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    val activeStart: LocalTime = DEFAULT_ACTIVE_START,
    val activeEnd: LocalTime = DEFAULT_ACTIVE_END,
    val displayDurationMillis: Long = DEFAULT_DISPLAY_DURATION_MILLIS,
    val autoPlayPronunciation: Boolean = false,
    val overlayPopupEnabled: Boolean = false,
    val pausedUntil: Instant? = null,
    val quickPauseActionsEnabled: Boolean = true,
    val unlockedPausedUntilEpochMillis: Long = 0L,
    val unlockedPausedIndefinitely: Boolean = false
) {
    val intervalMinutes: Int
        get() = (intervalMillis / 60_000L).toInt().coerceAtLeast(1)

    val unlockedReminderIntervalMillis: Long
        get() = intervalMillis

    val unlockedPauseState: UnlockedReminderPauseState
        get() = when {
            unlockedPausedIndefinitely -> UnlockedReminderPauseState.PausedIndefinitely
            System.currentTimeMillis() < unlockedPausedUntilEpochMillis -> UnlockedReminderPauseState.PausedUntil(unlockedPausedUntilEpochMillis)
            else -> UnlockedReminderPauseState.Active
        }

    val isUnlockedPaused: Boolean
        get() = unlockedPausedIndefinitely || System.currentTimeMillis() < unlockedPausedUntilEpochMillis

    init {
        require(intervalMillis in MIN_INTERVAL_MILLIS..MAX_INTERVAL_MILLIS) {
            "Interval must be between $MIN_INTERVAL_MILLIS and $MAX_INTERVAL_MILLIS millis."
        }
        require(displayDurationMillis in MIN_DISPLAY_DURATION_MILLIS..MAX_DISPLAY_DURATION_MILLIS) {
            "Display duration must be between $MIN_DISPLAY_DURATION_MILLIS and $MAX_DISPLAY_DURATION_MILLIS millis."
        }
    }

    companion object {
        const val MIN_INTERVAL_MILLIS = 5_000L
        const val DEFAULT_INTERVAL_MILLIS = 15 * 60_000L
        const val MAX_INTERVAL_MILLIS = 525_600 * 60_000L
        const val MIN_DISPLAY_DURATION_MILLIS = 1_500L
        const val DEFAULT_DISPLAY_DURATION_MILLIS = 5_000L
        const val MAX_DISPLAY_DURATION_MILLIS = 60_000L
        val DEFAULT_ACTIVE_START: LocalTime = LocalTime.of(8, 0)
        val DEFAULT_ACTIVE_END: LocalTime = LocalTime.of(22, 0)
    }
}

data class AndroidVocabularyCandidate(
    val contentId: ContentId,
    val packageId: InstalledPackageId,
    val packageName: String,
    val primaryText: String,
    val answer: String?,
    val translation: String?,
    val ipa: String?,
    val partOfSpeech: String?,
    val imageReference: String?,
    val primaryAudioReference: String?,
    val answerAudioReference: String? = null,
    val exampleAudioReference: String? = null,
    val translationAudioReference: String? = null,
    val exampleTranslationAudioReference: String? = null,
    val example: String? = null,
    val exampleTranslation: String? = null,
    val lesson: String? = null,
    val section: String? = null
)

sealed interface AndroidVocabularyCandidateSelectionResult {
    data class Selected(val candidate: AndroidVocabularyCandidate) : AndroidVocabularyCandidateSelectionResult
    data class NoCandidate(val reason: Reason) : AndroidVocabularyCandidateSelectionResult

    enum class Reason {
        DISABLED,
        PACKAGE_NOT_SELECTED,
        PACKAGE_UNAVAILABLE,
        PACKAGE_EMPTY,
        NO_ELIGIBLE_CANDIDATE;

        fun userFacingMessage(): String = when (this) {
            DISABLED -> "Vocabulary reminders are disabled."
            PACKAGE_NOT_SELECTED -> "No vocabulary package selected."
            PACKAGE_UNAVAILABLE -> "Selected vocabulary package is unavailable."
            PACKAGE_EMPTY -> "Selected vocabulary package is empty."
            NO_ELIGIBLE_CANDIDATE -> "No eligible vocabulary found for current mode."
        }
    }
}

data class AndroidReminderReviewSession(
    val packageId: String,
    val mode: AndroidVocabularyReminderSelectionMode,
    val anchorContentId: String,
    val items: List<AndroidVocabularyCandidate>,
    val initialIndex: Int
)

data class AndroidVocabularyReminderDraft(
    val enabled: Boolean,
    val selectedPackageId: String?,
    val selectionMode: AndroidVocabularyReminderSelectionMode,
    val intervalValueText: String,
    val intervalUnit: AndroidVocabularyReminderIntervalUnit,
    val activeStartText: String,
    val activeEndText: String,
    val displayDurationText: String,
    val autoPlayPronunciation: Boolean,
    val overlayPopupEnabled: Boolean = false,
    val quickPauseActionsEnabled: Boolean = true
) {
    fun validate(
        pausedUntil: Instant?,
        unlockedPausedUntilEpochMillis: Long = 0L,
        unlockedPausedIndefinitely: Boolean = false
    ): AndroidVocabularyReminderDraftValidation {
        val intervalValue = intervalValueText.trim().toIntOrNull()
            ?: return AndroidVocabularyReminderDraftValidation.Invalid("Interval must be a valid positive number.")
        if (intervalValue <= 0) {
            return AndroidVocabularyReminderDraftValidation.Invalid("Interval must be greater than zero.")
        }
        val intervalMillis = when (intervalUnit) {
            AndroidVocabularyReminderIntervalUnit.SECONDS -> intervalValue * 1000L
            AndroidVocabularyReminderIntervalUnit.MINUTES -> intervalValue * 60_000L
        }
        if (intervalMillis !in AndroidVocabularyReminderSettings.MIN_INTERVAL_MILLIS..AndroidVocabularyReminderSettings.MAX_INTERVAL_MILLIS) {
            return AndroidVocabularyReminderDraftValidation.Invalid(
                "Interval must be between 5 seconds and 365 days."
            )
        }

        val displaySeconds = displayDurationText.trim().toDoubleOrNull()
            ?: return AndroidVocabularyReminderDraftValidation.Invalid("Display duration must be a valid number of seconds.")
        if (displaySeconds < 1.5 || displaySeconds > 60.0) {
            return AndroidVocabularyReminderDraftValidation.Invalid("Display duration must be between 1.5 and 60 seconds.")
        }
        val displayDurationMillis = (displaySeconds * 1000.0).toLong()

        val start = parseTimeSafely(activeStartText)
            ?: return AndroidVocabularyReminderDraftValidation.Invalid("Active window start time is invalid (use HH:mm format).")
        val end = parseTimeSafely(activeEndText)
            ?: return AndroidVocabularyReminderDraftValidation.Invalid("Active window end time is invalid (use HH:mm format).")

        return AndroidVocabularyReminderDraftValidation.Valid(
            AndroidVocabularyReminderSettings(
                enabled = enabled,
                selectedPackageId = selectedPackageId?.trim()?.takeIf(String::isNotEmpty),
                selectionMode = selectionMode,
                intervalMillis = intervalMillis,
                activeStart = start,
                activeEnd = end,
                displayDurationMillis = displayDurationMillis,
                autoPlayPronunciation = autoPlayPronunciation,
                overlayPopupEnabled = overlayPopupEnabled,
                pausedUntil = pausedUntil,
                quickPauseActionsEnabled = quickPauseActionsEnabled,
                unlockedPausedUntilEpochMillis = unlockedPausedUntilEpochMillis,
                unlockedPausedIndefinitely = unlockedPausedIndefinitely
            )
        )
    }

    private fun parseTimeSafely(text: String): LocalTime? {
        val parts = text.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val min = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || min !in 0..59) return null
        return LocalTime.of(hour, min)
    }

    companion object {
        fun from(settings: AndroidVocabularyReminderSettings): AndroidVocabularyReminderDraft {
            val isSeconds = (settings.intervalMillis % 60_000L) != 0L || settings.intervalMillis < 60_000L
            val unit = if (isSeconds) AndroidVocabularyReminderIntervalUnit.SECONDS else AndroidVocabularyReminderIntervalUnit.MINUTES
            val value = if (isSeconds) (settings.intervalMillis / 1000L).toString() else (settings.intervalMillis / 60_000L).toString()

            val startStr = String.format("%02d:%02d", settings.activeStart.hour, settings.activeStart.minute)
            val endStr = String.format("%02d:%02d", settings.activeEnd.hour, settings.activeEnd.minute)
            val displaySecStr = String.format(java.util.Locale.US, "%.1f", settings.displayDurationMillis / 1000.0)

            return AndroidVocabularyReminderDraft(
                enabled = settings.enabled,
                selectedPackageId = settings.selectedPackageId,
                selectionMode = settings.selectionMode,
                intervalValueText = value,
                intervalUnit = unit,
                activeStartText = startStr,
                activeEndText = endStr,
                displayDurationText = displaySecStr,
                autoPlayPronunciation = settings.autoPlayPronunciation,
                overlayPopupEnabled = settings.overlayPopupEnabled,
                quickPauseActionsEnabled = settings.quickPauseActionsEnabled
            )
        }
    }
}

sealed interface AndroidVocabularyReminderDraftValidation {
    data class Valid(val settings: AndroidVocabularyReminderSettings) : AndroidVocabularyReminderDraftValidation
    data class Invalid(val message: String) : AndroidVocabularyReminderDraftValidation
}

sealed interface AndroidVocabularyReminderActionResult {
    data object Success : AndroidVocabularyReminderActionResult
    data class Failure(val message: String) : AndroidVocabularyReminderActionResult
}

enum class AndroidLockScreenVocabularyMode {
    AGAIN_HARD,
    DUE,
    NEW_UNSEEN,
    RANDOM_LEARNED,
    MARKED_DIFFICULT,
    RANDOM_ALL
}

enum class LockWallpaperWordSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
    HUGE
}

enum class LockWallpaperImageSize {
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
    MAXIMUM
}

enum class LockWallpaperVietnameseSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
    HUGE
}

enum class LockScreenPresentationPhase {
    SCREEN_ON_STABLE,
    PREPARING_WHILE_OFF,
    PREPARED_FOR_NEXT_WAKE
}

data class PreparedLockWallpaperPresentation(
    val sessionToken: Long,
    val candidate: AndroidVocabularyCandidate,
    val audioSourcePath: String?,
    val wallpaperBitmap: Bitmap? = null,
    val isAudioPlayed: AtomicBoolean = AtomicBoolean(false)
)

data class LockScreenPresentationBuffer(
    val visible: PreparedLockWallpaperPresentation? = null,
    val nextReady: PreparedLockWallpaperPresentation? = null
) {
    fun withNextReady(ready: PreparedLockWallpaperPresentation): LockScreenPresentationBuffer =
        copy(nextReady = ready)

    fun activateNext(): LockScreenPresentationBuffer =
        copy(visible = nextReady, nextReady = null)
}

data class AndroidLockScreenVocabularySettings(
    val enabled: Boolean = false,
    val selectedPackageId: String? = null,
    val selectionMode: AndroidLockScreenVocabularyMode = AndroidLockScreenVocabularyMode.AGAIN_HARD,
    val autoPlayPronunciation: Boolean = false,
    val customBackgroundPath: String? = null,
    val wordSize: LockWallpaperWordSize = LockWallpaperWordSize.EXTRA_LARGE,
    val vietnameseSize: LockWallpaperVietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
    val imageSize: LockWallpaperImageSize = LockWallpaperImageSize.EXTRA_LARGE,
    val cardBackgroundOpacity: Float = 0.72f,
    val quickReviewIntervalMillis: Long = DEFAULT_QUICK_REVIEW_INTERVAL_MILLIS,
    val screenOffPreparationEnabled: Boolean = true,
    val screenOffPrepareDelayMillis: Long = DEFAULT_SCREEN_OFF_PREPARE_DELAY_MILLIS
) {
    val clampedCardBackgroundOpacity: Float
        get() = cardBackgroundOpacity.coerceIn(0.20f, 1.00f)

    companion object {
        const val DEFAULT_QUICK_REVIEW_INTERVAL_MILLIS = 5_000L
        const val DEFAULT_SCREEN_OFF_PREPARE_DELAY_MILLIS = 0L
    }
}

data class AndroidLockScreenVocabularyDraft(
    val enabled: Boolean,
    val selectedPackageId: String?,
    val selectionMode: AndroidLockScreenVocabularyMode,
    val autoPlayPronunciation: Boolean,
    val customBackgroundPath: String? = null,
    val wordSize: LockWallpaperWordSize = LockWallpaperWordSize.EXTRA_LARGE,
    val vietnameseSize: LockWallpaperVietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
    val imageSize: LockWallpaperImageSize = LockWallpaperImageSize.EXTRA_LARGE,
    val cardBackgroundOpacity: Float = 0.72f,
    val quickReviewIntervalMillis: Long = AndroidLockScreenVocabularySettings.DEFAULT_QUICK_REVIEW_INTERVAL_MILLIS,
    val screenOffPreparationEnabled: Boolean = true,
    val screenOffPrepareDelayMillis: Long = AndroidLockScreenVocabularySettings.DEFAULT_SCREEN_OFF_PREPARE_DELAY_MILLIS
) {
    fun toSettings(): AndroidLockScreenVocabularySettings {
        return AndroidLockScreenVocabularySettings(
            enabled = enabled,
            selectedPackageId = selectedPackageId,
            selectionMode = selectionMode,
            autoPlayPronunciation = autoPlayPronunciation,
            customBackgroundPath = customBackgroundPath,
            wordSize = wordSize,
            vietnameseSize = vietnameseSize,
            imageSize = imageSize,
            cardBackgroundOpacity = cardBackgroundOpacity.coerceIn(0.20f, 1.00f),
            quickReviewIntervalMillis = quickReviewIntervalMillis,
            screenOffPreparationEnabled = screenOffPreparationEnabled,
            screenOffPrepareDelayMillis = screenOffPrepareDelayMillis
        )
    }

    companion object {
        fun from(settings: AndroidLockScreenVocabularySettings): AndroidLockScreenVocabularyDraft {
            return AndroidLockScreenVocabularyDraft(
                enabled = settings.enabled,
                selectedPackageId = settings.selectedPackageId,
                selectionMode = settings.selectionMode,
                autoPlayPronunciation = settings.autoPlayPronunciation,
                customBackgroundPath = settings.customBackgroundPath,
                wordSize = settings.wordSize,
                vietnameseSize = settings.vietnameseSize,
                imageSize = settings.imageSize,
                cardBackgroundOpacity = settings.cardBackgroundOpacity,
                quickReviewIntervalMillis = settings.quickReviewIntervalMillis,
                screenOffPreparationEnabled = settings.screenOffPreparationEnabled,
                screenOffPrepareDelayMillis = settings.screenOffPrepareDelayMillis
            )
        }
    }
}
