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
    val selectedLessonFilter: String = "ALL", // "ALL" or specific lesson name
    val availableLessons: List<String> = emptyList(),
    val mediaFilter: BrowserMediaFilter = BrowserMediaFilter.ALL,
    val sortOption: BrowserSortOption = BrowserSortOption.ORIGINAL_ORDER,
    val selectedContentId: String? = null,
    val activePlayingAudioRef: String? = null
) {
    val totalCount: Int get() = allItems.size

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
