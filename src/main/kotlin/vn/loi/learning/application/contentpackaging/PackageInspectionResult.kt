package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Kết quả kiểm tra thông tin gói nội dung OPD3 từ Package Inspector.
 */
data class PackageInspectionResult(
    val packageVersion: String,
    val schemaVersion: String,
    val topicId: TopicId?,
    val topicName: String,
    val format: String = "OPD3",
    val contentCount: Int,
    val learningItemCount: Int,
    val mediaCount: Int,
    val assetSizes: Map<String, Long>,
    val checksums: Map<String, String>,
    val diagnostics: List<String>
) {
    val isValid: Boolean
        get() = diagnostics.none { it.startsWith("ERROR:") }

    val errors: List<String>
        get() = diagnostics.filter { it.startsWith("ERROR:") }

    val warnings: List<String>
        get() = diagnostics.filter { it.startsWith("WARNING:") }
}
