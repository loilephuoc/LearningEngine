package vn.loi.learning.infrastructure.persistence.json

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JsonFileReaderTest {

    @Test
    fun `returns empty value when file does not exist`() {
        val directory =
            Files.createTempDirectory(
                "json-file-reader-missing-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "missing.json"
                )

            val result =
                JsonFileReader.read(
                    filePath =
                        filePath,
                    emptyValue =
                        emptyList<String>()
                ) { content ->
                    Json.decodeFromString(
                        content
                    )
                }

            assertEquals(
                emptyList(),
                result
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `returns empty value when file is empty`() {
        val directory =
            Files.createTempDirectory(
                "json-file-reader-empty-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "empty.json"
                )

            Files.writeString(
                filePath,
                "",
                StandardCharsets.UTF_8
            )

            val emptyValue =
                listOf(
                    "default"
                )

            val result =
                JsonFileReader.read(
                    filePath =
                        filePath,
                    emptyValue =
                        emptyValue
                ) { content ->
                    Json.decodeFromString<List<String>>(
                        content
                    )
                }

            assertSame(
                emptyValue,
                result
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `returns empty value when file contains only whitespace`() {
        val directory =
            Files.createTempDirectory(
                "json-file-reader-whitespace-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "whitespace.json"
                )

            Files.writeString(
                filePath,
                " \n\t\r ",
                StandardCharsets.UTF_8
            )

            val result =
                JsonFileReader.read(
                    filePath =
                        filePath,
                    emptyValue =
                        emptyList<String>()
                ) { content ->
                    Json.decodeFromString(
                        content
                    )
                }

            assertEquals(
                emptyList(),
                result
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `decodes valid utf eight json`() {
        val directory =
            Files.createTempDirectory(
                "json-file-reader-valid-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "valid.json"
                )

            Files.writeString(
                filePath,
                """["Học tập","🧠","学習"]""",
                StandardCharsets.UTF_8
            )

            val result =
                JsonFileReader.read(
                    filePath =
                        filePath,
                    emptyValue =
                        emptyList<String>()
                ) { content ->
                    Json.decodeFromString(
                        content
                    )
                }

            assertEquals(
                listOf(
                    "Học tập",
                    "🧠",
                    "学習"
                ),
                result
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `wraps serialization failure with persistence exception`() {
        val directory =
            Files.createTempDirectory(
                "json-file-reader-invalid-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "invalid.json"
                )

            Files.writeString(
                filePath,
                """["unfinished"""",
                StandardCharsets.UTF_8
            )

            val failure =
                assertFailsWith<InvalidJsonPersistenceException> {
                    JsonFileReader.read(
                        filePath =
                            filePath,
                        emptyValue =
                            emptyList<String>()
                    ) { content ->
                        Json.decodeFromString<List<String>>(
                            content
                        )
                    }
                }

            assertEquals(
                filePath,
                failure.filePath
            )

            assertIs<SerializationException>(
                failure.cause
            )

            assertTrue(
                failure.message
                    .orEmpty()
                    .contains(
                        filePath
                            .toAbsolutePath()
                            .normalize()
                            .toString()
                    )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `does not wrap decoder failures unrelated to serialization`() {
        val directory =
            Files.createTempDirectory(
                "json-file-reader-decoder-failure-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "data.json"
                )

            Files.writeString(
                filePath,
                "[]",
                StandardCharsets.UTF_8
            )

            val expectedFailure =
                IllegalStateException(
                    "mapping failed"
                )

            val failure =
                assertFailsWith<IllegalStateException> {
                    JsonFileReader.read(
                        filePath =
                            filePath,
                        emptyValue =
                            emptyList<String>()
                    ) {
                        throw expectedFailure
                    }
                }

            assertSame(
                expectedFailure,
                failure
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `preserves io failure without wrapping it as invalid json`() {
        val directory =
            Files.createTempDirectory(
                "json-file-reader-io-test"
            )

        try {
            assertFailsWith<IOException> {
                JsonFileReader.read(
                    filePath =
                        directory,
                    emptyValue =
                        emptyList<String>()
                ) { content ->
                    Json.decodeFromString<List<String>>(
                        content
                    )
                }
            }
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }
}