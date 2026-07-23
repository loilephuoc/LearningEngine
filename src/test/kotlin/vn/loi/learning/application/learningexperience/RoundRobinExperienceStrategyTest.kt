package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundRobinExperienceStrategyTest {
    private val strategy = RoundRobinExperienceStrategy()

    @Test
    fun `three options rotate in canonical sequence`() {
        assertEquals(listOf(0, 1, 2, 0), (0L..3L).map { select(three(), it).selectedIndex })
    }

    @Test
    fun `two options rotate in canonical sequence`() {
        val options = options(
            LearningExperienceKind.IMAGE_RECALL,
            LearningExperienceKind.PROMPT_RECALL
        )

        assertEquals(listOf(0, 1, 0, 1), (0L..3L).map { select(options, it).selectedIndex })
    }

    @Test
    fun `one option is selected for every ordinal`() {
        val prompt = options(LearningExperienceKind.PROMPT_RECALL)

        assertEquals(
            listOf(0, 0, 0, 0),
            listOf(Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE).map {
                select(prompt, it).selectedIndex
            }
        )
    }

    @Test
    fun `zero preserves first-option legacy behavior`() {
        assertEquals(0, select(three(), 0).selectedIndex)
    }

    @Test
    fun `large and negative ordinals use deterministic floor mod`() {
        assertEquals(1, select(three(), Long.MAX_VALUE).selectedIndex)
        assertEquals(2, select(three(), -1).selectedIndex)
        assertEquals(1, select(three(), -2).selectedIndex)
    }

    @Test
    fun `selection does not mutate options and repeated requests are equal`() {
        val options = three()
        val original = options.orderedKinds.toList()
        val request = ExperienceSelectionRequest(options, 7)

        assertEquals(strategy.select(request), strategy.select(request))
        assertEquals(original, options.orderedKinds)
    }

    private fun select(options: LearningExperienceOptions, ordinal: Long) =
        strategy.select(ExperienceSelectionRequest(options, ordinal))

    private fun three() = options(
        LearningExperienceKind.IMAGE_RECALL,
        LearningExperienceKind.LISTENING_RECALL,
        LearningExperienceKind.PROMPT_RECALL
    )

    private fun options(vararg kinds: LearningExperienceKind) =
        LearningExperienceOptions.from(kinds.asList())
}
