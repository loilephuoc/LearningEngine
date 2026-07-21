package vn.loi.learning.application.study

/**
 * Các stage được kiểm tra invariant trong
 * QueueTransformationPipeline.
 */
enum class QueueTransformationStage {

    STRATEGY,

    INITIAL_DIVERSITY,

    BALANCER,

    FINAL_DIVERSITY
}