package vn.loi.learning.application.knowledge

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeKind
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Kiểm thử InstalledLibraryKnowledgeGraphProjection:
 * - graph rỗng khi không có package ACTIVE,
 * - mỗi active package trở thành PACKAGE node,
 * - node id = packageId.value (canonical identity),
 * - package ARCHIVED/REMOVED không được project,
 * - deterministic node ordering.
 */
class InstalledLibraryKnowledgeGraphProjectionTest {

    // ── Stub InMemory Repository ───────────────────────────────────────────

    private class StubInstalledPackageRepository(
        private val packages: List<InstalledPackage>
    ) : InstalledPackageRepository {
        override fun findById(id: InstalledPackageId): InstalledPackage? =
            packages.find { it.id == id }

        override fun findByPackageId(packageId: PackageId): InstalledPackage? =
            packages.find { it.packageId == packageId }

        override fun findByTopicId(topicId: TopicId): InstalledPackage? =
            packages.find { it.topicId == topicId }

        override fun findAllByState(state: PackageState): List<InstalledPackage> =
            packages.filter { it.state == state }

        override fun findAll(): List<InstalledPackage> = packages

        override fun save(installedPackage: InstalledPackage) {}

        override fun delete(id: InstalledPackageId) {}
    }

    private fun makePackage(
        pkgId: String,
        name: String,
        state: PackageState = PackageState.ACTIVE
    ): InstalledPackage = InstalledPackage.reconstitute(
        id = InstalledPackageId("inst-$pkgId"),
        libraryId = LibraryId("lib-1"),
        packageId = PackageId(pkgId),
        topicId = TopicId(pkgId),
        name = PackageName(name),
        version = PackageVersion("1.0"),
        state = state,
        installedAt = Instant.EPOCH,
        contentCount = 0,
        learningItemCount = 0
    )

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    fun `returns empty graph when no packages are installed`() {
        val repo = StubInstalledPackageRepository(emptyList())
        val projection = InstalledLibraryKnowledgeGraphProjection(repo)
        val graph = projection.project()
        assertTrue(graph.nodes().isEmpty())
        assertTrue(graph.edges().isEmpty())
    }

    @Test
    fun `returns empty graph when all packages are archived or removed`() {
        val repo = StubInstalledPackageRepository(
            listOf(
                makePackage("p1", "Package 1", PackageState.ARCHIVED),
                makePackage("p2", "Package 2", PackageState.REMOVED)
            )
        )
        val projection = InstalledLibraryKnowledgeGraphProjection(repo)
        val graph = projection.project()
        assertTrue(graph.nodes().isEmpty())
    }

    @Test
    fun `projects each active package as a PACKAGE node with canonical packageId`() {
        val repo = StubInstalledPackageRepository(
            listOf(
                makePackage("pkg-japanese", "Japanese N5"),
                makePackage("pkg-english", "English Grammar")
            )
        )
        val projection = InstalledLibraryKnowledgeGraphProjection(repo)
        val graph = projection.project()

        assertEquals(2, graph.nodes().size)
        val nodeIds = graph.nodes().map { it.id.value }.toSet()
        assertTrue("pkg-japanese" in nodeIds)
        assertTrue("pkg-english" in nodeIds)
        assertTrue(graph.nodes().all { it.kind == KnowledgeNodeKind.PACKAGE })
    }

    @Test
    fun `node displayName matches installed package name`() {
        val repo = StubInstalledPackageRepository(
            listOf(makePackage("pkg-kanji", "Basic Kanji 400"))
        )
        val projection = InstalledLibraryKnowledgeGraphProjection(repo)
        val graph = projection.project()

        val node = graph.nodes().first()
        assertEquals("Basic Kanji 400", node.displayName)
    }

    @Test
    fun `excludes archived and removed packages from projection`() {
        val repo = StubInstalledPackageRepository(
            listOf(
                makePackage("pkg-a", "Active Package", PackageState.ACTIVE),
                makePackage("pkg-b", "Archived Package", PackageState.ARCHIVED),
                makePackage("pkg-c", "Removed Package", PackageState.REMOVED)
            )
        )
        val projection = InstalledLibraryKnowledgeGraphProjection(repo)
        val graph = projection.project()

        assertEquals(1, graph.nodes().size)
        assertEquals("pkg-a", graph.nodes().first().id.value)
    }

    @Test
    fun `result graph has no edges (flat projection)`() {
        val repo = StubInstalledPackageRepository(
            listOf(
                makePackage("p1", "Package 1"),
                makePackage("p2", "Package 2")
            )
        )
        val projection = InstalledLibraryKnowledgeGraphProjection(repo)
        val graph = projection.project()

        assertTrue(graph.edges().isEmpty(), "Installed library projection must produce no edges")
    }

    @Test
    fun `node ordering is deterministic regardless of input order`() {
        val packagesA = listOf(
            makePackage("zzz", "ZZZ Package"),
            makePackage("aaa", "AAA Package"),
            makePackage("mmm", "MMM Package")
        )
        val packagesB = listOf(
            makePackage("mmm", "MMM Package"),
            makePackage("zzz", "ZZZ Package"),
            makePackage("aaa", "AAA Package")
        )
        val projA = InstalledLibraryKnowledgeGraphProjection(StubInstalledPackageRepository(packagesA)).project()
        val projB = InstalledLibraryKnowledgeGraphProjection(StubInstalledPackageRepository(packagesB)).project()

        assertEquals(projA.nodes().map { it.id.value }, projB.nodes().map { it.id.value })
        assertEquals(listOf("aaa", "mmm", "zzz"), projA.nodes().map { it.id.value })
    }

    @Test
    fun `projection is usable for graph query API`() {
        val repo = StubInstalledPackageRepository(
            listOf(makePackage("pkg-vocab", "Vocabulary Pack"))
        )
        val projection = InstalledLibraryKnowledgeGraphProjection(repo)
        val graph = projection.project()

        val node = graph.findNode(
            vn.loi.learning.domain.knowledge.model.KnowledgeNodeId("pkg-vocab")
        )
        assertNotNull(node)
        assertEquals(KnowledgeNodeKind.PACKAGE, node.kind)
        assertTrue(graph.containsNode(vn.loi.learning.domain.knowledge.model.KnowledgeNodeId("pkg-vocab")))
    }
}
