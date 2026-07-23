package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryEntry
import vn.loi.learning.domain.library.model.LibraryId

class LibraryAggregateTest {

    private val libId = LibraryId("lib-main")
    private val pkgId1 = InstalledPackageId("inst-1")
    private val pkgId2 = InstalledPackageId("inst-2")

    @Test
    fun `library initializes cleanly and rejects blank name`() {
        val library = Library(id = libId, name = "Main Library")
        assertEquals(libId, library.id)
        assertEquals("Main Library", library.name)
        assertTrue(library.entries.isEmpty())

        assertFailsWith<IllegalArgumentException> {
            Library(id = libId, name = "   ")
        }
    }

    @Test
    fun `library rejects duplicate package entry initializations`() {
        val duplicateEntries = listOf(
            LibraryEntry(pkgId1),
            LibraryEntry(pkgId1)
        )

        assertFailsWith<IllegalArgumentException> {
            Library(id = libId, name = "Main Library", entries = duplicateEntries)
        }
    }

    @Test
    fun `library registers package and unregisters successfully`() {
        val lib = Library(id = libId, name = "Main Library")
        assertFalse(lib.hasPackage(pkgId1))

        val libWithPkg1 = lib.registerPackage(pkgId1)
        assertTrue(libWithPkg1.hasPackage(pkgId1))
        assertEquals(1, libWithPkg1.entries.size)

        val libWithPkg2 = libWithPkg1.registerPackage(pkgId2)
        assertEquals(2, libWithPkg2.entries.size)

        // Attempting duplicate registration fails
        assertFailsWith<IllegalArgumentException> {
            libWithPkg2.registerPackage(pkgId1)
        }

        val libAfterUnregister = libWithPkg2.unregisterPackage(pkgId1)
        assertFalse(libAfterUnregister.hasPackage(pkgId1))
        assertTrue(libAfterUnregister.hasPackage(pkgId2))
        assertEquals(1, libAfterUnregister.entries.size)
    }
}
