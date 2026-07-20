package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.Serializable
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

/**
 * DTO JSON của một LearningItem trong package export.
 */
@Serializable
data class PackageExportLearningItemJson(
    val id: String,
    val contentId: String,
    val mode: String,
    val isEnabled: Boolean
) {

    companion object {
        fun from(
            learningItem: LearningItem
        ): PackageExportLearningItemJson =
            PackageExportLearningItemJson(
                id = learningItem.id.value,
                contentId = learningItem.contentId.value,
                mode = learningItem.mode.name,
                isEnabled = learningItem.isEnabled
            )
    }

    fun toDomain(): LearningItem =
        LearningItem(
            id = LearningItemId(id),
            contentId = ContentId(contentId),
            mode = LearningMode.valueOf(mode),
            isEnabled = isEnabled
        )
}