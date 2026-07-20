package vn.loi.learning.domain.content.packaging.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibraryId

class ContentPackageTest {

    private val packageId =
        PackageId(
            "package-1"
        )

    private val descriptor =
        PackageDescriptor(
            name = "Vocabulary Package",
            version = "1.0.0",
            format = "OPD3"
        )

    @Test
    fun `package ID rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            PackageId(" ")
        }
    }

    @Test
    fun `descriptor rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            PackageDescriptor(
                name = " ",
                version = "1.0.0",
                format = "OPD3"
            )
        }
    }

    @Test
    fun `descriptor rejects blank version`() {
        assertFailsWith<IllegalArgumentException> {
            PackageDescriptor(
                name = "Vocabulary Package",
                version = " ",
                format = "OPD3"
            )
        }
    }

    @Test
    fun `descriptor rejects blank format`() {
        assertFailsWith<IllegalArgumentException> {
            PackageDescriptor(
                name = "Vocabulary Package",
                version = "1.0.0",
                format = " "
            )
        }
    }

    @Test
    fun `new package is empty`() {
        val contentPackage =
            ContentPackage(
                id = packageId,
                descriptor = descriptor
            )

        assertEquals(
            "Vocabulary Package",
            contentPackage.name
        )

        assertEquals(
            "1.0.0",
            contentPackage.version
        )

        assertEquals(
            "OPD3",
            contentPackage.format
        )

        assertEquals(
            0,
            contentPackage.libraryCount
        )

        assertTrue(
            contentPackage.isEmpty
        )
    }

    @Test
    fun `register adds library immutably`() {
        val libraryId =
            ContentLibraryId(
                "library-1"
            )

        val original =
            ContentPackage(
                id = packageId,
                descriptor = descriptor
            )

        val updated =
            original.register(
                libraryId
            )

        assertTrue(
            original.isEmpty
        )

        assertFalse(
            updated.isEmpty
        )

        assertTrue(
            updated.contains(
                libraryId
            )
        )

        assertEquals(
            1,
            updated.libraryCount
        )
    }

    @Test
    fun `register existing library returns same instance`() {
        val libraryId =
            ContentLibraryId(
                "library-1"
            )

        val contentPackage =
            ContentPackage(
                id = packageId,
                descriptor = descriptor,
                libraryIds = setOf(libraryId)
            )

        assertSame(
            contentPackage,
            contentPackage.register(libraryId)
        )
    }

    @Test
    fun `registerAll adds only missing libraries`() {
        val first =
            ContentLibraryId(
                "library-1"
            )

        val second =
            ContentLibraryId(
                "library-2"
            )

        val contentPackage =
            ContentPackage(
                id = packageId,
                descriptor = descriptor,
                libraryIds = setOf(first)
            )

        val updated =
            contentPackage.registerAll(
                setOf(first, second)
            )

        assertEquals(
            setOf(first, second),
            updated.libraryIds
        )

        assertEquals(
            2,
            updated.libraryCount
        )
    }

    @Test
    fun `remove deletes library immutably`() {
        val libraryId =
            ContentLibraryId(
                "library-1"
            )

        val original =
            ContentPackage(
                id = packageId,
                descriptor = descriptor,
                libraryIds = setOf(libraryId)
            )

        val updated =
            original.remove(
                libraryId
            )

        assertTrue(
            original.contains(libraryId)
        )

        assertFalse(
            updated.contains(libraryId)
        )

        assertTrue(
            updated.isEmpty
        )
    }

    @Test
    fun `remove missing library returns same instance`() {
        val contentPackage =
            ContentPackage(
                id = packageId,
                descriptor = descriptor
            )

        assertSame(
            contentPackage,
            contentPackage.remove(
                ContentLibraryId(
                    "missing-library"
                )
            )
        )
    }
}
