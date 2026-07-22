package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDiagnosticExporterTest {
    @Test
    fun `exports deterministic redacted diagnostics without learning content`() {
        val directory = Files.createTempDirectory("diagnostic-export-test")
        try {
            val target = directory.resolve(DesktopDiagnosticExporter.FILE_NAME)
            val diagnostics = diagnostics()

            DesktopDiagnosticExporter.export(diagnostics, target)

            val content = Files.readString(target)
            assertEquals(DesktopDiagnosticExporter.content(diagnostics), content)
            assertTrue(content.contains("<user-home>"))
            assertFalse(content.contains("C:\\Users\\private"))
            assertFalse(content.contains("learning item"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `existing target is preserved and no temporary artifact remains`() {
        val directory = Files.createTempDirectory("diagnostic-export-existing-test")
        try {
            val target = directory.resolve(DesktopDiagnosticExporter.FILE_NAME)
            val original = "keep me".toByteArray()
            Files.write(target, original)

            assertFailsWith<DesktopDiagnosticExportException> {
                DesktopDiagnosticExporter.export(diagnostics(), target)
            }

            assertContentEquals(original, Files.readAllBytes(target))
            assertEquals(
                listOf(DesktopDiagnosticExporter.FILE_NAME),
                Files.list(directory).use { files -> files.map { it.fileName.toString() }.toList() }
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun diagnostics() =
        DesktopRuntimeDiagnostics(
            applicationId = "vn.loi.learning.desktop",
            applicationName = "Learning Engine 2.0",
            version = "1.0-SNAPSHOT",
            buildChannel = "development",
            buildRevision = "local",
            buildNumber = "0",
            operatingSystem = "Windows 11",
            architecture = "amd64",
            javaRuntime = "21",
            dataDirectory = "<user-home>\\data",
            configDirectory = "<user-home>\\config",
            logsDirectory = "<user-home>\\logs",
            logFile = "<user-home>\\logs\\current.log",
            legacyDataInUse = false
        )
}
