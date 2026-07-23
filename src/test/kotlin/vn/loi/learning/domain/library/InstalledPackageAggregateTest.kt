package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion

class InstalledPackageAggregateTest {

    private val libId = LibraryId("lib-1")
    private val instId = InstalledPackageId("inst-pkg-100")
    private val pkgId = PackageId("pkg-n1")
    private val topicId = TopicId("topic-kanji-n1")

    private val samplePkg = InstalledPackage(
        id = instId,
        libraryId = libId,
        packageId = pkgId,
        topicId = topicId,
        name = PackageName("Kanji N1 Master"),
        version = PackageVersion("1.0"),
        contentCount = 50,
        learningItemCount = 100
    )

    @Test
    fun `installed package validates non-negative counts`() {
        assertTrue(samplePkg.isActive)
        assertFalse(samplePkg.isArchived)
        assertFalse(samplePkg.isRemoved)

        assertFailsWith<IllegalArgumentException> {
            samplePkg.copy(contentCount = -1)
        }

        assertFailsWith<IllegalArgumentException> {
            samplePkg.copy(learningItemCount = -5)
        }
    }

    @Test
    fun `installed package transitions through archive restore remove state lifecycle`() {
        // Archive
        val archivedPkg = samplePkg.archive()
        assertEquals(PackageState.ARCHIVED, archivedPkg.state)
        assertTrue(archivedPkg.isArchived)
        assertFalse(archivedPkg.isActive)

        // Archive again is idempotent
        assertEquals(archivedPkg, archivedPkg.archive())

        // Restore
        val restoredPkg = archivedPkg.restore()
        assertEquals(PackageState.ACTIVE, restoredPkg.state)
        assertTrue(restoredPkg.isActive)

        // Remove
        val removedPkg = restoredPkg.remove()
        assertEquals(PackageState.REMOVED, removedPkg.state)
        assertTrue(removedPkg.isRemoved)

        // Cannot archive or restore a REMOVED package
        assertFailsWith<IllegalStateException> { removedPkg.archive() }
        assertFailsWith<IllegalStateException> { removedPkg.restore() }
    }
}
