package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.KnowledgeGraphRecord

/**
 * Store interface for [KnowledgeGraphRecord] persistence.
 */
interface KnowledgeGraphStore {
    fun load(): KnowledgeGraphRecord
    fun save(record: KnowledgeGraphRecord)
}
