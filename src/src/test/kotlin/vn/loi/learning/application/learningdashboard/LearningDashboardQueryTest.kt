package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardQueryTest {

    @Test
    fun `query preserves requested dashboard parameters`() {
        val learnerId =
            LearnerId("learner-1")

        val forecastWindowEnds =
            listOf(
                Moment(4_000L),
                Moment(5_000L),
                Moment(6_000L)
            )

        val query =
            LearningDashboardQuery(
                learnerId = learnerId,
                activityFrom = Moment(1_000L),
                activityUntil = Moment(2_000L),
                at = Moment(3_000L),
                forecastWindowEnds =
                    forecastWindowEnds
            )

        assertEquals(
            expected = learnerId,
            actual = query.learnerId
        )

        assertEquals(
            expected = Moment(1_000L),
            actual = query.activityFrom
        )

        assertEquals(
            expected = Moment(2_000L),
            actual = query.activityUntil
        )

        assertEquals(
            expected = Moment(3_000L),
            actual = query.at
        )

        assertSame(
            expected = forecastWindowEnds,
            actual = query.forecastWindowEnds
        )
    }

    @Test
    fun `activity range may represent one exact moment`() {
        LearningDashboardQuery(
            learnerId =
                LearnerId("learner-1"),
            activityFrom =
                Moment(1_000L),
            activityUntil =
                Moment(1_000L),
            at =
                Moment(2_000L),
            forecastWindowEnds =
                emptyList()
        )
    }

    @Test
    fun `activity until must not be before activity from`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityFrom =
                    Moment(2_000L),
                activityUntil =
                    Moment(1_000L),
                at =
                    Moment(3_000L),
                forecastWindowEnds =
                    emptyList()
            )
        }
    }

    @Test
    fun `query may request no forecast buckets`() {
        val query =
            LearningDashboardQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityFrom =
                    Moment(1_000L),
                activityUntil =
                    Moment(2_000L),
                at =
                    Moment(3_000L),
                forecastWindowEnds =
                    emptyList()
            )

        assertEquals(
            expected = emptyList(),
            actual = query.forecastWindowEnds
        )
    }

    @Test
    fun `first forecast window end must be after dashboard time`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityFrom =
                    Moment(1_000L),
                activityUntil =
                    Moment(2_000L),
                at =
                    Moment(3_000L),
                forecastWindowEnds =
                    listOf(
                        Moment(3_000L)
                    )
            )
        }
    }

    @Test
    fun `forecast window ends must be strictly increasing`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityFrom =
                    Moment(1_000L),
                activityUntil =
                    Moment(2_000L),
                at =
                    Moment(3_000L),
                forecastWindowEnds =
                    listOf(
                        Moment(5_000L),
                        Moment(4_000L)
                    )
            )
        }
    }

    @Test
    fun `duplicate forecast window ends are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityFrom =
                    Moment(1_000L),
                activityUntil =
                    Moment(2_000L),
                at =
                    Moment(3_000L),
                forecastWindowEnds =
                    listOf(
                        Moment(4_000L),
                        Moment(4_000L)
                    )
            )
        }
    }
}