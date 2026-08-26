package vn.loi.learning.android.study.debug

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.android.study.AndroidStudyEvent
import vn.loi.learning.android.study.AndroidStudyState
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallMode
import vn.loi.learning.domain.study.recall.RecallOutcome

class AdaptiveStudyUiLabTest {

    private fun sampleContent(
        id: String = "c1",
        primary: String = "resilience",
        meaning: String = "khả năng phục hồi",
        example: String? = "Her resilience was inspiring.",
        audio: String? = "audio_1.mp3",
        image: String? = "image_1.png"
    ): Content {
        return Content(
            id = ContentId(id),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = primary,
                translatedText = meaning,
                exampleText = example
            ),
            media = ContentMedia(
                primaryAudio = audio,
                image = image
            ),
            metadata = ContentMetadata(lesson = "Lesson 1"),
            customFields = ContentCustomFields(
                setOf(
                    ContentCustomField(ContentFieldId("ipa"), "rɪˈzɪljəns"),
                    ContentCustomField(ContentFieldId("partOfSpeech"), "noun")
                )
            )
        )
    }

    @Test
    fun `1 Lab home selects Typing and builds preview request`() {
        val content = sampleContent()
        val allContents = listOf(content)

        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = allContents,
            mode = LabStudyMode.TYPING,
            mediaResolver = { "media/$it" },
            currentInput = "",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.Typing>(state)
        assertEquals("khả năng phục hồi", state.prompt)
        assertEquals("media/audio_1.mp3", state.resolvedExpectedAnswerAudio)
        assertEquals(TypingAnswerEvaluationStatus.EMPTY, state.evaluation)
        assertFalse(state.completed)
        assertNotNull(state.hud, "Must provide HUD metrics for production header")
        assertNotNull(state.navigation, "Must provide navigation state for production review navigation")
    }

    @Test
    fun `2 Listening request builds correct production-compatible state`() {
        val content = sampleContent()
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.LISTENING,
            mediaResolver = { "media/$it" },
            currentInput = "",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.Listening>(state)
        assertEquals("media/audio_1.mp3", state.audioPath)
        assertEquals("media/audio_1.mp3", state.resolvedPromptAudio)
        assertNotNull(state.hud)
    }

    @Test
    fun `Listening exact input builds production compact success and incomplete input stays active`() {
        val content = sampleContent()
        fun state(input: String, pending: Boolean) = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.LISTENING,
            mediaResolver = { "media/$it" },
            currentInput = input,
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = pending,
            listeningCompletionPending = pending,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        ) as AndroidStudyState.Listening

        assertTrue(isExactListeningPreviewAnswer("  RESILIENCE ", content.text.primaryText))
        assertFalse(isExactListeningPreviewAnswer("resilien", content.text.primaryText))

        val active = state("resilien", false)
        assertFalse(active.completionPending)
        assertFalse(active.completed)
        assertEquals(TypingAnswerEvaluationStatus.EMPTY, active.evaluation)

        val success = state("resilience", true)
        assertTrue(success.completionPending)
        assertTrue(success.completed)
        assertEquals(RecallOutcome.CORRECT, success.outcome)
        assertEquals(TypingAnswerEvaluationStatus.CORRECT, success.evaluation)
        assertEquals(ReviewRating.GOOD, success.previousCanonicalRating)
        assertTrue(success.canonicalRatingTransitionEligible)
        assertEquals(ReviewRating.GOOD, success.automaticRating?.rating)
        assertEquals("media/image_1.png", success.resolvedImage)
        assertEquals("resilience", success.plan.answerContract.canonicalAnswer)
        assertEquals("NOUN", success.partOfSpeech)
        assertEquals("rɪˈzɪljəns", success.pronunciation)
        assertEquals("khả năng phục hồi", success.meaning)
    }

    @Test
    fun `Listening preview exact answer and navigation are local reset only`() {
        val previewSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiPreviewScreen.kt")
        )
        val answerChanged = previewSource.substringAfter("is AndroidStudyEvent.AnswerChanged ->")
            .substringBefore("is AndroidStudyEvent.Submit ->")
        val nextVisited = previewSource.substringAfter("is AndroidStudyEvent.NextVisited ->")
            .substringBefore("is AndroidStudyEvent.PreviousVisited ->")

        assertTrue(answerChanged.contains("selectedMode == LabStudyMode.LISTENING"))
        assertTrue(answerChanged.contains("isExactListeningPreviewAnswer"))
        assertTrue(answerChanged.contains("listeningCompletionPending = true"))
        assertTrue(answerChanged.contains("isCompleted = true"))
        assertTrue(nextVisited.contains("listeningCompletionPending = false"))
        assertTrue(nextVisited.contains("currentItemIndex + 1"))
        assertTrue(nextVisited.contains("else {\n                                0"))
        assertFalse(previewSource.contains("AndroidStudyFacade"))
        assertFalse(previewSource.contains("executeRecallLearning"))
    }

    @Test
    fun `3 Multiple Choice request builds correct state with choices`() {
        val target = sampleContent("c1", "resilience", "khả năng phục hồi")
        val distractor = sampleContent("c2", "meticulous", "tỉ mỉ")
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = target,
            allPackageContents = listOf(target, distractor),
            mode = LabStudyMode.MULTIPLE_CHOICE,
            mediaResolver = { "media/$it" },
            currentInput = "",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 2,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.MultipleChoice>(state)
        assertTrue(state.choices.size >= 4, "Must generate at least 4 choices")
        val correctChoice = state.choices.find { it.correct }
        assertNotNull(correctChoice)
        assertEquals("khả năng phục hồi", correctChoice.text)
    }

    @Test
    fun `4 Image Recall request builds correct state`() {
        val content = sampleContent(image = "tree.png")
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.IMAGE_RECALL,
            mediaResolver = { "media/$it" },
            currentInput = "tree",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.ImageRecall>(state)
        assertEquals("media/tree.png", state.imagePath)
        assertEquals("media/tree.png", state.resolvedImage)
    }

    @Test
    fun `Adaptive Typing and Image Recall exact previews enter automatic compact success`() {
        val content = sampleContent()
        fun build(mode: LabStudyMode) = AdaptiveStudyUiLabStateFactory.buildState(
            content, listOf(content), mode, { "media/$it" }, "resilience", null,
            false, true,
            adaptiveTypingCompletionPending = mode == LabStudyMode.TYPING,
            imageRecallCompletionPending = mode == LabStudyMode.IMAGE_RECALL,
            currentIndex = 0, totalCount = 1, packageTitle = "Test"
        )

        val typing = build(LabStudyMode.TYPING) as AndroidStudyState.Typing
        val image = build(LabStudyMode.IMAGE_RECALL) as AndroidStudyState.ImageRecall
        listOf(typing.completionPending, image.completionPending).forEach(::assertTrue)
        assertEquals(RecallOutcome.CORRECT, typing.outcome)
        assertEquals(RecallOutcome.CORRECT, image.outcome)
        assertEquals(ReviewRating.GOOD, typing.automaticRating?.rating)
        assertEquals(ReviewRating.GOOD, image.automaticRating?.rating)
        assertNotNull(image.resolvedImage)
    }

    @Test
    fun `5 Example Completion request builds correct state with cloze span`() {
        val content = sampleContent(
            primary = "resilience",
            example = "Her mental resilience helped her."
        )
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.EXAMPLE_COMPLETION,
            mediaResolver = { "media/$it" },
            currentInput = "resilience",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.ExampleCompletion>(state)
        assertEquals("Her mental ", state.prefix)
        assertEquals("resilience", state.blank)
        assertEquals(" helped her.", state.suffix)
        assertEquals(RecallOutcome.CORRECT, state.outcome)
    }

    @Test
    fun `6 Production preview renders through the same real StudyScreen container`() {
        val previewSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiPreviewScreen.kt")
        )
        assertTrue(
            previewSource.contains("StudyScreen("),
            "AdaptiveStudyUiPreviewScreen must render canonical production StudyScreen"
        )
        assertTrue(
            previewSource.contains("import vn.loi.learning.android.study.StudyScreen"),
            "AdaptiveStudyUiPreviewScreen must import real StudyScreen"
        )
    }

    @Test
    fun `7 Preview event sink does not call real review commit or mutate database`() {
        val previewSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiPreviewScreen.kt")
        )
        assertFalse(
            previewSource.contains("AndroidStudyFacade"),
            "AdaptiveStudyUiPreviewScreen must NOT instantiate or call AndroidStudyFacade"
        )
        assertFalse(
            previewSource.contains("commitReview"),
            "AdaptiveStudyUiPreviewScreen must NOT invoke commitReview"
        )
        assertFalse(
            previewSource.contains("fsrs"),
            "AdaptiveStudyUiPreviewScreen must NOT invoke FSRS algorithms"
        )
    }

    @Test
    fun `8 Typing Check produces local-only evaluation and outcome`() {
        val content = sampleContent(primary = "resilience")
        val correctState = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.TYPING,
            mediaResolver = { null },
            currentInput = "resilience",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test"
        ) as AndroidStudyState.Typing
        assertEquals(TypingAnswerEvaluationStatus.CORRECT, correctState.evaluation)
        assertEquals(RecallOutcome.CORRECT, correctState.outcome)

        val wrongState = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.TYPING,
            mediaResolver = { null },
            currentInput = "wrong_answer",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test"
        ) as AndroidStudyState.Typing
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, wrongState.evaluation)
        assertEquals(RecallOutcome.INCORRECT, wrongState.outcome)
    }

    @Test
    fun `9 Reveal produces local-only revealed state`() {
        val content = sampleContent()
        val stateRevealed = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.TYPING,
            mediaResolver = { null },
            currentInput = "",
            selectedChoiceId = null,
            isRevealed = true,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test"
        ) as AndroidStudyState.Typing
        assertTrue(stateRevealed.revealed)
        assertFalse(stateRevealed.completed)
    }

    @Test
    fun `10 Multiple Choice selection produces local-only state`() {
        val content = sampleContent("c1", "resilience", "khả năng phục hồi")
        val selectedState = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.MULTIPLE_CHOICE,
            mediaResolver = { null },
            currentInput = "",
            selectedChoiceId = "choice-target-c1",
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test"
        ) as AndroidStudyState.MultipleChoice
        assertEquals("choice-target-c1", selectedState.selectedChoiceId)
        assertTrue(selectedState.completed)
        assertEquals(RecallOutcome.CORRECT, selectedState.outcome)
    }

    @Test
    fun `11 Rating tap produces zero FSRS writes in preview controller`() {
        val previewSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiPreviewScreen.kt")
        )
        val rateBranch = previewSource.substringAfter("is AndroidStudyEvent.RateIntroduction,")
            .substringBefore("is AndroidStudyEvent.Retry ->")

        // Must update local index / input without calling study repositories
        assertTrue(rateBranch.contains("currentItemIndex"))
        assertTrue(rateBranch.contains("isCompleted = false"))
        assertFalse(rateBranch.contains("reviewRepository"))
        assertFalse(rateBranch.contains("scheduleNext"))
    }

    @Test
    fun `12 Preview exit leaves real study queue unchanged`() {
        val previewSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiPreviewScreen.kt")
        )
        val homeBranch = previewSource.substringAfter("is AndroidStudyEvent.Home ->")
            .substringBefore("is AndroidStudyEvent.AnswerChanged ->")
        assertTrue(homeBranch.contains("onBack()"))
    }

    @Test
    fun `13 BuildConfig DEBUG gate remains enforced on launcher and preview routes`() {
        val mainActivitySource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/MainActivity.kt")
        )
        val debugSection = mainActivitySource.substringAfter("if (BuildConfig.DEBUG) {")
            .substringBefore("opensStudyFromExplicitEvent")
        assertTrue(
            debugSection.contains("adaptive_study_ui_lab"),
            "adaptive_study_ui_lab route must be guarded by BuildConfig.DEBUG"
        )
        assertTrue(
            debugSection.contains("adaptive_study_ui_preview"),
            "adaptive_study_ui_preview route must be guarded by BuildConfig.DEBUG"
        )
    }

    @Test
    fun `14 No Lab vertical-scroll ancestor wraps the production StudyScreen in preview`() {
        val previewSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiPreviewScreen.kt")
        )
        val outerContainer = previewSource.substringAfter("AdaptiveStudyUiPreviewScreen(")
            .substringBefore("StudyScreen(")
        assertFalse(
            outerContainer.contains("verticalScroll"),
            "AdaptiveStudyUiPreviewScreen must NOT wrap StudyScreen in a verticalScroll modifier"
        )
    }

    @Test
    fun `15 Lab launcher does not embed stage cards and provides Open Production Preview action`() {
        val labLauncherSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiLabScreen.kt")
        )
        assertFalse(
            labLauncherSource.contains("TypingStudyStage("),
            "AdaptiveStudyUiLabScreen must not embed TypingStudyStage"
        )
        assertFalse(
            labLauncherSource.contains("ListeningStudyStage("),
            "AdaptiveStudyUiLabScreen must not embed ListeningStudyStage"
        )
        assertFalse(
            labLauncherSource.contains("MultipleChoiceStudyStage("),
            "AdaptiveStudyUiLabScreen must not embed MultipleChoiceStudyStage"
        )
        assertFalse(
            labLauncherSource.contains("ImageRecallStudyStage("),
            "AdaptiveStudyUiLabScreen must not embed ImageRecallStudyStage"
        )
        assertFalse(
            labLauncherSource.contains("ExampleCompletionStudyStage("),
            "AdaptiveStudyUiLabScreen must not embed ExampleCompletionStudyStage"
        )
        assertTrue(
            labLauncherSource.contains("Mở giao diện học thật"),
            "AdaptiveStudyUiLabScreen must have Open Production Preview button"
        )
    }
}
