package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.repository.KnowledgeGraphRepository
import vn.loi.learning.infrastructure.persistence.mapper.KnowledgeGraphRecordMapper
import vn.loi.learning.infrastructure.persistence.store.KnowledgeGraphStore

/**
 * JSON-backed implementation của [KnowledgeGraphRepository].
 *
 * - get(): load record từ store → map về domain.
 * - save(): map domain → record → ghi qua store (atomic replacement).
 * - Restart-safe: write/close/reopen/query giữ nguyên graph semantics.
 * - Không expose internal maps hay mutable collections.
 */
class StoreBackedKnowledgeGraphRepository(
    private val store: KnowledgeGraphStore
) : KnowledgeGraphRepository {

    override fun get(): KnowledgeGraph =
        KnowledgeGraphRecordMapper.toDomain(store.load())

    override fun save(graph: KnowledgeGraph) {
        val record = KnowledgeGraphRecordMapper.toRecord(graph)
        store.save(record)
    }
}
