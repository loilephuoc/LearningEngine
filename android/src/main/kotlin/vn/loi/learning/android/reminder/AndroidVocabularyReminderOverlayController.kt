package vn.loi.learning.android.reminder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import vn.loi.learning.android.BuildConfig
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

interface AndroidVocabularyReminderOverlayPresenter {
    fun show(
        candidate: AndroidVocabularyCandidate,
        mode: AndroidVocabularyReminderSelectionMode,
        displayDurationMillis: Long,
        onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)? = null
    ): Boolean

    fun hide()
    val isShowing: Boolean
    fun shutdown()
}

interface AndroidVocabularyReminderDeviceStateProvider {
    fun isOverlayPermissionGranted(): Boolean
    fun isScreenInteractive(): Boolean
    fun isDeviceLocked(): Boolean
}

class DefaultAndroidVocabularyReminderDeviceStateProvider(
    private val context: Context
) : AndroidVocabularyReminderDeviceStateProvider {
    override fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    override fun isScreenInteractive(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return powerManager?.isInteractive ?: true
    }

    override fun isDeviceLocked(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isKeyguard = keyguardManager?.isKeyguardLocked ?: false
        val isDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            keyguardManager?.isDeviceLocked ?: false
        } else {
            false
        }
        Log.d("VocabularyReminderOverlay", "DeviceState check: isKeyguardLocked=$isKeyguard, isDeviceLocked=$isDevice")
        return isKeyguard
    }
}

class AndroidVocabularyReminderOverlayController(
    private val context: Context,
    private val resolveMedia: (String) -> String? = { null }
) : AndroidVocabularyReminderOverlayPresenter {

    private val windowManager: WindowManager? = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentView: View? = null
    private var autoDismissRunnable: Runnable? = null
    private val showing = AtomicBoolean(false)

    override val isShowing: Boolean
        get() = showing.get()

    override fun show(
        candidate: AndroidVocabularyCandidate,
        mode: AndroidVocabularyReminderSelectionMode,
        displayDurationMillis: Long,
        onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)?
    ): Boolean {
        if (windowManager == null) return false

        // Run on Main thread synchronously if already on main, or dispatch and block briefly
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return performShow(candidate, mode, displayDurationMillis, onReview)
        }

        var result = false
        val latch = java.util.concurrent.CountDownLatch(1)
        mainHandler.post {
            try {
                result = performShow(candidate, mode, displayDurationMillis, onReview)
            } finally {
                latch.countDown()
            }
        }
        runCatching {
            latch.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
        return result
    }

    private fun performShow(
        candidate: AndroidVocabularyCandidate,
        mode: AndroidVocabularyReminderSelectionMode,
        displayDurationMillis: Long,
        onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)?
    ): Boolean {
        performHide()

        val view = try {
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.overlay_vocabulary_reminder, null)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to inflate overlay layout", e)
            return false
        }

        val primaryTextView = view.findViewById<TextView>(R.id.overlay_primary_text)
        val metadataRow = view.findViewById<LinearLayout>(R.id.overlay_metadata_row)
        val ipaTextView = view.findViewById<TextView>(R.id.overlay_ipa)
        val posTextView = view.findViewById<TextView>(R.id.overlay_pos)
        val meaningTextView = view.findViewById<TextView>(R.id.overlay_meaning)
        val thumbnailImageView = view.findViewById<ImageView>(R.id.overlay_thumbnail)
        val closeButton = view.findViewById<ImageView>(R.id.overlay_close_button)

        primaryTextView.text = candidate.primaryText

        val ipa = candidate.ipa?.trim()?.takeIf { it.isNotBlank() }
        if (ipa != null) {
            ipaTextView.text = "/$ipa/"
            ipaTextView.visibility = View.VISIBLE
        } else {
            ipaTextView.visibility = View.GONE
        }

        val pos = candidate.partOfSpeech?.trim()?.takeIf { it.isNotBlank() }
        if (pos != null) {
            posTextView.text = pos
            posTextView.visibility = View.VISIBLE
        } else {
            posTextView.visibility = View.GONE
        }

        if (ipa != null || pos != null) {
            metadataRow.visibility = View.VISIBLE
        } else {
            metadataRow.visibility = View.GONE
        }

        val meaning = candidate.translation?.takeIf { it.isNotBlank() }
            ?: candidate.answer?.takeIf { it.isNotBlank() }
            ?: ""
        meaningTextView.text = meaning

        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels
        val horizontalMarginPx = (10 * density).toInt()
        val popupWidth = screenWidth - (horizontalMarginPx * 2)
        val statusBarHeight = getStatusBarHeight(context)
        val topMarginPx = statusBarHeight + (10 * density).toInt()

        val targetImageWidthPx = (popupWidth * 0.40f).toInt().coerceIn((145 * density).toInt(), (180 * density).toInt())
        val targetImageHeightPx = (165 * density).toInt()

        val resolvedPath = candidate.imageReference?.let(resolveMedia)
        val bitmap = resolvedPath?.let { path ->
            decodeOverlayImage(candidate.contentId.value, candidate.imageReference, path)
        }
        val roundedBitmap = bitmap?.let { createRoundedCornerBitmap(it, 16f * density) }

        if (roundedBitmap != null) {
            val lp = thumbnailImageView.layoutParams
            lp.width = targetImageWidthPx
            lp.height = targetImageHeightPx
            thumbnailImageView.layoutParams = lp
            thumbnailImageView.setImageBitmap(roundedBitmap)
            thumbnailImageView.visibility = View.VISIBLE
        } else {
            thumbnailImageView.visibility = View.GONE
        }

        view.setOnClickListener {
            performHide()
            if (onReview != null) {
                onReview(candidate.packageId.value, candidate.contentId.value, mode)
            } else {
                launchReviewScreen(candidate, mode)
            }
        }

        closeButton.setOnClickListener {
            performHide()
        }

        val layoutParams = WindowManager.LayoutParams(
            popupWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = topMarginPx
        }

        Log.i(TAG, "performShow: inflating overlay layout for candidate '${candidate.primaryText}' (popupWidth=$popupWidth, topMarginPx=$topMarginPx, imageSize=${targetImageWidthPx}x${targetImageHeightPx})")

        return try {
            windowManager?.addView(view, layoutParams)
            currentView = view
            showing.set(true)

            val dismissRunnable = Runnable {
                performHide()
            }
            autoDismissRunnable = dismissRunnable
            mainHandler.postDelayed(dismissRunnable, displayDurationMillis)

            Log.i(TAG, "performShow SUCCESS: WindowManager.addView completed for '${candidate.primaryText}' (duration=${displayDurationMillis}ms)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "performShow FAILED: WindowManager.addView threw exception: ${e.javaClass.name}: ${e.message}", e)
            currentView = null
            showing.set(false)
            false
        }
    }

    override fun hide() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            performHide()
        } else {
            mainHandler.post { performHide() }
        }
    }

    private fun performHide() {
        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        autoDismissRunnable = null

        val view = currentView
        if (view != null) {
            currentView = null
            showing.set(false)
            runCatching {
                windowManager?.removeView(view)
            }.onFailure { e ->
                Log.w(TAG, "Error removing overlay view", e)
            }
        } else {
            showing.set(false)
        }
    }

    override fun shutdown() {
        hide()
    }

    private fun launchReviewScreen(candidate: AndroidVocabularyCandidate, mode: AndroidVocabularyReminderSelectionMode) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = AndroidVocabularyReminderNotificationHelper.ACTION_REMINDER_REVIEW
            putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_PACKAGE_ID, candidate.packageId.value)
            putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_CONTENT_ID, candidate.contentId.value)
            putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_REMINDER_MODE, mode.name)
            putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_NOTIFICATION_ID, AndroidVocabularyReminderNotificationHelper.REMINDER_NOTIFICATION_ID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun decodeOverlayImage(contentId: String, rawRef: String, resolvedPath: String?): Bitmap? {
        val file = resolvedPath?.let(::File)
        if (file == null || !file.exists() || !file.isFile) return null

        return runCatching {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val maxDimension = OVERLAY_IMAGE_MAX_DIMENSION
            var inSampleSize = 1
            if (boundsOptions.outHeight > maxDimension || boundsOptions.outWidth > maxDimension) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        }.getOrNull()
    }

    private fun createRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
        return runCatching {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            output
        }.getOrDefault(bitmap)
    }

    private fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            (24 * context.resources.displayMetrics.density).toInt()
        }
    }

    companion object {
        private const val TAG = "VocabularyReminderOverlay"
        const val OVERLAY_IMAGE_MAX_DIMENSION = 720
    }
}
