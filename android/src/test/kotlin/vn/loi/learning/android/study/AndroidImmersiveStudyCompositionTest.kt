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
    private val presentationPolicy = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyPresentationPolicy.kt")
    )
    private val controls = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/components/StudyControls.kt")
    )
    private val answerSection = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/components/IntroductionAnswerSection.kt")
    )

    @Test
    fun `Introduction front is meaning and hero first without answer controls`() {
        assertTrue(screen.contains("state.meaning ?: \"Nghĩa tiếng Việt\""))
        assertTrue(screen.contains("Learning image, tap to discover"))
        assertTrue(screen.contains("Learning canvas, tap to discover the English word"))
        assertFalse(screen.contains("Text(\"Tap to reveal\""))
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction)"))
        val introduction = introductionSource()
        assertTrue(introduction.contains("if (!state.revealed) StudyAudioTextTarget("))
        assertTrue(introduction.indexOf("text = meaning") < introduction.indexOf("LearningEngineImage("))
        assertTrue(introduction.indexOf("LearningEngineImage(") < introduction.indexOf("StudyAnswerSection("))
        assertTrue(introduction.contains("modifier = Modifier.fillMaxSize().graphicsLayer"))
        assertTrue(introduction.contains("verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)"))
        assertFalse(introduction.contains("Arrangement.Bottom"))
        assertFalse(introduction.contains("Arrangement.Center"))
    }

    @Test
    fun `revealed Introduction uses non-overlapping stage and persistent four way dock`() {
        assertFalse(screen.contains("label = \"introduction reveal\""))
        assertFalse(screen.contains("AnimatedContent(\n                targetState = state.revealed"))
        assertTrue(screen.contains("StudyRatingBar"))
        listOf("Again", "Hard", "Good", "Easy").forEach { rating ->
            assertTrue(controls.contains("RatingButton(\"$rating\""))
        }
        assertFalse(screen.contains("\"Expected Answer\""))
        assertFalse(screen.contains("\"Meaning\""))
        assertFalse(screen.contains("\"Example\""))
        assertFalse(screen.contains("\"Translation\""))
        val introduction = introductionSource()
        val image = introduction.indexOf("LearningEngineImage(")
        val answer = introduction.indexOf("StudyAnswerSection(")
        assertTrue(image < answer)
        assertTrue(introduction.contains("StudyStageCard("))
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction) Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(introduction.contains("state = introductionScrollState"))
        assertFalse(introduction.contains("Spacer("))
    }

    @Test
    fun `Introduction occupies the available interaction viewport while content remains scrollable`() {
        val introduction = introductionSource()
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction) Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(introduction.contains("BoxWithConstraints(modifier.fillMaxSize())"))
        assertTrue(introduction.contains(".weight(1f).introductionStageGestures("))
        assertTrue(introduction.contains("LazyColumn("))
        assertTrue(introduction.contains("state = introductionScrollState"))
        assertTrue(introduction.contains("resolveIntroductionImageBounds(maxHeight.value.toInt())"))
        assertFalse(introduction.contains("Arrangement.SpaceEvenly"))
        assertFalse(introduction.contains("requiredHeight"))
        assertFalse(introduction.contains("Spacer("))
        val scrollingContent = introduction.substringAfter("LazyColumn(").substringBefore("StudyRatingBar(")
        assertFalse(scrollingContent.contains("StudyRatingBar("))
        assertTrue(introduction.indexOf("StudyRatingBar(") > introduction.indexOf("LazyColumn("))
    }

    @Test
    fun `front clue remains neutral while revealed answer owns strong emphasis`() {
        val introduction = introductionSource()
        val clue = introduction.substringAfter("if (!state.revealed) StudyAudioTextTarget(")
            .substringBefore("state.resolvedImage?.let")
        val answer = answerSection.substringAfter("englishAnswer, LearningContentTypography.vocabulary")
        assertFalse(clue.contains("strongEmphasis = true"))
        assertTrue(answer.contains("strongEmphasis = true"))
        val target = answerSection.substringAfter("internal fun StudyAudioTextTarget(")
        assertTrue(target.contains("strongEmphasis: Boolean = false"))
        assertTrue(target.contains("else Color.Transparent"))
    }

    @Test
    fun `all canonical review modes remain in the immersive stage`() {
        listOf("Typing", "MultipleChoice", "Listening", "ImageRecall", "ExampleCompletion").forEach { mode ->
            assertTrue(screen.contains("is AndroidStudyState.$mode"))
        }
        val foundation = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/components/StudyFoundationComponents.kt"))
        assertTrue(foundation.contains("StudyAnswerInput("))
        assertTrue(foundation.contains("imePadding()"))
        assertTrue(foundation.contains("defaultMinSize(minHeight = 56.dp)"))
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
        assertTrue(screen.contains("StudyContentDensity"))
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
            .substringBefore("@Composable\nprivate fun LearnNewProgressHeader(")
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
        assertFalse(answerSection.contains("languageLabel = \"EN\""))
        assertTrue(answerSection.contains("\"English example\""))
        assertFalse(answerSection.contains("languageLabel = \"VI\""))
        assertTrue(answerSection.contains("\"Vietnamese example\""))
        assertTrue(answerSection.contains("StudyExampleColors.english"))
        assertTrue(answerSection.contains("StudyExampleColors.vietnamese"))
    }

    @Test
    fun `runtime item advance has no artificial swipe delay and uses short transition`() {
        assertFalse(screen.contains("delay(if (reducedMotion) 0 else 110)"))
        assertFalse(screen.contains("gestureScope.launch"))
        assertTrue(screen.contains("onIntroductionRatingWithFeedback = { introduction, rating, focus ->"))
        assertTrue(screen.indexOf("outgoingStudyFeedback(") < screen.indexOf("onEvent(AndroidStudyEvent.RateIntroduction(rating))"))
        assertTrue(screen.contains("studyMotionDurationMillis(StudyMotionRole.CARD_ENTER, reducedMotion)"))
        assertFalse(screen.contains("-constraints.maxHeight * 1.08f"))
        val swipeDispatch = screen.substringAfter("IntroductionStageGesture.SWIPE_GOOD -> {")
            .substringBefore("IntroductionStageGesture.NONE")
        assertTrue(swipeDispatch.indexOf("onSwipeGood()") < swipeDispatch.indexOf("onGestureEnd(gesture)"))
    }

    @Test
    fun `rating feedback freezes one in-place card without duplicate overlay`() {
        assertFalse(screen.contains("StudyRatingFeedbackOverlay"))
        assertTrue(screen.contains("val presentedState = frozenIntroduction ?: state"))
        assertTrue(screen.contains("frozenIntroduction = introduction"))
        assertTrue(screen.contains("feedback = feedbackVisual"))
        assertTrue(screen.contains("selectedRating = feedbackRating"))
        assertTrue(screen.contains("introductionAutoplayEnabled = outgoingFeedback == null"))
    }

    @Test
    fun `revealed audio targets preserve child priority and English focus without Vietnamese resume`() {
        val introduction = introductionSource()
        assertTrue(screen.contains("nextIntroductionPlaybackFocus("))
        assertFalse(screen.contains("resumableLoopFocus"))
        assertFalse(screen.contains("resumeLoopAfterTemporary"))
        assertTrue(introduction.contains("restartAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)"))
        assertTrue(answerSection.contains("onEnglishExampleAudio"))
        assertTrue(answerSection.contains("onVietnameseExampleAudio"))
        assertTrue(screen.contains("childConsumed = childConsumed || change.isConsumed"))
    }

    @Test
    fun `Introduction revealed taps route through explicit canonical audio actions`() {
        val introduction = introductionSource()
        assertTrue(screen.contains("val toggleIntroductionEnglishLoop: () -> Unit"))
        assertTrue(screen.contains("toggleIntroductionEnglishLoop()"))
        assertTrue(introduction.contains("onAnswerAudio = {\n                            onGenericStageTap()"))
        assertTrue(introduction.contains("onImageExpandedChange(!imageExpanded)\n                                onGenericStageTap()"))
        assertTrue(introduction.contains("restartAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)"))
        assertTrue(introduction.contains("playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false)"))
        assertTrue(introduction.contains("playAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false)"))
        assertTrue(answerSection.contains("boundedAudioTarget = true"))
        assertTrue(answerSection.contains("boundedAudioTarget = !isLooping"))
    }

    @Test
    fun `Introduction context bar prefers package position without relabeling session progress`() {
        assertTrue(screen.contains("?.packagePosition ?: state.currentPosition"))
        assertTrue(screen.contains("?.packageTotal ?: state.totalItems"))
        assertTrue(screen.contains("currentPosition = (state as? AndroidStudyState.Introduction)"))
    }

    @Test
    fun `rating row uses stable Anki-like semantic palette on both Introduction states`() {
        val button = controls.substringAfter("private fun RatingButton(")
            .substringBefore("internal fun StudyActionDock(")
        assertTrue(button.contains("ReviewRating.AGAIN"))
        assertTrue(button.contains("StudyRatingColors.again"))
        assertTrue(button.contains("ReviewRating.HARD"))
        assertTrue(button.contains("StudyRatingColors.hard"))
        assertTrue(button.contains("ReviewRating.GOOD"))
        assertTrue(button.contains("StudyRatingColors.good"))
        assertTrue(button.contains("ReviewRating.EASY"))
        assertTrue(button.contains("StudyRatingColors.easy"))
        assertTrue(button.contains("defaultMinSize(minHeight = LearningSpacing.touchTarget)"))
    }

    @Test
    fun `review feedback is restrained accessible and answer remains visual focus`() {
        val reveal = screen.substringAfter("private fun StudyRevealAndFeedbackContent(")
            .substringBefore("private fun Completion(")
        assertTrue(reveal.contains("StudyAnswerSection("))
        assertTrue(reveal.contains("liveRegion = LiveRegionMode.Polite"))
        assertFalse(reveal.contains("Surface("))
        listOf("\"Expected Answer\"", "\"Meaning\"", "\"Example\"", "\"Translation\"").forEach {
            assertFalse(reveal.contains(it), it)
        }
    }

    @Test
    fun `choice tiles wrap and preserve selection semantics`() {
        val foundation = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/components/StudyFoundationComponents.kt"))
        val choice = foundation.substringAfter("internal fun StudyChoiceTile(")
            .substringBefore("internal fun StudyAnswerInput(")
        assertTrue(choice.contains("defaultMinSize(minHeight = 56.dp)"))
        assertTrue(choice.contains("selected = visualState == StudyChoiceVisualState.SELECTED"))
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
        assertTrue(presentationPolicy.contains("nextIntroductionPlaybackFocus("))
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
        .substringBefore("private fun IntroductionInteractionHint(")
}
