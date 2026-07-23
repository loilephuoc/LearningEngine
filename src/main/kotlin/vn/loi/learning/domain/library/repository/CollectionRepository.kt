package vn.loi.learning.domain.library.repository

import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Repository interface quản lý bộ sưu tập (Collection) của người học.
 */
interface CollectionRepository {
    fun findById(id: CollectionId): Collection?
    fun findByName(libraryId: LibraryId, name: CollectionName): Collection?
    fun findAllByLibraryId(libraryId: LibraryId): List<Collection>
    fun findAllByLibraryIdAndState(libraryId: LibraryId, state: CollectionState): List<Collection> =
        findAllByLibraryId(libraryId).filter { it.state == state }
    fun save(collection: Collection)
    fun delete(id: CollectionId)
    fun existsByName(libraryId: LibraryId, name: CollectionName): Boolean
}
