package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode

class StudyScenePresentationTest {
    private val vietnameseCue =
        PresentedLearningBlock.Text(SafeMarkdownDocument.plain("nghĩa tiếng Việt"))
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
            listOf(vietnameseCue, englishAudio, vietnameseAudio),
            SceneType.PROMPT,
            manualVietnamese,
            answerRevealed = false
        )

        assertTrue(vietnameseCue in visible)
        assertTrue(vietnameseAudio in visible)
        assertFalse(englishAudio in visible)
    }

    @Test
    fun `reveal keeps the same language policy while renderer selects answer layers`() {
        val meaningVisible = visibleStudySceneBlocks(
            listOf(vietnameseCue, vietnameseAudio),
            SceneType.MEANING,
            manualVietnamese,
            answerRevealed = true
        )
        val englishVisible = visibleStudySceneBlocks(
            listOf(englishAudio),
            SceneType.PROMPT,
            manualVietnamese,
            answerRevealed = true
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
                SceneType.PROMPT,
                EffectiveStudyPresentation.UNRESTRICTED,
                answerRevealed = false
            )
        )
    }
}
