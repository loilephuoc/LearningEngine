package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId

class OrphanPackageLearningStateReconciler(
    private val installedPackageRepository: InstalledPackageRepository?,
    private val contentPackageRepository: ContentPackageRepository,
    private val memoryStateRepository: MemoryStateRepository?,
    private val reviewEventRepository: ReviewEventRepository?,
    private val studySessionRepository: StudySessionRepository?,
    private val studyQueueRepository: StudyQueueRepository?
) {

    fun reconcileIfRepairing(
        candidatePackage: ContentPackage,
        importedContent: ImportedPackageContent
    ) {
        val installedPackages = installedPackageRepository?.findAll() ?: return
        val candidateAliases = setOf(
            candidatePackage.id.value,
            candidatePackage.name,
            candidatePackage.topicId.value
        )
        val hasLiveOwner = installedPackages.any { installedPackage ->
            installedPackage.state in setOf(PackageState.ACTIVE, PackageState.ARCHIVED) &&
                candidateAliases.any { alias ->
                    alias == installedPackage.packageId.value ||
                        alias == installedPackage.id.value ||
                        alias == installedPackage.name.value ||
                        alias == installedPackage.topicId.value
                }
        }
        if (hasLiveOwner) return

        val orphanPackage = contentPackageRepository.findById(candidatePackage.id) ?: return
        val exactLearningItemIds = importedContent.learningItems.mapTo(HashSet()) { it.id }
        if (exactLearningItemIds.isEmpty()) return

        memoryStateRepository?.deleteByLearningItemIds(exactLearningItemIds)
        reviewEventRepository?.deleteByLearningItemIds(exactLearningItemIds)

        val deterministicInstalledPackageId = InstalledPackageId("inst-${candidatePackage.id.value}")
        studySessionRepository?.findAll()
            .orEmpty()
            .filter { session ->
                session.installedPackageId == deterministicInstalledPackageId ||
                    (
                        session.topicId == orphanPackage.topicId &&
                            sessionOwnsAnyLearningItem(session.id, exactLearningItemIds)
                        )
            }
            .forEach { session ->
                studyQueueRepository?.deleteBySessionId(session.id)
                studySessionRepository?.deleteById(session.id)
            }
    }

    private fun sessionOwnsAnyLearningItem(
        sessionId: vn.loi.learning.domain.study.session.model.SessionId,
        ownedLearningItemIds: Set<LearningItemId>
    ): Boolean {
        val queue = studyQueueRepository?.findBySessionId(sessionId) ?: return false
        return queue.completedLearningItemIds.any { it in ownedLearningItemIds } ||
            queue.remainingLearningItemIds.any { it in ownedLearningItemIds }
    }
}
