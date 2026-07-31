package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyAnswerScrollTransitionTest {
    @Test
    fun `typing front does not request an answer scroll reset`() {
        assertNull(resolveTypingAnswerScrollTransitionKey("item-a", typingAnswerSideActive = false))
        assertNull(resolveTypingAnswerScrollTransitionKey(null, typingAnswerSideActive = true))
    }

    @Test
    fun `typing answer side requests one item scoped reset identity`() {
        val first = resolveTypingAnswerScrollTransitionKey("item-a", typingAnswerSideActive = true)
        val recomposition = resolveTypingAnswerScrollTransitionKey("item-a", typingAnswerSideActive = true)

        assertEquals(StudyAnswerScrollTransitionKey("item-a"), first)
        assertEquals(first, recomposition)
    }

    @Test
    fun `new item has a distinct reset identity and its front cancels the old answer effect`() {
        val itemA = resolveTypingAnswerScrollTransitionKey("item-a", typingAnswerSideActive = true)
        val itemBFront = resolveTypingAnswerScrollTransitionKey("item-b", typingAnswerSideActive = false)
        val itemBAnswer = resolveTypingAnswerScrollTransitionKey("item-b", typingAnswerSideActive = true)

        assertNull(itemBFront)
        assertNotEquals(itemA, itemBAnswer)
    }

    @Test
    fun `screen waits for answer composition then immediately scrolls shared body to top`() {
        val source = studySource("StudyScreen.kt")
        val effectStart = source.indexOf("LaunchedEffect(answerScrollTransitionKey)")
        val effectEnd = source.indexOf("LaunchedEffect(\n        uiState.experienceRotationContext", effectStart)
        val effect = source.substring(effectStart, effectEnd)

        assertTrue(source.contains("val mainBodyScrollState = rememberScrollState()"))
        assertTrue(source.contains(".verticalScroll(mainBodyScrollState)"))
        assertTrue(effect.contains("withFrameNanos { }"))
        assertTrue(effect.contains("mainBodyScrollState.scrollTo(0)"))
        assertTrue(effect.indexOf("withFrameNanos { }") < effect.indexOf("scrollTo(0)"))
        assertFalse(effect.contains("animateScrollTo"))
        assertFalse(effect.contains("delay("))
    }

    @Test
    fun `reset keys exclude mutable answer side presentation state`() {
        val source = studySource("StudyScreen.kt")
        val keyStart = source.indexOf("val answerScrollTransitionKey =")
        val keyEnd = source.indexOf("val typingSuccessDecision", keyStart)
        val key = source.substring(keyStart, keyEnd)

        assertTrue(key.contains("uiState.currentLearningItemId"))
        assertTrue(key.contains("learningScene is TypingScene && uiState.canReview"))
        listOf("typingElapsedMillis", "audio", "schedulerFeedback", "confidence", "heightMode")
            .forEach { excluded -> assertFalse(key.contains(excluded), excluded) }
    }

    @Test
    fun `answer comparison remains semantically before scheduler feedback without a nested scroll anchor`() {
        val source = studySource("FocusedAnswerSurface.kt")
        val surfaceStart = source.indexOf("fun FocusedAnswerSurface(")
        val surfaceEnd = source.indexOf("private fun ResponsiveAnswerSupportingRegion(", surfaceStart)
        val surface = source.substring(surfaceStart, surfaceEnd)

        assertTrue(surface.indexOf("typingComparisonForCanonicalWord") < surface.indexOf("schedulerFeedback?.let"))
        assertFalse(surface.contains("verticalScroll("))
        assertFalse(surface.contains("BringIntoViewRequester"))
    }

    private fun studySource(name: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
}
