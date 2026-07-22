package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
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
        assertFalse(message.contains("session-42"))
        assertFalse(message.contains("Unknown queue state LEGACY"))
        assertContains(message, "Retry")
    }

    @Test
    fun `generic study failure remains actionable`() {
        val message = StudyFailureMessage.forStudyData(
            IllegalStateException("Unable to read study-sessions.json")
        )

        assertFalse(message.contains("Unable to read study-sessions.json"))
        assertContains(message, "restore a verified backup")
        assertContains(message, "Retry")
    }
}
