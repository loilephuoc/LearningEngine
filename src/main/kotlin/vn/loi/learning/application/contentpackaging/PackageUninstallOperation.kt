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
        val catalog = packageCatalogRepository.findById(command.catalogId)

        val allInstPkgs = installedPackageRepository?.findAll().orEmpty()

        val matchingInstPkgs = allInstPkgs.filter { instPkg ->
            instPkg.packageId == command.packageId ||
                instPkg.id.value == command.packageId.value ||
                instPkg.name.value.equals(command.packageId.value, ignoreCase = true) ||
                instPkg.topicId.value == command.packageId.value
        }

        val candidatePkgIds = (
            setOf(command.packageId) +
                matchingInstPkgs.map { it.packageId } +
                matchingInstPkgs.map { vn.loi.learning.domain.content.packaging.model.PackageId(it.id.value) } +
                matchingInstPkgs.map { vn.loi.learning.domain.content.packaging.model.PackageId(it.name.value) } +
                matchingInstPkgs.map { vn.loi.learning.domain.content.packaging.model.PackageId(it.topicId.value) }
        ).toSet()

        val allContentPackages = contentPackageRepository.findAll().filter { cp ->
            candidatePkgIds.any { candidate ->
                cp.id == candidate ||
                    cp.name.equals(candidate.value, ignoreCase = true) ||
                    cp.topicId.value == candidate.value ||
                    cp.libraryIds.any { libId -> libId.value == candidate.value }
            }
        }

        val contentPackage = contentPackageRepository.findById(command.packageId) ?: allContentPackages.firstOrNull()

        if (contentPackage != null) {
            removalDependencyGuard.ensureCanRemove(
                contentPackage.id
            )
        }

        val packageLibraryIds = mutableSetOf<vn.loi.learning.domain.content.library.model.ContentLibraryId>()

        candidatePkgIds.forEach { candidate ->
            packageLibraryIds.add(vn.loi.learning.domain.content.library.model.ContentLibraryId(candidate.value))
        }

        allContentPackages.forEach { cp ->
            packageLibraryIds.addAll(cp.libraryIds)
            packageLibraryIds.add(vn.loi.learning.domain.content.library.model.ContentLibraryId(cp.id.value))
        }

        matchingInstPkgs.forEach { instPkg ->
            packageLibraryIds.add(vn.loi.learning.domain.content.library.model.ContentLibraryId(instPkg.libraryId.value))
        }

        val sharedLibraryIds = mutableSetOf<vn.loi.learning.domain.content.library.model.ContentLibraryId>()

        contentPackageRepository
            .findAll()
            .asSequence()
            .filter { otherPackage ->
                candidatePkgIds.none { it == otherPackage.id }
            }
            .flatMap { otherPackage ->
                otherPackage.libraryIds.asSequence()
            }
            .forEach { sharedLibraryIds.add(it) }

        val removableLibraryIds =
            packageLibraryIds -
                    sharedLibraryIds

        val allLibraries =
            contentLibraryRepository.findAll()

        val removableLibraries =
            allLibraries.filter { library ->
                library.id in removableLibraryIds ||
                    candidatePkgIds.any { cand -> library.id.value.equals(cand.value, ignoreCase = true) }
            }

        val candidateContentIds =
            removableLibraries
                .asSequence()
                .flatMap { library ->
                    library.contentIds.asSequence()
                }
                .toSet()

        val preservedContentIds =
            allLibraries
                .asSequence()
                .filter { library ->
                    library.id !in removableLibraryIds
                }
                .flatMap { library ->
                    library.contentIds.asSequence()
                }
                .toSet()

        val removableContentIds =
            candidateContentIds -
                    preservedContentIds

        val targetContentIds = candidateContentIds + removableContentIds
        val targetLearningItems = (
            learningItemRepository.findAllEnabled().filter { it.contentId in targetContentIds } +
                learningItemRepository.findByContentIds(targetContentIds)
        ).distinctBy { it.id }
        val targetLearningItemIds = targetLearningItems.map { it.id }.toSet()

        if (targetLearningItemIds.isNotEmpty()) {
            memoryStateRepository?.deleteByLearningItemIds(targetLearningItemIds)
            reviewEventRepository?.deleteByLearningItemIds(targetLearningItemIds)
        }

        val targetTopicIds = (
            matchingInstPkgs.map { it.topicId } +
                listOfNotNull(contentPackage?.topicId) +
                candidatePkgIds.map { TopicId(it.value) }
        ).toSet()

        val candidatePkgIdValues = candidatePkgIds.map { it.value }.toSet()
        val allSessions = studySessionRepository?.findAll().orEmpty()
        val matchingSessions = allSessions.filter { session ->
            session.installedPackageId?.value in candidatePkgIdValues ||
                session.topicId in targetTopicIds ||
                session.includedContentIds.any { it in targetContentIds }
        }
        val matchingSessionIds = matchingSessions.map { it.id }.toSet()

        matchingSessionIds.forEach { sessionId ->
            studyQueueRepository?.deleteBySessionId(sessionId)
            studySessionRepository?.deleteById(sessionId)
        }

        targetTopicIds.forEach { topicId ->
            studySessionRepository?.deleteForTopic(topicId)
        }

        learningItemRepository.deleteByContentIds(
            removableContentIds
        )

        contentRepository.deleteAllById(
            removableContentIds
        )

        contentLibraryRepository.deleteAllById(
            removableLibraryIds
        )

        if (catalog != null && catalog.contains(command.packageId)) {
            val updatedCatalog = catalog.remove(command.packageId)
            packageCatalogRepository.save(updatedCatalog)
        }

        contentPackageRepository.deleteById(
            command.packageId
        )

        if (installedPackageRepository != null) {
            for (matchingInstPkg in matchingInstPkgs) {
                installedPackageRepository.delete(matchingInstPkg.id)

                if (libraryRepository != null) {
                    val lib = libraryRepository.findById(matchingInstPkg.libraryId)
                    if (lib != null && lib.hasPackage(matchingInstPkg.id)) {
                        val updatedLib = lib.unregisterEntry(matchingInstPkg.id)
                        libraryRepository.save(updatedLib)
                    }
                }

                if (collectionRepository != null) {
                    val cols = collectionRepository.findAllByLibraryId(matchingInstPkg.libraryId)
                    for (col in cols) {
                        if (col.containsPackage(matchingInstPkg.id)) {
                            val removeResult = col.removePackage(matchingInstPkg.id)
                            collectionRepository.save(removeResult.aggregate)
                        }
                    }
                }
            }
        }
    }
}