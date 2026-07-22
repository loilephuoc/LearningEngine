package vn.loi.learning.desktop.runtime

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopRuntimeDiagnosticsTest {
    @Test
    fun `creates deterministic support diagnostics with user home redacted`() {
        val home = Path.of("C:\\Users\\private-user")
        val root = home.resolve("AppData").resolve("Local").resolve("LearningEngine")

        val diagnostics =
            DesktopRuntimeDiagnosticsFactory.create(
                directories =
                    DesktopRuntimeDirectories(
                        data = root.resolve("data"),
                        config = root.resolve("config"),
                        cache = root.resolve("cache"),
                        logs = root.resolve("logs"),
                        temp = Path.of("C:\\Temp\\LearningEngine"),
                        legacyDataInUse = false
                    ),
                buildMetadata =
                    DesktopBuildMetadata(
                        applicationVersion = "2.0.0",
                        buildChannel = "beta",
                        buildRevision = "abc123",
                        buildNumber = "42"
                    ),
                logFile = root.resolve("logs").resolve("current.log"),
                userHome = home,
                osName = "Windows 11",
                osVersion = "10.0",
                architecture = "amd64",
                javaRuntime = "21.0.8"
            )

        val summary = diagnostics.supportSummary()

        assertEquals("2.0.0", diagnostics.version)
        assertEquals("beta", diagnostics.buildChannel)
        assertContains(summary, "Application ID: vn.loi.learning.desktop")
        assertContains(summary, "Operating system: Windows 11 10.0")
        assertContains(summary, "Data directory: <user-home>")
        assertFalse(summary.contains("private-user"))
    }

    @Test
    fun `path outside user home remains explicit`() {
        assertEquals(
            Path.of("C:\\Shared\\LearningEngine").toAbsolutePath().normalize().toString(),
            DesktopRuntimeDiagnosticsFactory.redact(
                path = Path.of("C:\\Shared\\LearningEngine"),
                userHome = Path.of("C:\\Users\\learner")
            )
        )
    }
}
