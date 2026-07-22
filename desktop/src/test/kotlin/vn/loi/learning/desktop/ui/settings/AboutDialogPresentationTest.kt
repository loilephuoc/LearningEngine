package vn.loi.learning.desktop.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics

class AboutDialogPresentationTest {
    @Test
    fun `About presentation uses the redacted runtime diagnostic snapshot`() {
        val diagnostics = DesktopRuntimeDiagnostics(
            applicationId = "vn.loi.learningengine",
            applicationName = "Learning Engine 2.0",
            version = "2.0.0",
            buildChannel = "local",
            buildRevision = "abc123",
            buildNumber = "7",
            operatingSystem = "Windows 11",
            architecture = "amd64",
            javaRuntime = "21",
            dataDirectory = "<user-home>\\data",
            configDirectory = "<user-home>\\config",
            logsDirectory = "<user-home>\\logs",
            logFile = "<user-home>\\logs\\current.log",
            legacyDataInUse = false
        )

        val presentation = resolveAboutDialogPresentation(diagnostics)

        assertEquals("Learning Engine 2.0", presentation.title)
        assertEquals("Version 2.0.0", presentation.version)
        assertTrue(presentation.supportSummary.contains("<user-home>"))
        assertFalse(presentation.supportSummary.contains("C:\\Users"))
    }
}
