package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Assembler kết hợp thông tin từ Engine, Library và Memory
 * để xây dựng projection cho Learning Workspace.
 */
class WorkspaceProjectionAssembler(
    private val applicationContext: LearningApplicationContext
) {

    fun assembleExploreItems(contentIdStr: String): List<LearningWorkspaceItemUiModel> {
        val contentId = ContentId(contentIdStr)
        val engine = applicationContext.engine

        val domainContent = engine.getContent(contentId)

        val libraryItems = applicationContext.libraryContents.queryForLibraries(
            setOf(ContentLibraryId(contentIdStr))
        )
        val libraryContentItem = libraryItems.firstOrNull { it.id == contentIdStr }

        val learningItems = engine.getLearningItemsByContentIds(setOf(contentId))
            .filter { it.contentId == contentId }

        val memoryRepo = applicationContext.memoryStateRepository
        val learnerId = LearnerId("default-learner")
        val now = Moment(System.currentTimeMillis())

        val primaryText = domainContent?.text?.primaryText ?: libraryContentItem?.primaryText
        val translatedText = domainContent?.text?.translatedText ?: libraryContentItem?.translatedText
        val pronunciation = domainContent?.text?.pronunciation
        val contentType = domainContent?.type?.name ?: libraryContentItem?.type

        if (learningItems.isEmpty()) {
            return listOf(
                LearningWorkspaceItemUiModel(
                    id = contentIdStr,
                    contentId = contentIdStr,
                    mode = "GENERAL",
                    primaryText = primaryText,
                    translatedText = translatedText,
                    pronunciation = pronunciation,
                    contentType = contentType,
                    isEnabled = true,
                    stage = "UNSEEN",
                    isDue = false
                )
            )
        }

        return learningItems.map { item ->
            val memoryState = memoryRepo?.find(learnerId, item.id)
            LearningWorkspaceItemUiModel(
                id = item.id.value,
                contentId = item.contentId.value,
                mode = item.mode.name,
                primaryText = primaryText,
                translatedText = translatedText,
                pronunciation = pronunciation,
                contentType = contentType,
                isEnabled = item.isEnabled,
                stage = memoryState?.stage?.name ?: "UNSEEN",
                isDue = memoryState?.isDue(now) ?: false
            )
        }
    }
}
