package vn.loi.learning.desktop.ui.contentlibrary

data class CreateCollectionDialogState(
    val visible: Boolean = false,
    val libraryId: String = "",
    val libraryName: String = "",
    val collectionName: String = ""
)