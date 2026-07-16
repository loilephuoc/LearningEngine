package vn.loi.learning.infrastructure.importer.legacy

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.learning.model.LearningItem

/**
 * Kết quả sau khi chuyển đổi một file JSON cũ.
 */
data class LegacyImportResult(
    val contents: List<Content>,
    val learningItems: List<LearningItem>,
    val skippedRecords: List<SkippedLegacyRecord>
) {

    val importedContentCount: Int
        get() = contents.size

    val importedLearningItemCount: Int
        get() = learningItems.size

    val skippedRecordCount: Int
        get() = skippedRecords.size
}

data class SkippedLegacyRecord(
    val index: Int,
    val reason: String
)