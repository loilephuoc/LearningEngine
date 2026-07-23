package vn.loi.learning.desktop.ui.study

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner

class DesktopSessionBootstrapTest {

    private val planner = ProductBrainPlanner()

    @Test
    fun `Desktop session bootstrap builds overview with valid context goal timeline decision`() {
        val overview = planner.bootstrapSession(
            learnerId = "desktop-learner-01",
            topicId = "Topic: JLPT N3 Grammar",
            content = null,
            availableTimeMinutes = 15
        )

        assertNotNull(overview)
        assertEquals("desktop-learner-01", overview.context.learnerId)
        assertEquals("Topic: JLPT N3 Grammar", overview.context.topicId)
        assertEquals("DURABLE_RECALL", overview.goal.objective)
        assertEquals("SESSION_BOOTSTRAP", overview.initialDecision.triggerEvent)
        assertTrue(overview.initialDecision.pedagogicalRationale.contains("Topic: JLPT N3 Grammar"))
        assertTrue(overview.isReadyToStart)
    }

    @Test
    fun `StudyUiState holds sessionOverview projection`() {
        val overview = planner.bootstrapSession(
            learnerId = "learner-test",
            topicId = "N3 Vocabulary",
            content = null
        )

        val uiState = StudyUiState(
            studyTitle = "N3 Vocabulary",
            sessionOverview = overview,
            isSessionOverviewVisible = true
        )

        assertNotNull(uiState.sessionOverview)
        assertTrue(uiState.isSessionOverviewVisible)
        assertEquals("N3 Vocabulary", uiState.sessionOverview?.goal?.topicId)
    }
}
