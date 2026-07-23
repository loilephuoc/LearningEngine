package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.library.model.InstalledPackage

sealed interface PackageImportOutcome {
    data class NewPackageInstalled(
        val installedPackage: InstalledPackage,
        val importedContentCount: Int,
        val importedLearningItemCount: Int
    ) : PackageImportOutcome

    data class AlreadyInstalledIdentical(
        val installedPackage: InstalledPackage,
        val message: String = "Package is already installed and identical."
    ) : PackageImportOutcome

    data class ReplacementCompleted(
        val installedPackage: InstalledPackage,
        val previousVersion: String,
        val newVersion: String,
        val preservedTopicId: String
    ) : PackageImportOutcome

    data class ConflictDetected(
        val decision: PackageImportDecision,
        val conflictReasons: List<String>
    ) : PackageImportOutcome

    data class TechnicalFailure(
        val sanitizedMessage: String
    ) : PackageImportOutcome
}
