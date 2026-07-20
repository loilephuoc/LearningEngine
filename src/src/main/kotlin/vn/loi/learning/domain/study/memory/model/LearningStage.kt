package vn.loi.learning.domain.study.memory.model

/**
 * Giai đoạn ghi nhớ hiện tại của một Learner đối với một LearningItem.
 *
 * Stage thuộc MemoryState, không thuộc LearningItem,
 * vì hai người có thể ở hai giai đoạn khác nhau với cùng một item.
 */
enum class LearningStage {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING,
    MASTERED,
    SUSPENDED
}