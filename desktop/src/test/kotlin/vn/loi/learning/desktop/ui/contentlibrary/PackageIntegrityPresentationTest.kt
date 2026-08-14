package vn.loi.learning.desktop.ui.contentlibrary

import java.time.Instant
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.integrity.*

class PackageIntegrityPresentationTest {
    @Test
    fun `healthy report exposes readable status counts and no repair action`() {
        val presentation = report(IntegrityStatus.HEALTHY, emptyList()).toPresentation()
        assertEquals("Healthy", presentation.statusText)
        assertEquals("0 errors · 0 warnings · 0 info", presentation.summaryText)
        assertTrue("Healthy" in presentation.accessibilityDescription)
        assertFalse(presentation.offersRepair)
    }

    @Test
    fun `warning and error reports retain exact finding counts and non-color severity`() {
        val findings = listOf(
            IntegrityFinding(IntegritySeverity.ERROR, "MISSING_CONTENT", "Content", "c1", "Missing content"),
            IntegrityFinding(IntegritySeverity.WARNING, "MISSING_MEDIA", "Content", "c2", "Missing media"),
            IntegrityFinding(IntegritySeverity.INFO, "ORPHAN_TRAJECTORY", "Content", "c3", "Retained history")
        )
        val presentation = report(IntegrityStatus.ERRORS, findings).toPresentation()
        assertEquals("Errors", presentation.statusText)
        assertEquals(3, presentation.findingCount)
        assertEquals("1 errors · 1 warnings · 1 info", presentation.summaryText)
        assertTrue("1 errors" in presentation.accessibilityDescription)
        assertFalse(presentation.offersRepair)
    }

    @Test
    fun `package report modal remains package-scoped and offers no repair controls`() {
        val source = Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/ContentLibraryScreen.kt")
            .toFile().readText()
        assertTrue(source.contains("PACKAGE ${'$'}{severity.name}S"))
        assertFalse(source.contains("Text(\"Repair\""))
        assertFalse(source.contains("Text(\"Fix All\""))
    }

    private fun report(status: IntegrityStatus, findings: List<IntegrityFinding>): PackageIntegrityReport {
        val summary = IntegritySummary(
            findings.count { it.severity == IntegritySeverity.ERROR },
            findings.count { it.severity == IntegritySeverity.WARNING },
            findings.count { it.severity == IntegritySeverity.INFO }
        )
        return PackageIntegrityReport("installed", "package", "Package", Instant.EPOCH, status, findings, summary)
    }
}
