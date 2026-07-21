package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.store.InMemoryLibraryCollectionStore

class StoreBackedLibraryCollectionRepositoryTest {

    private val libraryId =
        ContentLibraryId(
            "library-main"
        )

    private val otherLibraryId =
        ContentLibraryId(
            "library-other"
        )

    @Test
    fun `saved collection can be restored from store`() {
        val store =
            InMemoryLibraryCollectionStore()

        val repository =
            StoreBackedLibraryCollectionRepository(
                store
            )

        val collection =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-english"
                    ),
                libraryId = libraryId,
                name = "English",
                packageIds =
                    setOf(
                        PackageId(
                            "package-vocabulary"
                        )
                    )
            )

        repository.save(
            collection
        )

        val restoredRepository =
            StoreBackedLibraryCollectionRepository(
                store
            )

        assertEquals(
            collection,
            restoredRepository.findById(
                collection.id
            )
        )
    }

    @Test
    fun `saving same collection id replaces existing aggregate`() {
        val repository =
            createRepository()

        val collectionId =
            LibraryCollectionId(
                "collection-english"
            )

        repository.save(
            createCollection(
                collectionId = collectionId,
                libraryId = libraryId,
                name = "Original",
                packageIds = emptySet()
            )
        )

        val updated =
            createCollection(
                collectionId = collectionId,
                libraryId = libraryId,
                name = "Updated",
                packageIds =
                    setOf(
                        PackageId(
                            "package-updated"
                        )
                    )
            )

        repository.save(
            updated
        )

        assertEquals(
            listOf(
                updated
            ),
            repository.findAll()
        )
    }

    @Test
    fun `findAllByLibraryId returns only owned collections`() {
        val repository =
            createRepository()

        val first =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-english"
                    ),
                libraryId = libraryId,
                name = "English",
                packageIds = emptySet()
            )

        val second =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-physics"
                    ),
                libraryId = otherLibraryId,
                name = "Physics",
                packageIds = emptySet()
            )

        val third =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-medical"
                    ),
                libraryId = libraryId,
                name = "Medical",
                packageIds = emptySet()
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
                libraryId
            )
        )
    }

    @Test
    fun `deleteById removes collection and preserves others`() {
        val repository =
            createRepository()

        val deleted =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-deleted"
                    ),
                libraryId = libraryId,
                name = "Deleted",
                packageIds = emptySet()
            )

        val retained =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-retained"
                    ),
                libraryId = libraryId,
                name = "Retained",
                packageIds = emptySet()
            )

        repository.saveAll(
            listOf(
                deleted,
                retained
            )
        )

        repository.deleteById(
            deleted.id
        )

        assertNull(
            repository.findById(
                deleted.id
            )
        )

        assertEquals(
            listOf(
                retained
            ),
            repository.findAll()
        )
    }

    @Test
    fun `deleteAllById removes selected collections`() {
        val repository =
            createRepository()

        val first =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-1"
                    ),
                libraryId = libraryId,
                name = "First",
                packageIds = emptySet()
            )

        val second =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-2"
                    ),
                libraryId = libraryId,
                name = "Second",
                packageIds = emptySet()
            )

        val retained =
            createCollection(
                collectionId =
                    LibraryCollectionId(
                        "collection-3"
                    ),
                libraryId = libraryId,
                name = "Retained",
                packageIds = emptySet()
            )

        repository.saveAll(
            listOf(
                first,
                second,
                retained
            )
        )

        repository.deleteAllById(
            setOf(
                first.id,
                second.id
            )
        )

        assertEquals(
            listOf(
                retained
            ),
            repository.findAll()
        )
    }

    private fun createRepository(): StoreBackedLibraryCollectionRepository =
        StoreBackedLibraryCollectionRepository(
            InMemoryLibraryCollectionStore()
        )

    private fun createCollection(
        collectionId: LibraryCollectionId,
        libraryId: ContentLibraryId,
        name: String,
        packageIds: Set<PackageId>
    ): LibraryCollection =
        LibraryCollection(
            id = collectionId,
            libraryId = libraryId,
            descriptor =
                LibraryCollectionDescriptor(
                    name = name
                ),
            packageIds = packageIds
        )
}