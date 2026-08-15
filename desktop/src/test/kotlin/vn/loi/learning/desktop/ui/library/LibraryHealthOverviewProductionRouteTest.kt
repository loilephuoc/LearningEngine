package vn.loi.learning.desktop.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.integrity.*
import vn.loi.learning.desktop.ui.contentlibrary.LibraryHealthOverviewState
import vn.loi.learning.desktop.ui.contentlibrary.LibraryHealthPackageResult

@OptIn(ExperimentalTestApi::class)
class LibraryHealthOverviewProductionRouteTest {
    @Test
    fun `production LibraryScreenContent exposes action for an empty library`() {
        var checks = 0
        runComposeUiTest {
            setContent {
                LibraryScreenContent(
                    uiState = LibraryUiState.Empty(),
                    onCheckLibraryHealth = { checks++ }
                )
            }
            onNodeWithText("Check Library Health").assertIsDisplayed().performClick()
            waitForIdle()
        }
        assertEquals(1, checks)
    }

    @Test
    fun `busy production route disables duplicate scan`() = runComposeUiTest {
        setContent {
            LibraryScreenContent(
                uiState = LibraryUiState.Empty(),
                libraryHealthOverviewState = LibraryHealthOverviewState(scanning = true),
                onCheckLibraryHealth = {}
            )
        }
        onNodeWithText("Checking…").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun `summary rows expose merged semantics and row opens frozen report authority`() {
        var opened: String? = null
        val state = LibraryHealthOverviewState(
            scannedAt = Instant.EPOCH,
            results = listOf(
                LibraryHealthPackageResult("healthy", "Healthy package", report("healthy", IntegrityStatus.HEALTHY)),
                LibraryHealthPackageResult("warning", "Warning package", report("warning", IntegrityStatus.WARNINGS)),
                LibraryHealthPackageResult("failed", "Failed package", failure = "Unavailable")
            )
        )
        runComposeUiTest {
            setContent {
                LibraryScreenContent(
                    uiState = LibraryUiState.Empty(),
                    libraryHealthOverviewState = state,
                    onCheckLibraryHealth = {},
                    onOpenLibraryHealthReport = { opened = it }
                )
            }
            onNodeWithContentDescription(
                "Library Health. 3 packages checked. 1 healthy. 1 with warnings. 0 with errors. 1 checks failed."
            ).assertIsDisplayed()
            onNodeWithContentDescription("Warning package. Status Warnings. 0 errors, 1 warnings, 0 info.")
                .assertIsDisplayed().performClick()
            onNodeWithText("Failed package").assertIsDisplayed()
            waitForIdle()
        }
        assertEquals("warning", opened)
    }

    @Test
    fun `production LibraryScreen wires row drill-down to existing integrity dialog without rescan`() {
        val source = java.io.File("src/main/kotlin/vn/loi/learning/desktop/ui/library/LibraryScreen.kt").readText()
        assertTrue(source.contains("onOpenLibraryHealthReport = contentLibraryViewModel::showLibraryHealthReport"))
        assertTrue(source.contains("contentLibraryViewModel.checkLibraryHealth(packages)"))
        assertTrue(source.contains("PackageIntegrityDialog("))
    }

    private fun report(id: String, status: IntegrityStatus): PackageIntegrityReport {
        val warning = if (status == IntegrityStatus.WARNINGS) {
            listOf(IntegrityFinding(IntegritySeverity.WARNING, "TEST", "Package", id, "warning"))
        } else emptyList()
        return PackageIntegrityReport(
            id, id, if (id == "warning") "Warning package" else "Healthy package",
            Instant.EPOCH, status, warning, IntegritySummary(0, warning.size, 0)
        )
    }
}
