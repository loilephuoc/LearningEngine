package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.application.progress.LearningProgressSnapshot
import vn.loi.learning.application.progress.LearningStageCounts
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.Retrievability

class LearningDashboardSnapshotTest {

    @Test
    fun `snapshot preserves composed sections`() {
        val activity =
            LearningDashboardActivitySnapshot(
                progress =
                    LearningProgressSnapshot(
                        totalReviews = 8,
                        activeDays = 3,
                        againCount = 1,
                        hardCount = 1,
                        goodCount = 4,
                        easyCount = 2
                    )
            )

        val memory =
            LearningDashboardMemorySnapshot(
                stageCounts =
                    LearningStageCounts(
                        newCount = 1,
                        learningCount = 2,
                        reviewCount = 3,
                        relearningCount = 4,
                        masteredCount = 5,
                        suspendedCount = 6
                    )
            )

        val scheduling =
            LearningDashboardSchedulingSnapshot(
                dueStatistics =
                    LearningDashboardDueStatistics(
                        dueCount = 7,
                        overdueCount = 3
                    )
            )

        val retention =
            LearningDashboardRetentionSnapshot(
                statistics =
                    LearningDashboardRetentionStatistics(
                        averageRetrievability =
                            Retrievability(0.88),
                        evaluatedMemoryCount = 10
                    )
            )

        val forecast =
            LearningDashboardForecastSnapshot(
                forecast =
                    LearningDashboardForecast(
                        buckets =
                            listOf(
                                LearningDashboardForecastBucket(
                                    windowStart = Moment(1_000L),
                                    windowEnd = Moment(2_000L),
                                    dueCount = 4
                                )
                            )
                    )
            )

        val snapshot =
            LearningDashboardSnapshot(
                activity = activity,
                memory = memory,
                scheduling = scheduling,
                retention = retention,
                forecast = forecast
            )

        assertSame(
            expected = activity,
            actual = snapshot.activity
        )

        assertSame(
            expected = memory,
            actual = snapshot.memory
        )

        assertSame(
            expected = scheduling,
            actual = snapshot.scheduling
        )

        assertSame(
            expected = retention,
            actual = snapshot.retention
        )

        assertSame(
            expected = forecast,
            actual = snapshot.forecast
        )
    }

    @Test
    fun `snapshot reports activity through activity section`() {
        val snapshot =
            LearningDashboardSnapshot(
                activity =
                    LearningDashboardActivitySnapshot(
                        progress =
                            LearningProgressSnapshot(
                                totalReviews = 3,
                                activeDays = 1,
                                againCount = 1,
                                hardCount = 0,
                                goodCount = 2,
                                easyCount = 0
                            )
                    ),
                memory =
                    LearningDashboardMemorySnapshot.EMPTY,
                scheduling =
                    LearningDashboardSchedulingSnapshot.EMPTY,
                retention =
                    LearningDashboardRetentionSnapshot.EMPTY,
                forecast =
                    LearningDashboardForecastSnapshot.EMPTY
            )

        assertTrue(
            actual = snapshot.hasActivity
        )
    }

    @Test
    fun `snapshot reports memories through memory section`() {
        val snapshot =
            LearningDashboardSnapshot(
                activity =
                    LearningDashboardActivitySnapshot.EMPTY,
                memory =
                    LearningDashboardMemorySnapshot(
                        stageCounts =
                            LearningStageCounts(
                                newCount = 1,
                                learningCount = 0,
                                reviewCount = 0,
                                relearningCount = 0,
                                masteredCount = 0,
                                suspendedCount = 0
                            )
                    ),
                scheduling =
                    LearningDashboardSchedulingSnapshot.EMPTY,
                retention =
                    LearningDashboardRetentionSnapshot.EMPTY,
                forecast =
                    LearningDashboardForecastSnapshot.EMPTY
            )

        assertTrue(
            actual = snapshot.hasMemories
        )
    }

    @Test
    fun `snapshot reports due state through scheduling section`() {
        val snapshot =
            LearningDashboardSnapshot(
                activity =
                    LearningDashboardActivitySnapshot.EMPTY,
                memory =
                    LearningDashboardMemorySnapshot.EMPTY,
                scheduling =
                    LearningDashboardSchedulingSnapshot(
                        dueStatistics =
                            LearningDashboardDueStatistics(
                                dueCount = 4,
                                overdueCount = 2
                            )
                    ),
                retention =
                    LearningDashboardRetentionSnapshot.EMPTY,
                forecast =
                    LearningDashboardForecastSnapshot.EMPTY
            )

        assertTrue(
            actual = snapshot.hasDueMemories
        )

        assertTrue(
            actual = snapshot.hasOverdueMemories
        )
    }

    @Test
    fun `snapshot reports retention data through retention section`() {
        val snapshot =
            LearningDashboardSnapshot(
                activity =
                    LearningDashboardActivitySnapshot.EMPTY,
                memory =
                    LearningDashboardMemorySnapshot.EMPTY,
                scheduling =
                    LearningDashboardSchedulingSnapshot.EMPTY,
                retention =
                    LearningDashboardRetentionSnapshot(
                        statistics =
                            LearningDashboardRetentionStatistics(
                                averageRetrievability =
                                    Retrievability(0.92),
                                evaluatedMemoryCount = 5
                            )
                    ),
                forecast =
                    LearningDashboardForecastSnapshot.EMPTY
            )

        assertTrue(
            actual = snapshot.hasRetentionData
        )
    }

    @Test
    fun `snapshot reports forecast through forecast section`() {
        val snapshot =
            LearningDashboardSnapshot(
                activity =
                    LearningDashboardActivitySnapshot.EMPTY,
                memory =
                    LearningDashboardMemorySnapshot.EMPTY,
                scheduling =
                    LearningDashboardSchedulingSnapshot.EMPTY,
                retention =
                    LearningDashboardRetentionSnapshot.EMPTY,
                forecast =
                    LearningDashboardForecastSnapshot(
                        forecast =
                            LearningDashboardForecast(
                                buckets =
                                    listOf(
                                        LearningDashboardForecastBucket(
                                            windowStart =
                                                Moment(1_000L),
                                            windowEnd =
                                                Moment(2_000L),
                                            dueCount = 3
                                        )
                                    )
                            )
                    )
            )

        assertTrue(
            actual = snapshot.hasForecastBuckets
        )

        assertTrue(
            actual = snapshot.hasForecastDueMemories
        )
    }

    @Test
    fun `forecast may contain buckets without due memories`() {
        val snapshot =
            LearningDashboardSnapshot(
                activity =
                    LearningDashboardActivitySnapshot.EMPTY,
                memory =
                    LearningDashboardMemorySnapshot.EMPTY,
                scheduling =
                    LearningDashboardSchedulingSnapshot.EMPTY,
                retention =
                    LearningDashboardRetentionSnapshot.EMPTY,
                forecast =
                    LearningDashboardForecastSnapshot(
                        forecast =
                            LearningDashboardForecast(
                                buckets =
                                    listOf(
                                        LearningDashboardForecastBucket(
                                            windowStart =
                                                Moment(1_000L),
                                            windowEnd =
                                                Moment(2_000L),
                                            dueCount = 0
                                        )
                                    )
                            )
                    )
            )

        assertTrue(
            actual = snapshot.hasForecastBuckets
        )

        assertFalse(
            actual = snapshot.hasForecastDueMemories
        )
    }

    @Test
    fun `empty snapshot contains empty sections`() {
        val snapshot =
            LearningDashboardSnapshot.EMPTY

        assertEquals(
            expected =
                LearningDashboardActivitySnapshot.EMPTY,
            actual = snapshot.activity
        )

        assertEquals(
            expected =
                LearningDashboardMemorySnapshot.EMPTY,
            actual = snapshot.memory
        )

        assertEquals(
            expected =
                LearningDashboardSchedulingSnapshot.EMPTY,
            actual = snapshot.scheduling
        )

        assertEquals(
            expected =
                LearningDashboardRetentionSnapshot.EMPTY,
            actual = snapshot.retention
        )

        assertEquals(
            expected =
                LearningDashboardForecastSnapshot.EMPTY,
            actual = snapshot.forecast
        )

        assertFalse(
            actual = snapshot.hasActivity
        )

        assertFalse(
            actual = snapshot.hasMemories
        )

        assertFalse(
            actual = snapshot.hasDueMemories
        )

        assertFalse(
            actual = snapshot.hasOverdueMemories
        )

        assertFalse(
            actual = snapshot.hasRetentionData
        )

        assertFalse(
            actual = snapshot.hasForecastBuckets
        )

        assertFalse(
            actual = snapshot.hasForecastDueMemories
        )
    }
}