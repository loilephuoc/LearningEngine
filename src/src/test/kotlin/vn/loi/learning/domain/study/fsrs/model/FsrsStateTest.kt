package vn.loi.learning.domain.study.fsrs.model

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.Stability

class FsrsStateTest {

    @Test
    fun `state stores difficulty and stability`() {
        val difficulty =
            Difficulty.of(6.0)

        val stability =
            Stability.of(12.0)

        val state =
            FsrsState(
                difficulty = difficulty,
                stability = stability
            )

        assertEquals(
            difficulty,
            state.difficulty
        )

        assertEquals(
            stability,
            state.stability
        )
    }

    @Test
    fun `copy creates a new state without changing the original`() {
        val original =
            FsrsState(
                difficulty = Difficulty.of(5.0),
                stability = Stability.of(8.0)
            )

        val updated =
            original.copy(
                stability = Stability.of(16.0)
            )

        assertEquals(
            Stability.of(8.0),
            original.stability
        )

        assertEquals(
            Stability.of(16.0),
            updated.stability
        )

        assertEquals(
            original.difficulty,
            updated.difficulty
        )
    }
}