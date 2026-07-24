package vn.loi.learning.domain.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.knowledge.model.KnowledgeEdge
import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphFactory
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphValidationIssue
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphValidationResult
import vn.loi.learning.domain.knowledge.model.KnowledgeNode
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeId
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeKind
import vn.loi.learning.domain.knowledge.model.KnowledgeRelationshipType

/**
 * Kiểm thử Checkpoint 1 — Immutable Graph Domain.
 * Và Checkpoint 2 — Deterministic Query API.
 */
class KnowledgeGraphDomainTest {

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun node(id: String, kind: KnowledgeNodeKind = KnowledgeNodeKind.TOPIC, name: String = "Display $id") =
        KnowledgeNode(KnowledgeNodeId(id), kind, name)

    private fun edge(src: String, tgt: String, type: KnowledgeRelationshipType = KnowledgeRelationshipType.CONTAINS) =
        KnowledgeEdge(KnowledgeNodeId(src), KnowledgeNodeId(tgt), type)

    private fun buildValid(
        nodes: List<KnowledgeNode>,
        edges: List<KnowledgeEdge> = emptyList()
    ): KnowledgeGraph {
        val result = KnowledgeGraphFactory.build(nodes, edges)
        require(result is KnowledgeGraphValidationResult.Valid) { "Expected Valid but got: $result" }
        return result.graph
    }

    // ── KnowledgeNodeId ────────────────────────────────────────────────────

    @Test
    fun `KnowledgeNodeId rejects blank value`() {
        assertFailsWith<IllegalArgumentException> { KnowledgeNodeId("") }
        assertFailsWith<IllegalArgumentException> { KnowledgeNodeId("   ") }
    }

    @Test
    fun `KnowledgeNodeId accepts non-blank value and toString equals value`() {
        val id = KnowledgeNodeId("node-abc")
        assertEquals("node-abc", id.value)
        assertEquals("node-abc", id.toString())
    }

    @Test
    fun `KnowledgeNodeId equality by value`() {
        assertEquals(KnowledgeNodeId("x"), KnowledgeNodeId("x"))
        assertFalse(KnowledgeNodeId("x") == KnowledgeNodeId("y"))
    }

    // ── KnowledgeNode ──────────────────────────────────────────────────────

    @Test
    fun `KnowledgeNode rejects blank displayName`() {
        assertFailsWith<IllegalArgumentException> {
            KnowledgeNode(KnowledgeNodeId("n1"), KnowledgeNodeKind.TOPIC, "")
        }
        assertFailsWith<IllegalArgumentException> {
            KnowledgeNode(KnowledgeNodeId("n1"), KnowledgeNodeKind.TOPIC, "   ")
        }
    }

    @Test
    fun `KnowledgeNode equality is based on id and kind NOT displayName`() {
        val n1 = KnowledgeNode(KnowledgeNodeId("n1"), KnowledgeNodeKind.TOPIC, "Name A")
        val n2 = KnowledgeNode(KnowledgeNodeId("n1"), KnowledgeNodeKind.TOPIC, "Name B")
        val n3 = KnowledgeNode(KnowledgeNodeId("n1"), KnowledgeNodeKind.LESSON, "Name A")

        assertEquals(n1, n2, "Nodes with same id+kind must be equal regardless of displayName")
        assertFalse(n1 == n3, "Nodes with different kind must not be equal")
        assertEquals(n1.hashCode(), n2.hashCode())
    }

    @Test
    fun `KnowledgeNode all kinds are accepted`() {
        KnowledgeNodeKind.values().forEach { kind ->
            val n = KnowledgeNode(KnowledgeNodeId("node-${kind.name}"), kind, "Display")
            assertEquals(kind, n.kind)
        }
    }

    // ── KnowledgeEdge ──────────────────────────────────────────────────────

    @Test
    fun `KnowledgeEdge equality by triple`() {
        val e1 = edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        val e2 = edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        val e3 = edge("a", "b", KnowledgeRelationshipType.DEPENDS_ON)
        assertEquals(e1, e2)
        assertFalse(e1 == e3)
    }

    @Test
    fun `KnowledgeRelationshipType all values are present`() {
        val expected = setOf(
            "CONTAINS", "BELONGS_TO", "DEPENDS_ON", "REQUIRES",
            "PREREQUISITE_OF", "RECOMMENDED_AFTER", "REFERENCES"
        )
        val actual = KnowledgeRelationshipType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    // ── Empty graph ────────────────────────────────────────────────────────

    @Test
    fun `empty graph is valid and returns empty collections`() {
        val result = KnowledgeGraphFactory.empty()
        val graph = result.graph
        assertTrue(graph.nodes().isEmpty())
        assertTrue(graph.edges().isEmpty())
    }

    @Test
    fun `empty graph built via build is also valid`() {
        val result = KnowledgeGraphFactory.build(emptyList(), emptyList())
        assertTrue(result is KnowledgeGraphValidationResult.Valid)
        val graph = result.graph
        assertTrue(graph.nodes().isEmpty())
        assertTrue(graph.edges().isEmpty())
    }

    // ── Valid graph ────────────────────────────────────────────────────────

    @Test
    fun `valid graph with nodes and edges succeeds`() {
        val pkg = node("pkg", KnowledgeNodeKind.PACKAGE)
        val topic = node("topic", KnowledgeNodeKind.TOPIC)
        val e = edge("pkg", "topic", KnowledgeRelationshipType.CONTAINS)
        val graph = buildValid(listOf(pkg, topic), listOf(e))
        assertEquals(2, graph.nodes().size)
        assertEquals(1, graph.edges().size)
    }

    // ── Validation: duplicate node id ─────────────────────────────────────

    @Test
    fun `duplicate node id produces Invalid with DuplicateNodeId issue`() {
        val n1 = node("dup", KnowledgeNodeKind.TOPIC, "Topic A")
        val n2 = node("dup", KnowledgeNodeKind.LESSON, "Topic B")
        val result = KnowledgeGraphFactory.build(listOf(n1, n2), emptyList())
        assertTrue(result is KnowledgeGraphValidationResult.Invalid)
        val issues = result.issues
        assertTrue(issues.any { it is KnowledgeGraphValidationIssue.DuplicateNodeId })
    }

    // ── Validation: duplicate edge ────────────────────────────────────────

    @Test
    fun `duplicate edge produces Invalid with DuplicateEdge issue`() {
        val a = node("a")
        val b = node("b")
        val e = edge("a", "b")
        val result = KnowledgeGraphFactory.build(listOf(a, b), listOf(e, e))
        assertTrue(result is KnowledgeGraphValidationResult.Invalid)
        val issues = result.issues
        assertTrue(issues.any { it is KnowledgeGraphValidationIssue.DuplicateEdge })
    }

    // ── Validation: self edge ─────────────────────────────────────────────

    @Test
    fun `self edge produces Invalid with SelfEdge issue`() {
        val a = node("a")
        val e = edge("a", "a")
        val result = KnowledgeGraphFactory.build(listOf(a), listOf(e))
        assertTrue(result is KnowledgeGraphValidationResult.Invalid)
        val issues = result.issues
        assertTrue(issues.any { it is KnowledgeGraphValidationIssue.SelfEdge })
    }

    // ── Validation: dangling source ───────────────────────────────────────

    @Test
    fun `edge with missing source node produces Invalid with MissingSourceNode`() {
        val b = node("b")
        val e = edge("missing", "b")
        val result = KnowledgeGraphFactory.build(listOf(b), listOf(e))
        assertTrue(result is KnowledgeGraphValidationResult.Invalid)
        val issues = result.issues
        assertTrue(issues.any { it is KnowledgeGraphValidationIssue.MissingSourceNode })
    }

    // ── Validation: dangling target ───────────────────────────────────────

    @Test
    fun `edge with missing target node produces Invalid with MissingTargetNode`() {
        val a = node("a")
        val e = edge("a", "missing")
        val result = KnowledgeGraphFactory.build(listOf(a), listOf(e))
        assertTrue(result is KnowledgeGraphValidationResult.Invalid)
        val issues = result.issues
        assertTrue(issues.any { it is KnowledgeGraphValidationIssue.MissingTargetNode })
    }

    // ── Typed relationships ───────────────────────────────────────────────

    @Test
    fun `all typed relationships can be stored and retrieved`() {
        val nodes = listOf(node("a"), node("b"))
        KnowledgeRelationshipType.values().forEach { type ->
            val graph = buildValid(nodes, listOf(edge("a", "b", type)))
            val found = graph.edges().first()
            assertEquals(type, found.relationshipType)
        }
    }

    // ── Deterministic equality ────────────────────────────────────────────

    @Test
    fun `two graphs with same nodes and edges in different insertion order are equal`() {
        val n1 = node("z")
        val n2 = node("a")
        val e1 = edge("z", "a")

        val g1 = buildValid(listOf(n1, n2), listOf(e1))
        val g2 = buildValid(listOf(n2, n1), listOf(e1))
        assertEquals(g1, g2, "Graphs must be equal regardless of input insertion order")
        assertEquals(g1.hashCode(), g2.hashCode())
    }

    // ── Deterministic ordering ────────────────────────────────────────────

    @Test
    fun `nodes are returned in deterministic order by id value`() {
        val nodes = listOf(node("z"), node("a"), node("m"))
        val graph = buildValid(nodes)
        val ids = graph.nodes().map { it.id.value }
        assertEquals(listOf("a", "m", "z"), ids)
    }

    @Test
    fun `edges are returned in deterministic order by source then target then type`() {
        val nodes = listOf(node("a"), node("b"), node("c"))
        val edges = listOf(
            edge("b", "c", KnowledgeRelationshipType.BELONGS_TO),
            edge("a", "c", KnowledgeRelationshipType.CONTAINS),
            edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        )
        val graph = buildValid(nodes, edges)
        val sortedEdges = graph.edges()
        assertEquals("a", sortedEdges[0].sourceId.value)
        assertEquals("b", sortedEdges[0].targetId.value)
        assertEquals("a", sortedEdges[1].sourceId.value)
        assertEquals("c", sortedEdges[1].targetId.value)
        assertEquals("b", sortedEdges[2].sourceId.value)
    }

    // ── Immutable exposed collections ─────────────────────────────────────

    @Test
    fun `nodes list is immutable (not modifiable)`() {
        val graph = buildValid(listOf(node("a")))
        val nodes = graph.nodes()
        // Verify cast to MutableList would fail or list is not mutable
        assertFalse(nodes is java.util.ArrayList, "nodes() must not return a mutable ArrayList")
    }

    @Test
    fun `edges list is immutable (not modifiable)`() {
        val a = node("a")
        val b = node("b")
        val graph = buildValid(listOf(a, b), listOf(edge("a", "b")))
        val edges = graph.edges()
        assertFalse(edges is java.util.ArrayList, "edges() must not return a mutable ArrayList")
    }

    // ── Query API tests (Checkpoint 2) ────────────────────────────────────

    @Test
    fun `findNode returns correct node or null`() {
        val n = node("node-x", KnowledgeNodeKind.LESSON)
        val graph = buildValid(listOf(n))
        assertEquals(n, graph.findNode(KnowledgeNodeId("node-x")))
        assertNull(graph.findNode(KnowledgeNodeId("missing")))
    }

    @Test
    fun `containsNode returns true for existing and false for missing`() {
        val n = node("existing")
        val graph = buildValid(listOf(n))
        assertTrue(graph.containsNode(KnowledgeNodeId("existing")))
        assertFalse(graph.containsNode(KnowledgeNodeId("ghost")))
    }

    @Test
    fun `outgoing returns edges from a node`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val e1 = edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        val e2 = edge("a", "c", KnowledgeRelationshipType.REFERENCES)
        val e3 = edge("b", "c")
        val graph = buildValid(listOf(a, b, c), listOf(e1, e2, e3))
        val out = graph.outgoing(KnowledgeNodeId("a"))
        assertEquals(2, out.size)
        assertTrue(out.contains(e1))
        assertTrue(out.contains(e2))
    }

    @Test
    fun `outgoing returns empty for missing node`() {
        val graph = buildValid(listOf(node("a")))
        assertTrue(graph.outgoing(KnowledgeNodeId("ghost")).isEmpty())
    }

    @Test
    fun `incoming returns edges to a node`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val e1 = edge("a", "c")
        val e2 = edge("b", "c")
        val graph = buildValid(listOf(a, b, c), listOf(e1, e2))
        val inc = graph.incoming(KnowledgeNodeId("c"))
        assertEquals(2, inc.size)
    }

    @Test
    fun `neighbors returns unique nodes from both incoming and outgoing`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val e1 = edge("a", "b")
        val e2 = edge("c", "a")
        val graph = buildValid(listOf(a, b, c), listOf(e1, e2))
        val neighbors = graph.neighbors(KnowledgeNodeId("a"))
        assertEquals(2, neighbors.size)
        val neighborIds = neighbors.map { it.id.value }.toSet()
        assertTrue("b" in neighborIds)
        assertTrue("c" in neighborIds)
    }

    @Test
    fun `neighbors has no duplicates`() {
        val a = node("a")
        val b = node("b")
        val e1 = edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        val e2 = edge("b", "a", KnowledgeRelationshipType.BELONGS_TO)
        val graph = buildValid(listOf(a, b), listOf(e1, e2))
        val neighborsOfA = graph.neighbors(KnowledgeNodeId("a"))
        assertEquals(1, neighborsOfA.size) // b appears only once
    }

    @Test
    fun `successors filters by relationship type`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val e1 = edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        val e2 = edge("a", "c", KnowledgeRelationshipType.REFERENCES)
        val graph = buildValid(listOf(a, b, c), listOf(e1, e2))

        val containsSuccessors = graph.successors(KnowledgeNodeId("a"), KnowledgeRelationshipType.CONTAINS)
        assertEquals(listOf(b), containsSuccessors)

        val refsSuccessors = graph.successors(KnowledgeNodeId("a"), KnowledgeRelationshipType.REFERENCES)
        assertEquals(listOf(c), refsSuccessors)
    }

    @Test
    fun `predecessors filters by relationship type`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val e1 = edge("a", "c", KnowledgeRelationshipType.CONTAINS)
        val e2 = edge("b", "c", KnowledgeRelationshipType.DEPENDS_ON)
        val graph = buildValid(listOf(a, b, c), listOf(e1, e2))

        val containsPreds = graph.predecessors(KnowledgeNodeId("c"), KnowledgeRelationshipType.CONTAINS)
        assertEquals(listOf(a), containsPreds)

        val dependPreds = graph.predecessors(KnowledgeNodeId("c"), KnowledgeRelationshipType.DEPENDS_ON)
        assertEquals(listOf(b), dependPreds)
    }

    @Test
    fun `edgesBetween returns only edges between two specific nodes`() {
        val a = node("a")
        val b = node("b")
        val c = node("c")
        val e1 = edge("a", "b", KnowledgeRelationshipType.CONTAINS)
        val e2 = edge("a", "b", KnowledgeRelationshipType.REFERENCES)
        val e3 = edge("a", "c")
        val graph = buildValid(listOf(a, b, c), listOf(e1, e2, e3))
        val between = graph.edgesBetween(KnowledgeNodeId("a"), KnowledgeNodeId("b"))
        assertEquals(2, between.size)
        assertTrue(between.any { it.relationshipType == KnowledgeRelationshipType.CONTAINS })
        assertTrue(between.any { it.relationshipType == KnowledgeRelationshipType.REFERENCES })
    }

    @Test
    fun `edgesBetween returns empty when no edges between nodes`() {
        val a = node("a")
        val b = node("b")
        val graph = buildValid(listOf(a, b))
        assertTrue(graph.edgesBetween(KnowledgeNodeId("a"), KnowledgeNodeId("b")).isEmpty())
    }

    @Test
    fun `nodesOfKind filters correctly`() {
        val pkg = node("pkg", KnowledgeNodeKind.PACKAGE)
        val topic1 = node("t1", KnowledgeNodeKind.TOPIC)
        val topic2 = node("t2", KnowledgeNodeKind.TOPIC)
        val lesson = node("l1", KnowledgeNodeKind.LESSON)
        val graph = buildValid(listOf(pkg, topic1, topic2, lesson))

        val topics = graph.nodesOfKind(KnowledgeNodeKind.TOPIC)
        assertEquals(2, topics.size)
        assertTrue(topics.all { it.kind == KnowledgeNodeKind.TOPIC })

        val packages = graph.nodesOfKind(KnowledgeNodeKind.PACKAGE)
        assertEquals(1, packages.size)

        val resources = graph.nodesOfKind(KnowledgeNodeKind.RESOURCE)
        assertTrue(resources.isEmpty())
    }

    @Test
    fun `missing node queries return empty collections`() {
        val graph = buildValid(listOf(node("a")))
        val ghost = KnowledgeNodeId("ghost")
        assertTrue(graph.outgoing(ghost).isEmpty())
        assertTrue(graph.incoming(ghost).isEmpty())
        assertTrue(graph.neighbors(ghost).isEmpty())
        assertTrue(graph.successors(ghost, KnowledgeRelationshipType.CONTAINS).isEmpty())
        assertTrue(graph.predecessors(ghost, KnowledgeRelationshipType.CONTAINS).isEmpty())
    }

    @Test
    fun `neighbors ordering is deterministic`() {
        val hub = node("hub")
        val z = node("z")
        val a = node("a")
        val m = node("m")
        val graph = buildValid(
            listOf(hub, z, a, m),
            listOf(
                edge("hub", "z"),
                edge("hub", "a"),
                edge("hub", "m")
            )
        )
        val neighborIds = graph.neighbors(KnowledgeNodeId("hub")).map { it.id.value }
        assertEquals(listOf("a", "m", "z"), neighborIds)
    }

    @Test
    fun `successors ordering is deterministic`() {
        val src = node("src")
        val z = node("z")
        val a = node("a")
        val graph = buildValid(
            listOf(src, z, a),
            listOf(
                edge("src", "z", KnowledgeRelationshipType.CONTAINS),
                edge("src", "a", KnowledgeRelationshipType.CONTAINS)
            )
        )
        val ids = graph.successors(KnowledgeNodeId("src"), KnowledgeRelationshipType.CONTAINS)
            .map { it.id.value }
        assertEquals(listOf("a", "z"), ids)
    }
}
