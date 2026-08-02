package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vn.loi.learning.application.session.PracticeProgress
import vn.loi.learning.application.session.RatingInventory
import vn.loi.learning.application.session.ManualRatingOverrideAvailability
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import kotlin.test.assertNull

class PracticeSessionPresentationTest {
    @Test
    fun `practice progress renders current round without cumulative denominator`() {
        val state = StudyUiState(
            hasActiveSession = true,
            practiceProgress = PracticeProgress(round = 2, position = 1, membershipSize = 20)
        )

        assertEquals("Vòng 2 — 1 / 20", state.progressLabel)
    }

    @Test
    fun `chooser passes through shared rating inventory without recounting`() {
        val inventory = RatingInventory(1, 2, 3, 4, 5, 15)
        val presentation = resolveStudyIdlePresentation(
            StudyUiState(
                activeInstalledPackageId = InstalledPackageId("package"),
                ratingInventory = inventory
            )
        )

        assertEquals(inventory, assertNotNull(presentation).ratingInventory)
    }

    @Test
    fun `practice identity always exposes labeled override with typed availability`() {
        val disabled = PracticeSessionIdentityPresentationResolver.resolve(
            SessionEvaluationPolicy.PRACTICE_ONLY,
            ManualRatingOverrideAvailability.NO_COMMITTED_RATING
        )!!
        assertEquals("LUYỆN TẬP", disabled.badgeLabel)
        assertEquals("Đổi đánh giá", disabled.overrideLabel)
        assertEquals(false, disabled.overrideEnabled)
        assertEquals("Từ này chưa có đánh giá để thay đổi.", disabled.overrideSupport)

        assertNull(
            PracticeSessionIdentityPresentationResolver.resolve(
                SessionEvaluationPolicy.EVALUATIVE,
                ManualRatingOverrideAvailability.NOT_PRACTICE
            )
        )
    }
}
