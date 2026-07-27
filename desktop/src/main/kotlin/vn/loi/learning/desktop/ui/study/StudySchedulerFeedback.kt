package vn.loi.learning.desktop.ui.study

data class StudySchedulerFeedback(
    val rating: String,
    val stageTransition: String,
    val scheduledInterval: String,
    val nextReviewAt: String,
    val difficultyBefore: String,
    val difficultyAfter: String,
    val stabilityBefore: String,
    val stabilityAfter: String,
    val reviewCount: Int,
    val lapseCount: Int
)

internal fun formatVietnameseReviewInterval(millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(1L)
    if (minutes < 60L) return "$minutes phút"
    val hours = millis / 3_600_000L
    if (hours < 24L) return "$hours giờ"
    val days = millis / 86_400_000L
    if (days < 7L) return if (days == 1L) "1 ngày" else "$days ngày"
    val weeks = days / 7L
    return if (weeks == 1L) "1 tuần" else "$weeks tuần"
}
