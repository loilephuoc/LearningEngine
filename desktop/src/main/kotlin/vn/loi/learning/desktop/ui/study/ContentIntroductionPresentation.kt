package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

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
