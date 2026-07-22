package vn.loi.learning.desktop.ui.settings

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics

class SettingsAccessibilityTest {
    @Test
    fun `runtime diagnostic properties follow stable About order`() {
        val properties =
            resolveRuntimeDiagnosticProperties(
                DesktopRuntimeDiagnostics(
                    applicationId = "app",
                    applicationName = "Learning Engine",
                    version = "2.0",
                    buildChannel = "beta",
                    buildRevision = "abc",
                    buildNumber = "42",
                    operatingSystem = "Windows 11",
                    architecture = "amd64",
                    javaRuntime = "21",
                    dataDirectory = "<user-home>\\data",
                    configDirectory = "<user-home>\\config",
                    logsDirectory = "<user-home>\\logs",
                    logFile = "<user-home>\\logs\\current.log",
                    legacyDataInUse = false
                )
            )

        assertEquals(
            listOf(
                "Version",
                "Build channel",
                "Build revision",
                "Build number",
                "Operating system",
                "Architecture",
                "Java runtime",
                "Data directory",
                "Logs directory",
                "Current log"
            ),
            properties.map(Pair<String, String>::first)
        )
    }

    @Test
    fun `property exposes label and value as one semantic unit`() {
        val accessibility =
            resolveSettingsPropertyAccessibility(
                label = "Scheduler",
                value = "FSRS"
            )

        assertEquals(
            "Scheduler: FSRS.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank property value receives stable fallback`() {
        val accessibility =
            resolveSettingsPropertyAccessibility(
                label = "Runtime",
                value = "   "
            )

        assertEquals(
            "Unavailable",
            accessibility.value
        )
        assertEquals(
            "Runtime: Unavailable.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `section summary follows visible property order`() {
        val description =
            resolveSettingsSectionContentDescription(
                title = "Learning Engine",
                properties =
                    listOf(
                        "Scheduler" to "FSRS",
                        "Architecture" to "Clean Architecture + DDD",
                        "Persistence" to "JSON"
                    )
            )

        assertEquals(
            "Learning Engine settings. " +
                "Scheduler: FSRS. " +
                "Architecture: Clean Architecture + DDD. " +
                "Persistence: JSON.",
            description
        )
        assertContains(
            description,
            "Learning Engine settings"
        )
    }
}
