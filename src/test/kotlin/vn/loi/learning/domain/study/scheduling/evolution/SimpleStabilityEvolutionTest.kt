package vn.loi.learning.domain.study.scheduling.evolution

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.scheduling.SchedulerPolicy

class SimpleStabilityEvolutionTest {

    private val defaultEvolution =
        SimpleStabilityEvolution(
            SchedulerPolicy()
        )

    @Test
    fun `again multiplies stability and applies minimum`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.of(2.0),
                rating = ReviewRating.AGAIN
            )

        assertEquals(
            1.0,
            next.days
        )
    }

    @Test
    fun `again applies minimum stability when multiplied value is too low`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.of(0.1),
                rating = ReviewRating.AGAIN
            )

        assertEquals(
            0.1,
            next.days
        )
    }

    @Test
    fun `hard uses initial stability when current stability is zero`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.ZERO,
                rating = ReviewRating.HARD
            )

        assertEquals(
            0.5,
            next.days
        )
    }

    @Test
    fun `hard multiplies existing stability and applies minimum`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.of(2.0),
                rating = ReviewRating.HARD
            )

        assertEquals(
            2.4,
            next.days
        )
    }

    @Test
    fun `hard applies minimum stability when multiplied value is too low`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.of(0.1),
                rating = ReviewRating.HARD
            )

        assertEquals(
            0.5,
            next.days
        )
    }

    @Test
    fun `good uses initial stability when current stability is zero`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.ZERO,
                rating = ReviewRating.GOOD
            )

        assertEquals(
            1.0,
            next.days
        )
    }

    @Test
    fun `good multiplies existing stability`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.of(2.0),
                rating = ReviewRating.GOOD
            )

        assertEquals(
            5.0,
            next.days
        )
    }

    @Test
    fun `easy uses initial stability when current stability is zero`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.ZERO,
                rating = ReviewRating.EASY
            )

        assertEquals(
            4.0,
            next.days
        )
    }

    @Test
    fun `easy multiplies existing stability`() {
        val next =
            defaultEvolution.evolve(
                current = Stability.of(2.0),
                rating = ReviewRating.EASY
            )

        assertEquals(
            7.0,
            next.days
        )
    }

    @Test
    fun `custom policy changes stability evolution`() {
        val policy =
            SchedulerPolicy(
                good =
                    SchedulerPolicy.GoodPolicy(
                        initialStabilityDays = 2.0,
                        stabilityMultiplier = 4.0
                    )
            )

        val evolution =
            SimpleStabilityEvolution(policy)

        val initial =
            evolution.evolve(
                current = Stability.ZERO,
                rating = ReviewRating.GOOD
            )

        val multiplied =
            evolution.evolve(
                current = Stability.of(2.0),
                rating = ReviewRating.GOOD
            )

        assertEquals(
            2.0,
            initial.days
        )

        assertEquals(
            8.0,
            multiplied.days
        )
    }
}