package vn.loi.learning.application.packageprogress

import vn.loi.learning.domain.content.model.ContentId

data class LessonLearningProgress(
    val contentId: ContentId,
    val title: String,
    val group: String?,
    val section: String?,
    val lesson: String?,
    val totalLearningItemCount: Int,
    val unseenItemCount: Int,
    val newStateItemCount: Int,
    val startedItemCount: Int,
    val masteredItemCount: Int,
    val dueItemCount: Int,
    val suspendedItemCount: Int,
    val completionPercent: Int,
    val startedPercent: Int
)
