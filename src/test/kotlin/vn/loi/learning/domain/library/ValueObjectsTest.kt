package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageVersion

class ValueObjectsTest {

    @Test
    fun `LibraryId accepts valid string and rejects blank`() {
        val libId = LibraryId("lib-1")
        assertEquals("lib-1", libId.value)
        assertEquals("lib-1", libId.toString())

        assertFailsWith<IllegalArgumentException> { LibraryId("") }
        assertFailsWith<IllegalArgumentException> { LibraryId("   ") }
    }

    @Test
    fun `InstalledPackageId accepts valid string and rejects blank`() {
        val instId = InstalledPackageId("inst-123")
        assertEquals("inst-123", instId.value)

        assertFailsWith<IllegalArgumentException> { InstalledPackageId("") }
    }

    @Test
    fun `CollectionId accepts valid string and rejects blank`() {
        val colId = CollectionId("col-99")
        assertEquals("col-99", colId.value)

        assertFailsWith<IllegalArgumentException> { CollectionId("") }
    }

    @Test
    fun `PackageVersion accepts valid string and rejects blank`() {
        val ver = PackageVersion("1.0.0")
        assertEquals("1.0.0", ver.value)

        assertFailsWith<IllegalArgumentException> { PackageVersion("") }
    }

    @Test
    fun `CollectionName trims whitespace and enforces length limits`() {
        val name1 = CollectionName("  JLPT N3  ")
        assertEquals("JLPT N3", name1.trimmedValue)
        assertEquals("JLPT N3", name1.toString())

        val name2 = CollectionName("jlpt n3")
        assertEquals(name1, name2)
        assertEquals(name1.hashCode(), name2.hashCode())

        assertFailsWith<IllegalArgumentException> { CollectionName("   ") }
        assertFailsWith<IllegalArgumentException> { CollectionName("a".repeat(101)) }
    }

    @Test
    fun `PackageName accepts valid title and rejects blank or over 200 chars`() {
        val pkgName = PackageName("Basic Japanese Kanji")
        assertEquals("Basic Japanese Kanji", pkgName.value)

        assertFailsWith<IllegalArgumentException> { PackageName("") }
        assertFailsWith<IllegalArgumentException> { PackageName("a".repeat(201)) }
    }
}
