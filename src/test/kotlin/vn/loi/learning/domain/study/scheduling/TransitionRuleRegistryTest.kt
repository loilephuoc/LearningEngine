package vn.loi.learning.domain.study.scheduling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewRating

class TransitionRuleRegistryTest {

    @Test
    fun `registry returns the rule registered for rating`() {
        val againRule = AgainTransitionRule()

        val registry =
            TransitionRuleRegistry(
                listOf(againRule)
            )

        assertEquals(
            againRule,
            registry.ruleFor(ReviewRating.AGAIN)
        )
    }

    @Test
    fun `registry rejects duplicate rules for the same rating`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                TransitionRuleRegistry(
                    listOf(
                        FakeAgainRule(),
                        FakeAgainRule()
                    )
                )
            }

        assertEquals(
            "Duplicate TransitionRule detected.",
            exception.message
        )
    }

    @Test
    fun `registry rejects lookup for an unregistered rating`() {
        val registry =
            TransitionRuleRegistry(
                listOf(
                    AgainTransitionRule()
                )
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                registry.ruleFor(ReviewRating.GOOD)
            }

        assertEquals(
            "No TransitionRule registered for GOOD",
            exception.message
        )
    }

    private class FakeAgainRule : TransitionRule {

        override val rating: ReviewRating =
            ReviewRating.AGAIN

        override fun apply(
            state: MemoryState
        ): SchedulerTransition =
            error("Not required by this registry test.")
    }
}