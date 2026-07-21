package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Trạng thái của dialog đổi tên LibraryCollection.
 */
data class RenameCollectionDialogState(
    val visible: Boolean = false,
    val collectionId: String = "",
    val currentName: String = "",
    val collectionName: String = ""
)