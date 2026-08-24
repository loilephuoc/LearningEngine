package vn.loi.learning.android.reminder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

enum class OverlayState {
    HIDDEN,
    ENTERING,
    VISIBLE,
    EXITING
}

enum class OverlayExitMode {
    SIMPLE_FADE,
    IMMEDIATE
}

interface AndroidVocabularyReminderOverlayPresenter {
    val isShowing: Boolean
    fun show(
        candidate: AndroidVocabularyCandidate,
        mode: AndroidVocabularyReminderSelectionMode,
        displayDurationMillis: Long = 5000L,
        onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)? = null,
        onQuickPause: ((action: ReminderQuickPauseAction) -> Unit)? = null,
        onDismissed: ((reason: String) -> Unit)? = null
    ): Boolean
    fun hide()
    fun shutdown()
    fun isOverlayPermissionGranted(): Boolean = true
}

interface AndroidVocabularyReminderDeviceStateProvider {
    fun isScreenOn(): Boolean
    fun isDeviceLocked(): Boolean
    fun isOverlayPermissionGranted(): Boolean = true
}

typealias DefaultAndroidVocabularyReminderDeviceStateProvider = SystemVocabularyReminderDeviceStateProvider

class SystemVocabularyReminderDeviceStateProvider(
    private val context: Context
) : AndroidVocabularyReminderDeviceStateProvider {
    override fun isScreenOn(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return powerManager?.isInteractive ?: false
    }

    override fun isDeviceLocked(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        return keyguardManager?.isKeyguardLocked ?: false
    }

    override fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}

class AndroidVocabularyReminderOverlayController(
    private val context: Context,
    private val resolveMedia: (String) -> String? = { null },
    private val defaultQuickPauseHandler: ((ReminderQuickPauseAction) -> Unit)? = null
) : AndroidVocabularyReminderOverlayPresenter {

    private val windowManager: WindowManager? = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentView: View? = null
    private var autoDismissRunnable: Runnable? = null
    private var currentCountdownAnimator: ViewPropertyAnimator? = null
    private var currentOnDismissed: ((String) -> Unit)? = null
    private var currentInstanceId: Long = 0L
    private val showing = AtomicBoolean(false)
    var state: OverlayState = OverlayState.HIDDEN
        private set

    override val isShowing: Boolean
        get() = showing.get()

    override fun show(
        candidate: AndroidVocabularyCandidate,
        mode: AndroidVocabularyReminderSelectionMode,
        displayDurationMillis: Long,
        onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)?,
        onQuickPause: ((action: ReminderQuickPauseAction) -> Unit)?,
        onDismissed: ((reason: String) -> Unit)?
    ): Boolean {
        if (windowManager == null) return false

        val effectivePauseHandler = onQuickPause ?: defaultQuickPauseHandler

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return performShow(candidate, mode, displayDurationMillis, onReview, effectivePauseHandler, onDismissed)
        }

        var result = false
        val latch = java.util.concurrent.CountDownLatch(1)
        mainHandler.post {
            try {
                result = performShow(candidate, mode, displayDurationMillis, onReview, effectivePauseHandler, onDismissed)
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
        onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)?,
        onQuickPause: ((action: ReminderQuickPauseAction) -> Unit)?,
        onDismissed: ((reason: String) -> Unit)?
    ): Boolean {
        // Cancel in-flight animations and cleanly remove prior view if re-entered
        performHide(animated = false, reason = "REPLACE_EXISTING")
        val instanceId = ++currentInstanceId
        currentOnDismissed = onDismissed

        val view = try {
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.overlay_vocabulary_reminder, null)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to inflate overlay layout", e)
            return false
        }

        val cardRoot = view.findViewById<View>(R.id.overlay_card_root) ?: view
        val primaryTextView = view.findViewById<TextView>(R.id.overlay_primary_text)
        val metadataRow = view.findViewById<LinearLayout>(R.id.overlay_metadata_row)
        val ipaTextView = view.findViewById<TextView>(R.id.overlay_ipa)
        val posTextView = view.findViewById<TextView>(R.id.overlay_pos)
        val meaningTextView = view.findViewById<TextView>(R.id.overlay_meaning)
        val thumbnailImageView = view.findViewById<ImageView>(R.id.overlay_thumbnail)
        val closeContainer = view.findViewById<View>(R.id.overlay_close_container)
        val closeButton = view.findViewById<ImageView>(R.id.overlay_close_button)
        val quickPauseRow = view.findViewById<LinearLayout>(R.id.overlay_quick_pause_row)
        val pause5mBtn = view.findViewById<View>(R.id.overlay_pause_5m)
        val togglePauseBtn = view.findViewById<View>(R.id.overlay_toggle_pause)
        val pauseIcon = view.findViewById<ImageView>(R.id.overlay_pause_icon)
        val toggleMuteBtn = view.findViewById<View>(R.id.overlay_toggle_mute)
        val muteIcon = view.findViewById<ImageView>(R.id.overlay_mute_icon)
        val countdownTrack = view.findViewById<FrameLayout>(R.id.overlay_countdown_track)
        val countdownProgress = view.findViewById<View>(R.id.overlay_countdown_progress)

        var isCountdownPaused = false
        var countdownRemainingMillis = displayDurationMillis
        var countdownStartedMillis = 0L

        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels
        val horizontalMarginPx = (10 * density).toInt()
        val popupWidth = screenWidth - (horizontalMarginPx * 2)
        val statusBarHeight = getStatusBarHeight(context)
        val topMarginPx = statusBarHeight + (10 * density).toInt()

        // 1. Resolve & Decode Image
        val resolvedPath = candidate.imageReference?.let(resolveMedia)
        val rawBitmap = resolvedPath?.let { path ->
            decodeOverlayImage(candidate.contentId.value, candidate.imageReference, path)
        }

        // 2. Assess Content Length for Adaptive Column Sizing
        val headword = candidate.primaryText.trim()
        val ipaText = candidate.ipa?.trim()?.takeIf { it.isNotBlank() }
        val posText = candidate.partOfSpeech?.trim()?.takeIf { it.isNotBlank() }
        val meaningText = (candidate.translation?.trim()?.takeIf { it.isNotBlank() }
            ?: candidate.answer?.trim()?.takeIf { it.isNotBlank() }
            ?: "").trim()

        val isExtremelyLongContent = headword.length > 20 || meaningText.length > 45

        // Dynamic Image Width allocation based on text pressure
        val imageWidthPx = if (rawBitmap != null) {
            val baseImageWidth = when {
                isExtremelyLongContent -> (popupWidth * 0.30f).toInt().coerceIn((100 * density).toInt(), (125 * density).toInt())
                headword.length > 14 || meaningText.length > 28 -> (popupWidth * 0.35f).toInt().coerceIn((120 * density).toInt(), (145 * density).toInt())
                else -> (popupWidth * 0.40f).toInt().coerceIn((135 * density).toInt(), (165 * density).toInt())
            }
            baseImageWidth
        } else {
            0
        }
        val imageHeightPx = if (rawBitmap != null) (145 * density).toInt() else 0

        if (rawBitmap != null) {
            val roundedBitmap = createRoundedCornerBitmap(rawBitmap, 6f * density)
            val lp = thumbnailImageView.layoutParams
            lp.width = imageWidthPx
            lp.height = imageHeightPx
            thumbnailImageView.layoutParams = lp
            thumbnailImageView.setImageBitmap(roundedBitmap)
            thumbnailImageView.visibility = View.VISIBLE
        } else {
            thumbnailImageView.visibility = View.GONE
        }

        // 3. Compute Available Text Column Width
        val textContainerMarginStart = if (rawBitmap != null) (12 * density).toInt() else 0
        val textContainerMarginEnd = (12 * density).toInt()
        val cardInnerPadding = (28 * density).toInt()
        val availableTextWidthPx = (popupWidth - cardInnerPadding - imageWidthPx - textContainerMarginStart - textContainerMarginEnd).coerceAtLeast((150 * density).toInt())

        // 4. Auto-Fit English Headword (Pre-resolved before attachment)
        val englishFit = autoFitEnglishHeadword(headword, availableTextWidthPx, density)
        primaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, englishFit.textSizeSp)
        primaryTextView.maxLines = englishFit.maxLines
        primaryTextView.text = headword

        // 5. Auto-Fit IPA + POS (Pre-resolved before attachment)
        if (ipaText != null || posText != null) {
            metadataRow.visibility = View.VISIBLE
            if (posText != null) {
                posTextView.text = posText.uppercase()
                posTextView.isSingleLine = true
                posTextView.maxLines = 1
                posTextView.visibility = View.VISIBLE
            } else {
                posTextView.visibility = View.GONE
            }

            if (ipaText != null) {
                ipaTextView.text = "/$ipaText/"
                ipaTextView.isSingleLine = true
                ipaTextView.maxLines = 1
                ipaTextView.visibility = View.VISIBLE
                val ipaSizeSp = if (ipaText.length > 22 || availableTextWidthPx < (200 * density).toInt()) 11.5f else 13f
                ipaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ipaSizeSp)
            } else {
                ipaTextView.visibility = View.GONE
            }
        } else {
            metadataRow.visibility = View.GONE
        }

        // 6. Auto-Fit Vietnamese Meaning (Pre-resolved before attachment)
        val vietnameseSizeSp = autoFitVietnameseMeaning(meaningText, availableTextWidthPx, density)
        meaningTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, vietnameseSizeSp)
        meaningTextView.maxLines = 3
        meaningTextView.text = meaningText

        // 7. Compact Quick Pause & Audio Controls
        quickPauseRow.visibility = View.VISIBLE
        pause5mBtn.setOnClickListener {
            requestDismiss(instanceId, "USER_PAUSE_5M") {
                Log.i(TAG, "[UnlockedPause] duration=5m userAction=TAP")
                onQuickPause?.invoke(ReminderQuickPauseAction.ForDuration(java.time.Duration.ofMinutes(5)))
            }
        }
        pause5mBtn.setOnLongClickListener {
            autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
            currentCountdownAnimator?.cancel()

            val wrapper = android.view.ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            val popup = android.widget.PopupMenu(wrapper, pause5mBtn)
            popup.menu.add(0, 1, 0, context.getString(R.string.reminder_pause_option_30m))
            popup.menu.add(0, 2, 1, context.getString(R.string.reminder_pause_option_1h))
            popup.menu.add(0, 3, 2, context.getString(R.string.reminder_pause_option_4h))
            popup.menu.add(0, 4, 3, context.getString(R.string.reminder_pause_option_indefinite))
            popup.menu.add(0, 5, 4, context.getString(R.string.action_cancel))
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        requestDismiss(instanceId, "USER_PAUSE_30M") {
                            Log.i(TAG, "[UnlockedPause] duration=30m userAction=LONG_PRESS_MENU")
                            onQuickPause?.invoke(ReminderQuickPauseAction.ForDuration(java.time.Duration.ofMinutes(30)))
                        }
                        true
                    }
                    2 -> {
                        requestDismiss(instanceId, "USER_PAUSE_1H") {
                            Log.i(TAG, "[UnlockedPause] duration=1h userAction=LONG_PRESS_MENU")
                            onQuickPause?.invoke(ReminderQuickPauseAction.ForDuration(java.time.Duration.ofHours(1)))
                        }
                        true
                    }
                    3 -> {
                        requestDismiss(instanceId, "USER_PAUSE_4H") {
                            Log.i(TAG, "[UnlockedPause] duration=4h userAction=LONG_PRESS_MENU")
                            onQuickPause?.invoke(ReminderQuickPauseAction.ForDuration(java.time.Duration.ofHours(4)))
                        }
                        true
                    }
                    4 -> {
                        requestDismiss(instanceId, "USER_PAUSE_INDEFINITE") {
                            Log.i(TAG, "[UnlockedPause] duration=INDEFINITE userAction=LONG_PRESS_MENU")
                            onQuickPause?.invoke(ReminderQuickPauseAction.Indefinitely)
                        }
                        true
                    }
                    else -> {
                        val remainingMs = 3000L
                        val dismissRunnable = Runnable {
                            if (state == OverlayState.VISIBLE && currentView == view && currentInstanceId == instanceId) {
                                requestDismiss(instanceId, "AUTO_DISMISS")
                            }
                        }
                        autoDismissRunnable = dismissRunnable
                        mainHandler.postDelayed(dismissRunnable, remainingMs)
                        true
                    }
                }
            }
            popup.setOnDismissListener {
                if (state == OverlayState.VISIBLE && currentView == view && autoDismissRunnable == null) {
                    val remainingMs = 3000L
                    val dismissRunnable = Runnable {
                        if (state == OverlayState.VISIBLE && currentView == view && currentInstanceId == instanceId) {
                            requestDismiss(instanceId, "AUTO_DISMISS")
                        }
                    }
                    autoDismissRunnable = dismissRunnable
                    mainHandler.postDelayed(dismissRunnable, remainingMs)
                }
            }
            popup.show()
            true
        }

        fun updatePauseButton() {
            if (isCountdownPaused) {
                pauseIcon?.setImageResource(R.drawable.ic_overlay_resume)
                pauseIcon?.contentDescription = "Tiếp tục"
            } else {
                pauseIcon?.setImageResource(R.drawable.ic_overlay_pause)
                pauseIcon?.contentDescription = "Tạm dừng"
            }
        }
        updatePauseButton()

        togglePauseBtn?.setOnClickListener {
            if (state != OverlayState.VISIBLE || currentView != view || currentInstanceId != instanceId) return@setOnClickListener
            if (!isCountdownPaused) {
                isCountdownPaused = true
                val elapsed = System.currentTimeMillis() - countdownStartedMillis
                countdownRemainingMillis = maxOf(0L, countdownRemainingMillis - elapsed)
                autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
                autoDismissRunnable = null
                currentCountdownAnimator?.cancel()
                currentCountdownAnimator = null
                val progressFraction = if (displayDurationMillis > 0) {
                    (countdownRemainingMillis.toFloat() / displayDurationMillis.toFloat()).coerceIn(0f, 1f)
                } else 0f
                countdownProgress?.scaleX = progressFraction
                updatePauseButton()
                Log.i(TAG, "[OverlayPause] instanceId=$instanceId action=PAUSE remainingMs=$countdownRemainingMillis progressFraction=$progressFraction")
            } else {
                isCountdownPaused = false
                updatePauseButton()
                if (countdownRemainingMillis > 0L) {
                    countdownStartedMillis = System.currentTimeMillis()
                    val animator = countdownProgress?.animate()
                        ?.scaleX(0f)
                        ?.setDuration(countdownRemainingMillis)
                        ?.setInterpolator(LinearInterpolator())
                    currentCountdownAnimator = animator
                    animator?.start()

                    val dismissRunnable = Runnable {
                        if (state == OverlayState.VISIBLE && currentView == view && currentInstanceId == instanceId && !isCountdownPaused) {
                            requestDismiss(instanceId, "AUTO_DISMISS")
                        }
                    }
                    autoDismissRunnable = dismissRunnable
                    mainHandler.postDelayed(dismissRunnable, countdownRemainingMillis)
                    Log.i(TAG, "[OverlayPause] instanceId=$instanceId action=RESUME remainingMs=$countdownRemainingMillis")
                } else {
                    requestDismiss(instanceId, "AUTO_DISMISS")
                }
            }
        }

        fun updateMuteIcon() {
            val isMuted = vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value
            muteIcon?.setImageResource(if (isMuted) R.drawable.ic_autoplay_mute else R.drawable.ic_autoplay_unmute)
            muteIcon?.contentDescription = if (isMuted) "Bật âm thanh" else "Tắt âm thanh"
            if (isMuted) {
                muteIcon?.setColorFilter(0xFFEF4444.toInt(), PorterDuff.Mode.SRC_IN)
            } else {
                muteIcon?.clearColorFilter()
            }
        }
        updateMuteIcon()
        toggleMuteBtn?.setOnClickListener {
            vn.loi.learning.android.media.LearningEngineAudioPolicy.toggleMuted()
            updateMuteIcon()
        }

        // 8. Close Button & Review Tap
        closeContainer.setOnClickListener {
            requestDismiss(instanceId, "USER_CLOSE_CONTAINER")
        }
        closeButton.setOnClickListener {
            requestDismiss(instanceId, "USER_CLOSE_BUTTON")
        }

        cardRoot.setOnClickListener {
            requestDismiss(instanceId, "USER_REVIEW_CLICK") {
                if (onReview != null) {
                    onReview(candidate.packageId.value, candidate.contentId.value, mode)
                } else {
                    launchReviewScreen(candidate, mode)
                }
            }
        }

        // 9. WindowManager Layout
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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = topMarginPx
            windowAnimations = 0 // Disable secondary system/window fade to eliminate HyperOS compositor ghosting
        }

        Log.i(TAG, "performShow: displaying compact responsive overlay for '${candidate.primaryText}' (popupWidth=$popupWidth, textWidth=$availableTextWidthPx, englishSize=${englishFit.textSizeSp}sp, vnSize=${vietnameseSizeSp}sp)")

        return try {
            // Keep window root stable and opaque
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f

            // Inner card starts invisible for enter animation
            cardRoot.alpha = 0f
            cardRoot.scaleX = 0.92f
            cardRoot.scaleY = 0.92f

            windowManager?.addView(view, layoutParams)
            currentView = view
            state = OverlayState.ENTERING
            showing.set(true)

            // One-Shot Pre-Draw Gate: Ensure all geometry is 100% stable before first visible frame
            view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                    if (currentView != view || state != OverlayState.ENTERING || currentInstanceId != instanceId) {
                        return true
                    }

                    // Geometry is completely settled while alpha=0. Now start smooth 160ms enter animation on cardRoot
                    cardRoot.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160L)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            if (state == OverlayState.ENTERING && currentView == view && currentInstanceId == instanceId) {
                                state = OverlayState.VISIBLE

                                // Visual countdown progress bar (visual only, NO dismissal callback from animation end)
                                countdownTrack?.visibility = View.VISIBLE
                                countdownProgress?.pivotX = 0f
                                countdownProgress?.scaleX = 1f
                                countdownStartedMillis = System.currentTimeMillis()

                                val animator = countdownProgress?.animate()
                                    ?.scaleX(0f)
                                    ?.setDuration(displayDurationMillis)
                                    ?.setInterpolator(LinearInterpolator())
                                currentCountdownAnimator = animator
                                animator?.start()

                                // Single authoritative deadline timer
                                val dismissAt = System.currentTimeMillis() + displayDurationMillis
                                Log.i(TAG, "[OverlayTimer] instanceId=$instanceId action=ARM dismissAt=$dismissAt")
                                val dismissRunnable = Runnable {
                                    Log.i(TAG, "[OverlayTimer] instanceId=$instanceId action=FIRE")
                                    if (state == OverlayState.VISIBLE && currentView == view && currentInstanceId == instanceId && !isCountdownPaused) {
                                        requestDismiss(instanceId, "AUTO_DISMISS")
                                    }
                                }
                                autoDismissRunnable = dismissRunnable
                                mainHandler.postDelayed(dismissRunnable, displayDurationMillis)
                            }
                        }
                        .start()

                    return true
                }
            })

            true
        } catch (e: Throwable) {
            Log.e(TAG, "performShow FAILED: ${e.message}", e)
            currentView = null
            state = OverlayState.HIDDEN
            showing.set(false)
            false
        }
    }

    private data class EnglishFit(val textSizeSp: Float, val maxLines: Int)

    private fun autoFitEnglishHeadword(text: String, availableWidthPx: Int, density: Float): EnglishFit {
        val candidateSizes = listOf(26f, 23f, 20f, 18f, 16f, 14.5f)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
        }

        // Try single line fit from largest to smallest
        for (size in candidateSizes) {
            paint.textSize = size * density
            val textWidth = paint.measureText(text)
            if (textWidth <= availableWidthPx) {
                return EnglishFit(size, 1)
            }
        }

        // Try 2 lines fit from moderate size downwards
        for (size in listOf(20f, 18f, 16f, 14.5f)) {
            paint.textSize = size * density
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidthPx)
                    .setMaxLines(2)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, paint, availableWidthPx, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            }
            if (staticLayout.lineCount <= 2) {
                return EnglishFit(size, 2)
            }
        }
        return EnglishFit(14.5f, 2)
    }

    private fun autoFitVietnameseMeaning(text: String, availableWidthPx: Int, density: Float): Float {
        val candidateSizes = listOf(17.5f, 16f, 14.5f, 13.5f, 12f)
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
                StaticLayout(text, paint, availableWidthPx, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            }
            if (staticLayout.lineCount <= 3) {
                return size
            }
        }
        return 12f
    }

    fun requestDismiss(instanceId: Long, reason: String, onComplete: (() -> Unit)? = null): Boolean {
        if (instanceId != currentInstanceId) {
            Log.d(TAG, "[OverlayDismiss] instanceId=$instanceId activeInstanceId=$currentInstanceId reason=$reason action=IGNORED")
            return false
        }
        if (state == OverlayState.EXITING || state == OverlayState.HIDDEN) {
            Log.d(TAG, "[OverlayDismiss] instanceId=$instanceId state=$state reason=$reason action=IGNORED")
            return false
        }
        Log.i(TAG, "[OverlayDismiss] instanceId=$instanceId reason=$reason action=ACCEPTED")
        performHide(animated = true, targetInstanceId = instanceId, reason = reason, onComplete = onComplete)
        return true
    }

    override fun hide() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            requestDismiss(currentInstanceId, "DIRECT_HIDE")
        } else {
            mainHandler.post { requestDismiss(currentInstanceId, "DIRECT_HIDE") }
        }
    }

    private fun performHide(
        animated: Boolean = true,
        targetInstanceId: Long = currentInstanceId,
        reason: String = "HIDE",
        onComplete: (() -> Unit)? = null
    ) {
        if (targetInstanceId != currentInstanceId && currentView != null) {
            Log.d(TAG, "[OverlayDismiss] instanceId=$targetInstanceId activeInstanceId=$currentInstanceId reason=$reason action=IGNORED")
            return
        }

        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        autoDismissRunnable = null
        currentCountdownAnimator?.cancel()
        currentCountdownAnimator = null

        val view = currentView
        val cardRoot = view?.findViewById<View>(R.id.overlay_card_root) ?: view
        val instanceId = currentInstanceId
        val dismissCallback = currentOnDismissed
        if (view == null || state == OverlayState.HIDDEN) {
            currentOnDismissed = null
            state = OverlayState.HIDDEN
            showing.set(false)
            onComplete?.invoke()
            dismissCallback?.invoke(reason)
            return
        }

        // Immediate removal for non-animated, replace existing, entering dismissal, or IMMEDIATE mode
        if (!animated || state == OverlayState.ENTERING || activeExitMode == OverlayExitMode.IMMEDIATE) {
            currentView = null
            currentOnDismissed = null
            cardRoot?.animate()?.cancel()
            runCatching {
                windowManager?.removeView(view)
                Log.i(TAG, "[OverlayLifecycle] instanceId=$instanceId action=REMOVE_VIEW (immediate reason=$reason)")
            }.onFailure { e ->
                Log.w(TAG, "Error removing overlay view: ${e.message}", e)
            }
            state = OverlayState.HIDDEN
            showing.set(false)
            onComplete?.invoke()
            dismissCallback?.invoke(reason)
            return
        }

        if (state == OverlayState.EXITING) {
            // Already in exit animation, ignore double trigger
            return
        }

        state = OverlayState.EXITING
        val exitingView = view
        val exitingCardRoot = cardRoot
        val exitingInstanceId = instanceId
        exitingCardRoot?.animate()?.cancel()

        // Freeze card geometry (scaleX=1f, scaleY=1f). Only fade alpha from current to 0f in 100ms
        exitingCardRoot?.scaleX = 1f
        exitingCardRoot?.scaleY = 1f

        exitingCardRoot?.animate()
            ?.alpha(0f)
            ?.setDuration(EXIT_FADE_DURATION_MS)
            ?.setInterpolator(LinearInterpolator())
            ?.withEndAction {
                try {
                    if (currentView == exitingView && currentInstanceId == exitingInstanceId) {
                        Log.i(TAG, "[OverlayLifecycle] instanceId=$exitingInstanceId action=REMOVE_VIEW")
                        windowManager?.removeView(exitingView)
                        currentView = null
                        currentOnDismissed = null
                        state = OverlayState.HIDDEN
                        showing.set(false)
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error removing overlay view after exit animation", e)
                }
                onComplete?.invoke()
                dismissCallback?.invoke(reason)
            }
            ?.start()
    }

    override fun shutdown() {
        performHide(animated = false, reason = "SHUTDOWN")
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
                inSampleSize = inSampleSize.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        }.getOrNull()
    }

    private fun createRoundedCornerBitmap(src: Bitmap, cornerRadiusPx: Float): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, src.width, src.height)
        val rectF = RectF(rect)

        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        return output
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
        private const val TAG = "VocabularyOverlayPresenter"
        private const val OVERLAY_IMAGE_MAX_DIMENSION = 512
        var activeExitMode: OverlayExitMode = OverlayExitMode.SIMPLE_FADE
        const val EXIT_FADE_DURATION_MS = 100L
    }
}
