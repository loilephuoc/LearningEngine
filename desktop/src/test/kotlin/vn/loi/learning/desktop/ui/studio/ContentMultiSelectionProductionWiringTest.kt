package vn.loi.learning.desktop.ui.studio

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentMultiSelectionProductionWiringTest {
    @Test
    fun `Library production route wires every safe multi-selection action`() {
        val library = source("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/library/LibraryScreen.kt")
        val screen = source("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/studio/ContentStudioScreen.kt")
        val explorer = source("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/studio/ContentExplorerPane.kt")

        listOf(
            "togglePackageBrowserMultiSelection", "selectPackageBrowserVisibleRange",
            "selectAllVisiblePackageBrowserItems", "clearPackageBrowserMultiSelection",
            "highlightSelectedPackageBrowserItems", "removeHighlightFromSelectedPackageBrowserItems",
            "checkSelectedPackageBrowserMedia",
            "confirmBatchDelete", "dismissBatchDeleteConfirmation"
        ).forEach { assertTrue(library.contains(it), "Missing production wiring: $it") }
        assertTrue(screen.contains("ContentExplorerPane("))
        assertTrue(explorer.contains("Check Selected Media"))
        assertFalse(explorer.contains("Delete Selected"))
    }

    @Test
    fun `Ctrl A is bubbling and header plus keyboard Delete share selection-first authority`() {
        val screen = source("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/studio/ContentStudioScreen.kt")
        assertTrue(screen.contains(".onKeyEvent { event ->"))
        assertFalse(screen.contains("event.isCtrlPressed && event.key == Key.A"))
        val explorer = source("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/studio/ContentExplorerPane.kt")
        assertTrue(explorer.contains("event.isCtrlPressed && event.key == Key.A"))
        assertTrue(explorer.contains("onSelectAllVisible()"))
        assertTrue(screen.contains("onRequestDelete?.invoke()"))
        assertTrue(screen.contains("event.key == Key.Delete"))
        assertTrue(screen.contains("onDeleteClick = { onRequestDelete?.invoke() }"))
        assertTrue(screen.contains("onConfirmBatchDelete()"))
        assertTrue(screen.contains("Delete Selected (\$deleteTargetCount)"))
        assertFalse(explorer.contains("onDeleteSelected"))
    }

    private fun source(relative: String): String {
        val workingDirectory = Path.of("").toAbsolutePath()
        val repositoryRoot = if (workingDirectory.fileName.toString() == "desktop") {
            workingDirectory.parent
        } else workingDirectory
        return Files.readString(repositoryRoot.resolve(relative))
    }
}
