package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.SpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleTargetHighlightingTest {
    @Test
    fun `English target matching is case insensitive and supports multiple occurrences`() {
        val text = "A DISASTER can follow another disaster."

        assertEquals(
            listOf(
                ExampleTargetMatch(2, 10),
                ExampleTargetMatch(30, 38)
            ),
            resolveExampleTargetMatches(text, "disaster", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `English target requires word boundaries`() {
        val text = "A disaster is not highlighted inside disastrous wording."

        assertEquals(
            listOf(ExampleTargetMatch(2, 10)),
            resolveExampleTargetMatches(text, "disaster", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `multi-word English target with punctuation matches accurately`() {
        val text = "Please sign here at the bottom."

        assertEquals(
            listOf(ExampleTargetMatch(17, 31)),
            resolveExampleTargetMatches(text, "at the bottom.", ExampleTargetLanguage.ENGLISH)
        )
        assertEquals(
            listOf(ExampleTargetMatch(17, 30)),
            resolveExampleTargetMatches(text, "at the bottom", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `multi-word English target uses exact phrase boundaries`() {
        val text = "Many homeless people help other homeless people."

        assertEquals(
            listOf(
                ExampleTargetMatch(5, 20),
                ExampleTargetMatch(32, 47)
            ),
            resolveExampleTargetMatches(text, "homeless people", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `Vietnamese phrase matching is case-insensitive and supports multiple occurrences`() {
        val text = "THẢM HỌA này là một thảm họa lớn."

        assertEquals(
            listOf(
                ExampleTargetMatch(0, 8),
                ExampleTargetMatch(20, 28)
            ),
            resolveExampleTargetMatches(text, "thảm họa", ExampleTargetLanguage.VIETNAMESE)
        )
    }

    @Test
    fun `semantic inflections are not guessed`() {
        assertTrue(resolveExampleTargetMatches("She wrote it.", "write", ExampleTargetLanguage.ENGLISH).isEmpty())
        assertTrue(resolveExampleTargetMatches("He went home.", "go", ExampleTargetLanguage.ENGLISH).isEmpty())
    }

    @Test
    fun `English infinitive verb target is normalized to canonical verb`() {
        val signText = "Please sign here at the bottom."
        assertEquals(
            listOf(ExampleTargetMatch(7, 11)),
            resolveExampleTargetMatches(signText, "to sign", ExampleTargetLanguage.ENGLISH)
        )

        val studyText = "Students need to study every day."
        assertEquals(
            listOf(ExampleTargetMatch(14, 22)),
            resolveExampleTargetMatches(studyText, "to study", ExampleTargetLanguage.ENGLISH)
        )

        val workText = "She works hard for her family."
        assertTrue(resolveExampleTargetMatches(workText, "to work", ExampleTargetLanguage.ENGLISH).isEmpty())
    }

    @Test
    fun `English infinitive normalization does not match partial substring in non-target word`() {
        val signatureText = "Please check signature"
        assertTrue(
            resolveExampleTargetMatches(signatureText, "to sign", ExampleTargetLanguage.ENGLISH).isEmpty()
        )
    }

    @Test
    fun `uncertain substring does not highlight and raw text is unchanged`() {
        val original = "This was disastrous."

        val annotated = highlightedExampleText(
            text = original,
            target = "disaster",
            language = ExampleTargetLanguage.ENGLISH,
            highlightStyle = SpanStyle()
        )

        assertEquals(original, annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
        assertEquals("This was disastrous.", original)
    }

    @Test
    fun `apostrophe variants whitespace punctuation case and exact original ranges are supported`() {
        val cases = listOf(
            Triple("I don't mind. It’s all the same to me", "I don't mind", "I don't mind"),
            Triple("I don’t mind.", "I don't mind", "I don’t mind"),
            Triple("I don't mind.", "I don’t mind", "I don't mind"),
            Triple("A TRAFFIC WARDEN arrived.", "traffic warden", "TRAFFIC WARDEN"),
            Triple("Please, take off!", "take off", "take off"),
            Triple("Please\tlook\nafter it.", "look after", "look\nafter"),
            Triple("Her mother—in—law agreed.", "mother-in-law", "mother—in—law"),
            Triple("\"I can't\", she said.", "can't", "can't"),
            Triple("He doesn't.", "doesn’t", "doesn't"),
            Triple("I'm ready.", "I`m", "I'm"),
            Triple("(ice cream), then ice cream.", "ice cream", "ice cream"),
            Triple("A phrasal verb.", "phrasal verb", "phrasal verb"),
            Triple("The innocent foal.", "innocent", "innocent")
        )
        cases.forEach { (text, key, expected) ->
            val matches = resolveExampleTargetMatches(text, key, ExampleTargetLanguage.ENGLISH)
            assertTrue(matches.isNotEmpty(), "$key should match $text")
            matches.forEach { assertEquals(expected, text.substring(it.start, it.endExclusive)) }
        }
    }

    @Test
    fun `repeated phrases and longest overlapping candidate win`() {
        val repeated = "Take off, then take off."
        assertEquals(
            listOf(ExampleTargetMatch(0, 8), ExampleTargetMatch(15, 23)),
            resolveExampleTargetMatches(repeated, "take off", ExampleTargetLanguage.ENGLISH)
        )
        assertEquals(
            listOf(ExampleTargetMatch(0, 14)),
            LearningKeyMatcher.resolve(
                "traffic warden",
                listOf("traffic", "traffic warden", "warden"),
                LearningKeyMatchingPolicy(ExampleTargetLanguage.ENGLISH)
            )
        )
    }

    @Test
    fun `unicode composition maps Vietnamese match to original display text`() {
        val decomposed = "To\u0302i ye\u0302u tie\u0302\u0301ng Vie\u0323\u0302t."
        val match = resolveExampleTargetMatches(
            decomposed,
            "tôi yêu tiếng Việt",
            ExampleTargetLanguage.VIETNAMESE
        ).single()
        assertEquals(decomposed.removeSuffix("."), decomposed.substring(match.start, match.endExclusive))
        assertTrue(resolveExampleTargetMatches("Tôi yêu Việt Nam.", "toi", ExampleTargetLanguage.VIETNAMESE).isEmpty())
    }

    @Test
    fun `blank absent malformed surrogate and inner word targets are safe`() {
        assertTrue(resolveExampleTargetMatches("theme other", "he", ExampleTargetLanguage.ENGLISH).isEmpty())
        assertTrue(resolveExampleTargetMatches("text", "", ExampleTargetLanguage.ENGLISH).isEmpty())
        assertTrue(resolveExampleTargetMatches("text", "absent", ExampleTargetLanguage.ENGLISH).isEmpty())
        assertTrue(resolveExampleTargetMatches("\uD800 text", "\uD800", ExampleTargetLanguage.ENGLISH).isNotEmpty())
    }
}
