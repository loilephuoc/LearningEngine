package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserProjectionPolicy
import vn.loi.learning.domain.library.model.InstalledPackageId

data class PackageContentBrowserUiState(
    val installedPackageId: InstalledPackageId,
    val packageName: String,
    val allItems: List<PackageContentBrowserItem>,
    val query: String = "",
    val appliedQuery: String = "",
    val selectedLessonFilter: String = "ALL",
    val availableLessons: List<String> = emptyList(),
    val mediaFilter: BrowserMediaFilter = BrowserMediaFilter.ALL,
    val sortOption: BrowserSortOption = BrowserSortOption.ORIGINAL_ORDER,
    val selectedContentId: String? = null,
    val activePlayingAudioRef: String? = null,
    /** ID của Content đang ở chế độ edit (null = view mode). */
    val editingContentId: String? = null,
    /** True khi đang khởi tạo một Content mới. */
    val isCreatingNewItem: Boolean = false,
    /** Bản thảo chưa lưu; null khi không edit/create. */
    val draftEdits: ContentDraftEdits? = null,
    /** Hiển thị dialog xác nhận xóa. */
    val showDeleteConfirm: Boolean = false,
    /** Hiển thị dialog cảnh báo thay đổi chưa lưu (row/package/back). */
    val showUnsavedChangesDialog: Boolean = false,
    /** Typed pending action để thực thi sau khi resolve unsaved dialog. */
    val pendingAction: PackageBrowserPendingAction? = null
) {
    val totalCount: Int get() = allItems.size

    /** True khi có thay đổi chưa lưu. */
    val isDirty: Boolean get() = (editingContentId != null && draftEdits != null) || isCreatingNewItem

    val filteredItems: List<PackageContentBrowserItem> by lazy {
        PackageContentBrowserProjectionPolicy.filterAndSort(
            items = allItems,
            query = appliedQuery,
            lessonFilter = selectedLessonFilter,
            mediaFilter = mediaFilter,
            sortOption = sortOption
        )
    }

    val selectedItemInView: PackageContentBrowserItem? get() {
        val id = selectedContentId ?: return null
        return filteredItems.firstOrNull { it.contentId.value == id }
    }

    val selectedItemAnywhere: PackageContentBrowserItem? get() {
        val id = selectedContentId ?: return null
        return allItems.firstOrNull { it.contentId.value == id }
    }

    val isFilterDefault: Boolean get() =
        query.isBlank() && appliedQuery.isBlank() && selectedLessonFilter == "ALL" &&
                mediaFilter == BrowserMediaFilter.ALL && sortOption == BrowserSortOption.ORIGINAL_ORDER
}
