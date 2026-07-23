package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId

class CollectionAggregateTest {

    private val colId = CollectionId("col-jlpt-n2")
    private val libId = LibraryId("lib-main")
    private val name1 = CollectionName("JLPT N2 Vocabulary")
    private val name2 = CollectionName("JLPT N2 Grammar")

    private val pkgId1 = InstalledPackageId("inst-1")
    private val pkgId2 = InstalledPackageId("inst-2")

    @Test
    fun `collection initializes cleanly and supports rename`() {
        val collection = Collection(id = colId, libraryId = libId, name = name1)
        assertEquals(colId, collection.id)
        assertEquals(libId, collection.libraryId)
        assertEquals(name1, collection.name)
        assertTrue(collection.assignedPackageIds.isEmpty())

        val renamed = collection.rename(name2)
        assertEquals(name2, renamed.name)
    }

    @Test
    fun `collection manages assigned package IDs preventing duplicates`() {
        val collection = Collection(id = colId, libraryId = libId, name = name1)

        val withPkg1 = collection.assignPackage(pkgId1)
        assertTrue(withPkg1.containsPackage(pkgId1))
        assertEquals(1, withPkg1.assignedPackageIds.size)

        // Assigning duplicate is idempotent
        val duplicateAssign = withPkg1.assignPackage(pkgId1)
        assertEquals(withPkg1, duplicateAssign)

        val withPkg2 = withPkg1.assignPackage(pkgId2)
        assertEquals(2, withPkg2.assignedPackageIds.size)

        val afterRemoval = withPkg2.removePackage(pkgId1)
        assertFalse(afterRemoval.containsPackage(pkgId1))
        assertTrue(afterRemoval.containsPackage(pkgId2))
        assertEquals(1, afterRemoval.assignedPackageIds.size)
    }
}
