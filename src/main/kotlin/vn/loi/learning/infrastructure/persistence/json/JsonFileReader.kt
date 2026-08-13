package vn.loi.learning.infrastructure.persistence.json

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
        traceName: String? = null,
        decode: (String) -> T
    ): T {
        if (
            Files.notExists(
                filePath
            )
        ) {
            return emptyValue
        }

        val readStarted = System.nanoTime()
        val content = PortableTextFileReader.read(filePath)
        val readMs = (System.nanoTime() - readStarted) / 1_000_000

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
            val decodeStarted = System.nanoTime()
            decode(
                content
            ).also {
                traceName?.let { name ->
                    JsonPersistenceTrace.write(
                        name, filePath, "read_decode", (System.nanoTime() - readStarted) / 1_000_000,
                        detail = "readMs=$readMs jsonDecodeMs=${(System.nanoTime() - decodeStarted) / 1_000_000}"
                    )
                }
            }
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
