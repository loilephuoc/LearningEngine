package vn.loi.learning.desktop.notification

import java.time.LocalTime
import vn.loi.learning.application.contentpackaging.InstalledPackageItem
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.domain.library.model.InstalledPackageId

data class DesktopVocabularyReminderConfigurationState(
    val settings: DesktopVocabularyReminderSettings,
    val packages: List<InstalledPackageItem>
)

sealed interface DesktopVocabularyReminderActionResult {
    data object Success : DesktopVocabularyReminderActionResult
    data class Failure(val message: String) : DesktopVocabularyReminderActionResult
}

data class DesktopVocabularyReminderDraft(
    val enabled: Boolean,
    val selectedPackageId: InstalledPackageId?,
    val selectionMode: DesktopVocabularyReminderSelectionMode,
    val intervalText: String,
    val activeStartText: String,
    val activeEndText: String,
    val displayDurationText: String,
    val autoPlayPronunciation: Boolean
) {
    fun validate(pausedUntil: java.time.Instant?): DesktopVocabularyReminderDraftValidation {
        val interval = intervalText.trim().toIntOrNull()
            ?.takeIf { it in DesktopVocabularyReminderSettings.MIN_INTERVAL_MINUTES..
                DesktopVocabularyReminderSettings.MAX_INTERVAL_MINUTES }
            ?: return DesktopVocabularyReminderDraftValidation.Invalid("Interval must be a whole number greater than zero.")
        val durationSeconds = displayDurationText.trim().toBigDecimalOrNull()
            ?: return DesktopVocabularyReminderDraftValidation.Invalid("Popup duration must be a number from 1.5 to 60 seconds.")
        val durationMillis = runCatching { durationSeconds.movePointRight(3).longValueExact() }.getOrNull()
            ?.takeIf { it in DesktopVocabularyReminderSettings.MIN_DISPLAY_DURATION_MILLIS..
                DesktopVocabularyReminderSettings.MAX_DISPLAY_DURATION_MILLIS }
            ?: return DesktopVocabularyReminderDraftValidation.Invalid("Popup duration must be from 1.5 to 60 seconds.")
        val start = runCatching { LocalTime.parse(activeStartText.trim()) }.getOrNull()
            ?: return DesktopVocabularyReminderDraftValidation.Invalid("Active from must use HH:mm.")
        val end = runCatching { LocalTime.parse(activeEndText.trim()) }.getOrNull()
            ?: return DesktopVocabularyReminderDraftValidation.Invalid("Active until must use HH:mm.")
        return DesktopVocabularyReminderDraftValidation.Valid(
            DesktopVocabularyReminderSettings(
                enabled = enabled,
                selectedPackageId = selectedPackageId,
                selectionMode = selectionMode,
                intervalMinutes = interval,
                activeStart = start,
                activeEnd = end,
                displayDurationMillis = durationMillis,
                autoPlayPronunciation = autoPlayPronunciation,
                pausedUntil = pausedUntil
            )
        )
    }

    companion object {
        fun from(settings: DesktopVocabularyReminderSettings) = DesktopVocabularyReminderDraft(
            enabled = settings.enabled,
            selectedPackageId = settings.selectedPackageId,
            selectionMode = settings.selectionMode,
            intervalText = settings.intervalMinutes.toString(),
            activeStartText = settings.activeStart.toString(),
            activeEndText = settings.activeEnd.toString(),
            displayDurationText = java.math.BigDecimal.valueOf(settings.displayDurationMillis, 3).stripTrailingZeros().toPlainString(),
            autoPlayPronunciation = settings.autoPlayPronunciation
        )
    }
}

sealed interface DesktopVocabularyReminderDraftValidation {
    data class Valid(val settings: DesktopVocabularyReminderSettings) : DesktopVocabularyReminderDraftValidation
    data class Invalid(val message: String) : DesktopVocabularyReminderDraftValidation
}

class DesktopVocabularyReminderSettingsController(
    private val runtime: DesktopVocabularyReminderRuntime,
    private val previewSelector: DesktopVocabularyReminderSelectionSource,
    private val popupController: DesktopVocabularyReminderPopupController,
    private val installedPackages: InstalledPackageQueryService
) {
    fun load() = DesktopVocabularyReminderConfigurationState(runtime.settings, installedPackages.query())

    fun apply(draft: DesktopVocabularyReminderDraft): DesktopVocabularyReminderActionResult {
        val validated = draft.validate(runtime.settings.pausedUntil)
        if (validated is DesktopVocabularyReminderDraftValidation.Invalid) return validated.failure()
        val settings = (validated as DesktopVocabularyReminderDraftValidation.Valid).settings
        if (settings.selectedPackageId != null && installedPackages.findById(settings.selectedPackageId.value) == null) {
            return DesktopVocabularyReminderActionResult.Failure("Selected package is unavailable.")
        }
        return if (runtime.updateSettings(settings)) DesktopVocabularyReminderActionResult.Success
        else DesktopVocabularyReminderActionResult.Failure("Could not save reminder settings.")
    }

    fun setEnabled(enabled: Boolean) =
        if (runtime.updateSettings(runtime.settings.copy(enabled = enabled))) DesktopVocabularyReminderActionResult.Success
        else DesktopVocabularyReminderActionResult.Failure("Could not save reminder settings.")

    fun preview(draft: DesktopVocabularyReminderDraft): DesktopVocabularyReminderActionResult {
        val validated = draft.validate(runtime.settings.pausedUntil)
        if (validated is DesktopVocabularyReminderDraftValidation.Invalid) return validated.failure()
        val settings = (validated as DesktopVocabularyReminderDraftValidation.Valid).settings.copy(enabled = true, pausedUntil = null)
        if (popupController.isReminderActive) return DesktopVocabularyReminderActionResult.Failure("A reminder popup is already visible.")
        return when (val result = previewSelector.select(settings)) {
            is DesktopVocabularyCandidateSelectionResult.Selected -> {
                popupController.dispatch(result.candidate, settings.displayDurationMillis, settings.autoPlayPronunciation)
                DesktopVocabularyReminderActionResult.Success
            }
            is DesktopVocabularyCandidateSelectionResult.NoCandidate ->
                DesktopVocabularyReminderActionResult.Failure(result.reason.message())
        }
    }

    fun pause30Minutes() = action(runtime.pauseFor30Minutes())
    fun pauseOneHour() = action(runtime.pauseForOneHour())
    fun pauseToday() = action(runtime.pauseToday())
    fun resumeNow() = action(runtime.resumeNow())

    private fun action(success: Boolean) = if (success) DesktopVocabularyReminderActionResult.Success
        else DesktopVocabularyReminderActionResult.Failure("Could not save reminder settings.")
    private fun DesktopVocabularyReminderDraftValidation.Invalid.failure() = DesktopVocabularyReminderActionResult.Failure(message)
    private fun DesktopVocabularyCandidateSelectionResult.Reason.message() = when (this) {
        DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_NOT_SELECTED -> "Select a package first."
        DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE -> "Selected package is unavailable."
        DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_EMPTY -> "Selected package has no content."
        DesktopVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE -> "No eligible vocabulary candidate was found."
        DesktopVocabularyCandidateSelectionResult.Reason.DISABLED -> "Reminder preview is unavailable."
    }
}
