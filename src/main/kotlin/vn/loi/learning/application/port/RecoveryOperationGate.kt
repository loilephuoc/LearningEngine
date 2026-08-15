package vn.loi.learning.application.port

import java.util.concurrent.locks.ReentrantReadWriteLock

enum class RecoveryOperation { CANONICAL_MUTATION, BACKUP, RESTORE }

class RecoveryOperationBusyException(val requested: RecoveryOperation) :
    IllegalStateException("Another incompatible learning-data operation is active.")

/** One process-wide authority separating ordinary canonical writes from recovery maintenance. */
class RecoveryOperationGate {
    private val lock = ReentrantReadWriteLock(true)

    fun <T> canonicalMutation(block: () -> T): T = withShared(RecoveryOperation.CANONICAL_MUTATION, block)
    fun <T> backup(block: () -> T): T = withExclusive(RecoveryOperation.BACKUP, block)
    fun <T> restore(block: () -> T): T = withExclusive(RecoveryOperation.RESTORE, block)

    private fun <T> withShared(operation: RecoveryOperation, block: () -> T): T {
        if (!lock.readLock().tryLock()) throw RecoveryOperationBusyException(operation)
        return try { block() } finally { lock.readLock().unlock() }
    }

    private fun <T> withExclusive(operation: RecoveryOperation, block: () -> T): T {
        if (!lock.writeLock().tryLock()) throw RecoveryOperationBusyException(operation)
        return try { block() } finally { lock.writeLock().unlock() }
    }
}

class RecoveryCoordinatedTransactionRunner(
    private val delegate: TransactionRunner,
    private val gate: RecoveryOperationGate
) : TransactionRunner {
    override fun <T> runInTransaction(block: () -> T): T =
        gate.canonicalMutation { delegate.runInTransaction(block) }
}
