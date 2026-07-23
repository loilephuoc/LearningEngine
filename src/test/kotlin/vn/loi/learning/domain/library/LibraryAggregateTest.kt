package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.event.LibraryCreatedEvent
import vn.loi.learning.domain.library.event.PackageInstalledEvent
import vn.loi.learning.domain.library.event.PackageRemovedEvent
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageVersion

class LibraryAggregateTest {

    private val libId = LibraryId("lib-main")
    private val pkgId1 = PackageId("pkg-kanji-n1")
    private val pkgId2 = PackageId("pkg-vocab-n1")

    private val instId1 = InstalledPackageId("inst-1")
    private val instId2 = InstalledPackageId("inst-2")
    private val instId3 = InstalledPackageId("inst-3")

    private val topicId1 = TopicId("topic-1")
    private val topicId2 = TopicId("topic-2")

    private fun createSamplePackage(
        id: InstalledPackageId,
        packageId: PackageId,
        topicId: TopicId = topicId1
    ): InstalledPackage = InstalledPackage.install(
        id = id,
        libraryId = libId,
        packageId = packageId,
        topicId = topicId,
        name = PackageName("Sample Package"),
        version = PackageVersion("1.0"),
        contentCount = 10,
        learningItemCount = 20
    ).aggregate

    @Test
    fun `Library factory creates aggregate and emits LibraryCreatedEvent`() {
        val mutation = Library.create(id = libId, name = "Main Library")
        val library = mutation.aggregate
        val event = mutation.event

        assertEquals(libId, library.id)
        assertEquals("Main Library", library.name)
        assertTrue(library.entries.isEmpty())
        assertEquals(libId, event.libraryId)
        assertEquals("Main Library", event.name)
    }

    @Test
    fun `Library creation rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            Library.create(id = libId, name = "   ")
        }
    }

    @Test
    fun `registerPackage adds entry and emits PackageInstalledEvent`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val pkg1 = createSamplePackage(instId1, pkgId1)

        val mutation = lib.registerPackage(pkg1)
        val updatedLib = mutation.aggregate
        val event = mutation.event

        assertTrue(updatedLib.hasPackage(instId1))
        assertTrue(updatedLib.hasActivePackage(instId1))
        assertTrue(updatedLib.hasActivePackageForPackageId(pkgId1))
        assertEquals(1, updatedLib.entries.size)

        assertEquals(instId1, event.installedPackageId)
        assertEquals(libId, event.libraryId)
        assertEquals(pkgId1, event.packageId)
    }

    @Test
    fun `Single Active Version Invariant rejects duplicate active PackageId`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val pkg1 = createSamplePackage(instId1, pkgId1)
        val pkg1DuplicateVersion = createSamplePackage(instId2, pkgId1) // Same PackageId, different InstalledPackageId

        val libWithPkg1 = lib.registerPackage(pkg1).aggregate

        // Attempting to register another active package for the same PackageId throws IllegalStateException
        val ex = assertFailsWith<IllegalStateException> {
            libWithPkg1.registerPackage(pkg1DuplicateVersion)
        }
        assertTrue(ex.message!!.contains("already has an ACTIVE InstalledPackage for PackageId"))
    }

    @Test
    fun `unregisterPackage removes entry and emits PackageRemovedEvent`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val pkg1 = createSamplePackage(instId1, pkgId1)
        val libWithPkg1 = lib.registerPackage(pkg1).aggregate

        val unregisterMutation = libWithPkg1.unregisterPackage(instId1)
        val emptyLib = unregisterMutation.aggregate
        val event = unregisterMutation.event

        assertFalse(emptyLib.hasPackage(instId1))
        assertEquals(instId1, event.installedPackageId)
        assertEquals(pkgId1, event.packageId)

        // Unregistering non-existent package fails
        assertFailsWith<IllegalStateException> {
            emptyLib.unregisterPackage(instId1)
        }
    }

    @Test
    fun `removePackageAndCleanCollections unregisters package and cleans collection references`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val pkg1 = createSamplePackage(instId1, pkgId1)
        val pkg2 = createSamplePackage(instId2, pkgId2)

        val libWithPackages = lib.registerPackage(pkg1).aggregate.registerPackage(pkg2).aggregate

        val col1 = Collection.create(CollectionId("col-1"), libId, CollectionName("Collection 1")).aggregate
            .assignPackage(pkg1, libWithPackages).aggregate
            .assignPackage(pkg2, libWithPackages).aggregate

        val col2 = Collection.create(CollectionId("col-2"), libId, CollectionName("Collection 2")).aggregate
            .assignPackage(pkg1, libWithPackages).aggregate

        val removalResult = libWithPackages.removePackageAndCleanCollections(instId1, listOf(col1, col2))

        val updatedLib = removalResult.libraryMutation.aggregate
        val colMutations = removalResult.collectionMutations

        assertFalse(updatedLib.hasPackage(instId1))
        assertTrue(updatedLib.hasPackage(instId2))

        assertEquals(2, colMutations.size)
        val updatedCol1 = colMutations[0].aggregate
        val updatedCol2 = colMutations[1].aggregate

        assertFalse(updatedCol1.containsPackage(instId1))
        assertTrue(updatedCol1.containsPackage(instId2))
        assertFalse(updatedCol2.containsPackage(instId1))
    }

    @Test
    fun `validateCollectionNameUnique rejects case-insensitive duplicate collection names in library`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val col1 = Collection.create(CollectionId("col-1"), libId, CollectionName("JLPT N3")).aggregate

        // Does not throw for unique name
        lib.validateCollectionNameUnique(CollectionName("JLPT N2"), listOf(col1))

        // Throws for duplicate case-insensitive name
        assertFailsWith<IllegalArgumentException> {
            lib.validateCollectionNameUnique(CollectionName("jlpt n3"), listOf(col1))
        }
    }
}
