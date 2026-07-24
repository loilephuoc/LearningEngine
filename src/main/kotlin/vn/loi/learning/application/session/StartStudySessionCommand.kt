package vn.loi.learning.application.session

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy

data class StartStudySessionCommand(
    val sessionId: SessionId,
    val learnerId: LearnerId,
    val startedAt: Moment,
    val policy: SessionPolicy = SessionPolicy(),
    val includedContentIds: Set<ContentId> = emptySet(),
    val topicId: TopicId? = null,
    val installedPackageId: InstalledPackageId? = null
)
