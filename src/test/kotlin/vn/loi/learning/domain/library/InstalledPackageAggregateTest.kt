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

    @Test
    fun `install factory creates InstalledPackage in ACTIVE state and emits PackageInstalledEvent`() {
        val mutation = InstalledPackage.install(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Kanji N1 Master"),
            version = PackageVersion("1.0"),
            contentCount = 50,
            learningItemCount = 100
        )

        val pkg = mutation.aggregate
        val event = mutation.event

        assertEquals(instId, pkg.id)
        assertEquals(libId, pkg.libraryId)
        assertEquals(pkgId, pkg.packageId)
        assertEquals(topicId, pkg.topicId)
        assertEquals(PackageState.ACTIVE, pkg.state)
        assertTrue(pkg.isActive)
        assertEquals(50, pkg.contentCount)
        assertEquals(100, pkg.learningItemCount)

        assertEquals(instId, event.installedPackageId)
        assertEquals(libId, event.libraryId)
        assertEquals(pkgId, event.packageId)
        assertEquals(topicId, event.topicId)
        assertEquals(PackageVersion("1.0"), event.version)
    }

    @Test
    fun `installed package validates non-negative counts`() {
        assertFailsWith<IllegalArgumentException> {
            InstalledPackage.install(
                id = instId,
                libraryId = libId,
                packageId = pkgId,
                topicId = topicId,
                name = PackageName("Kanji N1 Master"),
                version = PackageVersion("1.0"),
                contentCount = -1,
                learningItemCount = 100
            )
        }
    }

    @Test
    fun `installed package transitions through archive restore remove lifecycle emitting events`() {
        val samplePkg = InstalledPackage.install(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Kanji N1 Master"),
            version = PackageVersion("1.0"),
            contentCount = 50,
            learningItemCount = 100
        ).aggregate

        // Archive
        val archiveMutation = samplePkg.archive()
        val archivedPkg = archiveMutation.aggregate
        val archiveEvent = archiveMutation.event

        assertEquals(PackageState.ARCHIVED, archivedPkg.state)
        assertTrue(archivedPkg.isArchived)
        assertEquals(instId, archiveEvent.installedPackageId)
        assertEquals(pkgId, archiveEvent.packageId)

        // Attempting to archive an already ARCHIVED package fails
        assertFailsWith<IllegalStateException> {
            archivedPkg.archive()
        }

        // Restore
        val restoreMutation = archivedPkg.restore()
        val restoredPkg = restoreMutation.aggregate
        val restoreEvent = restoreMutation.event

        assertEquals(PackageState.ACTIVE, restoredPkg.state)
        assertTrue(restoredPkg.isActive)
        assertEquals(instId, restoreEvent.installedPackageId)

        // Remove
        val removeMutation = restoredPkg.remove()
        val removedPkg = removeMutation.aggregate
        val removeEvent = removeMutation.event

        assertEquals(PackageState.REMOVED, removedPkg.state)
        assertTrue(removedPkg.isRemoved)
        assertEquals(instId, removeEvent.installedPackageId)
        assertEquals(libId, removeEvent.libraryId)

        // Cannot archive, restore, or remove a REMOVED package
        assertFailsWith<IllegalStateException> { removedPkg.archive() }
        assertFailsWith<IllegalStateException> { removedPkg.restore() }
        assertFailsWith<IllegalStateException> { removedPkg.remove() }
    }
}
