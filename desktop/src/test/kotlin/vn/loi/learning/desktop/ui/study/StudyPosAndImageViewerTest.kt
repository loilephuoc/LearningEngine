package vn.loi.learning.desktop.ui.study

import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyPosAndImageViewerTest {
    @Test
    fun `viewer retains the selected image and close only clears viewer state`() {
        val image = Path.of("lesson", "prompt.png")

        val opened = StudyImageViewerState().open(image)
        assertTrue(opened.visible)
        assertEquals(image, opened.imagePath)

        val closed = opened.close()
        assertFalse(closed.visible)
        assertNull(closed.imagePath)
    }

    @Test
    fun `study image opens fitted full-window dialog while audio remains a separate action`() {
        val source = studySource("FocusedAnswerSurface.kt")
        val imageBlock = source.substringAfter("internal fun StudyVocabularyImageBlock(")

        assertTrue(imageBlock.contains("viewerState.open(imagePath)"))
        assertTrue(imageBlock.contains("Dialog("))
        assertTrue(imageBlock.contains("DialogProperties(usePlatformDefaultWidth = false)"))
        assertTrue(imageBlock.contains("onDismissRequest = { viewerState = viewerState.close() }"))
        assertTrue(imageBlock.contains("contentScale = ContentScale.Fit"))
        assertTrue(imageBlock.contains("fillMaxWidth(0.94f)"))
        assertTrue(imageBlock.contains("fillMaxHeight(0.92f)"))
        assertTrue(imageBlock.contains("contentDescription = \"Đóng ảnh\""))
        assertTrue(imageBlock.contains("IconButton("))
        assertTrue(imageBlock.contains("enabled = enabled"))
        assertTrue(imageBlock.contains("audioController!!.playOnce(audioPath!!)"))
        assertFalse(imageBlock.contains(".audioPressable("))
    }

    private fun studySource(fileName: String): String =
        File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$fileName").readText()
}
