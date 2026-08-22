package vn.loi.learning.application.sync

import java.nio.file.Files
import kotlin.test.*
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.persistence.json.JsonLocalSyncStateRepository
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner
import vn.loi.learning.infrastructure.LearningApplicationFactory

class LocalSyncCoordinatorTest {
    private val account = SyncAccountId("account")
    private fun change(id: String = "event-1") = OutboundSyncChange(
        account, SyncEventId(id), IdempotencyKey("key-$id"), SyncDeviceId("desktop"),
        SyncEntityId("content-1"), delta = ContentFieldDelta(ContentField.QUESTION, DeltaOperation.SET, "new")
    )

    @Test
    fun `local mutation and outbox survive restart atomically`() {
        val root = Files.createTempDirectory("sync-state-")
        try {
            val domainFile = root.resolve("domain.txt")
            val syncFile = root.resolve("sync-state.json")
            val coordinator = LocalSyncCoordinator(
                JsonLocalSyncStateRepository(syncFile), JsonFileTransactionRunner(listOf(domainFile, syncFile))
            )
            coordinator.mutateAndEnqueue(change()) { Files.writeString(domainFile, "mutated") }

            val reopened = JsonLocalSyncStateRepository(syncFile)
            assertEquals("mutated", Files.readString(domainFile))
            assertEquals(listOf(change()), reopened.pendingOutbox(account))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `outbox failure rolls local mutation back`() {
        val root = Files.createTempDirectory("sync-rollback-")
        try {
            val domainFile = root.resolve("domain.txt").also { Files.writeString(it, "before") }
            val syncFile = root.resolve("sync-state.json")
            val state = JsonLocalSyncStateRepository(syncFile)
            state.enqueue(change())
            val coordinator = LocalSyncCoordinator(state, JsonFileTransactionRunner(listOf(domainFile, syncFile)))

            assertFailsWith<IllegalArgumentException> {
                coordinator.mutateAndEnqueue(change().copy(entityId = SyncEntityId("different"))) {
                    Files.writeString(domainFile, "after")
                }
            }
            assertEquals("before", Files.readString(domainFile))
            assertEquals(listOf(change()), JsonLocalSyncStateRepository(syncFile).pendingOutbox(account))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `failed apply does not record inbox or advance cursor and retry applies once`() {
        val root = Files.createTempDirectory("sync-apply-")
        try {
            val domainFile = root.resolve("domain.txt").also { Files.writeString(it, "before") }
            val syncFile = root.resolve("sync-state.json")
            val state = JsonLocalSyncStateRepository(syncFile)
            val coordinator = LocalSyncCoordinator(state, JsonFileTransactionRunner(listOf(domainFile, syncFile)))
            val remote = RemoteSyncChange(SyncRevision(1), change())

            assertFailsWith<IllegalStateException> {
                coordinator.applyOnce(remote) { Files.writeString(domainFile, "partial"); error("crash") }
            }
            assertEquals("before", Files.readString(domainFile))
            assertEquals(SyncCursor.START, state.cursor(account))
            assertTrue(coordinator.applyOnce(remote) { Files.writeString(domainFile, "applied") })
            assertFalse(coordinator.applyOnce(remote) { error("must not apply twice") })
            assertEquals("applied", Files.readString(domainFile))
            assertEquals(SyncCursor(1), JsonLocalSyncStateRepository(syncFile).cursor(account))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `outbox remains retryable until explicit acknowledgement`() {
        val root = Files.createTempDirectory("sync-ack-")
        try {
            val state = JsonLocalSyncStateRepository(root.resolve("sync-state.json"))
            state.enqueue(change())
            assertEquals(1, JsonLocalSyncStateRepository(root.resolve("sync-state.json")).pendingOutbox(account).size)
            state.acknowledgeOutbox(account, setOf(change().eventId))
            assertTrue(JsonLocalSyncStateRepository(root.resolve("sync-state.json")).pendingOutbox(account).isEmpty())
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `persisted application composition exposes durable sync boundary`() {
        val root = Files.createTempDirectory("sync-composition-")
        try {
            val first = LearningApplicationFactory.createPersisted(root, reconcilePartOfSpeechRegistryOnCreate = false)
            requireNotNull(first.localSyncCoordinator).mutateAndEnqueue(change()) { "committed" }

            val reopened = LearningApplicationFactory.createPersisted(root, reconcilePartOfSpeechRegistryOnCreate = false)
            assertEquals(listOf(change()), requireNotNull(reopened.localSyncStateRepository).pendingOutbox(account))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `persisted validation rejects corrupt sync state`() {
        val root = Files.createTempDirectory("sync-validation-")
        try {
            Files.writeString(root.resolve("sync-state.json"), "{not-json")
            assertFails { LearningApplicationFactory.validatePersisted(root) }
        } finally { root.toFile().deleteRecursively() }
    }
}
