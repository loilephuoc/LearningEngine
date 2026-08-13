package vn.loi.learning.android.study

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class AndroidPronunciationPresentationTest {
    @Test
    fun `legacy delimiter variants share one canonical presentation`() {
        val expected = "/əˈkaʊnt/"
        listOf(
            "(noun) //əˈkaʊnt//",
            "(noun) //əˈkaʊnt/",
            "(noun) /əˈkaʊnt/",
            "noun //əˈkaʊnt//",
            "//əˈkaʊnt//",
            "//əˈkaʊnt/",
            "/əˈkaʊnt/",
            "əˈkaʊnt"
        ).forEach { input ->
            assertEquals(expected, normalizedIntroductionPronunciation("noun", input), input)
        }
    }

    @Test
    fun `phrases and unicode heavy IPA retain their phonetic payload`() {
        assertEquals(
            "/ˈækʃən fɪlm/",
            normalizedIntroductionPronunciation("noun", "//ˈækʃən fɪlm/")
        )
        assertEquals(
            "/ˌɪntəˈnæʃənəl ɔːˈθɒrəti ŋ ʊ/",
            normalizedIntroductionPronunciation(null, "///ˌɪntəˈnæʃənəl ɔːˈθɒrəti ŋ ʊ//")
        )
    }

    @Test
    fun `legacy specific POS descriptors are removed even when canonical POS is generic`() {
        assertEquals(
            "/ˈstreɪliən/",
            normalizedIntroductionPronunciation("word", "(adjective/noun) //ˈstreɪliən/")
        )
        assertEquals(
            "/əˈkaʊnt/",
            normalizedIntroductionPronunciation("word", "(noun) //əˈkaʊnt//")
        )
        assertEquals(
            "/ˈeniweə/",
            normalizedIntroductionPronunciation(null, "(adverb) /ˈeniweə/")
        )
    }

    @Test
    fun `parenthesized phonetic payload is not mistaken for legacy POS metadata`() {
        assertEquals("/(r)ed/", normalizedIntroductionPronunciation("word", "(r)ed"))
    }

    @Test
    fun `missing pronunciation has no presentation`() {
        assertNull(normalizedIntroductionPronunciation("noun", null))
        assertNull(normalizedIntroductionPronunciation("noun", "   "))
        assertNull(normalizedIntroductionPronunciation("noun", " //// "))
    }
}
