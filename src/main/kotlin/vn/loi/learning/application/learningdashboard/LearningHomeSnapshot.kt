package vn.loi.learning.application.learningdashboard

/** Lightweight snapshot for the eager Desktop Home destination. */
data class LearningHomeSnapshot(
    val activity: LearningDashboardActivitySnapshot,
    val memory: LearningDashboardMemorySnapshot,
    val scheduling: LearningDashboardSchedulingSnapshot,
    val retention: LearningDashboardRetentionSnapshot
)
