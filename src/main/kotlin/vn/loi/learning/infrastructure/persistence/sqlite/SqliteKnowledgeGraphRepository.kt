package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.infrastructure.persistence.record.KnowledgeEdgeRecord
import vn.loi.learning.infrastructure.persistence.record.KnowledgeGraphRecord
import vn.loi.learning.infrastructure.persistence.record.KnowledgeNodeRecord
import vn.loi.learning.infrastructure.persistence.store.KnowledgeGraphStore

class SqliteKnowledgeGraphRepository(
    private val database: LearningEngineDatabase
) : KnowledgeGraphStore {

    private val queries = database.knowledgeGraphQueries

    override fun load(): KnowledgeGraphRecord {
        val row = queries.get().executeAsOneOrNull() ?: return KnowledgeGraphRecord()
        return KnowledgeGraphRecord(
            nodes = SqliteJsonUtils.decodeOrDefault(row.nodesJson, emptyList<KnowledgeNodeRecord>()),
            edges = SqliteJsonUtils.decodeOrDefault(row.edgesJson, emptyList<KnowledgeEdgeRecord>())
        )
    }

    override fun save(record: KnowledgeGraphRecord) {
        queries.insertOrReplace(
            nodesJson = SqliteJsonUtils.encode(record.nodes),
            edgesJson = SqliteJsonUtils.encode(record.edges)
        )
    }
}
