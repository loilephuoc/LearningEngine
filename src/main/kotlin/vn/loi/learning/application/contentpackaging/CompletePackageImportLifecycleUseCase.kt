package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.repository.LibraryRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.command.LibraryCommandService
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository

/** Completes raw package persistence into the canonical InstalledPackage/Library lifecycle. */
class CompletePackageImportLifecycleUseCase(
    private val importer: ConflictAwarePackageImporter,
    private val libraries: LibraryRepository,
    private val libraryId: LibraryId,
    private val installedPackages: InstalledPackageRepository,
    private val libraryCommands: LibraryCommandService,
    private val contentLibraries: ContentLibraryRepository,
    private val contents: ContentRepository,
    private val learningItems: LearningItemRepository
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
            val ownedContentIds = pkg.libraryIds
                .map { id -> requireNotNull(contentLibraries.findById(id)) { "Imported package references missing ContentLibrary $id." } }
                .flatMapTo(linkedSetOf()) { it.contentIds }
            val existingContentIds = contents.findByIds(ownedContentIds).mapTo(HashSet()) { it.id }
            require(existingContentIds == ownedContentIds) { "Imported package contains missing canonical Content records." }
            val ownedLearningItemCount = learningItems.findByContentIds(ownedContentIds).size
            importer.executeImport(
                decision, libraryId, existingContentIds.size, ownedLearningItemCount
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
        val committedPackage = when (outcome) {
            is PackageImportOutcome.NewPackageInstalled -> outcome.installedPackage
            is PackageImportOutcome.AlreadyInstalledIdentical -> outcome.installedPackage
            is PackageImportOutcome.ReplacementCompleted -> outcome.installedPackage
            else -> null
        }
        committedPackage?.let { expected ->
            require(installedPackages.findById(expected.id) == expected) {
                "InstalledPackage postcondition failed for ${expected.id}."
            }
        }
        outcome
    }
}
