package vn.loi.learning.infrastructure.transaction

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class JsonFileTransactionRunnerTest {

    @Test
    fun `restores existing files and deletes newly created files when transaction fails`() {
        val directory = Files.createTempDirectory("json-transaction-test")
        val existingFile = directory.resolve("existing.json")
        val newFile = directory.resolve("new.json")

        try {
            Files.writeString(existingFile, "before")

            val runner = JsonFileTransactionRunner(
                listOf(existingFile, newFile)
            )

            val failure = assertFailsWith<IllegalStateException> {
                runner.runInTransaction {
                    Files.writeString(existingFile, "after")
                    Files.writeString(newFile, "created")
                    throw IllegalStateException("transaction failed")
                }
            }

            assertEquals("transaction failed", failure.message)
            assertEquals("before", Files.readString(existingFile))
            assertFalse(Files.exists(newFile))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
