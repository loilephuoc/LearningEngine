package vn.loi.learning.domain.study.analytics.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Moment

class StudyPeriodTest {

    @Test
    fun `period includes start and excludes end`() {
        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        assertTrue(
            Moment(1_000L) in period
        )

        assertTrue(
            Moment(1_999L) in period
        )

        assertFalse(
            Moment(2_000L) in period
        )
    }

    @Test
    fun `period excludes moments before start and after end`() {
        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        assertFalse(
            Moment(999L) in period
        )

        assertFalse(
            Moment(2_001L) in period
        )
    }

    @Test
    fun `period rejects equal start and end`() {
        assertFailsWith<IllegalArgumentException> {
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(1_000L)
            )
        }
    }

    @Test
    fun `period rejects start after end`() {
        assertFailsWith<IllegalArgumentException> {
            StudyPeriod(
                startInclusive = Moment(2_000L),
                endExclusive = Moment(1_000L)
            )
        }
    }
}