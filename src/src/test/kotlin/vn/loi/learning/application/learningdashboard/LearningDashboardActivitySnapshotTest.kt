package vn.loi.learning.application.learningdashboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.progress.LearningProgressSnapshot

class LearningDashboardActivitySnapshotTest {

    @Test
    fun `preserves composed learning progress snapshot`() {
        val progress =
            LearningProgressSnapshot(
                totalReviews = 10,
                activeDays = 2,
                againCount = 1,
                hardCount = 2,
                goodCount = 5,
                easyCount = 2
            )

        val snapshot =
            LearningDashboardActivitySnapshot(
                progress = progress
            )

        assertSame(
            progress,
            snapshot.progress
        )
    }

    @Test
    fun `reports activity when progress contains reviews`() {
        val snapshot =
            LearningDashboardActivitySnapshot(
                progress =
                    LearningProgressSnapshot(
                        totalReviews = 4,
                        activeDays = 1,
                        againCount = 1,
                        hardCount = 1,
                        goodCount = 2,
                        easyCount = 0
                    )
            )

        assertTrue(snapshot.hasActivity)
    }

    @Test
    fun `reports no activity when progress is empty`() {
        val snapshot =
            LearningDashboardActivitySnapshot(
                progress = LearningProgressSnapshot.EMPTY
            )

        assertFalse(snapshot.hasActivity)
    }

    @Test
    fun `empty snapshot uses empty learning progress`() {
        val snapshot =
            LearningDashboardActivitySnapshot.EMPTY

        assertEquals(
            LearningProgressSnapshot.EMPTY,
            snapshot.progress
        )

        assertFalse(snapshot.hasActivity)
    }
}