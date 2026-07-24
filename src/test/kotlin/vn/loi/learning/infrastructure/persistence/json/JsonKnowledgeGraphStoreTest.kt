package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
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
import vn.loi.learning.infrastructure.persistence.mapper.KnowledgeGraphRecordMapper
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedKnowledgeGraphRepository

/**
 * Kiểm thử JsonKnowledgeGraphStore: missing-file, save/load round-trip, restart behavior.
 */
class JsonKnowledgeGraphStoreTest {

    private fun buildGraph(
        nodes: List<KnowledgeNode>,
        edges: List<KnowledgeEdge> = emptyList()
    ) = (KnowledgeGraphFactory.build(nodes, edges) as KnowledgeGraphValidationResult.Valid).graph

    private fun node(id: String, kind: KnowledgeNodeKind = KnowledgeNodeKind.TOPIC) =
        KnowledgeNode(KnowledgeNodeId(id), kind, "Display $id")

    // ── Missing file behavior ─────────────────────────────────────────────

    @Test
    fun `load returns empty graph record when file does not exist`() {
        val directory = Files.createTempDirectory("json-knowledge-graph-store-missing")
        val path = directory.resolve("knowledge-graph.json")
        val store = JsonKnowledgeGraphStore(path)

        val record = store.load()
        assertTrue(record.nodes.isEmpty())
        assertTrue(record.edges.isEmpty())
    }

    // ── Save/load round-trip ───────────────────────────────────────────────

    @Test
    fun `saves and loads graph with nodes and edges`() {
        val directory = Files.createTempDirectory("json-knowledge-graph-store-test")
        val path = directory.resolve("knowledge-graph.json")
        val store = JsonKnowledgeGraphStore(path)

        val pkg = node("pkg", KnowledgeNodeKind.PACKAGE)
        val topic = node("topic", KnowledgeNodeKind.TOPIC)
        val e = KnowledgeEdge(
            KnowledgeNodeId("pkg"),
            KnowledgeNodeId("topic"),
            KnowledgeRelationshipType.CONTAINS
        )
        val graph = buildGraph(listOf(pkg, topic), listOf(e))
        val record = KnowledgeGraphRecordMapper.toRecord(graph)

        store.save(record)

        val loadedRecord = store.load()
        val loadedGraph = KnowledgeGraphRecordMapper.toDomain(loadedRecord)

        assertEquals(graph, loadedGraph)
    }

    @Test
    fun `saves and loads empty graph`() {
        val directory = Files.createTempDirectory("json-knowledge-graph-store-empty")
        val path = directory.resolve("knowledge-graph.json")
        val store = JsonKnowledgeGraphStore(path)

        val graph = buildGraph(emptyList())
        val record = KnowledgeGraphRecordMapper.toRecord(graph)
        store.save(record)

        val loadedRecord = store.load()
        val loadedGraph = KnowledgeGraphRecordMapper.toDomain(loadedRecord)

        assertEquals(graph, loadedGraph)
    }

    // ── Restart-safe via StoreBackedRepository ────────────────────────────

    @Test
    fun `repository save-close-reopen-get restores graph identically`() {
        val directory = Files.createTempDirectory("json-knowledge-graph-restart-test")
        val path = directory.resolve("knowledge-graph.json")

        val pkg = node("pkg", KnowledgeNodeKind.PACKAGE)
        val topic = node("t1", KnowledgeNodeKind.TOPIC)
        val lesson = node("l1", KnowledgeNodeKind.LESSON)
        val e1 = KnowledgeEdge(KnowledgeNodeId("pkg"), KnowledgeNodeId("t1"), KnowledgeRelationshipType.CONTAINS)
        val e2 = KnowledgeEdge(KnowledgeNodeId("t1"), KnowledgeNodeId("l1"), KnowledgeRelationshipType.CONTAINS)
        val graph = buildGraph(listOf(pkg, topic, lesson), listOf(e1, e2))

        // First session: save
        val repo1 = StoreBackedKnowledgeGraphRepository(JsonKnowledgeGraphStore(path))
        repo1.save(graph)

        // Simulate restart: new store and repository instances
        val repo2 = StoreBackedKnowledgeGraphRepository(JsonKnowledgeGraphStore(path))
        val restored = repo2.get()

        assertEquals(graph, restored)
        assertEquals(3, restored.nodes().size)
        assertEquals(2, restored.edges().size)
    }

    @Test
    fun `repository get returns empty graph when no file exists`() {
        val directory = Files.createTempDirectory("json-knowledge-graph-missing-repo")
        val path = directory.resolve("knowledge-graph.json")
        val repo = StoreBackedKnowledgeGraphRepository(JsonKnowledgeGraphStore(path))
        val graph = repo.get()
        assertTrue(graph.nodes().isEmpty())
        assertTrue(graph.edges().isEmpty())
    }
}
