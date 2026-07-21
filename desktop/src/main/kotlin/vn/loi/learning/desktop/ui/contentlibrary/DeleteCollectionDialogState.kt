package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Trạng thái dialog xác nhận xóa Collection.
 */
data class DeleteCollectionDialogState(
    val visible: Boolean = false,
    val collectionId: String = "",
    val collectionName: String = ""
)