package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.packageprogress.PackageLearningProgress

sealed interface PackageProgressPresentation {
    data class Available(
        val totalLearningItemCount: Int,
        val unseenItemCount: Int,
        val newStateItemCount: Int,
        val startedItemCount: Int,
        val masteredItemCount: Int,
        val dueItemCount: Int,
        val suspendedItemCount: Int,
        val completionPercent: Int,
        val startedPercent: Int
    ) : PackageProgressPresentation

    data object Unavailable : PackageProgressPresentation
}

internal fun PackageLearningProgress.toPresentation(): PackageProgressPresentation.Available =
    PackageProgressPresentation.Available(
        totalLearningItemCount = totalLearningItemCount,
        unseenItemCount = unseenItemCount,
        newStateItemCount = newStateItemCount,
        startedItemCount = startedItemCount,
        masteredItemCount = masteredItemCount,
        dueItemCount = dueItemCount,
        suspendedItemCount = suspendedItemCount,
        completionPercent = completionPercent,
        startedPercent = startedPercent
    )
