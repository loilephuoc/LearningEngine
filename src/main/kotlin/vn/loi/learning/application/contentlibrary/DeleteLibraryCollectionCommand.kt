package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.LibraryCollectionId

/**
 * Command xóa một LibraryCollection.
 */
data class DeleteLibraryCollectionCommand(
    val collectionId: LibraryCollectionId
)