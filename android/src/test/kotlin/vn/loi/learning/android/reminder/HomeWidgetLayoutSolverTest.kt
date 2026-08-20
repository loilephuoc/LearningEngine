package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class HomeWidgetLayoutSolverTest {

    private val density = 3.0f
    private val availableWidthPx = (195f * density).toInt()
    private val cardUsableWidthPx = (340f * density).toInt()
    private val cardUsableHeightPx = (115f * density).toInt()

    @Test
    fun `portrait source aspect is classified correctly and receives taller narrower viewport`() {
        val aspect = 0.72f
        val classification = AndroidHomeVocabularyWidgetRenderer.classifySourceAspect(aspect)
        assertEquals(AndroidHomeVocabularyWidgetRenderer.ImageAspectClass.PORTRAIT, classification)

        val viewportAspect = AndroidHomeVocabularyWidgetRenderer.solveViewportAspect(classification, aspect)
        assertTrue(viewportAspect in 0.78f..0.95f)
    }

    @Test
    fun `near square source aspect is classified correctly and receives balanced viewport`() {
        val aspect = 1.05f
        val classification = AndroidHomeVocabularyWidgetRenderer.classifySourceAspect(aspect)
        assertEquals(AndroidHomeVocabularyWidgetRenderer.ImageAspectClass.NEAR_SQUARE, classification)

        val viewportAspect = AndroidHomeVocabularyWidgetRenderer.solveViewportAspect(classification, aspect)
        assertTrue(viewportAspect in 0.95f..1.18f)
    }

    @Test
    fun `landscape source aspect is classified correctly and receives wide viewport`() {
        val aspect = 1.45f
        val classification = AndroidHomeVocabularyWidgetRenderer.classifySourceAspect(aspect)
        assertEquals(AndroidHomeVocabularyWidgetRenderer.ImageAspectClass.LANDSCAPE, classification)

        val viewportAspect = AndroidHomeVocabularyWidgetRenderer.solveViewportAspect(classification, aspect)
        assertTrue(viewportAspect in 1.20f..1.60f)
    }

    @Test
    fun `very wide source aspect is clamped to maximum 1_65 to preserve text width`() {
        val aspect = 2.20f
        val classification = AndroidHomeVocabularyWidgetRenderer.classifySourceAspect(aspect)
        assertEquals(AndroidHomeVocabularyWidgetRenderer.ImageAspectClass.VERY_WIDE, classification)

        val viewportAspect = AndroidHomeVocabularyWidgetRenderer.solveViewportAspect(classification, aspect)
        assertEquals(1.65f, viewportAspect)
    }

    @Test
    fun `effective semantic rect detection detects padded margins on own-style portrait artwork`() {
        val w = 400
        val h = 400
        val getPixel: (Int, Int) -> Int = { x, y ->
            if (x in 80 until 320 && y in 20 until 380) {
                0xFF0000FF.toInt()
            } else {
                0xFFFFFFFF.toInt()
            }
        }

        val semanticRect = AndroidHomeVocabularyWidgetRenderer.estimateEffectiveSemanticRect(w, h, getPixel, maxTrimRatio = 0.25f)
        val rawAspect = w.toFloat() / h.toFloat()
        val effW = (semanticRect.right - semanticRect.left).toFloat()
        val effH = (semanticRect.bottom - semanticRect.top).toFloat()
        val effAspect = effW / effH

        assertEquals(1.0f, rawAspect)
        assertTrue(effAspect < 0.85f)
        assertEquals(
            AndroidHomeVocabularyWidgetRenderer.ImageAspectClass.PORTRAIT,
            AndroidHomeVocabularyWidgetRenderer.classifySourceAspect(effAspect)
        )
    }

    @Test
    fun `effective semantic rect detection keeps cucumber-style landscape artwork intact`() {
        val w = 400
        val h = 280
        val getPixel: (Int, Int) -> Int = { x, y ->
            if (x in 5 until 395 && y in 5 until 275) {
                0xFFFFFF00.toInt() // Yellow
            } else {
                0xFF00FF00.toInt() // Green
            }
        }

        val semanticRect = AndroidHomeVocabularyWidgetRenderer.estimateEffectiveSemanticRect(w, h, getPixel, maxTrimRatio = 0.25f)
        val effW = (semanticRect.right - semanticRect.left).toFloat()
        val effH = (semanticRect.bottom - semanticRect.top).toFloat()
        val effAspect = effW / effH

        assertTrue(effAspect >= 1.30f)
        assertEquals(
            AndroidHomeVocabularyWidgetRenderer.ImageAspectClass.LANDSCAPE,
            AndroidHomeVocabularyWidgetRenderer.classifySourceAspect(effAspect)
        )
    }

    @Test
    fun `short word selects large font size and relaxed spacing with zero overflow`() {
        val solution = AndroidHomeVocabularyWidgetRenderer.solvePrimaryLayout(
            headword = "check-in",
            ipa = "/ˈtʃek.ɪn/",
            pos = "noun",
            meaning = "Sự làm thủ tục vào khách sạn/sân bay",
            targetWordSize = LockWallpaperWordSize.LARGE,
            targetVnSize = LockWallpaperVietnameseSize.MEDIUM,
            availableWidthPx = availableWidthPx,
            availableHeightPx = cardUsableHeightPx,
            density = density
        )

        assertFalse(solution.isOverflow)
        assertTrue(solution.englishSizeSp >= 24f)
        assertTrue(solution.vnSizeSp >= 14f)
        assertEquals(1, solution.englishLines)
        assertTrue(solution.totalContentHeightPx <= cardUsableHeightPx)
        assertFalse(solution.metadata.isWrapped)
    }

    @Test
    fun `medium word selects balanced font sizes and fits within available height`() {
        val solution = AndroidHomeVocabularyWidgetRenderer.solvePrimaryLayout(
            headword = "to explode",
            ipa = "/ɪkˈspləʊd/",
            pos = "verb",
            meaning = "Nổ tung, bùng nổ",
            targetWordSize = LockWallpaperWordSize.LARGE,
            targetVnSize = LockWallpaperVietnameseSize.MEDIUM,
            availableWidthPx = availableWidthPx,
            availableHeightPx = cardUsableHeightPx,
            density = density
        )

        assertFalse(solution.isOverflow)
        assertEquals(1, solution.englishLines)
        assertTrue(solution.totalContentHeightPx <= cardUsableHeightPx)
        assertFalse(solution.metadata.isWrapped)
    }

    @Test
    fun `long english and long vietnamese adapts font size and lines with zero overflow`() {
        val solution = AndroidHomeVocabularyWidgetRenderer.solvePrimaryLayout(
            headword = "take exercise",
            ipa = "/teɪk ˈek.sə.saɪz/",
            pos = "phrase",
            meaning = "Tập thể dục, rèn luyện thân thể (thường dùng trong Anh-Anh)",
            targetWordSize = LockWallpaperWordSize.LARGE,
            targetVnSize = LockWallpaperVietnameseSize.MEDIUM,
            availableWidthPx = availableWidthPx,
            availableHeightPx = cardUsableHeightPx,
            density = density
        )

        assertFalse(solution.isOverflow)
        assertTrue(solution.totalContentHeightPx <= cardUsableHeightPx)
        assertTrue(solution.vnLines in 2..3)
        assertFalse(solution.metadata.isWrapped)
    }

    @Test
    fun `very long phrase wraps to two lines and stays within card vertical budget`() {
        val solution = AndroidHomeVocabularyWidgetRenderer.solvePrimaryLayout(
            headword = "a blessing in disguise",
            ipa = "/ə ˈbles.ɪŋ ɪn dɪsˈɡaɪz/",
            pos = "idiom",
            meaning = "Trong cái rủi có cái may, họa hóa phúc",
            targetWordSize = LockWallpaperWordSize.LARGE,
            targetVnSize = LockWallpaperVietnameseSize.MEDIUM,
            availableWidthPx = availableWidthPx,
            availableHeightPx = cardUsableHeightPx,
            density = density
        )

        assertFalse(solution.isOverflow)
        assertEquals(2, solution.englishLines)
        assertTrue(solution.totalContentHeightPx <= cardUsableHeightPx)
        assertFalse(solution.metadata.isWrapped)
    }

    @Test
    fun `adjective pos badge remains strictly single line without wrapping`() {
        val metadata = AndroidHomeVocabularyWidgetRenderer.solveMetadata(
            ipa = "/ˈstɪl/",
            pos = "adjective",
            availableWidthPx = availableWidthPx,
            density = density
        )

        assertFalse(metadata.isWrapped)
        assertTrue(metadata.posSizeSp >= 9.5f)
        assertTrue(metadata.totalWidthPx <= availableWidthPx)
    }

    @Test
    fun `transitive verb long pos badge fits single line with reduced padding`() {
        val metadata = AndroidHomeVocabularyWidgetRenderer.solveMetadata(
            ipa = "/dɪˈlaɪ.tɪd/",
            pos = "transitive verb",
            availableWidthPx = availableWidthPx,
            density = density
        )

        assertFalse(metadata.isWrapped)
        assertTrue(metadata.totalWidthPx <= availableWidthPx)
    }

    @Test
    fun `long ipa and long pos solver reduces ipa to keep single line`() {
        val metadata = AndroidHomeVocabularyWidgetRenderer.solveMetadata(
            ipa = "/ˌɪn.təˌnæʃ.ən.əl.aɪˈzeɪ.ʃən/",
            pos = "adjective",
            availableWidthPx = availableWidthPx,
            density = density
        )

        assertFalse(metadata.isWrapped)
        assertTrue(metadata.totalWidthPx <= availableWidthPx)
    }

    @Test
    fun `padded grape source reaches target semantic occupancy 98_5 percent without exceeding 1_60x`() {
        val boxW = 340
        val boxH = 370
        val solution = AndroidHomeVocabularyWidgetRenderer.solveOccupancyAndZoom(
            effW = 240f,
            effH = 225f,
            decW = 300f,
            decH = 300f,
            boxWidthPx = boxW,
            boxHeightPx = boxH
        )

        assertTrue(solution.occupancyBefore < 0.80f)
        assertTrue(solution.acceptedZoom in 1.15f..1.60f)
        assertTrue(solution.acceptedZoom <= 1.60f)
    }

    @Test
    fun `already filled degree source stays at 1x zoom`() {
        val boxW = 340
        val boxH = 370
        val solution = AndroidHomeVocabularyWidgetRenderer.solveOccupancyAndZoom(
            effW = 340f,
            effH = 370f,
            decW = 340f,
            decH = 370f,
            boxWidthPx = boxW,
            boxHeightPx = boxH
        )

        assertEquals(1.0f, solution.acceptedZoom)
        assertEquals(1.0f, solution.requestedZoom)
    }

    @Test
    fun `extreme padded source caps zoom strictly at 1_60x max zoom`() {
        val boxW = 340
        val boxH = 370
        val solution = AndroidHomeVocabularyWidgetRenderer.solveOccupancyAndZoom(
            effW = 100f,
            effH = 100f,
            decW = 300f,
            decH = 300f,
            boxWidthPx = boxW,
            boxHeightPx = boxH
        )

        assertEquals(1.60f, solution.acceptedZoom)
    }

    @Test
    fun `semantic safety bounds prevent clipping effective content`() {
        val boxW = 340
        val boxH = 370
        val solution = AndroidHomeVocabularyWidgetRenderer.solveOccupancyAndZoom(
            effW = 280f,
            effH = 300f,
            decW = 300f,
            decH = 300f,
            boxWidthPx = boxW,
            boxHeightPx = boxH
        )

        assertTrue(280f * solution.finalScale <= boxW.toFloat() + 0.1f)
        assertTrue(300f * solution.finalScale <= boxH.toFloat() + 0.1f)
    }

    @Test
    fun `short bilingual examples fit safely with zero overflow and comfortable fonts`() {
        val maxSafeExampleHeightPx = (45f * density).toInt()
        val solution = AndroidHomeVocabularyWidgetRenderer.solveExampleLayout(
            englishExample = "She has the power to decide.",
            vietnameseExample = "Cô ấy có quyền quyết định.",
            availableWidthPx = cardUsableWidthPx,
            availableHeightPx = maxSafeExampleHeightPx,
            density = density
        )

        assertFalse(solution.impossible)
        assertTrue(solution.englishSizeSp >= 11f)
        assertTrue(solution.vnSizeSp >= 10f)
        assertTrue(solution.totalHeightPx <= maxSafeExampleHeightPx)
    }

    @Test
    fun `long english with short vietnamese adapts font sizes and line count`() {
        val maxSafeExampleHeightPx = (50f * density).toInt()
        val solution = AndroidHomeVocabularyWidgetRenderer.solveExampleLayout(
            englishExample = "Losing that difficult job turned out to be a blessing in disguise for his career.",
            vietnameseExample = "Mất công việc đó hóa ra lại là điều may mắn.",
            availableWidthPx = cardUsableWidthPx,
            availableHeightPx = maxSafeExampleHeightPx,
            density = density
        )

        assertFalse(solution.impossible)
        assertTrue(solution.totalHeightPx <= maxSafeExampleHeightPx)
    }

    @Test
    fun `both long bilingual examples fit with compressed gap and smaller font floor`() {
        val maxSafeExampleHeightPx = (55f * density).toInt()
        val solution = AndroidHomeVocabularyWidgetRenderer.solveExampleLayout(
            englishExample = "The international conference discussed modern localization processes for global software products.",
            vietnameseExample = "Hội nghị quốc tế đã thảo luận về quy trình bản địa hóa hiện đại cho các sản phẩm phần mềm toàn cầu.",
            availableWidthPx = cardUsableWidthPx,
            availableHeightPx = maxSafeExampleHeightPx,
            density = density
        )

        assertFalse(solution.impossible)
        assertTrue(solution.englishSizeSp >= 8.0f)
        assertTrue(solution.vnSizeSp >= 7.5f)
        assertTrue(solution.totalHeightPx <= maxSafeExampleHeightPx)
    }

    @Test
    fun `impossible extremely long examples fallback to hide example area without clipping`() {
        val tinySafeHeightPx = (12f * density).toInt()
        val solution = AndroidHomeVocabularyWidgetRenderer.solveExampleLayout(
            englishExample = "This is an extremely long paragraph containing dozens of words that cannot possibly fit in a tiny 12dp vertical container on any screen.",
            vietnameseExample = "Đây là một đoạn văn bản cực kỳ dài chứa rất nhiều từ không thể nào nằm vừa trong một khung hình 12dp.",
            availableWidthPx = cardUsableWidthPx,
            availableHeightPx = tinySafeHeightPx,
            density = density
        )

        assertTrue(solution.impossible)
    }

    @Test
    fun `missing examples return impossible so example zone is hidden cleanly`() {
        val maxSafeExampleHeightPx = (50f * density).toInt()
        val solution = AndroidHomeVocabularyWidgetRenderer.solveExampleLayout(
            englishExample = null,
            vietnameseExample = "",
            availableWidthPx = cardUsableWidthPx,
            availableHeightPx = maxSafeExampleHeightPx,
            density = density
        )

        assertTrue(solution.impossible)
    }
}
