package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.application.learningexperience.LearningExperienceKind

internal fun resolveContentIntroductionState(
    origin: SessionItemOrigin,
    contentId: ContentId,
    introducedContentIds: Set<ContentId>
): ContentIntroductionState =
    when {
        origin != SessionItemOrigin.NEW -> ContentIntroductionState.NOT_APPLICABLE
        contentId in introducedContentIds -> ContentIntroductionState.COMPLETED
        else -> ContentIntroductionState.REQUIRED
    }

internal fun shouldRevealAnswerAfterIntroduction(
    primaryKind: LearningExperienceKind?,
    experienceCount: Int
): Boolean =
    primaryKind != LearningExperienceKind.TYPING_RECALL && experienceCount == 1
