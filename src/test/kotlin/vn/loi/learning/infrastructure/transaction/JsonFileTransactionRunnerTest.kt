package vn.loi.learning.infrastructure.transaction

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonFileTransactionRunnerTest {

    @Test
    fun `restores existing files and deletes newly created files when transaction fails`() {
        val directory =
            Files.createTempDirectory(
                "json-transaction-test"
            )

        val existingFile =
            directory.resolve(
                "existing.json"
            )

        val newFile =
            directory.resolve(
                "new.json"
            )

        try {
            Files.writeString(
                existingFile,
                "before"
            )

            val runner =
                JsonFileTransactionRunner(
                    listOf(
                        existingFile,
                        newFile
                    )
                )

            val failure =
                assertFailsWith<IllegalStateException> {
                    runner.runInTransaction {
                        Files.writeString(
                            existingFile,
                            "after"
                        )

                        Files.writeString(
                            newFile,
                            "created"
                        )

                        throw IllegalStateException(
                            "transaction failed"
                        )
                    }
                }

            assertEquals(
                "transaction failed",
                failure.message
            )

            assertEquals(
                "before",
                Files.readString(
                    existingFile
                )
            )

            assertFalse(
                Files.exists(
                    newFile
                )
            )

            assertTrue(
                failure.suppressedExceptions.isEmpty()
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `successful transaction keeps all written changes`() {
        val directory =
            Files.createTempDirectory(
                "json-transaction-success-test"
            )

        val existingFile =
            directory.resolve(
                "existing.json"
            )

        val newFile =
            directory.resolve(
                "new.json"
            )

        try {
            Files.writeString(
                existingFile,
                "before"
            )

            val runner =
                JsonFileTransactionRunner(
                    listOf(
                        existingFile,
                        newFile
                    )
                )

            val result =
                runner.runInTransaction {
                    Files.writeString(
                        existingFile,
                        "after"
                    )

                    Files.writeString(
                        newFile,
                        "created"
                    )

                    "committed"
                }

            assertEquals(
                "committed",
                result
            )

            assertEquals(
                "after",
                Files.readString(
                    existingFile
                )
            )

            assertEquals(
                "created",
                Files.readString(
                    newFile
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `rollback continues after one file restoration fails`() {
        val directory =
            Files.createTempDirectory(
                "json-transaction-partial-rollback-test"
            )

        val blockedParent =
            directory.resolve(
                "blocked-parent"
            )

        val blockedFile =
            blockedParent.resolve(
                "blocked.json"
            )

        val restorableFile =
            directory.resolve(
                "restorable.json"
            )

        try {
            Files.createDirectories(
                blockedParent
            )

            Files.writeString(
                blockedFile,
                "blocked-before"
            )

            Files.writeString(
                restorableFile,
                "restorable-before"
            )

            val runner =
                JsonFileTransactionRunner(
                    listOf(
                        blockedFile,
                        restorableFile
                    )
                )

            val failure =
                assertFailsWith<IllegalStateException> {
                    runner.runInTransaction {
                        Files.writeString(
                            blockedFile,
                            "blocked-after"
                        )

                        Files.writeString(
                            restorableFile,
                            "restorable-after"
                        )

                        Files.delete(
                            blockedFile
                        )

                        Files.delete(
                            blockedParent
                        )

                        Files.writeString(
                            blockedParent,
                            "prevents directory restoration"
                        )

                        throw IllegalStateException(
                            "business operation failed"
                        )
                    }
                }

            assertEquals(
                "business operation failed",
                failure.message
            )

            assertEquals(
                "restorable-before",
                Files.readString(
                    restorableFile
                )
            )

            assertTrue(
                failure.suppressedExceptions.isNotEmpty()
            )

            assertTrue(
                Files.isRegularFile(
                    blockedParent
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `duplicate equivalent paths are snapshotted only once`() {
        val directory =
            Files.createTempDirectory(
                "json-transaction-duplicate-path-test"
            )

        val file =
            directory.resolve(
                "data.json"
            )

        try {
            val originalContent =
                byteArrayOf(
                    1,
                    2,
                    3,
                    4
                )

            Files.write(
                file,
                originalContent
            )

            val equivalentPath =
                file.parent
                    .resolve(".")
                    .resolve(
                        file.fileName
                    )

            val runner =
                JsonFileTransactionRunner(
                    listOf(
                        file,
                        equivalentPath,
                        file.toAbsolutePath()
                    )
                )

            assertFailsWith<IllegalStateException> {
                runner.runInTransaction {
                    Files.write(
                        file,
                        byteArrayOf(
                            9,
                            8,
                            7
                        )
                    )

                    throw IllegalStateException(
                        "rollback"
                    )
                }
            }

            assertContentEquals(
                originalContent,
                Files.readAllBytes(
                    file
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }
}