package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Command gỡ tham chiếu PackageId khỏi LibraryCollection.
 */
data class DetachPackageFromLibraryCollectionCommand(
    val collectionId: LibraryCollectionId,
    val packageId: PackageId
)