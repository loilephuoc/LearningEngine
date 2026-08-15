package vn.loi.learning.application.port

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RecoveryOperationGateTest {
    private val canonicalOperations = listOf(
        "package import", "Content Studio save", "Content Studio create", "Content Studio delete",
        "Content Studio undo", "Study rating", "session persistence", "queue persistence", "archive", "uninstall"
    )
    @Test
    fun `backup is refused while canonical mutation is active`() {
        val gate = RecoveryOperationGate()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val mutation = executor.submit { gate.canonicalMutation { entered.countDown(); release.await() } }
            check(entered.await(5, TimeUnit.SECONDS))

            val failure = assertFailsWith<RecoveryOperationBusyException> { gate.backup { error("unreachable") } }
            assertEquals(RecoveryOperation.BACKUP, failure.requested)
            release.countDown()
            mutation.get(5, TimeUnit.SECONDS)
        } finally { release.countDown(); executor.shutdownNow() }
    }

    @Test
    fun `canonical mutation is refused while restore is active`() {
        val gate = RecoveryOperationGate()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val restore = executor.submit { gate.restore { entered.countDown(); release.await() } }
            check(entered.await(5, TimeUnit.SECONDS))

            val failure = assertFailsWith<RecoveryOperationBusyException> { gate.canonicalMutation {} }
            assertEquals(RecoveryOperation.CANONICAL_MUTATION, failure.requested)
            release.countDown()
            restore.get(5, TimeUnit.SECONDS)
        } finally { release.countDown(); executor.shutdownNow() }
    }

    @Test
    fun `backup and restore exclude every canonical operation in both directions`() {
        canonicalOperations.forEach { operation ->
            assertBlockedByActiveCanonical(operation, RecoveryOperation.BACKUP)
            assertBlockedByActiveCanonical(operation, RecoveryOperation.RESTORE)
            assertCanonicalBlockedByRecovery(operation, RecoveryOperation.BACKUP)
            assertCanonicalBlockedByRecovery(operation, RecoveryOperation.RESTORE)
        }
    }

    @Test
    fun `backup backup restore restore and backup restore never overlap`() {
        listOf(
            RecoveryOperation.BACKUP to RecoveryOperation.BACKUP,
            RecoveryOperation.BACKUP to RecoveryOperation.RESTORE,
            RecoveryOperation.RESTORE to RecoveryOperation.BACKUP,
            RecoveryOperation.RESTORE to RecoveryOperation.RESTORE
        ).forEach { (active, requested) ->
            val gate = RecoveryOperationGate()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val executor = Executors.newSingleThreadExecutor()
            try {
                val running = executor.submit { invoke(gate, active) { entered.countDown(); release.await() } }
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                assertFailsWith<RecoveryOperationBusyException>("$active vs $requested") { invoke(gate, requested) {} }
                release.countDown()
                running.get(5, TimeUnit.SECONDS)
            } finally { release.countDown(); executor.shutdownNow() }
        }
    }

    private fun assertBlockedByActiveCanonical(label: String, requested: RecoveryOperation) {
        val gate = RecoveryOperationGate()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val running = executor.submit { gate.canonicalMutation { entered.countDown(); release.await() } }
            assertTrue(entered.await(5, TimeUnit.SECONDS), label)
            assertFailsWith<RecoveryOperationBusyException>(label) { invoke(gate, requested) {} }
            release.countDown(); running.get(5, TimeUnit.SECONDS)
        } finally { release.countDown(); executor.shutdownNow() }
    }

    private fun assertCanonicalBlockedByRecovery(label: String, active: RecoveryOperation) {
        val gate = RecoveryOperationGate()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val running = executor.submit { invoke(gate, active) { entered.countDown(); release.await() } }
            assertTrue(entered.await(5, TimeUnit.SECONDS), label)
            assertFailsWith<RecoveryOperationBusyException>(label) { gate.canonicalMutation {} }
            release.countDown(); running.get(5, TimeUnit.SECONDS)
        } finally { release.countDown(); executor.shutdownNow() }
    }

    private fun <T> invoke(gate: RecoveryOperationGate, operation: RecoveryOperation, block: () -> T): T =
        when (operation) {
            RecoveryOperation.CANONICAL_MUTATION -> gate.canonicalMutation(block)
            RecoveryOperation.BACKUP -> gate.backup(block)
            RecoveryOperation.RESTORE -> gate.restore(block)
        }
}
