package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan

object QuestionPresentationRecommendationResolver {
    fun resolve(
        plan: LearningExperiencePlan?,
        selection: ExperienceSelectionResult?
    ): StudyPresentationRecommendation {
        val kind = selection?.selectedKind
            ?.takeIf { selected -> plan != null && selected in plan.options.orderedKinds }
        val hasMeaning = plan?.capabilities?.hasMeaning == true
        return when (kind) {
            LearningExperienceKind.LISTENING_RECALL ->
                recommendation(
                    showPrimaryEnglish = false,
                    allowPrimaryEnglishAudio = true,
                    autoplayPrimaryEnglish = true
                )

            LearningExperienceKind.PROMPT_RECALL,
            LearningExperienceKind.TYPING_RECALL ->
                recommendation(showVietnameseMeaning = hasMeaning)

            LearningExperienceKind.IMAGE_RECALL,
            null -> recommendation()
        }
    }

    private fun recommendation(
        showPrimaryEnglish: Boolean = false,
        allowPrimaryEnglishAudio: Boolean = false,
        showVietnameseMeaning: Boolean = false,
        autoplayPrimaryEnglish: Boolean = false
    ) = StudyPresentationRecommendation(
        showPrimaryEnglish = showPrimaryEnglish,
        allowPrimaryEnglishAudio = allowPrimaryEnglishAudio,
        showVietnameseMeaning = showVietnameseMeaning,
        showEnglishExamples = false,
        showVietnameseExamples = false,
        autoplayPrimaryEnglish = autoplayPrimaryEnglish
    )
}
