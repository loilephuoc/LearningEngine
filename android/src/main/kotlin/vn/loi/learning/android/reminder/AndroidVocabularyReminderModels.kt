package vn.loi.learning.android.reminder

import java.time.Instant
import java.time.LocalTime
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

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
    val pausedUntil: Instant? = null
) {
    val intervalMinutes: Int
        get() = (intervalMillis / 60_000L).toInt().coerceAtLeast(1)

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
        NO_ELIGIBLE_CANDIDATE
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
    val overlayPopupEnabled: Boolean = false
) {
    fun validate(pausedUntil: Instant?): AndroidVocabularyReminderDraftValidation {
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
                pausedUntil = pausedUntil
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
                overlayPopupEnabled = settings.overlayPopupEnabled
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
