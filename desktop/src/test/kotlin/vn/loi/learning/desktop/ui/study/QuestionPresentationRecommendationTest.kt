package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceSelectionReason
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

class QuestionPresentationRecommendationTest {
    @Test
    fun `Listening Recall recommends audio without identity meaning or examples`() {
        val recommendation = resolve(LearningExperienceKind.LISTENING_RECALL)

        assertFalse(recommendation.showPrimaryEnglish)
        assertTrue(recommendation.allowPrimaryEnglishAudio)
        assertTrue(recommendation.autoplayPrimaryEnglish)
        assertFalse(recommendation.showVietnameseMeaning)
        assertFalse(recommendation.showEnglishExamples)
        assertFalse(recommendation.showVietnameseExamples)
    }

    @Test
    fun `Image Recall recommends only its projected non-answer image`() {
        val recommendation = resolve(LearningExperienceKind.IMAGE_RECALL)

        assertFalse(recommendation.showPrimaryEnglish)
        assertFalse(recommendation.allowPrimaryEnglishAudio)
        assertFalse(recommendation.showVietnameseMeaning)
        assertFalse(recommendation.showEnglishExamples)
        assertFalse(recommendation.showVietnameseExamples)
    }

    @Test
    fun `Prompt derives Vietnamese cue visibility without required autoplay`() {
        val available = resolve(LearningExperienceKind.PROMPT_RECALL, hasMeaning = true)

        assertTrue(available.showVietnameseMeaning)
        assertFalse(available.autoplayVietnameseMeaning)
        assertFalse(available.requireVietnameseMeaning)
        assertFalse(
            resolve(LearningExperienceKind.PROMPT_RECALL, hasMeaning = false)
                .showVietnameseMeaning
        )
    }

    @Test
    fun `Typing requires visible Vietnamese cue and one-shot autoplay when available`() {
        val available = resolve(LearningExperienceKind.TYPING_RECALL, hasMeaning = true)
        val missing = resolve(LearningExperienceKind.TYPING_RECALL, hasMeaning = false)

        assertTrue(available.showVietnameseMeaning)
        assertTrue(available.autoplayVietnameseMeaning)
        assertTrue(available.requireVietnameseMeaning)
        assertFalse(missing.showVietnameseMeaning)
        assertFalse(missing.autoplayVietnameseMeaning)
        assertFalse(missing.requireVietnameseMeaning)
    }

    @Test
    fun `missing or inconsistent plan fails closed instead of using show-all baseline`() {
        val missing = QuestionPresentationRecommendationResolver.resolve(null, null)
        val plan = plan(LearningExperienceKind.IMAGE_RECALL)
        val inconsistent = QuestionPresentationRecommendationResolver.resolve(
            plan,
            selection(LearningExperienceKind.LISTENING_RECALL)
        )

        listOf(missing, inconsistent).forEach { recommendation ->
            assertFalse(recommendation.showPrimaryEnglish)
            assertFalse(recommendation.allowPrimaryEnglishAudio)
            assertFalse(recommendation.showVietnameseMeaning)
            assertFalse(recommendation.showEnglishExamples)
            assertFalse(recommendation.showVietnameseExamples)
        }
    }

    private fun resolve(
        kind: LearningExperienceKind,
        hasMeaning: Boolean = true
    ): StudyPresentationRecommendation =
        QuestionPresentationRecommendationResolver.resolve(
            plan(kind, hasMeaning),
            selection(kind)
        )

    private fun plan(
        kind: LearningExperienceKind,
        hasMeaning: Boolean = true
    ) = LearningExperiencePlan(
        options = LearningExperienceOptions.from(listOf(kind)),
        capabilities = LearningExperienceCapabilities(
            hasPromptText = true,
            hasPromptImage = kind == LearningExperienceKind.IMAGE_RECALL,
            hasPromptAudio = kind == LearningExperienceKind.LISTENING_RECALL,
            hasMeaning = hasMeaning,
            hasExample = true,
            hasAnswerAudio = true,
            hasExampleAudio = true
        ),
        context = LearningExperienceContext(answerRevealed = false),
        visibleSupportingRoles = emptySet(),
        typingPrompt =
            if (kind == LearningExperienceKind.TYPING_RECALL) {
                TypingRecallPrompt("answer")
            } else {
                null
            }
    )

    private fun selection(kind: LearningExperienceKind) =
        ExperienceSelectionResult(
            selectedKind = kind,
            availableKinds = listOf(kind),
            selectedIndex = 0,
            reason = ExperienceSelectionReason.ROUND_ROBIN
        )
}
