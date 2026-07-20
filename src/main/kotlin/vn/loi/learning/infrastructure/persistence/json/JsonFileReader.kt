package vn.loi.learning.infrastructure.persistence.json

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.SerializationException

/**
 * Chuẩn hóa việc đọc JSON persistence.
 *
 * Quy ước:
 * - file không tồn tại: trả emptyValue;
 * - file rỗng hoặc chỉ có whitespace: trả emptyValue;
 * - JSON không hợp lệ: ném InvalidJsonPersistenceException;
 * - lỗi filesystem và I/O: giữ nguyên exception gốc.
 */
internal object JsonFileReader {

    fun <T> read(
        filePath: Path,
        emptyValue: T,
        decode: (String) -> T
    ): T {
        if (
            Files.notExists(
                filePath
            )
        ) {
            return emptyValue
        }

        val content =
            Files.readString(
                filePath,
                StandardCharsets.UTF_8
            )

        if (
            content.isBlank()
        ) {
            return emptyValue
        }

        return try {
            decode(
                content
            )
        } catch (
            failure: SerializationException
        ) {
            throw InvalidJsonPersistenceException(
                filePath =
                    filePath,
                cause =
                    failure
            )
        }
    }
}