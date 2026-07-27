package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.nio.file.Path
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class FocusedVocabularyAnswerTest {

    @Test
    fun `1 - Vocabulary identity is visually modeled separately from meaning`() {
        val content = Content(
            id = ContentId("vocab-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "apple",
                translatedText = "quả táo",
                pronunciation = "ˈæp.əl"
            )
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)

        assertEquals("apple", model.englishWord)
        assertEquals("quả táo", model.vietnameseMeaning)
    }

    @Test
    fun `2 - IPA and part of speech remain available`() {
        val content = Content(
            id = ContentId("vocab-2"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "run",
                translatedText = "chạy",
                pronunciation = "rʌn"
            ),
            customFields = ContentCustomFields(
                setOf(ContentCustomField(ContentFieldId("partOfSpeech"), "verb"))
            )
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)

        assertEquals("/rʌn/", model.ipa)
        assertEquals("VERB", model.partOfSpeech)
    }

    @Test
    fun `3 - Missing image does not break layout`() {
        val content = Content(
            id = ContentId("vocab-3"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "book",
                translatedText = "sách"
            ),
            media = ContentMedia(image = null)
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)

        assertNull(model.imagePath)
    }

    @Test
    fun `4 - Missing IPA does not produce empty decoration`() {
        val content = Content(
            id = ContentId("vocab-4"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "cat",
                translatedText = "con mèo",
                pronunciation = null
            )
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)

        assertNull(model.ipa)
    }

    @Test
    fun `5 - Missing part of speech does not produce an empty badge`() {
        val content = Content(
            id = ContentId("vocab-5"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "dog",
                translatedText = "con chó"
            ),
            customFields = ContentCustomFields()
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)

        assertNull(model.partOfSpeech)
    }

    @Test
    fun `6 - One example renders correctly`() {
        val content = Content(
            id = ContentId("vocab-6"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "dog",
                translatedText = "con chó",
                exampleText = "I have a dog.",
                exampleTranslation = "Tôi có một con chó."
            )
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)

        assertEquals(1, model.examples.size)
        assertEquals("I have a dog.", model.examples[0].englishText)
        assertEquals("Tôi có một con chó.", model.examples[0].vietnameseTranslation)
    }

    @Test
    fun `7 - Two examples preserve deterministic order`() {
        val ex1 = FocusedExampleItem(englishText = "First example", vietnameseTranslation = "Ví dụ thứ nhất")
        val ex2 = FocusedExampleItem(englishText = "Second example", vietnameseTranslation = "Ví dụ thứ hai")
        val model = FocusedVocabularyAnswerModel(
            englishWord = "order-test",
            vietnameseMeaning = "kiểm tra thứ tự",
            examples = listOf(ex1, ex2)
        )

        assertEquals(2, model.examples.size)
        assertEquals("First example", model.examples[0].englishText)
        assertEquals("Second example", model.examples[1].englishText)
    }

    @Test
    fun `8 - Scheduler detail is collapsed by default`() {
        val feedback = StudySchedulerFeedback(
            rating = "GOOD",
            stageTransition = "LEARNING -> REVIEW",
            scheduledInterval = "2d",
            nextReviewAt = "Tomorrow",
            difficultyBefore = "5.0",
            difficultyAfter = "4.8",
            stabilityBefore = "1.0",
            stabilityAfter = "3.0",
            reviewCount = 2,
            lapseCount = 0
        )
        val accessibility = resolveStudySchedulerFeedbackAccessibility(feedback)
        assertEquals("GOOD rating; LEARNING -> REVIEW; next interval 2d; next review Tomorrow", accessibility.conciseSummary)
    }

    @Test
    fun `raw combined part of speech and pronunciation are normalized for presentation`() {
        val normalized = normalizePronunciation("/(noun) //hɪl///")

        assertEquals("/hɪl/", normalized.ipa)
        assertEquals("NOUN", normalized.partOfSpeech)
        assertTrue(normalizePronunciation("/rʌn/").ipa == "/rʌn/")
        assertNull(normalizePronunciation("/rʌn/").partOfSpeech)
    }

    @Test
    fun `scheduler intervals use human readable Vietnamese labels`() {
        assertEquals("2 phút", formatVietnameseReviewInterval(120_000))
        assertEquals("1 ngày", formatVietnameseReviewInterval(86_400_000))
        assertEquals("2 ngày", formatVietnameseReviewInterval(172_800_000))
        assertEquals("1 tuần", formatVietnameseReviewInterval(604_800_000))
    }

    @Test
    fun `authoritative roles resolve four audio paths and legacy bilingual example pairing`() {
        val primary = Path.of("word.mp3")
        val meaning = Path.of("meaning-vi.mp3")
        val example = Path.of("example-en.mp3")
        val exampleVi = Path.of("example-vi.mp3")
        val content = Content(
            id = ContentId("vocab-audio"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "hill",
                translatedText = "đồi",
                exampleText = "We climbed the hill.\nChúng tôi đã leo lên ngọn đồi."
            )
        )
        val context = LearningSceneContext(answerRevealed = true)
        val capabilities = SceneCapabilities(true, false, true, true)
        val scene = PromptScene(
            context,
            capabilities,
            listOf(PresentedLearningBlock.Audio(primary, "renamed", "localized", PresentedAudioRole.PRIMARY_WORD)),
            listOf(
                MeaningScene(
                    context,
                    capabilities,
                    listOf(PresentedLearningBlock.Audio(meaning, "renamed", "localized", PresentedAudioRole.MEANING_TRANSLATION))
                ),
                ExampleScene(
                    context,
                    capabilities,
                    listOf(
                        PresentedLearningBlock.Audio(example, "renamed", "localized", PresentedAudioRole.EXAMPLE_PRIMARY),
                        PresentedLearningBlock.Audio(exampleVi, "renamed", "localized", PresentedAudioRole.EXAMPLE_TRANSLATION)
                    )
                )
            )
        )

        val model = FocusedVocabularyAnswerResolver.resolve(StudyUiState(domainContent = content), scene)

        assertEquals(primary, model.primaryAudioPath)
        assertEquals(meaning, model.meaningAudioPath)
        assertEquals(example, model.examples.single().englishAudioPath)
        assertEquals(exampleVi, model.examples.single().vietnameseAudioPath)
        assertEquals("We climbed the hill.", model.examples.single().englishText)
        assertEquals("Chúng tôi đã leo lên ngọn đồi.", model.examples.single().vietnameseTranslation)
    }

    @Test
    fun `part of speech aliases normalize without losing unknown valid values`() {
        val cases = mapOf(
            "N" to "NOUN",
            "n." to "NOUN",
            "V" to "VERB",
            "adj" to "ADJECTIVE",
            "ADV" to "ADVERB",
            "prep" to "PREPOSITION",
            "pron" to "PRONOUN",
            "conj" to "CONJUNCTION",
            "phrasal verb" to "PHRASAL VERB"
        )
        cases.forEach { (raw, expected) ->
            assertEquals(expected, normalizePartOfSpeech(raw), raw)
        }
    }
}
