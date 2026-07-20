package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.study.learning.model.LearningItem

/**
 * Tạo learning-items.json từ danh sách LearningItem.
 */
class PackageExportLearningItemsFileFactory(
    private val serializer: PackageExportLearningItemsSerializer = PackageExportLearningItemsSerializer()
) {

    fun create(
        learningItems: List<LearningItem>
    ): PackageExportFile =
        PackageExportFile(
            relativePath = "learning-items.json",
            content = serializer.serialize(learningItems)
        )
}
