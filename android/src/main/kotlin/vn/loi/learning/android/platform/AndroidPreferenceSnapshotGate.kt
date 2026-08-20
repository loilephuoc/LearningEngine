package vn.loi.learning.android.platform

import android.content.SharedPreferences
import java.util.concurrent.locks.ReentrantReadWriteLock

/** Process-wide coordination for typed preference snapshots and their production writers. */
internal object AndroidPreferenceSnapshotGate {
    private val lock = ReentrantReadWriteLock(true)

    fun <T> mutation(block: () -> T): T {
        lock.readLock().lock()
        return try { block() } finally { lock.readLock().unlock() }
    }

    fun <T> snapshot(block: () -> T): T {
        lock.writeLock().lock()
        return try { block() } finally { lock.writeLock().unlock() }
    }
}

internal fun SharedPreferences.Editor.coordinatedApply() = AndroidPreferenceSnapshotGate.mutation { apply() }
internal fun SharedPreferences.Editor.coordinatedCommit(): Boolean = AndroidPreferenceSnapshotGate.mutation { commit() }
