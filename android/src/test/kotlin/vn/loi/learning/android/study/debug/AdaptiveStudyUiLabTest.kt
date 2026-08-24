package vn.loi.learning.android.study.debug

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
    fun `state factory creates valid typing study state with correct evaluation`() {
        val content = sampleContent()
        val allContents = listOf(content)

        // Incomplete state
        val stateIncomplete = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = allContents,
            mode = LabStudyMode.TYPING,
            mediaResolver = { "resolved/$it" },
            currentInput = "resil",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.Typing>(stateIncomplete)
        assertEquals("khả năng phục hồi", stateIncomplete.prompt)
        assertEquals("resolved/audio_1.mp3", stateIncomplete.resolvedExpectedAnswerAudio)
        assertEquals(TypingAnswerEvaluationStatus.EMPTY, stateIncomplete.evaluation)
        assertFalse(stateIncomplete.completed)

        // Correct completed state
        val stateCorrect = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = allContents,
            mode = LabStudyMode.TYPING,
            mediaResolver = { "resolved/$it" },
            currentInput = "resilience",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.Typing>(stateCorrect)
        assertEquals(TypingAnswerEvaluationStatus.CORRECT, stateCorrect.evaluation)
        assertEquals(RecallOutcome.CORRECT, stateCorrect.outcome)
        assertTrue(stateCorrect.completed)

        // Incorrect completed state
        val stateIncorrect = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = allContents,
            mode = LabStudyMode.TYPING,
            mediaResolver = { "resolved/$it" },
            currentInput = "wrong",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.Typing>(stateIncorrect)
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, stateIncorrect.evaluation)
        assertEquals(RecallOutcome.INCORRECT, stateIncorrect.outcome)
    }

    @Test
    fun `state factory creates valid listening study state and handles missing audio safely`() {
        val contentNoAudio = sampleContent(audio = null)
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = contentNoAudio,
            allPackageContents = listOf(contentNoAudio),
            mode = LabStudyMode.LISTENING,
            mediaResolver = { "resolved/$it" },
            currentInput = "",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.Listening>(state)
        assertEquals(null, state.audioPath)
        assertEquals(null, state.resolvedPromptAudio)
    }

    @Test
    fun `state factory creates valid multiple choice state with fallback distractors when package is small`() {
        val target = sampleContent("c1", "resilience", "khả năng phục hồi")
        val other = sampleContent("c2", "meticulous", "tỉ mỉ")
        val allContents = listOf(target, other)

        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = target,
            allPackageContents = allContents,
            mode = LabStudyMode.MULTIPLE_CHOICE,
            mediaResolver = { "resolved/$it" },
            currentInput = "",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 2,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.MultipleChoice>(state)
        assertTrue(state.choices.size >= 4, "Should have at least 4 choices including fallbacks")
        val correctChoice = state.choices.find { it.correct }
        assertNotNull(correctChoice)
        assertEquals("khả năng phục hồi", correctChoice.text)

        // Select correct choice
        val stateSelected = AdaptiveStudyUiLabStateFactory.buildState(
            content = target,
            allPackageContents = allContents,
            mode = LabStudyMode.MULTIPLE_CHOICE,
            mediaResolver = { "resolved/$it" },
            currentInput = "",
            selectedChoiceId = "choice-target-c1",
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 2,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.MultipleChoice>(stateSelected)
        assertEquals(RecallOutcome.CORRECT, stateSelected.outcome)
    }

    @Test
    fun `state factory creates valid image recall state and handles missing image safely`() {
        val contentNoImg = sampleContent(image = null)
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = contentNoImg,
            allPackageContents = listOf(contentNoImg),
            mode = LabStudyMode.IMAGE_RECALL,
            mediaResolver = { "resolved/$it" },
            currentInput = "resilience",
            selectedChoiceId = null,
            isRevealed = true,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.ImageRecall>(state)
        assertEquals(null, state.imagePath)
        assertEquals(null, state.resolvedImage)
        assertTrue(state.completed)
    }

    @Test
    fun `state factory creates valid example completion state with accurate cloze span`() {
        val content = sampleContent(
            primary = "resilience",
            example = "Her resilience was inspiring to all."
        )
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.EXAMPLE_COMPLETION,
            mediaResolver = { "resolved/$it" },
            currentInput = "resilience",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = true,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.ExampleCompletion>(state)
        assertEquals("Her ", state.prefix)
        assertEquals("resilience", state.blank)
        assertEquals(" was inspiring to all.", state.suffix)
        assertEquals(RecallOutcome.CORRECT, state.outcome)
    }

    @Test
    fun `state factory example completion falls back gracefully when primary not in example`() {
        val content = sampleContent(
            primary = "resilience",
            example = "This sentence does not contain the headword."
        )
        val state = AdaptiveStudyUiLabStateFactory.buildState(
            content = content,
            allPackageContents = listOf(content),
            mode = LabStudyMode.EXAMPLE_COMPLETION,
            mediaResolver = { "resolved/$it" },
            currentInput = "",
            selectedChoiceId = null,
            isRevealed = false,
            isCompleted = false,
            currentIndex = 0,
            totalCount = 1,
            packageTitle = "Test Package"
        )
        assertIs<AndroidStudyState.ExampleCompletion>(state)
        assertNotNull(state.prefix)
        assertNotNull(state.blank)
        assertNotNull(state.suffix)
    }

    @Test
    fun `lab modes map to valid engine recall modes`() {
        assertEquals(RecallMode.TYPING, LabStudyMode.TYPING.recallMode)
        assertEquals(RecallMode.LISTENING, LabStudyMode.LISTENING.recallMode)
        assertEquals(RecallMode.MULTIPLE_CHOICE, LabStudyMode.MULTIPLE_CHOICE.recallMode)
        assertEquals(RecallMode.IMAGE_RECALL, LabStudyMode.IMAGE_RECALL.recallMode)
        assertEquals(RecallMode.EXAMPLE_COMPLETION, LabStudyMode.EXAMPLE_COMPLETION.recallMode)
    }

    @Test
    fun `lab layout respects stage scroll ownership and guarantees finite constraints`() {
        val screenSource = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/debug/AdaptiveStudyUiLabScreen.kt")
        )

        // 1. Root Column inside Scaffold must NOT have verticalScroll modifier
        val rootColumnModifier = screenSource
            .substringAfter("Scaffold(")
            .substringAfter("Column(")
            .substringAfter("modifier = Modifier")
            .substringBefore("// 1. Controls Header:")
            .substringBefore(")")
        assertFalse(
            rootColumnModifier.contains("verticalScroll"),
            "Root Column modifier must NOT have verticalScroll which causes infinite maximum height constraints"
        )

        // 2. Stage host container must use weight(1f) to ensure finite bounded height
        assertTrue(
            screenSource.contains(".weight(1f)"),
            "Stage host container must use weight(1f) to guarantee finite bounds"
        )

        // 3. TypingStudyStage MUST receive fillMaxSize and MUST NOT have verticalScroll passed to it
        // because TypingStudyStage owns vertical scrolling internally via TypedAnswerStageFrame(fillViewport = true)
        val typingCall = screenSource.substringAfter("is AndroidStudyState.Typing ->")
            .substringBefore("is AndroidStudyState.Listening ->")
        assertTrue(
            typingCall.contains("modifier = Modifier.fillMaxSize()"),
            "TypingStudyStage must receive Modifier.fillMaxSize() without verticalScroll wrapper"
        )
        val typingCallCode = typingCall.lines().filterNot { it.trim().startsWith("//") }.joinToString("\n")
        assertFalse(
            typingCallCode.contains("verticalScroll"),
            "TypingStudyStage must NOT have verticalScroll modifier passed into it"
        )

        // 4. Non-typing stages (Listening, MultipleChoice, ImageRecall, ExampleCompletion)
        // receive verticalScroll within the finite weight(1f) container
        val whenBlock = screenSource.substringAfter("when (val s = studyState) {")
        val listeningCall = whenBlock.substringAfter("is AndroidStudyState.Listening ->")
            .substringBefore("is AndroidStudyState.MultipleChoice ->")
        assertTrue(listeningCall.contains("verticalScroll(nonTypingScrollState)"))

        val mcCall = whenBlock.substringAfter("is AndroidStudyState.MultipleChoice ->")
            .substringBefore("is AndroidStudyState.ImageRecall ->")
        assertTrue(mcCall.contains("verticalScroll(nonTypingScrollState)"))

        val irCall = whenBlock.substringAfter("is AndroidStudyState.ImageRecall ->")
            .substringBefore("is AndroidStudyState.ExampleCompletion ->")
        assertTrue(irCall.contains("verticalScroll(nonTypingScrollState)"))

        val ecCall = whenBlock.substringAfter("is AndroidStudyState.ExampleCompletion ->")
            .substringBefore("Trạng thái kiểm thử không khả dụng")
        assertTrue(ecCall.contains("verticalScroll(nonTypingScrollState)"))

        // 5. All 5 real production stage composables must be directly embedded
        assertTrue(screenSource.contains("TypingStudyStage("))
        assertTrue(screenSource.contains("ListeningStudyStage("))
        assertTrue(screenSource.contains("MultipleChoiceStudyStage("))
        assertTrue(screenSource.contains("ImageRecallStudyStage("))
        assertTrue(screenSource.contains("ExampleCompletionStudyStage("))
    }
}
