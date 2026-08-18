package vn.loi.learning.desktop.ui.browser.imagereuse

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageReuseReviewLayoutTest {

    @Test
    fun `ImageReuseReviewDialog uses responsive unconstrained platform dialog sizing with margin bounds`() {
        val source = imageReuseSource("ImageReuseReviewDialog.kt")

        assertTrue(source.contains("properties = DialogProperties(usePlatformDefaultWidth = false)"))
        assertTrue(source.contains("fillMaxWidth(0.95f)"))
        assertTrue(source.contains("widthIn(min = 760.dp, max = 1400.dp)"))
        assertTrue(source.contains("fillMaxHeight(0.92f)"))
        assertTrue(source.contains("heightIn(min = 540.dp, max = 880.dp)"))
    }

    @Test
    fun `Target and Source comparison columns remain equal weight 50-50 side by side with large fit preview`() {
        val source = imageReuseSource("ImageReuseReviewDialog.kt")

        val targetSection = source.substringAfter("// LEFT: TARGET ITEM").substringBefore("// RIGHT: SOURCE CANDIDATE")
        val sourceSection = source.substringAfter("// RIGHT: SOURCE CANDIDATE").substringBefore("// Bottom Shortcut Guidance & Actions")

        assertTrue(targetSection.contains("modifier = Modifier.weight(1f).fillMaxHeight()"))
        assertTrue(sourceSection.contains("modifier = Modifier.weight(1f).fillMaxHeight()"))
        assertTrue(targetSection.contains("ReviewImagePreview("))
        assertTrue(sourceSection.contains("ReviewImagePreview("))
        assertTrue(source.contains("contentScale = ContentScale.Fit"))
    }

    @Test
    fun `Footer layout is responsive with BoxWithConstraints and includes all required buttons`() {
        val source = imageReuseSource("ImageReuseReviewDialog.kt")
        val footerSource = source.substringAfter("// Bottom Shortcut Guidance & Actions").substringBefore("private sealed interface ReviewImageState")

        assertTrue(footerSource.contains("BoxWithConstraints(modifier = Modifier.fillMaxWidth())"))
        assertTrue(footerSource.contains("\"Previous Item\""))
        assertTrue(footerSource.contains("\"Undo Last Use\""))
        assertTrue(footerSource.contains("\"Close\""))
        assertTrue(footerSource.contains("\"Skip Candidate\""))
        assertTrue(footerSource.contains("\"Skip This Item\""))
        assertTrue(footerSource.contains("primaryButtonText"))
        assertTrue(footerSource.contains("Enter: Use & Next"))
        assertTrue(footerSource.contains("Enter: Save & Next"))
    }

    private fun imageReuseSource(name: String): String = imageReuseSourceDirectory().resolve(name).readText()

    private fun imageReuseSourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/browser/imagereuse")
        return if (fromRoot.isDirectory) {
            fromRoot
        } else {
            File("src/main/kotlin/vn/loi/learning/desktop/ui/browser/imagereuse")
        }
    }
}
