package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionId

class InMemoryLibraryCollectionRepository : LibraryCollectionRepository {

    private val collections =
        linkedMapOf<LibraryCollectionId, LibraryCollection>()

    override fun findById(
        collectionId: LibraryCollectionId
    ): LibraryCollection? =
        collections[collectionId]

    override fun findAllByLibraryId(
        libraryId: ContentLibraryId
    ): List<LibraryCollection> =
        collections.values.filter { collection ->
            collection.libraryId == libraryId
        }

    override fun save(
        collection: LibraryCollection
    ) {
        collections[collection.id] =
            collection
    }

    override fun deleteById(
        collectionId: LibraryCollectionId
    ) {
        collections.remove(
            collectionId
        )
    }

    override fun findAll(): List<LibraryCollection> =
        collections.values.toList()

    fun count(): Int =
        collections.size

    fun clear() {
        collections.clear()
    }
}