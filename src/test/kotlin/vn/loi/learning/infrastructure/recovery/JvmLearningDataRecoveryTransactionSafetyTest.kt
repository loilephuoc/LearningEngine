package vn.loi.learning.infrastructure.recovery

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JvmLearningDataRecoveryTransactionSafetyTest {
    @Test
    fun `post mutation failure rolls back exact bytes`() {
        val root = Files.createTempDirectory("recovery-transaction")
        try {
            val data = Files.createDirectories(root.resolve("data"))
            val safety = root.resolve("safety")
            val state = data.resolve("state.json")
            Files.writeString(state, "backup")
            val archive = root.resolve("backup.lebak")
            JvmLearningDataRecoveryManager(mapOf("data" to data), safety).createBackup(archive)
            val before = "live".toByteArray()
            Files.write(state, before)
            val failing = JvmLearningDataRecoveryManager(
                mapOf("data" to data), safety,
                failureHook = { phase, _ -> if (phase == "restore-write") throw java.io.IOException("injected") }
            )

            assertFailsWith<LearningDataRecoveryException> { failing.restore(archive, false) }
            assertContentEquals(before, Files.readAllBytes(state))
            assertTrue(Files.list(safety).use { it.findAny().isPresent })
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `rollback failure is catastrophic and retains verified safety backup`() {
        val root = Files.createTempDirectory("recovery-catastrophic")
        try {
            val data = Files.createDirectories(root.resolve("data"))
            val safety = root.resolve("safety")
            val state = data.resolve("state.json")
            Files.writeString(state, "backup")
            val archive = root.resolve("backup.lebak")
            JvmLearningDataRecoveryManager(mapOf("data" to data), safety).createBackup(archive)
            Files.writeString(state, "live")
            val failing = JvmLearningDataRecoveryManager(
                mapOf("data" to data), safety,
                failureHook = { phase, _ ->
                    if (phase == "restore-write" || phase == "rollback-write") throw java.io.IOException("injected $phase")
                }
            )

            val failure = assertFailsWith<CatastrophicLearningDataRecoveryException> { failing.restore(archive, false) }
            assertTrue(Files.exists(failure.safetyBackup))
            JvmLearningDataRecoveryManager(mapOf("data" to data), safety).validate(failure.safetyBackup)
        } finally { root.toFile().deleteRecursively() }
    }
}
