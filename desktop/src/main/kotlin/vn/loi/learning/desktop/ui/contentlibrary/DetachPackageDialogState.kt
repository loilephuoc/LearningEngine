package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Trạng thái dialog xác nhận gỡ Package khỏi Collection.
 */
data class DetachPackageDialogState(
    val visible: Boolean = false,
    val collectionId: String = "",
    val collectionName: String = "",
    val packageId: String = "",
    val packageName: String = ""
)
