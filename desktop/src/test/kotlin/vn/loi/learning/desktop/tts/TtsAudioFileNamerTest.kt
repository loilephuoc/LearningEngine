package vn.loi.learning.desktop.tts

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TtsAudioFileNamerTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-08-22T04:44:23.184Z"),
        ZoneOffset.UTC
    )

    private val namer = TtsAudioFileNamer(fixedClock)

    @Test
    fun `formatTimestamp produces exact millisecond timestamp`() {
        val formatted = namer.formatTimestamp()
        assertEquals("20260822_04-44-23-184", formatted)
    }

    @Test
    fun `buildBaseFileName formats question field with en language`() {
        val fileName = namer.buildBaseFileName(
            contentId = "1842",
            field = TtsField.QUESTION,
            languageCode = "en"
        )
        assertEquals("1842_question_en_20260822_04-44-23-184.mp3", fileName)
    }

    @Test
    fun `buildBaseFileName formats example field with en language`() {
        val fileName = namer.buildBaseFileName(
            contentId = "1842",
            field = TtsField.EXAMPLE,
            languageCode = "en"
        )
        assertEquals("1842_example_en_20260822_04-44-23-184.mp3", fileName)
    }

    @Test
    fun `buildBaseFileName formats translation field with vi language`() {
        val fileName = namer.buildBaseFileName(
            contentId = "1842",
            field = TtsField.TRANSLATION,
            languageCode = "vi"
        )
        assertEquals("1842_translation_vi_20260822_04-44-23-184.mp3", fileName)
    }

    @Test
    fun `buildBaseFileName formats answer field with en language`() {
        val fileName = namer.buildBaseFileName(
            contentId = "1842",
            field = TtsField.ANSWER,
            languageCode = "en"
        )
        assertEquals("1842_answer_en_20260822_04-44-23-184.mp3", fileName)
    }

    @Test
    fun `generateUniqueFileName returns base name when no collision exists`() {
        val fileName = namer.generateUniqueFileName(
            contentId = "c100",
            field = TtsField.QUESTION,
            languageCode = "en",
            exists = { false }
        )
        assertEquals("c100_question_en_20260822_04-44-23-184.mp3", fileName)
    }

    @Test
    fun `generateUniqueFileName resolves collision by appending suffix`() {
        val existingFiles = setOf(
            "c100_question_en_20260822_04-44-23-184.mp3",
            "c100_question_en_20260822_04-44-23-184_2.mp3"
        )

        val fileName = namer.generateUniqueFileName(
            contentId = "c100",
            field = TtsField.QUESTION,
            languageCode = "en",
            exists = { it in existingFiles }
        )

        assertEquals("c100_question_en_20260822_04-44-23-184_3.mp3", fileName)
    }

    @Test
    fun `sanitizes weird tokens in contentId`() {
        val fileName = namer.buildBaseFileName(
            contentId = "content/item #1:test",
            field = TtsField.QUESTION,
            languageCode = "EN"
        )
        assertEquals("content_item__1_test_question_en_20260822_04-44-23-184.mp3", fileName)
    }

    @Test
    fun `rejects blank contentId or languageCode`() {
        assertFailsWith<IllegalArgumentException> {
            namer.buildBaseFileName("", TtsField.QUESTION, "en")
        }
        assertFailsWith<IllegalArgumentException> {
            namer.buildBaseFileName("123", TtsField.QUESTION, "   ")
        }
    }
}
