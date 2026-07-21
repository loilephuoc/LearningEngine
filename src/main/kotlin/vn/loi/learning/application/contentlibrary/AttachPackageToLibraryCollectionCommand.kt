package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Command gắn một ContentPackage đã tồn tại vào LibraryCollection.
 */
data class AttachPackageToLibraryCollectionCommand(
    val collectionId: LibraryCollectionId,
    val packageId: PackageId
)