package vn.loi.learning.android.reminder

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import java.io.Closeable
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AndroidLockScreenVocabularyCoordinator(
    private val context: Context,
    private val preferencesController: AndroidVocabularyReminderPreferencesController,
    private val selector: AndroidVocabularyReminderCandidateSelector,
    private val resolveMedia: (String) -> String? = { null },
    private val overlayPresenter: AndroidVocabularyReminderOverlayPresenter? = null,
    private val notificationHelper: AndroidVocabularyReminderNotificationHelper? = null
) : Closeable {

    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cycleCounter = AtomicLong(System.currentTimeMillis())

    private val stateLock = Any()
    var currentDeviceState: VocabularyPresentationDeviceState = VocabularyPresentationDeviceState.SCREEN_OFF
        private set

    @Volatile
    var presentationBuffer: LockScreenPresentationBuffer = LockScreenPresentationBuffer()
        private set

    val currentPresentation: PreparedLockWallpaperPresentation?
        get() = presentationBuffer.visible

    private var lockQuickReviewRunnable: Runnable? = null
    private var unlockedReminderRunnable: Runnable? = null
    private var pauseExpiryRunnable: Runnable? = null
    private var screenOffPrepareRunnable: Runnable? = null
    private var armedIntervalMs: Long = 0L

    private val audioLock = Any()
    private var activeMediaPlayer: MediaPlayer? = null
    private var activeAudioCandidateId: String? = null

    init {
        Log.i(TAG_SESSION, "[Coordinator init] AndroidLockScreenVocabularyCoordinator created instance=$this")
    }

    private val deviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG_SESSION, "BroadcastReceiver onReceive: action=${intent?.action}")
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    transitionDeviceState(VocabularyPresentationDeviceState.SCREEN_OFF, "INTENT_SCREEN_OFF")
                }
                Intent.ACTION_SCREEN_ON -> {
                    val isLocked = isKeyguardLocked()
                    val targetState = if (isLocked) {
                        VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
                    } else {
                        VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
                    }
                    transitionDeviceState(targetState, "INTENT_SCREEN_ON(isLocked=$isLocked)")
                }
                Intent.ACTION_USER_PRESENT -> {
                    transitionDeviceState(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, "INTENT_USER_PRESENT")
                }
            }
        }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(deviceStateReceiver, filter)

        // Observe preference changes to automatically reconcile schedule
        coordinatorScope.launch {
            preferencesController.settings.collectLatest { _ ->
                reconcileUnlockedReminderSchedule("SETTINGS_FLOW_UPDATED")
            }
        }

        // Evaluate initial device state
        val isInteractive = isScreenInteractive()
        val isLocked = isKeyguardLocked()
        val initialState = when {
            !isInteractive -> VocabularyPresentationDeviceState.SCREEN_OFF
            isLocked -> VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
            else -> VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        }
        transitionDeviceState(initialState, "START_INITIAL_EVALUATION")
        prepareInitialPresentationIfEnabled()
    }

    fun transitionDeviceState(newState: VocabularyPresentationDeviceState, reason: String) {
        synchronized(stateLock) {
            val oldState = currentDeviceState
            if (oldState == newState) {
                Log.d(TAG_STATE, "[DeviceStateTransition] NO_OP from=$oldState to=$newState reason=$reason")
                return
            }
            currentDeviceState = newState
            Log.i(TAG_STATE, "[DeviceStateTransition] from=$oldState to=$newState reason=$reason")

            // 1. Cancel timers belonging to old state
            when (oldState) {
                VocabularyPresentationDeviceState.SCREEN_OFF -> {
                    cancelScreenOffPrepareTimer()
                }
                VocabularyPresentationDeviceState.LOCKED_SCREEN_ON -> {
                    cancelLockQuickTimer()
                    stopActiveAudio("LEAVING_LOCKED_SCREEN_ON")
                }
                VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON -> {
                    cancelUnlockedReminderTimer()
                    cancelPauseExpiryTimer()
                    overlayPresenter?.hide()
                }
            }

            // 2. Initialize timers & actions for new state
            when (newState) {
                VocabularyPresentationDeviceState.SCREEN_OFF -> {
                    handleEnteredScreenOff()
                }
                VocabularyPresentationDeviceState.LOCKED_SCREEN_ON -> {
                    handleEnteredLockedScreenOn()
                }
                VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON -> {
                    handleEnteredUnlockedScreenOn()
                }
            }
        }
    }

    // --- STATE HANDLERS ---

    private fun handleEnteredScreenOff() {
        val settings = preferencesController.currentLockScreen()
        if (!settings.enabled || !settings.screenOffPreparationEnabled) {
            Log.d(TAG_SCREEN_OFF_TIMER, "[ScreenOffPrepareTimer] disabled in settings")
            return
        }

        val delayMs = settings.screenOffPrepareDelayMillis
        if (delayMs <= 0L) {
            Log.i(TAG_SCREEN_OFF_TIMER, "[ScreenOffPrepareTimer] action=START delayMs=0 (IMMEDIATE)")
            executor.execute {
                prepareNextCandidateIntoBuffer("SCREEN_OFF_IMMEDIATE")
            }
        } else {
            Log.i(TAG_SCREEN_OFF_TIMER, "[ScreenOffPrepareTimer] action=START delayMs=$delayMs")
            val runnable = Runnable {
                synchronized(stateLock) {
                    if (currentDeviceState == VocabularyPresentationDeviceState.SCREEN_OFF) {
                        Log.i(TAG_SCREEN_OFF_TIMER, "[ScreenOffPrepareTimer] action=FIRE delayMs=$delayMs")
                        executor.execute {
                            prepareNextCandidateIntoBuffer("SCREEN_OFF_DELAYED")
                        }
                    }
                }
            }
            screenOffPrepareRunnable = runnable
            mainHandler.postDelayed(runnable, delayMs)
        }
    }

    private fun handleEnteredLockedScreenOn() {
        val settings = preferencesController.currentLockScreen()
        if (!settings.enabled) return

        val buf = presentationBuffer
        val visibleBefore = buf.visible?.candidate?.contentId?.value
        val nextReadyBefore = buf.nextReady?.candidate?.contentId?.value

        if (buf.nextReady != null) {
            // CASE A: nextReady exists -> promote and activate immediately
            val targetId = buf.nextReady?.candidate?.contentId?.value
            Log.i(TAG_SESSION, "[LockWake] event=SCREEN_ON visibleBefore=$visibleBefore nextReadyBefore=$nextReadyBefore action=ACTIVATE_NEXT_READY visibleAfter=$targetId")
            activateNextReadyCandidate("WAKE_NEXT_READY")
        } else if (buf.visible != null) {
            // CASE B: nextReady == null AND visible != null -> fallback to visible, do NOT shuffle/advance
            val fallback = buf.visible
            Log.i(TAG_SESSION, "[LockWake] event=SCREEN_ON visibleBefore=$visibleBefore nextReadyBefore=null action=PRESENT_VISIBLE_FALLBACK visibleAfter=${fallback?.candidate?.contentId?.value}")
            Log.i(TAG_SESSION, "[LockVocabularySession] event=SCREEN_ON candidateId=${fallback?.candidate?.contentId?.value} action=PRESENT reason=SCREEN_WAKE_FALLBACK")
            fallback?.let { playCandidateAudioIfConfigured(it, settings) }
            // Prefetch next candidate in background so next wake has nextReady
            executor.execute {
                prepareNextCandidateIntoBuffer("WAKE_FALLBACK_PREFETCH")
            }
        } else {
            // CASE C: visible == null AND nextReady == null -> first-run fallback preparation
            Log.i(TAG_SESSION, "[LockWake] event=SCREEN_ON visibleBefore=null nextReadyBefore=null action=BUFFER_EMPTY visibleAfter=null")
            executor.execute {
                prepareNextCandidateIntoBuffer("WAKE_BUFFER_EMPTY")
                mainHandler.post {
                    synchronized(stateLock) {
                        if (currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON) {
                            activateNextReadyCandidate("WAKE_BUFFER_EMPTY_ACTIVATED")
                        }
                    }
                }
            }
        }
    }

    private var resumeNowPendingImmediatePopup: Boolean = false

    private fun handleEnteredUnlockedScreenOn() {
        synchronized(stateLock) {
            if (resumeNowPendingImmediatePopup) {
                resumeNowPendingImmediatePopup = false
                val settings = preferencesController.current()
                if (settings.enabled && !settings.isUnlockedPaused) {
                    Log.i(TAG_UNLOCKED_TIMER, "[UnlockedResumeNow] action=PENDING_FIRED_ON_UNLOCK")
                    dispatchUnlockedOverlay(settings, source = "RESUME_NOW")
                    return
                }
            }
        }
        reconcileUnlockedReminderSchedule("UNLOCKED_SCREEN_ON")
    }

    // --- TIMERS MANAGEMENT ---

    private fun startLockQuickTimer(intervalMs: Long, candidateId: String) {
        cancelLockQuickTimer()
        Log.i(TAG_LOCK_TIMER, "[LockQuickTimer] action=START intervalMs=$intervalMs candidateId=$candidateId")
        val runnable = Runnable {
            synchronized(stateLock) {
                if (currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON) {
                    val settings = preferencesController.currentLockScreen()
                    if (settings.enabled) {
                        Log.i(TAG_LOCK_TIMER, "[LockQuickTimer] action=FIRE intervalMs=$intervalMs candidateId=$candidateId")
                        val activated = activateNextReadyCandidate("QUICK_REVIEW_INTERVAL_EXPIRED")
                        if (!activated) {
                            // If buffer wasn't ready, trigger prepare and retry quickly
                            executor.execute {
                                prepareNextCandidateIntoBuffer("QUICK_REVIEW_RETRY_PREPARE")
                                mainHandler.post {
                                    synchronized(stateLock) {
                                        if (currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON) {
                                            activateNextReadyCandidate("QUICK_REVIEW_RETRY_ACTIVATED")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        lockQuickReviewRunnable = runnable
        mainHandler.postDelayed(runnable, intervalMs)
    }

    private fun cancelLockQuickTimer() {
        lockQuickReviewRunnable?.let {
            mainHandler.removeCallbacks(it)
            Log.d(TAG_LOCK_TIMER, "[LockQuickTimer] action=CANCEL")
        }
        lockQuickReviewRunnable = null
    }

    fun reconcileUnlockedReminderSchedule(reason: String) {
        synchronized(stateLock) {
            val state = currentDeviceState
            val settings = preferencesController.current()
            val now = System.currentTimeMillis()
            val pausedUntil = settings.unlockedPausedUntilEpochMillis
            val isPaused = now < pausedUntil
            val intervalMs = settings.unlockedReminderIntervalMillis

            if (state != VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON || !settings.enabled) {
                Log.i(TAG_UNLOCKED_TIMER, "[UnlockedSchedule] reason=$reason state=$state pausedUntil=$pausedUntil now=$now intervalMs=$intervalMs action=CANCEL")
                cancelUnlockedReminderTimer()
                cancelPauseExpiryTimer()
                AndroidLockScreenVocabularyService.updateNotification(context)
                return
            }

            if (isPaused) {
                val remainingPauseMs = (pausedUntil - now).coerceAtLeast(100L)
                Log.i(TAG_UNLOCKED_TIMER, "[UnlockedSchedule] reason=$reason state=$state pausedUntil=$pausedUntil now=$now intervalMs=$intervalMs action=WAIT_PAUSE")
                cancelUnlockedReminderTimer()
                armPauseExpiryTimer(pausedUntil, remainingPauseMs)
                AndroidLockScreenVocabularyService.updateNotification(context)
                return
            }

            // Not paused (or pause expired)
            cancelPauseExpiryTimer()
            AndroidLockScreenVocabularyService.updateNotification(context)
            if (unlockedReminderRunnable != null && armedIntervalMs == intervalMs) {
                Log.d(TAG_UNLOCKED_TIMER, "[UnlockedSchedule] reason=$reason state=$state intervalMs=$intervalMs action=NO_OP")
                return
            }

            Log.i(TAG_UNLOCKED_TIMER, "[UnlockedSchedule] reason=$reason state=$state intervalMs=$intervalMs action=START_INTERVAL")
            cancelUnlockedReminderTimer()
            armedIntervalMs = intervalMs
            Log.i(TAG_UNLOCKED_TIMER, "[UnlockedReminderTimer] action=START intervalMs=$intervalMs")
            val runnable = Runnable {
                synchronized(stateLock) {
                    if (currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) {
                        val curSettings = preferencesController.current()
                        if (curSettings.enabled && !curSettings.isUnlockedPaused) {
                            Log.i(TAG_UNLOCKED_TIMER, "[UnlockedReminderTimer] action=FIRE intervalMs=$intervalMs")
                            armedIntervalMs = 0L
                            unlockedReminderRunnable = null
                            dispatchUnlockedOverlay(curSettings, source = "STATE_TIMER")
                        }
                    }
                }
            }
            unlockedReminderRunnable = runnable
            mainHandler.postDelayed(runnable, intervalMs)
        }
    }

    private fun armPauseExpiryTimer(pausedUntil: Long, remainingMs: Long) {
        cancelPauseExpiryTimer()
        Log.i(TAG_UNLOCKED_TIMER, "[UnlockedPauseExpiry] pausedUntil=$pausedUntil remainingMs=$remainingMs action=ARMED")
        val runnable = Runnable {
            synchronized(stateLock) {
                Log.i(TAG_UNLOCKED_TIMER, "[UnlockedPauseExpiry] action=FIRED")
                pauseExpiryRunnable = null
                reconcileUnlockedReminderSchedule("PAUSE_EXPIRED")
            }
        }
        pauseExpiryRunnable = runnable
        mainHandler.postDelayed(runnable, remainingMs)
    }

    private fun cancelPauseExpiryTimer() {
        pauseExpiryRunnable?.let {
            mainHandler.removeCallbacks(it)
            Log.d(TAG_UNLOCKED_TIMER, "[UnlockedPauseExpiry] action=CANCEL")
        }
        pauseExpiryRunnable = null
    }

    private fun cancelUnlockedReminderTimer() {
        unlockedReminderRunnable?.let {
            mainHandler.removeCallbacks(it)
            Log.d(TAG_UNLOCKED_TIMER, "[UnlockedReminderTimer] action=CANCEL")
        }
        unlockedReminderRunnable = null
        armedIntervalMs = 0L
    }

    private fun cancelScreenOffPrepareTimer() {
        screenOffPrepareRunnable?.let {
            mainHandler.removeCallbacks(it)
            Log.d(TAG_SCREEN_OFF_TIMER, "[ScreenOffPrepareTimer] action=CANCEL")
        }
        screenOffPrepareRunnable = null
    }

    // --- DOUBLE BUFFER: PREPARE VS ACTIVATE ---

    fun prepareNextCandidateIntoBuffer(reason: String) {
        val settings = preferencesController.currentLockScreen()
        if (!settings.enabled) return

        // If nextReady already exists and has a rendered bitmap, do not double-advance
        if (presentationBuffer.nextReady != null) {
            Log.d(TAG_BUFFER, "[PresentationBuffer] nextReady already prepared (${presentationBuffer.nextReady?.candidate?.contentId?.value}), skip prepare reason=$reason")
            return
        }

        val selectionResult = selector.selectLockScreen(settings)
        if (selectionResult !is AndroidVocabularyCandidateSelectionResult.Selected) {
            Log.d(TAG_SESSION, "[LockVocabularySession] selectLockScreen returned no candidate reason=$reason")
            return
        }

        val candidate = selectionResult.candidate
        val candidateId = candidate.contentId.value
        val sessionToken = cycleCounter.incrementAndGet()
        Log.i(TAG_SESSION, "[LockVocabularySession] event=PREPARE candidateId=$candidateId action=SELECT reason=$reason")

        val imagePath = candidate.imageReference?.let(resolveMedia)
        val audioPath = candidate.primaryAudioReference?.let(resolveMedia)
        val customBgPath = settings.customBackgroundPath

        // Render wallpaper bitmap off-screen
        try {
            val (baseMaxW, baseMaxH) = when (settings.imageSize) {
                LockWallpaperImageSize.MEDIUM -> Pair(700, 400)
                LockWallpaperImageSize.LARGE -> Pair(800, 440)
                LockWallpaperImageSize.EXTRA_LARGE -> Pair(900, 490)
                LockWallpaperImageSize.MAXIMUM -> Pair(1000, 550)
            }
            val decodedImageBitmap = decodeBoundedBitmap(imagePath, maxWidth = baseMaxW, maxHeight = baseMaxH)
            val customBgBitmap = decodeBoundedBitmap(customBgPath, maxWidth = 1220, maxHeight = 2712)

            val renderModel = LockWallpaperVocabularyRenderModel(
                headword = candidate.primaryText,
                ipa = candidate.ipa,
                partOfSpeech = candidate.partOfSpeech,
                meaning = candidate.translation ?: candidate.answer ?: "",
                imageBitmap = decodedImageBitmap,
                hasPrimaryAudio = audioPath != null,
                backgroundBitmap = customBgBitmap,
                wordSize = settings.wordSize,
                vietnameseSize = settings.vietnameseSize,
                imageSize = settings.imageSize,
                cardBackgroundOpacity = settings.clampedCardBackgroundOpacity
            )

            Log.i(
                TAG_RENDER,
                "[LockWallpaperRender] candidateId=$candidateId opacity=${settings.clampedCardBackgroundOpacity} wordSize=${settings.wordSize} vietnameseSize=${settings.vietnameseSize} imageSize=${settings.imageSize} settingsSource=PERSISTED"
            )

            val renderedBitmap = AndroidLockScreenWallpaperRenderer.renderVocabularyWallpaper(renderModel)

            val prepared = PreparedLockWallpaperPresentation(
                candidate = candidate,
                wallpaperBitmap = renderedBitmap,
                audioSourcePath = audioPath,
                sessionToken = sessionToken
            )

            decodedImageBitmap?.recycle()
            customBgBitmap?.recycle()

            synchronized(stateLock) {
                presentationBuffer = presentationBuffer.withNextReady(prepared)
                Log.i(TAG_BUFFER, "[PresentationBuffer] stored candidateId=$candidateId into nextReady buffer (reason=$reason)")
            }
        } catch (e: Throwable) {
            Log.e(TAG_SESSION, "Failed to render wallpaper bitmap during prepare: ${e.message}", e)
        }
    }

    fun activateNextReadyCandidate(reason: String): Boolean {
        synchronized(stateLock) {
            val buf = presentationBuffer
            val next = buf.nextReady
            if (next == null) {
                Log.d(TAG_BUFFER, "[PresentationBuffer] activateNextReadyCandidate called but nextReady is null (reason=$reason)")
                return false
            }

            val oldVisible = buf.visible
            presentationBuffer = buf.activateNext()
            Log.i(TAG_BUFFER, "[PresentationBuffer] activated candidateId=${next.candidate.contentId.value} to visible (reason=$reason)")

            // Commit bitmap to WallpaperManager
            try {
                val wm = WallpaperManager.getInstance(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setBitmap(next.wallpaperBitmap, null, false, WallpaperManager.FLAG_LOCK)
                } else {
                    wm.setBitmap(next.wallpaperBitmap)
                }
                Log.i(TAG_SESSION, "[LockVocabularySession] event=SCREEN_ON candidateId=${next.candidate.contentId.value} action=PRESENT reason=$reason")
            } catch (e: Throwable) {
                Log.e(TAG_SESSION, "Failed to set wallpaper bitmap on activation: ${e.message}", e)
            } finally {
                oldVisible?.wallpaperBitmap?.recycle()
            }

            // Play audio if device is currently LOCKED_SCREEN_ON
            val settings = preferencesController.currentLockScreen()
            if (currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON) {
                playCandidateAudioIfConfigured(next, settings)
            }

            // Immediately trigger background preparation of the NEXT candidate so buffer stays full!
            executor.execute {
                prepareNextCandidateIntoBuffer("AUTO_PREFETCH_AFTER_ACTIVATION")
            }
            return true
        }
    }

    fun reRenderCurrentPresentation(reason: String) {
        executor.execute {
            val visiblePres = presentationBuffer.visible ?: return@execute
            val candidate = visiblePres.candidate
            val settings = preferencesController.currentLockScreen()

            try {
                val imagePath = candidate.imageReference?.let(resolveMedia)
                val (baseMaxW, baseMaxH) = when (settings.imageSize) {
                    LockWallpaperImageSize.MEDIUM -> Pair(700, 400)
                    LockWallpaperImageSize.LARGE -> Pair(800, 440)
                    LockWallpaperImageSize.EXTRA_LARGE -> Pair(900, 490)
                    LockWallpaperImageSize.MAXIMUM -> Pair(1000, 550)
                }
                val decodedImageBitmap = decodeBoundedBitmap(imagePath, maxWidth = baseMaxW, maxHeight = baseMaxH)
                val customBgBitmap = decodeBoundedBitmap(settings.customBackgroundPath, maxWidth = 1220, maxHeight = 2712)

                val renderModel = LockWallpaperVocabularyRenderModel(
                    headword = candidate.primaryText,
                    ipa = candidate.ipa,
                    partOfSpeech = candidate.partOfSpeech,
                    meaning = candidate.translation ?: candidate.answer ?: "",
                    imageBitmap = decodedImageBitmap,
                    hasPrimaryAudio = visiblePres.audioSourcePath != null,
                    backgroundBitmap = customBgBitmap,
                    wordSize = settings.wordSize,
                    vietnameseSize = settings.vietnameseSize,
                    imageSize = settings.imageSize,
                    cardBackgroundOpacity = settings.clampedCardBackgroundOpacity
                )

                Log.i(
                    TAG_RENDER,
                    "[LockWallpaperRender] candidateId=${candidate.contentId.value} opacity=${settings.clampedCardBackgroundOpacity} wordSize=${settings.wordSize} vietnameseSize=${settings.vietnameseSize} imageSize=${settings.imageSize} settingsSource=PERSISTED"
                )

                val wallpaperBitmap = AndroidLockScreenWallpaperRenderer.renderVocabularyWallpaper(renderModel)
                val wm = WallpaperManager.getInstance(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setBitmap(wallpaperBitmap, null, false, WallpaperManager.FLAG_LOCK)
                } else {
                    wm.setBitmap(wallpaperBitmap)
                }
                wallpaperBitmap.recycle()
                decodedImageBitmap?.recycle()
                customBgBitmap?.recycle()
                Log.i(TAG_SESSION, "[LockVocabularySession] re-rendered current candidate ${candidate.contentId.value} reason=$reason")
            } catch (e: Throwable) {
                Log.e(TAG_SESSION, "Failed to re-render presentation: ${e.message}", e)
            }
        }
    }

    fun prepareInitialPresentationIfEnabled() {
        val settings = preferencesController.currentLockScreen()
        if (settings.enabled && presentationBuffer.visible == null && presentationBuffer.nextReady == null) {
            executor.execute {
                prepareNextCandidateIntoBuffer("INITIAL_PREPARE")
                mainHandler.post {
                    synchronized(stateLock) {
                        if (presentationBuffer.visible == null && presentationBuffer.nextReady != null) {
                            activateNextReadyCandidate("INITIAL_ACTIVATE")
                        }
                    }
                }
            }
        }
    }

    // --- AUDIO PLAYBACK ---

    private fun playCandidateAudioIfConfigured(
        presentation: PreparedLockWallpaperPresentation,
        settings: AndroidLockScreenVocabularySettings
    ) {
        val audioPath = presentation.audioSourcePath
        val candidate = presentation.candidate
        val candidateId = candidate.contentId.value
        val fileExists = audioPath != null && File(audioPath).exists()

        if (settings.autoPlayPronunciation && fileExists && audioPath != null && !vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value) {
            if (presentation.isAudioPlayed.compareAndSet(false, true)) {
                startAudioPlayback(presentation.sessionToken, candidateId, candidate.primaryText, audioPath)
            } else {
                Log.d(TAG_AUDIO, "[AUDIO_DUPLICATE_IGNORED] candidateId=$candidateId")
                onAudioCompletedOrSkipped(candidateId, settings)
            }
        } else {
            // Autoplay OFF or no audio -> start quick review countdown immediately
            onAudioCompletedOrSkipped(candidateId, settings)
        }
    }

    private fun startAudioPlayback(cycleId: Long, candidateId: String, headword: String, audioPath: String) {
        val file = File(audioPath)
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG_AUDIO, "[Audio] fileNotFound candidateId=$candidateId path=$audioPath")
            val settings = preferencesController.currentLockScreen()
            onAudioCompletedOrSkipped(candidateId, settings)
            return
        }

        if (vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value) {
            Log.i(TAG_AUDIO, "[Audio] candidateId=$candidateId SKIPPED because app is MUTED")
            val settings = preferencesController.currentLockScreen()
            onAudioCompletedOrSkipped(candidateId, settings)
            return
        }

        synchronized(audioLock) {
            stopActiveAudio("PREPARING_NEW_PLAYBACK")
            activeAudioCandidateId = candidateId

            try {
                val mediaPlayer = MediaPlayer()
                activeMediaPlayer = mediaPlayer

                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                mediaPlayer.setAudioAttributes(attributes)
                mediaPlayer.setDataSource(file.absolutePath)

                mediaPlayer.setOnPreparedListener { mp ->
                    synchronized(audioLock) {
                        if (activeMediaPlayer == mp && activeAudioCandidateId == candidateId && !vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value) {
                            mp.start()
                            Log.i(TAG_AUDIO, "[Audio] candidateId=$candidateId START (headword='$headword')")
                        } else {
                            mp.release()
                            if (activeMediaPlayer == mp) {
                                activeMediaPlayer = null
                                activeAudioCandidateId = null
                            }
                        }
                    }
                }

                mediaPlayer.setOnCompletionListener { mp ->
                    synchronized(audioLock) {
                        if (activeMediaPlayer == mp) {
                            activeMediaPlayer = null
                            activeAudioCandidateId = null
                        }
                        mp.release()
                        Log.i(TAG_AUDIO, "[Audio] candidateId=$candidateId COMPLETE (headword='$headword')")
                    }
                    val settings = preferencesController.currentLockScreen()
                    onAudioCompletedOrSkipped(candidateId, settings)
                }

                mediaPlayer.setOnErrorListener { mp, what, extra ->
                    synchronized(audioLock) {
                        if (activeMediaPlayer == mp) {
                            activeMediaPlayer = null
                            activeAudioCandidateId = null
                        }
                        mp.release()
                        Log.e(TAG_AUDIO, "[Audio] candidateId=$candidateId playError what=$what extra=$extra")
                    }
                    val settings = preferencesController.currentLockScreen()
                    onAudioCompletedOrSkipped(candidateId, settings)
                    true
                }

                mediaPlayer.prepareAsync()
            } catch (e: Throwable) {
                Log.e(TAG_AUDIO, "[Audio] candidateId=$candidateId prepareError: ${e.message}", e)
                activeMediaPlayer = null
                activeAudioCandidateId = null
                val settings = preferencesController.currentLockScreen()
                onAudioCompletedOrSkipped(candidateId, settings)
            }
        }
    }

    private fun onAudioCompletedOrSkipped(candidateId: String, settings: AndroidLockScreenVocabularySettings) {
        synchronized(stateLock) {
            if (currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON) {
                val interval = settings.quickReviewIntervalMillis
                startLockQuickTimer(interval, candidateId)
            }
        }
    }

    private fun stopActiveAudio(reason: String) {
        synchronized(audioLock) {
            val player = activeMediaPlayer
            val session = activeAudioCandidateId
            if (player != null) {
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                    Log.i(TAG_AUDIO, "[Audio] candidateId=$session STOP reason=$reason")
                } catch (e: Throwable) {
                    Log.d(TAG_AUDIO, "Error releasing media player: ${e.message}")
                } finally {
                    activeMediaPlayer = null
                    activeAudioCandidateId = null
                }
            }
        }
    }

    private val nextUnlockedDispatchId = java.util.concurrent.atomic.AtomicLong(1000L)

    // --- UNLOCKED REMINDER DISPATCH ---

    private fun dispatchUnlockedOverlay(
        settings: AndroidVocabularyReminderSettings,
        source: String = "STATE_TIMER"
    ) {
        if (overlayPresenter == null) return
        val dispatchId = nextUnlockedDispatchId.incrementAndGet()
        val result = selector.select(settings)
        if (result is AndroidVocabularyCandidateSelectionResult.Selected) {
            val candidate = result.candidate
            val candidateId = candidate.contentId.value
            Log.i(TAG_UNLOCKED_TIMER, "[UnlockedOverlayDispatch] source=$source dispatchId=$dispatchId candidateId=$candidateId action=SELECT headword='${candidate.primaryText}'")

            val audioPath = candidate.primaryAudioReference?.let(resolveMedia)
            val hasAudio = settings.autoPlayPronunciation && audioPath != null && File(audioPath).exists() && !vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value

            Log.i(TAG_UNLOCKED_TIMER, "[UnlockedOverlayDispatch] source=$source dispatchId=$dispatchId candidateId=$candidateId action=SHOW")

            var isPausedAgain = false

            val shown = overlayPresenter.show(
                candidate = candidate,
                mode = settings.selectionMode,
                displayDurationMillis = settings.displayDurationMillis,
                onQuickPause = if (settings.quickPauseActionsEnabled) {
                    { minutes ->
                        isPausedAgain = true
                        Log.i(TAG_UNLOCKED_TIMER, "[UnlockedPause] duration=${minutes}m userAction=TAP")
                        preferencesController.pauseUnlocked(java.time.Duration.ofMinutes(minutes))
                        AndroidLockScreenVocabularyService.updateNotification(context)
                        if (source == "RESUME_NOW") {
                            Log.i(TAG_UNLOCKED_TIMER, "[UnlockedResumeNow] action=NORMAL_REARM_SUPPRESSED reason=PAUSED_AGAIN")
                        }
                        reconcileUnlockedReminderSchedule("PAUSE_TAPPED")
                    }
                } else null,
                onDismissed = { reason ->
                    if (source == "RESUME_NOW") {
                        Log.i(TAG_UNLOCKED_TIMER, "[UnlockedResumeNow] action=IMMEDIATE_POPUP_FINISHED reason=$reason")
                        if (!isPausedAgain) {
                            reconcileUnlockedReminderSchedule("RESUME_NOW_POPUP_FINISHED")
                        }
                    } else {
                        if (!isPausedAgain) {
                            reconcileUnlockedReminderSchedule("POPUP_DISMISSED")
                        }
                    }
                }
            )

            if (shown) {
                Log.i(TAG_UNLOCKED_TIMER, "[UnlockedOverlayDispatch] source=$source dispatchId=$dispatchId candidateId=$candidateId action=ATTACHED")
                if (hasAudio && audioPath != null) {
                    Log.i(TAG_UNLOCKED_TIMER, "[UnlockedOverlayDispatch] source=$source dispatchId=$dispatchId candidateId=$candidateId action=AUDIO_START")
                    startAudioPlayback(dispatchId, candidateId, candidate.primaryText, audioPath)
                }
            }
        }
    }

    fun resumeUnlockedNow() {
        val now = System.currentTimeMillis()
        val pausedUntilBefore = preferencesController.current().unlockedPausedUntilEpochMillis
        Log.i(TAG_UNLOCKED_TIMER, "[UnlockedResumeNow] action=TAP pausedUntilBefore=$pausedUntilBefore now=$now")

        preferencesController.resumeUnlocked()
        Log.i(TAG_UNLOCKED_TIMER, "[UnlockedResumeNow] action=PAUSE_CLEARED")
        AndroidLockScreenVocabularyService.updateNotification(context)

        synchronized(stateLock) {
            cancelPauseExpiryTimer()
            cancelUnlockedReminderTimer()

            val state = currentDeviceState
            val settings = preferencesController.current()

            if (state == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && settings.enabled) {
                resumeNowPendingImmediatePopup = false
                dispatchUnlockedOverlay(settings, source = "RESUME_NOW")
            } else {
                resumeNowPendingImmediatePopup = true
                Log.i(TAG_UNLOCKED_TIMER, "[UnlockedResumeNow] action=PENDING_FOR_NEXT_UNLOCK state=$state")
            }
        }
    }

    // --- UTILITIES ---

    private fun isScreenInteractive(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isInteractive ?: true
    }

    private fun isKeyguardLocked(): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }

    private fun decodeBoundedBitmap(path: String?, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        return runCatching {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            var sampleSize = 1
            while ((boundsOptions.outWidth / (sampleSize * 2)) >= maxWidth &&
                (boundsOptions.outHeight / (sampleSize * 2)) >= maxHeight
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        }.getOrNull()
    }

    // Backward-compatibility hooks
    fun handleScreenOff() = transitionDeviceState(VocabularyPresentationDeviceState.SCREEN_OFF, "DIRECT_SCREEN_OFF")
    fun handleScreenOn() {
        val isLocked = isKeyguardLocked()
        val target = if (isLocked) VocabularyPresentationDeviceState.LOCKED_SCREEN_ON else VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        transitionDeviceState(target, "DIRECT_SCREEN_ON")
    }

    fun stop() {
        if (started.compareAndSet(true, false)) {
            try {
                context.unregisterReceiver(deviceStateReceiver)
            } catch (_: Throwable) {}
            coordinatorScope.cancel()
            cancelLockQuickTimer()
            cancelUnlockedReminderTimer()
            cancelPauseExpiryTimer()
            cancelScreenOffPrepareTimer()
            stopActiveAudio("COORDINATOR_STOPPED")
            presentationBuffer = LockScreenPresentationBuffer()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            stop()
            executor.shutdown()
        }
    }

    companion object {
        private const val TAG_SESSION = "LockVocabularySession"
        private const val TAG_STATE = "DeviceStateTransition"
        private const val TAG_LOCK_TIMER = "LockQuickTimer"
        private const val TAG_UNLOCKED_TIMER = "UnlockedReminderTimer"
        private const val TAG_SCREEN_OFF_TIMER = "ScreenOffPrepareTimer"
        private const val TAG_BUFFER = "PresentationBuffer"
        private const val TAG_RENDER = "LockWallpaperRender"
        private const val TAG_AUDIO = "Audio"
    }
}
