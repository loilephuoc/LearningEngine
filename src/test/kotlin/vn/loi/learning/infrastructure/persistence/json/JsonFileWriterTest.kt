package vn.loi.learning.infrastructure.persistence.json

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonFileWriterTest {

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