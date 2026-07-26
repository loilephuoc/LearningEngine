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
        val firstItem = items.firstOrNull()
        val firstDraft = firstItem?.toDraftEdits()

        return PackageContentBrowserUiState(
            installedPackageId = installedPackageId,
            packageName = packageName,
            allItems = items,
            availableLessons = availableLessons,
            selectedContentId = firstItem?.contentId?.value,
            editingContentId = firstItem?.contentId?.value,
            loadedBaselineDraft = firstDraft,
            draftEdits = firstDraft
        )
    }

    /**
     * Tạo một Content mới và lưu vào repository.
     * Reload lại package content và chọn Content mới tạo.
     *
     * Post-condition: verifies the created Content is visible in the reloaded package browser.
     * Throws if the created item is absent after reload — indicating an ownership registration failure.
     */
    fun createContent(
        draft: ContentDraftEdits,
        installedPackageId: InstalledPackageId,
        packageName: String
    ): PackageContentBrowserUiState {
        val service = editService
            ?: throw IllegalStateException("ContentBrowserEditService is not provided to PackageContentBrowserFacade.")

        val preCreateState = loadForPackage(installedPackageId, packageName)
        val preCreateCount = preCreateState.allItems.size

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

        // Post-condition: the created Content must appear in the reloaded package browser.
        // A failure here means the new item was persisted but not registered in package ownership.
        if (!reloaded.allItems.any { it.contentId == created.id }) {
            throw IllegalStateException(
                "Created Content '${created.id.value}' is not visible in package ownership after reload. " +
                    "The item was persisted but not registered in the correct ContentLibrary."
            )
        }

        val postCreateCount = reloaded.allItems.size
        if (postCreateCount != preCreateCount + 1) {
            throw IllegalStateException(
                "Expected package content count to increase by 1 after create " +
                    "(was $preCreateCount, now $postCreateCount) for Content '${created.id.value}'."
            )
        }

        val createdItem = reloaded.allItems.firstOrNull { it.contentId == created.id }
        val createdDraft = createdItem?.toDraftEdits()

        return reloaded.copy(
            selectedContentId = created.id.value,
            editingContentId = created.id.value,
            isCreatingNewItem = false,
            loadedBaselineDraft = createdDraft,
            draftEdits = createdDraft
        )
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

        val reloaded = loadForPackage(installedPackageId, packageName)
        val updatedItem = reloaded.allItems.firstOrNull { it.contentId.value == draft.contentId }
            ?: reloaded.selectedItemInView
        val updatedDraft = updatedItem?.toDraftEdits()

        return reloaded.copy(
            selectedContentId = updatedItem?.contentId?.value ?: draft.contentId,
            editingContentId = updatedItem?.contentId?.value ?: draft.contentId,
            isCreatingNewItem = false,
            loadedBaselineDraft = updatedDraft,
            draftEdits = updatedDraft
        )
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

        val reloaded = loadForPackage(installedPackageId, packageName)
        val selectedItem = reloaded.selectedItemInView ?: reloaded.allItems.firstOrNull()
        val selectedDraft = selectedItem?.toDraftEdits()

        return reloaded.copy(
            selectedContentId = selectedItem?.contentId?.value,
            editingContentId = selectedItem?.contentId?.value,
            loadedBaselineDraft = selectedDraft,
            draftEdits = selectedDraft
        )
    }
}
