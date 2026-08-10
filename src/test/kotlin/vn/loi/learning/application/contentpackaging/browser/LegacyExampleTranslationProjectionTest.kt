package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LegacyExampleTranslationProjectionTest {
    @Test
    fun `separate schema fields remain authoritative and normalized`() {
        val result = LegacyExampleTranslationProjection.project(
            "  Rugby is a tough sport.  ",
            "  Bóng bầu dục là một môn thể thao mạnh mẽ.  "
        )
        assertEquals("Rugby is a tough sport.", result.exampleText)
        assertEquals("Bóng bầu dục là một môn thể thao mạnh mẽ.", result.exampleTranslation)
    }

    @Test
    fun `legacy newline pair splits only with clear Vietnamese signal`() {
        val result = LegacyExampleTranslationProjection.project(
            "Rugby is a tough sport.\n\n  Bóng bầu dục là một môn thể thao mạnh mẽ. ",
            null
        )
        assertEquals("Rugby is a tough sport.", result.exampleText)
        assertEquals("Bóng bầu dục là một môn thể thao mạnh mẽ.", result.exampleTranslation)
    }

    @Test
    fun `multiline English remains one example`() {
        val raw = "This is a long English example.\nIt continues on another line."
        val result = LegacyExampleTranslationProjection.project(raw, null)
        assertEquals(raw, result.exampleText)
        assertNull(result.exampleTranslation)
    }

    @Test
    fun `blank values do not create empty surfaces`() {
        assertEquals(
            LegacyExampleTranslationProjection.SplitExample(null, null),
            LegacyExampleTranslationProjection.project(" \n ", "  ")
        )
    }

    @Test
    fun `blank translation preserves normalized English whitespace`() {
        val result = LegacyExampleTranslationProjection.project("  One English example.  ", " ")
        assertEquals("One English example.", result.exampleText)
        assertNull(result.exampleTranslation)
    }
}
