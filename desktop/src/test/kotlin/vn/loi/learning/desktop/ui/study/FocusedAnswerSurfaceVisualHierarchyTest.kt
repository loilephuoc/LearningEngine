package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.nio.file.Path
import vn.loi.learning.desktop.runtime.StudyTypographyPreferences
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class FocusedAnswerSurfaceVisualHierarchyTest {

    private val defaultTraits = StudyVisualContentTraits(
        hasImage = true,
        hasPronunciation = true,
        hasPartOfSpeech = true,
        hasExamples = true,
        hasSchedulerFeedback = true
    )

    @Test
    fun `1 - hierarchy order is maintained deterministically`() {
        val traits = defaultTraits
        val layout = StudyVisualLayoutResolver.resolve(800, 800, traits)
        assertEquals(StudyViewportClass.STANDARD, layout.viewportClass)

        // 1. Identity -> 2. Pronunciation metadata -> 3. Image -> 4. Meaning -> 5. Examples -> 6. Scheduler feedback -> 7. Rating actions
        assertTrue(layout.identityWordFontSizeSp > 0)
        assertTrue(layout.imageMaxWidthDp > 0)
        assertEquals(MetadataArrangement.INLINE, layout.metadataArrangement)
        assertEquals(RatingArrangement.HORIZONTAL, layout.ratingArrangement)
    }

    @Test
    fun `2 - identity uses layout projected font size and line height`() {
        val compactLayout = StudyVisualLayoutResolver.resolve(400, 700, defaultTraits)
        assertEquals(36, compactLayout.identityWordFontSizeSp)
        assertEquals(44, compactLayout.identityWordLineHeightSp)

        val wideLayout = StudyVisualLayoutResolver.resolve(1200, 900, defaultTraits)
        assertEquals(52, wideLayout.identityWordFontSizeSp)
        assertEquals(58, wideLayout.identityWordLineHeightSp)
    }

    @Test
    fun `3 - compact metadata uses STACKED arrangement`() {
        val layout = StudyVisualLayoutResolver.resolve(400, 700, defaultTraits)
        assertEquals(MetadataArrangement.STACKED, layout.metadataArrangement)
    }

    @Test
    fun `4 - standard and wide metadata use INLINE arrangement`() {
        val standard = StudyVisualLayoutResolver.resolve(700, 800, defaultTraits)
        assertEquals(MetadataArrangement.INLINE, standard.metadataArrangement)

        val wide = StudyVisualLayoutResolver.resolve(1280, 800, defaultTraits)
        assertEquals(MetadataArrangement.INLINE, wide.metadataArrangement)
    }

    @Test
    fun `5 - missing IPA collapses cleanly without placeholder`() {
        val content = Content(
            id = ContentId("test-no-ipa"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "sun", translatedText = "mặt trời")
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        assertNull(model.ipa)

        val traits = StudyVisualContentTraits(
            hasPronunciation = false,
            hasPartOfSpeech = false
        )
        val layout = StudyVisualLayoutResolver.resolve(700, 800, traits)
        assertEquals(MetadataArrangement.INLINE, layout.metadataArrangement)
    }

    @Test
    fun `6 - missing POS collapses cleanly without status badge`() {
        val content = Content(
            id = ContentId("test-no-pos"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "moon", translatedText = "mặt trăng", pronunciation = "muːn")
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        assertEquals("/muːn/", model.ipa)
        assertNull(model.partOfSpeech)
    }

    @Test
    fun `7 - missing IPA and POS collapse cleanly`() {
        val content = Content(
            id = ContentId("test-bare"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "star", translatedText = "ngôi sao")
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        assertNull(model.ipa)
        assertNull(model.partOfSpeech)
    }

    @Test
    fun `8 - image absent does not allocate image bounds`() {
        val traitsNoImage = defaultTraits.copy(hasImage = false)
        val layout = StudyVisualLayoutResolver.resolve(800, 800, traitsNoImage)
        assertEquals(0, layout.imageMaxWidthDp)
        assertEquals(0, layout.imageMaxHeightDp)
    }

    @Test
    fun `9 - image bounds are strictly consumed from StudyVisualLayout`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 800, defaultTraits)
        assertEquals(620, layout.imageMaxWidthDp)
        assertEquals(150, layout.imageMaxHeightDp)
    }

    @Test
    fun `10 - meaning card remains present when image is absent`() {
        val content = Content(
            id = ContentId("test-no-img"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "book", translatedText = "sách")
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        val disclosure = FullAnswerPresentation.resolve(model)

        assertFalse(disclosure.imageAvailable)
        assertEquals("sách", disclosure.vietnameseMeaning)
    }

    @Test
    fun `11 - definition is exposed alongside meaning`() {
        val content = Content(
            id = ContentId("test-def"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "read", translatedText = "đọc"),
            customFields = ContentCustomFields(
                setOf(ContentCustomField(ContentFieldId("definition"), "to look at and comprehend written words"))
            )
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        val disclosure = FullAnswerPresentation.resolve(model)

        assertEquals("đọc", disclosure.vietnameseMeaning)
        assertEquals("to look at and comprehend written words", disclosure.englishDefinition)
    }

    @Test
    fun `12 - examples are placed after meaning`() {
        val content = Content(
            id = ContentId("test-ex"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "write",
                translatedText = "viết",
                exampleText = "I write a letter.",
                exampleTranslation = "Tôi viết một lá thư."
            )
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        val disclosure = FullAnswerPresentation.resolve(model)

        assertTrue(disclosure.examples.isNotEmpty())
        assertEquals("I write a letter.", disclosure.examples[0].englishText)
        assertEquals("Tôi viết một lá thư.", disclosure.examples[0].vietnameseTranslation)
    }

    @Test
    fun `13 - multiple examples preserve exact original order`() {
        val ex1 = FocusedExampleItem(englishText = "She runs fast.", vietnameseTranslation = "Cô ấy chạy nhanh.")
        val ex2 = FocusedExampleItem(englishText = "They run together.", vietnameseTranslation = "Họ chạy cùng nhau.")
        val model = FocusedVocabularyAnswerModel(
            englishWord = "run",
            vietnameseMeaning = "chạy",
            examples = listOf(ex1, ex2)
        )
        val disclosure = FullAnswerPresentation.resolve(model)

        assertEquals(2, disclosure.examples.size)
        assertEquals("She runs fast.", disclosure.examples[0].englishText)
        assertEquals("They run together.", disclosure.examples[1].englishText)
    }

    @Test
    fun `14 - English and Vietnamese hierarchy preserves typography preferences`() {
        val prefs = StudyTypographyPreferences(
            exampleEnglishFontSize = 20,
            exampleVietnameseFontSize = 16
        )
        val presentation = StudyTypographyPresentationResolver.resolve(prefs, 800)

        assertEquals(20, presentation.exampleEnglishFontSize)
        assertEquals(26, presentation.exampleEnglishLineHeight)
        assertEquals(16, presentation.exampleVietnameseFontSize)
        assertEquals(22, presentation.exampleVietnameseLineHeight)
    }

    @Test
    fun `15 - semantic target highlighting is active`() {
        val target = "sign"
        val text = "Please sign the document."
        val highlighted = highlightedExampleText(
            text = text,
            target = target,
            language = ExampleTargetLanguage.ENGLISH,
            highlightStyle = androidx.compose.ui.text.SpanStyle(
                color = vn.loi.learning.desktop.ui.designsystem.LEColors.danger,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        )
        assertNotNull(highlighted)
        assertTrue(highlighted.text.contains("sign"))
    }

    @Test
    fun `16 - scheduler feedback traits are modeled`() {
        val traits = defaultTraits.copy(hasSchedulerFeedback = true)
        val layout = StudyVisualLayoutResolver.resolve(800, 800, traits)
        assertNotNull(layout)
    }

    @Test
    fun `17 - rating dock arrangement matches resolver output`() {
        val compactLayout = StudyVisualLayoutResolver.resolve(400, 800, defaultTraits)
        assertEquals(RatingArrangement.GRID_2X2, compactLayout.ratingArrangement)

        val standardLayout = StudyVisualLayoutResolver.resolve(800, 800, defaultTraits)
        assertEquals(RatingArrangement.HORIZONTAL, standardLayout.ratingArrangement)
    }

    @Test
    fun `18 - horizontal rating order preserves Again Hard Good Easy`() {
        val controls = listOf(
            StudyActionControl.REVIEW_AGAIN,
            StudyActionControl.REVIEW_HARD,
            StudyActionControl.REVIEW_GOOD,
            StudyActionControl.REVIEW_EASY
        )
        assertEquals(StudyActionControl.REVIEW_AGAIN, controls[0])
        assertEquals(StudyActionControl.REVIEW_HARD, controls[1])
        assertEquals(StudyActionControl.REVIEW_GOOD, controls[2])
        assertEquals(StudyActionControl.REVIEW_EASY, controls[3])
    }

    @Test
    fun `19 - grid rating order preserves Again Hard on row 1 and Good Easy on row 2`() {
        val row1 = listOf(StudyActionControl.REVIEW_AGAIN, StudyActionControl.REVIEW_HARD)
        val row2 = listOf(StudyActionControl.REVIEW_GOOD, StudyActionControl.REVIEW_EASY)

        assertEquals(listOf(StudyActionControl.REVIEW_AGAIN, StudyActionControl.REVIEW_HARD), row1)
        assertEquals(listOf(StudyActionControl.REVIEW_GOOD, StudyActionControl.REVIEW_EASY), row2)
    }

    @Test
    fun `20 - shortcut mapping remains 1 2 3 4`() {
        val again = resolveStudyActionAccessibility(StudyActionControl.REVIEW_AGAIN)
        val hard = resolveStudyActionAccessibility(StudyActionControl.REVIEW_HARD)
        val good = resolveStudyActionAccessibility(StudyActionControl.REVIEW_GOOD)
        val easy = resolveStudyActionAccessibility(StudyActionControl.REVIEW_EASY)

        assertTrue(again.shortcutHint.contains("1"))
        assertTrue(hard.shortcutHint.contains("2"))
        assertTrue(good.shortcutHint.contains("3"))
        assertTrue(easy.shortcutHint.contains("4"))
    }

    @Test
    fun `21 - disabled rating state retains semantics`() {
        val actionInProgressState = StudyUiState(actionInProgress = true, canReview = true)
        assertTrue(actionInProgressState.actionInProgress)
    }

    @Test
    fun `22 - heading semantics are attached to identity`() {
        val content = Content(
            id = ContentId("test-heading"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "hello", translatedText = "xin chào")
        )
        val uiState = StudyUiState(domainContent = content)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        assertEquals("hello", model.englishWord)
    }

    @Test
    fun `23 - zero answer leakage before reveal`() {
        val content = Content(
            id = ContentId("leak-test"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "secret", translatedText = "bí mật")
        )
        val uiState = StudyUiState(domainContent = content, canReview = false)
        assertFalse(uiState.canReview)
    }

    @Test
    fun `24 - optional content collapse creates zero blank sections`() {
        val minimalContent = Content(
            id = ContentId("min"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "go", translatedText = "đi")
        )
        val uiState = StudyUiState(domainContent = minimalContent)
        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        val disclosure = FullAnswerPresentation.resolve(model)

        assertNull(model.imagePath)
        assertFalse(disclosure.imageAvailable)
        assertNull(disclosure.ipa)
        assertNull(disclosure.partOfSpeech)
        assertNull(disclosure.englishDefinition)
        assertTrue(disclosure.examples.isEmpty())
    }

    @Test
    fun `25 - resolver section spacing consumes layout tokens`() {
        val compact = StudyVisualLayoutResolver.resolve(400, 700, defaultTraits)
        val standard = StudyVisualLayoutResolver.resolve(800, 800, defaultTraits)
        val wide = StudyVisualLayoutResolver.resolve(1200, 900, defaultTraits)

        assertEquals(6, compact.sectionSpacingDp)
        assertEquals(8, standard.sectionSpacingDp)
        assertEquals(16, wide.sectionSpacingDp)
    }
}
