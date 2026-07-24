package vn.loi.learning.domain.knowledge.service

import vn.loi.learning.domain.knowledge.model.KnowledgeEdge
import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.model.KnowledgeNode
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeId
import vn.loi.learning.domain.knowledge.model.KnowledgeRelationshipType

/**
 * Kết quả tìm đường đi ngắn nhất (shortest path) trong knowledge graph.
 */
sealed class PathResult {
    /**
     * Đường đi tìm thấy — danh sách các node theo thứ tự từ source đến target.
     * Source và target đều được bao gồm.
     */
    data class Found(val path: List<KnowledgeNode>) : PathResult()

    /** Không tìm thấy đường đi (target không thể reach được từ source). */
    object NotFound : PathResult() {
        override fun toString() = "PathResult.NotFound"
    }

    /**
     * Node nguồn hoặc target không tồn tại trong graph.
     */
    data class NodeNotFound(val nodeId: KnowledgeNodeId) : PathResult()
}

/**
 * Kết quả sắp xếp topo (topological order) trên một tập quan hệ.
 */
sealed class TopologicalOrderResult {
    /**
     * Thành công — danh sách nodes theo thứ tự topo (không có cycle).
     */
    data class Ordered(val nodes: List<KnowledgeNode>) : TopologicalOrderResult()

    /**
     * Thất bại — phát hiện ít nhất một cycle trong các quan hệ được chọn.
     * Chứa danh sách các node tham gia cycle (deterministic, sorted).
     */
    data class CycleDetected(val cycleNodes: List<KnowledgeNode>) : TopologicalOrderResult()
}

/**
 * Service phân tích và duyệt knowledge graph.
 *
 * - Cycle-safe: không bị vòng lặp vô hạn trên graph có chu trình.
 * - Deterministic: kết quả không phụ thuộc vào thứ tự duyệt của source collection.
 * - Không chứa learner state.
 * - Không phụ thuộc vào infrastructure.
 */
object KnowledgeGraphAnalyzer {

    /**
     * Kiểm tra xem `targetId` có thể reach được từ `sourceId` qua các edges trong graph không.
     *
     * - Nếu source == target: trả về `true` (node có thể reach chính nó).
     * - Nếu một trong hai node không tồn tại: trả về `false`.
     * - Có thể lọc theo [relationshipTypes] (nếu null thì xét tất cả loại quan hệ).
     */
    fun isReachable(
        graph: KnowledgeGraph,
        sourceId: KnowledgeNodeId,
        targetId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>? = null
    ): Boolean {
        if (!graph.containsNode(sourceId) || !graph.containsNode(targetId)) return false
        if (sourceId == targetId) return true
        return bfsReachable(graph, sourceId, targetId, relationshipTypes)
    }

    /**
     * Tất cả nodes có thể reach được từ `sourceId` (không bao gồm source chính nó).
     *
     * - Nếu source không tồn tại: trả về list rỗng.
     * - Có thể lọc theo [relationshipTypes].
     * - Kết quả theo thứ tự deterministic (sort by id.value, kind.name).
     */
    fun reachableFrom(
        graph: KnowledgeGraph,
        sourceId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>? = null
    ): List<KnowledgeNode> {
        if (!graph.containsNode(sourceId)) return emptyList()
        val visited = mutableSetOf<KnowledgeNodeId>()
        val queue = ArrayDeque<KnowledgeNodeId>()
        queue.add(sourceId)
        visited.add(sourceId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val successors = adjacentOutgoing(graph, current, relationshipTypes)
            for (neighbor in successors) {
                if (visited.add(neighbor.id)) {
                    queue.add(neighbor.id)
                }
            }
        }
        visited.remove(sourceId)
        return visited
            .mapNotNull { graph.findNode(it) }
            .sortedWith(compareBy({ it.id.value }, { it.kind.name }))
    }

    /**
     * Tìm đường đi ngắn nhất (BFS) từ `sourceId` đến `targetId`.
     *
     * - Nếu source == target: trả về [PathResult.Found] chứa chỉ source.
     * - Nếu node không tồn tại: trả về [PathResult.NodeNotFound].
     * - Nếu không thể reach: trả về [PathResult.NotFound].
     * - Tie-breaking deterministic: khi có nhiều đường đi bằng nhau, chọn đường
     *   có chuỗi id.value nhỏ nhất tại mỗi bước.
     * - Có thể lọc theo [relationshipTypes].
     */
    fun shortestPath(
        graph: KnowledgeGraph,
        sourceId: KnowledgeNodeId,
        targetId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>? = null
    ): PathResult {
        val sourceNode = graph.findNode(sourceId)
            ?: return PathResult.NodeNotFound(sourceId)
        val targetNode = graph.findNode(targetId)
            ?: return PathResult.NodeNotFound(targetId)

        if (sourceId == targetId) return PathResult.Found(listOf(sourceNode))

        // BFS với tie-breaking deterministic
        val visited = mutableSetOf<KnowledgeNodeId>()
        val parentMap = mutableMapOf<KnowledgeNodeId, KnowledgeNodeId>()
        val queue = ArrayDeque<KnowledgeNodeId>()
        queue.add(sourceId)
        visited.add(sourceId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = adjacentOutgoing(graph, current, relationshipTypes)
                .sortedBy { it.id.value }
            for (neighbor in neighbors) {
                if (visited.add(neighbor.id)) {
                    parentMap[neighbor.id] = current
                    if (neighbor.id == targetId) {
                        return PathResult.Found(reconstructPath(graph, parentMap, sourceId, targetId))
                    }
                    queue.add(neighbor.id)
                }
            }
        }
        return PathResult.NotFound
    }

    /**
     * Phát hiện các chu trình có hướng (directed cycles) trong graph.
     *
     * - Có thể lọc theo [relationshipTypes].
     * - Trả về danh sách nodes nằm trong cycle, sorted deterministically.
     * - Trả về list rỗng nếu không có cycle.
     */
    fun detectDirectedCycles(
        graph: KnowledgeGraph,
        relationshipTypes: Set<KnowledgeRelationshipType>? = null
    ): List<KnowledgeNode> {
        val allNodes = graph.nodes()
        val color = mutableMapOf<KnowledgeNodeId, Int>() // 0=white, 1=gray, 2=black
        val cycleNodeIds = mutableSetOf<KnowledgeNodeId>()
        val stack = mutableListOf<KnowledgeNodeId>()

        fun dfs(nodeId: KnowledgeNodeId) {
            color[nodeId] = 1
            stack.add(nodeId)
            val successors = adjacentOutgoing(graph, nodeId, relationshipTypes)
                .sortedBy { it.id.value }
            for (neighbor in successors) {
                when (color[neighbor.id] ?: 0) {
                    1 -> {
                        // Back edge — cycle found; collect cycle nodes from stack
                        val cycleStart = stack.indexOf(neighbor.id)
                        if (cycleStart >= 0) {
                            cycleNodeIds.addAll(stack.subList(cycleStart, stack.size))
                        }
                    }
                    0 -> dfs(neighbor.id)
                    // 2 = already processed
                }
            }
            stack.removeLastOrNull()
            color[nodeId] = 2
        }

        for (node in allNodes.sortedBy { it.id.value }) {
            if ((color[node.id] ?: 0) == 0) {
                dfs(node.id)
            }
        }
        return cycleNodeIds
            .mapNotNull { graph.findNode(it) }
            .sortedWith(compareBy({ it.id.value }, { it.kind.name }))
    }

    /**
     * Sắp xếp các nodes theo thứ tự topo (topological order) trên tập quan hệ được chọn.
     *
     * - Chỉ xét outgoing edges của [relationshipTypes].
     * - Nếu có cycle: trả về [TopologicalOrderResult.CycleDetected].
     * - Nếu không có cycle: trả về [TopologicalOrderResult.Ordered].
     * - Tie-breaking: khi có nhiều node không có predecessor (in-degree == 0),
     *   ưu tiên node có id.value nhỏ nhất (deterministic Kahn's algorithm).
     */
    fun topologicalOrder(
        graph: KnowledgeGraph,
        relationshipTypes: Set<KnowledgeRelationshipType>
    ): TopologicalOrderResult {
        val allNodes = graph.nodes()
        val inDegree = mutableMapOf<KnowledgeNodeId, Int>()
        for (node in allNodes) inDegree[node.id] = 0

        for (node in allNodes) {
            val successors = adjacentOutgoing(graph, node.id, relationshipTypes)
            for (succ in successors) {
                inDegree[succ.id] = (inDegree[succ.id] ?: 0) + 1
            }
        }

        // Kahn's algorithm — deterministic: sort candidates by id.value
        val queue = java.util.PriorityQueue<KnowledgeNodeId>(
            compareBy { it.value }
        )
        for ((nodeId, deg) in inDegree) {
            if (deg == 0) queue.add(nodeId)
        }

        val result = mutableListOf<KnowledgeNode>()
        while (queue.isNotEmpty()) {
            val currentId = queue.poll()
            val currentNode = graph.findNode(currentId) ?: continue
            result.add(currentNode)
            val successors = adjacentOutgoing(graph, currentId, relationshipTypes)
                .sortedBy { it.id.value }
            for (succ in successors) {
                val newDeg = (inDegree[succ.id] ?: 1) - 1
                inDegree[succ.id] = newDeg
                if (newDeg == 0) queue.add(succ.id)
            }
        }

        return if (result.size == allNodes.size) {
            TopologicalOrderResult.Ordered(result)
        } else {
            // Nodes not in result are in cycles
            val processedIds = result.map { it.id }.toSet()
            val cycleNodes = allNodes
                .filter { it.id !in processedIds }
                .sortedWith(compareBy({ it.id.value }, { it.kind.name }))
            TopologicalOrderResult.CycleDetected(cycleNodes)
        }
    }

    /**
     * Tìm tất cả dependencies hoặc prerequisites bắc cầu (transitive) của `nodeId`
     * qua các [relationshipTypes] được chỉ định.
     *
     * - Cycle-safe: mỗi node chỉ được xét một lần.
     * - Không bao gồm source node chính nó.
     * - Kết quả sorted deterministically theo (id.value, kind.name).
     */
    fun transitiveSuccessors(
        graph: KnowledgeGraph,
        nodeId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>
    ): List<KnowledgeNode> {
        if (!graph.containsNode(nodeId)) return emptyList()
        val visited = mutableSetOf<KnowledgeNodeId>()
        val queue = ArrayDeque<KnowledgeNodeId>()
        queue.add(nodeId)
        visited.add(nodeId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val successors = adjacentOutgoing(graph, current, relationshipTypes)
            for (succ in successors) {
                if (visited.add(succ.id)) {
                    queue.add(succ.id)
                }
            }
        }
        visited.remove(nodeId)
        return visited
            .mapNotNull { graph.findNode(it) }
            .sortedWith(compareBy({ it.id.value }, { it.kind.name }))
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private fun adjacentOutgoing(
        graph: KnowledgeGraph,
        nodeId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>?
    ): List<KnowledgeNode> {
        val outgoing = graph.outgoing(nodeId)
        val filtered = if (relationshipTypes == null) {
            outgoing
        } else {
            outgoing.filter { it.relationshipType in relationshipTypes }
        }
        return filtered.mapNotNull { graph.findNode(it.targetId) }
    }

    private fun reconstructPath(
        graph: KnowledgeGraph,
        parentMap: Map<KnowledgeNodeId, KnowledgeNodeId>,
        sourceId: KnowledgeNodeId,
        targetId: KnowledgeNodeId
    ): List<KnowledgeNode> {
        val path = mutableListOf<KnowledgeNodeId>()
        var current: KnowledgeNodeId? = targetId
        while (current != null && current != sourceId) {
            path.add(current)
            current = parentMap[current]
        }
        path.add(sourceId)
        path.reverse()
        return path.mapNotNull { graph.findNode(it) }
    }

    private fun bfsReachable(
        graph: KnowledgeGraph,
        sourceId: KnowledgeNodeId,
        targetId: KnowledgeNodeId,
        relationshipTypes: Set<KnowledgeRelationshipType>?
    ): Boolean {
        val visited = mutableSetOf<KnowledgeNodeId>()
        val queue = ArrayDeque<KnowledgeNodeId>()
        queue.add(sourceId)
        visited.add(sourceId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = adjacentOutgoing(graph, current, relationshipTypes)
            for (neighbor in neighbors) {
                if (neighbor.id == targetId) return true
                if (visited.add(neighbor.id)) {
                    queue.add(neighbor.id)
                }
            }
        }
        return false
    }
}
