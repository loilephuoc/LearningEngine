package vn.loi.learning.application.contentpackaging

import java.time.Instant
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Điều phối thao tác import gói bài học có kiểm soát xung đột (AC-6).
 * Đảm bảo tính nguyên tố (atomicity) và hoàn tác an toàn (rollback safety) (AC-7).
 *
 * AC-R3: Không bao giờ fabricate aggregate giả. Các outcome phải dùng
 * aggregate thực được lấy từ repository hoặc được persist thành công.
 */
class ConflictAwarePackageImporter(
    private val inspector: PackageImportInspector,
    private val installedPackageRepository: InstalledPackageRepository,
    private val transactionRunner: TransactionRunner
) {

    fun inspectCandidate(
        candidatePackageId: PackageId,
        candidateTopicId: TopicId?,
        candidateName: String,
        candidateVersion: String,
        candidateChecksum: String? = null,
        libraryId: LibraryId
    ): PackageImportDecision {
        return inspector.inspect(
            candidatePackageId = candidatePackageId,
            candidateTopicId = candidateTopicId,
            candidateName = candidateName,
            candidateVersion = candidateVersion,
            candidateChecksum = candidateChecksum,
            libraryId = libraryId
        )
    }

    fun executeImport(
        decision: PackageImportDecision,
        libraryId: LibraryId,
        contentCount: Int = 1,
        learningItemCount: Int = 1,
        contentChecksum: String? = null
    ): PackageImportOutcome {
        return when (decision.type) {
            ImportDecisionType.CONFLICT -> {
                PackageImportOutcome.ConflictDetected(
                    decision = decision,
                    conflictReasons = decision.conflictReasons
                )
            }

            ImportDecisionType.IDENTICAL_PACKAGE -> {
                // AC-R3: Must resolve the real aggregate from repository.
                // Inspector already verified canonical equality evidence exists — so existing record must be findable.
                val existingId = decision.existingInstalledPackageId
                    ?: return PackageImportOutcome.TechnicalFailure(
                        sanitizedMessage = "Identical package decision is missing existing installed package ID."
                    )
                val existing = installedPackageRepository.findById(existingId)
                    ?: return PackageImportOutcome.TechnicalFailure(
                        sanitizedMessage = "Repository state inconsistency: identical decision references a package that cannot be found."
                    )
                PackageImportOutcome.AlreadyInstalledIdentical(installedPackage = existing)
            }

            ImportDecisionType.NEW_PACKAGE -> {
                try {
                    val installedPkg = transactionRunner.runInTransaction {
                        val newInstId = InstalledPackageId("inst-" + decision.candidatePackageId.value)
                        val pkg = InstalledPackage.reconstitute(
                            id = newInstId,
                            libraryId = libraryId,
                            packageId = decision.candidatePackageId,
                            topicId = decision.candidateTopicId ?: TopicId("topic-" + decision.candidatePackageId.value),
                            name = PackageName(decision.candidateName),
                            version = PackageVersion(decision.candidateVersion),
                            state = PackageState.ACTIVE,
                            installedAt = Instant.now(),
                            contentCount = contentCount,
                            learningItemCount = learningItemCount,
                            contentChecksum = contentChecksum ?: decision.candidateChecksum
                        )
                        installedPackageRepository.save(pkg)
                        pkg
                    }
                    PackageImportOutcome.NewPackageInstalled(
                        installedPackage = installedPkg,
                        importedContentCount = installedPkg.contentCount,
                        importedLearningItemCount = installedPkg.learningItemCount
                    )
                } catch (ex: Throwable) {
                    PackageImportOutcome.TechnicalFailure(
                        sanitizedMessage = "Failed to install new package."
                    )
                }
            }

            ImportDecisionType.SAFE_REPLACEMENT -> {
                try {
                    val replacedPkg = transactionRunner.runInTransaction {
                        val existingInstId = decision.existingInstalledPackageId
                            ?: throw IllegalStateException("Existing package ID missing for replacement.")
                        val existing = installedPackageRepository.findById(existingInstId)
                            ?: throw IllegalStateException("Installed package record not found.")

                        // Preserve canonical TopicId (AC-R6), and update contentChecksum
                        val updated = InstalledPackage.reconstitute(
                            id = existing.id,
                            libraryId = existing.libraryId,
                            packageId = decision.candidatePackageId,
                            topicId = existing.topicId,
                            name = PackageName(decision.candidateName),
                            version = PackageVersion(decision.candidateVersion),
                            state = PackageState.ACTIVE,
                            installedAt = Instant.now(),
                            contentCount = if (contentCount > 0) contentCount else existing.contentCount,
                            learningItemCount = if (learningItemCount > 0) learningItemCount else existing.learningItemCount,
                            contentChecksum = contentChecksum ?: decision.candidateChecksum
                        )
                        installedPackageRepository.save(updated)
                        updated
                    }
                    PackageImportOutcome.ReplacementCompleted(
                        installedPackage = replacedPkg,
                        previousVersion = decision.existingVersion ?: "1.0.0",
                        newVersion = decision.candidateVersion,
                        preservedTopicId = replacedPkg.topicId.value
                    )
                } catch (ex: Throwable) {
                    PackageImportOutcome.TechnicalFailure(
                        sanitizedMessage = "Failed to apply package replacement."
                    )
                }
            }
        }
    }
}
