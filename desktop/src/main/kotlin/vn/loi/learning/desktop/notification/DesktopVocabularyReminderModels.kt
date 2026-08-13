package vn.loi.learning.desktop.notification

import java.time.Instant
import java.time.LocalTime
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

enum class DesktopVocabularyReminderSelectionMode {
    AGAIN_HARD,
    DUE,
    RANDOM_LEARNED,
    RANDOM_ALL
}

data class DesktopVocabularyReminderSettings(
    val enabled: Boolean = false,
    val selectedPackageId: InstalledPackageId? = null,
    val selectionMode: DesktopVocabularyReminderSelectionMode =
        DesktopVocabularyReminderSelectionMode.AGAIN_HARD,
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    val activeStart: LocalTime = DEFAULT_ACTIVE_START,
    val activeEnd: LocalTime = DEFAULT_ACTIVE_END,
    val displayDurationSeconds: Int = DEFAULT_DISPLAY_DURATION_SECONDS,
    val pausedUntil: Instant? = null
) {
    init {
        require(intervalMinutes in ALLOWED_INTERVAL_MINUTES) {
            "Reminder interval must be one of $ALLOWED_INTERVAL_MINUTES."
        }
        require(displayDurationSeconds in MIN_DISPLAY_DURATION_SECONDS..MAX_DISPLAY_DURATION_SECONDS) {
            "Reminder display duration must be between $MIN_DISPLAY_DURATION_SECONDS and $MAX_DISPLAY_DURATION_SECONDS seconds."
        }
    }

    companion object {
        val ALLOWED_INTERVAL_MINUTES = setOf(5, 10, 15, 30, 60)
        const val DEFAULT_INTERVAL_MINUTES = 15
        val DEFAULT_ACTIVE_START: LocalTime = LocalTime.of(8, 0)
        val DEFAULT_ACTIVE_END: LocalTime = LocalTime.of(22, 0)
        const val DEFAULT_DISPLAY_DURATION_SECONDS = 8
        const val MIN_DISPLAY_DURATION_SECONDS = 3
        const val MAX_DISPLAY_DURATION_SECONDS = 60
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
