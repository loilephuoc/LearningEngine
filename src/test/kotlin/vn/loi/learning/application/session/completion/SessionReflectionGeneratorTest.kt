package vn.loi.learning.application.session.completion

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue
import vn.loi.learning.application.scene.EvidencePerformance

class SessionReflectionGeneratorTest {
    private val generator = SessionReflectionGenerator()

    @Test
    fun `reflection stays positive and identifies reinforcement for every performance`() {
        EvidencePerformance.entries.forEach { performance ->
            val reflection = generator.generate(completionInput(performance).evidence)
            assertContains(reflection.encouragement, if (performance == EvidencePerformance.CORRECT) "successfully" else "progress".takeIf { performance == EvidencePerformance.PARTIAL } ?: "opportunity")
            assertTrue(reflection.reinforcement.isNotBlank())
            assertTrue(reflection.reinforcement.contains("review", ignoreCase = true) || reflection.reinforcement.contains("revisit", ignoreCase = true))
        }
    }
}
