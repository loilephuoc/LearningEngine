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
        assertTrue(introduction.contains("modifier = Modifier.fillMaxSize().graphicsLayer"))
        assertTrue(introduction.contains("verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)"))
        assertFalse(introduction.contains("Arrangement.Bottom"))
        assertFalse(introduction.contains("Arrangement.Center"))
    }

    @Test
    fun `revealed Introduction uses non-overlapping stage and persistent four way dock`() {
        assertFalse(screen.contains("label = \"introduction reveal\""))
        assertFalse(screen.contains("AnimatedContent(\n                targetState = state.revealed"))
        assertTrue(screen.contains("LearningEngineRatingRow"))
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
        assertTrue(metadata < image && image < answer && answer < revealedMeaning && revealedMeaning < example)
        assertTrue(introduction.contains("color = MaterialTheme.colorScheme.surface\n"))
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction) Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(introduction.contains("state = introductionScrollState"))
        assertFalse(introduction.contains("Spacer("))
    }

    @Test
    fun `Introduction occupies the available interaction viewport while content remains scrollable`() {
        val introduction = introductionSource()
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction) Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(introduction.contains("BoxWithConstraints(modifier.fillMaxSize())"))
        assertTrue(introduction.contains("modifier = Modifier.fillMaxWidth().weight(1f).introductionStageGestures("))
        assertTrue(introduction.contains("LazyColumn("))
        assertTrue(introduction.contains("state = introductionScrollState"))
        assertTrue(introduction.contains("resolveIntroductionImageBounds(maxHeight.value.toInt())"))
        assertFalse(introduction.contains("Arrangement.SpaceEvenly"))
        assertFalse(introduction.contains("requiredHeight"))
        assertFalse(introduction.contains("Spacer("))
        val scrollingContent = introduction.substringAfter("LazyColumn(").substringBefore("LearningEngineRatingRow(")
        assertFalse(scrollingContent.contains("LearningEngineRatingRow("))
        assertTrue(introduction.indexOf("LearningEngineRatingRow(") > introduction.indexOf("LazyColumn("))
    }

    @Test
    fun `front clue remains neutral while revealed answer owns strong emphasis`() {
        val introduction = introductionSource()
        val clue = introduction.substringAfter("if (!state.revealed) IntroductionAudioTextTarget(")
            .substringBefore("state.resolvedImage?.let")
        val answer = introduction.substringAfter("text = state.answer")
            .substringBefore("if (!state.pronunciation")
        assertFalse(clue.contains("strongEmphasis = true"))
        assertTrue(answer.contains("strongEmphasis = true"))
        val target = screen.substringAfter("private fun IntroductionAudioTextTarget(")
            .substringBefore("private fun StudyPromptHeader(")
        assertTrue(target.contains("strongEmphasis: Boolean = false"))
        assertTrue(target.contains("else -> androidx.compose.ui.graphics.Color.Transparent"))
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
        assertTrue(screen.contains("LearnNewProgressHeader(state, hud)"))
        assertTrue(screen.contains("LearningEngineCompactHud(hud)"))
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
    fun `Learn New HUD restores canonical compact statistics without recalculation`() {
        val hud = screen.substringAfter("private fun LearnNewProgressHeader(")
            .substringBefore("private fun CompactLearnMetric(")
        listOf("totalLearned", "newCompleted", "newConfiguredTarget", "reviewCompleted",
            "reviewConfiguredTarget", "dueCount", "againCount", "hardCount", "goodCount", "easyCount").forEach {
            assertTrue(hud.contains(it), it)
        }
        listOf("Again", "Hard", "Good", "Easy").forEach { assertTrue(hud.contains(it), it) }
        assertFalse(hud.contains("CompactLearnMetric(\"A\""))
        assertFalse(hud.contains("CompactLearnMetric(\"H\""))
        assertFalse(hud.contains("CompactLearnMetric(\"G\""))
        assertFalse(hud.contains("CompactLearnMetric(\"E\""))
        assertFalse(hud.contains("Repository"))
        assertFalse(hud.contains("context.engine"))
    }

    @Test
    fun `Introduction examples are separate semantic language audio surfaces`() {
        val introduction = introductionSource()
        assertTrue(introduction.contains("languageLabel = \"EN\""))
        assertTrue(introduction.contains("accessibilityLabel = \"English example\""))
        assertTrue(introduction.contains("englishRoute.role, englishRoute.path, englishRoute.isLooping"))
        assertTrue(introduction.contains("languageLabel = \"VI\""))
        assertTrue(introduction.contains("accessibilityLabel = \"Vietnamese example\""))
        assertTrue(introduction.contains("vietnameseRoute.role, vietnameseRoute.path, vietnameseRoute.isLooping"))
        assertFalse(introduction.contains("LearningEngineAudioIndicator("))
    }

    @Test
    fun `runtime item advance has no artificial swipe delay and uses short transition`() {
        assertFalse(screen.contains("delay(if (reducedMotion) 0 else 110)"))
        assertFalse(screen.contains("gestureScope.launch"))
        assertTrue(screen.contains("onEvent(AndroidStudyEvent.RateIntroduction(rating))\n            audioController.stop()"))
        assertTrue(screen.contains("fadeIn(tween(if (reducedMotion) 0 else 70))"))
    }

    @Test
    fun `rating row uses stable Anki-like semantic palette on both Introduction states`() {
        val button = screen.substringAfter("private fun RatingDockButton(")
            .substringBefore("private fun LearningEngineCompactHud(")
        assertTrue(button.contains("ReviewRating.AGAIN"))
        assertTrue(button.contains("errorContainer"))
        assertTrue(button.contains("ReviewRating.HARD"))
        assertTrue(button.contains("semantic.warning.copy(alpha = 0.18f)"))
        assertTrue(button.contains("ReviewRating.GOOD"))
        assertTrue(button.contains("containerColor = MaterialTheme.colorScheme.primary"))
        assertTrue(button.contains("ReviewRating.EASY"))
        assertTrue(button.contains("semantic.info.copy(alpha = 0.18f)"))
        assertTrue(button.contains("defaultMinSize(minHeight = LearningSpacing.touchTarget)"))
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
        assertTrue(screen.contains("resolveIntroductionImageBounds("))
        assertTrue(screen.contains("resolveIntroductionImageBounds(maxHeight.value.toInt())"))
        assertTrue(screen.contains("!state.revealed || imageExpanded -> bounds.frontMaxHeightDp"))
        assertTrue(screen.contains("else -> bounds.revealMaxHeightDp"))
        assertTrue(screen.contains("adaptiveFitBounds = LearningImageFitBounds("))
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
