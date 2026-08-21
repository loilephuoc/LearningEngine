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
    val imageStatusFilter: ImageStatusFilter = ImageStatusFilter.ALL,
    val sortOption: BrowserSortOption = BrowserSortOption.ORIGINAL_ORDER,
    val problemFilter: ContentProblemFilter = ContentProblemFilter.NONE,
    val problemProjection: ContentProblemProjection = ContentProblemProjection(),
    val selectedContentId: String? = null,
    /** Runtime-only Content Studio markers. Never persisted or copied into domain content. */
    val highlightedContentIds: Set<String> = emptySet(),
    /** Runtime-only work set, independent from the primary editor row and Highlight. */
    val selectedContentIds: Set<String> = emptySet(),
    /** Content identity used to resolve Shift ranges against the current visible ordering. */
    val selectionAnchorContentId: String? = null,
    val selectedMediaCheck: SelectedMediaCheckSummary? = null,
    val pendingBatchPartOfSpeech: String? = null,
    val isBatchPartOfSpeechSubmitting: Boolean = false,
    val batchPartOfSpeechResult: String? = null,
    val pendingBatchDeleteContentIds: Set<String> = emptySet(),
    val batchDeleteBlockerMessage: String? = null,
    val isBatchDeleteSubmitting: Boolean = false,
    /** Changes only when clearing search should center the current editor row. */
    val centerSelectedRowRequest: Long = 0L,
    val activePlayingAudioRef: String? = null,
    /** ID của Content đang ở chế độ edit (null = view mode). */
    val editingContentId: String? = null,
    /** True khi đang khởi tạo một Content mới. */
    val isCreatingNewItem: Boolean = false,
    /** Baseline snapshot của item khi vừa load/save (để so sánh dirty thực sự). */
    val loadedBaselineDraft: ContentDraftEdits? = null,
    /** Bản thảo chưa lưu; null khi không edit/create. */
    val draftEdits: ContentDraftEdits? = null,
    /** Hiển thị dialog xác nhận xóa. */
    val showDeleteConfirm: Boolean = false,
    /** Canonical single-delete target resolved from explicit selection before primary fallback. */
    val deleteTargetContentId: String? = null,
    /** Hiển thị dialog cảnh báo thay đổi chưa lưu (row/package/back). */
    val showUnsavedChangesDialog: Boolean = false,
    /** Typed pending action để thực thi sau khi resolve unsaved dialog. */
    val pendingAction: PackageBrowserPendingAction? = null,
    val canUndoDelete: Boolean = false,
    val undoDeleteLabel: String? = null,
    val canUndoBatchTts: Boolean = false,
    val undoBatchTtsLabel: String? = null,
    val lastBatchTtsUndoSnapshot: vn.loi.learning.desktop.tts.batch.BatchTtsUndoSnapshot? = null,
    val isCreateSubmitting: Boolean = false,
    val posReviewState: vn.loi.learning.desktop.ui.browser.posreview.PosBatchReviewState? = null,
    val contentMaintenanceExportState: vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportState? = null
) {
    val totalCount: Int get() = allItems.size

    val duplicateImageCounts: Map<String, Int> by lazy {
        ImageStatusProjectionPolicy.computeDuplicateImageCounts(allItems)
    }

    val duplicateImageKeys: Set<String> by lazy {
        duplicateImageCounts.keys
    }

    val duplicateQuestionCounts: Map<String, Int> by lazy {
        ImageStatusProjectionPolicy.computeDuplicateQuestionCounts(allItems)
    }

    val duplicateQuestionKeys: Set<String> by lazy {
        duplicateQuestionCounts.keys
    }

    /**
     * True KHI VÀ CHỈ KHI người dùng có thao tác chỉnh sửa thực sự trên bản thảo
     * so với baseline ban đầu của item được chọn, HOẶC đang tạo mới item (isCreatingNewItem).
     * Mở chế độ edit hay chọn xem item KHÔNG BAO GIỜ làm isDirty = true.
     */
    val isDirty: Boolean get() {
        if (isCreatingNewItem) return true
        if (editingContentId == null || draftEdits == null || loadedBaselineDraft == null) return false
        return draftEdits != loadedBaselineDraft
    }

    val filteredItems: List<PackageContentBrowserItem> by lazy {
        val baseFiltered = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = allItems,
            query = appliedQuery,
            lessonFilter = selectedLessonFilter,
            mediaFilter = mediaFilter,
            sortOption = sortOption
        ).filter { problemProjection.matches(it.contentId.value, problemFilter) }

        val imageFiltered = ImageStatusProjectionPolicy.filter(
            items = baseFiltered,
            filter = imageStatusFilter,
            duplicateImageKeys = duplicateImageKeys,
            duplicateQuestionKeys = duplicateQuestionKeys
        )
        when (imageStatusFilter) {
            ContentItemFilter.DUPLICATE_IMAGE -> {
                // Group items sharing the same duplicate image filename together while preserving original relative order inside each group
                imageFiltered.sortedWith(
                    compareBy(
                        { it.imageRef?.let(ImageStatusProjectionPolicy::imageRefKey).orEmpty() },
                        { it.index }
                    )
                )
            }
            ContentItemFilter.DUPLICATE_QUESTION -> {
                // Group items sharing the same duplicate question together while preserving original relative order inside each group
                imageFiltered.sortedWith(
                    compareBy(
                        { ImageStatusProjectionPolicy.questionKey(it.questionText) },
                        { it.index }
                    )
                )
            }
            else -> imageFiltered
        }
    }

    val duplicateImageGroups: List<DuplicateImageGroup> by lazy {
        if (imageStatusFilter == ImageStatusFilter.DUPLICATE_IMAGE) {
            ImageStatusProjectionPolicy.computeDuplicateGroups(filteredItems)
        } else {
            emptyList()
        }
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
                mediaFilter == BrowserMediaFilter.ALL && imageStatusFilter == ImageStatusFilter.ALL &&
                sortOption == BrowserSortOption.ORIGINAL_ORDER && problemFilter == ContentProblemFilter.NONE
}
