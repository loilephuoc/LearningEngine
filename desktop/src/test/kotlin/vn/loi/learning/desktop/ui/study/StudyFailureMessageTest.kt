package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
import vn.loi.learning.infrastructure.persistence.mapper.InvalidPersistedRecordException

class StudyFailureMessageTest {

    @Test
    fun `persisted record failure identifies record and retry action`() {
        val failure = IllegalStateException(
            "Session recovery failed",
            InvalidPersistedRecordException(
                recordType = "study-queue",
                recordId = "session-42",
                cause = IllegalArgumentException("Unknown queue state LEGACY")
            )
        )

        val message = StudyFailureMessage.forStudyData(failure)

        assertContains(message, "study-queue")
        assertContains(message, "session-42")
        assertContains(message, "Unknown queue state LEGACY")
        assertContains(message, "Retry")
    }

    @Test
    fun `generic study failure remains actionable`() {
        val message = StudyFailureMessage.forStudyData(
            IllegalStateException("Unable to read study-sessions.json")
        )

        assertContains(message, "Unable to read study-sessions.json")
        assertContains(message, "Fix the persisted data")
        assertContains(message, "Retry")
    }
}
