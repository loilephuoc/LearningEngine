package vn.loi.learning.domain.study.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsConfiguration
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

class DefaultFsrsAlgorithmConfigurationTest {

    @Test
    fun `algorithm created from default configuration preserves default behavior`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newMemoryState(reviewedAt)

        val traditionalAlgorithm =
            DefaultFsrsAlgorithm()

        val configuredAlgorithm =
            DefaultFsrsAlgorithm(
                configuration =
                    FsrsConfiguration.DEFAULT
            )

        ReviewRating.entries.forEach { rating ->
            val traditionalDecision =
                traditionalAlgorithm.schedule(
                    currentState = currentState,
                    rating = rating,
                    reviewedAt = reviewedAt
                )

            val configuredDecision =
                configuredAlgorithm.schedule(
                    currentState = currentState,
                    rating = rating,
                    reviewedAt = reviewedAt
                )

            assertEquals(
                traditionalDecision,
                configuredDecision
            )
        }
    }

    @Test
    fun `algorithm uses parameters supplied through configuration`() {
        val customValues =
            DoubleArray(
                FsrsParameters.PARAMETER_COUNT
            ) { index ->
                FsrsParameters.DEFAULT[index]
            }

        customValues[2] =
            CUSTOM_GOOD_STABILITY

        val configuration =
            FsrsConfiguration(
                parameters =
                    FsrsParameters.of(customValues),
                desiredRetention =
                    DesiredRetention.DEFAULT
            )

        val algorithm =
            DefaultFsrsAlgorithm(
                configuration = configuration
            )

        val reviewedAt =
            Moment(2_000_000L)

        val decision =
            algorithm.schedule(
                currentState =
                    newMemoryState(reviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )

        assertEquals(
            CUSTOM_GOOD_STABILITY,
            decision.nextState.stabilityDays,
            DOUBLE_TOLERANCE
        )
    }

    @Test
    fun `algorithm uses desired retention supplied through configuration`() {
        val reviewedAt =
            Moment(3_000_000L)

        val currentState =
            newMemoryState(reviewedAt)

        val lowerRetentionAlgorithm =
            DefaultFsrsAlgorithm(
                configuration =
                    FsrsConfiguration(
                        parameters =
                            FsrsParameters.DEFAULT,
                        desiredRetention =
                            DesiredRetention(0.80)
                    )
            )

        val higherRetentionAlgorithm =
            DefaultFsrsAlgorithm(
                configuration =
                    FsrsConfiguration(
                        parameters =
                            FsrsParameters.DEFAULT,
                        desiredRetention =
                            DesiredRetention(0.95)
                    )
            )

        val lowerRetentionDecision =
            lowerRetentionAlgorithm.schedule(
                currentState = currentState,
                rating = ReviewRating.EASY,
                reviewedAt = reviewedAt
            )

        val higherRetentionDecision =
            higherRetentionAlgorithm.schedule(
                currentState = currentState,
                rating = ReviewRating.EASY,
                reviewedAt = reviewedAt
            )

        /*
         * Desired retention cao hơn phải tạo interval
         * ngắn hơn hoặc bằng.
         */
        kotlin.test.assertTrue(
            higherRetentionDecision.scheduledInterval <=
                    lowerRetentionDecision.scheduledInterval
        )
    }

    private fun newMemoryState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId =
                LearnerId(
                    "learner-configuration"
                ),
            learningItemId =
                LearningItemId(
                    "item-configuration"
                ),
            availableAt = availableAt
        )

    private companion object {

        const val CUSTOM_GOOD_STABILITY =
            4.5

        const val DOUBLE_TOLERANCE =
            1e-12
    }
}