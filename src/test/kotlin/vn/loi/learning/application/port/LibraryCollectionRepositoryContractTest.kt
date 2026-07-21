package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository

class LibraryCollectionRepositoryContractTest {

    private fun createRepository(): LibraryCollectionRepository =
        InMemoryLibraryCollectionRepository()

    @Test
    fun `findById returns null when collection does not exist`() {
        val repository =
            createRepository()

        assertNull(
            repository.findById(
                LibraryCollectionId(
                    "missing-collection"
                )
            )
        )
    }

    @Test
    fun `saved collection can be found by ID`() {
        val repository =
            createRepository()

        val collection =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )

        repository.save(
            collection
        )

        assertEquals(
            collection,
            repository.findById(
                collection.id
            )
        )
    }

    @Test
    fun `saving same ID replaces previous aggregate`() {
        val repository =
            createRepository()

        val collectionId =
            LibraryCollectionId(
                "collection-1"
            )

        val original =
            createCollection(
                id = collectionId.value,
                libraryId = "library-1",
                name = "Original"
            )

        val updated =
            createCollection(
                id = collectionId.value,
                libraryId = "library-1",
                name = "Updated",
                packageIds = setOf(
                    PackageId(
                        "package-1"
                    )
                )
            )

        repository.save(
            original
        )

        repository.save(
            updated
        )

        assertEquals(
            updated,
            repository.findById(
                collectionId
            )
        )

        assertEquals(
            listOf(updated),
            repository.findAll()
        )
    }

    @Test
    fun `saveAll stores multiple collections`() {
        val repository =
            createRepository()

        val first =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )

        val second =
            createCollection(
                id = "collection-2",
                libraryId = "library-1",
                name = "Physics"
            )

        repository.saveAll(
            listOf(
                first,
                second
            )
        )

        assertEquals(
            listOf(
                first,
                second
            ),
            repository.findAll()
        )
    }

    @Test
    fun `saveAll replaces existing collection and preserves others`() {
        val repository =
            createRepository()

        val original =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "Original"
            )

        val preserved =
            createCollection(
                id = "collection-2",
                libraryId = "library-1",
                name = "Preserved"
            )

        val updated =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "Updated",
                packageIds = setOf(
                    PackageId(
                        "package-updated"
                    )
                )
            )

        repository.saveAll(
            listOf(
                original,
                preserved
            )
        )

        repository.saveAll(
            listOf(
                updated
            )
        )

        assertEquals(
            listOf(
                updated,
                preserved
            ),
            repository.findAll()
        )
    }

    @Test
    fun `findAllByLibraryId returns only collections owned by library`() {
        val repository =
            createRepository()

        val first =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )

        val second =
            createCollection(
                id = "collection-2",
                libraryId = "library-2",
                name = "Physics"
            )

        val third =
            createCollection(
                id = "collection-3",
                libraryId = "library-1",
                name = "Medical"
            )

        repository.saveAll(
            listOf(
                first,
                second,
                third
            )
        )

        assertEquals(
            listOf(
                first,
                third
            ),
            repository.findAllByLibraryId(
                ContentLibraryId(
                    "library-1"
                )
            )
        )
    }

    @Test
    fun `findAllByLibraryId returns empty list when library has no collections`() {
        val repository =
            createRepository()

        repository.save(
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )
        )

        assertEquals(
            emptyList(),
            repository.findAllByLibraryId(
                ContentLibraryId(
                    "library-missing"
                )
            )
        )
    }

    @Test
    fun `deleteById removes collection`() {
        val repository =
            createRepository()

        val collection =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )

        repository.save(
            collection
        )

        repository.deleteById(
            collection.id
        )

        assertNull(
            repository.findById(
                collection.id
            )
        )
    }

    @Test
    fun `deleteAllById removes selected collections and preserves others`() {
        val repository =
            createRepository()

        val first =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )

        val second =
            createCollection(
                id = "collection-2",
                libraryId = "library-1",
                name = "Physics"
            )

        val preserved =
            createCollection(
                id = "collection-3",
                libraryId = "library-1",
                name = "Medical"
            )

        repository.saveAll(
            listOf(
                first,
                second,
                preserved
            )
        )

        repository.deleteAllById(
            setOf(
                first.id,
                second.id
            )
        )

        assertNull(
            repository.findById(
                first.id
            )
        )

        assertNull(
            repository.findById(
                second.id
            )
        )

        assertEquals(
            listOf(
                preserved
            ),
            repository.findAll()
        )
    }

    @Test
    fun `deleteAllById accepts empty set without changing repository`() {
        val repository =
            createRepository()

        val collection =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )

        repository.save(
            collection
        )

        repository.deleteAllById(
            emptySet()
        )

        assertEquals(
            listOf(
                collection
            ),
            repository.findAll()
        )
    }

    @Test
    fun `findAll returns collections in insertion order`() {
        val repository =
            createRepository()

        val first =
            createCollection(
                id = "collection-1",
                libraryId = "library-1",
                name = "English"
            )

        val second =
            createCollection(
                id = "collection-2",
                libraryId = "library-2",
                name = "Physics"
            )

        repository.save(
            first
        )

        repository.save(
            second
        )

        assertEquals(
            listOf(
                first,
                second
            ),
            repository.findAll()
        )
    }

    private fun createCollection(
        id: String,
        libraryId: String,
        name: String,
        packageIds: Set<PackageId> = emptySet()
    ): LibraryCollection =
        LibraryCollection(
            id = LibraryCollectionId(
                id
            ),
            libraryId = ContentLibraryId(
                libraryId
            ),
            descriptor =
                LibraryCollectionDescriptor(
                    name = name
                ),
            packageIds = packageIds
        )
}