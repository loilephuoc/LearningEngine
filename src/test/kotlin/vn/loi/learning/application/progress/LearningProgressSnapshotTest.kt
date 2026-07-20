package vn.loi.learning.application.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningProgressSnapshotTest {

    @Test
    fun `snapshot exposes review and rating counts`() {
        val snapshot =
            LearningProgressSnapshot(
                totalReviews = 10,
                activeDays = 4,
                againCount = 2,
                hardCount = 1,
                goodCount = 5,
                easyCount = 2
            )

        assertEquals(
            expected = 10,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 4,
            actual = snapshot.activeDays
        )

        assertEquals(
            expected = 2,
            actual = snapshot.againCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.hardCount
        )

        assertEquals(
            expected = 5,
            actual = snapshot.goodCount
        )

        assertEquals(
            expected = 2,
            actual = snapshot.easyCount
        )

        assertTrue(
            actual = snapshot.hasReviewActivity
        )
    }

    @Test
    fun `snapshot calculates average reviews per active day`() {
        val snapshot =
            LearningProgressSnapshot(
                totalReviews = 10,
                activeDays = 4,
                againCount = 2,
                hardCount = 1,
                goodCount = 5,
                easyCount = 2
            )

        assertEquals(
            expected = 2.5,
            actual = snapshot.averageReviewsPerActiveDay
        )
    }

    @Test
    fun `snapshot calculates accuracy from non again reviews`() {
        val snapshot =
            LearningProgressSnapshot(
                totalReviews = 10,
                activeDays = 4,
                againCount = 2,
                hardCount = 1,
                goodCount = 5,
                easyCount = 2
            )

        assertEquals(
            expected = 0.8,
            actual = snapshot.accuracy
        )
    }

    @Test
    fun `empty snapshot has no activity or derived metrics`() {
        val snapshot =
            LearningProgressSnapshot.EMPTY

        assertEquals(
            expected = 0,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = snapshot.activeDays
        )

        assertFalse(
            actual = snapshot.hasReviewActivity
        )

        assertNull(
            actual =
                snapshot.averageReviewsPerActiveDay
        )

        assertNull(
            actual = snapshot.accuracy
        )
    }

    @Test
    fun `single review day calculates average correctly`() {
        val snapshot =
            LearningProgressSnapshot(
                totalReviews = 3,
                activeDays = 1,
                againCount = 1,
                hardCount = 0,
                goodCount = 2,
                easyCount = 0
            )

        assertEquals(
            expected = 3.0,
            actual = snapshot.averageReviewsPerActiveDay
        )

        assertEquals(
            expected = 2.0 / 3.0,
            actual = snapshot.accuracy
        )
    }

    @Test
    fun `snapshot rejects negative total reviews`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressSnapshot(
                    totalReviews = -1,
                    activeDays = 0,
                    againCount = 0,
                    hardCount = 0,
                    goodCount = 0,
                    easyCount = 0
                )
            }

        assertEquals(
            expected =
                "Total reviews must not be negative.",
            actual = exception.message
        )
    }

    @Test
    fun `snapshot rejects negative active days`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressSnapshot(
                    totalReviews = 1,
                    activeDays = -1,
                    againCount = 1,
                    hardCount = 0,
                    goodCount = 0,
                    easyCount = 0
                )
            }

        assertEquals(
            expected =
                "Active days must not be negative.",
            actual = exception.message
        )
    }

    @Test
    fun `snapshot rejects active days greater than total reviews`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressSnapshot(
                    totalReviews = 2,
                    activeDays = 3,
                    againCount = 2,
                    hardCount = 0,
                    goodCount = 0,
                    easyCount = 0
                )
            }

        assertEquals(
            expected =
                "Active days must not exceed total reviews.",
            actual = exception.message
        )
    }

    @Test
    fun `snapshot rejects negative rating count`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressSnapshot(
                    totalReviews = 1,
                    activeDays = 1,
                    againCount = -1,
                    hardCount = 0,
                    goodCount = 1,
                    easyCount = 1
                )
            }

        assertEquals(
            expected =
                "Again count must not be negative.",
            actual = exception.message
        )
    }

    @Test
    fun `snapshot rejects rating counts different from total reviews`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                LearningProgressSnapshot(
                    totalReviews = 5,
                    activeDays = 2,
                    againCount = 1,
                    hardCount = 1,
                    goodCount = 1,
                    easyCount = 1
                )
            }

        assertEquals(
            expected =
                "Rating counts must equal total reviews.",
            actual = exception.message
        )
    }
}