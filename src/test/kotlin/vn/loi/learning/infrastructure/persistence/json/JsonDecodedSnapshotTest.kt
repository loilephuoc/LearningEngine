package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JsonDecodedSnapshotTest {
    @Test
    fun `concurrent cold callers share one decode and one published snapshot`() {
        val file = Files.createTempDirectory("json-decoded-concurrent").resolve("records.json")
        Files.writeString(file, "persisted")
        val snapshot = JsonDecodedSnapshot<String>(file)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        var decodes = 0
        val executor = Executors.newFixedThreadPool(2)
        try {
            val load = {
                snapshot.load {
                    decodes += 1
                    entered.countDown()
                    release.await(5, TimeUnit.SECONDS)
                    listOf("stable")
                }
            }
            val first = executor.submit<List<String>>(load)
            assertEquals(true, entered.await(5, TimeUnit.SECONDS))
            val second = executor.submit<List<String>>(load)
            release.countDown()

            val firstValue = first.get(5, TimeUnit.SECONDS)
            val secondValue = second.get(5, TimeUnit.SECONDS)
            assertEquals(1, decodes)
            assertSame(firstValue, secondValue)
            assertEquals(listOf("stable"), secondValue)
        } finally {
            release.countDown()
            executor.shutdownNow()
            file.parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reuses decoded records until persisted file is replaced`() {
        val directory = Files.createTempDirectory("json-decoded-snapshot")
        val file = directory.resolve("records.json")
        try {
            Files.writeString(file, "one")
            val snapshot = JsonDecodedSnapshot<String>(file)
            var decodes = 0
            fun load() = snapshot.load { decodes += 1; listOf(Files.readString(file)) }

            assertEquals(listOf("one"), load())
            assertEquals(listOf("one"), load())
            assertEquals(1, decodes)

            val replacement = directory.resolve("replacement.tmp")
            Files.writeString(replacement, "two-two")
            Files.move(replacement, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            assertEquals(listOf("two-two"), load())
            assertEquals(2, decodes)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `successful write publishes an immutable read-after-write snapshot`() {
        val file = Files.createTempDirectory("json-decoded-write").resolve("records.json")
        val snapshot = JsonDecodedSnapshot<String>(file)
        Files.writeString(file, "persisted")
        snapshot.written(mutableListOf("updated"))
        var decodes = 0

        assertEquals(listOf("updated"), snapshot.load { decodes += 1; emptyList() })
        assertEquals(0, decodes)
    }
}
