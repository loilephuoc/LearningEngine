package vn.loi.learning.android.reminder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import vn.loi.learning.android.R

object AndroidVocabularyReminderOverlayAudit {
    private const val TAG = "OverlayAudit"

    fun renderAndSaveOverlay(
        context: Context,
        headword: String,
        ipa: String?,
        pos: String?,
        meaning: String,
        sampleImage: Bitmap?,
        outputFilePath: String
    ): Boolean {
        return runCatching {
            val density = context.resources.displayMetrics.density
            val screenWidth = 1220
            val screenHeight = 2712
            val horizontalMarginPx = (10 * density).toInt()
            val popupWidth = screenWidth - (horizontalMarginPx * 2)

            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.overlay_vocabulary_reminder, null)

            val primaryTextView = view.findViewById<TextView>(R.id.overlay_primary_text)
            val metadataRow = view.findViewById<LinearLayout>(R.id.overlay_metadata_row)
            val ipaTextView = view.findViewById<TextView>(R.id.overlay_ipa)
            val posTextView = view.findViewById<TextView>(R.id.overlay_pos)
            val meaningTextView = view.findViewById<TextView>(R.id.overlay_meaning)
            val thumbnailImageView = view.findViewById<ImageView>(R.id.overlay_thumbnail)
            val quickPauseRow = view.findViewById<LinearLayout>(R.id.overlay_quick_pause_row)

            val isExtremelyLongContent = headword.length > 20 || meaning.length > 45

            val imageWidthPx = if (sampleImage != null) {
                when {
                    isExtremelyLongContent -> (popupWidth * 0.30f).toInt().coerceIn((100 * density).toInt(), (125 * density).toInt())
                    headword.length > 14 || meaning.length > 28 -> (popupWidth * 0.35f).toInt().coerceIn((120 * density).toInt(), (145 * density).toInt())
                    else -> (popupWidth * 0.40f).toInt().coerceIn((135 * density).toInt(), (165 * density).toInt())
                }
            } else {
                0
            }
            val imageHeightPx = if (sampleImage != null) (145 * density).toInt() else 0

            if (sampleImage != null) {
                val rounded = createRoundedCornerBitmap(sampleImage, 6f * density)
                val lp = thumbnailImageView.layoutParams
                lp.width = imageWidthPx
                lp.height = imageHeightPx
                thumbnailImageView.layoutParams = lp
                thumbnailImageView.setImageBitmap(rounded)
                thumbnailImageView.visibility = View.VISIBLE
            } else {
                thumbnailImageView.visibility = View.GONE
            }

            val textContainerMarginStart = if (sampleImage != null) (12 * density).toInt() else 0
            val textContainerMarginEnd = (12 * density).toInt()
            val cardInnerPadding = (28 * density).toInt()
            val availableTextWidthPx = (popupWidth - cardInnerPadding - imageWidthPx - textContainerMarginStart - textContainerMarginEnd).coerceAtLeast((150 * density).toInt())

            val englishSizeSp = autoFitEnglish(headword, availableTextWidthPx, density)
            primaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, englishSizeSp)
            primaryTextView.text = headword

            if (ipa != null || pos != null) {
                metadataRow.visibility = View.VISIBLE
                if (ipa != null) {
                    ipaTextView.text = "/$ipa/"
                    ipaTextView.visibility = View.VISIBLE
                    val ipaSizeSp = if (ipa.length > 22 || availableTextWidthPx < (200 * density).toInt()) 11.5f else 13f
                    ipaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ipaSizeSp)
                } else {
                    ipaTextView.visibility = View.GONE
                }
                if (pos != null) {
                    posTextView.text = pos.uppercase()
                    posTextView.visibility = View.VISIBLE
                } else {
                    posTextView.visibility = View.GONE
                }
            } else {
                metadataRow.visibility = View.GONE
            }

            val vnSizeSp = autoFitVietnamese(meaning, availableTextWidthPx, density)
            meaningTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, vnSizeSp)
            meaningTextView.text = meaning

            quickPauseRow.visibility = View.VISIBLE
            val countdownTrack = view.findViewById<View>(R.id.overlay_countdown_track)
            val countdownProgress = view.findViewById<View>(R.id.overlay_countdown_progress)
            countdownTrack?.visibility = View.VISIBLE
            countdownProgress?.pivotX = 0f
            countdownProgress?.scaleX = 0.75f

            // Measure & Layout
            view.measure(
                View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupHeight = view.measuredHeight
            view.layout(0, 0, popupWidth, popupHeight)

            // Compose full screen with Dark translucent background to simulate device
            val fullScreenBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fullScreenBitmap)

            // Background wallpaper tone
            val bgPaint = Paint().apply { color = 0xFF12141A.toInt() }
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), bgPaint)

            // Draw status bar simulation at top
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF94A3B8.toInt()
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("09:41", 60f, 100f, statusPaint)

            // Draw overlay card centered at Y = 160px
            canvas.save()
            canvas.translate(horizontalMarginPx.toFloat(), 160f)
            view.draw(canvas)
            canvas.restore()

            val file = File(outputFilePath)
            FileOutputStream(file).use { out ->
                fullScreenBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.i(TAG, "Saved overlay audit to $outputFilePath")
            true
        }.getOrElse { e ->
            Log.e(TAG, "Failed to render overlay audit", e)
            false
        }
    }

    private fun autoFitEnglish(text: String, availableWidthPx: Int, density: Float): Float {
        val candidateSizes = listOf(28f, 25f, 22f, 19f, 17f, 15f)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
        }
        for (size in candidateSizes) {
            paint.textSize = size * density
            if (paint.measureText(text) <= availableWidthPx) {
                return size
            }
        }
        val multilineSizes = listOf(22f, 19f, 17f, 15f)
        for (size in multilineSizes) {
            paint.textSize = size * density
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidthPx)
                    .setMaxLines(2)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, paint, availableWidthPx, android.text.Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            }
            if (staticLayout.lineCount <= 2) {
                return size
            }
        }
        return 15f
    }

    private fun autoFitVietnamese(text: String, availableWidthPx: Int, density: Float): Float {
        val candidateSizes = listOf(19f, 17f, 15f, 13.5f, 12f)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
        }
        for (size in candidateSizes) {
            paint.textSize = size * density
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidthPx)
                    .setMaxLines(3)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, paint, availableWidthPx, android.text.Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            }
            if (staticLayout.lineCount <= 3) {
                return size
            }
        }
        return 12f
    }

    private fun createRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
        return runCatching {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            val rectF = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            output
        }.getOrDefault(bitmap)
    }
}
