package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord

/**
 * Persistence Store cho MemoryStateRecord.
 *
 * Store chỉ chịu trách nhiệm đọc và ghi Record.
 * Không biết Domain.
 * Không biết Repository.
 * Không biết Scheduler.
 */
interface MemoryStateStore {

    /**
     * Đọc toàn bộ record từ storage.
     */
    fun load(): List<MemoryStateRecord>

    /**
     * Ghi đè toàn bộ storage.
     */
    fun save(
        records: List<MemoryStateRecord>
    )
}