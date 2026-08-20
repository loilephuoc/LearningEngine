package vn.loi.learning.android.reminder

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

data class AndroidHomeVocabularyWidgetSettings(
    val autoNextEnabled: Boolean = true,
    val intervalMillis: Long = 60_000L,
    val selectedPackageId: String? = null,
    val selectionMode: AndroidVocabularyReminderSelectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
    val wordSize: LockWallpaperWordSize = LockWallpaperWordSize.LARGE,
    val vietnameseSize: LockWallpaperVietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
    val imageSize: LockWallpaperImageSize = LockWallpaperImageSize.LARGE,
    val cardBackgroundOpacity: Float = 0.92f,
    val updateOnlyScreenOn: Boolean = true,
    val currentCandidateId: String? = null,
    val autoAudioEnabled: Boolean = false
) {
    val clampedCardBackgroundOpacity: Float
        get() = cardBackgroundOpacity.coerceIn(0.20f, 1.0f)

    val clampedIntervalMillis: Long
        get() = intervalMillis.coerceIn(MIN_INTERVAL_MILLIS, MAX_INTERVAL_MILLIS)

    fun hasVisualOrScheduleChanges(other: AndroidHomeVocabularyWidgetSettings): Boolean {
        return autoNextEnabled != other.autoNextEnabled ||
            intervalMillis != other.intervalMillis ||
            selectedPackageId != other.selectedPackageId ||
            selectionMode != other.selectionMode ||
            wordSize != other.wordSize ||
            vietnameseSize != other.vietnameseSize ||
            imageSize != other.imageSize ||
            cardBackgroundOpacity != other.cardBackgroundOpacity ||
            updateOnlyScreenOn != other.updateOnlyScreenOn ||
            autoAudioEnabled != other.autoAudioEnabled
    }

    companion object {
        const val MIN_INTERVAL_MILLIS = 2_000L // 2 seconds
        const val MAX_INTERVAL_MILLIS = 86_400_000L // 24 hours (1440 minutes)
    }
}

data class AndroidHomeVocabularyWidgetDraft(
    val autoNextEnabled: Boolean = true,
    val intervalValueText: String = "1",
    val intervalUnit: AndroidVocabularyReminderIntervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES,
    val selectedPackageId: String? = null,
    val selectionMode: AndroidVocabularyReminderSelectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
    val wordSize: LockWallpaperWordSize = LockWallpaperWordSize.LARGE,
    val vietnameseSize: LockWallpaperVietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
    val imageSize: LockWallpaperImageSize = LockWallpaperImageSize.LARGE,
    val cardBackgroundOpacity: Float = 0.92f,
    val updateOnlyScreenOn: Boolean = true,
    val autoAudioEnabled: Boolean = false
) {
    val calculatedIntervalMillis: Long
        get() {
            val num = intervalValueText.trim().toLongOrNull()
            return when (intervalUnit) {
                AndroidVocabularyReminderIntervalUnit.SECONDS -> {
                    val sec = (num ?: 30L).coerceIn(2L, 3599L)
                    sec * 1000L
                }
                AndroidVocabularyReminderIntervalUnit.MINUTES -> {
                    val min = (num ?: 1L).coerceIn(1L, 1440L)
                    min * 60_000L
                }
            }
        }

    val intervalValidationMessage: String?
        get() {
            val num = intervalValueText.trim().toLongOrNull()
            if (num == null) return "Please enter a valid number"
            return when (intervalUnit) {
                AndroidVocabularyReminderIntervalUnit.SECONDS -> {
                    if (num < 2) "Minimum interval is 2 seconds"
                    else if (num > 3599) "Maximum interval is 3599 seconds"
                    else null
                }
                AndroidVocabularyReminderIntervalUnit.MINUTES -> {
                    if (num < 1) "Minimum interval is 1 minute"
                    else if (num > 1440) "Maximum interval is 1440 minutes"
                    else null
                }
            }
        }

    fun toSettings(currentCandidateId: String? = null): AndroidHomeVocabularyWidgetSettings {
        return AndroidHomeVocabularyWidgetSettings(
            autoNextEnabled = autoNextEnabled,
            intervalMillis = calculatedIntervalMillis,
            selectedPackageId = selectedPackageId,
            selectionMode = selectionMode,
            wordSize = wordSize,
            vietnameseSize = vietnameseSize,
            imageSize = imageSize,
            cardBackgroundOpacity = cardBackgroundOpacity.coerceIn(0.20f, 1.0f),
            updateOnlyScreenOn = updateOnlyScreenOn,
            currentCandidateId = currentCandidateId,
            autoAudioEnabled = autoAudioEnabled
        )
    }

    companion object {
        fun from(settings: AndroidHomeVocabularyWidgetSettings): AndroidHomeVocabularyWidgetDraft {
            val ms = settings.clampedIntervalMillis
            val isSeconds = ms % 60_000L != 0L || ms < 60_000L
            val unit = if (isSeconds) AndroidVocabularyReminderIntervalUnit.SECONDS else AndroidVocabularyReminderIntervalUnit.MINUTES
            val textVal = if (isSeconds) (ms / 1000L).toString() else (ms / 60_000L).toString()

            return AndroidHomeVocabularyWidgetDraft(
                autoNextEnabled = settings.autoNextEnabled,
                intervalValueText = textVal,
                intervalUnit = unit,
                selectedPackageId = settings.selectedPackageId,
                selectionMode = settings.selectionMode,
                wordSize = settings.wordSize,
                vietnameseSize = settings.vietnameseSize,
                imageSize = settings.imageSize,
                cardBackgroundOpacity = settings.clampedCardBackgroundOpacity,
                updateOnlyScreenOn = settings.updateOnlyScreenOn,
                autoAudioEnabled = settings.autoAudioEnabled
            )
        }
    }
}
