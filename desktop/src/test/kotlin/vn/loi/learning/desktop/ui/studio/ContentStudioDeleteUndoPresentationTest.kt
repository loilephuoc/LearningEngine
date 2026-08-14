package vn.loi.learning.desktop.ui.studio

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentStudioDeleteUndoPresentationTest {
    private val source = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/studio/ContentStudioScreen.kt")
    )

    @Test
    fun `undo delete is visible and confirmation promises runtime scope only`() {
        assertTrue(source.contains("text = \"Undo Delete\""))
        assertTrue(source.contains("while this Content Studio session remains open"))
        assertFalse(source.contains("This action cannot be undone."))
    }

    @Test
    fun `ctrl z delete undo bubbles after text editors and remains draft guarded`() {
        val bubbleHandler = source.indexOf(".onKeyEvent { event ->")
        val previewHandler = source.indexOf(".onPreviewKeyEvent { event ->")
        assertTrue(bubbleHandler in 0 until previewHandler)
        assertTrue(source.contains("event.isCtrlPressed && event.key == Key.Z"))
        assertTrue(source.contains("!uiState.isDirty && !uiState.isCreatingNewItem"))
    }
}
