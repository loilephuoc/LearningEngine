package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollectionId

/**
 * Command tạo một collection mới trong Content Library.
 */
data class CreateLibraryCollectionCommand(
    val collectionId: LibraryCollectionId,
    val libraryId: ContentLibraryId,
    val name: String
)