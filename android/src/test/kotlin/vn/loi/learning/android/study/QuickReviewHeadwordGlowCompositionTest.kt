package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class QuickReviewHeadwordGlowCompositionTest {
    private val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
    private val answerSection = source(
        "vn/loi/learning/android/study/components/IntroductionAnswerSection.kt"
    )
    private val designTokens = source("vn/loi/learning/android/ui/LearningEngineDesignTokens.kt")

    @Test
    fun `glow policy requires live revealed Quick Review accepted gate`() {
        assertFalse(quickReviewHeadwordGlowActive(true, true, false, false))
        assertTrue(quickReviewHeadwordGlowActive(true, true, true, false))
        assertFalse(quickReviewHeadwordGlowActive(false, true, true, false))
        assertFalse(quickReviewHeadwordGlowActive(true, false, true, false))
        assertFalse(quickReviewHeadwordGlowActive(true, true, true, true))
    }

    @Test
    fun `Introduction wires existing pending authority to revealed English headword`() {
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("fun StudyAudioButton(")
        val answerCall = introduction.substringAfter("StudyAnswerSection(")
            .substringBefore("\n                                )")
        assertTrue(answerCall.contains("englishAnswer = state.answer"))
        assertTrue(answerCall.contains("swipeSuccessGlowActive = quickReviewHeadwordGlowActive("))
        assertTrue(answerCall.contains("transitionPending = quickReviewTransitionPending"))
        assertTrue(answerCall.contains("historyPreview = state.historyPreview"))
        assertFalse(answerCall.contains("quickReviewQuestionPlaying"))
    }

    @Test
    fun `only first headword target receives optional glow`() {
        val section = answerSection.substringAfter("internal fun StudyAnswerSection(")
            .substringBefore("@Composable\nprivate fun StudyExampleSurface(")
        val headword = section.substringAfter("StudyAudioTextTarget(").substringBefore("FlowRow(")
        val meaning = section.substringAfter("vietnameseAnswer?.takeIf").substringBefore("if (!englishExample")
        assertTrue(headword.contains("swipeSuccessGlowActive = swipeSuccessGlowActive"))
        assertFalse(meaning.contains("swipeSuccessGlowActive"))
        assertFalse(section.substringAfter("StudyExampleSurface(").contains("swipeSuccessGlowActive"))
        assertTrue(answerSection.contains("swipeSuccessGlowActive: Boolean = false"))
    }

    @Test
    fun `orange glyph shadows overlay the original text without a container`() {
        val target = answerSection.substringAfter("internal fun StudyAudioTextTarget(")
        val glyphGlow = target.substringAfter("if (swipeSuccessGlowActive) {")
            .substringBefore("Text(\n                    text, style = style")
        assertTrue(glyphGlow.contains("StudySwipeFeedbackColors.quickReviewHeadword"))
        assertTrue(glyphGlow.contains("Shadow(orange.copy(alpha = 0.25f * swipeGlowAlpha), Offset.Zero, 24f)"))
        assertTrue(glyphGlow.contains("Shadow(orange.copy(alpha = 0.46f * swipeGlowAlpha), Offset.Zero, 14f)"))
        assertTrue(glyphGlow.contains("Shadow(orange.copy(alpha = 0.74f * swipeGlowAlpha), Offset.Zero, 6f)"))
        assertTrue(glyphGlow.contains("style = style.copy(shadow = glow)"))
        assertTrue(glyphGlow.contains("color = Color.Transparent"))
        assertTrue(glyphGlow.contains("Modifier.clearAndSetSemantics { }"))
        assertTrue(target.contains("durationMillis = 1_000"))
        assertTrue(target.contains("initialValue = 0.60f"))
        assertTrue(target.contains("targetValue = 1f"))
        assertTrue(target.contains("swipeSuccessGlowActive && !reducedMotion"))
        assertTrue(target.contains("1f + ((swipeGlowAlpha - 0.60f) / 0.40f).coerceIn(0f, 1f) * 0.04f"))
        assertTrue(target.contains("scaleX = swipeSuccessScale"))
        assertTrue(target.contains("scaleY = swipeSuccessScale"))
        assertTrue(target.contains("else 1f"))
        assertTrue(target.contains("swipeSuccessGlowActive -> StudySwipeFeedbackColors.quickReviewHeadwordForeground"))
        assertTrue(designTokens.contains("quickReviewHeadwordForeground = Color(0xFF000000)"))
        assertTrue(target.contains("isPlaying || strongEmphasis -> MaterialTheme.colorScheme.primary"))
        assertTrue(target.contains("else -> MaterialTheme.colorScheme.onSurface"))
        assertFalse(glyphGlow.contains("drawRoundRect"))
        assertFalse(glyphGlow.contains("BorderStroke"))
        assertFalse(glyphGlow.contains("Surface("))
        assertFalse(glyphGlow.contains("background("))
        assertFalse(glyphGlow.contains("padding("))
        assertFalse(glyphGlow.contains("contentDescription"))
    }

    @Test
    fun `approved image pulse and question gate remain independent`() {
        assertTrue(screen.contains("scaleX = imageFeedbackScale * quickReviewPulseScale"))
        assertTrue(screen.contains("targetValue = if (quickReviewQuestionPlaying && !reducedMotion) 1.04f else 1f"))
        val gate = screen.substringAfter("val startQuickReviewQuestionGate:")
            .substringBefore("LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen)")
        assertTrue(gate.contains("path = introduction.resolvedPromptAudio"))
        assertTrue(gate.contains("onEvent(AndroidStudyEvent.NextVisited)"))
        assertFalse(gate.contains("swipeSuccessGlowActive"))
    }

    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin").resolve(relative))
}
