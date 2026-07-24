package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.knowledge.model.KnowledgeEdge
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
 * Kiểm thử KnowledgeGraphRecordMapper: round-trip, unknown kind/relationship handling.
 */
class KnowledgeGraphRecordMapperTest {

    private fun buildGraph(
        nodes: List<KnowledgeNode>,
        edges: List<KnowledgeEdge> = emptyList()
    ) = (KnowledgeGraphFactory.build(nodes, edges) as KnowledgeGraphValidationResult.Valid).graph

    private fun node(id: String, kind: KnowledgeNodeKind = KnowledgeNodeKind.TOPIC) =
        KnowledgeNode(KnowledgeNodeId(id), kind, "Display $id")

    // ── toRecord ────────────────────────────────────────────────────────────

    @Test
    fun `toRecord maps all node kinds as canonical enum names`() {
        KnowledgeNodeKind.values().forEach { kind ->
            val graph = buildGraph(listOf(node("n-${kind.name}", kind)))
            val record = KnowledgeGraphRecordMapper.toRecord(graph)
            assertEquals(kind.name, record.nodes.first().kind)
        }
    }

    @Test
    fun `toRecord maps all relationship types as canonical enum names`() {
        val a = node("a")
        val b = node("b")
        KnowledgeRelationshipType.values().forEach { type ->
            val graph = buildGraph(
                listOf(a, b),
                listOf(KnowledgeEdge(KnowledgeNodeId("a"), KnowledgeNodeId("b"), type))
            )
            val record = KnowledgeGraphRecordMapper.toRecord(graph)
            assertEquals(type.name, record.edges.first().relationshipType)
        }
    }

    @Test
    fun `toRecord produces deterministic node ordering`() {
        val graph = buildGraph(listOf(node("z"), node("a"), node("m")))
        val record = KnowledgeGraphRecordMapper.toRecord(graph)
        assertEquals(listOf("a", "m", "z"), record.nodes.map { it.id })
    }

    // ── Round-trip ──────────────────────────────────────────────────────────

    @Test
    fun `mapper round-trip preserves empty graph`() {
        val graph = buildGraph(emptyList())
        val record = KnowledgeGraphRecordMapper.toRecord(graph)
        val restored = KnowledgeGraphRecordMapper.toDomain(record)
        assertEquals(graph, restored)
    }

    @Test
    fun `mapper round-trip preserves full graph`() {
        val pkg = node("pkg", KnowledgeNodeKind.PACKAGE)
        val topic = node("topic", KnowledgeNodeKind.TOPIC)
        val lesson = node("lesson", KnowledgeNodeKind.LESSON)
        val e1 = KnowledgeEdge(KnowledgeNodeId("pkg"), KnowledgeNodeId("topic"), KnowledgeRelationshipType.CONTAINS)
        val e2 = KnowledgeEdge(KnowledgeNodeId("topic"), KnowledgeNodeId("lesson"), KnowledgeRelationshipType.CONTAINS)
        val graph = buildGraph(listOf(pkg, topic, lesson), listOf(e1, e2))
        val record = KnowledgeGraphRecordMapper.toRecord(graph)
        val restored = KnowledgeGraphRecordMapper.toDomain(record)
        assertEquals(graph, restored)
        assertEquals(3, restored.nodes().size)
        assertEquals(2, restored.edges().size)
    }

    @Test
    fun `toDomain skips node with unknown kind`() {
        val record = KnowledgeGraphRecord(
            nodes = listOf(
                KnowledgeNodeRecord("a", "TOPIC", "Topic A"),
                KnowledgeNodeRecord("b", "UNKNOWN_KIND", "Unknown B")
            ),
            edges = emptyList()
        )
        val graph = KnowledgeGraphRecordMapper.toDomain(record)
        assertEquals(1, graph.nodes().size)
        assertEquals("a", graph.nodes().first().id.value)
    }

    @Test
    fun `toDomain skips edge with unknown relationship type`() {
        val record = KnowledgeGraphRecord(
            nodes = listOf(
                KnowledgeNodeRecord("a", "TOPIC", "Topic A"),
                KnowledgeNodeRecord("b", "LESSON", "Lesson B")
            ),
            edges = listOf(
                KnowledgeEdgeRecord("a", "b", "CONTAINS"),
                KnowledgeEdgeRecord("a", "b", "UNKNOWN_REL")
            )
        )
        val graph = KnowledgeGraphRecordMapper.toDomain(record)
        // Only the valid edge is kept; the edge with UNKNOWN_REL is skipped
        // But now there might be an issue if the valid CONTAINS edge is the only one
        // The graph should be valid with just the CONTAINS edge
        assertEquals(1, graph.edges().size)
        assertEquals(KnowledgeRelationshipType.CONTAINS, graph.edges().first().relationshipType)
    }
}
