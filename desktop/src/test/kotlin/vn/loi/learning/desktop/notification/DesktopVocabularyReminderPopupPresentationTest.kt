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
    fun `vertical presentation policy is substantially larger and taller than compact`() {
        val compact = COMPACT_PRESENTATION
        val vertical = VERTICAL_PRESENTATION
        assertTrue(vertical.heightWithImageDp > compact.heightWithImageDp)
        assertTrue(vertical.imageMaxWidthDp > compact.imageMaxWidthDp)
        assertTrue(vertical.imageMaxHeightDp > compact.imageMaxHeightDp)
        assertTrue(vertical.widthWithoutImageDp < vertical.widthWithImageDp)
        assertTrue(vertical.heightWithoutImageDp < vertical.heightWithImageDp)
        assertEquals(2, vertical.questionMaxLines)
        assertEquals(3, vertical.secondaryTextMaxLines)
    }

    @Test
    fun `vertical image bounds preserve source ratio inside vertical maximum`() {
        assertEquals(DesktopVocabularyReminderImageBounds(400, 200), resolveVerticalReminderImageBounds(1200, 600))
        assertEquals(DesktopVocabularyReminderImageBounds(100, 200), resolveVerticalReminderImageBounds(600, 1200))
        assertEquals(DesktopVocabularyReminderImageBounds(200, 200), resolveVerticalReminderImageBounds(800, 800))
    }

    @Test
    fun `popup dimensions reflow when image is absent for both compact and vertical layouts`() {
        assertEquals(326 to 124, resolvePopupDimensions(candidate(imageReference = "image/ref"), DesktopVocabularyReminderPopupLayout.COMPACT))
        assertEquals(280 to 104, resolvePopupDimensions(candidate(imageReference = null), DesktopVocabularyReminderPopupLayout.COMPACT))
        val verticalWithImage = resolveVerticalPopupDimensions(candidate(imageReference = "image/ref"), imagePixelDimensions = 800 to 800)
        val verticalWithoutImage = resolveVerticalPopupDimensions(candidate(imageReference = null))
        assertTrue(verticalWithImage.popupHeightDp > verticalWithoutImage.popupHeightDp)
        assertEquals(0, verticalWithoutImage.imageHeightDp)
        assertTrue(verticalWithoutImage.popupHeightDp <= 200)
    }

    @Test
    fun `test A portrait image produces narrower popup width than landscape image`() {
        val portrait = resolveVerticalPopupDimensions(
            candidate = candidate(imageReference = "image/portrait"),
            imagePixelDimensions = 600 to 1000 // 0.6 ratio
        )
        val landscape = resolveVerticalPopupDimensions(
            candidate = candidate(imageReference = "image/landscape"),
            imagePixelDimensions = 1920 to 1080 // 1.77 ratio
        )
        assertTrue(portrait.popupWidthDp < landscape.popupWidthDp)
        assertEquals(260, portrait.popupWidthDp)
        assertTrue(landscape.popupWidthDp >= 380)
    }

    @Test
    fun `test B square image uses compact width`() {
        val square = resolveVerticalPopupDimensions(
            candidate = candidate(imageReference = "image/square"),
            imagePixelDimensions = 800 to 800
        )
        assertTrue(square.popupWidthDp in 280..340)
        assertEquals(200, square.imageHeightDp)
        assertEquals(200, square.imageWidthDp)
    }

    @Test
    fun `test C landscape image expands horizontally`() {
        val landscape = resolveVerticalPopupDimensions(
            candidate = candidate(imageReference = "image/wide"),
            imagePixelDimensions = 1600 to 900
        )
        assertTrue(landscape.popupWidthDp in 360..450)
    }

    @Test
    fun `test D no image collapses image region and popup becomes shorter`() {
        val noImage = resolveVerticalPopupDimensions(
            candidate = candidate(imageReference = null)
        )
        assertEquals(0, noImage.imageWidthDp)
        assertEquals(0, noImage.imageHeightDp)
        assertTrue(noImage.popupHeightDp in 140..200)
    }

    @Test
    fun `test E and F font size 22sp and 36sp adapt popup height to prevent text clipping`() {
        val font22 = resolveVerticalPopupDimensions(
            candidate = candidate(primaryText = "word"),
            englishFontSizeSp = 22f,
            imagePixelDimensions = 800 to 800
        )
        val font36 = resolveVerticalPopupDimensions(
            candidate = candidate(primaryText = "word"),
            englishFontSizeSp = 36f,
            imagePixelDimensions = 800 to 800
        )
        assertTrue(font36.popupHeightDp > font22.popupHeightDp)
    }

    @Test
    fun `test G long english phrase widens popup and allocates multi-line height`() {
        val longPhrase = resolveVerticalPopupDimensions(
            candidate = candidate(primaryText = "internationalization and accessibility", imageReference = "image/portrait"),
            englishFontSizeSp = 32f,
            imagePixelDimensions = 600 to 1000
        )
        assertTrue(longPhrase.popupWidthDp > 260)
        assertTrue(longPhrase.popupHeightDp > 360)
    }

    @Test
    fun `test H long vietnamese translation allocates height and keeps action bar reserved`() {
        val longViet = resolveVerticalPopupDimensions(
            candidate = candidate(
                translation = "đây là một bản dịch rất dài để kiểm tra việc bố trí dòng tiếng Việt",
                imageReference = "image/square"
            ),
            englishFontSizeSp = 22f,
            imagePixelDimensions = 800 to 800
        )
        assertTrue(longViet.popupHeightDp >= 380)
    }

    @Test
    fun `test I constrained monitor shrinks image to preserve all learning text within monitor work area`() {
        val constrained = resolveVerticalPopupDimensions(
            candidate = candidate(
                primaryText = "long english phrase",
                translation = "bản dịch tiếng Việt chi tiết",
                ipa = "/phrase/",
                partOfSpeech = "NOUN",
                imageReference = "image/square"
            ),
            englishFontSizeSp = 32f,
            imagePixelDimensions = 800 to 800,
            maxMonitorHeightDp = 420
        )
        assertTrue(constrained.popupHeightDp <= 420)
        assertTrue(constrained.imageHeightDp < 200, "Image must yield space when monitor height is constrained")
        assertTrue(constrained.imageHeightDp >= 80, "Image maintains minimum safe height")
    }

    @Test
    fun `vertical card centers text elements horizontally while compact preserves start alignment`() {
        val source = File("src/main/kotlin/vn/loi/learning/desktop/notification/DesktopVocabularyReminderPopupWindow.kt").readText()
        val verticalCardIndex = source.indexOf("fun DesktopVocabularyReminderVerticalCard")
        assertTrue(verticalCardIndex >= 0)
        val verticalCardSource = source.substring(verticalCardIndex)
        assertTrue(verticalCardSource.contains("textAlign = TextAlign.Center"))
        assertTrue(verticalCardSource.contains("horizontalArrangement = Arrangement.Center"))
        assertTrue(verticalCardSource.contains("fontSize = englishTextFontSizeSp.sp"))
        val compactCardIndex = source.indexOf("fun DesktopVocabularyReminderCard")
        val compactCardSource = source.substring(compactCardIndex, verticalCardIndex)
        assertTrue(compactCardSource.contains("fontSize = englishTextFontSizeSp.sp"))
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

    @Test
    fun `multi monitor resolution and fallback to primary works predictably`() {
        val mon1 = DesktopReminderMonitor("mon-1", "Primary", Rectangle(0, 0, 1920, 1080), Rectangle(0, 0, 1920, 1040), 1.0, 1.0, true)
        val mon2 = DesktopReminderMonitor("mon-2", "Secondary", Rectangle(1920, 0, 2560, 1440), Rectangle(1920, 0, 2560, 1400), 1.25, 1.25, false)
        val monitors = listOf(mon1, mon2)

        assertEquals(mon2, DesktopVocabularyReminderPopupPositioning.resolveMonitor("mon-2", monitors))
        assertEquals(mon1, DesktopVocabularyReminderPopupPositioning.resolveMonitor("missing", monitors))
        assertEquals(mon1, DesktopVocabularyReminderPopupPositioning.resolveMonitor(null, monitors))
    }

    @Test
    fun `resolveCustomPosition maps normalized coordinates into usable work area and clamps`() {
        val monitor = DesktopReminderMonitor("mon-1", "Primary", Rectangle(0, 0, 1920, 1080), Rectangle(0, 0, 1920, 1040), 1.0, 1.0, true)

        // 0.0, 0.0 -> top left
        val topLeft = DesktopVocabularyReminderPopupPositioning.resolveCustomPosition(monitor, 0.0, 0.0, 300, 100)
        assertEquals(0, topLeft.xDp)
        assertEquals(0, topLeft.yDp)

        // 1.0, 1.0 -> bottom right of usable area
        val botRight = DesktopVocabularyReminderPopupPositioning.resolveCustomPosition(monitor, 1.0, 1.0, 300, 100)
        assertEquals(1620, botRight.xDp)
        assertEquals(940, botRight.yDp)

        // 0.5, 0.5 -> center
        val center = DesktopVocabularyReminderPopupPositioning.resolveCustomPosition(monitor, 0.5, 0.5, 300, 100)
        assertEquals(810, center.xDp)
        assertEquals(470, center.yDp)

        // Out of bounds normalized values clamp safely
        val clamped = DesktopVocabularyReminderPopupPositioning.resolveCustomPosition(monitor, -0.5, 1.5, 300, 100)
        assertEquals(0, clamped.xDp)
        assertEquals(940, clamped.yDp)
    }

    @Test
    fun `calculateNormalizedPosition accurately identifies target monitor and relative coordinates`() {
        val mon1 = DesktopReminderMonitor("mon-1", "Primary", Rectangle(0, 0, 1920, 1080), Rectangle(0, 0, 1920, 1040), 1.0, 1.0, true)
        val mon2 = DesktopReminderMonitor("mon-2", "Secondary", Rectangle(1920, 0, 1920, 1080), Rectangle(1920, 0, 1920, 1040), 1.0, 1.0, false)
        val monitors = listOf(mon1, mon2)

        // Position on monitor 2
        val (targetId, norm) = DesktopVocabularyReminderPopupPositioning.calculateNormalizedPosition(
            logicalX = 1920 + 810,
            logicalY = 470,
            widthDp = 300,
            heightDp = 100,
            monitors = monitors
        )
        assertEquals("mon-2", targetId)
        assertEquals(0.5, norm.first, 0.01)
        assertEquals(0.5, norm.second, 0.01)
    }

    @Test
    fun `adaptive vertical layout resolves valid dimensions at 48sp font maximum`() {
        val longEnglish = "internationalization"
        val candidateWithLongEnglish = candidate(
            imageReference = "img/landscape",
            primaryText = longEnglish,
            translation = "quá trình quốc tế hóa và bản địa hóa phần mềm",
            ipa = "/ˌɪn.təˌnæʃ.ən.əl.aɪˈzeɪ.ʃən/",
            partOfSpeech = "noun"
        )
        listOf(14f, 22f, 36f, 40f, 44f, 48f).forEach { fontSizeSp ->
            val dimensions = resolveVerticalPopupDimensions(
                candidate = candidateWithLongEnglish,
                imagePixelDimensions = 800 to 450,
                englishFontSizeSp = fontSizeSp
            )
            assertTrue(dimensions.popupWidthDp in 260..420, "Width $dimensions at $fontSizeSp sp must fit bounds")
            assertTrue(dimensions.popupHeightDp in 200..500, "Height $dimensions at $fontSizeSp sp must fit bounds")
            assertTrue(dimensions.imageHeightDp > 0)
        }
    }

    @Test
    fun `popup source contains layout toggle and snooze quick controls`() {
        val source = File("src/main/kotlin/vn/loi/learning/desktop/notification/DesktopVocabularyReminderPopupWindow.kt").readText()
        assertTrue(source.contains("Icons.Default.Snooze"))
        assertTrue(source.contains("Icons.Default.ViewAgenda"))
        assertTrue(source.contains("5 minutes"))
        assertTrue(source.contains("10 minutes"))
        assertTrue(source.contains("30 minutes"))
        assertTrue(source.contains("60 minutes"))
        assertTrue(source.contains("Custom..."))
        assertTrue(source.contains("CustomSnoozeDialog"))
        assertTrue(source.contains("Minutes (1–1440)"))
    }

    @Test
    fun `audio icon uses error red tint when muted in compact and vertical layouts`() {
        val source = File("src/main/kotlin/vn/loi/learning/desktop/notification/DesktopVocabularyReminderPopupWindow.kt").readText()
        assertTrue(source.contains("tint = if (!autoPlayPronunciation) MaterialTheme.colorScheme.error"), "Muted audio must use error/red tint")
        assertTrue(source.contains("else if (!audioAvailable) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)"), "Disabled audio must not use red tint")
    }

    private fun candidate(
        imageReference: String? = null,
        primaryText: String = "Word",
        translation: String? = "Translation",
        ipa: String? = null,
        partOfSpeech: String? = null
    ) = DesktopVocabularyCandidate(
        ContentId("content"), InstalledPackageId("package"), "Package", primaryText,
        "Answer", translation, ipa, partOfSpeech, imageReference, null, null, null, null
    )
}
