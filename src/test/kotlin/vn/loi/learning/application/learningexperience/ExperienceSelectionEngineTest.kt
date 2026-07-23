package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExperienceSelectionEngineTest {
    @Test
    fun `engine delegates and builds authoritative semantic result`() {
        var delegated: ExperienceSelectionRequest? = null
        val strategy = ExperienceSelectionStrategy { request ->
            delegated = request
            ExperienceSelectionDecision(1, ExperienceSelectionReason.ROUND_ROBIN)
        }
        val options = LearningExperienceOptions.from(
            listOf(
                LearningExperienceKind.IMAGE_RECALL,
                LearningExperienceKind.PROMPT_RECALL
            )
        )
        val request = ExperienceSelectionRequest(options, 91)

        val result = ExperienceSelectionEngine(strategy).select(request)

        assertEquals(request, delegated)
        assertEquals(LearningExperienceKind.PROMPT_RECALL, result.selectedKind)
        assertEquals(options.orderedKinds, result.availableKinds)
        assertEquals(1, result.selectedIndex)
        assertEquals(ExperienceSelectionReason.ROUND_ROBIN, result.reason)
    }

    @Test
    fun `engine rejects strategy index outside availability`() {
        val engine = ExperienceSelectionEngine(
            ExperienceSelectionStrategy {
                ExperienceSelectionDecision(4, ExperienceSelectionReason.ROUND_ROBIN)
            }
        )
        val request = ExperienceSelectionRequest(
            LearningExperienceOptions.from(
                listOf(LearningExperienceKind.PROMPT_RECALL)
            ),
            0
        )

        assertFailsWith<IllegalArgumentException> {
            engine.select(request)
        }
    }

    @Test
    fun `empty availability normalizes safely to prompt`() {
        val options = LearningExperienceOptions.from(emptyList())
        val result = ExperienceSelectionEngine(RoundRobinExperienceStrategy())
            .select(ExperienceSelectionRequest(options, Long.MIN_VALUE))

        assertEquals(LearningExperienceKind.PROMPT_RECALL, result.selectedKind)
        assertEquals(listOf(LearningExperienceKind.PROMPT_RECALL), result.availableKinds)
        assertEquals(0, result.selectedIndex)
    }
}
