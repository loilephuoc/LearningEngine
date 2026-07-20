package vn.loi.learning.application.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

class LearningProgressQueryTest {

    @Test
    fun `query keeps learner period evaluation moment and daily periods`() {
        val learnerId =
            LearnerId("learner-1")

        val activityPeriod =
            period(
                start = 1_000L,
                end = 2_000L
            )

        val dailyPeriods =
            listOf(
                period(
                    start = 1_000L,
                    end = 1_500L
                ),
                period(
                    start = 1_500L,
                    end = 2_000L
                )
            )

        val evaluatedAt =
            Moment(2_000L)

        val query =
            LearningProgressQuery(
                learnerId = learnerId,
                activityPeriod = activityPeriod,
                evaluatedAt = evaluatedAt,
                dailyPeriods = dailyPeriods
            )

        assertEquals(
            expected = learnerId,
            actual = query.learnerId
        )

        assertEquals(
            expected = activityPeriod,
            actual = query.activityPeriod
        )

        assertEquals(
            expected = evaluatedAt,
            actual = query.evaluatedAt
        )

        assertEquals(
            expected = dailyPeriods,
            actual = query.dailyPeriods
        )
    }

    @Test
    fun `evaluation moment may equal period start`() {
        val activityPeriod =
            period(
                start = 1_000L,
                end = 2_000L
            )

        val query =
            LearningProgressQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityPeriod = activityPeriod,
                evaluatedAt =
                    activityPeriod.startInclusive
            )

        assertEquals(
            expected =
                activityPeriod.startInclusive,
            actual = query.evaluatedAt
        )
    }

    @Test
    fun `evaluation moment may be after activity period`() {
        val query =
            LearningProgressQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityPeriod =
                    period(
                        start = 1_000L,
                        end = 2_000L
                    ),
                evaluatedAt =
                    Moment(3_000L)
            )

        assertEquals(
            expected = Moment(3_000L),
            actual = query.evaluatedAt
        )
    }

    @Test
    fun `daily periods may be empty`() {
        val query =
            LearningProgressQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityPeriod =
                    period(
                        start = 1_000L,
                        end = 2_000L
                    ),
                evaluatedAt =
                    Moment(2_000L),
                dailyPeriods = emptyList()
            )

        assertTrue(
            actual = query.dailyPeriods.isEmpty()
        )
    }

    @Test
    fun `daily periods may cover part of activity period`() {
        val dailyPeriods =
            listOf(
                period(
                    start = 1_200L,
                    end = 1_400L
                ),
                period(
                    start = 1_400L,
                    end = 1_600L
                )
            )

        val query =
            LearningProgressQuery(
                learnerId =
                    LearnerId("learner-1"),
                activityPeriod =
                    period(
                        start = 1_000L,
                        end = 2_000L
                    ),
                evaluatedAt =
                    Moment(2_000L),
                dailyPeriods = dailyPeriods
            )

        assertEquals(
            expected = dailyPeriods,
            actual = query.dailyPeriods
        )
    }

    @Test
    fun `query rejects evaluation moment before activity period`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressQuery(
                    learnerId =
                        LearnerId("learner-1"),
                    activityPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    evaluatedAt =
                        Moment(999L)
                )
            }

        assertEquals(
            expected =
                "Evaluation moment must not be before activity period.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects daily period before activity period`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressQuery(
                    learnerId =
                        LearnerId("learner-1"),
                    activityPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    evaluatedAt =
                        Moment(2_000L),
                    dailyPeriods =
                        listOf(
                            period(
                                start = 900L,
                                end = 1_100L
                            )
                        )
                )
            }

        assertEquals(
            expected =
                "Every daily period must be contained in activity period.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects daily period after activity period`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressQuery(
                    learnerId =
                        LearnerId("learner-1"),
                    activityPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    evaluatedAt =
                        Moment(2_000L),
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_900L,
                                end = 2_100L
                            )
                        )
                )
            }

        assertEquals(
            expected =
                "Every daily period must be contained in activity period.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects daily periods with gap`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressQuery(
                    learnerId =
                        LearnerId("learner-1"),
                    activityPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    evaluatedAt =
                        Moment(2_000L),
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_000L,
                                end = 1_200L
                            ),
                            period(
                                start = 1_300L,
                                end = 1_500L
                            )
                        )
                )
            }

        assertEquals(
            expected =
                "Daily periods must be ordered and contiguous.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects overlapping daily periods`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressQuery(
                    learnerId =
                        LearnerId("learner-1"),
                    activityPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    evaluatedAt =
                        Moment(2_000L),
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_000L,
                                end = 1_300L
                            ),
                            period(
                                start = 1_200L,
                                end = 1_500L
                            )
                        )
                )
            }

        assertEquals(
            expected =
                "Daily periods must be ordered and contiguous.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects reversed daily periods`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressQuery(
                    learnerId =
                        LearnerId("learner-1"),
                    activityPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    evaluatedAt =
                        Moment(2_000L),
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_400L,
                                end = 1_600L
                            ),
                            period(
                                start = 1_200L,
                                end = 1_400L
                            )
                        )
                )
            }

        assertEquals(
            expected =
                "Daily periods must be ordered and contiguous.",
            actual = exception.message
        )
    }

    private fun period(
        start: Long,
        end: Long
    ): StudyPeriod =
        StudyPeriod(
            startInclusive = Moment(start),
            endExclusive = Moment(end)
        )
}