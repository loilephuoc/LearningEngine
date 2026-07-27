package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionId

class PackageUninstallOperation(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val contentPackageRepository: ContentPackageRepository,
    private val packageCatalogRepository: PackageCatalogRepository,
    private val removalDependencyGuard:
    PackageRemovalDependencyGuard =
        PackageRemovalDependencyGuard(
            contentPackageRepository
        ),
    private val installedPackageRepository: InstalledPackageRepository? = null,
    private val libraryRepository: LibraryRepository? = null,
    private val collectionRepository: CollectionRepository? = null,
    private val memoryStateRepository: MemoryStateRepository? = null,
    private val reviewEventRepository: ReviewEventRepository? = null,
    private val studySessionRepository: StudySessionRepository? = null,
    private val studyQueueRepository: vn.loi.learning.application.port.StudyQueueRepository? = null
) {

    fun execute(
        command: UninstallContentPackageCommand
    ) {
        val plan = resolveRemovalPlan(command)
        plan.contentPackageIds.forEach(removalDependencyGuard::ensureCanRemove)

        if (plan.learningItemIds.isNotEmpty()) {
            memoryStateRepository?.deleteByLearningItemIds(plan.learningItemIds)
            reviewEventRepository?.deleteByLearningItemIds(plan.learningItemIds)
        }
        plan.studySessionIds.forEach { sessionId ->
            studyQueueRepository?.deleteBySessionId(sessionId)
            studySessionRepository?.deleteById(sessionId)
        }
        plan.topicIds.forEach { studySessionRepository?.deleteForTopic(it) }
        learningItemRepository.deleteAllById(plan.learningItemIds)
        contentRepository.deleteAllById(plan.contentIds)
        contentLibraryRepository.deleteAllById(plan.contentLibraryIds)
        plan.contentPackageIds.forEach(contentPackageRepository::deleteById)

        val catalog = packageCatalogRepository.findById(command.catalogId)
        if (catalog != null) {
            val updatedCatalog = plan.catalogPackageIds.fold(catalog) { current, packageId ->
                if (current.contains(packageId)) current.remove(packageId) else current
            }
            if (updatedCatalog != catalog) packageCatalogRepository.save(updatedCatalog)
        }

        plan.installedPackages.forEach { matchingInstPkg ->
            installedPackageRepository?.delete(matchingInstPkg.id)
            libraryRepository?.findById(matchingInstPkg.libraryId)
                ?.takeIf { it.hasPackage(matchingInstPkg.id) }
                ?.let { libraryRepository.save(it.unregisterEntry(matchingInstPkg.id)) }
            collectionRepository?.findAllByLibraryId(matchingInstPkg.libraryId)
                .orEmpty()
                .filter { it.containsPackage(matchingInstPkg.id) }
                .forEach { collection ->
                    collectionRepository?.save(collection.removePackage(matchingInstPkg.id).aggregate)
                }
        }
    }

    private fun resolveRemovalPlan(
        command: UninstallContentPackageCommand
    ): PackageUninstallRemovalPlan {
        val allInstalledPackages = installedPackageRepository?.findAll().orEmpty()
        val matchingInstalledPackages = allInstalledPackages.filter { instPkg ->
            instPkg.packageId == command.packageId ||
                instPkg.id.value == command.packageId.value ||
                instPkg.name.value.equals(command.packageId.value, ignoreCase = true) ||
                instPkg.topicId.value == command.packageId.value
        }
        val candidatePackageIds = (
            setOf(command.packageId) +
                matchingInstalledPackages.map { it.packageId } +
                matchingInstalledPackages.map { PackageId(it.id.value) } +
                matchingInstalledPackages.map { PackageId(it.name.value) } +
                matchingInstalledPackages.map { PackageId(it.topicId.value) }
        ).toSet()
        val allContentPackages = contentPackageRepository.findAll()
        val matchingContentPackages = allContentPackages.filter { contentPackage ->
            candidatePackageIds.any { candidate ->
                contentPackage.id == candidate ||
                    contentPackage.name.equals(candidate.value, ignoreCase = true) ||
                    contentPackage.topicId.value == candidate.value
            }
        }
        if (matchingInstalledPackages.isNotEmpty() && matchingContentPackages.isEmpty()) {
            error("Cannot uninstall ${command.packageId}: installed package ownership has no ContentPackage.")
        }
        if (matchingInstalledPackages.any { it.contentCount > 0 } &&
            matchingContentPackages.all { it.libraryIds.isEmpty() }
        ) {
            error("Cannot uninstall ${command.packageId}: installed content ownership has no ContentLibrary.")
        }
        val ownedLibraryIds = matchingContentPackages.flatMapTo(HashSet()) { it.libraryIds }
        val allLibraries = contentLibraryRepository.findAll()
        val librariesById = allLibraries.associateBy { it.id }
        val missingLibraryIds = ownedLibraryIds - librariesById.keys
        if (matchingInstalledPackages.isNotEmpty() && missingLibraryIds.isNotEmpty()) {
            error(
                "Cannot uninstall ${command.packageId}: missing owned ContentLibrary records " +
                    missingLibraryIds.joinToString()
            )
        }
        val matchingContentPackageIds = matchingContentPackages.mapTo(HashSet()) { it.id }
        val sharedLibraryIds = allContentPackages
            .asSequence()
            .filter { it.id !in matchingContentPackageIds }
            .flatMap { it.libraryIds.asSequence() }
            .toSet()
        val removableLibraryIds = ownedLibraryIds - sharedLibraryIds
        val removableContentCandidates = removableLibraryIds
            .asSequence()
            .mapNotNull(librariesById::get)
            .flatMap { it.contentIds.asSequence() }
            .toSet()
        val preservedContentIds = allLibraries
            .asSequence()
            .filter { it.id !in removableLibraryIds }
            .flatMap { it.contentIds.asSequence() }
            .toSet()
        val removableContentIds = removableContentCandidates - preservedContentIds
        val removableLearningItemIds = (
            learningItemRepository.findAllEnabled().filter { it.contentId in removableContentIds } +
                learningItemRepository.findByContentIds(removableContentIds)
            ).mapTo(HashSet()) { it.id }
        val topicIds = (
            matchingInstalledPackages.map { it.topicId } +
                matchingContentPackages.map { it.topicId }
        ).toSet()
        val installedPackageIds = matchingInstalledPackages.mapTo(HashSet()) { it.id }
        val packageIdValues = candidatePackageIds.mapTo(HashSet()) { it.value }
        val matchingSessionIds = studySessionRepository?.findAll().orEmpty()
            .filter { session ->
                session.installedPackageId in installedPackageIds ||
                    session.installedPackageId?.value in packageIdValues ||
                    session.topicId in topicIds ||
                    session.includedContentIds.any { it in removableContentIds }
            }
            .mapTo(HashSet()) { it.id }

        return PackageUninstallRemovalPlan(
            installedPackages = matchingInstalledPackages.toList(),
            contentPackageIds = matchingContentPackageIds.toSet(),
            contentLibraryIds = removableLibraryIds.toSet(),
            contentIds = removableContentIds.toSet(),
            learningItemIds = removableLearningItemIds.toSet(),
            studySessionIds = matchingSessionIds.toSet(),
            topicIds = topicIds,
            catalogPackageIds = (matchingContentPackageIds + command.packageId).toSet()
        )
    }
}

private data class PackageUninstallRemovalPlan(
    val installedPackages: List<vn.loi.learning.domain.library.model.InstalledPackage>,
    val contentPackageIds: Set<PackageId>,
    val contentLibraryIds: Set<ContentLibraryId>,
    val contentIds: Set<ContentId>,
    val learningItemIds: Set<LearningItemId>,
    val studySessionIds: Set<SessionId>,
    val topicIds: Set<TopicId>,
    val catalogPackageIds: Set<PackageId>
)
