package vn.loi.learning.application.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

class DashboardQueryTest {

    @Test
    fun `query describes dashboard periods for one learner`() {
        val learnerId =
            LearnerId("learner-1")

        val todayPeriod =
            period(
                start = 1_700L,
                end = 1_900L
            )

        val currentWeekPeriod =
            period(
                start = 1_400L,
                end = 1_900L
            )

        val currentMonthPeriod =
            period(
                start = 1_000L,
                end = 2_000L
            )

        val dailyPeriods =
            listOf(
                period(
                    start = 1_000L,
                    end = 1_200L
                ),
                period(
                    start = 1_200L,
                    end = 1_400L
                ),
                period(
                    start = 1_400L,
                    end = 1_700L
                ),
                todayPeriod
            )

        val query =
            DashboardQuery(
                learnerId = learnerId,
                todayPeriod = todayPeriod,
                currentWeekPeriod = currentWeekPeriod,
                currentMonthPeriod = currentMonthPeriod,
                dailyPeriods = dailyPeriods
            )

        assertEquals(
            expected = learnerId,
            actual = query.learnerId
        )

        assertEquals(
            expected = todayPeriod,
            actual = query.todayPeriod
        )

        assertEquals(
            expected = currentWeekPeriod,
            actual = query.currentWeekPeriod
        )

        assertEquals(
            expected = currentMonthPeriod,
            actual = query.currentMonthPeriod
        )

        assertEquals(
            expected = dailyPeriods,
            actual = query.dailyPeriods
        )
    }

    @Test
    fun `query allows empty daily periods`() {
        val query =
            validQuery(
                dailyPeriods = emptyList()
            )

        assertEquals(
            expected = emptyList(),
            actual = query.dailyPeriods
        )
    }

    @Test
    fun `query rejects current week period that does not contain today period`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                DashboardQuery(
                    learnerId = LearnerId("learner-1"),
                    todayPeriod =
                        period(
                            start = 1_700L,
                            end = 1_900L
                        ),
                    currentWeekPeriod =
                        period(
                            start = 1_400L,
                            end = 1_800L
                        ),
                    currentMonthPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        )
                )
            }

        assertEquals(
            expected =
                "Current week period must contain today period.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects current month period that does not contain today period`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                DashboardQuery(
                    learnerId = LearnerId("learner-1"),
                    todayPeriod =
                        period(
                            start = 1_700L,
                            end = 1_900L
                        ),
                    currentWeekPeriod =
                        period(
                            start = 1_400L,
                            end = 1_900L
                        ),
                    currentMonthPeriod =
                        period(
                            start = 1_800L,
                            end = 2_000L
                        )
                )
            }

        assertEquals(
            expected =
                "Current month period must contain today period.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects daily period before current month`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                validQuery(
                    dailyPeriods =
                        listOf(
                            period(
                                start = 900L,
                                end = 1_700L
                            ),
                            todayPeriod()
                        )
                )
            }

        assertEquals(
            expected =
                "Every daily period must be contained in current month period.",
            actual = exception.message
        )
    }

    @Test
    fun `query rejects daily period after current month`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                validQuery(
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_000L,
                                end = 1_700L
                            ),
                            period(
                                start = 1_700L,
                                end = 2_100L
                            )
                        )
                )
            }

        assertEquals(
            expected =
                "Every daily period must be contained in current month period.",
            actual = exception.message
        )
    }

    @Test
    fun `query accepts contiguous daily periods ending with today`() {
        val dailyPeriods =
            listOf(
                period(
                    start = 1_000L,
                    end = 1_200L
                ),
                period(
                    start = 1_200L,
                    end = 1_400L
                ),
                period(
                    start = 1_400L,
                    end = 1_700L
                ),
                todayPeriod()
            )

        val query =
            validQuery(
                dailyPeriods = dailyPeriods
            )

        assertEquals(
            expected = dailyPeriods,
            actual = query.dailyPeriods
        )
    }

    @Test
    fun `query accepts only today as daily period`() {
        val dailyPeriods =
            listOf(todayPeriod())

        val query =
            validQuery(
                dailyPeriods = dailyPeriods
            )

        assertEquals(
            expected = dailyPeriods,
            actual = query.dailyPeriods
        )
    }

    @Test
    fun `query rejects gaps between daily periods`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                validQuery(
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_000L,
                                end = 1_200L
                            ),
                            period(
                                start = 1_300L,
                                end = 1_700L
                            ),
                            todayPeriod()
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
                validQuery(
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_000L,
                                end = 1_500L
                            ),
                            period(
                                start = 1_400L,
                                end = 1_700L
                            ),
                            todayPeriod()
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
    fun `query rejects duplicate daily periods`() {
        val duplicatedPeriod =
            period(
                start = 1_000L,
                end = 1_700L
            )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                validQuery(
                    dailyPeriods =
                        listOf(
                            duplicatedPeriod,
                            duplicatedPeriod,
                            todayPeriod()
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
    fun `query rejects daily periods in descending order`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                validQuery(
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_400L,
                                end = 1_700L
                            ),
                            period(
                                start = 1_000L,
                                end = 1_400L
                            ),
                            todayPeriod()
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
    fun `query rejects daily periods that do not end with today`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                validQuery(
                    dailyPeriods =
                        listOf(
                            period(
                                start = 1_000L,
                                end = 1_300L
                            ),
                            period(
                                start = 1_300L,
                                end = 1_600L
                            )
                        )
                )
            }

        assertEquals(
            expected =
                "Daily periods must end with today period.",
            actual = exception.message
        )
    }

    @Test
    fun `today period may equal current week and current month periods`() {
        val period =
            period(
                start = 1_000L,
                end = 2_000L
            )

        val query =
            DashboardQuery(
                learnerId = LearnerId("learner-1"),
                todayPeriod = period,
                currentWeekPeriod = period,
                currentMonthPeriod = period,
                dailyPeriods = listOf(period)
            )

        assertEquals(
            expected = period,
            actual = query.todayPeriod
        )

        assertEquals(
            expected = period,
            actual = query.currentWeekPeriod
        )

        assertEquals(
            expected = period,
            actual = query.currentMonthPeriod
        )

        assertEquals(
            expected = listOf(period),
            actual = query.dailyPeriods
        )
    }

    private fun validQuery(
        dailyPeriods: List<StudyPeriod>
    ): DashboardQuery =
        DashboardQuery(
            learnerId = LearnerId("learner-1"),
            todayPeriod = todayPeriod(),
            currentWeekPeriod =
                period(
                    start = 1_400L,
                    end = 1_900L
                ),
            currentMonthPeriod =
                period(
                    start = 1_000L,
                    end = 2_000L
                ),
            dailyPeriods = dailyPeriods
        )

    private fun todayPeriod(): StudyPeriod =
        period(
            start = 1_700L,
            end = 1_900L
        )

    private fun period(
        start: Long,
        end: Long
    ): StudyPeriod =
        StudyPeriod(
            startInclusive = Moment(start),
            endExclusive = Moment(end)
        )
}