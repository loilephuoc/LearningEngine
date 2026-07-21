package vn.loi.learning.application.port

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionId

/**
 * Port dùng để lưu và truy xuất LibraryCollection.
 *
 * Application chỉ phụ thuộc contract này và không biết collection được lưu
 * trong RAM, JSON, SQLite, server hay cloud.
 */
interface LibraryCollectionRepository {

    fun findById(
        collectionId: LibraryCollectionId
    ): LibraryCollection?

    fun findAllByLibraryId(
        libraryId: ContentLibraryId
    ): List<LibraryCollection>

    fun save(
        collection: LibraryCollection
    )

    fun saveAll(
        collections: List<LibraryCollection>
    ) {
        collections.forEach(::save)
    }

    fun deleteById(
        collectionId: LibraryCollectionId
    )

    fun deleteAllById(
        collectionIds: Set<LibraryCollectionId>
    ) {
        collectionIds.forEach(::deleteById)
    }

    fun findAll(): List<LibraryCollection>
}