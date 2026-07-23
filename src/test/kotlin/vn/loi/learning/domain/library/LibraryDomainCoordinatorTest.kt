package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.event.PackageInstalledEvent
import vn.loi.learning.domain.library.event.PackageRemovedFromCollectionEvent
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.library.service.LibraryDomainCoordinator

class LibraryDomainCoordinatorTest {

    private val libId = LibraryId("lib-main")
    private val pkgId1 = PackageId("pkg-kanji-n1")
    private val topicId1 = TopicId("topic-kanji-1")

    private val instId1 = InstalledPackageId("inst-v1.0")
    private val instId2 = InstalledPackageId("inst-v2.0")

    private val colId1 = CollectionId("col-1")
    private val colId2 = CollectionId("col-2")

    private fun createInitialLibrary(): Library =
        Library.create(id = libId, name = "Main Library").aggregate

    @Test
    fun `1 Canonical restore through LibraryDomainCoordinator rejects conflict with another active version`() {
        val lib = createInitialLibrary()

        // 1. Install version 1.0 & Archive
        val installV1 = LibraryDomainCoordinator.installPackage(lib, instId1, pkgId1, topicId1, PackageName("N1"), PackageVersion("1.0"), 10, 20, emptyList())
        val pkgV1Archived = installV1.installedPackage.archive().aggregate

        // 2. Install version 2.0 (ACTIVE)
        val installV2 = LibraryDomainCoordinator.installPackage(installV1.library, instId2, pkgId1, topicId1, PackageName("N1"), PackageVersion("2.0"), 15, 30, listOf(pkgV1Archived))
        val libWithBoth = installV2.library
        val pkgV2Active = installV2.installedPackage

        // 3. Restoring V1.0 while V2.0 is ACTIVE is rejected by LibraryDomainCoordinator
        val ex = assertFailsWith<IllegalStateException> {
            LibraryDomainCoordinator.restorePackage(libWithBoth, pkgV1Archived, listOf(pkgV1Archived, pkgV2Active))
        }
        assertTrue(ex.message!!.contains("already has an ACTIVE InstalledPackage for PackageId"))
    }

    @Test
    fun `2 Archive remains functional and produces PackageArchivedEvent`() {
        val lib = createInitialLibrary()
        val installResult = LibraryDomainCoordinator.installPackage(lib, instId1, pkgId1, topicId1, PackageName("N1"), PackageVersion("1.0"), 10, 20, emptyList())
        val pkg = installResult.installedPackage

        val archiveMutation = pkg.archive()
        val archivedPkg = archiveMutation.aggregate
        val event = archiveMutation.event

        assertEquals(PackageState.ARCHIVED, archivedPkg.state)
        assertEquals(instId1, event.installedPackageId)
        assertEquals(pkgId1, event.packageId)
    }

    @Test
    fun `3 Archiving permits installing newer active version and then restoring after newer version is archived`() {
        val lib = createInitialLibrary()
        val installV1 = LibraryDomainCoordinator.installPackage(lib, instId1, pkgId1, topicId1, PackageName("N1"), PackageVersion("1.0"), 10, 20, emptyList())
        val pkgV1Archived = installV1.installedPackage.archive().aggregate

        // Install V2.0 while V1.0 is ARCHIVED
        val installV2 = LibraryDomainCoordinator.installPackage(installV1.library, instId2, pkgId1, topicId1, PackageName("N1"), PackageVersion("2.0"), 15, 30, listOf(pkgV1Archived))

        // Archive V2.0 as well
        val pkgV2Archived = installV2.installedPackage.archive().aggregate

        // Now restoring V1.0 succeeds because no other version is ACTIVE
        val restoreResult = LibraryDomainCoordinator.restorePackage(installV2.library, pkgV1Archived, listOf(pkgV1Archived, pkgV2Archived))
        assertTrue(restoreResult.installedPackage.isActive)
    }

    @Test
    fun `4 Install produces exactly one PackageInstalledEvent`() {
        val lib = createInitialLibrary()
        val installResult = LibraryDomainCoordinator.installPackage(lib, instId1, pkgId1, topicId1, PackageName("N1"), PackageVersion("1.0"), 10, 20, emptyList())

        val event: PackageInstalledEvent = installResult.event
        assertEquals(instId1, event.installedPackageId)
        assertEquals(libId, event.libraryId)
        assertEquals(pkgId1, event.packageId)
    }

    @Test
    fun `5 Collection creation rejects a duplicate name case-insensitively through LibraryDomainCoordinator`() {
        val lib = createInitialLibrary()
        val col1 = LibraryDomainCoordinator.createCollection(lib, colId1, CollectionName("JLPT N3 Vocabulary"), emptyList()).aggregate

        assertFailsWith<IllegalArgumentException> {
            LibraryDomainCoordinator.createCollection(lib, colId2, CollectionName("jlpt n3 vocabulary"), listOf(col1))
        }
    }

    @Test
    fun `6 Collection rename rejects collision case-insensitively through LibraryDomainCoordinator`() {
        val lib = createInitialLibrary()
        val col1 = LibraryDomainCoordinator.createCollection(lib, colId1, CollectionName("JLPT N3"), emptyList()).aggregate
        val col2 = LibraryDomainCoordinator.createCollection(lib, colId2, CollectionName("JLPT N2"), listOf(col1)).aggregate

        assertFailsWith<IllegalArgumentException> {
            LibraryDomainCoordinator.renameCollection(lib, col2, CollectionName("jlpt n3"), listOf(col1, col2))
        }
    }

    @Test
    fun `7 Deleted Collection cannot be renamed`() {
        val lib = createInitialLibrary()
        val col = LibraryDomainCoordinator.createCollection(lib, colId1, CollectionName("JLPT N3"), emptyList()).aggregate
        val deletedCol = col.delete().aggregate

        assertEquals(CollectionState.DELETED, deletedCol.state)
        assertTrue(deletedCol.isDeleted)

        assertFailsWith<IllegalStateException> {
            LibraryDomainCoordinator.renameCollection(lib, deletedCol, CollectionName("JLPT N2"), listOf(deletedCol))
        }
    }

    @Test
    fun `8 Deleted Collection cannot accept or remove assignments`() {
        val lib = createInitialLibrary()
        val installResult = LibraryDomainCoordinator.installPackage(lib, instId1, pkgId1, topicId1, PackageName("N1"), PackageVersion("1.0"), 10, 20, emptyList())
        val col = LibraryDomainCoordinator.createCollection(installResult.library, colId1, CollectionName("JLPT N3"), emptyList()).aggregate
        val deletedCol = col.delete().aggregate

        assertFailsWith<IllegalStateException> {
            deletedCol.assignPackage(installResult.installedPackage, installResult.library)
        }

        assertFailsWith<IllegalStateException> {
            deletedCol.removePackage(instId1)
        }
    }

    @Test
    fun `9 Package removal coordination cleans all affected Collection references without placing Collection ownership inside Library`() {
        val lib = createInitialLibrary()
        val installResult = LibraryDomainCoordinator.installPackage(lib, instId1, pkgId1, topicId1, PackageName("N1"), PackageVersion("1.0"), 10, 20, emptyList())
        val libWithPkg = installResult.library
        val pkg = installResult.installedPackage

        val col1 = LibraryDomainCoordinator.createCollection(libWithPkg, colId1, CollectionName("Col 1"), emptyList()).aggregate
            .assignPackage(pkg, libWithPkg).aggregate
        val col2 = LibraryDomainCoordinator.createCollection(libWithPkg, colId2, CollectionName("Col 2"), listOf(col1)).aggregate
            .assignPackage(pkg, libWithPkg).aggregate

        val removalResult = LibraryDomainCoordinator.removePackage(libWithPkg, pkg, listOf(col1, col2))

        assertFalse(removalResult.library.hasPackage(instId1))
        assertTrue(removalResult.installedPackage.isRemoved)
        assertEquals(2, removalResult.updatedCollections.size)
        assertFalse(removalResult.updatedCollections[0].containsPackage(instId1))
        assertFalse(removalResult.updatedCollections[1].containsPackage(instId1))

        assertEquals(3, removalResult.events.size)
        assertTrue(removalResult.events[2] is PackageRemovedFromCollectionEvent)
    }

    @Test
    fun `10 Controlled reconstitution factories restore valid state`() {
        val pkg = InstalledPackage.reconstitute(
            id = instId1,
            libraryId = libId,
            packageId = pkgId1,
            topicId = topicId1,
            name = PackageName("Kanji"),
            version = PackageVersion("1.0"),
            state = PackageState.ACTIVE,
            installedAt = java.time.Instant.now(),
            contentCount = 5,
            learningItemCount = 10
        )
        assertEquals(instId1, pkg.id)
        assertTrue(pkg.isActive)
    }
}
