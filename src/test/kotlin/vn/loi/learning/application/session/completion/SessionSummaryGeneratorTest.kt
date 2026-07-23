package vn.loi.learning.application.session.completion

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SessionSummaryGeneratorTest {
    @Test
    fun `summary uses session goal and learner-facing next step`() {
        val input = completionInput()
        val summary = SessionSummaryGenerator().generate(input)

        assertContains(summary.whatWasLearned, "durable recall")
        assertContains(summary.overallOutcome, "successful recall")
        assertEquals(input.decisionExplanation.nextStep, summary.whatHappensNext)
    }
}
