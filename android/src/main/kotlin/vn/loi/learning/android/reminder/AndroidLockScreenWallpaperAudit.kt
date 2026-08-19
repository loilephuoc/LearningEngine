package vn.loi.learning.android.reminder

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.Log

object AndroidLockScreenWallpaperAudit {
    private const val TAG = "WallpaperAudit"

    fun createSampleImage(width: Int, height: Int, label: String, bgInt: Int = 0xFF3B82F6.toInt()): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply { color = bgInt }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(3f, 3f, width - 3f, height - 3f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = (height * 0.12f).coerceIn(20f, 44f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(label, width / 2f, height / 2f, textPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE0E7FF.toInt()
            textSize = (height * 0.08f).coerceIn(16f, 28f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Embedded Context / Minh Họa", width / 2f, height / 2f + 40f, subPaint)

        return bitmap
    }

    fun applyTestCase(
        context: Context,
        caseId: String,
        wordSize: LockWallpaperWordSize = LockWallpaperWordSize.EXTRA_LARGE,
        vietnameseSize: LockWallpaperVietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
        imageSize: LockWallpaperImageSize = LockWallpaperImageSize.EXTRA_LARGE,
        cardBackgroundOpacity: Float = 0.72f
    ): Boolean {
        val model = when (caseId) {
            "CASE_VN_SMALL" -> LockWallpaperVocabularyRenderModel(
                headword = "sustainable",
                ipa = "səˈsteɪ.nə.bəl",
                partOfSpeech = "adjective",
                meaning = "Bền vững",
                imageBitmap = createSampleImage(500, 500, "Sustainable 1:1", 0xFF2563EB.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.SMALL,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = cardBackgroundOpacity
            )
            "CASE_VN_MEDIUM" -> LockWallpaperVocabularyRenderModel(
                headword = "sustainable",
                ipa = "səˈsteɪ.nə.bəl",
                partOfSpeech = "adjective",
                meaning = "Bền vững",
                imageBitmap = createSampleImage(500, 500, "Sustainable 1:1", 0xFF2563EB.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = cardBackgroundOpacity
            )
            "CASE_VN_LARGE" -> LockWallpaperVocabularyRenderModel(
                headword = "sustainable",
                ipa = "səˈsteɪ.nə.bəl",
                partOfSpeech = "adjective",
                meaning = "Bền vững",
                imageBitmap = createSampleImage(500, 500, "Sustainable 1:1", 0xFF2563EB.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.LARGE,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = cardBackgroundOpacity
            )
            "CASE_VN_XL" -> LockWallpaperVocabularyRenderModel(
                headword = "sustainable",
                ipa = "səˈsteɪ.nə.bəl",
                partOfSpeech = "adjective",
                meaning = "Bền vững",
                imageBitmap = createSampleImage(500, 500, "Sustainable 1:1", 0xFF2563EB.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.EXTRA_LARGE,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = cardBackgroundOpacity
            )
            "CASE_VN_HUGE" -> LockWallpaperVocabularyRenderModel(
                headword = "sustainable",
                ipa = "səˈsteɪ.nə.bəl",
                partOfSpeech = "adjective",
                meaning = "Bền vững",
                imageBitmap = createSampleImage(500, 500, "Sustainable 1:1", 0xFF2563EB.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.HUGE,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = cardBackgroundOpacity
            )
            "CASE_OPACITY_20" -> LockWallpaperVocabularyRenderModel(
                headword = "breakthrough",
                ipa = "ˈbreɪk.θruː",
                partOfSpeech = "noun",
                meaning = "Bước đột phá quan trọng",
                imageBitmap = createSampleImage(500, 500, "Breakthrough 1:1", 0xFF7C3AED.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = 0.20f
            )
            "CASE_OPACITY_40" -> LockWallpaperVocabularyRenderModel(
                headword = "breakthrough",
                ipa = "ˈbreɪk.θruː",
                partOfSpeech = "noun",
                meaning = "Bước đột phá quan trọng",
                imageBitmap = createSampleImage(500, 500, "Breakthrough 1:1", 0xFF7C3AED.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = 0.40f
            )
            "CASE_OPACITY_70" -> LockWallpaperVocabularyRenderModel(
                headword = "breakthrough",
                ipa = "ˈbreɪk.θruː",
                partOfSpeech = "noun",
                meaning = "Bước đột phá quan trọng",
                imageBitmap = createSampleImage(500, 500, "Breakthrough 1:1", 0xFF7C3AED.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = 0.70f
            )
            "CASE_OPACITY_100" -> LockWallpaperVocabularyRenderModel(
                headword = "breakthrough",
                ipa = "ˈbreɪk.θruː",
                partOfSpeech = "noun",
                meaning = "Bước đột phá quan trọng",
                imageBitmap = createSampleImage(500, 500, "Breakthrough 1:1", 0xFF7C3AED.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = 1.00f
            )
            "CASE_EXTREME_HUGE_MAX" -> LockWallpaperVocabularyRenderModel(
                headword = "comprehensibility",
                ipa = "ˌkɒm.prɪˌhen.səˈbɪl.ə.ti",
                partOfSpeech = "noun",
                meaning = "Khả năng thấu hiểu và nắm bắt toàn diện một vấn đề phức tạp",
                imageBitmap = createSampleImage(500, 500, "Extreme 1:1", 0xFF059669.toInt()),
                hasPrimaryAudio = true,
                wordSize = LockWallpaperWordSize.HUGE,
                vietnameseSize = LockWallpaperVietnameseSize.HUGE,
                imageSize = LockWallpaperImageSize.MAXIMUM,
                cardBackgroundOpacity = cardBackgroundOpacity
            )
            else -> {
                Log.w(TAG, "Unknown test case: $caseId")
                return false
            }
        }

        return try {
            val bitmap = AndroidLockScreenWallpaperRenderer.renderVocabularyWallpaper(model)
            val wm = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
            } else {
                wm.setBitmap(bitmap)
            }
            bitmap.recycle()
            model.imageBitmap?.recycle()
            Log.i(TAG, "Successfully applied matrix test case: $caseId with wordSize=$wordSize vietnameseSize=$vietnameseSize imageSize=$imageSize opacity=$cardBackgroundOpacity")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply matrix test case: $caseId", e)
            false
        }
    }
}
