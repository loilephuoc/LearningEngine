package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Các hành động bị hoãn (pending actions) khi người dùng đang có bản thảo chưa lưu (dirty draft).
 * Được sử dụng bởi Unsaved Changes Guard để thực thi chính xác sau khi người dùng chọn Save hoặc Discard.
 */
sealed interface PackageBrowserPendingAction {
    data class SelectRow(val contentId: String) : PackageBrowserPendingAction
    data class DoubleClickRow(val contentId: String) : PackageBrowserPendingAction
    data class FocusImage(val contentId: String) : PackageBrowserPendingAction
    data class PlayQuestionAudio(val contentId: String, val audioRef: String) : PackageBrowserPendingAction
    object CloseBrowser : PackageBrowserPendingAction
    object BackToLibrary : PackageBrowserPendingAction
    data class BrowsePackage(val installedPackageId: InstalledPackageId, val packageName: String) : PackageBrowserPendingAction
    data class DeleteContent(val contentId: String) : PackageBrowserPendingAction
    data class BatchDelete(val contentIds: Set<String>) : PackageBrowserPendingAction
    data class ApplyQuery(val query: String) : PackageBrowserPendingAction
    data class ApplyLessonFilter(val lessonFilter: String) : PackageBrowserPendingAction
    data class ApplyMediaFilter(val mediaFilter: BrowserMediaFilter) : PackageBrowserPendingAction
    data class ApplySort(val sortOption: BrowserSortOption) : PackageBrowserPendingAction
    data class ApplyProblemFilter(val problemFilter: ContentProblemFilter) : PackageBrowserPendingAction
    object ResetFilters : PackageBrowserPendingAction
    object StartCreate : PackageBrowserPendingAction
}
