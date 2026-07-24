package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.KnowledgeGraphRecord

/**
 * In-memory implementation of [KnowledgeGraphStore] for use in tests and in-memory contexts.
 */
class InMemoryKnowledgeGraphStore : KnowledgeGraphStore {

    private var stored: KnowledgeGraphRecord = KnowledgeGraphRecord()

    override fun load(): KnowledgeGraphRecord = stored

    override fun save(record: KnowledgeGraphRecord) {
        stored = record
    }
}
