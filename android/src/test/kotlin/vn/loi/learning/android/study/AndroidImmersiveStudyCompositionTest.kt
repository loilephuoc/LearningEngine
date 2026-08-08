package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidImmersiveStudyCompositionTest {
    private val screen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
    )
    private val components = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/ui/LearningEngineComponents.kt")
    )

    @Test
    fun `Introduction front is meaning and hero first without answer controls`() {
        assertTrue(screen.contains("state.meaning ?: \"Nghĩa tiếng Việt\""))
        assertTrue(screen.contains("Learning image, tap to discover"))
        assertTrue(screen.contains("Learning canvas, tap to discover the English word"))
        assertFalse(screen.contains("Text(\"Tap to reveal\""))
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction)"))
        val introduction = introductionSource()
        assertTrue(introduction.contains("if (!state.revealed) IntroductionAudioTextTarget("))
        assertTrue(introduction.indexOf("text = meaning") < introduction.indexOf("LearningEngineImage("))
        assertTrue(introduction.indexOf("LearningEngineImage(") < introduction.indexOf("text = state.answer"))
    }

    @Test
    fun `revealed Introduction uses non-overlapping stage and persistent four way dock`() {
        assertFalse(screen.contains("label = \"introduction reveal\""))
        assertFalse(screen.contains("AnimatedContent(\n                targetState = state.revealed"))
        assertTrue(screen.contains("LearningEngineRatingDock"))
        listOf("Again", "Hard", "Good", "Easy").forEach { rating ->
            assertTrue(screen.contains("RatingDockButton(\"$rating\""))
        }
        assertFalse(screen.contains("\"Expected Answer\""))
        assertFalse(screen.contains("\"Meaning\""))
        assertFalse(screen.contains("\"Example\""))
        assertFalse(screen.contains("\"Translation\""))
        val introduction = introductionSource()
        val image = introduction.indexOf("LearningEngineImage(")
        val answer = introduction.indexOf("text = state.answer")
        val metadata = introduction.indexOf("state.partOfSpeech")
        val revealedMeaning = introduction.lastIndexOf("text = meaning")
        val example = introduction.indexOf("text = state.example.orEmpty()")
        assertTrue(image < answer && answer < metadata && metadata < revealedMeaning && revealedMeaning < example)
        assertTrue(introduction.contains("MaterialTheme.colorScheme.surfaceContainer"))
    }

    @Test
    fun `all canonical review modes remain in the immersive stage`() {
        listOf("Typing", "MultipleChoice", "Listening", "ImageRecall", "ExampleCompletion").forEach { mode ->
            assertTrue(screen.contains("is AndroidStudyState.$mode"))
        }
        assertTrue(screen.contains("AnswerField("))
        assertTrue(screen.contains("imePadding()"))
        assertTrue(screen.contains("defaultMinSize(minHeight = 56.dp)"))
    }

    @Test
    fun `HUD stays projected state and composition has no data authority`() {
        assertTrue(screen.contains("state.hud?.let { LearningEngineCompactHud(it) }"))
        assertFalse(screen.contains("StudyHeaderStatisticsQueryService"))
        assertFalse(screen.contains("Repository"))
        assertFalse(screen.contains("context.engine"))
    }

    @Test
    fun `study colors motion and image semantics remain token driven`() {
        assertFalse(Regex("Color\\(0x[0-9A-Fa-f]+\\)").containsMatchIn(screen))
        assertTrue(screen.contains("LearningMotion.standardMillis"))
        assertTrue(screen.contains("isReducedMotionEnabled()"))
        assertTrue(components.contains("interactionDescription: String"))
        assertTrue(components.contains("contentDescription = null"))
        assertTrue(screen.contains("defaultMinSize(minHeight = LearningSpacing.touchTarget)"))
    }

    @Test
    fun `runtime has one learning stage instead of separate main and report cards`() {
        assertTrue(screen.contains("private fun LearningEngineLearningStage("))
        assertTrue(screen.contains("private fun StudyRevealAndFeedbackContent("))
        assertFalse(screen.contains("private fun StudyMainCard("))
        assertFalse(screen.contains("private fun StudyRevealAndFeedbackSection("))
        val stage = screen.substringAfter("private fun LearningEngineLearningStage(")
            .substringBefore("private fun IntroductionLearningStage(")
        assertTrue(stage.contains("StudyModeInputArea("))
        assertTrue(stage.contains("StudyRevealAndFeedbackContent("))
    }

    @Test
    fun `compact HUD includes canonical progress total and secondary rating distribution`() {
        val hud = screen.substringAfter("private fun LearningEngineCompactHud(")
            .substringBefore("private fun progressDescription(")
        listOf("newCompleted", "newTarget", "reviewCompleted", "reviewTarget", "totalLearned",
            "dueCount", "againCount", "hardCount", "goodCount", "easyCount").forEach {
            assertTrue(hud.contains(it), it)
        }
        assertFalse(hud.contains("remember"))
        assertFalse(hud.contains("mutableState"))
    }

    @Test
    fun `review feedback is restrained accessible and answer remains visual focus`() {
        val reveal = screen.substringAfter("private fun StudyRevealAndFeedbackContent(")
            .substringBefore("private fun Completion(")
        assertTrue(reveal.contains("LearningContentTypography.vocabulary"))
        assertTrue(reveal.contains("liveRegion = LiveRegionMode.Polite"))
        assertFalse(reveal.contains("Surface("))
        listOf("\"Expected Answer\"", "\"Meaning\"", "\"Example\"", "\"Translation\"").forEach {
            assertFalse(reveal.contains(it), it)
        }
    }

    @Test
    fun `choice tiles wrap and preserve selection semantics`() {
        val choice = screen.substringAfter("private fun LearningEngineChoiceTile(")
            .substringBefore("private fun AnswerField(")
        assertTrue(choice.contains("defaultMinSize(minHeight = 56.dp)"))
        assertTrue(choice.contains("selected = isSelected"))
        assertFalse(choice.contains("maxLines"))
        assertFalse(choice.contains("RadioButton"))
    }

    @Test
    fun `hero media changes role across discovery reveal and image recall`() {
        assertTrue(screen.contains("resolveIntroductionImageSizing("))
        assertTrue(screen.contains("LocalConfiguration.current.screenHeightDp"))
        assertTrue(screen.contains("!state.revealed || imageExpanded -> sizing.frontDp"))
        assertTrue(screen.contains("else -> sizing.revealDp"))
        assertTrue(screen.contains("fillCanvas = true"))
    }

    @Test
    fun `image tap owns interaction while stage tap retains canonical audio cycling`() {
        val introduction = introductionSource()
        assertTrue(introduction.contains("onOpenFullscreen = {"))
        assertTrue(introduction.contains("onImageExpandedChange(!imageExpanded)"))
        assertTrue(introduction.contains("restartAudio("))
        assertTrue(screen.contains("onIntroductionStageTap = {"))
        assertTrue(screen.contains("nextIntroductionPlaybackFocus("))
        assertTrue(components.contains(".clickable { imagePath?.let(onOpenFullscreen) }"))
    }

    @Test
    fun `quiet study chrome is single line and progress remains slim`() {
        val topBar = components.substringAfter("fun LearningEngineStudyTopBar(")
            .substringBefore("fun isReducedMotionEnabled")
        assertTrue(topBar.contains("maxLines = 1"))
        assertTrue(topBar.contains("titleSmall"))
        assertTrue(topBar.contains("height(3.dp)"))
        assertFalse(topBar.contains("TopAppBar("))
    }

    private fun introductionSource() = screen.substringAfter("private fun IntroductionLearningStage(")
        .substringBefore("private fun IntroductionAudioTextTarget(")
}
