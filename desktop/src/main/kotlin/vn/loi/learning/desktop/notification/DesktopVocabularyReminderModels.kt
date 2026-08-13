package vn.loi.learning.desktop.notification

import java.time.Instant
import java.time.LocalTime
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

enum class DesktopVocabularyReminderSelectionMode {
    AGAIN_HARD,
    DUE,
    RANDOM_LEARNED,
    RANDOM_ALL,
    MARKED_DIFFICULT
}

data class DesktopVocabularyReminderSettings(
    val enabled: Boolean = false,
    val selectedPackageId: InstalledPackageId? = null,
    val selectionMode: DesktopVocabularyReminderSelectionMode =
        DesktopVocabularyReminderSelectionMode.AGAIN_HARD,
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    val activeStart: LocalTime = DEFAULT_ACTIVE_START,
    val activeEnd: LocalTime = DEFAULT_ACTIVE_END,
    val displayDurationMillis: Long = DEFAULT_DISPLAY_DURATION_MILLIS,
    val autoPlayPronunciation: Boolean = false,
    val pausedUntil: Instant? = null
) {
    init {
        require(intervalMinutes in MIN_INTERVAL_MINUTES..MAX_INTERVAL_MINUTES) {
            "Reminder interval must be between $MIN_INTERVAL_MINUTES and $MAX_INTERVAL_MINUTES minutes."
        }
        require(displayDurationMillis in MIN_DISPLAY_DURATION_MILLIS..MAX_DISPLAY_DURATION_MILLIS) {
            "Reminder display duration must be between $MIN_DISPLAY_DURATION_MILLIS and $MAX_DISPLAY_DURATION_MILLIS milliseconds."
        }
    }

    companion object {
        const val MIN_INTERVAL_MINUTES = 1
        const val MAX_INTERVAL_MINUTES = 525_600
        const val DEFAULT_INTERVAL_MINUTES = 15
        val DEFAULT_ACTIVE_START: LocalTime = LocalTime.of(8, 0)
        val DEFAULT_ACTIVE_END: LocalTime = LocalTime.of(22, 0)
        const val DEFAULT_DISPLAY_DURATION_MILLIS = 8_000L
        const val MIN_DISPLAY_DURATION_MILLIS = 1_500L
        const val MAX_DISPLAY_DURATION_MILLIS = 60_000L
    }
}

/** Transient, read-only projection for a later notification presentation layer. */
data class DesktopVocabularyCandidate(
    val contentId: ContentId,
    val installedPackageId: InstalledPackageId,
    val packageDisplayName: String,
    val primaryText: String,
    val answer: String?,
    val translation: String?,
    val ipa: String?,
    val partOfSpeech: String?,
    val imageReference: String?,
    val primaryAudioReference: String?,
    val lesson: String?,
    val section: String?
)

sealed interface DesktopVocabularyCandidateSelectionResult {
    data class Selected(val candidate: DesktopVocabularyCandidate) :
        DesktopVocabularyCandidateSelectionResult

    data class NoCandidate(val reason: Reason) : DesktopVocabularyCandidateSelectionResult

    enum class Reason {
        DISABLED,
        PACKAGE_NOT_SELECTED,
        PACKAGE_UNAVAILABLE,
        PACKAGE_EMPTY,
        NO_ELIGIBLE_CANDIDATE
    }
}
