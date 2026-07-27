package vn.loi.learning.application.packageprogress

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.learning.model.LearningItemId

/**
 * Service ứng dụng có trách nhiệm tính toán tiến độ học tập của một [InstalledPackageId]
 * cho một [LearnerId] tại thời điểm [Moment].
 *
 * Tính toán động từ Source of Truth domain (MemoryState và LearningItem enabled).
 * Không lưu trữ, cache lâu dài hay tạo progress record.
 */
class PackageLearningProgressQueryService(
    private val packageContentQuery: InstalledPackageContentQueryService,
    private val engine: LearningEngine,
    private val memoryStateQuery: MemoryStateQuery
) {

    fun execute(query: PackageLearningProgressQuery): PackageLearningProgress {
        val memoryStates = memoryStateQuery.findAll(query.learnerId).associateBy { it.learningItemId }
        return project(query.installedPackageId, query.at, memoryStates)
    }

    fun executeAll(
        installedPackageIds: Collection<InstalledPackageId>,
        learnerId: LearnerId,
        at: Moment
    ): Map<InstalledPackageId, Result<PackageLearningProgress>> {
        if (installedPackageIds.isEmpty()) return emptyMap()
        val memoryStates = memoryStateQuery.findAll(learnerId).associateBy { it.learningItemId }
        return installedPackageIds.distinct().associateWith { installedPackageId ->
            runCatching { project(installedPackageId, at, memoryStates) }
        }
    }

    private fun project(
        installedPackageId: InstalledPackageId,
        at: Moment,
        allLearnerMemoryStates: Map<LearningItemId, MemoryState>
    ): PackageLearningProgress {
        val contents = packageContentQuery.getContentsForPackage(installedPackageId)
        if (contents.isEmpty()) {
            return PackageLearningProgress.empty(installedPackageId)
        }

        val contentIdSet = contents.mapTo(hashSetOf()) { ContentId(it.id) }
        val learningItemsByContentId = engine.getLearningItemsByContentIds(contentIdSet)
            .groupBy { it.contentId }

        val lessonProgresses = contents.map { contentItem ->
            val contentId = ContentId(contentItem.id)
            val allItems = learningItemsByContentId[contentId] ?: emptyList()
            val enabledItems = allItems.filter { it.isEnabled }

            var unseenCount = 0
            var newStateCount = 0
            var startedCount = 0
            var masteredCount = 0
            var dueCount = 0
            var suspendedCount = 0

            for (item in enabledItems) {
                val memoryState = allLearnerMemoryStates[item.id]
                if (memoryState == null) {
                    unseenCount++
                } else {
                    when (memoryState.stage) {
                        LearningStage.NEW -> newStateCount++
                        LearningStage.SUSPENDED -> suspendedCount++
                        LearningStage.LEARNING,
                        LearningStage.REVIEW,
                        LearningStage.RELEARNING,
                        LearningStage.MASTERED -> {
                            startedCount++
                            if (memoryState.stage == LearningStage.MASTERED) {
                                masteredCount++
                            }
                        }
                    }

                    if (memoryState.isDue(at)) {
                        dueCount++
                    }
                }
            }

            val totalCount = enabledItems.size
            val completionPercent = if (totalCount == 0) 0 else (masteredCount * 100 / totalCount).coerceIn(0, 100)
            val startedPercent = if (totalCount == 0) 0 else (startedCount * 100 / totalCount).coerceIn(0, 100)

            LessonLearningProgress(
                contentId = contentId,
                title = contentItem.title,
                group = contentItem.group,
                section = contentItem.section,
                lesson = contentItem.lesson,
                totalLearningItemCount = totalCount,
                unseenItemCount = unseenCount,
                newStateItemCount = newStateCount,
                startedItemCount = startedCount,
                masteredItemCount = masteredCount,
                dueItemCount = dueCount,
                suspendedItemCount = suspendedCount,
                completionPercent = completionPercent,
                startedPercent = startedPercent
            )
        }

        val totalLessonCount = lessonProgresses.size
        val totalItemCount = lessonProgresses.sumOf { it.totalLearningItemCount }
        val unseenCount = lessonProgresses.sumOf { it.unseenItemCount }
        val newStateCount = lessonProgresses.sumOf { it.newStateItemCount }
        val startedCount = lessonProgresses.sumOf { it.startedItemCount }
        val masteredCount = lessonProgresses.sumOf { it.masteredItemCount }
        val dueCount = lessonProgresses.sumOf { it.dueItemCount }
        val suspendedCount = lessonProgresses.sumOf { it.suspendedItemCount }

        val pkgCompletionPercent = if (totalItemCount == 0) 0 else (masteredCount * 100 / totalItemCount).coerceIn(0, 100)
        val pkgStartedPercent = if (totalItemCount == 0) 0 else (startedCount * 100 / totalItemCount).coerceIn(0, 100)

        return PackageLearningProgress(
            installedPackageId = installedPackageId,
            totalLessonCount = totalLessonCount,
            totalLearningItemCount = totalItemCount,
            unseenItemCount = unseenCount,
            newStateItemCount = newStateCount,
            startedItemCount = startedCount,
            masteredItemCount = masteredCount,
            dueItemCount = dueCount,
            suspendedItemCount = suspendedCount,
            completionPercent = pkgCompletionPercent,
            startedPercent = pkgStartedPercent,
            lessons = lessonProgresses
        )
    }
}
