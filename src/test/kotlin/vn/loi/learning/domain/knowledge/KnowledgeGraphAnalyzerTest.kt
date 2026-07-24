package vn.loi.learning.domain.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.knowledge.model.KnowledgeEdge
import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphFactory
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphValidationResult
import vn.loi.learning.domain.knowledge.model.KnowledgeNode
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeId
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeKind
import vn.loi.learning.domain.knowledge.model.KnowledgeRelationshipType
import vn.loi.learning.domain.knowledge.service.KnowledgeGraphAnalyzer
import vn.loi.learning.domain.knowledge.service.PathResult
import vn.loi.learning.domain.knowledge.service.TopologicalOrderResult

/**
 * Kiểm thử Checkpoint 3 — Graph Traversal and Analysis.
 */
class KnowledgeGraphAnalyzerTest {

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun node(id: String, kind: KnowledgeNodeKind = KnowledgeNodeKind.TOPIC) =
        KnowledgeNode(KnowledgeNodeId(id), kind, "Display $id")

    private fun edge(src: String, tgt: String, type: KnowledgeRelationshipType = KnowledgeRelationshipType.DEPENDS_ON) =
        KnowledgeEdge(KnowledgeNodeId(src), KnowledgeNodeId(tgt), type)

    private fun buildGraph(
        nodes: List<KnowledgeNode>,
        edges: List<KnowledgeEdge> = emptyList()
    ): KnowledgeGraph {
        val result = KnowledgeGraphFactory.build(nodes, edges)
        require(result is KnowledgeGraphValidationResult.Valid)
        return result.graph
    }

    // ── isReachable ────────────────────────────────────────────────────────

    @Test
    fun `isReachable returns true for directly connected nodes`() {
        val a = node("a")
        val b = node("b")
        val graph = buildGraph(listOf(a, b), listOf(edge("a", "b")))
        assertTrue(KnowledgeGraphAnalyzer.isReachable(graph, KnowledgeNodeId("a"), KnowledgeNodeId("b")))
    }

    @Test
    fun `isReachable returns true for indirectly connected nodes`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val graph = buildGraph(listOf(a, b, c), listOf(edge("a", "b"), edge("b", "c")))
        assertTrue(KnowledgeGraphAnalyzer.isReachable(graph, KnowledgeNodeId("a"), KnowledgeNodeId("c")))
    }

    @Test
    fun `isReachable returns false for unreachable nodes`() {
        val a = node("a")
        val b = node("b")
        val graph = buildGraph(listOf(a, b)) // no edges
        assertFalse(KnowledgeGraphAnalyzer.isReachable(graph, KnowledgeNodeId("a"), KnowledgeNodeId("b")))
    }

    @Test
    fun `isReachable returns true when source equals target`() {
        val a = node("a")
        val graph = buildGraph(listOf(a))
        assertTrue(KnowledgeGraphAnalyzer.isReachable(graph, KnowledgeNodeId("a"), KnowledgeNodeId("a")))
    }

    @Test
    fun `isReachable returns false for missing node`() {
        val a = node("a")
        val graph = buildGraph(listOf(a))
        assertFalse(KnowledgeGraphAnalyzer.isReachable(graph, KnowledgeNodeId("a"), KnowledgeNodeId("ghost")))
        assertFalse(KnowledgeGraphAnalyzer.isReachable(graph, KnowledgeNodeId("ghost"), KnowledgeNodeId("a")))
    }

    @Test
    fun `isReachable respects relationship type filter`() {
        val a = node("a")
        val b = node("b")
        // Only CONTAINS edge, not DEPENDS_ON
        val graph = buildGraph(listOf(a, b), listOf(
            edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        ))
        // Filter by DEPENDS_ON should return false
        assertFalse(
            KnowledgeGraphAnalyzer.isReachable(
                graph, KnowledgeNodeId("a"), KnowledgeNodeId("b"),
                setOf(KnowledgeRelationshipType.DEPENDS_ON)
            )
        )
        // Filter by CONTAINS should return true
        assertTrue(
            KnowledgeGraphAnalyzer.isReachable(
                graph, KnowledgeNodeId("a"), KnowledgeNodeId("b"),
                setOf(KnowledgeRelationshipType.CONTAINS)
            )
        )
    }

    // ── reachableFrom ──────────────────────────────────────────────────────

    @Test
    fun `reachableFrom returns all transitively reachable nodes`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val d = node("d")
        val graph = buildGraph(
            listOf(a, b, c, d),
            listOf(edge("a", "b"), edge("a", "c"), edge("c", "d"))
        )
        val reachable = KnowledgeGraphAnalyzer.reachableFrom(graph, KnowledgeNodeId("a"))
            .map { it.id.value }.toSet()
        assertEquals(setOf("b", "c", "d"), reachable)
    }

    @Test
    fun `reachableFrom excludes source node itself`() {
        val a = node("a")
        val b = node("b")
        val graph = buildGraph(listOf(a, b), listOf(edge("a", "b")))
        val reachable = KnowledgeGraphAnalyzer.reachableFrom(graph, KnowledgeNodeId("a"))
        assertFalse(reachable.any { it.id.value == "a" })
    }

    @Test
    fun `reachableFrom returns empty for missing source`() {
        val graph = buildGraph(listOf(node("a")))
        assertTrue(KnowledgeGraphAnalyzer.reachableFrom(graph, KnowledgeNodeId("ghost")).isEmpty())
    }

    @Test
    fun `reachableFrom is cycle-safe`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        // Cycle: a -> b -> c -> a
        val graph = buildGraph(
            listOf(a, b, c),
            listOf(edge("a", "b"), edge("b", "c"), edge("c", "a"))
        )
        val reachable = KnowledgeGraphAnalyzer.reachableFrom(graph, KnowledgeNodeId("a"))
            .map { it.id.value }.toSet()
        assertEquals(setOf("b", "c"), reachable)
    }

    // ── shortestPath ───────────────────────────────────────────────────────

    @Test
    fun `shortestPath returns path of single node when source equals target`() {
        val a = node("a")
        val graph = buildGraph(listOf(a))
        val result = KnowledgeGraphAnalyzer.shortestPath(graph, KnowledgeNodeId("a"), KnowledgeNodeId("a"))
        assertTrue(result is PathResult.Found)
        assertEquals(listOf(a), result.path)
    }

    @Test
    fun `shortestPath returns NotFound when unreachable`() {
        val a = node("a")
        val b = node("b")
        val graph = buildGraph(listOf(a, b))
        val result = KnowledgeGraphAnalyzer.shortestPath(graph, KnowledgeNodeId("a"), KnowledgeNodeId("b"))
        assertEquals(PathResult.NotFound, result)
    }

    @Test
    fun `shortestPath returns NodeNotFound for missing source`() {
        val graph = buildGraph(listOf(node("a")))
        val result = KnowledgeGraphAnalyzer.shortestPath(
            graph, KnowledgeNodeId("ghost"), KnowledgeNodeId("a")
        )
        assertTrue(result is PathResult.NodeNotFound)
    }

    @Test
    fun `shortestPath finds minimum-length path`() {
        // Diamond graph: a -> b -> d AND a -> c -> d
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val d = node("d")
        val graph = buildGraph(
            listOf(a, b, c, d),
            listOf(edge("a", "b"), edge("b", "d"), edge("a", "c"), edge("c", "d"))
        )
        val result = KnowledgeGraphAnalyzer.shortestPath(graph, KnowledgeNodeId("a"), KnowledgeNodeId("d"))
        assertTrue(result is PathResult.Found)
        val path = result.path
        assertEquals(3, path.size) // a -> x -> d
        assertEquals("a", path.first().id.value)
        assertEquals("d", path.last().id.value)
    }

    @Test
    fun `shortestPath tie-breaking is deterministic`() {
        // Both paths have equal length; deterministic tie-breaking picks the lexicographically smaller path
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val d = node("d")
        val graph = buildGraph(
            listOf(a, b, c, d),
            listOf(edge("a", "b"), edge("b", "d"), edge("a", "c"), edge("c", "d"))
        )
        val result1 = KnowledgeGraphAnalyzer.shortestPath(graph, KnowledgeNodeId("a"), KnowledgeNodeId("d"))
        val result2 = KnowledgeGraphAnalyzer.shortestPath(graph, KnowledgeNodeId("a"), KnowledgeNodeId("d"))
        // Both runs must produce the same result
        assertEquals(result1, result2)
        // Tie-breaking: b comes before c alphabetically, so path via b is chosen
        val path = (result1 as PathResult.Found).path
        assertEquals("b", path[1].id.value)
    }

    // ── detectDirectedCycles ───────────────────────────────────────────────

    @Test
    fun `detectDirectedCycles returns empty for acyclic graph`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val graph = buildGraph(
            listOf(a, b, c),
            listOf(edge("a", "b"), edge("b", "c"))
        )
        val cycles = KnowledgeGraphAnalyzer.detectDirectedCycles(graph)
        assertTrue(cycles.isEmpty())
    }

    @Test
    fun `detectDirectedCycles detects simple cycle`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val graph = buildGraph(
            listOf(a, b, c),
            listOf(edge("a", "b"), edge("b", "c"), edge("c", "a"))
        )
        val cycles = KnowledgeGraphAnalyzer.detectDirectedCycles(graph)
        assertFalse(cycles.isEmpty(), "Should detect cycle nodes")
        val cycleIds = cycles.map { it.id.value }.toSet()
        assertTrue(cycleIds.containsAll(setOf("a", "b", "c")))
    }

    @Test
    fun `detectDirectedCycles respects relationship type filter`() {
        val a = node("a")
        val b = node("b")
        // Cycle via DEPENDS_ON, but we filter for CONTAINS only
        val graph = buildGraph(
            listOf(a, b),
            listOf(
                edge("a", "b", KnowledgeRelationshipType.DEPENDS_ON),
                edge("b", "a", KnowledgeRelationshipType.DEPENDS_ON)
            )
        )
        val cyclesWithContains = KnowledgeGraphAnalyzer.detectDirectedCycles(
            graph, setOf(KnowledgeRelationshipType.CONTAINS)
        )
        assertTrue(cyclesWithContains.isEmpty())

        val cyclesWithDepends = KnowledgeGraphAnalyzer.detectDirectedCycles(
            graph, setOf(KnowledgeRelationshipType.DEPENDS_ON)
        )
        assertFalse(cyclesWithDepends.isEmpty())
    }

    // ── topologicalOrder ───────────────────────────────────────────────────

    @Test
    fun `topologicalOrder returns Ordered for DAG`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val graph = buildGraph(
            listOf(a, b, c),
            listOf(edge("a", "b"), edge("b", "c"))
        )
        val result = KnowledgeGraphAnalyzer.topologicalOrder(
            graph, setOf(KnowledgeRelationshipType.DEPENDS_ON)
        )
        assertTrue(result is TopologicalOrderResult.Ordered)
        val ordered = result.nodes
        // a must come before b, b before c
        assertTrue(ordered.indexOf(a) < ordered.indexOf(b))
        assertTrue(ordered.indexOf(b) < ordered.indexOf(c))
    }

    @Test
    fun `topologicalOrder returns CycleDetected for cyclic graph`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val graph = buildGraph(
            listOf(a, b, c),
            listOf(edge("a", "b"), edge("b", "c"), edge("c", "a"))
        )
        val result = KnowledgeGraphAnalyzer.topologicalOrder(
            graph, setOf(KnowledgeRelationshipType.DEPENDS_ON)
        )
        assertTrue(result is TopologicalOrderResult.CycleDetected)
    }

    @Test
    fun `topologicalOrder is deterministic for equal in-degree nodes`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val d = node("d")
        // a and b both have in-degree 0
        val graph = buildGraph(
            listOf(a, b, c, d),
            listOf(edge("a", "d"), edge("b", "d"), edge("c", "d"))
        )
        val result = KnowledgeGraphAnalyzer.topologicalOrder(
            graph, setOf(KnowledgeRelationshipType.DEPENDS_ON)
        )
        val result2 = KnowledgeGraphAnalyzer.topologicalOrder(
            graph, setOf(KnowledgeRelationshipType.DEPENDS_ON)
        )
        assertEquals(result, result2)
    }

    // ── transitiveSuccessors (transitive dependency/prerequisite) ─────────

    @Test
    fun `transitiveSuccessors finds all transitive dependencies`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val d = node("d")
        val graph = buildGraph(
            listOf(a, b, c, d),
            listOf(
                edge("a", "b", KnowledgeRelationshipType.DEPENDS_ON),
                edge("b", "c", KnowledgeRelationshipType.DEPENDS_ON),
                edge("c", "d", KnowledgeRelationshipType.DEPENDS_ON)
            )
        )
        val result = KnowledgeGraphAnalyzer.transitiveSuccessors(
            graph, KnowledgeNodeId("a"), setOf(KnowledgeRelationshipType.DEPENDS_ON)
        ).map { it.id.value }.toSet()
        assertEquals(setOf("b", "c", "d"), result)
    }

    @Test
    fun `transitiveSuccessors is cycle-safe`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        // Cycle: a -> b -> c -> a
        val graph = buildGraph(
            listOf(a, b, c),
            listOf(
                edge("a", "b", KnowledgeRelationshipType.DEPENDS_ON),
                edge("b", "c", KnowledgeRelationshipType.DEPENDS_ON),
                edge("c", "a", KnowledgeRelationshipType.DEPENDS_ON)
            )
        )
        val result = KnowledgeGraphAnalyzer.transitiveSuccessors(
            graph, KnowledgeNodeId("a"), setOf(KnowledgeRelationshipType.DEPENDS_ON)
        ).map { it.id.value }.toSet()
        assertEquals(setOf("b", "c"), result) // a excluded (it's the source)
    }

    @Test
    fun `transitiveSuccessors filters by relationship type`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val graph = buildGraph(
            listOf(a, b, c),
            listOf(
                edge("a", "b", KnowledgeRelationshipType.DEPENDS_ON),
                edge("a", "c", KnowledgeRelationshipType.REFERENCES)
            )
        )
        val dependsResult = KnowledgeGraphAnalyzer.transitiveSuccessors(
            graph, KnowledgeNodeId("a"), setOf(KnowledgeRelationshipType.DEPENDS_ON)
        ).map { it.id.value }
        assertEquals(listOf("b"), dependsResult)
    }

    @Test
    fun `transitiveSuccessors returns empty for missing source`() {
        val graph = buildGraph(listOf(node("a")))
        val result = KnowledgeGraphAnalyzer.transitiveSuccessors(
            graph, KnowledgeNodeId("ghost"), setOf(KnowledgeRelationshipType.DEPENDS_ON)
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `transitiveSuccessors result is sorted deterministically`() {
        val a = node("a")
        val z = node("z")
        val m = node("m")
        val x = node("x")
        val graph = buildGraph(
            listOf(a, z, m, x),
            listOf(
                edge("a", "z", KnowledgeRelationshipType.REQUIRES),
                edge("a", "m", KnowledgeRelationshipType.REQUIRES),
                edge("a", "x", KnowledgeRelationshipType.REQUIRES)
            )
        )
        val ids = KnowledgeGraphAnalyzer.transitiveSuccessors(
            graph, KnowledgeNodeId("a"), setOf(KnowledgeRelationshipType.REQUIRES)
        ).map { it.id.value }
        assertEquals(listOf("m", "x", "z"), ids)
    }
}
