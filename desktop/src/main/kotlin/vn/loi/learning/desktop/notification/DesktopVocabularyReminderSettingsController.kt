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
    val intervalValueText: String,
    val intervalUnit: DesktopVocabularyReminderIntervalUnit = DesktopVocabularyReminderIntervalUnit.MINUTES,
    val activeStartText: String,
    val activeEndText: String,
    val displayDurationText: String,
    val autoPlayPronunciation: Boolean,
    val popupLocation: DesktopVocabularyReminderPopupLocation = DesktopVocabularyReminderPopupLocation()
) {
    val intervalText: String get() = intervalValueText

    constructor(
        enabled: Boolean,
        selectedPackageId: InstalledPackageId?,
        selectionMode: DesktopVocabularyReminderSelectionMode,
        intervalText: String,
        activeStartText: String,
        activeEndText: String,
        displayDurationText: String,
        autoPlayPronunciation: Boolean,
        popupLocation: DesktopVocabularyReminderPopupLocation = DesktopVocabularyReminderPopupLocation()
    ) : this(
        enabled = enabled,
        selectedPackageId = selectedPackageId,
        selectionMode = selectionMode,
        intervalValueText = intervalText,
        intervalUnit = DesktopVocabularyReminderIntervalUnit.MINUTES,
        activeStartText = activeStartText,
        activeEndText = activeEndText,
        displayDurationText = displayDurationText,
        autoPlayPronunciation = autoPlayPronunciation,
        popupLocation = popupLocation
    )

    fun validate(pausedUntil: java.time.Instant?): DesktopVocabularyReminderDraftValidation {
        val intervalMillis = when (intervalUnit) {
            DesktopVocabularyReminderIntervalUnit.SECONDS -> {
                val seconds = intervalValueText.trim().toLongOrNull()
                    ?: return DesktopVocabularyReminderDraftValidation.Invalid("Interval must be a valid number of seconds (at least 5).")
                val millis = seconds * 1_000L
                if (millis !in DesktopVocabularyReminderSettings.MIN_INTERVAL_MILLIS..DesktopVocabularyReminderSettings.MAX_INTERVAL_MILLIS) {
                    return DesktopVocabularyReminderDraftValidation.Invalid("Interval must be at least 5 seconds.")
                }
                millis
            }
            DesktopVocabularyReminderIntervalUnit.MINUTES -> {
                val minutes = intervalValueText.trim().toLongOrNull()
                    ?: return DesktopVocabularyReminderDraftValidation.Invalid("Interval must be a whole number of minutes (at least 1).")
                val millis = minutes * 60_000L
                if (millis !in DesktopVocabularyReminderSettings.MIN_INTERVAL_MILLIS..DesktopVocabularyReminderSettings.MAX_INTERVAL_MILLIS) {
                    return DesktopVocabularyReminderDraftValidation.Invalid("Interval must be at least 1 minute.")
                }
                millis
            }
        }
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
                intervalMillis = intervalMillis,
                activeStart = start,
                activeEnd = end,
                displayDurationMillis = durationMillis,
                autoPlayPronunciation = autoPlayPronunciation,
                pausedUntil = pausedUntil,
                popupLocation = popupLocation
            )
        )
    }

    companion object {
        fun from(settings: DesktopVocabularyReminderSettings): DesktopVocabularyReminderDraft {
            val isSeconds = settings.intervalMillis < 60_000L || (settings.intervalMillis % 60_000L != 0L)
            val unit = if (isSeconds) DesktopVocabularyReminderIntervalUnit.SECONDS else DesktopVocabularyReminderIntervalUnit.MINUTES
            val value = if (unit == DesktopVocabularyReminderIntervalUnit.SECONDS) {
                (settings.intervalMillis / 1_000L).toString()
            } else {
                (settings.intervalMillis / 60_000L).toString()
            }
            return DesktopVocabularyReminderDraft(
                enabled = settings.enabled,
                selectedPackageId = settings.selectedPackageId,
                selectionMode = settings.selectionMode,
                intervalValueText = value,
                intervalUnit = unit,
                activeStartText = settings.activeStart.toString(),
                activeEndText = settings.activeEnd.toString(),
                displayDurationText = java.math.BigDecimal.valueOf(settings.displayDurationMillis, 3).stripTrailingZeros().toPlainString(),
                autoPlayPronunciation = settings.autoPlayPronunciation,
                popupLocation = settings.popupLocation
            )
        }
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
                popupController.dispatch(
                    result.candidate,
                    settings.displayDurationMillis,
                    settings.autoPlayPronunciation,
                    settings.popupLocation
                )
                DesktopVocabularyReminderActionResult.Success
            }
            is DesktopVocabularyCandidateSelectionResult.NoCandidate ->
                DesktopVocabularyReminderActionResult.Failure(result.reason.message())
        }
    }

    fun updatePopupLocation(location: DesktopVocabularyReminderPopupLocation): Boolean =
        runtime.updateSettings(runtime.settings.copy(popupLocation = location))

    fun resetPopupPosition(): Boolean =
        runtime.updateSettings(
            runtime.settings.copy(
                popupLocation = runtime.settings.popupLocation.copy(
                    customPosition = false,
                    normalizedX = null,
                    normalizedY = null
                )
            )
        )

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
