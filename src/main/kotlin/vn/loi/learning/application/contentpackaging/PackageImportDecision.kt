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
 * Typed conflict reason enum (AC-R4).
 * Consumer không cần parse string; có thể switch theo typed reason.
 */
enum class PackageImportConflictReason {
    /** Candidate TopicId khác với InstalledPackage TopicId hiện có. */
    TOPIC_ID_MISMATCH,

    /** Candidate PackageId và TopicId mỗi loại trỏ đến hai record khác nhau (identity ambiguous). */
    AMBIGUOUS_EXISTING_IDENTITY,

    /** Candidate version cũ hơn installed version. */
    OLDER_VERSION,

    /**
     * Candidate và installed cùng PackageId + cùng version,
     * nhưng repository không lưu canonical content fingerprint để chứng minh identical content.
     * Theo conservative contract: không kết luận identical khi thiếu bằng chứng.
     */
    INSUFFICIENT_IDENTITY_EVIDENCE,
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
