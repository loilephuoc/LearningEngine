package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Inspection boundary trước mutation (AC-R1, AC-R2).
 * Đánh giá candidate package hoàn toàn READ-ONLY đối với repository.
 *
 * Canonical identity authority:
 * - PackageId (primary)
 * - TopicId (secondary)
 *
 * Package name / display name / filename / path không được dùng để resolve identity.
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

        // AC-R1: Only canonical identities - PackageId and TopicId
        val matchByPackageId = installedPackages.firstOrNull { it.packageId == candidatePackageId }
        val matchByTopicId = if (candidateTopicId != null) {
            installedPackages.firstOrNull { it.topicId == candidateTopicId }
        } else null

        // AC-R5: Ambiguous identity — PackageId and TopicId point to different records
        if (matchByPackageId != null && matchByTopicId != null &&
            matchByPackageId.id != matchByTopicId.id
        ) {
            return PackageImportDecision(
                type = ImportDecisionType.CONFLICT,
                candidatePackageId = candidatePackageId,
                candidateTopicId = candidateTopicId,
                candidateName = candidateName,
                candidateVersion = candidateVersion,
                candidateChecksum = candidateChecksum,
                existingInstalledPackageId = matchByPackageId.id,
                existingPackageId = matchByPackageId.packageId,
                existingVersion = matchByPackageId.version.value,
                conflictReasons = listOf(PackageImportConflictReason.AMBIGUOUS_EXISTING_IDENTITY)
            )
        }

        val existing: InstalledPackage? = matchByPackageId ?: matchByTopicId

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

        val existingVersionStr = existing.version.value
        val existingVersionParsed = NumericPackageVersion.parseOrNull(existingVersionStr)
        val candidateVersionParsed = NumericPackageVersion.parseOrNull(candidateVersion)

        // AC-R1: TopicId mismatch is always a conflict (regardless of PackageId match)
        if (candidateTopicId != null && existing.topicId != candidateTopicId) {
            return PackageImportDecision(
                type = ImportDecisionType.CONFLICT,
                candidatePackageId = candidatePackageId,
                candidateTopicId = candidateTopicId,
                candidateName = candidateName,
                candidateVersion = candidateVersion,
                candidateChecksum = candidateChecksum,
                existingInstalledPackageId = existing.id,
                existingPackageId = existing.packageId,
                existingVersion = existingVersionStr,
                conflictReasons = listOf(PackageImportConflictReason.TOPIC_ID_MISMATCH)
            )
        }

        // AC-R2: Identical check — same PackageId + same version
        // Conservative: if both candidate and installed have a canonical checksum AND they match → IDENTICAL
        // If checksums absent or unavailable, same PackageId+version is INSUFFICIENT_IDENTITY_EVIDENCE
        if (existing.packageId == candidatePackageId && existing.version.value == candidateVersion) {
            // Canonical content fingerprint comparison: both must supply checksum for identical
            val existingChecksum = existing.contentChecksum
            if (existingChecksum != null && candidateChecksum != null) {
                return if (existingChecksum == candidateChecksum) {
                    PackageImportDecision(
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
                } else {
                    // Same PackageId + same version + different canonical checksum → CONFLICT
                    PackageImportDecision(
                        type = ImportDecisionType.CONFLICT,
                        candidatePackageId = candidatePackageId,
                        candidateTopicId = candidateTopicId ?: existing.topicId,
                        candidateName = candidateName,
                        candidateVersion = candidateVersion,
                        candidateChecksum = candidateChecksum,
                        existingInstalledPackageId = existing.id,
                        existingPackageId = existing.packageId,
                        existingVersion = existingVersionStr,
                        conflictReasons = listOf(PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE)
                    )
                }
            } else {
                // No canonical fingerprint evidence available → conservative CONFLICT
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
                    conflictReasons = listOf(PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE)
                )
            }
        }

        // Version comparison for safe replacement vs older-version conflict
        if (candidateVersionParsed != null && existingVersionParsed != null) {
            if (candidateVersionParsed < existingVersionParsed) {
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
                    conflictReasons = listOf(PackageImportConflictReason.OLDER_VERSION)
                )
            }
        }

        // candidateVersion > existingVersion → SAFE_REPLACEMENT
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
