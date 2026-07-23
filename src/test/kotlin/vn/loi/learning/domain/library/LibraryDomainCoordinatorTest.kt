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
    fun `1 Archiving an installed package permits installing a newer active version of the same PackageId`() {
        val lib = createInitialLibrary()

        // 1. Install version 1.0
        val installV1Result = LibraryDomainCoordinator.installPackage(
            library = lib,
            id = instId1,
            packageId = pkgId1,
            topicId = topicId1,
            name = PackageName("Kanji N1"),
            version = PackageVersion("1.0"),
            contentCount = 10,
            learningItemCount = 20,
            installedPackagesInLibrary = emptyList()
        )

        val libWithV1 = installV1Result.library
        val pkgV1 = installV1Result.installedPackage

        // 2. Archive version 1.0
        val pkgV1Archived = pkgV1.archive().aggregate
        assertEquals(PackageState.ARCHIVED, pkgV1Archived.state)

        // 3. Install version 2.0 with the SAME PackageId
        val installV2Result = LibraryDomainCoordinator.installPackage(
            library = libWithV1,
            id = instId2,
            packageId = pkgId1,
            topicId = topicId1,
            name = PackageName("Kanji N1"),
            version = PackageVersion("2.0"),
            contentCount = 15,
            learningItemCount = 30,
            installedPackagesInLibrary = listOf(pkgV1Archived)
        )

        val libWithV2 = installV2Result.library
        val pkgV2 = installV2Result.installedPackage

        assertTrue(libWithV2.hasPackage(instId1))
        assertTrue(libWithV2.hasPackage(instId2))
        assertTrue(pkgV2.isActive)
    }

    @Test
    fun `2 Restoring an archived package is rejected when another active version of the same PackageId exists`() {
        val lib = createInitialLibrary()

        // Install V1.0 & Archive V1.0
        val installV1Result = LibraryDomainCoordinator.installPackage(
            library = lib,
            id = instId1,
            packageId = pkgId1,
            topicId = topicId1,
            name = PackageName("Kanji N1"),
            version = PackageVersion("1.0"),
            contentCount = 10,
            learningItemCount = 20,
            installedPackagesInLibrary = emptyList()
        )
        val pkgV1Archived = installV1Result.installedPackage.archive().aggregate

        // Install V2.0 (ACTIVE)
        val installV2Result = LibraryDomainCoordinator.installPackage(
            library = installV1Result.library,
            id = instId2,
            packageId = pkgId1,
            topicId = topicId1,
            name = PackageName("Kanji N1"),
            version = PackageVersion("2.0"),
            contentCount = 15,
            learningItemCount = 30,
            installedPackagesInLibrary = listOf(pkgV1Archived)
        )
        val libWithBoth = installV2Result.library
        val pkgV2Active = installV2Result.installedPackage

        // Attempting to restore V1.0 while V2.0 is ACTIVE throws IllegalStateException
        val ex = assertFailsWith<IllegalStateException> {
            LibraryDomainCoordinator.restorePackage(
                library = libWithBoth,
                installedPackage = pkgV1Archived,
                installedPackagesInLibrary = listOf(pkgV1Archived, pkgV2Active)
            )
        }
        assertTrue(ex.message!!.contains("already has an ACTIVE InstalledPackage for PackageId"))
    }

    @Test
    fun `3 Removing a package cannot leave stale active state in Library`() {
        val lib = createInitialLibrary()
        val installResult = LibraryDomainCoordinator.installPackage(
            library = lib,
            id = instId1,
            packageId = pkgId1,
            topicId = topicId1,
            name = PackageName("Kanji N1"),
            version = PackageVersion("1.0"),
            contentCount = 10,
            learningItemCount = 20,
            installedPackagesInLibrary = emptyList()
        )

        val removalResult = LibraryDomainCoordinator.removePackage(
            library = installResult.library,
            installedPackage = installResult.installedPackage,
            collections = emptyList()
        )

        val updatedLib = removalResult.library
        val removedPkg = removalResult.installedPackage

        assertFalse(updatedLib.hasPackage(instId1))
        assertFalse(updatedLib.hasActivePackage(instId1, listOf(removedPkg)))
        assertTrue(removedPkg.isRemoved)
    }

    @Test
    fun `4 A full installation flow emits exactly one PackageInstalledEvent`() {
        val lib = createInitialLibrary()
        val installResult = LibraryDomainCoordinator.installPackage(
            library = lib,
            id = instId1,
            packageId = pkgId1,
            topicId = topicId1,
            name = PackageName("Kanji N1"),
            version = PackageVersion("1.0"),
            contentCount = 10,
            learningItemCount = 20,
            installedPackagesInLibrary = emptyList()
        )

        val event: PackageInstalledEvent = installResult.event
        assertEquals(instId1, event.installedPackageId)
        assertEquals(libId, event.libraryId)
        assertEquals(pkgId1, event.packageId)
    }

    @Test
    fun `5 Collection creation rejects a duplicate name case-insensitively through the canonical public API`() {
        val lib = createInitialLibrary()
        val col1Mutation = LibraryDomainCoordinator.createCollection(
            library = lib,
            id = colId1,
            name = CollectionName("JLPT N3 Vocabulary"),
            existingCollections = emptyList()
        )

        assertFailsWith<IllegalArgumentException> {
            LibraryDomainCoordinator.createCollection(
                library = lib,
                id = colId2,
                name = CollectionName("jlpt n3 vocabulary"), // Case-insensitive duplicate
                existingCollections = listOf(col1Mutation.aggregate)
            )
        }
    }

    @Test
    fun `6 Collection rename rejects collision case-insensitively through the canonical public API`() {
        val lib = createInitialLibrary()
        val col1 = LibraryDomainCoordinator.createCollection(lib, colId1, CollectionName("JLPT N3"), existingCollections = emptyList()).aggregate
        val col2 = LibraryDomainCoordinator.createCollection(lib, colId2, CollectionName("JLPT N2"), existingCollections = listOf(col1)).aggregate

        assertFailsWith<IllegalArgumentException> {
            LibraryDomainCoordinator.renameCollection(
                library = lib,
                collection = col2,
                newName = CollectionName("jlpt n3"), // Collision with col1
                existingCollections = listOf(col1, col2)
            )
        }
    }

    @Test
    fun `7 Deleted Collection cannot be renamed`() {
        val lib = createInitialLibrary()
        val col = LibraryDomainCoordinator.createCollection(lib, colId1, CollectionName("JLPT N3"), existingCollections = emptyList()).aggregate
        val deletedCol = col.delete().aggregate

        assertEquals(CollectionState.DELETED, deletedCol.state)
        assertTrue(deletedCol.isDeleted)

        assertFailsWith<IllegalStateException> {
            deletedCol.rename(CollectionName("JLPT N2"))
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
    fun `9 Repeated deletion is rejected`() {
        val lib = createInitialLibrary()
        val col = LibraryDomainCoordinator.createCollection(lib, colId1, CollectionName("JLPT N3"), emptyList()).aggregate
        val deletedCol = col.delete().aggregate

        assertFailsWith<IllegalStateException> {
            deletedCol.delete()
        }
    }

    @Test
    fun `10 Package removal coordination cleans all affected Collection references without placing Collection ownership inside Library`() {
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

        // Events list contains PackageRemovedEvent and 2 PackageRemovedFromCollectionEvent items
        assertEquals(3, removalResult.events.size)
        assertTrue(removalResult.events[2] is PackageRemovedFromCollectionEvent)
    }

    @Test
    fun `11 Controlled reconstitution factories restore valid state`() {
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
