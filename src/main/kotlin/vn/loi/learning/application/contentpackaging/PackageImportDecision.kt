package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId

enum class ImportDecisionType {
    NEW_PACKAGE,
    IDENTICAL_PACKAGE,
    SAFE_REPLACEMENT,
    CONFLICT
}

/**
 * Typed conflict reason enum (AC-R4, AC-1).
 * Consumer không cần parse string; có thể switch theo typed reason.
 * Chỉ chứa reason có ít nhất một code path và test tương ứng.
 */
enum class PackageImportConflictReason {
    /** Candidate TopicId khác với InstalledPackage TopicId hiện có. */
    TOPIC_ID_MISMATCH,

    /** Candidate PackageId và TopicId mỗi loại trỏ đến hai record khác nhau (identity ambiguous). */
    AMBIGUOUS_EXISTING_IDENTITY,

    /** Candidate version cũ hơn installed version. */
    OLDER_VERSION,

    /**
     * Same PackageId + same version nhưng candidate và installed không có checksum để chứng minh identity.
     * Thiếu bằng chứng canonical content → conservative reject.
     */
    INSUFFICIENT_IDENTITY_EVIDENCE,

    /**
     * Same PackageId + same version nhưng checksum KHÁC NHAU — nội dung thực sự khác.
     * Tách biệt khỏi INSUFFICIENT_IDENTITY_EVIDENCE vì evidence đã đủ để kết luận conflict.
     */
    SAME_VERSION_DIFFERENT_CONTENT,

    /**
     * Candidate version string không parse được thành numeric version.
     * Không thể chứng minh candidate mới hơn → không cho phép replacement.
     */
    INVALID_VERSION,
}

data class PackageImportDecision(
    val type: ImportDecisionType,
    val candidatePackageId: PackageId,
    val candidateTopicId: TopicId?,
    val candidateName: String,
    val candidateVersion: String,
    val candidateChecksum: String? = null,
    val existingInstalledPackageId: InstalledPackageId? = null,
    val existingPackageId: PackageId? = null,
    val existingVersion: String? = null,
    val conflictReasons: List<PackageImportConflictReason> = emptyList()
) {
    val isExecutable: Boolean
        get() = type != ImportDecisionType.CONFLICT
}
