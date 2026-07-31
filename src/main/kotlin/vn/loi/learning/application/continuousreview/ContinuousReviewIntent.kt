package vn.loi.learning.application.continuousreview

import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

data class ContinuousReviewIntent(
    val learnerId: LearnerId,
    val installedPackageId: InstalledPackageId,
    val topicId: TopicId?,
    val enabled: Boolean,
    val updatedAt: Moment,
    val lastNoWorkPredecessorId: SessionId? = null
)
