package vn.loi.learning.domain.study.scheduling

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.ReviewRating

class TransitionRuleSetTest {

    @Test
    fun `each review rating has exactly one transition rule`() {
        val rules =
            listOf(
                AgainTransitionRule(),
                HardTransitionRule(),
                GoodTransitionRule(),
                EasyTransitionRule()
            )

        val ratings =
            rules.map { rule ->
                rule.rating
            }

        assertEquals(
            ReviewRating.entries.toSet(),
            ratings.toSet()
        )

        assertEquals(
            ratings.size,
            ratings.distinct().size
        )
    }
}