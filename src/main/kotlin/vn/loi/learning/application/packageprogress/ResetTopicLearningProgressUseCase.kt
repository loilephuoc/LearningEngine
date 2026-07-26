package vn.loi.learning.application.packageprogress

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.study.memory.model.LearnerId

class ResetTopicLearningProgressUseCase(
    private val installedPackageRepository: InstalledPackageRepository,
    private val contentLibraryRepository: ContentLibraryRepository? = null,
    private val learningItemRepository: LearningItemRepository,
    private val memoryStateRepository: MemoryStateRepository,
    private val studySessionRepository: StudySessionRepository
) {
    fun execute(learnerId: LearnerId, installedPackageId: InstalledPackageId): Boolean {
        val pkg = installedPackageRepository.findById(installedPackageId) ?: return false
        val topicId = pkg.topicId

        val targetLibraryIds = listOf(
            ContentLibraryId(installedPackageId.value),
            ContentLibraryId(pkg.id.value),
            ContentLibraryId(pkg.packageId.value)
        )

        val targetContentIds = targetLibraryIds
            .mapNotNull { contentLibraryRepository?.findById(it) }
            .flatMap { it.contentIds }
            .toSet()

        val allItems = learningItemRepository.findAllEnabled()
        val targetItems = if (targetContentIds.isNotEmpty()) {
            allItems.filter { it.contentId in targetContentIds }
        } else {
            allItems
        }

        targetItems.forEach { item ->
            memoryStateRepository.delete(learnerId, item.id)
        }

        studySessionRepository.deleteForTopic(learnerId, topicId)
        return true
    }
}
