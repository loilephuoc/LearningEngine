package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.knowledge.model.KnowledgeEdge
import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphFactory
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphValidationResult
import vn.loi.learning.domain.knowledge.model.KnowledgeNode
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeId
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeKind
import vn.loi.learning.domain.knowledge.model.KnowledgeRelationshipType
import vn.loi.learning.infrastructure.persistence.record.KnowledgeEdgeRecord
import vn.loi.learning.infrastructure.persistence.record.KnowledgeGraphRecord
import vn.loi.learning.infrastructure.persistence.record.KnowledgeNodeRecord

/**
 * Mapper giữa [KnowledgeGraph] domain model và [KnowledgeGraphRecord] persistence DTO.
 *
 * - Canonical typed enum serialization: dùng `.name` và `valueOf`.
 * - Unknown kind hoặc relationshipType: bỏ qua node/edge bị lỗi (defensive read).
 * - Không lưu tên class implementation.
 */
object KnowledgeGraphRecordMapper {

    // ── Domain → Record ──────────────────────────────────────────────────

    fun toRecord(graph: KnowledgeGraph): KnowledgeGraphRecord =
        KnowledgeGraphRecord(
            nodes = graph.nodes().map(::nodeToRecord),
            edges = graph.edges().map(::edgeToRecord)
        )

    private fun nodeToRecord(node: KnowledgeNode): KnowledgeNodeRecord =
        KnowledgeNodeRecord(
            id = node.id.value,
            kind = node.kind.name,
            displayName = node.displayName
        )

    private fun edgeToRecord(edge: KnowledgeEdge): KnowledgeEdgeRecord =
        KnowledgeEdgeRecord(
            sourceId = edge.sourceId.value,
            targetId = edge.targetId.value,
            relationshipType = edge.relationshipType.name
        )

    // ── Record → Domain ──────────────────────────────────────────────────

    /**
     * Chuyển đổi record về domain graph.
     *
     * - Các node hoặc edge có kind/relationshipType không được nhận biết sẽ bị bỏ qua.
     * - Graph được build thông qua [KnowledgeGraphFactory]; nếu invalid, ném [IllegalStateException]
     *   vì persisted data đã được validate trước khi lưu.
     */
    fun toDomain(record: KnowledgeGraphRecord): KnowledgeGraph {
        val nodes = record.nodes.mapNotNull(::nodeFromRecord)
        val edges = record.edges.mapNotNull(::edgeFromRecord)
        return when (val result = KnowledgeGraphFactory.build(nodes, edges)) {
            is KnowledgeGraphValidationResult.Valid -> result.graph
            is KnowledgeGraphValidationResult.Invalid ->
                throw IllegalStateException(
                    "Persisted knowledge graph record is invalid: ${result.issues}"
                )
        }
    }

    private fun nodeFromRecord(record: KnowledgeNodeRecord): KnowledgeNode? {
        val kind = try {
            KnowledgeNodeKind.valueOf(record.kind)
        } catch (_: IllegalArgumentException) {
            return null // Unknown kind — skip defensively
        }
        if (record.id.isBlank() || record.displayName.isBlank()) return null
        return KnowledgeNode(
            id = KnowledgeNodeId(record.id),
            kind = kind,
            displayName = record.displayName
        )
    }

    private fun edgeFromRecord(record: KnowledgeEdgeRecord): KnowledgeEdge? {
        val type = try {
            KnowledgeRelationshipType.valueOf(record.relationshipType)
        } catch (_: IllegalArgumentException) {
            return null // Unknown relationship type — skip defensively
        }
        if (record.sourceId.isBlank() || record.targetId.isBlank()) return null
        return KnowledgeEdge(
            sourceId = KnowledgeNodeId(record.sourceId),
            targetId = KnowledgeNodeId(record.targetId),
            relationshipType = type
        )
    }
}
