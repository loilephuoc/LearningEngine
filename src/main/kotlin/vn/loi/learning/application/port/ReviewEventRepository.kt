package vn.loi.learning.application.port

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent

interface ReviewEventRepository {

    fun append(event: ReviewEvent)

    fun findAll(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent>
}