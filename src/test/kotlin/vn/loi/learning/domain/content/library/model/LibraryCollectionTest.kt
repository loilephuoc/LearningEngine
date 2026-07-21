package vn.loi.learning.domain.content.library.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId

class LibraryCollectionTest {

    private val collectionId =
        LibraryCollectionId(
            "collection-english"
        )

    private val libraryId =
        ContentLibraryId(
            "library-main"
        )

    private val descriptor =
        LibraryCollectionDescriptor(
            name = "English"
        )

    @Test
    fun `collection ID rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            LibraryCollectionId(" ")
        }
    }

    @Test
    fun `collection descriptor rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            LibraryCollectionDescriptor(
                name = " "
            )
        }
    }

    @Test
    fun `new collection is empty`() {
        val collection =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor
            )

        assertEquals(
            collectionId,
            collection.id
        )

        assertEquals(
            libraryId,
            collection.libraryId
        )

        assertEquals(
            "English",
            collection.name
        )

        assertEquals(
            0,
            collection.packageCount
        )

        assertTrue(
            collection.isEmpty
        )
    }

    @Test
    fun `rename returns updated collection immutably`() {
        val original =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor
            )

        val updated =
            original.rename(
                name = "English Language"
            )

        assertEquals(
            "English",
            original.name
        )

        assertEquals(
            "English Language",
            updated.name
        )

        assertEquals(
            original.id,
            updated.id
        )

        assertEquals(
            original.libraryId,
            updated.libraryId
        )

        assertEquals(
            original.packageIds,
            updated.packageIds
        )
    }

    @Test
    fun `rename with same name returns same instance`() {
        val collection =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor
            )

        assertSame(
            collection,
            collection.rename(
                name = "English"
            )
        )
    }

    @Test
    fun `rename rejects blank name`() {
        val collection =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor
            )

        assertFailsWith<IllegalArgumentException> {
            collection.rename(
                name = " "
            )
        }
    }

    @Test
    fun `attach adds package reference immutably`() {
        val packageId =
            PackageId(
                "package-oxford-3000"
            )

        val original =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor
            )

        val updated =
            original.attach(
                packageId
            )

        assertTrue(
            original.isEmpty
        )

        assertFalse(
            updated.isEmpty
        )

        assertTrue(
            updated.contains(
                packageId
            )
        )

        assertEquals(
            1,
            updated.packageCount
        )
    }

    @Test
    fun `attach existing package returns same instance`() {
        val packageId =
            PackageId(
                "package-oxford-3000"
            )

        val collection =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor,
                packageIds = setOf(packageId)
            )

        assertSame(
            collection,
            collection.attach(
                packageId
            )
        )
    }

    @Test
    fun `attachAll adds only missing package references`() {
        val first =
            PackageId(
                "package-oxford-3000"
            )

        val second =
            PackageId(
                "package-ielts"
            )

        val collection =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor,
                packageIds = setOf(first)
            )

        val updated =
            collection.attachAll(
                setOf(first, second)
            )

        assertEquals(
            setOf(first, second),
            updated.packageIds
        )

        assertEquals(
            2,
            updated.packageCount
        )
    }

    @Test
    fun `attachAll with empty set returns same instance`() {
        val collection =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor
            )

        assertSame(
            collection,
            collection.attachAll(
                emptySet()
            )
        )
    }

    @Test
    fun `detach removes package reference immutably`() {
        val packageId =
            PackageId(
                "package-oxford-3000"
            )

        val original =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor,
                packageIds = setOf(packageId)
            )

        val updated =
            original.detach(
                packageId
            )

        assertTrue(
            original.contains(
                packageId
            )
        )

        assertFalse(
            updated.contains(
                packageId
            )
        )

        assertTrue(
            updated.isEmpty
        )
    }

    @Test
    fun `detach missing package returns same instance`() {
        val collection =
            LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor = descriptor
            )

        assertSame(
            collection,
            collection.detach(
                PackageId(
                    "missing-package"
                )
            )
        )
    }
}