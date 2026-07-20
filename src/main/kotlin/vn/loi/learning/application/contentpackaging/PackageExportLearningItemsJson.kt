package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.Serializable
import vn.loi.learning.domain.study.learning.model.LearningItem

/**
 * DTO JSON cho danh sách LearningItem trong package export.
 */
@Serializable
data class PackageExportLearningItemsJson(
    val learningItems: List<PackageExportLearningItemJson>
) {

    companion object {
        fun from(
            learningItems: List<LearningItem>
        ): PackageExportLearningItemsJson =
            PackageExportLearningItemsJson(
                learningItems = learningItems.map(PackageExportLearningItemJson::from)
            )
    }
}
