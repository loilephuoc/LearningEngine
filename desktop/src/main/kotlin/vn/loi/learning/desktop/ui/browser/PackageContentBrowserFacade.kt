package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.application.contentpackaging.browser.DeletedContentSnapshot
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.ContentMediaStorage
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
    fun importMediaAsset(packageName: String, file: java.io.File, storage: ContentMediaStorage): String =
        requireNotNull(editService) { "ContentBrowserEditService is not provided to PackageContentBrowserFacade." }
            .importMediaAsset(packageName, file, storage)

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

        val reloaded = try {
            loadForPackage(installedPackageId, packageName)
        } catch (failure: Exception) {
            throw CanonicalMutationCommittedException(created.id.value, "Create", failure)
        }

        // Post-condition: the created Content must appear in the reloaded package browser.
        // A failure here means the new item was persisted but not registered in package ownership.
        if (!reloaded.allItems.any { it.contentId == created.id }) {
            throw CanonicalMutationCommittedException(created.id.value, "Create", IllegalStateException(
                "Created Content is not visible in package ownership after refresh."
            ))
        }

        val postCreateCount = reloaded.allItems.size
        if (postCreateCount != preCreateCount + 1) {
            throw CanonicalMutationCommittedException(created.id.value, "Create", IllegalStateException(
                "Expected package projection count ${preCreateCount + 1}, but found $postCreateCount."
            ))
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

        val reloaded = try {
            loadForPackage(installedPackageId, packageName)
        } catch (failure: Exception) {
            throw CanonicalMutationCommittedException(draft.contentId, "Save", failure)
        }
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
    ): DeletedContentBrowserResult {
        val service = editService
            ?: throw IllegalStateException("ContentBrowserEditService is not provided to PackageContentBrowserFacade.")
        val itemRepo = learningItemRepository
            ?: throw IllegalStateException("LearningItemRepository is not provided to PackageContentBrowserFacade.")

        val snapshot = service.deleteContent(
            contentId = contentId,
            learningItemRepository = itemRepo,
            installedPackageId = installedPackageId
        )

        val reloaded = loadForPackage(installedPackageId, packageName)
        val selectedItem = reloaded.selectedItemInView ?: reloaded.allItems.firstOrNull()
        val selectedDraft = selectedItem?.toDraftEdits()

        return DeletedContentBrowserResult(reloaded.copy(
            selectedContentId = selectedItem?.contentId?.value,
            editingContentId = selectedItem?.contentId?.value,
            loadedBaselineDraft = selectedDraft,
            draftEdits = selectedDraft
        ), snapshot)
    }

    fun undoDelete(
        snapshot: DeletedContentSnapshot,
        installedPackageId: InstalledPackageId,
        packageName: String
    ): PackageContentBrowserUiState {
        require(snapshot.installedPackageId == installedPackageId) {
            "Cannot undo delete outside its original package."
        }
        val service = editService
            ?: throw IllegalStateException("ContentBrowserEditService is not provided to PackageContentBrowserFacade.")
        val itemRepo = learningItemRepository
            ?: throw IllegalStateException("LearningItemRepository is not provided to PackageContentBrowserFacade.")
        service.restoreDeletedContent(snapshot, itemRepo)
        return loadForPackage(installedPackageId, packageName)
    }
}

data class DeletedContentBrowserResult(
    val state: PackageContentBrowserUiState,
    val snapshot: DeletedContentSnapshot
)

class CanonicalMutationCommittedException(
    val contentId: String,
    val operation: String,
    cause: Throwable
) : IllegalStateException("$operation committed, but Content Studio refresh failed: ${cause.message}", cause)
