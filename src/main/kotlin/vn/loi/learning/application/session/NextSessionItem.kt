package vn.loi.learning.application.session

import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.study.session.model.StudySession

data class NextSessionItem(
    val session: StudySession,
    val item: NextLearningItem
)