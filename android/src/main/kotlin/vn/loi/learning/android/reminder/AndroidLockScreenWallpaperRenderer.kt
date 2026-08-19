package vn.loi.learning.android.reminder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import kotlin.math.roundToInt

data class LockWallpaperVocabularyRenderModel(
    val headword: String,
    val ipa: String?,
    val partOfSpeech: String?,
    val meaning: String,
    val imageBitmap: Bitmap?,
    val hasPrimaryAudio: Boolean,
    val backgroundBitmap: Bitmap? = null,
    val wordSize: LockWallpaperWordSize = LockWallpaperWordSize.EXTRA_LARGE,
    val vietnameseSize: LockWallpaperVietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
    val imageSize: LockWallpaperImageSize = LockWallpaperImageSize.EXTRA_LARGE,
    val cardBackgroundOpacity: Float = 0.72f
)

data class SafeBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    fun toRect(): Rect = Rect(left, top, right, bottom)
    fun toRectF(): RectF = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}

data class LockWallpaperSafeZoneProfile(
    val screenWidth: Int = 1220,
    val screenHeight: Int = 2712,
    val statusReserved: SafeBounds = SafeBounds(0, 0, 1220, 200),
    val clockReserved: SafeBounds = SafeBounds(0, 280, 1220, 960),
    val weatherReserved: SafeBounds = SafeBounds(0, 1020, 1220, 1400),
    val safeZoneB: SafeBounds = SafeBounds(50, 1420, 1170, 2250),
    val fingerprintReserved: SafeBounds = SafeBounds(350, 2250, 870, 2640),
    val bottomShortcutsReserved: SafeBounds = SafeBounds(0, 2400, 1220, 2712)
)

object AndroidLockScreenWallpaperRenderer {

    private const val TAG = "LockWallpaperLayout"

    fun renderDebugSafeZones(profile: LockWallpaperSafeZoneProfile = LockWallpaperSafeZoneProfile()): Bitmap {
        val bitmap = Bitmap.createBitmap(profile.screenWidth, profile.screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val darkBg = 0xFF0A0E1A.toInt()
        val bgPaint = Paint().apply { color = darkBg }
        canvas.drawRect(0f, 0f, profile.screenWidth.toFloat(), profile.screenHeight.toFloat(), bgPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 32f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        fun drawRegion(bounds: SafeBounds, fillColor: Int, strokeColor: Int, label: String) {
            fillPaint.color = fillColor
            strokePaint.color = strokeColor
            val rectF = bounds.toRectF()
            canvas.drawRoundRect(rectF, 16f, 16f, fillPaint)
            canvas.drawRoundRect(rectF, 16f, 16f, strokePaint)
            canvas.drawText(label, rectF.centerX(), rectF.centerY() + 10f, textPaint)
        }

        val redFill = 0x3CEF4444.toInt()
        val redStroke = 0xFFEF4444.toInt()
        val orangeFill = 0x46F97316.toInt()
        val orangeStroke = 0xFFF97316.toInt()
        val greenFill = 0x5022C55E.toInt()
        val greenStroke = 0xFF22C55E.toInt()

        drawRegion(profile.statusReserved, redFill, redStroke, "STATUS BAR RESERVED")
        drawRegion(profile.clockReserved, redFill, redStroke, "NATIVE CLOCK RESERVED")
        drawRegion(profile.weatherReserved, orangeFill, orangeStroke, "DATE / WEATHER RESERVED")
        drawRegion(profile.fingerprintReserved, redFill, redStroke, "FINGERPRINT RESERVED")
        drawRegion(profile.bottomShortcutsReserved, redFill, redStroke, "SHORTCUTS RESERVED")
        drawRegion(profile.safeZoneB, greenFill, greenStroke, "VOCABULARY SAFE ZONE B\n(1120 x 830 px)")

        return bitmap
    }

    fun renderVocabularyWallpaper(
        model: LockWallpaperVocabularyRenderModel,
        profile: LockWallpaperSafeZoneProfile = LockWallpaperSafeZoneProfile()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(profile.screenWidth, profile.screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Layer 0: Background (User selected photo with CENTER_CROP & scrim, or default gradient)
        if (model.backgroundBitmap != null && !model.backgroundBitmap.isRecycled) {
            drawUserBackground(canvas, model.backgroundBitmap, profile.screenWidth, profile.screenHeight)
        } else {
            drawDefaultBackground(canvas, profile.screenWidth, profile.screenHeight)
        }

        // 2. Compute Content-Driven Dynamic Vertical Layout
        val safe = profile.safeZoneB
        val availableWidth = safe.width.toFloat()
        val availableHeight = (safe.height - 20).toFloat() // 810 px max card height

        renderContentDrivenCard(canvas, model, safe, availableWidth, availableHeight)

        return bitmap
    }

    private fun renderContentDrivenCard(
        canvas: Canvas,
        model: LockWallpaperVocabularyRenderModel,
        safe: SafeBounds,
        availableWidth: Float,
        maxCardHeight: Float
    ) {
        val hasImage = model.imageBitmap != null && !model.imageBitmap.isRecycled

        // --- 1. Measure Tag ---
        val tagText = "LEARNING ENGINE • VOCABULARY"
        val tagSize = 22f
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF38BDF8.toInt()
            textSize = tagSize
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
            isFakeBoldText = true
        }
        val tagHeight = 24f

        // --- 2. Measure Image (FIT / CONTAIN with preset scaling) ---
        val (baseMaxImgW, baseMaxImgH) = when (model.imageSize) {
            LockWallpaperImageSize.MEDIUM -> Pair(700f, 380f)
            LockWallpaperImageSize.LARGE -> Pair(800f, 430f)
            LockWallpaperImageSize.EXTRA_LARGE -> Pair(900f, 490f)
            LockWallpaperImageSize.MAXIMUM -> Pair(1000f, 550f)
        }
        var maxImgW = baseMaxImgW
        var maxImgH = if (hasImage) baseMaxImgH else 0f

        var imgScaledW = 0f
        var imgScaledH = 0f
        if (hasImage) {
            val bmp = model.imageBitmap!!
            val bmpW = bmp.width.toFloat().coerceAtLeast(1f)
            val bmpH = bmp.height.toFloat().coerceAtLeast(1f)
            val scale = (maxImgW / bmpW).coerceAtMost(maxImgH / bmpH)
            imgScaledW = bmpW * scale
            imgScaledH = bmpH * scale
        }

        // --- 3. Measure Headword (No fake speaker icon - centered primary learning focal point) ---
        val headwordLen = model.headword.length
        val baseHeadwordSize = when (model.wordSize) {
            LockWallpaperWordSize.SMALL -> when {
                headwordLen <= 8 -> 64f
                headwordLen <= 12 -> 56f
                headwordLen <= 16 -> 48f
                headwordLen <= 22 -> 42f
                else -> 36f
            }
            LockWallpaperWordSize.MEDIUM -> when {
                headwordLen <= 8 -> 74f
                headwordLen <= 12 -> 66f
                headwordLen <= 16 -> 58f
                headwordLen <= 22 -> 48f
                else -> 40f
            }
            LockWallpaperWordSize.LARGE -> when {
                headwordLen <= 8 -> 84f
                headwordLen <= 12 -> 76f
                headwordLen <= 16 -> 66f
                headwordLen <= 22 -> 54f
                else -> 44f
            }
            LockWallpaperWordSize.EXTRA_LARGE -> when {
                headwordLen <= 8 -> 94f
                headwordLen <= 12 -> 84f
                headwordLen <= 16 -> 74f
                headwordLen <= 22 -> 60f
                else -> 48f
            }
            LockWallpaperWordSize.HUGE -> when {
                headwordLen <= 8 -> 106f
                headwordLen <= 12 -> 96f
                headwordLen <= 16 -> 84f
                headwordLen <= 22 -> 68f
                else -> 52f
            }
        }

        val wordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = baseHeadwordSize
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        // Dynamically shrink if word width exceeds available inner card width (1000px)
        val maxAvailableTextWidth = availableWidth - 100f
        var currentWordSize = baseHeadwordSize
        var measuredWordWidth = wordPaint.measureText(model.headword)
        while (measuredWordWidth > maxAvailableTextWidth && currentWordSize > 38f) {
            currentWordSize -= 2f
            wordPaint.textSize = currentWordSize
            measuredWordWidth = wordPaint.measureText(model.headword)
        }

        val headwordSize = currentWordSize
        val headwordHeight = headwordSize * 1.15f

        // --- 4. Measure IPA / POS ---
        val ipaPart = if (!model.ipa.isNullOrBlank()) "/${model.ipa.trim()}/" else ""
        val posPart = if (!model.partOfSpeech.isNullOrBlank()) "[${model.partOfSpeech.trim().uppercase()}]" else ""
        val metaText = listOf(ipaPart, posPart).filter { it.isNotBlank() }.joinToString("   ")
        val hasMeta = metaText.isNotBlank()
        var metaSize = 30f
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9CA3AF.toInt()
            textSize = metaSize
            textAlign = Paint.Align.CENTER
        }
        val metaHeight = if (hasMeta) 32f else 0f

        // --- 5. Measure Meaning (Adaptive Vietnamese Size Control) ---
        val meaningLen = model.meaning.length
        val baseMeaningSize = when (model.vietnameseSize) {
            LockWallpaperVietnameseSize.SMALL -> when {
                meaningLen <= 22 -> 34f
                meaningLen <= 45 -> 30f
                else -> 28f
            }
            LockWallpaperVietnameseSize.MEDIUM -> when {
                meaningLen <= 22 -> 42f
                meaningLen <= 45 -> 38f
                else -> 32f
            }
            LockWallpaperVietnameseSize.LARGE -> when {
                meaningLen <= 22 -> 50f
                meaningLen <= 45 -> 44f
                else -> 36f
            }
            LockWallpaperVietnameseSize.EXTRA_LARGE -> when {
                meaningLen <= 22 -> 58f
                meaningLen <= 45 -> 50f
                else -> 40f
            }
            LockWallpaperVietnameseSize.HUGE -> when {
                meaningLen <= 22 -> 66f
                meaningLen <= 45 -> 56f
                else -> 44f
            }
        }

        val meaningPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF9FAFB.toInt()
            textSize = baseMeaningSize
            isFakeBoldText = true
        }
        val textWidth = (availableWidth - 90f).toInt()
        var currentMeaningSize = baseMeaningSize
        var staticLayout = StaticLayout.Builder.obtain(
            model.meaning, 0, model.meaning.length, meaningPaint, textWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .build()

        while (staticLayout.lineCount > 3 && currentMeaningSize > 24f) {
            currentMeaningSize -= 2f
            meaningPaint.textSize = currentMeaningSize
            staticLayout = StaticLayout.Builder.obtain(
                model.meaning, 0, model.meaning.length, meaningPaint, textWidth
            ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(3)
                .build()
        }

        var meaningHeight = staticLayout.height.toFloat()

        // --- 6. Dynamic Spacing & Total Height Calculation ---
        var topPad = 30f
        var tagToImgGap = if (hasImage) 18f else 0f
        var imgToWordGap = if (hasImage) 36f else 28f
        var wordToMetaGap = if (hasMeta) 18f else 0f
        var metaToDivGap = 26f
        val divHeight = 2f
        var divToMeaningGap = 32f
        var bottomPad = 36f

        var totalContentHeight = topPad + tagHeight + tagToImgGap + imgScaledH +
                imgToWordGap + headwordHeight + wordToMetaGap + metaHeight +
                metaToDivGap + divHeight + divToMeaningGap + meaningHeight + bottomPad

        // Adaptive compromise order for constrained space (e.g. HUGE English + HUGE Vietnamese + MAXIMUM Image)
        if (totalContentHeight > maxCardHeight) {
            val overflow = totalContentHeight - maxCardHeight

            // Step 1: Reduce gaps
            topPad = 22f
            tagToImgGap = if (hasImage) 12f else 0f
            imgToWordGap = if (hasImage) 24f else 20f
            wordToMetaGap = if (hasMeta) 12f else 0f
            metaToDivGap = 18f
            divToMeaningGap = 22f
            bottomPad = 26f

            // Step 2: Reduce image height moderately if present
            if (hasImage) {
                val imgReduction = (overflow * 0.70f).coerceAtMost(imgScaledH - 260f)
                if (imgReduction > 0) {
                    val newMaxH = maxImgH - imgReduction
                    val scale = (maxImgW / model.imageBitmap!!.width).coerceAtMost(newMaxH / model.imageBitmap.height)
                    imgScaledW = model.imageBitmap.width * scale
                    imgScaledH = model.imageBitmap.height * scale
                }
            }

            // Step 3: Moderately reduce metadata
            if (hasMeta) {
                metaSize = 26f
                metaPaint.textSize = metaSize
            }

            totalContentHeight = topPad + tagHeight + tagToImgGap + imgScaledH +
                    imgToWordGap + headwordHeight + wordToMetaGap + metaHeight +
                    metaToDivGap + divHeight + divToMeaningGap + meaningHeight + bottomPad

            // Step 4: Reduce Vietnamese meaning if still overflowing
            if (totalContentHeight > maxCardHeight && currentMeaningSize > 26f) {
                val remainingOverflow = totalContentHeight - maxCardHeight
                val meaningReduction = (remainingOverflow * 0.4f).coerceAtMost(currentMeaningSize - 26f)
                currentMeaningSize -= meaningReduction
                meaningPaint.textSize = currentMeaningSize
                staticLayout = StaticLayout.Builder.obtain(
                    model.meaning, 0, model.meaning.length, meaningPaint, textWidth
                ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(3)
                    .build()
                meaningHeight = staticLayout.height.toFloat()

                totalContentHeight = topPad + tagHeight + tagToImgGap + imgScaledH +
                        imgToWordGap + headwordHeight + wordToMetaGap + metaHeight +
                        metaToDivGap + divHeight + divToMeaningGap + meaningHeight + bottomPad
            }
        }

        // --- 7. Calculate Card Coordinates ---
        val cardHeight = totalContentHeight.coerceAtMost(maxCardHeight)
        val cardTop = safe.top + ((safe.height - cardHeight) / 2f)
        val cardBottom = cardTop + cardHeight
        val cardRect = RectF(safe.left.toFloat(), cardTop, safe.right.toFloat(), cardBottom)
        val centerX = cardRect.centerX()

        // --- 8. Draw Card Background with User-Controlled Opacity ---
        val cardAlpha = (model.cardBackgroundOpacity.coerceIn(0.20f, 1.00f) * 255f).roundToInt().coerceIn(51, 255)
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (cardAlpha shl 24) or 0x00111827
            style = Paint.Style.FILL
        }
        val borderAlpha = (cardAlpha * 0.85f).roundToInt().coerceIn(40, 255)
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (borderAlpha shl 24) or 0x00374151
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorderPaint)

        // --- 9. Draw Elements at Content-Driven Positions ---
        var currentY = cardTop + topPad

        // Tag
        currentY += tagHeight
        canvas.drawText(tagText, centerX, currentY, tagPaint)

        // Image
        if (hasImage) {
            currentY += tagToImgGap
            val imgLeft = centerX - (imgScaledW / 2f)
            val imgTop = currentY
            val imgRect = RectF(imgLeft, imgTop, imgLeft + imgScaledW, imgTop + imgScaledH)

            val clipPath = Path().apply {
                addRoundRect(imgRect, 18f, 18f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(model.imageBitmap!!, null, imgRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()

            val imgBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (borderAlpha shl 24) or 0x00374151
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(imgRect, 18f, 18f, imgBorderPaint)

            currentY += imgScaledH
        }

        // Headword (Pure text, Centered)
        currentY += imgToWordGap
        val wordBaselineY = currentY + (headwordHeight * 0.78f)
        canvas.drawText(model.headword, centerX, wordBaselineY, wordPaint)
        currentY += headwordHeight

        // IPA / POS
        if (hasMeta) {
            currentY += wordToMetaGap
            val metaBaselineY = currentY + (metaHeight * 0.78f)
            canvas.drawText(metaText, centerX, metaBaselineY, metaPaint)
            currentY += metaHeight
        }

        // Divider
        currentY += metaToDivGap
        val divWidth = (availableWidth * 0.40f).coerceAtMost(360f)
        val divLeft = centerX - (divWidth / 2f)
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (borderAlpha shl 24) or 0x004B5563
            strokeWidth = divHeight
            style = Paint.Style.STROKE
        }
        canvas.drawLine(divLeft, currentY, divLeft + divWidth, currentY, divPaint)
        currentY += divHeight

        // Meaning
        currentY += divToMeaningGap
        canvas.save()
        val meaningLeft = centerX - (textWidth / 2f)
        canvas.translate(meaningLeft, currentY)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawUserBackground(canvas: Canvas, bitmap: Bitmap, screenWidth: Int, screenHeight: Int) {
        val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
        val srcH = bitmap.height.toFloat().coerceAtLeast(1f)
        val dstW = screenWidth.toFloat()
        val dstH = screenHeight.toFloat()

        // CENTER_CROP
        val scale = (dstW / srcW).coerceAtLeast(dstH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val left = (dstW - scaledW) / 2f
        val top = (dstH - scaledH) / 2f

        val dstRect = RectF(left, top, left + scaledW, top + scaledH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, dstRect, paint)

        // Scrim overlay (28% black)
        val scrimPaint = Paint().apply {
            color = 0x47000000.toInt()
        }
        canvas.drawRect(0f, 0f, dstW, dstH, scrimPaint)
    }

    private fun drawDefaultBackground(canvas: Canvas, screenWidth: Int, screenHeight: Int) {
        val darkBg = 0xFF0D1117.toInt()
        val bgPaint = Paint().apply { color = darkBg }
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), bgPaint)

        // Subtle geometric mountain shapes
        val polyPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF161B22.toInt()
            style = Paint.Style.FILL
        }
        val path1 = Path().apply {
            moveTo(0f, screenHeight.toFloat())
            lineTo(0f, screenHeight * 0.55f)
            lineTo(screenWidth * 0.45f, screenHeight * 0.38f)
            lineTo(screenWidth.toFloat(), screenHeight * 0.65f)
            lineTo(screenWidth.toFloat(), screenHeight.toFloat())
            close()
        }
        canvas.drawPath(path1, polyPaint1)

        val polyPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF21262D.toInt()
            style = Paint.Style.FILL
        }
        val path2 = Path().apply {
            moveTo(0f, screenHeight.toFloat())
            lineTo(0f, screenHeight * 0.70f)
            lineTo(screenWidth * 0.65f, screenHeight * 0.48f)
            lineTo(screenWidth.toFloat(), screenHeight * 0.58f)
            lineTo(screenWidth.toFloat(), screenHeight.toFloat())
            close()
        }
        canvas.drawPath(path2, polyPaint2)

        // Subtle moon accent
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66D97706.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(screenWidth * 0.5f, screenHeight * 0.34f, 160f, circlePaint)
    }
}
