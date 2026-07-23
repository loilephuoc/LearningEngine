package vn.loi.learning.application.contentpackaging

import java.time.Instant
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
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
        learningItemCount: Int = 1
    ): PackageImportOutcome {
        return when (decision.type) {
            ImportDecisionType.CONFLICT -> {
                PackageImportOutcome.ConflictDetected(
                    decision = decision,
                    conflictReasons = decision.conflictReasons
                )
            }

            ImportDecisionType.IDENTICAL_PACKAGE -> {
                val existing = decision.existingInstalledPackageId?.let { installedPackageRepository.findById(it) }
                    ?: installedPackageRepository.findByPackageId(decision.candidatePackageId)
                    ?: InstalledPackage.reconstitute(
                        id = InstalledPackageId("inst-" + decision.candidatePackageId.value),
                        libraryId = libraryId,
                        packageId = decision.candidatePackageId,
                        topicId = decision.candidateTopicId ?: TopicId("topic-" + decision.candidatePackageId.value),
                        name = PackageName(decision.candidateName),
                        version = PackageVersion(decision.candidateVersion),
                        state = PackageState.ACTIVE,
                        installedAt = Instant.now(),
                        contentCount = contentCount,
                        learningItemCount = learningItemCount
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
                            learningItemCount = learningItemCount
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
                        sanitizedMessage = "Failed to install new package: " + (ex.message ?: "infrastructure error")
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

                        // Preserve canonical TopicId and learner-owned progress
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
                            learningItemCount = if (learningItemCount > 0) learningItemCount else existing.learningItemCount
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
