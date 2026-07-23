package vn.loi.learning.application.library.query

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion

class LibraryQueryProjectionsTest {

    private val libId = LibraryId("lib-test")
    private val instId = InstalledPackageId("pkg-inst-1")
    private val pkgId = PackageId("pkg-1")
    private val topicId = TopicId("topic-1")
    private val colId = CollectionId("col-1")
    private val now = Instant.now()

    @Test
    fun `InstalledPackage toSummary projects correctly`() {
        val pkg = InstalledPackage.reconstitute(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Kanji N1"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = now,
            contentCount = 15,
            learningItemCount = 45
        )

        val summary = pkg.toSummary()

        assertEquals(instId, summary.id)
        assertEquals(libId, summary.libraryId)
        assertEquals(pkgId, summary.packageId)
        assertEquals(topicId, summary.topicId)
        assertEquals("Kanji N1", summary.name)
        assertEquals("1.0.0", summary.version)
        assertEquals(PackageState.ACTIVE, summary.state)
        assertTrue(summary.isActive)
        assertEquals(now, summary.installedAt)
        assertEquals(15, summary.contentCount)
        assertEquals(45, summary.learningItemCount)
    }

    @Test
    fun `Collection toSummary projects correctly`() {
        val col = Collection.reconstitute(
            id = colId,
            libraryId = libId,
            name = CollectionName("JLPT Prep"),
            description = "Japanese prep courses",
            assignedPackageIds = setOf(instId),
            state = CollectionState.ACTIVE,
            createdAt = now
        )

        val summary = col.toSummary()

        assertEquals(colId, summary.id)
        assertEquals(libId, summary.libraryId)
        assertEquals("JLPT Prep", summary.name)
        assertEquals("Japanese prep courses", summary.description)
        assertEquals(CollectionState.ACTIVE, summary.state)
        assertTrue(summary.isActive)
        assertEquals(now, summary.createdAt)
        assertEquals(setOf(instId), summary.assignedPackageIds)
        assertEquals(1, summary.assignedPackagesCount)
    }
}
