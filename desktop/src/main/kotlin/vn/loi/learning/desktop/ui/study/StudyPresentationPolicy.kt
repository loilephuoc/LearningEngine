package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences

data class StudyPresentationAvailability(
    val primaryEnglishAvailable: Boolean,
    val vietnameseMeaningAvailable: Boolean,
    val englishExamplesAvailable: Boolean,
    val vietnameseExamplesAvailable: Boolean,
    val primaryEnglishAudio: Path? = null,
    val vietnameseMeaningAudio: Path? = null,
    val englishExampleAudio: Path? = null,
    val vietnameseExampleAudio: Path? = null
)

data class StudyPresentationRecommendation(
    val showVietnameseMeaning: Boolean,
    val showEnglishExamples: Boolean,
    val showVietnameseExamples: Boolean,
    val autoplayPrimaryEnglish: Boolean,
    val autoplayVietnameseMeaning: Boolean = false,
    val autoplayEnglishExample: Boolean = false,
    val autoplayVietnameseExample: Boolean = false
)

data class EffectiveStudyPresentation(
    val controlMode: StudyPresentationControlMode = StudyPresentationControlMode.ADAPTIVE,
    val showPrimaryEnglish: Boolean,
    val showVietnameseMeaning: Boolean,
    val showEnglishExamples: Boolean,
    val showVietnameseExamples: Boolean,
    val autoplayPrimaryEnglish: Boolean,
    val autoplayVietnameseMeaning: Boolean,
    val autoplayEnglishExample: Boolean,
    val autoplayVietnameseExample: Boolean
) {
    companion object {
        val UNRESTRICTED =
            EffectiveStudyPresentation(
                showPrimaryEnglish = true,
                showVietnameseMeaning = true,
                showEnglishExamples = true,
                showVietnameseExamples = true,
                autoplayPrimaryEnglish = true,
                autoplayVietnameseMeaning = true,
                autoplayEnglishExample = true,
                autoplayVietnameseExample = true
            )
    }
}

object StudyPresentationPolicy {
    fun resolve(
        preferences: StudyPresentationPreferences,
        availability: StudyPresentationAvailability,
        recommendation: StudyPresentationRecommendation
    ): EffectiveStudyPresentation {
        val showVietnamese = when (preferences.controlMode) {
            StudyPresentationControlMode.ADAPTIVE -> recommendation.showVietnameseMeaning
            StudyPresentationControlMode.PREFERENCE_GUIDED,
            StudyPresentationControlMode.MANUAL -> preferences.showVietnamese
        }
        val showEnglishExamples = when (preferences.controlMode) {
            StudyPresentationControlMode.ADAPTIVE -> recommendation.showEnglishExamples
            StudyPresentationControlMode.PREFERENCE_GUIDED,
            StudyPresentationControlMode.MANUAL -> preferences.showEnglish
        }
        val showVietnameseExamples = when (preferences.controlMode) {
            StudyPresentationControlMode.ADAPTIVE -> recommendation.showVietnameseExamples
            StudyPresentationControlMode.PREFERENCE_GUIDED,
            StudyPresentationControlMode.MANUAL -> preferences.showVietnamese
        }
        val effectiveShowVietnamese =
            availability.vietnameseMeaningAvailable && showVietnamese
        val effectiveShowEnglishExamples =
            availability.englishExamplesAvailable && showEnglishExamples
        val effectiveShowVietnameseExamples =
            availability.vietnameseExamplesAvailable && showVietnameseExamples
        val userControlsVisibility =
            preferences.controlMode != StudyPresentationControlMode.ADAPTIVE
        val requestedPrimaryEnglish =
            !userControlsVisibility || preferences.showEnglish
        val effectiveShowPrimaryEnglish =
            availability.primaryEnglishAvailable && requestedPrimaryEnglish

        fun autoplay(
            engineAllows: Boolean,
            userAllows: Boolean,
            visible: Boolean,
            audio: Path?
        ): Boolean {
            val modeAllows = when (preferences.controlMode) {
                StudyPresentationControlMode.ADAPTIVE -> engineAllows
                StudyPresentationControlMode.PREFERENCE_GUIDED -> engineAllows && userAllows
                StudyPresentationControlMode.MANUAL -> userAllows
            }
            return modeAllows && visible && audio != null
        }

        return EffectiveStudyPresentation(
            controlMode = preferences.controlMode,
            showPrimaryEnglish = effectiveShowPrimaryEnglish,
            showVietnameseMeaning = effectiveShowVietnamese,
            showEnglishExamples = effectiveShowEnglishExamples,
            showVietnameseExamples = effectiveShowVietnameseExamples,
            autoplayPrimaryEnglish = autoplay(
                recommendation.autoplayPrimaryEnglish,
                preferences.autoplayEnglish,
                effectiveShowPrimaryEnglish,
                availability.primaryEnglishAudio
            ),
            autoplayVietnameseMeaning = autoplay(
                recommendation.autoplayVietnameseMeaning,
                preferences.autoplayVietnamese,
                effectiveShowVietnamese,
                availability.vietnameseMeaningAudio
            ),
            autoplayEnglishExample = autoplay(
                recommendation.autoplayEnglishExample,
                preferences.autoplayEnglish,
                effectiveShowEnglishExamples,
                availability.englishExampleAudio
            ),
            autoplayVietnameseExample = autoplay(
                recommendation.autoplayVietnameseExample,
                preferences.autoplayVietnamese,
                effectiveShowVietnameseExamples,
                availability.vietnameseExampleAudio
            )
        )
    }
}

fun FocusedVocabularyAnswerModel.presentationAvailability() =
    StudyPresentationAvailability(
        primaryEnglishAvailable = englishWord.isNotBlank(),
        vietnameseMeaningAvailable = vietnameseMeaning.isNotBlank(),
        englishExamplesAvailable = examples.any { it.englishText.isNotBlank() },
        vietnameseExamplesAvailable = examples.any { !it.vietnameseTranslation.isNullOrBlank() },
        primaryEnglishAudio = primaryAudioPath,
        vietnameseMeaningAudio = meaningAudioPath,
        englishExampleAudio = examples.firstNotNullOfOrNull { it.englishAudioPath ?: it.audioPath },
        vietnameseExampleAudio = examples.firstNotNullOfOrNull { it.vietnameseAudioPath }
    )

fun adaptiveBaseline(availability: StudyPresentationAvailability) =
    StudyPresentationRecommendation(
        showVietnameseMeaning = true,
        showEnglishExamples = true,
        showVietnameseExamples = true,
        autoplayPrimaryEnglish = true,
        autoplayVietnameseMeaning = false,
        autoplayEnglishExample = false,
        autoplayVietnameseExample = false
    )
