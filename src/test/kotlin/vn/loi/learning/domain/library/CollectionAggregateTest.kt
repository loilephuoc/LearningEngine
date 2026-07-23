package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion

class CollectionAggregateTest {

    private val colId = CollectionId("col-jlpt-n2")
    private val libId = LibraryId("lib-main")
    private val otherLibId = LibraryId("lib-other")

    private val name1 = CollectionName("JLPT N2 Vocabulary")
    private val name2 = CollectionName("JLPT N2 Grammar")

    private val pkgId1 = PackageId("pkg-1")

    private val instId1 = InstalledPackageId("inst-1")
    private val instIdAbsent = InstalledPackageId("inst-absent")

    private fun createSamplePackage(
        id: InstalledPackageId,
        packageId: PackageId,
        targetLibId: LibraryId = libId
    ): InstalledPackage = InstalledPackage.reconstitute(
        id = id,
        libraryId = targetLibId,
        packageId = packageId,
        topicId = TopicId("topic-1"),
        name = PackageName("Sample Package"),
        version = PackageVersion("1.0"),
        state = PackageState.ACTIVE,
        installedAt = java.time.Instant.now(),
        contentCount = 10,
        learningItemCount = 20
    )

    @Test
    fun `Collection create factory returns aggregate and CollectionCreatedEvent`() {
        val mutation = Collection.create(id = colId, libraryId = libId, name = name1, description = "Test Desc")
        val collection = mutation.aggregate
        val event = mutation.event

        assertEquals(colId, collection.id)
        assertEquals(libId, collection.libraryId)
        assertEquals(name1, collection.name)
        assertEquals("Test Desc", collection.description)
        assertTrue(collection.assignedPackageIds.isEmpty())

        assertEquals(colId, event.collectionId)
        assertEquals(libId, event.libraryId)
        assertEquals(name1, event.name)
    }

    @Test
    fun `Collection rename updates name and emits CollectionRenamedEvent`() {
        val collection = Collection.create(id = colId, libraryId = libId, name = name1).aggregate

        val renameMutation = collection.rename(name2)
        val renamed = renameMutation.aggregate
        val event = renameMutation.event

        assertEquals(name2, renamed.name)
        assertEquals(colId, event.collectionId)
        assertEquals(name1, event.oldName)
        assertEquals(name2, event.newName)

        // Renaming to same name fails
        assertFailsWith<IllegalStateException> {
            renamed.rename(name2)
        }
    }

    @Test
    fun `Collection delete emits CollectionDeletedEvent`() {
        val collection = Collection.create(id = colId, libraryId = libId, name = name1).aggregate
        val deleteMutation = collection.delete()

        assertEquals(colId, deleteMutation.event.collectionId)
        assertEquals(libId, deleteMutation.event.libraryId)
    }

    @Test
    fun `assignPackage accepts active package registered in parent Library`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate.registerEntry(instId1, pkgId1)
        val pkg1 = createSamplePackage(instId1, pkgId1)

        val collection = Collection.create(id = colId, libraryId = libId, name = name1).aggregate

        val assignMutation = collection.assignPackage(pkg1, lib)
        val updatedCol = assignMutation.aggregate
        val event = assignMutation.event

        assertTrue(updatedCol.containsPackage(instId1))
        assertEquals(1, updatedCol.assignedPackageIds.size)
        assertEquals(colId, event.collectionId)
        assertEquals(instId1, event.installedPackageId)

        // Assigning duplicate fails
        assertFailsWith<IllegalStateException> {
            updatedCol.assignPackage(pkg1, lib)
        }
    }

    @Test
    fun `assignPackage rejects package absent from parent Library`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val pkgAbsent = createSamplePackage(instIdAbsent, pkgId1)
        val collection = Collection.create(id = colId, libraryId = libId, name = name1).aggregate

        val ex = assertFailsWith<IllegalArgumentException> {
            collection.assignPackage(pkgAbsent, lib)
        }
        assertTrue(ex.message!!.contains("package is not active in Library"))
    }

    @Test
    fun `assignPackage rejects package from a different library`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate
        val pkgOtherLib = createSamplePackage(instId1, pkgId1, targetLibId = otherLibId)
        val collection = Collection.create(id = colId, libraryId = libId, name = name1).aggregate

        val ex = assertFailsWith<IllegalArgumentException> {
            collection.assignPackage(pkgOtherLib, lib)
        }
        assertTrue(ex.message!!.contains("belongs to library"))
    }

    @Test
    fun `removePackage removes package assignment and emits PackageRemovedFromCollectionEvent`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate.registerEntry(instId1, pkgId1)
        val pkg1 = createSamplePackage(instId1, pkgId1)

        val collection = Collection.create(id = colId, libraryId = libId, name = name1).aggregate
            .assignPackage(pkg1, lib).aggregate

        val removeMutation = collection.removePackage(instId1)
        val emptyCol = removeMutation.aggregate
        val event = removeMutation.event

        assertFalse(emptyCol.containsPackage(instId1))
        assertEquals(colId, event.collectionId)
        assertEquals(instId1, event.installedPackageId)

        // Removing non-assigned package fails
        assertFailsWith<IllegalStateException> {
            emptyCol.removePackage(instId1)
        }
    }
}
