package vn.loi.learning.application.session.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner

class ProductBrainSessionBootstrapTest {

    private val bootstrap = ProductBrainSessionBootstrap()
    private val planner = ProductBrainPlanner()

    @Test
    fun `bootstrap creates complete session overview with goal, timeline and initial decision`() {
        val overview = bootstrap.bootstrap(
            learnerId = "learner-01",
            topicId = "topic-jlpt-n3",
            content = null,
            availableTimeMinutes = 15
        )

        assertNotNull(overview)
        assertEquals("learner-01", overview.context.learnerId)
        assertEquals("topic-jlpt-n3", overview.context.topicId)
        assertEquals("DURABLE_RECALL", overview.goal.objective)
        assertEquals("SESSION_BOOTSTRAP", overview.initialDecision.triggerEvent)
        assertTrue(overview.initialDecision.rulesTriggered.contains("INITIAL_EXPOSURE"))
        assertTrue(overview.isReadyToStart)
    }

    @Test
    fun `ProductBrainPlanner delegates bootstrapSession correctly`() {
        val overview = planner.bootstrapSession(
            learnerId = "learner-02",
            topicId = "topic-medical-physics",
            content = null,
            availableTimeMinutes = 20
        )

        assertNotNull(overview)
        assertEquals("learner-02", overview.context.learnerId)
        assertEquals("topic-medical-physics", overview.context.topicId)
        assertEquals("DURABLE_RECALL", overview.goal.objective)
        assertTrue(overview.isReadyToStart)
    }
}
