package vn.loi.learning.infrastructure.persistence.json

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.Test

class PortableTextFileReaderTest {
    @Test
    fun `reads unicode and large existing utf eight content without changing bytes`() {
        val content = "cái giường — xà phòng\r\n".repeat(20_000)
        val stream = TrackingInputStream(content.toByteArray(StandardCharsets.UTF_8))

        val decoded = PortableTextFileReader.read(Path.of("existing.json")) { stream }

        assertEquals(content, decoded)
        assertTrue(stream.closed)
    }

    @Test
    fun `honors the requested charset`() {
        val content = "Tiếng Việt"
        val decoded = PortableTextFileReader.read(
            Path.of("utf16.json"), StandardCharsets.UTF_16LE
        ) { ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_16LE)) }
        assertEquals(content, decoded)
    }

    @Test
    fun `closes stream when reading fails and propagates original failure`() {
        val expected = IOException("simulated read failure")
        val stream = FailingInputStream(expected)

        val actual = assertFailsWith<IOException> {
            PortableTextFileReader.read(Path.of("broken.json")) { stream }
        }

        assertEquals(expected, actual)
        assertTrue(stream.closed)
    }

    private class TrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false
        override fun close() { closed = true; super.close() }
    }

    private class FailingInputStream(private val failure: IOException) : InputStream() {
        var closed = false
        override fun read(): Int = throw failure
        override fun close() { closed = true }
    }
}
