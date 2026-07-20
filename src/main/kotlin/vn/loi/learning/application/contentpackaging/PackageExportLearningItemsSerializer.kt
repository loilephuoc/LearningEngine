package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.study.learning.model.LearningItem

/**
 * Chuyển danh sách LearningItem thành learning-items.json.
 */
class PackageExportLearningItemsSerializer(
    private val json: Json = Json { prettyPrint = true }
) {

    fun serialize(
        learningItems: List<LearningItem>
    ): String =
        json.encodeToString(
            PackageExportLearningItemsJson.from(learningItems)
        )
}
