package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.library.model.ContentLibraryId

class ContentLibraryNotFoundException(
    val libraryId: ContentLibraryId
) : IllegalStateException(
    "Content library not found: $libraryId"
)