package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Facade cung cấp dữ liệu cho Desktop Learning Browser 1.0 & Content Studio.
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
     * Tạo một Content mới và lưu vào repository.
     * Reload lại package content và chọn Content mới tạo.
     */
    fun createContent(
        draft: ContentDraftEdits,
        installedPackageId: InstalledPackageId,
        packageName: String
    ): PackageContentBrowserUiState {
        val service = editService
            ?: throw IllegalStateException("ContentBrowserEditService is not provided to PackageContentBrowserFacade.")

        val created = service.createContent(
            installedPackageId = installedPackageId,
            questionText = draft.questionText,
            answerText = draft.answerText,
            pronunciation = draft.pronunciation,
            partOfSpeech = draft.partOfSpeech,
            exampleText = draft.exampleText,
            exampleTranslation = draft.exampleTranslation,
            imageRef = draft.imageRef,
            questionAudioRef = draft.questionAudioRef,
            answerAudioRef = draft.answerAudioRef,
            exampleAudioRef = draft.exampleAudioRef,
            translationAudioRef = draft.translationAudioRef,
            learningItemRepository = learningItemRepository
        )

        val reloaded = loadForPackage(installedPackageId, packageName)
        return reloaded.copy(selectedContentId = created.id.value)
    }

    /**
     * Persist một ContentDraftEdits (cả text và media) vào repository.
     * Sau khi gọi thành công, gọi [loadForPackage] để reload data.
     */
    fun persistEdit(
        draft: ContentDraftEdits,
        installedPackageId: InstalledPackageId,
        packageName: String
    ): PackageContentBrowserUiState {
        val service = editService
            ?: throw IllegalStateException("ContentBrowserEditService is not provided to PackageContentBrowserFacade.")

        service.updateContent(
            contentId = ContentId(draft.contentId),
            questionText = draft.questionText,
            answerText = draft.answerText,
            pronunciation = draft.pronunciation,
            partOfSpeech = draft.partOfSpeech,
            exampleText = draft.exampleText,
            exampleTranslation = draft.exampleTranslation,
            imageRef = draft.imageRef,
            questionAudioRef = draft.questionAudioRef,
            answerAudioRef = draft.answerAudioRef,
            exampleAudioRef = draft.exampleAudioRef,
            translationAudioRef = draft.translationAudioRef
        )

        return loadForPackage(installedPackageId, packageName)
    }

    /**
     * Xóa một Content và tất cả LearningItem liên quan.
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
