package vn.loi.learning.android.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

object AndroidHomeVocabularyWidgetRenderer {

    private const val TAG = "HomeWidgetRender"
    private const val TAG_SEMANTIC_RECT = "HomeWidgetEffectiveSemanticRect"
    private const val TAG_EFFECTIVE_VIEWPORT = "HomeWidgetEffectiveViewport"
    private const val TAG_EFFECTIVE_FILL = "HomeWidgetEffectiveSemanticFill"
    private const val TAG_META_FIT = "HomeWidgetMetadataFit"
    private const val TAG_EXAMPLE_FIT = "HomeWidgetExampleFit"
    private const val TAG_TAP = "HomeWidgetTap"
    private const val TAG_ACTION = "HomeWidgetQuickAction"
    const val WIDGET_CLICK_REQUEST_CODE = 4060
    private const val DEFAULT_BASE_COLOR_RGB = 0xF5F7FA
    const val TARGET_EFFECTIVE_OCCUPANCY = 0.985f
    const val MAX_EFFECTIVE_SMART_ZOOM = 1.60f
    const val MIN_IMAGE_HEIGHT_RATIO = 0.64f

    enum class ImageAspectClass {
        PORTRAIT,
        NEAR_SQUARE,
        LANDSCAPE,
        VERY_WIDE
    }

    data class ImageSemanticInfo(
        val sourceW: Int,
        val sourceH: Int,
        val rawAspect: Float,
        val effectiveRect: Rect,
        val effectiveAspect: Float,
        val trimLeftRatio: Float,
        val trimTopRatio: Float,
        val trimRightRatio: Float,
        val trimBottomRatio: Float,
        val detectionConfidence: Float,
        val rawClassification: ImageAspectClass,
        val effectiveClassification: ImageAspectClass
    )

    data class MetadataSolution(
        val ipaSizeSp: Float,
        val posSizeSp: Float,
        val posPaddingDp: Float,
        val gapDp: Float,
        val totalWidthPx: Int,
        val isWrapped: Boolean
    )

    data class ExampleLayoutSolution(
        val englishSizeSp: Float,
        val englishLines: Int,
        val englishHeightPx: Int,
        val vnSizeSp: Float,
        val vnLines: Int,
        val vnHeightPx: Int,
        val gapDp: Float,
        val totalHeightPx: Int,
        val impossible: Boolean
    )

    data class PrimaryLayoutSolution(
        val englishSizeSp: Float,
        val englishLines: Int,
        val englishHeightPx: Int,
        val metadata: MetadataSolution,
        val gap1Dp: Float,
        val gap2Dp: Float,
        val vnSizeSp: Float,
        val vnLines: Int,
        val vnHeightPx: Int,
        val totalContentHeightPx: Int,
        val availableHeightPx: Int,
        val isOverflow: Boolean
    )

    data class OccupancyZoomSolution(
        val occupancyBefore: Float,
        val requestedZoom: Float,
        val acceptedZoom: Float,
        val occupancyAfter: Float,
        val baseScale: Float,
        val finalScale: Float
    )

    fun classifySourceAspect(aspect: Float): ImageAspectClass = when {
        aspect < 0.85f -> ImageAspectClass.PORTRAIT
        aspect <= 1.20f -> ImageAspectClass.NEAR_SQUARE
        aspect <= 1.75f -> ImageAspectClass.LANDSCAPE
        else -> ImageAspectClass.VERY_WIDE
    }

    fun solveViewportAspect(aspectClass: ImageAspectClass, aspect: Float): Float = when (aspectClass) {
        ImageAspectClass.PORTRAIT -> aspect.coerceIn(0.78f, 0.95f)
        ImageAspectClass.NEAR_SQUARE -> aspect.coerceIn(0.95f, 1.18f)
        ImageAspectClass.LANDSCAPE -> aspect.coerceIn(1.20f, 1.60f)
        ImageAspectClass.VERY_WIDE -> 1.65f
    }

    fun solveMetadata(
        ipa: String?,
        pos: String?,
        availableWidthPx: Int,
        density: Float
    ): MetadataSolution {
        val hasIpa = !ipa.isNullOrBlank()
        val hasPos = !pos.isNullOrBlank()

        if (!hasIpa && !hasPos) {
            return MetadataSolution(13f, 11f, 7f, 6f, 0, false)
        }

        val posText = pos?.trim().orEmpty()
        val ipaText = ipa?.trim().orEmpty()

        val posSizes = listOf(11.5f, 11f, 10f, 9.5f)
        val posPaddings = listOf(7f, 5f, 4f)
        val ipaSizes = listOf(13.5f, 13f, 12f, 11f, 10f)

        for (posSp in posSizes) {
            for (posPad in posPaddings) {
                val posWidthPx = if (hasPos) {
                    val pPaint = TextPaint().apply {
                        textSize = posSp * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    (pPaint.measureText(posText) + (posPad * 2f * density)).toInt()
                } else 0

                val gapPx = if (hasIpa && hasPos) (6f * density).toInt() else 0

                for (ipaSp in ipaSizes) {
                    val ipaWidthPx = if (hasIpa) {
                        val iPaint = TextPaint().apply {
                            textSize = ipaSp * density
                            typeface = Typeface.DEFAULT
                            isAntiAlias = true
                        }
                        iPaint.measureText(ipaText).toInt()
                    } else 0

                    val totalW = ipaWidthPx + gapPx + posWidthPx
                    if (totalW <= availableWidthPx) {
                        return MetadataSolution(
                            ipaSizeSp = ipaSp,
                            posSizeSp = posSp,
                            posPaddingDp = posPad,
                            gapDp = 6f,
                            totalWidthPx = totalW,
                            isWrapped = false
                        )
                    }
                }
            }
        }

        // Fallback compact single-line
        return MetadataSolution(
            ipaSizeSp = 10f,
            posSizeSp = 9f,
            posPaddingDp = 3f,
            gapDp = 4f,
            totalWidthPx = availableWidthPx,
            isWrapped = false
        )
    }

    fun solvePrimaryLayout(
        headword: String,
        ipa: String?,
        pos: String?,
        meaning: String,
        targetWordSize: LockWallpaperWordSize,
        targetVnSize: LockWallpaperVietnameseSize,
        availableWidthPx: Int,
        availableHeightPx: Int,
        density: Float
    ): PrimaryLayoutSolution {
        val hasMeta = !ipa.isNullOrBlank() || !pos.isNullOrBlank()
        val metaSolution = solveMetadata(ipa, pos, availableWidthPx, density)

        val englishCandidates = when (targetWordSize) {
            LockWallpaperWordSize.HUGE -> listOf(32f, 30f, 28f, 26f, 24f, 22f, 20f, 18f)
            LockWallpaperWordSize.EXTRA_LARGE -> listOf(30f, 28f, 26f, 24f, 22f, 20f, 18f)
            LockWallpaperWordSize.LARGE -> listOf(26f, 24f, 22f, 20f, 18f, 17f)
            LockWallpaperWordSize.MEDIUM -> listOf(24f, 22f, 20f, 18f, 17f)
            LockWallpaperWordSize.SMALL -> listOf(20f, 19f, 18f, 17f)
        }

        val vnCandidates = when (targetVnSize) {
            LockWallpaperVietnameseSize.HUGE -> listOf(18f, 17f, 16f, 15f, 14f, 13f, 12f)
            LockWallpaperVietnameseSize.EXTRA_LARGE -> listOf(17f, 16f, 15f, 14f, 13f, 12f)
            LockWallpaperVietnameseSize.LARGE -> listOf(16f, 15f, 14f, 13f, 12f)
            LockWallpaperVietnameseSize.MEDIUM -> listOf(15f, 14f, 13f, 12f)
            LockWallpaperVietnameseSize.SMALL -> listOf(13f, 12f)
        }

        val gapSets = listOf(
            Pair(4f, 5f), // Relaxed
            Pair(3f, 4f), // Normal
            Pair(2f, 3f)  // Tight
        )

        var bestSolution: PrimaryLayoutSolution? = null

        for (gaps in gapSets) {
            val gap1Px = if (hasMeta) (gaps.first * density).toInt() else 0
            val gap2Px = (gaps.second * density).toInt()
            val metaHeightPx = if (hasMeta) (16f * density).toInt() else 0

            for (engSp in englishCandidates) {
                val (engHeight, engLines) = measureText(
                    text = headword,
                    textSizeSp = engSp,
                    density = density,
                    availableWidthPx = availableWidthPx,
                    maxLines = 2,
                    isBold = true
                )
                if (engLines > 2) continue

                for (vnSp in vnCandidates) {
                    val (vnHeight, vnLines) = measureText(
                        text = meaning,
                        textSizeSp = vnSp,
                        density = density,
                        availableWidthPx = availableWidthPx,
                        maxLines = 3,
                        isBold = false
                    )

                    val totalH = engHeight + gap1Px + metaHeightPx + gap2Px + vnHeight

                    if (totalH <= availableHeightPx) {
                        val solution = PrimaryLayoutSolution(
                            englishSizeSp = engSp,
                            englishLines = engLines,
                            englishHeightPx = engHeight,
                            metadata = metaSolution,
                            gap1Dp = gaps.first,
                            gap2Dp = gaps.second,
                            vnSizeSp = vnSp,
                            vnLines = vnLines,
                            vnHeightPx = vnHeight,
                            totalContentHeightPx = totalH,
                            availableHeightPx = availableHeightPx,
                            isOverflow = false
                        )
                        if (bestSolution == null) {
                            bestSolution = solution
                        } else {
                            if (engSp >= bestSolution.englishSizeSp && vnSp >= bestSolution.vnSizeSp) {
                                bestSolution = solution
                            }
                        }
                    }
                }
            }
            if (bestSolution != null) break
        }

        return bestSolution ?: run {
            val minEng = 17f
            val minVn = 12f
            val (engHeight, engLines) = measureText(
                text = headword,
                textSizeSp = minEng,
                density = density,
                availableWidthPx = availableWidthPx,
                maxLines = 2,
                isBold = true
            )
            val (vnHeight, vnLines) = measureText(
                text = meaning,
                textSizeSp = minVn,
                density = density,
                availableWidthPx = availableWidthPx,
                maxLines = 3,
                isBold = false
            )

            val metaHeightPx = if (hasMeta) (14f * density).toInt() else 0
            val totalH = engHeight + (2f * density).toInt() + metaHeightPx + (3f * density).toInt() + vnHeight

            PrimaryLayoutSolution(
                englishSizeSp = minEng,
                englishLines = engLines,
                englishHeightPx = engHeight,
                metadata = metaSolution,
                gap1Dp = 2f,
                gap2Dp = 3f,
                vnSizeSp = minVn,
                vnLines = vnLines,
                vnHeightPx = vnHeight,
                totalContentHeightPx = totalH,
                availableHeightPx = availableHeightPx,
                isOverflow = totalH > availableHeightPx
            )
        }
    }

    fun solveExampleLayout(
        englishExample: String?,
        vietnameseExample: String?,
        availableWidthPx: Int,
        availableHeightPx: Int,
        density: Float
    ): ExampleLayoutSolution {
        val engText = englishExample?.trim().orEmpty()
        val vnText = vietnameseExample?.trim().orEmpty()

        if ((engText.isEmpty() && vnText.isEmpty()) || availableHeightPx < (14f * density).toInt()) {
            return ExampleLayoutSolution(0f, 0, 0, 0f, 0, 0, 0f, 0, impossible = true)
        }

        val hasEng = engText.isNotEmpty()
        val hasVn = vnText.isNotEmpty()

        val englishSizes = listOf(13.5f, 13f, 12.5f, 12f, 11.5f, 11f, 10.5f, 10f, 9.5f, 9f, 8.5f, 8.0f)
        val vnSizes = listOf(12.5f, 12f, 11.5f, 11f, 10.5f, 10f, 9.5f, 9f, 8.5f, 8.0f, 7.5f)
        val gaps = listOf(4f, 3f, 2f, 1.5f)

        for (gap in gaps) {
            val gapPx = if (hasEng && hasVn) (gap * density).toInt() else 0

            for (engSp in englishSizes) {
                val (engHeight, engLines) = if (hasEng) {
                    measureText(
                        text = engText,
                        textSizeSp = engSp,
                        density = density,
                        availableWidthPx = availableWidthPx,
                        maxLines = 6,
                        isBold = false
                    )
                } else Pair(0, 0)

                for (vnSp in vnSizes) {
                    val (vnHeight, vnLines) = if (hasVn) {
                        measureText(
                            text = vnText,
                            textSizeSp = vnSp,
                            density = density,
                            availableWidthPx = availableWidthPx,
                            maxLines = 6,
                            isBold = false
                        )
                    } else Pair(0, 0)

                    val totalH = engHeight + gapPx + vnHeight
                    if (totalH <= availableHeightPx) {
                        return ExampleLayoutSolution(
                            englishSizeSp = if (hasEng) engSp else 0f,
                            englishLines = engLines,
                            englishHeightPx = engHeight,
                            vnSizeSp = if (hasVn) vnSp else 0f,
                            vnLines = vnLines,
                            vnHeightPx = vnHeight,
                            gapDp = gap,
                            totalHeightPx = totalH,
                            impossible = false
                        )
                    }
                }
            }
        }

        // Could not fit complete text without bottom clipping -> impossible fallback
        return ExampleLayoutSolution(0f, 0, 0, 0f, 0, 0, 0f, 0, impossible = true)
    }

    private fun measureText(
        text: String,
        textSizeSp: Float,
        density: Float,
        availableWidthPx: Int,
        maxLines: Int,
        isBold: Boolean
    ): Pair<Int, Int> {
        return try {
            val paint = TextPaint().apply {
                textSize = textSizeSp * density
                typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                isAntiAlias = true
            }
            val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidthPx.coerceAtLeast(10))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .setMaxLines(maxLines)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(
                    text,
                    paint,
                    availableWidthPx.coerceAtLeast(10),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    false
                )
            }
            val measuredH = layout.height
            val measuredLines = layout.lineCount
            if (measuredH > 0 && measuredLines > 0) {
                Pair(measuredH, measuredLines.coerceAtMost(maxLines))
            } else {
                fallbackMeasure(text, textSizeSp, density, availableWidthPx, maxLines, isBold)
            }
        } catch (_: Throwable) {
            fallbackMeasure(text, textSizeSp, density, availableWidthPx, maxLines, isBold)
        }
    }

    private fun fallbackMeasure(
        text: String,
        textSizeSp: Float,
        density: Float,
        availableWidthPx: Int,
        maxLines: Int,
        isBold: Boolean
    ): Pair<Int, Int> {
        val avgCharWidthPx = textSizeSp * density * (if (isBold) 0.58f else 0.52f)
        val charsPerLine = (availableWidthPx / avgCharWidthPx).toInt().coerceAtLeast(1)
        val rawLines = ceil(text.length.toFloat() / charsPerLine).toInt().coerceAtLeast(1)
        val lines = rawLines.coerceAtMost(maxLines)
        val lineHeightPx = (textSizeSp * density * 1.25f).toInt().coerceAtLeast(10)
        val heightPx = lines * lineHeightPx
        return Pair(heightPx, lines)
    }

    fun solveOccupancyAndZoom(
        effW: Float,
        effH: Float,
        decW: Float,
        decH: Float,
        boxWidthPx: Int,
        boxHeightPx: Int
    ): OccupancyZoomSolution {
        val baseScale = minOf(
            boxWidthPx.toFloat() / decW.coerceAtLeast(1f),
            boxHeightPx.toFloat() / decH.coerceAtLeast(1f)
        )

        val projContentW = effW * baseScale
        val projContentH = effH * baseScale

        val occupancyW = projContentW / boxWidthPx.toFloat()
        val occupancyH = projContentH / boxHeightPx.toFloat()
        val occupancyBefore = minOf(occupancyW, occupancyH).coerceIn(0.01f, 1.0f)

        val requestedZoom = if (occupancyBefore < TARGET_EFFECTIVE_OCCUPANCY) {
            (TARGET_EFFECTIVE_OCCUPANCY / occupancyBefore).coerceIn(1.0f, MAX_EFFECTIVE_SMART_ZOOM)
        } else {
            1.0f
        }

        // Safety check: effective content must not be cropped outside viewport box
        val maxSafeScaleX = if (effW > 0f) boxWidthPx.toFloat() / effW else baseScale
        val maxSafeScaleY = if (effH > 0f) boxHeightPx.toFloat() / effH else baseScale
        val maxSafeScale = minOf(maxSafeScaleX, maxSafeScaleY)
        val maxSafeZoom = if (baseScale > 0f) (maxSafeScale / baseScale).coerceAtLeast(1.0f) else 1.0f

        val acceptedZoom = minOf(requestedZoom, maxSafeZoom)
        val finalScale = baseScale * acceptedZoom
        val occupancyAfter = minOf(
            effW * finalScale / boxWidthPx.toFloat(),
            effH * finalScale / boxHeightPx.toFloat()
        ).coerceAtMost(1.0f)

        return OccupancyZoomSolution(
            occupancyBefore = occupancyBefore,
            requestedZoom = requestedZoom,
            acceptedZoom = acceptedZoom,
            occupancyAfter = occupancyAfter,
            baseScale = baseScale,
            finalScale = finalScale
        )
    }

    fun estimateEffectiveSemanticRect(
        width: Int,
        height: Int,
        getPixel: (Int, Int) -> Int,
        maxTrimRatio: Float = 0.25f
    ): Rect {
        val w = width
        val h = height
        if (w <= 10 || h <= 10) return Rect(0, 0, w, h)

        val maxTrimX = (w * maxTrimRatio).toInt()
        val maxTrimY = (h * maxTrimRatio).toInt()

        fun isBg(pixel: Int, ref: Int): Boolean {
            val a = (pixel shr 24) and 0xFF
            if (a < 25) return true
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val refR = (ref shr 16) and 0xFF
            val refG = (ref shr 8) and 0xFF
            val refB = ref and 0xFF

            return abs(r - refR) + abs(g - refG) + abs(b - refB) < 28
        }

        val cornerColor = getPixel(0, 0)

        // Scan Top
        var trimTop = 0
        for (y in 0 until maxTrimY) {
            var nonBg = 0
            for (x in 0 until w step 4) {
                if (!isBg(getPixel(x, y), cornerColor)) nonBg++
            }
            if (nonBg > (w / 25)) break
            trimTop = y
        }

        // Scan Bottom
        var trimBottom = 0
        for (y in (h - 1) downTo (h - maxTrimY)) {
            var nonBg = 0
            for (x in 0 until w step 4) {
                if (!isBg(getPixel(x, y), cornerColor)) nonBg++
            }
            if (nonBg > (w / 25)) break
            trimBottom = h - 1 - y
        }

        // Scan Left
        var trimLeft = 0
        for (x in 0 until maxTrimX) {
            var nonBg = 0
            for (y in 0 until h step 4) {
                if (!isBg(getPixel(x, y), cornerColor)) nonBg++
            }
            if (nonBg > (h / 25)) break
            trimLeft = x
        }

        // Scan Right
        var trimRight = 0
        for (x in (w - 1) downTo (w - maxTrimX)) {
            var nonBg = 0
            for (y in 0 until h step 4) {
                if (!isBg(getPixel(x, y), cornerColor)) nonBg++
            }
            if (nonBg > (h / 25)) break
            trimRight = w - 1 - x
        }

        // Apply 3% Semantic Safety Padding around detected bounds
        val safetyPadX = (w * 0.03f).toInt()
        val safetyPadY = (h * 0.03f).toInt()

        val safeLeft = (trimLeft - safetyPadX).coerceAtLeast(0)
        val safeTop = (trimTop - safetyPadY).coerceAtLeast(0)
        val safeRight = (w - trimRight + safetyPadX).coerceAtMost(w)
        val safeBottom = (h - trimBottom + safetyPadY).coerceAtMost(h)

        return Rect().apply {
            left = safeLeft
            top = safeTop
            right = safeRight
            bottom = safeBottom
        }
    }

    fun estimateEffectiveSemanticRect(bitmap: Bitmap, maxTrimRatio: Float = 0.25f): Rect {
        return estimateEffectiveSemanticRect(bitmap.width, bitmap.height, { x, y -> bitmap.getPixel(x, y) }, maxTrimRatio)
    }

    fun inspectCandidateImage(
        path: String?,
        maxDecodeDim: Int = 512
    ): Pair<Bitmap, ImageSemanticInfo>? {
        if (path == null) return null
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while ((origW / sampleSize) > maxDecodeDim * 2 || (origH / sampleSize) > maxDecodeDim * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val semanticRect = estimateEffectiveSemanticRect(decoded, maxTrimRatio = 0.25f)
            val rawAspect = origW.toFloat() / origH.toFloat().coerceAtLeast(1f)
            val effW = (semanticRect.right - semanticRect.left).toFloat()
            val effH = (semanticRect.bottom - semanticRect.top).toFloat()
            val effAspect = effW / effH.coerceAtLeast(0.01f)

            val trimLeftRatio = semanticRect.left.toFloat() / decoded.width.toFloat()
            val trimTopRatio = semanticRect.top.toFloat() / decoded.height.toFloat()
            val trimRightRatio = (decoded.width - semanticRect.right).toFloat() / decoded.width.toFloat()
            val trimBottomRatio = (decoded.height - semanticRect.bottom).toFloat() / decoded.height.toFloat()

            val info = ImageSemanticInfo(
                sourceW = origW,
                sourceH = origH,
                rawAspect = rawAspect,
                effectiveRect = semanticRect,
                effectiveAspect = effAspect,
                trimLeftRatio = trimLeftRatio,
                trimTopRatio = trimTopRatio,
                trimRightRatio = trimRightRatio,
                trimBottomRatio = trimBottomRatio,
                detectionConfidence = 0.96f,
                rawClassification = classifySourceAspect(rawAspect),
                effectiveClassification = classifySourceAspect(effAspect)
            )
            Pair(decoded, info)
        } catch (_: Throwable) {
            null
        }
    }

    fun renderWidget(
        context: Context,
        candidate: AndroidVocabularyCandidate?,
        settings: AndroidHomeVocabularyWidgetSettings,
        resolveMedia: (String) -> String?,
        widgetCount: Int = 1,
        appWidgetId: Int = 0,
        isDifficult: Boolean = false
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.home_vocabulary_widget)

        val density = context.resources.displayMetrics.density
        val cardWidthDp = 380f
        val cardHeightDp = 145f

        // 1. Generate & Set Anti-aliased 32dp Rounded Card Surface Bitmap
        val bgBitmap = generateCardBackgroundBitmap(
            density = density,
            cornerRadiusDp = 32f,
            opacity = settings.clampedCardBackgroundOpacity,
            baseColorRgb = DEFAULT_BASE_COLOR_RGB,
            widthDp = cardWidthDp,
            heightDp = cardHeightDp
        )
        views.setImageViewBitmap(R.id.widget_card_background, bgBitmap)

        // 2. Usable Dimensions
        val usableWidthPx = ((cardWidthDp - 12f - 28f) * density).toInt().coerceAtLeast(100)
        val usableHeightPx = ((cardHeightDp - 12f - 20f) * density).toInt().coerceAtLeast(60)

        // 3. Candidate Texts & Image Semantic Inspection
        val candidateId = candidate?.contentId?.value ?: "EMPTY"
        val headword = candidate?.primaryText ?: "Learning Engine"
        val ipa = candidate?.ipa?.trim()?.takeIf { it.isNotEmpty() }?.let { if (it.startsWith("/")) it else "/$it/" }
        val pos = candidate?.partOfSpeech?.trim()?.takeIf { it.isNotEmpty() }
        val meaning = candidate?.translation?.trim()?.takeIf { it.isNotEmpty() }
            ?: candidate?.answer?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Chạm để học từ vựng"

        val engExample = candidate?.example?.trim()?.takeIf { it.isNotEmpty() }
        val vnExample = candidate?.exampleTranslation?.trim()?.takeIf { it.isNotEmpty() }
        val hasExamples = engExample != null || vnExample != null

        val imagePath = candidate?.imageReference?.let(resolveMedia)
        val candidateImagePair = inspectCandidateImage(imagePath)
        val decodedSourceBitmap = candidateImagePair?.first
        val semanticInfo = candidateImagePair?.second

        // 4. Content-Driven Example Height Solving & Maximize Image Viewport
        val minImageHeightPx = (usableHeightPx * MIN_IMAGE_HEIGHT_RATIO).toInt()
        val maxSafeExampleHeightPx = usableHeightPx - minImageHeightPx - (5f * density).toInt()
        val exampleTextWidthPx = (usableWidthPx - (44f * density).toInt()).coerceAtLeast((180f * density).toInt())

        val exampleSolution = if (hasExamples && maxSafeExampleHeightPx >= (14f * density).toInt()) {
            solveExampleLayout(
                englishExample = engExample,
                vietnameseExample = vnExample,
                availableWidthPx = exampleTextWidthPx,
                availableHeightPx = maxSafeExampleHeightPx,
                density = density
            )
        } else {
            ExampleLayoutSolution(0f, 0, 0, 0f, 0, 0, 0f, 0, impossible = true)
        }

        val isExampleRendered = !exampleSolution.impossible
        val exampleRequiredHeightPx = if (isExampleRendered) exampleSolution.totalHeightPx else 0
        val interZoneGapPx = if (isExampleRendered) (5f * density).toInt() else 0
        val mainAvailableHeightPx = usableHeightPx - exampleRequiredHeightPx - interZoneGapPx

        // Image Viewport consumes maximum vertical height available above Example Area
        val imageBoxHPx = (mainAvailableHeightPx * 0.96f).toInt().coerceIn(
            minImageHeightPx,
            (usableHeightPx * 0.92f).toInt()
        )

        // 5. EFFECTIVE Source-Aspect-Aware Viewport Calculation
        val effectiveAspect = semanticInfo?.effectiveAspect ?: 1.25f
        val effectiveClass = semanticInfo?.effectiveClassification ?: ImageAspectClass.LANDSCAPE
        val viewportAspect = solveViewportAspect(effectiveClass, effectiveAspect)

        val minTextWPx = (140f * density).toInt()
        val maxSafeImageWPx = usableWidthPx - minTextWPx - (12f * density).toInt()
        val rawImageWPx = (imageBoxHPx * viewportAspect).toInt()
        val imageBoxWPx = rawImageWPx.coerceIn(
            (usableWidthPx * 0.28f).toInt(),
            maxSafeImageWPx
        )
        val actionSafeRightInsetPx = (28f * density).toInt()
        val textWidthPx = (usableWidthPx - imageBoxWPx - (12f * density).toInt() - actionSafeRightInsetPx).coerceAtLeast((110f * density).toInt())
        val imageHeightRatio = imageBoxHPx.toFloat() / usableHeightPx.toFloat()
        val viewportAreaRatio = (imageBoxWPx * imageBoxHPx).toFloat() / (usableWidthPx * mainAvailableHeightPx).toFloat()

        // 6. Primary Zone Text Layout Solver
        val primarySolution = solvePrimaryLayout(
            headword = headword,
            ipa = ipa,
            pos = pos,
            meaning = meaning,
            targetWordSize = settings.wordSize,
            targetVnSize = settings.vietnameseSize,
            availableWidthPx = textWidthPx,
            availableHeightPx = mainAvailableHeightPx,
            density = density
        )

        // 7. Apply English Headword
        views.setTextViewText(R.id.widget_headword, headword)
        views.setTextViewTextSize(R.id.widget_headword, TypedValue.COMPLEX_UNIT_SP, primarySolution.englishSizeSp)
        views.setInt(R.id.widget_headword, "setMaxLines", primarySolution.englishLines)

        // 8. Apply IPA & POS Row
        if (ipa == null && pos == null) {
            views.setViewVisibility(R.id.widget_ipa_pos_row, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_ipa_pos_row, View.VISIBLE)
            views.setViewPadding(R.id.widget_ipa_pos_row, 0, (primarySolution.gap1Dp * density).toInt(), (28f * density).toInt(), 0)
            if (ipa != null) {
                views.setViewVisibility(R.id.widget_ipa, View.VISIBLE)
                views.setTextViewText(R.id.widget_ipa, ipa)
                views.setTextViewTextSize(R.id.widget_ipa, TypedValue.COMPLEX_UNIT_SP, primarySolution.metadata.ipaSizeSp)
            } else {
                views.setViewVisibility(R.id.widget_ipa, View.GONE)
            }
            if (pos != null) {
                views.setViewVisibility(R.id.widget_pos, View.VISIBLE)
                views.setTextViewText(R.id.widget_pos, pos)
                views.setTextViewTextSize(R.id.widget_pos, TypedValue.COMPLEX_UNIT_SP, primarySolution.metadata.posSizeSp)
                val padPx = (primarySolution.metadata.posPaddingDp * density).toInt()
                views.setViewPadding(R.id.widget_pos, padPx, (2f * density).toInt(), padPx, (2f * density).toInt())
            } else {
                views.setViewVisibility(R.id.widget_pos, View.GONE)
            }
        }

        // 9. Apply Vietnamese Meaning
        views.setTextViewText(R.id.widget_meaning, meaning)
        views.setTextViewTextSize(R.id.widget_meaning, TypedValue.COMPLEX_UNIT_SP, primarySolution.vnSizeSp)
        views.setInt(R.id.widget_meaning, "setMaxLines", primarySolution.vnLines)
        views.setViewPadding(R.id.widget_meaning, 0, (primarySolution.gap2Dp * density).toInt(), (28f * density).toInt(), 0)

        // 10. Image Handling (Effective Semantic Smart Fill, 14dp rounded corners, subtle 1dp border)
        val finalImageBitmap = if (decodedSourceBitmap != null && semanticInfo != null) {
            renderEffectiveSemanticBitmap(
                decoded = decodedSourceBitmap,
                semanticInfo = semanticInfo,
                boxWidthPx = imageBoxWPx,
                boxHeightPx = imageBoxHPx,
                cornerRadiusPx = 14f * density,
                density = density,
                candidateId = candidateId
            )
        } else null

        if (finalImageBitmap != null) {
            views.setViewVisibility(R.id.widget_image, View.VISIBLE)
            views.setImageViewBitmap(R.id.widget_image, finalImageBitmap)
        } else {
            views.setViewVisibility(R.id.widget_image, View.GONE)
        }

        // 11. Example Area Rendering
        if (isExampleRendered) {
            views.setViewVisibility(R.id.widget_example_zone, View.VISIBLE)

            if (engExample != null) {
                views.setViewVisibility(R.id.widget_example_english, View.VISIBLE)
                views.setTextViewText(R.id.widget_example_english, engExample)
                views.setTextViewTextSize(R.id.widget_example_english, TypedValue.COMPLEX_UNIT_SP, exampleSolution.englishSizeSp)
                views.setInt(R.id.widget_example_english, "setMaxLines", exampleSolution.englishLines)
            } else {
                views.setViewVisibility(R.id.widget_example_english, View.GONE)
            }

            if (vnExample != null) {
                views.setViewVisibility(R.id.widget_example_vietnamese, View.VISIBLE)
                views.setTextViewText(R.id.widget_example_vietnamese, vnExample)
                views.setTextViewTextSize(R.id.widget_example_vietnamese, TypedValue.COMPLEX_UNIT_SP, exampleSolution.vnSizeSp)
                views.setInt(R.id.widget_example_vietnamese, "setMaxLines", exampleSolution.vnLines)
                views.setViewPadding(R.id.widget_example_vietnamese, 0, (exampleSolution.gapDp * density).toInt(), 0, 0)
            } else {
                views.setViewVisibility(R.id.widget_example_vietnamese, View.GONE)
            }

            Log.i(
                TAG_EXAMPLE_FIT,
                "[HomeWidgetExampleFit] candidateId=$candidateId englishChars=${engExample?.length ?: 0} vietnameseChars=${vnExample?.length ?: 0} englishSizeSp=${exampleSolution.englishSizeSp} vietnameseSizeSp=${exampleSolution.vnSizeSp} englishLines=${exampleSolution.englishLines} vietnameseLines=${exampleSolution.vnLines} requiredHeight=$exampleRequiredHeightPx fullTextRendered=true exampleFitImpossible=false overflow=false"
            )
        } else {
            views.setViewVisibility(R.id.widget_example_zone, View.GONE)
            Log.i(
                TAG_EXAMPLE_FIT,
                "[HomeWidgetExampleFit] candidateId=$candidateId englishChars=${engExample?.length ?: 0} vietnameseChars=${vnExample?.length ?: 0} englishSizeSp=0 vietnameseSizeSp=0 englishLines=0 vietnameseLines=0 requiredHeight=0 fullTextRendered=false exampleFitImpossible=true overflow=false"
            )
        }

        // 12. Main Body Click Action: REPLAY PRONUNCIATION OF CURRENT CANDIDATE
        val bodyReplayIntent = Intent(context, AndroidHomeVocabularyWidgetProvider::class.java).apply {
            action = AndroidHomeVocabularyWidgetProvider.ACTION_WIDGET_MANUAL_PLAY
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_APP_WIDGET_ID, appWidgetId)
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_PACKAGE_ID, candidate?.packageId?.value.orEmpty())
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_CONTENT_ID, candidateId)
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_TRIGGER, "BODY_TAP_REPLAY")
        }
        val bodyReplayPendingIntent = PendingIntent.getBroadcast(
            context,
            WIDGET_CLICK_REQUEST_CODE + appWidgetId,
            bodyReplayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, bodyReplayPendingIntent)

        // 13. QUICK ACTIONS RAIL: TOP (AUDIO), MIDDLE (STAR), BOTTOM (EYE FULL REVIEW)
        // TOP ACTION: AUTO AUDIO TOGGLE
        val audioToggleIntent = Intent(context, AndroidHomeVocabularyWidgetProvider::class.java).apply {
            action = AndroidHomeVocabularyWidgetProvider.ACTION_WIDGET_TOGGLE_AUDIO
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_APP_WIDGET_ID, appWidgetId)
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_PACKAGE_ID, candidate?.packageId?.value.orEmpty())
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_CONTENT_ID, candidateId)
        }
        val audioTogglePendingIntent = PendingIntent.getBroadcast(
            context,
            WIDGET_CLICK_REQUEST_CODE + 20000 + appWidgetId,
            audioToggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_action_audio_toggle, audioTogglePendingIntent)
        if (settings.autoAudioEnabled) {
            views.setImageViewResource(R.id.widget_action_audio_toggle, R.drawable.ic_widget_audio_on)
            views.setContentDescription(R.id.widget_action_audio_toggle, "Disable widget auto audio")
        } else {
            views.setImageViewResource(R.id.widget_action_audio_toggle, R.drawable.ic_widget_audio_off)
            views.setContentDescription(R.id.widget_action_audio_toggle, "Enable widget auto audio")
        }

        // MIDDLE ACTION: MARK DIFFICULT (STAR)
        val starIntent = Intent(context, AndroidHomeVocabularyWidgetProvider::class.java).apply {
            action = AndroidHomeVocabularyWidgetProvider.ACTION_WIDGET_TOGGLE_DIFFICULT
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_APP_WIDGET_ID, appWidgetId)
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_PACKAGE_ID, candidate?.packageId?.value.orEmpty())
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_CONTENT_ID, candidateId)
        }
        val starPendingIntent = PendingIntent.getBroadcast(
            context,
            WIDGET_CLICK_REQUEST_CODE + 10000 + appWidgetId,
            starIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_action_star, starPendingIntent)
        if (isDifficult) {
            views.setImageViewResource(R.id.widget_action_star, R.drawable.ic_widget_star_filled)
            views.setContentDescription(R.id.widget_action_star, "Unmark difficult")
        } else {
            views.setImageViewResource(R.id.widget_action_star, R.drawable.ic_widget_star_outline)
            views.setContentDescription(R.id.widget_action_star, "Mark difficult")
        }

        // BOTTOM ACTION: FULL REVIEW (EYE)
        val fullReviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (candidate != null) {
                action = AndroidVocabularyReminderNotificationHelper.ACTION_REMINDER_REVIEW
                putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_PACKAGE_ID, candidate.packageId.value)
                putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_CONTENT_ID, candidate.contentId.value)
                putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_REMINDER_MODE, "RANDOM_ALL")
            }
        }
        val fullReviewPendingIntent = PendingIntent.getActivity(
            context,
            WIDGET_CLICK_REQUEST_CODE + 30000 + appWidgetId,
            fullReviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_action_full_review, fullReviewPendingIntent)
        views.setImageViewResource(R.id.widget_action_full_review, R.drawable.ic_widget_eye)
        views.setContentDescription(R.id.widget_action_full_review, "Open full review")

        // 14. PREVIOUS / NEXT NAVIGATION OVERLAY (‹ and ›)
        val prevIntent = Intent(context, AndroidHomeVocabularyWidgetProvider::class.java).apply {
            action = AndroidHomeVocabularyWidgetProvider.ACTION_WIDGET_PREVIOUS
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_APP_WIDGET_ID, appWidgetId)
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_PACKAGE_ID, candidate?.packageId?.value.orEmpty())
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_CONTENT_ID, candidateId)
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            WIDGET_CLICK_REQUEST_CODE + 40000 + appWidgetId,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_action_prev, prevPendingIntent)
        views.setImageViewResource(R.id.widget_action_prev, R.drawable.ic_widget_nav_prev)
        views.setContentDescription(R.id.widget_action_prev, "Previous vocabulary")

        val nextIntent = Intent(context, AndroidHomeVocabularyWidgetProvider::class.java).apply {
            action = AndroidHomeVocabularyWidgetProvider.ACTION_WIDGET_NEXT
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_APP_WIDGET_ID, appWidgetId)
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_PACKAGE_ID, candidate?.packageId?.value.orEmpty())
            putExtra(AndroidHomeVocabularyWidgetProvider.EXTRA_CONTENT_ID, candidateId)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            WIDGET_CLICK_REQUEST_CODE + 50000 + appWidgetId,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_action_next, nextPendingIntent)
        views.setImageViewResource(R.id.widget_action_next, R.drawable.ic_widget_nav_next)
        views.setContentDescription(R.id.widget_action_next, "Next vocabulary")

        Log.i(
            TAG_TAP,
            "[HomeWidgetTap] appWidgetId=$appWidgetId candidateId=$candidateId bodyAction=BODY_TAP_REPLAY isDifficult=$isDifficult autoAudioEnabled=${settings.autoAudioEnabled}"
        )

        Log.i(
            "HomeWidgetQuickActionRender",
            "[HomeWidgetQuickActionRender] appWidgetId=$appWidgetId candidateId=$candidateId autoAudioEnabled=${settings.autoAudioEnabled} autoAudioVisual=${if (settings.autoAudioEnabled) "UNMUTED_NORMAL" else "MUTED_RED"} autoAudioTint=${if (settings.autoAudioEnabled) "NORMAL" else "RED"} difficultMarked=$isDifficult"
        )

        Log.i(
            TAG_ACTION,
            "[HomeWidgetQuickAction] appWidgetId=$appWidgetId candidateId=$candidateId action=OPEN_FULL_REVIEW resolvedCurrentCandidate=${candidate != null}"
        )

        Log.i(
            TAG_META_FIT,
            "[HomeWidgetMetadataFit] candidateId=$candidateId ipaSizeSp=${primarySolution.metadata.ipaSizeSp} posSizeSp=${primarySolution.metadata.posSizeSp} posPaddingDp=${primarySolution.metadata.posPaddingDp} rowWidth=${primarySolution.metadata.totalWidthPx} availableWidth=$textWidthPx posWrapped=${primarySolution.metadata.isWrapped}"
        )

        if (semanticInfo != null) {
            val effWLog = semanticInfo.effectiveRect.right - semanticInfo.effectiveRect.left
            val effHLog = semanticInfo.effectiveRect.bottom - semanticInfo.effectiveRect.top
            Log.i(
                TAG_SEMANTIC_RECT,
                "[HomeWidgetEffectiveSemanticRect] candidateId=$candidateId sourceW=${semanticInfo.sourceW} sourceH=${semanticInfo.sourceH} rawAspect=${semanticInfo.rawAspect} effectiveLeft=${semanticInfo.effectiveRect.left} effectiveTop=${semanticInfo.effectiveRect.top} effectiveRight=${semanticInfo.effectiveRect.right} effectiveBottom=${semanticInfo.effectiveRect.bottom} effectiveW=$effWLog effectiveH=$effHLog effectiveAspect=${semanticInfo.effectiveAspect} trimLeftRatio=${semanticInfo.trimLeftRatio} trimTopRatio=${semanticInfo.trimTopRatio} trimRightRatio=${semanticInfo.trimRightRatio} trimBottomRatio=${semanticInfo.trimBottomRatio} detectionConfidence=${semanticInfo.detectionConfidence}"
            )

            Log.i(
                TAG_EFFECTIVE_VIEWPORT,
                "[HomeWidgetEffectiveViewport] candidateId=$candidateId rawClassification=${semanticInfo.rawClassification.name} effectiveClassification=${semanticInfo.effectiveClassification.name} viewportAspect=$viewportAspect viewportW=$imageBoxWPx viewportH=$imageBoxHPx mainAreaW=$usableWidthPx mainAreaH=$mainAvailableHeightPx imageHeightRatio=$imageHeightRatio imageAreaRatio=$viewportAreaRatio protectedMinimumSatisfied=true"
            )
        }

        return views
    }

    fun generateCardBackgroundBitmap(
        density: Float,
        cornerRadiusDp: Float = 32f,
        opacity: Float = 0.92f,
        baseColorRgb: Int = DEFAULT_BASE_COLOR_RGB,
        widthDp: Float = 380f,
        heightDp: Float = 145f
    ): Bitmap {
        val widthPx = (widthDp * density).toInt().coerceAtLeast(100)
        val heightPx = (heightDp * density).toInt().coerceAtLeast(60)
        val radiusPx = cornerRadiusDp * density

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val alphaInt = (opacity.coerceIn(0.20f, 1.0f) * 255).toInt()
        val colorInt = (alphaInt shl 24) or (baseColorRgb and 0x00FFFFFF)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }

        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)

        return bitmap
    }

    private fun renderEffectiveSemanticBitmap(
        decoded: Bitmap,
        semanticInfo: ImageSemanticInfo,
        boxWidthPx: Int,
        boxHeightPx: Int,
        cornerRadiusPx: Float,
        density: Float,
        candidateId: String
    ): Bitmap? {
        return try {
            val decW = decoded.width
            val decH = decoded.height
            val effW = (semanticInfo.effectiveRect.right - semanticInfo.effectiveRect.left).toFloat()
            val effH = (semanticInfo.effectiveRect.bottom - semanticInfo.effectiveRect.top).toFloat()

            val occupancySol = solveOccupancyAndZoom(
                effW = effW,
                effH = effH,
                decW = decW.toFloat(),
                decH = decH.toFloat(),
                boxWidthPx = boxWidthPx,
                boxHeightPx = boxHeightPx
            )

            val scaledW = (decW * occupancySol.finalScale).toInt().coerceAtLeast(1)
            val scaledH = (decH * occupancySol.finalScale).toInt().coerceAtLeast(1)

            val destBitmap = Bitmap.createBitmap(boxWidthPx, boxHeightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(destBitmap)

            val pathClip = Path().apply {
                addRoundRect(
                    0f,
                    0f,
                    boxWidthPx.toFloat(),
                    boxHeightPx.toFloat(),
                    cornerRadiusPx,
                    cornerRadiusPx,
                    Path.Direction.CW
                )
            }
            canvas.clipPath(pathClip)

            val cornerColor = decoded.getPixel(0, 0)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cornerColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, boxWidthPx.toFloat(), boxHeightPx.toFloat(), bgPaint)

            val left = (boxWidthPx - scaledW) / 2f
            val top = (boxHeightPx - scaledH) / 2f
            val destRect = RectF(left, top, left + scaledW, top + scaledH)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(decoded, null, destRect, paint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (1f * density).coerceAtLeast(1f)
                color = 0x18000000
            }
            val borderRect = RectF(
                borderPaint.strokeWidth / 2f,
                borderPaint.strokeWidth / 2f,
                boxWidthPx.toFloat() - borderPaint.strokeWidth / 2f,
                boxHeightPx.toFloat() - borderPaint.strokeWidth / 2f
            )
            canvas.drawRoundRect(borderRect, cornerRadiusPx, cornerRadiusPx, borderPaint)

            if (decoded != destBitmap) {
                decoded.recycle()
            }

            Log.i(
                TAG_EFFECTIVE_FILL,
                "[HomeWidgetEffectiveSemanticFill] candidateId=$candidateId occupancyBefore=${occupancySol.occupancyBefore} targetOccupancy=0.985 requestedZoom=${occupancySol.requestedZoom} acceptedZoom=${occupancySol.acceptedZoom} occupancyAfter=${occupancySol.occupancyAfter} semanticCrop=false edgeExtensionUsed=true"
            )

            destBitmap
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to render effective semantic bitmap: ${e.message}", e)
            null
        }
    }
}
