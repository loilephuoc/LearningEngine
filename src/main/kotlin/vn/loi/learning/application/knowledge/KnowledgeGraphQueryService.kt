package vn.loi.learning.application.knowledge

import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.model.KnowledgeNode
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeId
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeKind
import vn.loi.learning.domain.knowledge.model.KnowledgeRelationshipType
import vn.loi.learning.domain.knowledge.repository.KnowledgeGraphRepository
import vn.loi.learning.domain.knowledge.service.KnowledgeGraphAnalyzer
import vn.loi.learning.domain.knowledge.service.PathResult
import vn.loi.learning.domain.knowledge.service.TopologicalOrderResult

/**
 * Application service cung cấp query API trên knowledge graph.
 *
 * - Không expose record hay JSON.
 * - Graph vẫn immutable.
 * - Tất cả kết quả là deterministic.
 */
class KnowledgeGraphQueryService(
    private val repository: KnowledgeGraphRepository
) {
    fun getGraph(): KnowledgeGraph = repository.get()

    fun findNode(id: KnowledgeNodeId): KnowledgeNode? =
        getGraph().findNode(id)

    fun containsNode(id: KnowledgeNodeId): Boolean =
        getGraph().containsNode(id)

    fun nodes(): List<KnowledgeNode> = getGraph().nodes()

    fun nodesOfKind(kind: KnowledgeNodeKind): List<KnowledgeNode> =
        getGraph().nodesOfKind(kind)

    fun successors(
        nodeId: KnowledgeNodeId,
        relationshipType: KnowledgeRelationshipType
    ): List<KnowledgeNode> = getGraph().successors(nodeId, relationshipType)

    fun predecessors(
        nodeId: KnowledgeNodeId,
        relationshipType: KnowledgeRelationshipType
    ): List<KnowledgeNode> = getGraph().predecessors(nodeId, relationshipType)

    fun isReachable(
        sourceId: KnowledgeNodeId,
        targetId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>? = null
    ): Boolean = KnowledgeGraphAnalyzer.isReachable(getGraph(), sourceId, targetId, relationshipTypes)

    fun shortestPath(
        sourceId: KnowledgeNodeId,
        targetId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>? = null
    ): PathResult = KnowledgeGraphAnalyzer.shortestPath(getGraph(), sourceId, targetId, relationshipTypes)

    fun topologicalOrder(
        relationshipTypes: Set<KnowledgeRelationshipType>
    ): TopologicalOrderResult = KnowledgeGraphAnalyzer.topologicalOrder(getGraph(), relationshipTypes)

    fun transitiveSuccessors(
        nodeId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>
    ): List<KnowledgeNode> = KnowledgeGraphAnalyzer.transitiveSuccessors(getGraph(), nodeId, relationshipTypes)
}
