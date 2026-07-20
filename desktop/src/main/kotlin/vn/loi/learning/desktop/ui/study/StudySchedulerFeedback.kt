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