package vn.loi.learning.application.study

import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Yêu cầu tìm LearningItem tiếp theo cho một người học
 * tại một thời điểm xác định.
 */
data class GetNextLearningItemQuery(
    val learnerId: LearnerId,
    val now: Moment
)