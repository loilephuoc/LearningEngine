package vn.loi.learning.application.learninginsight

data class LearningInsightPresentationPolicy(
    val maximumSecondaryInsights: Int = 2,
    val maximumPrimaryMetrics: Int = 3,
    val maximumSecondaryMetrics: Int = 2
) {
    init {
        require(maximumSecondaryInsights >= 0)
        require(maximumPrimaryMetrics > 0)
        require(maximumSecondaryMetrics > 0)
    }
}
