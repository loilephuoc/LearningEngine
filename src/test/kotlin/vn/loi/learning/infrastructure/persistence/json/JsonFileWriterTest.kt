package vn.loi.learning.infrastructure.persistence.json

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JsonFileWriterTest {

    @Test
    fun `ignores and preserves stale temporary artifact beside valid target`() {
        val directory =
            Files.createTempDirectory("json-file-writer-stale-temp-test")

        try {
            val filePath = directory.resolve("data.json")
            val staleFile = directory.resolve("data.json.crashed.tmp")
            val staleBytes = "incomplete candidate".toByteArray()

            Files.writeString(filePath, "[\"current\"]")
            Files.write(staleFile, staleBytes)

            val loaded =
                JsonFileReader.read(
                    filePath = filePath,
                    emptyValue = emptyList<String>(),
                    recordType = "test record"
                ) { content ->
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(
                        content
                    )
                }

            assertEquals(listOf("current"), loaded)

            JsonFileWriter.write(
                filePath = filePath,
                content = "[\"replacement\"]"
            )

            assertEquals("[\"replacement\"]", Files.readString(filePath))
            assertContentEquals(staleBytes, Files.readAllBytes(staleFile))
            assertEquals(
                listOf("data.json", "data.json.crashed.tmp"),
                Files.list(directory).use { files ->
                    files.map { it.fileName.toString() }.sorted().toList()
                }
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `unexpected atomic move failure preserves previous snapshot and original failure`() {
        val directory =
            Files.createTempDirectory("json-file-writer-move-failure-test")

        try {
            val filePath = directory.resolve("data.json")
            val previous = """{"version":"previous"}"""
            Files.writeString(filePath, previous)

            val expectedFailure = IOException("atomic replacement failed")

            val failure =
                assertFailsWith<IOException> {
                    JsonFileWriter.write(
                        filePath = filePath,
                        content = """{"version":"candidate"}""",
                        fileMover = JsonFileMover { _, _, atomic ->
                            assertTrue(atomic)
                            throw expectedFailure
                        }
                    )
                }

            assertSame(expectedFailure, failure)
            assertEquals(previous, Files.readString(filePath))
            assertEquals(
                listOf("data.json"),
                Files.list(directory).use { files ->
                    files.map { it.fileName.toString() }.toList()
                }
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `failed non-atomic fallback preserves snapshot and both move failures`() {
        val directory =
            Files.createTempDirectory("json-file-writer-fallback-failure-test")

        try {
            val filePath = directory.resolve("data.json")
            val previous = """{"version":"previous"}"""
            Files.writeString(filePath, previous)

            val atomicFailure =
                AtomicMoveNotSupportedException("temporary", "data", "unsupported")

            val fallbackFailure = IOException("fallback replacement failed")

            val failure =
                assertFailsWith<IOException> {
                    JsonFileWriter.write(
                        filePath = filePath,
                        content = """{"version":"candidate"}""",
                        fileMover = JsonFileMover { _, _, atomic ->
                            if (atomic) {
                                throw atomicFailure
                            }

                            throw fallbackFailure
                        }
                    )
                }

            assertSame(fallbackFailure, failure)
            assertTrue(failure.suppressed.contains(atomicFailure))
            assertEquals(previous, Files.readString(filePath))
            assertEquals(
                listOf("data.json"),
                Files.list(directory).use { files ->
                    files.map { it.fileName.toString() }.toList()
                }
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `uses non-atomic fallback only when atomic move is unsupported`() {
        val directory =
            Files.createTempDirectory("json-file-writer-fallback-success-test")

        try {
            val filePath = directory.resolve("data.json")
            val moveModes = mutableListOf<Boolean>()

            JsonFileWriter.write(
                filePath = filePath,
                content = """{"version":"fallback"}""",
                fileMover = JsonFileMover { source, target, atomic ->
                    moveModes.add(atomic)

                    if (atomic) {
                        throw AtomicMoveNotSupportedException(
                            source.toString(),
                            target.toString(),
                            "unsupported"
                        )
                    }

                    Files.move(
                        source,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            )

            assertEquals(listOf(true, false), moveModes)
            assertEquals(
                """{"version":"fallback"}""",
                Files.readString(filePath)
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `writes content and creates missing parent directories`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-parent-test"
            )

        try {
            val filePath =
                directory
                    .resolve(
                        "nested"
                    )
                    .resolve(
                        "data.json"
                    )

            JsonFileWriter.write(
                filePath =
                    filePath,
                content =
                    """{"value":"created"}"""
            )

            assertEquals(
                """{"value":"created"}""",
                Files.readString(
                    filePath
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `replaces an existing file snapshot`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-replace-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "data.json"
                )

            Files.writeString(
                filePath,
                """{"value":"before"}"""
            )

            JsonFileWriter.write(
                filePath =
                    filePath,
                content =
                    """{"value":"after"}"""
            )

            assertEquals(
                """{"value":"after"}""",
                Files.readString(
                    filePath
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `writes complete utf eight content`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-utf-eight-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "unicode.json"
                )

            val content =
                """
                {
                  "vietnamese": "Học tập bền vững",
                  "japanese": "学習",
                  "emoji": "🧠"
                }
                """.trimIndent()

            JsonFileWriter.write(
                filePath =
                    filePath,
                content =
                    content
            )

            assertEquals(
                content,
                Files.readString(
                    filePath,
                    StandardCharsets.UTF_8
                )
            )

            assertContentEquals(
                content.toByteArray(
                    StandardCharsets.UTF_8
                ),
                Files.readAllBytes(
                    filePath
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `writes empty content as an empty file`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-empty-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "empty.json"
                )

            Files.writeString(
                filePath,
                "old content"
            )

            JsonFileWriter.write(
                filePath =
                    filePath,
                content =
                    ""
            )

            assertEquals(
                0L,
                Files.size(
                    filePath
                )
            )

            assertEquals(
                "",
                Files.readString(
                    filePath
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `does not leave temporary files after successful write`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-cleanup-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "data.json"
                )

            JsonFileWriter.write(
                filePath =
                    filePath,
                content =
                    "[]"
            )

            val remainingFiles =
                Files.list(
                    directory
                ).use { files ->
                    files
                        .map { path ->
                            path.fileName.toString()
                        }
                        .toList()
                }

            assertEquals(
                listOf(
                    "data.json"
                ),
                remainingFiles
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `different target files use independent temporary files`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-independent-test"
            )

        try {
            val firstFile =
                directory.resolve(
                    "first.json"
                )

            val secondFile =
                directory.resolve(
                    "second.json"
                )

            JsonFileWriter.write(
                filePath =
                    firstFile,
                content =
                    """{"id":1}"""
            )

            JsonFileWriter.write(
                filePath =
                    secondFile,
                content =
                    """{"id":2}"""
            )

            assertEquals(
                """{"id":1}""",
                Files.readString(
                    firstFile
                )
            )

            assertEquals(
                """{"id":2}""",
                Files.readString(
                    secondFile
                )
            )

            assertTrue(
                Files.list(
                    directory
                ).use { files ->
                    files.noneMatch { path ->
                        path.fileName
                            .toString()
                            .endsWith(
                                ".tmp"
                            )
                    }
                }
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `repeated writes always expose one complete snapshot`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-repeated-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "data.json"
                )

            val snapshots =
                listOf(
                    """{"version":1,"items":[]}""",
                    """{"version":2,"items":["one"]}""",
                    """{"version":3,"items":["one","two"]}"""
                )

            snapshots.forEach { snapshot ->
                JsonFileWriter.write(
                    filePath =
                        filePath,
                    content =
                        snapshot
                )

                assertEquals(
                    snapshot,
                    Files.readString(
                        filePath,
                        StandardCharsets.UTF_8
                    )
                )
            }

            val remainingFiles =
                Files.list(
                    directory
                ).use { files ->
                    files
                        .map { path ->
                            path.fileName.toString()
                        }
                        .toList()
                }

            assertEquals(
                listOf(
                    "data.json"
                ),
                remainingFiles
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `synchronizes parent directory after completed replacement`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-directory-sync-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "data.json"
                )

            val synchronizedDirectories =
                mutableListOf<Path>()

            JsonFileWriter.write(
                filePath =
                    filePath,
                content =
                    """{"status":"complete"}""",
                directorySynchronizer =
                    JsonDirectorySynchronizer { synchronizedDirectory ->
                        assertTrue(
                            Files.exists(
                                filePath
                            )
                        )

                        assertEquals(
                            """{"status":"complete"}""",
                            Files.readString(
                                filePath,
                                StandardCharsets.UTF_8
                            )
                        )

                        synchronizedDirectories.add(
                            synchronizedDirectory
                        )
                    }
            )

            assertEquals(
                listOf(
                    directory
                        .toAbsolutePath()
                        .normalize()
                ),
                synchronizedDirectories
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `propagates directory synchronization failure after completed replacement`() {
        val directory =
            Files.createTempDirectory(
                "json-file-writer-directory-sync-failure-test"
            )

        try {
            val filePath =
                directory.resolve(
                    "data.json"
                )

            val failure =
                assertFailsWith<IllegalStateException> {
                    JsonFileWriter.write(
                        filePath =
                            filePath,
                        content =
                            """{"status":"written"}""",
                        directorySynchronizer =
                            JsonDirectorySynchronizer {
                                throw IllegalStateException(
                                    "directory sync failed"
                                )
                            }
                    )
                }

            assertEquals(
                "directory sync failed",
                failure.message
            )

            assertEquals(
                """{"status":"written"}""",
                Files.readString(
                    filePath,
                    StandardCharsets.UTF_8
                )
            )

            assertTrue(
                Files.list(
                    directory
                ).use { files ->
                    files.noneMatch { path ->
                        path.fileName
                            .toString()
                            .endsWith(
                                ".tmp"
                            )
                    }
                }
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }
}
