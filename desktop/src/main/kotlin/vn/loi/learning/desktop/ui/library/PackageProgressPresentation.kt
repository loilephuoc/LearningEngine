package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.packageprogress.PackageLearningProgress
import vn.loi.learning.application.packageprogress.PackageLatestRatingDistribution
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        val startedPercent: Int,
        val latestRatings: PackageLatestRatingPresentation? = null
    ) : PackageProgressPresentation {
        val learningItemCount: Int
            get() = (startedItemCount - masteredItemCount).coerceAtLeast(0)

        val hasSuspendedItems: Boolean
            get() = suspendedItemCount > 0

        val startedRatio: Double
            get() = if (totalLearningItemCount == 0) {
                0.0
            } else {
                startedItemCount.toDouble() / totalLearningItemCount
            }
    }

    data object Unavailable : PackageProgressPresentation
}

data class PackageLatestRatingPresentation(
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int
) {
    val ratedItemCount = againCount + hardCount + goodCount + easyCount
}

internal fun PackageLatestRatingDistribution.toPresentation() =
    PackageLatestRatingPresentation(againCount, hardCount, goodCount, easyCount)

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

internal val packageMetricLabels = listOf(
    "Tổng từ",
    "Chưa học",
    "Đang học",
    "Cần ôn hôm nay",
    "Đã học",
    "Đã thành thạo"
)

internal data class PackageMetricPresentation(val label: String, val value: Int)

internal fun packageMetrics(progress: PackageProgressPresentation.Available): List<PackageMetricPresentation> =
    listOf(
        progress.totalLearningItemCount,
        progress.unseenItemCount,
        progress.learningItemCount,
        progress.dueItemCount,
        progress.startedItemCount,
        progress.masteredItemCount
    ).mapIndexed { index, value -> PackageMetricPresentation(packageMetricLabels[index], value) }

internal fun suspendedProgressLabel(progress: PackageProgressPresentation.Available): String? =
    progress.suspendedItemCount.takeIf { it > 0 }?.let {
        "Tạm ngưng ${formatVietnameseCount(it)}"
    }

internal const val PACKAGE_RATING_UNAVAILABLE_LABEL = "Chưa có dữ liệu đánh giá"

internal val PACKAGE_METRIC_LABEL_FONT_SIZE = 13.sp
internal val PACKAGE_METRIC_VALUE_FONT_SIZE = 23.sp
internal val PACKAGE_METRIC_ROW_HEIGHT = 88.dp
internal val PACKAGE_PROGRESS_BAR_HEIGHT = 10.dp

internal fun formatVietnameseCount(value: Int): String =
    java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN"))
        .format(value)

internal fun formatVietnameseProgress(completed: Int, total: Int): String {
    if (total <= 0 || completed <= 0) return "0%"
    if (completed >= total) return "100%"
    val percent = completed.toDouble() * 100.0 / total
    val pattern = if (percent < 10.0) "0.0" else "0.#"
    return java.text.DecimalFormat(
        pattern,
        java.text.DecimalFormatSymbols(java.util.Locale.forLanguageTag("vi-VN"))
    ).format(percent) + "%"
}
