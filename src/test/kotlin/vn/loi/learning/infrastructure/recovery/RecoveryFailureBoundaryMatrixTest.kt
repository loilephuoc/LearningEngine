package vn.loi.learning.infrastructure.recovery

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.port.RecoveryOperationGate
import vn.loi.learning.application.port.RecoveryCoordinatedTransactionRunner
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class RecoveryFailureBoundaryMatrixTest {
    @Test
    fun `restore staging and prevalidation injection failures leave zero live mutation`() {
        val phases = listOf(
            "restore-gate-acquired", "restore-stage-extract:first", "restore-stage-extract:middle",
            "restore-stage-extract:last", "restore-staged-domain-validate"
        )
        phases.forEach { selected -> fixture().use { fixture ->
            val archive = fixture.backupOf("restored")
            fixture.writeState("pre")
            val before = fixture.snapshot()
            val manager = fixture.manager { phase, detail -> if (matches(selected, phase, detail)) error("injected $selected") }
            assertFailsWith<LearningDataRecoveryException>(selected) { manager.restore(archive, false) }
            assertSnapshotsEqual(before, fixture.snapshot(), selected)
        } }
    }

    @Test
    fun `point in time staging excludes writers and accepted backup is entirely pre write`() {
        fixture().use { fixture ->
            val gate = RecoveryOperationGate()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val executor = Executors.newSingleThreadExecutor()
            val target = fixture.root.resolve("point-in-time.lebak")
            val manager = JvmLearningDataRecoveryManager(
                mapOf("config" to fixture.config, "data" to fixture.data), fixture.safety,
                gate = gate,
                failureHook = { phase, detail ->
                    if (phase == "backup-snapshot-copy" && detail?.startsWith("0:") == true) {
                        entered.countDown(); release.await(5, TimeUnit.SECONDS)
                    }
                }
            )
            val transaction = RecoveryCoordinatedTransactionRunner(InMemoryTransactionRunner(), gate)
            val before = fixture.snapshot()
            try {
                val backup = executor.submit { manager.createBackup(target) }
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                assertFailsWith<vn.loi.learning.application.port.RecoveryOperationBusyException> {
                    transaction.runInTransaction { fixture.writeState("post") }
                }
                release.countDown(); backup.get(5, TimeUnit.SECONDS)
                fixture.writeState("destroyed")
                manager.restore(target, false)
                assertSnapshotsEqual(before, fixture.snapshot(), "point-in-time")
            } finally { release.countDown(); executor.shutdownNow() }
        }
    }

    @Test
    fun `active canonical transaction refuses backup before snapshot starts`() {
        fixture().use { fixture ->
            val gate = RecoveryOperationGate()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val executor = Executors.newSingleThreadExecutor()
            val manager = JvmLearningDataRecoveryManager(mapOf("config" to fixture.config, "data" to fixture.data), fixture.safety, gate = gate)
            val transaction = RecoveryCoordinatedTransactionRunner(InMemoryTransactionRunner(), gate)
            try {
                val writer = executor.submit { transaction.runInTransaction { entered.countDown(); release.await() } }
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                assertFailsWith<vn.loi.learning.application.port.RecoveryOperationBusyException> {
                    manager.createBackup(fixture.root.resolve("busy.lebak"))
                }
                assertFalse(Files.exists(fixture.root.resolve("busy.lebak")))
                release.countDown(); writer.get(5, TimeUnit.SECONDS)
            } finally { release.countDown(); executor.shutdownNow() }
        }
    }
    @Test
    fun `every backup boundary failure preserves live bytes and publishes nothing`() {
        val phases = listOf(
            "backup-temporary-archive-create", "backup-staging-create", "backup-snapshot-copy:first",
            "backup-snapshot-copy:middle", "backup-snapshot-copy:last",
            "backup-snapshot-copy:data/media/first.bin", "backup-snapshot-copy:data/media/middle.bin",
            "backup-snapshot-copy:data/media/final.bin", "backup-manifest-create",
            "backup-hash:first", "backup-hash:last", "backup-archive-entry-write:manifest.txt",
            "backup-archive-entry-write:data/media/first.bin", "backup-archive-entry-write:data/media/middle.bin",
            "backup-archive-entry-write:data/media/final.bin", "backup-archive-verify", "backup-publish"
        )
        phases.forEach { selected -> fixture().use { fixture ->
            val before = fixture.snapshot()
            val target = fixture.root.resolve("failed.lebak")
            val manager = fixture.manager { phase, detail -> if (matches(selected, phase, detail)) error("injected $selected") }

            assertFailsWith<LearningDataRecoveryException>(selected) { manager.createBackup(target) }

            assertSnapshotsEqual(before, fixture.snapshot(), selected)
            assertFalse(Files.exists(target), selected)
            assertTrue(fixture.operationTemps().isEmpty(), selected)
        } }
    }

    @Test
    fun `safety backup failures never begin canonical replacement`() {
        val phases = listOf(
            "safety-temporary-archive-create", "safety-staging-create", "safety-snapshot-copy:first",
            "safety-snapshot-copy:middle", "safety-snapshot-copy:last", "safety-manifest-create",
            "safety-snapshot-copy:data/media/first.bin", "safety-snapshot-copy:data/media/middle.bin",
            "safety-snapshot-copy:data/media/final.bin",
            "safety-hash:first", "safety-hash:last", "safety-archive-entry-write:manifest.txt",
            "safety-archive-verify", "safety-publish", "safety-verified"
        )
        phases.forEach { selected -> fixture().use { fixture ->
            val archive = fixture.backupOf("restored")
            fixture.writeState("pre")
            val before = fixture.snapshot()
            val manager = fixture.manager { phase, detail -> if (matches(selected, phase, detail)) error("injected $selected") }

            assertFailsWith<LearningDataRecoveryException>(selected) { manager.restore(archive, false) }

            assertSnapshotsEqual(before, fixture.snapshot(), selected)
            assertTrue(fixture.operationTemps().isEmpty(), selected)
        } }
    }

    @Test
    fun `every replacement and post validation failure returns exact pre restore state`() {
        val phases = listOf(
            "restore-write:first", "restore-write:middle", "restore-write:last",
            "restore-write:data/media/first.bin", "restore-write:data/media/middle.bin",
            "restore-write:data/media/final.bin",
            "restore-post-copy-verify", "restore-live-domain-validate", "restore-success-publish"
        )
        phases.forEach { selected -> fixture().use { fixture ->
            val archive = fixture.backupOf("restored")
            fixture.writeState("pre")
            val before = fixture.snapshot()
            val manager = fixture.manager { phase, detail -> if (matches(selected, phase, detail)) error("injected $selected") }

            assertFailsWith<LearningDataRecoveryException>(selected) { manager.restore(archive, false) }

            assertSnapshotsEqual(before, fixture.snapshot(), selected)
            assertTrue(Files.list(fixture.safety).use { it.findAny().isPresent }, selected)
        } }
    }

    @Test
    fun `rollback first middle final and media failures are catastrophic with retained evidence`() {
        val rollbackSelections = listOf(
            "rollback-write:first", "rollback-write:middle", "rollback-write:last",
            "rollback-write:data/media/first.bin", "rollback-write:data/media/middle.bin",
            "rollback-write:data/media/final.bin"
        )
        rollbackSelections.forEach { selected -> fixture().use { fixture ->
            val archive = fixture.backupOf("restored")
            fixture.writeState("pre")
            val manager = fixture.manager { phase, detail ->
                if (phase == "restore-post-copy-verify" || matches(selected, phase, detail)) error("injected $selected")
            }

            val failure = assertFailsWith<CatastrophicLearningDataRecoveryException>(selected) { manager.restore(archive, false) }

            assertTrue(Files.exists(failure.safetyBackup), selected)
            JvmLearningDataRecoveryManager(mapOf("config" to fixture.config, "data" to fixture.data), fixture.safety)
                .validate(failure.safetyBackup)
            assertFalse(failure.message.orEmpty().contains("pre-secret"), selected)
        } }
    }

    @Test
    fun `existing backup is never overwritten`() {
        fixture().use { fixture ->
            val target = fixture.root.resolve("existing.lebak")
            val original = "existing archive".toByteArray()
            Files.write(target, original)
            assertFailsWith<LearningDataRecoveryException> { fixture.manager().createBackup(target) }
            assertContentEquals(original, Files.readAllBytes(target))
        }
    }

    @Test
    fun `successful restore has exact restored state and never a third state`() {
        fixture().use { fixture ->
            val archive = fixture.backupOf("restored")
            val restored = fixture.snapshot()
            fixture.writeState("pre")
            val pre = fixture.snapshot()
            fixture.manager().restore(archive, false)
            val observed = fixture.snapshot()
            assertTrue(same(restored, observed) xor same(pre, observed))
            assertTrue(same(restored, observed))
        }
    }

    private fun fixture() = Fixture(Files.createTempDirectory("recovery-boundary-"))

    private class Fixture(val root: Path) : AutoCloseable {
        val data = Files.createDirectories(root.resolve("data"))
        val config = Files.createDirectories(root.resolve("config"))
        val safety = Files.createDirectories(config.resolve("backups"))

        init { writeState("initial") }

        fun manager(hook: (String, String?) -> Unit = { _, _ -> }) =
            JvmLearningDataRecoveryManager(mapOf("config" to config, "data" to data), safety, failureHook = hook)

        fun backupOf(value: String): Path {
            writeState(value)
            val archive = root.resolve("$value.lebak")
            manager().createBackup(archive)
            return archive
        }

        fun writeState(value: String) {
            listOf(
                config.resolve("settings.properties"),
                data.resolve("a.json"), data.resolve("m.json"), data.resolve("z.json"),
                data.resolve("media/first.bin"), data.resolve("media/middle.bin"), data.resolve("media/final.bin")
            ).forEachIndexed { index, path ->
                Files.createDirectories(path.parent)
                Files.writeString(path, "$value-$index-${if (value == "pre") "pre-secret" else "safe"}")
            }
        }

        fun snapshot(): Map<String, ByteArray> = listOf("config" to config, "data" to data).flatMap { (prefix, base) ->
            Files.walk(base).use { paths -> paths.filter(Files::isRegularFile).filter { !it.startsWith(safety) }
                .map { "$prefix/${base.relativize(it).toString().replace('\\', '/')}" to Files.readAllBytes(it) }.toList() }
        }.sortedBy { it.first }.toMap()

        fun operationTemps(): List<Path> = Files.walk(root).use { paths -> paths.filter {
            val name = it.fileName.toString()
            name.startsWith(".learning-engine-backup-") || name.startsWith(".learning-engine-snapshot-") ||
                name.startsWith(".learning-engine-restore-")
        }.toList() }

        override fun close() { root.toFile().deleteRecursively() }
    }

    companion object {
        private fun matches(selected: String, phase: String, detail: String?): Boolean {
            val wantedPhase = selected.substringBeforeLast(':', selected)
            val selector = selected.substringAfterLast(':', "")
            if (phase != wantedPhase) return false
            if (selector.isEmpty()) return true
            if (selector == "manifest.txt") return detail == selector
            if (selector.startsWith("data/")) return detail?.substringAfterLast(':') == selector
            if (selector == "media") return detail?.substringAfterLast(':')?.contains("/media/") == true
            val parts = detail?.split(':', limit = 3) ?: return false
            val index = parts[0].toInt()
            val size = parts[1].toInt()
            return when (selector) { "first" -> index == 0; "middle" -> index == size / 2; "last" -> index == size - 1; else -> false }
        }

        private fun assertSnapshotsEqual(expected: Map<String, ByteArray>, actual: Map<String, ByteArray>, label: String) {
            assertEquals(expected.keys, actual.keys, label)
            expected.forEach { (name, bytes) -> assertContentEquals(bytes, actual.getValue(name), "$label:$name") }
        }

        private fun same(first: Map<String, ByteArray>, second: Map<String, ByteArray>) =
            first.keys == second.keys && first.all { (name, bytes) -> second.getValue(name).contentEquals(bytes) }
    }
}
