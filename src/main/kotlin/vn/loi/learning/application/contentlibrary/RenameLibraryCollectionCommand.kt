package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.LibraryCollectionId

/**
 * Command đổi tên một LibraryCollection đã tồn tại.
 */
data class RenameLibraryCollectionCommand(
    val collectionId: LibraryCollectionId,
    val name: String
)