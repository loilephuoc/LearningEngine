package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudyContentAccessibilityTest {
    @Test
    fun `hidden answer exposes only the prompt description`() {
        val accessibility =
            resolveStudyContentAccessibility(
                StudyUiState(
                    contentText = "What is Kotlin?",
                    translationText = "A programming language",
                    canRevealAnswer = true
                )
            )

        assertEquals(
            "Study prompt. What is Kotlin?",
            accessibility.promptDescription
        )
        assertNull(accessibility.answerDescription)
    }

    @Test
    fun `revealed answer labels both prompt and answer`() {
        val accessibility =
            resolveStudyContentAccessibility(
                StudyUiState(
                    contentText = "What is Kotlin?",
                    translationText = "A programming language",
                    canReview = true
                )
            )

        assertEquals(
            "Study prompt. What is Kotlin?",
            accessibility.promptDescription
        )
        assertEquals(
            "Study answer. A programming language",
            accessibility.answerDescription
        )
    }

    @Test
    fun `blank content has deterministic accessible fallback text`() {
        val accessibility =
            resolveStudyContentAccessibility(
                StudyUiState(
                    contentText = "   ",
                    translationText = "",
                    canReview = true
                )
            )

        assertEquals(
            "Study prompt. No prompt text available",
            accessibility.promptDescription
        )
        assertEquals(
            "Study answer. No answer text available",
            accessibility.answerDescription
        )
    }
}
