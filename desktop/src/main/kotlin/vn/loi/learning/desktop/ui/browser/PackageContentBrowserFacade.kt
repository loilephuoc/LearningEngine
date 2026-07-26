package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Facade cung cấp dữ liệu cho Desktop Learning Browser 1.0.
 *
 * Chuyển giao các truy vấn từ Application Service [PackageContentBrowserQueryService]
 * sang presentation UI state [PackageContentBrowserUiState].
 *
 * Từ PLE-017A CP2: bổ sung [editService] và [learningItemRepository]
 * để hỗ trợ persist edit và delete.
 */
class PackageContentBrowserFacade(
    private val queryService: PackageContentBrowserQueryService? = null,
    private val editService: ContentBrowserEditService? = null,
    private val learningItemRepository: LearningItemRepository? = null
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

    /**
     * Persist một ContentDraftEdits vào repository.
     * Sau khi gọi thành công, gọi [loadForPackage] để reload data.
     *
     * @throws IllegalStateException nếu editService chưa được cung cấp.
     * @throws IllegalArgumentException nếu content không tồn tại hoặc validation thất bại.
     */
    fun persistEdit(
        draft: ContentDraftEdits,
        installedPackageId: InstalledPackageId,
        packageName: String
    ): PackageContentBrowserUiState {
        val service = editService
            ?: throw IllegalStateException("ContentBrowserEditService is not provided to PackageContentBrowserFacade.")

        service.updateTextFields(
            contentId = ContentId(draft.contentId),
            questionText = draft.questionText,
            answerText = draft.answerText,
            pronunciation = draft.pronunciation,
            partOfSpeech = draft.partOfSpeech,
            exampleText = draft.exampleText,
            exampleTranslation = draft.exampleTranslation
        )

        // Reload sau khi persist
        return loadForPackage(installedPackageId, packageName)
    }

    /**
     * Xóa một Content và tất cả LearningItem liên quan.
     * Sau khi xóa, reload danh sách.
     *
     * @throws IllegalStateException nếu editService hoặc learningItemRepository chưa được cung cấp.
     */
    fun deleteContent(
        contentId: ContentId,
        installedPackageId: InstalledPackageId,
        packageName: String
    ): PackageContentBrowserUiState {
        val service = editService
            ?: throw IllegalStateException("ContentBrowserEditService is not provided to PackageContentBrowserFacade.")
        val itemRepo = learningItemRepository
            ?: throw IllegalStateException("LearningItemRepository is not provided to PackageContentBrowserFacade.")

        service.deleteContent(
            contentId = contentId,
            learningItemRepository = itemRepo,
            installedPackageId = installedPackageId
        )

        return loadForPackage(installedPackageId, packageName)
    }
}
