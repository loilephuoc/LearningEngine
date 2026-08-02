package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionPolicy

class ManualRatingOverrideAvailabilityTest {
    private val practice = SessionPolicy(
        evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
        practiceLoopPolicy = PracticeLoopPolicy.LOOP_FIXED_MEMBERSHIP_SHUFFLED
    )

    @Test
    fun `practice requires a committed rating before override is available`() {
        assertEquals(
            ManualRatingOverrideAvailability.NO_COMMITTED_RATING,
            ManualRatingOverrideAvailabilityResolver.resolve(practice, null)
        )
        assertEquals(
            ManualRatingOverrideAvailability.AVAILABLE,
            ManualRatingOverrideAvailabilityResolver.resolve(practice, ReviewRating.GOOD)
        )
        assertEquals(
            ManualRatingOverrideAvailability.NOT_PRACTICE,
            ManualRatingOverrideAvailabilityResolver.resolve(SessionPolicy(), ReviewRating.GOOD)
        )
    }
}
