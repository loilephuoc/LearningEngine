package vn.loi.learning.application.session

import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

data class NextSessionItem(
    val session: StudySession,
    val item: NextLearningItem,
    val progress: LearningSessionProgress? = null,
    val origin: SessionItemOrigin =
        if (item.isNew) SessionItemOrigin.NEW else SessionItemOrigin.REVIEW
)
