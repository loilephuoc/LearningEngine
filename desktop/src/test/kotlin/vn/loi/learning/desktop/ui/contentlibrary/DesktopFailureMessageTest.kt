package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertContains
import vn.loi.learning.infrastructure.persistence.mapper.InvalidPersistedRecordException

class DesktopFailureMessageTest {

    @Test
    fun `persisted record message identifies type id cause and recovery action`() {
        val failure =
            IllegalStateException(
                "Repository load failed",
                InvalidPersistedRecordException(
                    recordType = "content",
                    recordId = "lesson-42",
                    cause =
                        IllegalArgumentException(
                            "Unknown content type LEGACY"
                        )
                )
            )

        val message =
            DesktopFailureMessage.forPersistedData(
                failure
            )

        assertContains(message, "content")
        assertContains(message, "lesson-42")
        assertContains(message, "Unknown content type LEGACY")
        assertContains(message, "Refresh")
    }

    @Test
    fun `generic load failure remains actionable`() {
        val message =
            DesktopFailureMessage.forPersistedData(
                IllegalStateException(
                    "Unable to read content-libraries.json"
                )
            )

        assertContains(message, "Unable to read content-libraries.json")
        assertContains(message, "Fix the persisted data")
        assertContains(message, "Refresh")
    }
}
