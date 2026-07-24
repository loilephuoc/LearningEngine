package vn.loi.learning.application.packageprogress

import vn.loi.learning.domain.library.model.InstalledPackageId

data class PackageLearningProgress(
    val installedPackageId: InstalledPackageId,
    val totalLessonCount: Int,
    val totalLearningItemCount: Int,
    val unseenItemCount: Int,
    val newStateItemCount: Int,
    val startedItemCount: Int,
    val masteredItemCount: Int,
    val dueItemCount: Int,
    val suspendedItemCount: Int,
    val completionPercent: Int,
    val startedPercent: Int,
    val lessons: List<LessonLearningProgress>
) {
    companion object {
        fun empty(installedPackageId: InstalledPackageId): PackageLearningProgress =
            PackageLearningProgress(
                installedPackageId = installedPackageId,
                totalLessonCount = 0,
                totalLearningItemCount = 0,
                unseenItemCount = 0,
                newStateItemCount = 0,
                startedItemCount = 0,
                masteredItemCount = 0,
                dueItemCount = 0,
                suspendedItemCount = 0,
                completionPercent = 0,
                startedPercent = 0,
                lessons = emptyList()
            )
    }
}
