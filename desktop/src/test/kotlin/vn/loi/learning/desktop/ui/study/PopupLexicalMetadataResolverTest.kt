package vn.loi.learning.desktop.ui.study

import kotlin.test.*

class PopupLexicalMetadataResolverTest {
    @Test
    fun `IPA and POS remain in source order with delimiters normalized once`() {
        val result = PopupLexicalMetadataResolver.resolve("mjuː.zɪk", "NOUN")

        assertEquals("/mjuː.zɪk/", result.ipa)
        assertEquals("NOUN", result.partOfSpeech)
        assertTrue(result.visible)
    }

    @Test
    fun `IPA only has no reserved POS value`() {
        val result = PopupLexicalMetadataResolver.resolve("/mjuː.zɪk/", null)

        assertEquals("/mjuː.zɪk/", result.ipa)
        assertNull(result.partOfSpeech)
    }

    @Test
    fun `POS only has no reserved IPA value`() {
        val result = PopupLexicalMetadataResolver.resolve(null, "NOUN")

        assertNull(result.ipa)
        assertEquals("NOUN", result.partOfSpeech)
    }

    @Test
    fun `missing IPA and POS hides lexical row`() {
        assertFalse(PopupLexicalMetadataResolver.resolve(null, null).visible)
    }

    @Test
    fun `blank IPA and POS behave as missing`() {
        val result = PopupLexicalMetadataResolver.resolve("  ", "  ")

        assertNull(result.ipa)
        assertNull(result.partOfSpeech)
        assertFalse(result.visible)
    }

    @Test
    fun `accessibility labels are localized without changing IPA`() {
        val ipa = "/mjuː.zɪk/"
        assertEquals("Pronunciation: $ipa", StudyWorkspaceStrings.ENGLISH.typingPronunciationAccessibility(ipa))
        assertEquals("Part of speech: NOUN", StudyWorkspaceStrings.ENGLISH.typingPartOfSpeechAccessibility("NOUN"))
    }
}
