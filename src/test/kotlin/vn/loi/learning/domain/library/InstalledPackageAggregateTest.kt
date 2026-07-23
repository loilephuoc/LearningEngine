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
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.library.service.LibraryDomainCoordinator

class InstalledPackageAggregateTest {

    private val libId = LibraryId("lib-1")
    private val instId = InstalledPackageId("inst-pkg-100")
    private val pkgId = PackageId("pkg-n1")
    private val topicId = TopicId("topic-kanji-n1")

    @Test
    fun `reconstitute factory creates InstalledPackage in specified state`() {
        val pkg = InstalledPackage.reconstitute(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Kanji N1 Master"),
            version = PackageVersion("1.0"),
            state = PackageState.ACTIVE,
            installedAt = java.time.Instant.now(),
            contentCount = 50,
            learningItemCount = 100
        )

        assertEquals(instId, pkg.id)
        assertEquals(libId, pkg.libraryId)
        assertEquals(pkgId, pkg.packageId)
        assertEquals(topicId, pkg.topicId)
        assertEquals(PackageState.ACTIVE, pkg.state)
        assertTrue(pkg.isActive)
        assertEquals(50, pkg.contentCount)
        assertEquals(100, pkg.learningItemCount)
    }

    @Test
    fun `installed package validates non-negative counts`() {
        assertFailsWith<IllegalArgumentException> {
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = libId,
                packageId = pkgId,
                topicId = topicId,
                name = PackageName("Kanji N1 Master"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = java.time.Instant.now(),
                contentCount = -1,
                learningItemCount = 100
            )
        }
    }

    @Test
    fun `archive is aggregate local while restore and remove require LibraryDomainCoordinator`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val installResult = LibraryDomainCoordinator.installPackage(
            library = lib,
            id = instId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Kanji N1 Master"),
            version = PackageVersion("1.0"),
            contentCount = 50,
            learningItemCount = 100,
            installedPackagesInLibrary = emptyList()
        )
        val samplePkg = installResult.installedPackage

        // 1. Archive (aggregate local)
        val archiveMutation = samplePkg.archive()
        val archivedPkg = archiveMutation.aggregate
        val archiveEvent = archiveMutation.event

        assertEquals(PackageState.ARCHIVED, archivedPkg.state)
        assertTrue(archivedPkg.isArchived)
        assertEquals(instId, archiveEvent.installedPackageId)

        // 2. Restore through Coordinator
        val restoreResult = LibraryDomainCoordinator.restorePackage(
            library = installResult.library,
            installedPackage = archivedPkg,
            installedPackagesInLibrary = listOf(archivedPkg)
        )
        val restoredPkg = restoreResult.installedPackage
        assertTrue(restoredPkg.isActive)

        // 3. Remove through Coordinator
        val removalResult = LibraryDomainCoordinator.removePackage(
            library = installResult.library,
            installedPackage = restoredPkg,
            collections = emptyList()
        )
        val removedPkg = removalResult.installedPackage
        assertTrue(removedPkg.isRemoved)
    }
}
