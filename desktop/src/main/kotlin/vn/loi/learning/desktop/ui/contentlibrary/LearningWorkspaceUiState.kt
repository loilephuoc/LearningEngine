package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Stage thông tin preview cho phiên học trong Learning Workspace.
 */
data class SessionPreviewStage(
    val key: String,
    val label: String
)

/**
 * Factory tạo session preview stages từ authoritative flow template types.
 */
object SessionPreviewFactory {
    fun defaultPreview(): List<SessionPreviewStage> {
        return listOf(
            SessionPreviewStage("primary", "Recall Prompt"),
            SessionPreviewStage("answer-reveal", "Reveal Answer"),
            SessionPreviewStage("rating-ready", "Rate Recall")
        )
    }

    fun fromTemplate(template: vn.loi.learning.application.flowtemplate.LearningFlowTemplate): List<SessionPreviewStage> {
        return template.stages.map { stage ->
            val label = when (stage) {
                is vn.loi.learning.application.flowtemplate.LearningFlowTemplateStage.Experience -> {
                    when (stage.slot) {
                        vn.loi.learning.application.flowtemplate.LearningFlowTemplateSlot.ROTATED_PRIMARY -> "Recall Prompt"
                        vn.loi.learning.application.flowtemplate.LearningFlowTemplateSlot.OPTIONAL_TYPING -> "Optional Typing Practice"
                    }
                }
                is vn.loi.learning.application.flowtemplate.LearningFlowTemplateStage.AnswerReveal -> "Reveal Answer"
                is vn.loi.learning.application.flowtemplate.LearningFlowTemplateStage.RatingReady -> "Rate Recall"
            }
            SessionPreviewStage(stage.key, label)
        }
    }
}

/**
 * Chế độ hiển thị của Learning Workspace state machine.
 */
enum class WorkspaceMode {
    EXPLORE,
    PREPARE
}

/**
 * UI model đại diện cho thông tin của một Learning Item trong Explore Mode.
 * Chứa thông tin xem trước nội dung học (content preview) làm nội dung chính
 * và các thông tin metadata (mode, stage, due) làm thông tin phụ.
 */
data class LearningWorkspaceItemUiModel(
    val id: String,
    val contentId: String,
    val mode: String,
    val primaryText: String? = null,
    val translatedText: String? = null,
    val pronunciation: String? = null,
    val contentType: String? = null,
    val isEnabled: Boolean = true,
    val stage: String = "UNSEEN",
    val isDue: Boolean = false
)

/**
 * Presentation projection cho Learning Workspace (bước chuẩn bị học trước khi mở Study).
 */
data class LearningWorkspaceUiState(
    val installedPackageId: InstalledPackageId,
    val contentId: ContentId,
    val packageName: String,
    val lessonTitle: String,
    val action: LessonStudyAction,
    val mode: WorkspaceMode = WorkspaceMode.EXPLORE,
    val isRecommended: Boolean = false,
    val recommendationReason: String? = null,
    val totalItemCount: Int = 0,
    val dueItemCount: Int = 0,
    val unseenItemCount: Int = 0,
    val startedItemCount: Int = 0,
    val masteredItemCount: Int = 0,
    val completionPercent: Int = 0,
    val previewStages: List<SessionPreviewStage> = SessionPreviewFactory.defaultPreview(),
    val canStart: Boolean = true,
    val unavailableReason: String? = null,
    val exploreItems: List<LearningWorkspaceItemUiModel> = emptyList(),
    val exploreIndex: Int = 0
) {
    val currentExploreItem: LearningWorkspaceItemUiModel?
        get() = exploreItems.getOrNull(exploreIndex) ?: exploreItems.firstOrNull()

    val canNavigatePrevious: Boolean
        get() = exploreIndex > 0

    val canNavigateNext: Boolean
        get() = exploreIndex < exploreItems.size - 1

    val availableModes: List<String>
        get() = exploreItems.map { it.mode }.distinct()
}

/**
 * Pure policy chiếu dữ liệu hiện có thành điểm chuẩn bị học LearningWorkspaceUiState.
 */
object LearningWorkspaceProjectionPolicy {

    fun create(
        browserState: LessonBrowserUiState,
        selectedItem: LessonBrowserItem,
        exploreItems: List<LearningWorkspaceItemUiModel> = emptyList(),
        mode: WorkspaceMode = WorkspaceMode.EXPLORE,
        exploreIndex: Int = 0
    ): LearningWorkspaceUiState? {
        val pkgId = browserState.installedPackageId ?: return null

        val action = LessonStudyActionPolicy.evaluate(selectedItem.progress, selectedItem.learningItemCount)

        val recommendation = browserState.recommendation
        val isRec = recommendation != null && recommendation.contentId.value == selectedItem.id
        val recReason = if (isRec) recommendation.reasonText else null

        val total = maxOf(selectedItem.progress.totalLearningItemCount, selectedItem.learningItemCount)
        val canStart = action.isEnabled && total > 0

        val unavailReason = when {
            total == 0 -> "No learning items available for this lesson."
            !action.isEnabled -> "Lesson study action is currently unavailable."
            else -> null
        }

        return LearningWorkspaceUiState(
            installedPackageId = pkgId,
            contentId = ContentId(selectedItem.id),
            packageName = browserState.libraryName,
            lessonTitle = selectedItem.title,
            action = action,
            mode = mode,
            isRecommended = isRec,
            recommendationReason = recReason,
            totalItemCount = total,
            dueItemCount = selectedItem.progress.dueItemCount,
            unseenItemCount = selectedItem.progress.unseenItemCount + selectedItem.progress.newStateItemCount,
            startedItemCount = selectedItem.progress.startedItemCount,
            masteredItemCount = selectedItem.progress.masteredItemCount,
            completionPercent = selectedItem.progress.completionPercent,
            previewStages = SessionPreviewFactory.defaultPreview(),
            canStart = canStart,
            unavailableReason = unavailReason,
            exploreItems = exploreItems,
            exploreIndex = exploreIndex
        )
    }
}
