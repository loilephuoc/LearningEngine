package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudyQueueStrategyType

class StudySessionGoalFingerprintTest {
    @Test
    fun `only New or Review limit changes the goal fingerprint`() {
        val baseline = SessionPolicy(newItemLimit = 10, reviewItemLimit = 20)
        val unrelatedPolicyChange =
            baseline.copy(queueStrategy = StudyQueueStrategyType.NEW_FIRST)

        assertEquals(baseline.goalFingerprint(), unrelatedPolicyChange.goalFingerprint())
        assertNotEquals(
            baseline.goalFingerprint(),
            baseline.copy(newItemLimit = 5).goalFingerprint()
        )
        assertNotEquals(
            baseline.goalFingerprint(),
            baseline.copy(reviewItemLimit = 50).goalFingerprint()
        )
    }
}
