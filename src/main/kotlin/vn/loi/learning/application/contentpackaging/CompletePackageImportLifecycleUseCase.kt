package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.repository.LibraryRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.command.LibraryCommandService

/** Completes raw package persistence into the canonical InstalledPackage/Library lifecycle. */
class CompletePackageImportLifecycleUseCase(
    private val importer: ConflictAwarePackageImporter,
    private val libraries: LibraryRepository,
    private val libraryId: LibraryId,
    private val installedPackages: InstalledPackageRepository,
    private val libraryCommands: LibraryCommandService
) {
    fun execute(results: List<PackageImportResult>): List<PackageImportOutcome> = results.map { result ->
        val pkg = result.contentPackage
        val decision = importer.inspectCandidate(
            pkg.id, pkg.topicId, pkg.name, pkg.version, libraryId = libraryId
        )
        val outcome = if (
            decision.type == ImportDecisionType.CONFLICT &&
            decision.conflictReasons == listOf(PackageImportConflictReason.INSUFFICIENT_IDENTITY_EVIDENCE)
        ) {
            PackageImportOutcome.AlreadyInstalledIdentical(
                requireNotNull(decision.existingInstalledPackageId?.let(installedPackages::findById))
            )
        } else {
            require(decision.type != ImportDecisionType.CONFLICT) {
                "Imported package conflicts with installed package state."
            }
            importer.executeImport(
                decision, libraryId, result.importedContentCount, result.importedLearningItemCount
            )
        }
        require(outcome !is PackageImportOutcome.TechnicalFailure) {
            "Imported package could not be installed."
        }
        if (outcome is PackageImportOutcome.NewPackageInstalled) {
            val library = requireNotNull(libraries.findById(libraryId)) { "Library $libraryId does not exist." }
            val registered = library.registerEntry(
                outcome.installedPackage.id,
                outcome.installedPackage.packageId,
                outcome.installedPackage.installedAt
            )
            libraries.save(registered)
            if (registered.activePackageId == null) {
                require(libraryCommands.setActivePackage(libraryId, outcome.installedPackage.id) is LibraryCommandResult.Success) {
                    "Imported package could not become the active Library package."
                }
            }
        }
        outcome
    }
}
