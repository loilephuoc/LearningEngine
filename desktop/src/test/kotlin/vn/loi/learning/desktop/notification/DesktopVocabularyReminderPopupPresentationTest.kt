package vn.loi.learning.desktop.notification

import java.awt.Insets
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.File
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class DesktopVocabularyReminderPopupPresentationTest {
    @Test
    fun `window policy is no focus always on top and deliberately non interactive`() {
        val policy = DesktopVocabularyReminderPopupWindowPolicy()
        assertTrue(policy.undecorated)
        assertTrue(policy.transparent)
        assertFalse(policy.resizable)
        assertFalse(policy.focusable)
        assertTrue(policy.alwaysOnTop)
    }

    @Test
    fun `compact presentation bounds footprint image and text without lesson metadata`() {
        val policy = COMPACT_PRESENTATION
        assertTrue(policy.widthWithImageDp in 300..330)
        assertTrue(policy.widthWithoutImageDp < policy.widthWithImageDp)
        assertTrue(policy.heightWithImageDp in 110..135)
        assertTrue(policy.heightWithoutImageDp < policy.heightWithImageDp)
        assertTrue(policy.imageMaxWidthDp in 110..120)
        assertTrue(policy.imageMaxHeightDp in 100..110)
        assertEquals(2, policy.questionMaxLines)
        assertEquals(2, policy.secondaryTextMaxLines)
        assertFalse(policy.showLessonOrSection)
        val source = File("src/main/kotlin/vn/loi/learning/desktop/notification/DesktopVocabularyReminderPopupWindow.kt").readText()
        assertTrue(source.contains("if (reference == null) return"), "Missing image must reclaim its layout space")
        assertFalse(source.contains("candidate.lesson"))
        assertFalse(source.contains("candidate.section"))
        assertFalse(source.contains("candidate.packageDisplayName"))
        assertFalse(source.contains("candidate.answer"))
        assertTrue(source.contains("StudyPosBadge"))
        assertTrue(source.contains("ContentScale.Fit"))
        assertFalse(source.contains("ContentScale.Crop"))
        assertFalse(source.contains("Surface("), "Image must not have a visible wrapper surface")
        assertTrue(source.contains("onClick = onClose"))
        assertTrue(source.contains("onClick = onToggleAudio"))
        assertTrue(source.contains("onClick = onToggleDifficult"))
        assertTrue(source.contains("MaterialTheme.colorScheme.error"))
        assertTrue(source.indexOf("onClick = onClose") < source.indexOf("onClick = onToggleDifficult"))
        assertTrue(source.indexOf("onClick = onToggleDifficult") < source.indexOf("onClick = onToggleAudio"))
        assertTrue(source.contains("Modifier.width(30.dp).fillMaxSize()"))
        assertFalse(source.contains("Row(horizontalArrangement = Arrangement.spacedBy(2.dp))"))
    }

    @Test
    fun `image bounds preserve source ratio inside compact maximum without blank wrapper`() {
        assertEquals(DesktopVocabularyReminderImageBounds(118, 59), resolveReminderImageBounds(1200, 600))
        assertEquals(DesktopVocabularyReminderImageBounds(54, 108), resolveReminderImageBounds(600, 1200))
        assertEquals(DesktopVocabularyReminderImageBounds(108, 108), resolveReminderImageBounds(800, 800))
    }

    @Test
    fun `popup dimensions reflow when image is absent`() {
        assertEquals(326 to 124, resolvePopupDimensions(candidate(imageReference = "image/ref")))
        assertEquals(280 to 104, resolvePopupDimensions(candidate(imageReference = null)))
    }

    @Test
    fun `adaptive width honors each row reserves image and rail and clamps`() {
        val short = resolveAdaptivePopupWidth(40, 60, 50, 0)
        assertEquals(308, short)
        assertTrue(resolveAdaptivePopupWidth(260, 80, 90, 0) > short)
        assertTrue(resolveAdaptivePopupWidth(80, 300, 90, 0) > short)
        assertTrue(resolveAdaptivePopupWidth(80, 90, 320, 0) > short)
        assertTrue(resolveAdaptivePopupWidth(120, 120, 120, 118) > short)
        assertEquals(450, resolveAdaptivePopupWidth(2_000, 10, 10, 118))
    }

    @Test
    fun `viewer uses original image fit dark background and visible close`() {
        val source = File("src/main/kotlin/vn/loi/learning/desktop/notification/DesktopVocabularyReminderImageViewer.kt").readText()
        assertTrue(source.contains("Files.readAllBytes"))
        assertTrue(source.contains("ContentScale.Fit"))
        assertFalse(source.contains("ContentScale.Crop"))
        assertTrue(source.contains("Color.Black"))
        assertTrue(source.contains("Close full vocabulary image"))
    }

    @Test
    fun `text measurement stays inside desktop window content composition`() {
        val source = File("src/main/kotlin/vn/loi/learning/desktop/notification/DesktopVocabularyReminderPopupWindow.kt").readText()
        val windowContent = source.indexOf("    Window(\n        onCloseRequest")
        val measurement = source.indexOf("rememberTextMeasurer()")
        val card = source.indexOf("DesktopVocabularyReminderCard(", measurement)
        assertTrue(windowContent >= 0)
        assertTrue(measurement > windowContent, "Platform text measurement must execute inside Window content")
        assertTrue(card > measurement)
        assertTrue(source.contains("LaunchedEffect(visible.generation, adaptivePlacement)"))
    }

    @Test
    fun `content description includes available vocabulary and omits absent optionals`() {
        val description = buildPopupContentDescription(candidate())
        assertEquals("Vocabulary reminder. Word. Translation", description)
        assertFalse(description.contains("null"))
    }

    @Test
    fun `position uses source monitor work area at one hundred and one hundred fifty percent scaling`() {
        assertEquals(
            DesktopVocabularyReminderPopupPlacement(1504, 764, 400, 260),
            DesktopVocabularyReminderPopupPositioning.bottomRight(
                DesktopVocabularyReminderMonitorGeometry(
                    Rectangle(0, 0, 1920, 1080), Insets(0, 0, 40, 0), 1.0, 1.0
                ), 400, 260
            )
        )
        assertEquals(
            DesktopVocabularyReminderPopupPlacement(2784, 764, 400, 260),
            DesktopVocabularyReminderPopupPositioning.bottomRight(
                DesktopVocabularyReminderMonitorGeometry(
                    Rectangle(1920, 0, 2880, 1620), Insets(0, 0, 60, 0), 1.5, 1.5
                ), 400, 260
            )
        )
    }

    @Test
    fun `position clamps popup within small negative origin work area`() {
        assertEquals(
            DesktopVocabularyReminderPopupPlacement(-790, 10, 780, 560),
            DesktopVocabularyReminderPopupPositioning.bottomRight(
                DesktopVocabularyReminderMonitorGeometry(
                    Rectangle(-800, 0, 800, 600), Insets(10, 10, 30, 10), 1.0, 1.0
                ), 900, 700
            )
        )
    }

    private fun candidate(imageReference: String? = null) = DesktopVocabularyCandidate(
        ContentId("content"), InstalledPackageId("package"), "Package", "Word",
        "Answer", "Translation", null, null, imageReference, null, null, null
    )
}
