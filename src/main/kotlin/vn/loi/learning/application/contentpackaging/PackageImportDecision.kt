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
    val conflictReasons: List<String> = emptyList()
) {
    val isExecutable: Boolean
        get() = type != ImportDecisionType.CONFLICT
}
