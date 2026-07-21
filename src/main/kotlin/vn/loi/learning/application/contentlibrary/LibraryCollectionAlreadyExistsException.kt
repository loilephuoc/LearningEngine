package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.LibraryCollectionId

class LibraryCollectionAlreadyExistsException(
    val collectionId: LibraryCollectionId
) : IllegalStateException(
    "Library collection already exists: $collectionId"
)