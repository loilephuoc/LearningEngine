package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Inspection boundary trước mutation (AC-1).
 * Đánh giá candidate package hoàn toàn READ-ONLY đối với repository.
 */
class PackageImportInspector(
    private val installedPackageRepository: InstalledPackageRepository
) {
    fun inspect(
        candidatePackageId: PackageId,
        candidateTopicId: TopicId?,
        candidateName: String,
        candidateVersion: String,
        candidateChecksum: String? = null,
        libraryId: LibraryId
    ): PackageImportDecision {
        val installedPackages = installedPackageRepository.findAllByLibraryId(libraryId)
            .filter { it.state != PackageState.REMOVED }

        val matchByPackageId = installedPackages.firstOrNull { it.packageId == candidatePackageId }
        val matchByTopicId = if (candidateTopicId != null) {
            installedPackages.firstOrNull { it.topicId == candidateTopicId }
        } else null
        val matchByName = installedPackages.firstOrNull { it.name.value.equals(candidateName, ignoreCase = true) }

        val existing = matchByPackageId ?: matchByTopicId ?: matchByName

        if (existing == null) {
            return PackageImportDecision(
                type = ImportDecisionType.NEW_PACKAGE,
                candidatePackageId = candidatePackageId,
                candidateTopicId = candidateTopicId,
                candidateName = candidateName,
                candidateVersion = candidateVersion,
                candidateChecksum = candidateChecksum
            )
        }

        val conflictReasons = mutableListOf<String>()

        if (candidateTopicId != null && existing.topicId != candidateTopicId) {
            conflictReasons += "Candidate topic ID '${candidateTopicId.value}' conflicts with installed package topic ID '${existing.topicId.value}'."
        }

        if (matchByName != null && matchByName.packageId != candidatePackageId && matchByPackageId == null) {
            conflictReasons += "Package name '$candidateName' matches installed package '${existing.name.value}' but package ID '${candidatePackageId.value}' differs from installed ID '${existing.packageId.value}'."
        }

        val existingVersionStr = existing.version.value
        val existingVersion = NumericPackageVersion.parseOrNull(existingVersionStr)
        val candidateVersionParsed = NumericPackageVersion.parseOrNull(candidateVersion)

        // Deterministic identical check
        if (existing.packageId == candidatePackageId &&
            existing.version.value == candidateVersion &&
            conflictReasons.isEmpty()
        ) {
            return PackageImportDecision(
                type = ImportDecisionType.IDENTICAL_PACKAGE,
                candidatePackageId = candidatePackageId,
                candidateTopicId = candidateTopicId ?: existing.topicId,
                candidateName = candidateName,
                candidateVersion = candidateVersion,
                candidateChecksum = candidateChecksum,
                existingInstalledPackageId = existing.id,
                existingPackageId = existing.packageId,
                existingVersion = existingVersionStr
            )
        }

        if (candidateVersionParsed != null && existingVersion != null) {
            if (candidateVersionParsed < existingVersion) {
                conflictReasons += "Candidate package version '$candidateVersion' is older than installed version '$existingVersionStr'."
            } else if (candidateVersionParsed == existingVersion && candidateChecksum != null) {
                conflictReasons += "Candidate package version '$candidateVersion' is identical to installed version but checksum differs."
            }
        }

        if (conflictReasons.isNotEmpty()) {
            return PackageImportDecision(
                type = ImportDecisionType.CONFLICT,
                candidatePackageId = candidatePackageId,
                candidateTopicId = candidateTopicId ?: existing.topicId,
                candidateName = candidateName,
                candidateVersion = candidateVersion,
                candidateChecksum = candidateChecksum,
                existingInstalledPackageId = existing.id,
                existingPackageId = existing.packageId,
                existingVersion = existingVersionStr,
                conflictReasons = conflictReasons
            )
        }

        return PackageImportDecision(
            type = ImportDecisionType.SAFE_REPLACEMENT,
            candidatePackageId = candidatePackageId,
            candidateTopicId = candidateTopicId ?: existing.topicId,
            candidateName = candidateName,
            candidateVersion = candidateVersion,
            candidateChecksum = candidateChecksum,
            existingInstalledPackageId = existing.id,
            existingPackageId = existing.packageId,
            existingVersion = existingVersionStr
        )
    }
}
