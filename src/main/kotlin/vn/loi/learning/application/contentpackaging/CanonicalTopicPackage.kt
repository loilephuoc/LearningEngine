package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId

/**
 * Model đại diện canonical cho một gói chủ đề học tập (topic package).
 *
 * Chứa cấu trúc nội dung, danh mục kỹ năng (learning items), tham chiếu media
 * và thông tin nguồn độc lập với đường dẫn file hệ thống hay trạng thái UI.
 */
data class CanonicalTopicPackage(
    val topicId: TopicId,
    val logicalTopicName: String,
    val sourceMetadata: LegacyTopicSourceMetadata,
    val contents: List<Content>,
    val learningItems: List<LearningItem>,
    val mediaReferences: List<CanonicalMediaReference>,
    val tags: Set<String> = emptySet()
) {

    init {
        require(logicalTopicName.isNotBlank()) {
            "Canonical topic logical name must not be blank."
        }
    }
}

/**
 * Thẻ metadata thông tin nguồn gốc của gói chủ đề legacy.
 */
data class LegacyTopicSourceMetadata(
    val logicalTopicName: String,
    val jsonSource: String,
    val packageSource: String,
    val format: String = "OPD3",
    val version: String = "1"
) {

    init {
        require(logicalTopicName.isNotBlank()) {
            "Legacy topic source logical name must not be blank."
        }
        require(jsonSource.isNotBlank()) {
            "Legacy topic JSON source must not be blank."
        }
        require(packageSource.isNotBlank()) {
            "Legacy topic PKG source must not be blank."
        }
    }
}

/**
 * Biểu diễn tham chiếu media trong gói canonical.
 */
data class CanonicalMediaReference(
    val referencedAsset: String,
    val logicalPath: String,
    val mediaType: CanonicalMediaType,
    val owningContentId: ContentId,
    val owningLearningItemId: LearningItemId? = null,
    val status: CanonicalMediaStatus
) {

    init {
        require(referencedAsset.isNotBlank()) {
            "Referenced asset must not be blank."
        }
        require(logicalPath.isNotBlank()) {
            "Logical asset path must not be blank."
        }
    }
}

/**
 * Phân loại định dạng tệp media.
 */
enum class CanonicalMediaType {
    AUDIO,
    IMAGE,
    UNKNOWN
}

/**
 * Trạng thái hiện diện của file media trong gói tài nguyên PKG.
 */
enum class CanonicalMediaStatus {
    PRESENT,
    MISSING
}
