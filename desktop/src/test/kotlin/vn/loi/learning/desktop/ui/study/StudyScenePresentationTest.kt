package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode

class StudyScenePresentationTest {
    private val englishIdentity =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("word"),
            PresentedTextRole.PRIMARY_ENGLISH
        )
    private val vietnameseCue =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("nghĩa tiếng Việt"),
            PresentedTextRole.VIETNAMESE_MEANING
        )
    private val englishExample =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("English example"),
            PresentedTextRole.ENGLISH_EXAMPLE
        )
    private val vietnameseExample =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("Ví dụ tiếng Việt"),
            PresentedTextRole.VIETNAMESE_EXAMPLE
        )
    private val instruction =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("Listen carefully"),
            PresentedTextRole.INSTRUCTION
        )
    private val neutral =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("/wɜːd/"),
            PresentedTextRole.NEUTRAL
        )
    private val englishAudio =
        PresentedLearningBlock.Audio(
            Path.of("english.mp3"),
            "English",
            "English",
            PresentedAudioRole.PRIMARY_WORD
        )
    private val vietnameseAudio =
        PresentedLearningBlock.Audio(
            Path.of("vietnamese.mp3"),
            "Vietnamese",
            "Vietnamese",
            PresentedAudioRole.MEANING_TRANSLATION
        )
    private val manualVietnamese = EffectiveStudyPresentation(
        controlMode = StudyPresentationControlMode.MANUAL,
        showPrimaryEnglish = false,
        showVietnameseMeaning = true,
        showEnglishExamples = false,
        showVietnameseExamples = true,
        autoplayPrimaryEnglish = false,
        autoplayVietnameseMeaning = true,
        autoplayEnglishExample = false,
        autoplayVietnameseExample = true
    )

    @Test
    fun `question renderer keeps Vietnamese and removes English in manual mode`() {
        val visible = visibleStudySceneBlocks(
            listOf(
                englishIdentity,
                vietnameseCue,
                englishExample,
                vietnameseExample,
                instruction,
                neutral,
                englishAudio,
                vietnameseAudio
            ),
            manualVietnamese
        )

        assertFalse(englishIdentity in visible)
        assertTrue(vietnameseCue in visible)
        assertFalse(englishExample in visible)
        assertTrue(vietnameseExample in visible)
        assertTrue(instruction in visible)
        assertTrue(neutral in visible)
        assertTrue(vietnameseAudio in visible)
        assertFalse(englishAudio in visible)
    }

    @Test
    fun `manual English visibility is the direct inverse without hiding neutral text`() {
        val manualEnglish = manualVietnamese.copy(
            showPrimaryEnglish = true,
            showVietnameseMeaning = false,
            showEnglishExamples = true,
            showVietnameseExamples = false
        )

        val visible = visibleStudySceneBlocks(
            listOf(
                englishIdentity,
                vietnameseCue,
                englishExample,
                vietnameseExample,
                instruction,
                neutral
            ),
            manualEnglish
        )

        assertTrue(englishIdentity in visible)
        assertFalse(vietnameseCue in visible)
        assertTrue(englishExample in visible)
        assertFalse(vietnameseExample in visible)
        assertTrue(instruction in visible)
        assertTrue(neutral in visible)
    }

    @Test
    fun `question visibility remains preference driven before full answer disclosure`() {
        val meaningVisible = visibleStudySceneBlocks(
            listOf(vietnameseCue, vietnameseAudio),
            manualVietnamese
        )
        val englishVisible = visibleStudySceneBlocks(
            listOf(englishAudio),
            manualVietnamese
        )

        assertEquals(listOf(vietnameseCue, vietnameseAudio), meaningVisible)
        assertTrue(englishVisible.isEmpty())
    }

    @Test
    fun `adaptive renderer preserves all available scene blocks`() {
        assertEquals(
            listOf(vietnameseCue, englishAudio, vietnameseAudio),
            visibleStudySceneBlocks(
                listOf(vietnameseCue, englishAudio, vietnameseAudio),
                EffectiveStudyPresentation.UNRESTRICTED
            )
        )
    }
}
