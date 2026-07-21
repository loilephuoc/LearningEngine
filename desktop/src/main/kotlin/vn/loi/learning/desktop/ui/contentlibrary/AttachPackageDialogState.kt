package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Trạng thái dialog gắn Package vào Collection.
 */
data class AttachPackageDialogState(
    val visible: Boolean = false,
    val collectionId: String = "",
    val collectionName: String = "",
    val selectedPackageId: String = "",
    val availablePackages: List<ContentLibraryPackageItem> = emptyList()
)