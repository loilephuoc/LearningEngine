package vn.loi.learning.adapter.jvm

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Adapter JVM dùng để đọc text từ file.
 *
 * Hỗ trợ:
 * - UTF-8
 * - UTF-8 BOM
 * - UTF-16 LE/BE BOM
 * - fallback Windows-1258 cho dữ liệu tiếng Việt cũ
 *
 * Domain và Application không biết java.nio.file.Path hoặc Charset.
 */
class JvmJsonFileReader {

    fun read(path: Path): String {
        require(Files.exists(path)) {
            "File does not exist: $path"
        }

        require(Files.isRegularFile(path)) {
            "Path is not a regular file: $path"
        }

        val bytes = Files.readAllBytes(path)

        return when {
            bytes.hasUtf8Bom() ->
                String(
                    bytes,
                    UTF8_BOM_SIZE,
                    bytes.size - UTF8_BOM_SIZE,
                    StandardCharsets.UTF_8
                )

            bytes.hasUtf16LeBom() ->
                String(
                    bytes,
                    UTF16_BOM_SIZE,
                    bytes.size - UTF16_BOM_SIZE,
                    StandardCharsets.UTF_16LE
                )

            bytes.hasUtf16BeBom() ->
                String(
                    bytes,
                    UTF16_BOM_SIZE,
                    bytes.size - UTF16_BOM_SIZE,
                    StandardCharsets.UTF_16BE
                )

            else ->
                decodeWithoutBom(bytes)
        }
    }

    private fun decodeWithoutBom(bytes: ByteArray): String {
        val utf8Text = String(bytes, StandardCharsets.UTF_8)

        /*
         * Ký tự thay thế � xuất hiện khi byte không hợp lệ trong UTF-8.
         * Khi đó thử Windows-1258, thường gặp ở dữ liệu tiếng Việt cũ.
         */
        return if (REPLACEMENT_CHARACTER in utf8Text) {
            String(bytes, WINDOWS_1258)
        } else {
            utf8Text
        }
    }

    private fun ByteArray.hasUtf8Bom(): Boolean =
        size >= UTF8_BOM_SIZE &&
                this[0] == 0xEF.toByte() &&
                this[1] == 0xBB.toByte() &&
                this[2] == 0xBF.toByte()

    private fun ByteArray.hasUtf16LeBom(): Boolean =
        size >= UTF16_BOM_SIZE &&
                this[0] == 0xFF.toByte() &&
                this[1] == 0xFE.toByte()

    private fun ByteArray.hasUtf16BeBom(): Boolean =
        size >= UTF16_BOM_SIZE &&
                this[0] == 0xFE.toByte() &&
                this[1] == 0xFF.toByte()

    private companion object {
        const val UTF8_BOM_SIZE = 3
        const val UTF16_BOM_SIZE = 2
        const val REPLACEMENT_CHARACTER = '\uFFFD'

        val WINDOWS_1258: Charset =
            Charset.forName("windows-1258")
    }
}