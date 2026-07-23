package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Inspection boundary trước mutation (AC-1, AC-R1, AC-R2).
 * Đánh giá candidate package hoàn toàn READ-ONLY đối với repository.
 *
 * Canonical identity authority:
 * - PackageId (primary)
 * - TopicId (secondary)
 *
 * Package name / display name / filename / path không được dùng để resolve identity.
 *
 * SAFE_REPLACEMENT contract (AC-1):
 * - Chỉ trả về SAFE_REPLACEMENT khi candidate version > existing version (strict greater-than).
 * - Không dùng SAFE_REPLACEMENT làm fallback mặc định.
 * - Cả candidate và existing version đều phải parse được.
 * - Mọi trường hợp còn lại trả về typed CONFLICT.
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

        // AC-R1: Only canonical identities — PackageId and TopicId
        val matchByPackageId = installedPackages.firstOrNull { it.packageId == candidatePackageId }
        val matchByTopicId = if (candidateTopicId != null) {
            installedPackages.firstOrNull { it.topicId == candidateTopicId }
        } else null

        // AC-R5: Ambiguous identity — PackageId and TopicId point to different records
        if (matchByPackageId != null && matchByTopicId != null &&
            matchByPackageId.id != matchByTopicId.id
        ) {
            return conflict(
                candidatePackageId, candidateTopicId, candidateName, candidateVersion,
                candidateChecksum, matchByPackageId,
                listOf(PackageImportConflictReason.AMBIGUOUS_EXISTING_IDENTITY)
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

        // AC-R1: TopicId mismatch is always a conflict
        if (candidateTopicId != null && existing.topicId != candidateTopicId) {
            return conflict(
                candidatePackageId, candidateTopicId, candidateName, candidateVersion,
                candidateChecksum, existing,
                listOf(PackageImportConflictReason.TOPIC_ID_MISMATCH)
            )
        }

        val existingVersionStr = existing.version.value
        val candidateVersionParsed = NumericPackageVersion.parseOrNull(candidateVersion)
        val existingVersionParsed = NumericPackageVersion.parseOrNull(existingVersionStr)

        // AC-1: candidate version must parse; if not → INVALID_VERSION conflict
        if (candidateVersionParsed == null) {
            return conflict(
                candidatePackageId, candidateTopicId ?: existing.topicId, candidateName,
                candidateVersion, candidateChecksum, existing,
                listOf(PackageImportConflictReason.INVALID_VERSION)
            )
        }

        // AC-1: existing version must parse; if not → INVALID_VERSION conflict (legacy malformed)
        if (existingVersionParsed == null) {
            return conflict(
                candidatePackageId, candidateTopicId ?: existing.topicId, candidateName,
                candidateVersion, candidateChecksum, existing,
                listOf(PackageImportConflictReason.INVALID_VERSION)
            )
        }

        // AC-R2: Identical check — same PackageId + same parsed version
        if (existing.packageId == candidatePackageId &&
            candidateVersionParsed == existingVersionParsed
        ) {
            val existingChecksum = existing.contentChecksum
            return when {
                existingChecksum != null && candidateChecksum != null -> {
                    if (existingChecksum == candidateChecksum) {
                        // Canonical fingerprints match → IDENTICAL
                        identical(
                            candidatePackageId, candidateTopicId ?: existing.topicId,
                            candidateName, candidateVersion, candidateChecksum, existing
                        )
                    } else {
                        // Same version, different content fingerprint (AC-1: SAME_VERSION_DIFFERENT_CONTENT)
                        conflict(
                            candidatePackageId, candidateTopicId ?: existing.topicId,
                            candidateName, candidateVersion, candidateChecksum, existing,
                            listOf(PackageImportConflictReason.SAME_VERSION_DIFFERENT_CONTENT)
                        )
                    }
                }
                else -> {
                    // Missing checksum(s) — insufficient evidence to prove identical (AC-R2)
                    conflict(
                        candidatePackageId, candidateTopicId ?: existing.topicId,
                        candidateName, candidateVersion, candidateChecksum, existing,
                        listOf(PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE)
                    )
                }
            }
        }

        // AC-1: Strict ordering — candidateVersion < existingVersion → OLDER_VERSION
        if (candidateVersionParsed < existingVersionParsed) {
            return conflict(
                candidatePackageId, candidateTopicId ?: existing.topicId, candidateName,
                candidateVersion, candidateChecksum, existing,
                listOf(PackageImportConflictReason.OLDER_VERSION)
            )
        }

        // AC-1: candidateVersion > existingVersion → SAFE_REPLACEMENT (strict greater-than proven)
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

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun conflict(
        candidatePackageId: PackageId,
        resolvedTopicId: TopicId?,
        candidateName: String,
        candidateVersion: String,
        candidateChecksum: String?,
        existing: InstalledPackage,
        reasons: List<PackageImportConflictReason>
    ) = PackageImportDecision(
        type = ImportDecisionType.CONFLICT,
        candidatePackageId = candidatePackageId,
        candidateTopicId = resolvedTopicId,
        candidateName = candidateName,
        candidateVersion = candidateVersion,
        candidateChecksum = candidateChecksum,
        existingInstalledPackageId = existing.id,
        existingPackageId = existing.packageId,
        existingVersion = existing.version.value,
        conflictReasons = reasons
    )

    private fun identical(
        candidatePackageId: PackageId,
        resolvedTopicId: TopicId?,
        candidateName: String,
        candidateVersion: String,
        candidateChecksum: String?,
        existing: InstalledPackage
    ) = PackageImportDecision(
        type = ImportDecisionType.IDENTICAL_PACKAGE,
        candidatePackageId = candidatePackageId,
        candidateTopicId = resolvedTopicId,
        candidateName = candidateName,
        candidateVersion = candidateVersion,
        candidateChecksum = candidateChecksum,
        existingInstalledPackageId = existing.id,
        existingPackageId = existing.packageId,
        existingVersion = existing.version.value
    )
}
