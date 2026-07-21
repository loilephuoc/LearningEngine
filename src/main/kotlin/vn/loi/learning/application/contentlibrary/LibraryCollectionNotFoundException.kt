package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.LibraryCollectionId

class LibraryCollectionNotFoundException(
    val collectionId: LibraryCollectionId
) : IllegalStateException(
    "Library collection not found: $collectionId"
)