package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Retrievability

class LearningDashboardRetentionSnapshotTest {

    @Test
    fun `snapshot preserves composed retention statistics`() {
        val statistics =
            LearningDashboardRetentionStatistics(
                averageRetrievability =
                    Retrievability(0.86),
                evaluatedMemoryCount = 10
            )

        val snapshot =
            LearningDashboardRetentionSnapshot(
                statistics = statistics
            )

        assertSame(
            expected = statistics,
            actual = snapshot.statistics
        )
    }

    @Test
    fun `snapshot reports retention data through statistics`() {
        val snapshot =
            LearningDashboardRetentionSnapshot(
                statistics =
                    LearningDashboardRetentionStatistics(
                        averageRetrievability =
                            Retrievability(0.91),
                        evaluatedMemoryCount = 4
                    )
            )

        assertTrue(
            actual = snapshot.hasData
        )
    }

    @Test
    fun `empty snapshot contains no retention data`() {
        val snapshot =
            LearningDashboardRetentionSnapshot.EMPTY

        assertEquals(
            expected =
                LearningDashboardRetentionStatistics.EMPTY,
            actual = snapshot.statistics
        )

        assertFalse(
            actual = snapshot.hasData
        )
    }
}