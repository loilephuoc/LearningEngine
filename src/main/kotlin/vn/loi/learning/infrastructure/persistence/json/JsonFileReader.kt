package vn.loi.learning.infrastructure.persistence.json

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

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
        recordType: String? = null,
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
            throw InvalidJsonPersistenceException(
                filePath =
                    filePath,
                cause =
                    IllegalStateException(
                        "JSON persistence file is blank"
                    ),
                failureKind =
                    JsonPersistenceFailureKind.BLANK,
                recordType =
                    recordType
            )
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
                    failure,
                failureKind =
                    classifyFailure(
                        content =
                            content
                    ),
                recordType =
                    recordType
            )
        }
    }

    private fun classifyFailure(
        content: String
    ): JsonPersistenceFailureKind {
        try {
            Json.parseToJsonElement(
                content
            )

            return JsonPersistenceFailureKind.INVALID_SHAPE
        } catch (
            ignored: SerializationException
        ) {
            // The original decoder failure remains the public cause.
        }

        return if (
            endsWithIncompleteStructure(
                content
            )
        ) {
            JsonPersistenceFailureKind.TRUNCATED
        } else {
            JsonPersistenceFailureKind.MALFORMED
        }
    }

    private fun endsWithIncompleteStructure(
        content: String
    ): Boolean {
        val expectedClosings =
            ArrayDeque<Char>()

        var insideString =
            false

        var escaped =
            false

        content.forEach { character ->
            if (
                insideString
            ) {
                when {
                    escaped ->
                        escaped = false

                    character == '\\' ->
                        escaped = true

                    character == '"' ->
                        insideString = false
                }

                return@forEach
            }

            when (
                character
            ) {
                '"' ->
                    insideString = true

                '{' ->
                    expectedClosings.addLast(
                        '}'
                    )

                '[' ->
                    expectedClosings.addLast(
                        ']'
                    )

                '}', ']' -> {
                    if (
                        expectedClosings.lastOrNull() != character
                    ) {
                        return false
                    }

                    expectedClosings.removeLast()
                }
            }
        }

        return insideString ||
                escaped ||
                expectedClosings.isNotEmpty()
    }
}
