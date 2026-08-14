package vn.loi.learning.android.study

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test
import vn.loi.learning.domain.study.memory.model.ReviewRating

class QuickReviewSessionInsightsTest {
    @Test
    fun `new session starts at zero and total is always derived from buckets`() {
        val empty = QuickReviewSessionInsights("quick-session")
        assertEquals(0, empty.totalExposures)

        val completed = empty.recordSkip()
            .record(ReviewRating.AGAIN)
            .record(ReviewRating.HARD)
            .record(ReviewRating.GOOD)
            .record(ReviewRating.EASY)

        assertEquals(5, completed.totalExposures)
        assertEquals(1, completed.skipped)
        assertEquals(1, completed.again)
        assertEquals(1, completed.hard)
        assertEquals(1, completed.good)
        assertEquals(1, completed.easy)
    }

    @Test
    fun `invalid session identity and negative buckets are rejected`() {
        assertFailsWith<IllegalArgumentException> { QuickReviewSessionInsights("") }
        assertFailsWith<IllegalArgumentException> { QuickReviewSessionInsights("session", skipped = -1) }
    }

    @Test
    fun `accumulator rejects duplicate stale and cancelled completions and resets on new session`() {
        val accumulator = QuickReviewInsightsAccumulator()
        accumulator.begin("session-one")
        assertNull(accumulator.summary)

        assertNull(accumulator.recordSkip("stale-session", "visit-stale"))
        assertNull(accumulator.summary)
        assertEquals(1, accumulator.recordSkip("session-one", "visit-one")?.skipped)
        assertNull(accumulator.recordRating("session-one", "visit-one", ReviewRating.AGAIN))
        assertEquals(1, accumulator.summary?.totalExposures)

        accumulator.begin("session-two")
        assertNull(accumulator.summary)
        assertNull(accumulator.recordSkip("session-one", "visit-late-callback"))
        assertNull(accumulator.summary)
        assertEquals(1, accumulator.recordRating("session-two", "visit-two", ReviewRating.HARD)?.hard)
        assertEquals("session-two", accumulator.summary?.sessionId)
    }
}
