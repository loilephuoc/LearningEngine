package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Facade cung cấp dữ liệu cho Desktop Learning Browser 1.0.
 *
 * Chuyển giao các truy vấn từ Application Service [PackageContentBrowserQueryService]
 * sang presentation UI state [PackageContentBrowserUiState].
 */
class PackageContentBrowserFacade(
    private val queryService: PackageContentBrowserQueryService? = null
) {
    fun loadForPackage(
        installedPackageId: InstalledPackageId,
        packageName: String
    ): PackageContentBrowserUiState {
        val query = queryService
            ?: throw IllegalStateException("PackageContentBrowserQueryService is not provided to PackageContentBrowserFacade.")

        val items = query.getBrowserItemsForPackage(installedPackageId)
        val availableLessons = items.map { it.lesson }.distinct().sorted()

        return PackageContentBrowserUiState(
            installedPackageId = installedPackageId,
            packageName = packageName,
            allItems = items,
            availableLessons = availableLessons,
            selectedContentId = items.firstOrNull()?.contentId?.value
        )
    }
}
