package vn.loi.learning.desktop.ui.studio

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class ContentStudioClipboardPasteWiringTest {
    @Test
    fun `image paste is bubble routed and editable fields report focus ownership`() {
        val screen = Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/studio/ContentStudioScreen.kt")
        )
        val editor = Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/studio/ContentEditorPane.kt")
        )

        assertTrue(screen.contains(".onKeyEvent { event ->"))
        assertTrue(screen.contains("ContentStudioImagePastePolicy.shouldAttempt"))
        assertTrue(screen.contains("editableTextFocused = isEditableTextFocused"))
        assertTrue(screen.contains("val pasteImageFromClipboard: () -> Unit"))
        assertTrue(screen.contains("pasteImageFromClipboard()"))
        assertTrue(screen.contains("onQuickPasteImage = if"))
        assertTrue(screen.contains("Clipboard does not contain a supported image."))
        assertTrue(editor.contains(".onFocusChanged { reportEditableFocus(it.isFocused) }"))
        assertTrue(editor.contains("LocalContentStudioEditableFocusReporter"))
        assertTrue(editor.split(".quickPasteImageOnSecondaryClick(onQuickPasteImage)").size - 1 == 2)
        assertTrue(editor.contains("Ctrl+V or Right-click to paste copied image"))
        assertTrue(editor.contains("PointerButton.Secondary"))
        assertTrue(editor.contains("waitForUpOrCancellation()"))
    }
}
