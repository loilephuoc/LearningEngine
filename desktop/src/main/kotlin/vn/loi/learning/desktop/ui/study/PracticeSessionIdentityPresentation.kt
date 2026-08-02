package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.session.ManualRatingOverrideAvailability
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy

internal data class PracticeSessionIdentityPresentation(
    val badgeLabel: String,
    val overrideLabel: String,
    val overrideEnabled: Boolean,
    val overrideSupport: String
)

internal object PracticeSessionIdentityPresentationResolver {
    fun resolve(
        policy: SessionEvaluationPolicy,
        availability: ManualRatingOverrideAvailability
    ): PracticeSessionIdentityPresentation? =
        if (policy != SessionEvaluationPolicy.PRACTICE_ONLY) null
        else PracticeSessionIdentityPresentation(
            badgeLabel = "LUYỆN TẬP",
            overrideLabel = "Đổi đánh giá",
            overrideEnabled = availability == ManualRatingOverrideAvailability.AVAILABLE,
            overrideSupport =
                if (availability == ManualRatingOverrideAvailability.AVAILABLE) "Đổi đánh giá"
                else "Từ này chưa có đánh giá để thay đổi."
        )
}
