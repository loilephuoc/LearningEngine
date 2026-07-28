package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.session.model.SessionPolicy

internal data class StudySessionGoalFingerprint(
    val newItemLimit: Int,
    val reviewItemLimit: Int
)

internal fun SessionPolicy.goalFingerprint(): StudySessionGoalFingerprint =
    StudySessionGoalFingerprint(
        newItemLimit = newItemLimit,
        reviewItemLimit = reviewItemLimit
    )
