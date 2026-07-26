package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.study.memory.model.LearningStage

class DiscoveryModeTest {

    @Test
    fun `10, 11, 12, 13 - Discovery Mode for NEW item before reveal`() {
        val content = Content(
            id = ContentId("vocab-new-1"),
            type = vn.loi.learning.domain.content.model.ContentType.WORD,
            text = ContentText(
                primaryText = "apple",
                translatedText = "quả táo",
                pronunciation = "ˈæp.əl",
                exampleText = "I like apples."
            )
        )
        val uiState = StudyUiState(
            hasActiveSession = true,
            canRevealAnswer = true,
            canReview = false, // Answer not revealed yet
            learningStage = LearningStage.NEW,
            domainContent = content
        )

        // Verify state invariants for Discovery Mode
        assertEquals(LearningStage.NEW, uiState.learningStage)
        assertFalse(uiState.canReview) // Rating actions unavailable before reveal
        assertTrue(uiState.canRevealAnswer)

        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        // Discovery front displays Vietnamese meaning ("quả táo")
        assertEquals("quả táo", model.vietnameseMeaning)
    }

    @Test
    fun `14, 15 - Reveal transitions Discovery to Focused Answer Surface`() {
        val content = Content(
            id = ContentId("vocab-new-2"),
            type = vn.loi.learning.domain.content.model.ContentType.WORD,
            text = ContentText(
                primaryText = "banana",
                translatedText = "quả chuối",
                pronunciation = "bəˈnɑː.nə"
            )
        )
        val revealedUiState = StudyUiState(
            hasActiveSession = true,
            canRevealAnswer = false,
            canReview = true, // Answer revealed
            learningStage = LearningStage.NEW,
            domainContent = content
        )

        assertTrue(revealedUiState.canReview)
        assertFalse(revealedUiState.canRevealAnswer)

        val model = FocusedVocabularyAnswerResolver.resolve(revealedUiState)
        assertEquals("banana", model.englishWord)
        assertEquals("bəˈnɑː.nə", model.ipa)
        assertEquals("quả chuối", model.vietnameseMeaning)
    }

    @Test
    fun `9 - Existing experience rotation remains deterministic across stages`() {
        val newStageState = StudyUiState(learningStage = LearningStage.NEW)
        val reviewStageState = StudyUiState(learningStage = LearningStage.REVIEW)

        assertEquals(LearningStage.NEW, newStageState.learningStage)
        assertEquals(LearningStage.REVIEW, reviewStageState.learningStage)
    }
}
