package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId

class InMemoryContentLibraryRepository : ContentLibraryRepository {

    private val libraries =
        linkedMapOf<ContentLibraryId, ContentLibrary>()

    override fun findById(
        libraryId: ContentLibraryId
    ): ContentLibrary? =
        libraries[libraryId]

    override fun save(
        library: ContentLibrary
    ) {
        libraries[library.id] = library
    }

    override fun deleteById(
        libraryId: ContentLibraryId
    ) {
        libraries.remove(libraryId)
    }

    override fun findAll(): List<ContentLibrary> =
        libraries.values.toList()

    fun count(): Int =
        libraries.size

    fun clear() {
        libraries.clear()
    }
}
