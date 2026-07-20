package vn.loi.learning.domain.study.scheduling.evolution

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.SchedulerPolicy

class SimpleDifficultyEvolutionTest {

    private val defaultEvolution =
        SimpleDifficultyEvolution(
            SchedulerPolicy()
        )

    @Test
    fun `again increases difficulty using policy`() {
        val next =
            defaultEvolution.evolve(
                current = Difficulty.of(5.0),
                rating = ReviewRating.AGAIN
            )

        assertEquals(
            5.8,
            next.value
        )
    }

    @Test
    fun `hard increases difficulty using policy`() {
        val next =
            defaultEvolution.evolve(
                current = Difficulty.of(5.0),
                rating = ReviewRating.HARD
            )

        assertEquals(
            5.3,
            next.value
        )
    }

    @Test
    fun `good decreases difficulty using policy`() {
        val next =
            defaultEvolution.evolve(
                current = Difficulty.of(5.0),
                rating = ReviewRating.GOOD
            )

        assertEquals(
            4.8,
            next.value
        )
    }

    @Test
    fun `easy decreases difficulty using policy`() {
        val next =
            defaultEvolution.evolve(
                current = Difficulty.of(5.0),
                rating = ReviewRating.EASY
            )

        assertEquals(
            4.5,
            next.value
        )
    }

    @Test
    fun `increase clamps at maximum difficulty`() {
        val next =
            defaultEvolution.evolve(
                current = Difficulty.of(9.8),
                rating = ReviewRating.AGAIN
            )

        assertEquals(
            Difficulty.MAX_VALUE,
            next.value
        )
    }

    @Test
    fun `decrease clamps at minimum difficulty`() {
        val next =
            defaultEvolution.evolve(
                current = Difficulty.of(1.2),
                rating = ReviewRating.EASY
            )

        assertEquals(
            Difficulty.MIN_VALUE,
            next.value
        )
    }

    @Test
    fun `custom policy changes difficulty evolution`() {
        val policy =
            SchedulerPolicy(
                hard =
                    SchedulerPolicy.HardPolicy(
                        difficultyIncrease = 1.5
                    )
            )

        val evolution =
            SimpleDifficultyEvolution(policy)

        val next =
            evolution.evolve(
                current = Difficulty.of(5.0),
                rating = ReviewRating.HARD
            )

        assertEquals(
            6.5,
            next.value
        )
    }
}