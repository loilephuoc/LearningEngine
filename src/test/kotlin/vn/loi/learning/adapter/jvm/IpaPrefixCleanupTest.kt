package vn.loi.learning.adapter.jvm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IpaPrefixCleanupTest {
    @Test
    fun `removes only a leading parenthesized POS prefix`() {
        mapOf(
            "(Noun) - /ˈaʊt.breɪk/" to "/ˈaʊt.breɪk/",
            "(noun)-/ˈaʊt.breɪk/" to "/ˈaʊt.breɪk/",
            "(Noun)    -    /ˈaʊt.breɪk/" to "/ˈaʊt.breɪk/",
            "(noun) -prɪˈzentə(r)" to "prɪˈzentə(r)",
            "(adverb) -tə ˈbuːt" to "tə ˈbuːt",
            "(verb)-məʊn" to "məʊn",
            "(noun) - ˈsʌbɜːb/" to "ˈsʌbɜːb/"
        ).forEach { (before, after) -> assertEquals(after, IpaPrefixCleanup.clean(before)) }
    }

    @Test
    fun `removes a leading POS prefix separated by whitespace without normalizing IPA`() {
        mapOf(
            "(adv) /ˌæbsəˈluːtli/" to "/ˌæbsəˈluːtli/",
            "(adverb) //ˈɔːlsəʊ//" to "//ˈɔːlsəʊ//",
            "(phrase) d'uː wʌnz hɛə" to "d'uː wʌnz hɛə",
            "(adverb) ˈfriːkwəntli" to "ˈfriːkwəntli"
        ).forEach { (before, after) -> assertEquals(after, IpaPrefixCleanup.clean(before)) }
    }

    @Test
    fun `leaves prefix-free and structurally unmatched values untouched`() {
        listOf(
            "/niːd/",
            "ˈfriːkwəntli",
            "prɪˈzentə(r)",
            "word(noun)",
            "before (noun) after",
            "",
            "twen-ti-one"
        )
            .forEach { assertNull(IpaPrefixCleanup.clean(it)) }
    }

    @Test
    fun `trims only outer whitespace from captured IPA`() {
        assertEquals("tə  ˈbuːt", IpaPrefixCleanup.clean("  (adverb) -  tə  ˈbuːt  "))
    }
}
