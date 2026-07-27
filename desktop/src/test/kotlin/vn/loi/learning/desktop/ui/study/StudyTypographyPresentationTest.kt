package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyTypographyPreferences

class StudyTypographyPresentationTest {
    @Test
    fun `maps english and vietnamese preferences to their presentation roles`() {
        val presentation = StudyTypographyPresentationResolver.resolve(
            StudyTypographyPreferences(
                exampleEnglishFontSize = 24,
                exampleVietnameseFontSize = 18
            ),
            viewportWidthDp = 900
        )

        assertEquals(24, presentation.exampleEnglishFontSize)
        assertEquals(31, presentation.exampleEnglishLineHeight)
        assertEquals(18, presentation.exampleVietnameseFontSize)
        assertEquals(24, presentation.exampleVietnameseLineHeight)
    }

    @Test
    fun `fullscreen and narrow viewports never shrink below configured base and allow wrapping`() {
        val preferences = StudyTypographyPreferences(
            exampleEnglishFontSize = 28,
            exampleVietnameseFontSize = 24
        )

        val fullscreen = StudyTypographyPresentationResolver.resolve(preferences, 1920)
        val narrow = StudyTypographyPresentationResolver.resolve(preferences, 320)

        assertEquals(28, fullscreen.exampleEnglishFontSize)
        assertEquals(24, fullscreen.exampleVietnameseFontSize)
        assertEquals(fullscreen, narrow)
        assertTrue(narrow.softWrap)
    }

    @Test
    fun `preview uses exact learner examples and resolver mapping`() {
        val preview = resolveStudyTypographyPreview(
            StudyTypographyPreferences(
                exampleEnglishFontSize = 22,
                exampleVietnameseFontSize = 17
            ),
            viewportWidthDp = 480
        )

        assertEquals("There are many homeless people.", preview.englishText)
        assertEquals("Có rất nhiều người vô gia cư.", preview.vietnameseText)
        assertEquals(22, preview.typography.exampleEnglishFontSize)
        assertEquals(17, preview.typography.exampleVietnameseFontSize)
    }

    @Test
    fun `rejects invalid preferences and viewport`() {
        assertFailsWith<IllegalArgumentException> {
            StudyTypographyPreferences(exampleEnglishFontSize = 15)
        }
        assertFailsWith<IllegalArgumentException> {
            StudyTypographyPreferences(exampleVietnameseFontSize = 27)
        }
        assertFailsWith<IllegalArgumentException> {
            StudyTypographyPresentationResolver.resolve(StudyTypographyPreferences(), -1)
        }
    }
}
