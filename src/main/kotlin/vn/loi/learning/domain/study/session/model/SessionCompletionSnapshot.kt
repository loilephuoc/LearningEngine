package vn.loi.learning.domain.study.session.model

data class SessionCompletionSnapshot(
    val whatWasLearned: String,
    val overallOutcome: String,
    val reflection: String,
    val reinforcement: String,
    val whatHappensNext: String,
    val schedulingGuidance: String,
    val scheduledIntervalMillis: Long,
    val nextReviewAtEpochMillis: Long
) {
    init {
        require(whatWasLearned.isNotBlank())
        require(overallOutcome.isNotBlank())
        require(reflection.isNotBlank())
        require(reinforcement.isNotBlank())
        require(whatHappensNext.isNotBlank())
        require(schedulingGuidance.isNotBlank())
        require(scheduledIntervalMillis >= 0L)
        require(nextReviewAtEpochMillis >= 0L)
    }
}
