package vn.loi.learning.android.reminder

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AndroidHomeVocabularyWidgetCoordinator(
    private val context: Context,
    private val preferencesController: AndroidVocabularyReminderPreferencesController,
    private val selector: AndroidVocabularyReminderCandidateSelector,
    private val difficultMarkers: AndroidVocabularyReminderDifficultMarkers,
    private val resolveMedia: (String) -> String? = { null }
) {

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val started = AtomicBoolean(false)
    private val stateLock = Any()
    private val audioLock = Any()

    private val activeWidgetIds = mutableSetOf<Int>()
    private var currentCandidate: AndroidVocabularyCandidate? = null
    private var isPreparingCandidate: Boolean = false
    private var isAdvancingCandidate: Boolean = false
    private var autoNextRunnable: Runnable? = null
    private var armedIntervalMs: Long = 0L

    private var currentCycleToken: Long = 0L
    private var lastAutoPlayedCandidateId: String? = null
    private var lastAutoPlayedCycleToken: Long = 0L

    private var mediaPlayer: MediaPlayer? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] event=SCREEN_ON action=RESUME_TIMER")
                    reconcileAutoNextTimer("SCREEN_ON")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] event=SCREEN_OFF action=SUSPEND_TIMER")
                    stopAudioPlayback()
                    reconcileAutoNextTimer("SCREEN_OFF")
                }
                Intent.ACTION_USER_PRESENT -> {
                    reconcileAutoNextTimer("USER_PRESENT")
                }
            }
        }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(screenStateReceiver, filter)

        // Restore known active widgets from AppWidgetManager
        syncActiveWidgetIds()

        // Restore in-memory current candidate if persisted
        val settings = preferencesController.currentHomeWidget()
        val persistedId = settings.currentCandidateId
        val persistedPkgId = settings.selectedPackageId
        if (currentCandidate == null && persistedId != null && persistedPkgId != null) {
            executor.execute {
                val restored = selector.resolveCandidate(persistedPkgId, persistedId)
                if (restored != null) {
                    synchronized(stateLock) {
                        currentCandidate = restored
                        Log.i(TAG_INIT, "[HomeWidgetInit] widgetCount=${activeWidgetIds.size} currentCandidate=${restored.contentId.value} action=USE_EXISTING reason=RESTORE_PERSISTED")
                        if (activeWidgetIds.isNotEmpty()) {
                            reRenderWidgets(activeWidgetIds.toIntArray(), "RESTORE_PERSISTED")
                        }
                    }
                }
            }
        }

        // Observe settings changes
        coordinatorScope.launch {
            preferencesController.homeWidgetSettings.collectLatest { _ ->
                synchronized(stateLock) {
                    reconcileAutoNextTimer("SETTINGS_UPDATED")
                    reRenderAllWidgets("SETTINGS_UPDATED")
                }
            }
        }

        reconcileAutoNextTimer("COORDINATOR_START")
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        runCatching { context.unregisterReceiver(screenStateReceiver) }
        cancelAutoNextTimer("COORDINATOR_STOP")
        stopAudioPlayback()
    }

    fun onWidgetsUpdate(appWidgetIds: IntArray) {
        synchronized(stateLock) {
            for (id in appWidgetIds) {
                if (activeWidgetIds.add(id)) {
                    Log.i(TAG_WIDGET, "[HomeWidget] action=INSTANCE_ADDED widgetId=$id totalCount=${activeWidgetIds.size}")
                }
            }

            if (currentCandidate != null) {
                Log.i(TAG_INIT, "[HomeWidgetInit] widgetCount=${activeWidgetIds.size} currentCandidate=${currentCandidate?.contentId?.value} action=USE_EXISTING reason=WIDGETS_UPDATED")
                reRenderWidgets(appWidgetIds, "WIDGET_UPDATE")
            } else {
                prepareCandidateIfNeeded("INITIAL_WIDGET_ADD")
            }

            reconcileAutoNextTimer("WIDGETS_UPDATED")
        }
    }

    fun onWidgetOptionsChanged(appWidgetId: Int, newOptions: Bundle) {
        synchronized(stateLock) {
            activeWidgetIds.add(appWidgetId)
            reRenderWidgets(intArrayOf(appWidgetId), "OPTIONS_CHANGED")
        }
    }

    fun onWidgetsDeleted(appWidgetIds: IntArray) {
        synchronized(stateLock) {
            for (id in appWidgetIds) {
                if (activeWidgetIds.remove(id)) {
                    Log.i(TAG_WIDGET, "[HomeWidget] action=INSTANCE_REMOVED widgetId=$id totalCount=${activeWidgetIds.size}")
                }
            }
            if (activeWidgetIds.isEmpty()) {
                cancelAutoNextTimer("ALL_WIDGETS_REMOVED")
                stopAudioPlayback()
            }
        }
    }

    fun onFirstWidgetEnabled() {
        synchronized(stateLock) {
            syncActiveWidgetIds()
            Log.i(TAG_WIDGET, "[HomeWidget] action=FIRST_WIDGET_ENABLED totalCount=${activeWidgetIds.size}")
            if (currentCandidate != null) {
                Log.i(TAG_INIT, "[HomeWidgetInit] widgetCount=${activeWidgetIds.size} currentCandidate=${currentCandidate?.contentId?.value} action=USE_EXISTING reason=FIRST_WIDGET_ENABLED")
                reRenderAllWidgets("FIRST_WIDGET_ENABLED")
            } else {
                prepareCandidateIfNeeded("FIRST_WIDGET_ENABLED")
            }
            reconcileAutoNextTimer("FIRST_WIDGET_ENABLED")
        }
    }

    fun onLastWidgetDisabled() {
        synchronized(stateLock) {
            Log.i(TAG_WIDGET, "[HomeWidget] action=LAST_WIDGET_DISABLED")
            activeWidgetIds.clear()
            cancelAutoNextTimer("LAST_WIDGET_DISABLED")
            stopAudioPlayback()
        }
    }

    fun hasActiveWidgets(): Boolean {
        synchronized(stateLock) {
            syncActiveWidgetIds()
            return activeWidgetIds.isNotEmpty()
        }
    }

    private fun prepareCandidateIfNeeded(reason: String) {
        synchronized(stateLock) {
            if (currentCandidate != null) {
                Log.i(TAG_INIT, "[HomeWidgetInit] widgetCount=${activeWidgetIds.size} currentCandidate=${currentCandidate?.contentId?.value} action=USE_EXISTING reason=$reason")
                reRenderAllWidgets(reason)
                return
            }
            if (isPreparingCandidate) {
                Log.i(TAG_INIT, "[HomeWidgetInit] widgetCount=${activeWidgetIds.size} currentCandidate=null action=WAIT_IN_FLIGHT reason=$reason")
                return
            }
            isPreparingCandidate = true
            Log.i(TAG_INIT, "[HomeWidgetInit] widgetCount=${activeWidgetIds.size} currentCandidate=null action=PREPARE_FIRST reason=$reason")
        }

        executor.execute {
            val startTime = System.currentTimeMillis()
            val settings = preferencesController.currentHomeWidget()
            val selection = selector.selectHomeWidget(settings)
            val candidate = (selection as? AndroidVocabularyCandidateSelectionResult.Selected)?.candidate
            val durationMs = System.currentTimeMillis() - startTime

            synchronized(stateLock) {
                isPreparingCandidate = false
                if (candidate != null) {
                    currentCandidate = candidate
                    currentCycleToken++
                    val currentSettings = preferencesController.currentHomeWidget()
                    preferencesController.updateHomeWidgetSettings(
                        currentSettings.copy(
                            selectedPackageId = currentSettings.selectedPackageId ?: candidate.packageId.value,
                            currentCandidateId = candidate.contentId.value
                        )
                    )
                    Log.i(TAG_INIT, "[HomeWidgetInit] candidateId=${candidate.contentId.value} durationMs=$durationMs action=FIRST_CANDIDATE_READY")

                    syncActiveWidgetIds()
                    if (activeWidgetIds.isNotEmpty()) {
                        reRenderWidgets(activeWidgetIds.toIntArray(), "FIRST_CANDIDATE_READY")
                    }

                    checkAndTriggerAutoPlay(candidate, "FIRST_CANDIDATE_READY")
                } else {
                    Log.i(TAG_INIT, "[HomeWidgetInit] widgetCount=${activeWidgetIds.size} currentCandidate=null action=FAIL reason=NO_CANDIDATE")
                }
            }
        }
    }

    fun advanceToNextCandidate(reason: String) {
        synchronized(stateLock) {
            if (isAdvancingCandidate) {
                Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] action=SKIP_OVERLAPPING_ADVANCE reason=$reason")
                return
            }
            isAdvancingCandidate = true
        }

        executor.execute {
            val settings = preferencesController.currentHomeWidget()
            val selection = selector.selectHomeWidget(settings)
            val candidate = (selection as? AndroidVocabularyCandidateSelectionResult.Selected)?.candidate

            synchronized(stateLock) {
                isAdvancingCandidate = false
                currentCandidate = candidate
                if (candidate != null) {
                    currentCycleToken++
                    val currentSettings = preferencesController.currentHomeWidget()
                    preferencesController.updateHomeWidgetSettings(
                        currentSettings.copy(currentCandidateId = candidate.contentId.value)
                    )
                }

                syncActiveWidgetIds()
                if (activeWidgetIds.isNotEmpty()) {
                    reRenderWidgets(activeWidgetIds.toIntArray(), reason)
                }

                if (candidate != null) {
                    checkAndTriggerAutoPlay(candidate, reason)
                }
            }
        }
    }

    fun goToNextCandidate(reason: String) {
        Log.i(TAG_NAV, "[HomeWidgetNavigation] action=NEXT reason=$reason")
        advanceToNextCandidate(reason)
        reconcileAutoNextTimer("USER_NAV_NEXT")
    }

    fun goToPreviousCandidate(reason: String) {
        synchronized(stateLock) {
            if (isAdvancingCandidate) {
                Log.i(TAG_NAV, "[HomeWidgetNavigation] action=SKIP_OVERLAPPING_PREVIOUS reason=$reason")
                return
            }
            isAdvancingCandidate = true
        }

        executor.execute {
            val settings = preferencesController.currentHomeWidget()
            val selection = selector.selectPreviousHomeWidget(settings)
            val candidate = (selection as? AndroidVocabularyCandidateSelectionResult.Selected)?.candidate

            synchronized(stateLock) {
                isAdvancingCandidate = false
                currentCandidate = candidate
                if (candidate != null) {
                    currentCycleToken++
                    val currentSettings = preferencesController.currentHomeWidget()
                    preferencesController.updateHomeWidgetSettings(
                        currentSettings.copy(currentCandidateId = candidate.contentId.value)
                    )
                }

                syncActiveWidgetIds()
                if (activeWidgetIds.isNotEmpty()) {
                    reRenderWidgets(activeWidgetIds.toIntArray(), reason)
                }

                Log.i(TAG_NAV, "[HomeWidgetNavigation] action=PREVIOUS candidateId=${candidate?.contentId?.value} reason=$reason")

                if (candidate != null) {
                    checkAndTriggerAutoPlay(candidate, reason)
                }
            }
        }
        reconcileAutoNextTimer("USER_NAV_PREVIOUS")
    }

    // -------------------------------------------------------------
    // QUICK ACTION: MARK DIFFICULT (STAR)
    // -------------------------------------------------------------
    fun toggleDifficult(appWidgetId: Int, packageId: String?, contentId: String?) {
        synchronized(stateLock) {
            val candidate = currentCandidate
            val targetContentId = contentId?.takeIf { it.isNotBlank() } ?: candidate?.contentId?.value

            if (targetContentId == null) {
                Log.w(TAG_ACTION, "[HomeWidgetQuickAction] appWidgetId=$appWidgetId action=TOGGLE_DIFFICULT status=IGNORED reason=NO_TARGET_CANDIDATE")
                return
            }

            val contentIdObj = vn.loi.learning.domain.content.model.ContentId(targetContentId)
            val markedBefore = difficultMarkers.isMarked(contentIdObj)
            val markedAfter = difficultMarkers.toggle(contentIdObj)

            Log.i(
                TAG_ACTION,
                "[HomeWidgetQuickAction] appWidgetId=$appWidgetId candidateId=$targetContentId action=TOGGLE_DIFFICULT markedBefore=$markedBefore markedAfter=$markedAfter fsrsMutation=false"
            )

            reRenderAllWidgets("TOGGLE_DIFFICULT")
        }
    }

    // -------------------------------------------------------------
    // QUICK ACTION: AUTO-AUDIO TOGGLE
    // -------------------------------------------------------------
    fun toggleAutoAudio(appWidgetId: Int, packageId: String?, contentId: String?) {
        synchronized(stateLock) {
            val settings = preferencesController.currentHomeWidget()
            val enabledBefore = settings.autoAudioEnabled
            val enabledAfter = !enabledBefore

            preferencesController.updateHomeWidgetSettings(settings.copy(autoAudioEnabled = enabledAfter))

            val candidate = currentCandidate
            val targetContentId = contentId ?: candidate?.contentId?.value ?: "UNKNOWN"

            Log.i(
                TAG_AUDIO_PREF,
                "[HomeWidgetAudioPreference] action=TOGGLE before=$enabledBefore after=$enabledAfter persisted=true visual=${if (enabledAfter) "UNMUTED_NORMAL" else "MUTED_RED"} appWidgetId=$appWidgetId candidateId=$targetContentId"
            )

            reRenderAllWidgets("TOGGLE_AUDIO")

            if (enabledAfter && candidate != null) {
                // Immediate confirmation playback
                lastAutoPlayedCandidateId = candidate.contentId.value
                lastAutoPlayedCycleToken = currentCycleToken
                playCandidateAudio(candidate, "TOGGLE_AUDIO_CONFIRMATION", appWidgetId)
            } else if (!enabledAfter) {
                stopAudioPlayback()
            }
        }
    }

    // -------------------------------------------------------------
    // QUICK ACTION / BODY TAP: MANUAL PLAY
    // -------------------------------------------------------------
    fun playManualAudio(appWidgetId: Int, packageId: String?, contentId: String?, trigger: String = "MANUAL_PLAY") {
        synchronized(stateLock) {
            val candidate = currentCandidate
            val settings = preferencesController.currentHomeWidget()
            if (candidate == null) {
                Log.w(
                    TAG_AUDIO_PLAY,
                    "[HomeWidgetAudioPlayback] appWidgetId=$appWidgetId candidateId=NONE trigger=$trigger autoAudioEnabled=${settings.autoAudioEnabled} played=false reason=NO_CANDIDATE"
                )
                return
            }

            playCandidateAudio(candidate, trigger, appWidgetId)
        }
    }

    // -------------------------------------------------------------
    // AUDIO ENGINE IMPLEMENTATION
    // -------------------------------------------------------------
    private fun checkAndTriggerAutoPlay(candidate: AndroidVocabularyCandidate, reason: String) {
        val settings = preferencesController.currentHomeWidget()
        val candidateId = candidate.contentId.value
        val cycle = currentCycleToken

        if (!settings.autoAudioEnabled) {
            Log.i(
                TAG_AUDIO_PLAY,
                "[HomeWidgetAudioPlayback] appWidgetId=0 candidateId=$candidateId trigger=CANDIDATE_TRANSITION autoAudioEnabled=false screenInteractive=${isScreenInteractive()} played=false skipReason=AUTO_AUDIO_DISABLED generation=$cycle"
            )
            return
        }

        if (!isScreenInteractive()) {
            Log.i(
                TAG_AUDIO_PLAY,
                "[HomeWidgetAudioPlayback] appWidgetId=0 candidateId=$candidateId trigger=CANDIDATE_TRANSITION autoAudioEnabled=true screenInteractive=false played=false skipReason=SCREEN_NOT_INTERACTIVE generation=$cycle"
            )
            return
        }

        if (lastAutoPlayedCandidateId == candidateId && lastAutoPlayedCycleToken == cycle) {
            Log.i(
                TAG_AUDIO_PLAY,
                "[HomeWidgetAudioPlayback] appWidgetId=0 candidateId=$candidateId trigger=CANDIDATE_TRANSITION autoAudioEnabled=true screenInteractive=true played=false skipReason=ALREADY_PLAYED_FOR_GENERATION generation=$cycle"
            )
            return
        }

        val audioRef = candidate.primaryAudioReference
        val audioPath = audioRef?.let(resolveMedia)

        if (audioPath == null || !File(audioPath).exists()) {
            Log.i(
                TAG_AUDIO_PLAY,
                "[HomeWidgetAudioPlayback] appWidgetId=0 candidateId=$candidateId trigger=CANDIDATE_TRANSITION autoAudioEnabled=true screenInteractive=true played=false skipReason=NO_AUDIO generation=$cycle"
            )
            return
        }

        lastAutoPlayedCandidateId = candidateId
        lastAutoPlayedCycleToken = cycle

        Log.i(
            TAG_AUDIO_PLAY,
            "[HomeWidgetAudioPlayback] appWidgetId=0 candidateId=$candidateId trigger=CANDIDATE_TRANSITION autoAudioEnabled=true screenInteractive=true played=true skipReason=NONE generation=$cycle"
        )

        playCandidateAudio(candidate, "CANDIDATE_TRANSITION", 0)
    }

    private fun playCandidateAudio(
        candidate: AndroidVocabularyCandidate,
        trigger: String,
        appWidgetId: Int
    ) {
        val candidateId = candidate.contentId.value
        val audioRef = candidate.primaryAudioReference
        val audioPath = audioRef?.let(resolveMedia)
        val autoAudioEnabled = preferencesController.currentHomeWidget().autoAudioEnabled

        if (audioPath == null || !File(audioPath).exists()) {
            Log.i(
                TAG_AUDIO_PLAY,
                "[HomeWidgetAudioPlayback] appWidgetId=$appWidgetId candidateId=$candidateId trigger=$trigger autoAudioEnabled=$autoAudioEnabled played=false reason=NO_AUDIO"
            )
            return
        }

        synchronized(audioLock) {
            stopAudioPlayback()

            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .build()
                    )
                    setDataSource(context, Uri.fromFile(File(audioPath)))
                    setOnCompletionListener { player ->
                        synchronized(audioLock) {
                            if (mediaPlayer == player) {
                                mediaPlayer?.release()
                                mediaPlayer = null
                            }
                        }
                    }
                    setOnErrorListener { player, what, extra ->
                        Log.e(TAG_AUDIO_PLAY, "[HomeWidgetAudioPlayback] error what=$what extra=$extra candidateId=$candidateId")
                        synchronized(audioLock) {
                            if (mediaPlayer == player) {
                                mediaPlayer?.release()
                                mediaPlayer = null
                            }
                        }
                        true
                    }
                    prepare()
                    start()
                }
                mediaPlayer = mp

                Log.i(
                    TAG_AUDIO_PLAY,
                    "[HomeWidgetAudioPlayback] appWidgetId=$appWidgetId candidateId=$candidateId trigger=$trigger autoAudioEnabled=$autoAudioEnabled played=true reason=PLAYING"
                )
            } catch (t: Throwable) {
                Log.e(TAG_AUDIO_PLAY, "[HomeWidgetAudioPlayback] playbackFailed candidateId=$candidateId error=${t.message}", t)
            }
        }
    }

    fun stopAudioPlayback() {
        synchronized(audioLock) {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }
            } catch (_: Throwable) {
            } finally {
                mediaPlayer = null
            }
        }
    }

    fun reRenderAllWidgets(reason: String) {
        synchronized(stateLock) {
            syncActiveWidgetIds()
            if (activeWidgetIds.isNotEmpty()) {
                reRenderWidgets(activeWidgetIds.toIntArray(), reason)
            }
        }
    }

    private fun reRenderWidgets(widgetIds: IntArray, reason: String) {
        val settings = preferencesController.currentHomeWidget()
        val candidate = currentCandidate
        val isDifficult = if (candidate != null) {
            difficultMarkers.isMarked(candidate.contentId)
        } else false

        Log.i(
            "HomeWidgetAudioState",
            "[HomeWidgetAudioState] candidateId=${candidate?.contentId?.value ?: "EMPTY"} source=$reason persistedAutoAudioEnabled=${settings.autoAudioEnabled} renderedIcon=${if (settings.autoAudioEnabled) "SPEAKER" else "MUTED"} tint=${if (settings.autoAudioEnabled) "NORMAL" else "RED"} preferenceWrite=false"
        )

        val manager = AppWidgetManager.getInstance(context)
        for (widgetId in widgetIds) {
            val views = AndroidHomeVocabularyWidgetRenderer.renderWidget(
                context = context,
                candidate = candidate,
                settings = settings,
                resolveMedia = resolveMedia,
                widgetCount = widgetIds.size,
                appWidgetId = widgetId,
                isDifficult = isDifficult
            )
            manager.updateAppWidget(widgetId, views)
        }
    }

    fun reconcileAutoNextTimer(reason: String) {
        synchronized(stateLock) {
            syncActiveWidgetIds()
            val settings = preferencesController.currentHomeWidget()
            val hasWidgets = activeWidgetIds.isNotEmpty()
            val isScreenOn = isScreenInteractive()
            val shouldRunTimer = hasWidgets && settings.autoNextEnabled && (!settings.updateOnlyScreenOn || isScreenOn)

            if (!shouldRunTimer) {
                cancelAutoNextTimer(reason)
                return
            }

            val interval = settings.clampedIntervalMillis
            if (autoNextRunnable != null && armedIntervalMs == interval) {
                return
            }

            cancelAutoNextTimer("RE_ARM")
            armedIntervalMs = interval
            Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] action=START intervalMs=$interval reason=$reason")

            val runnable = object : Runnable {
                override fun run() {
                    synchronized(stateLock) {
                        if (activeWidgetIds.isNotEmpty()) {
                            val curSettings = preferencesController.currentHomeWidget()
                            val curScreenOn = isScreenInteractive()
                            if (curSettings.autoNextEnabled && (!curSettings.updateOnlyScreenOn || curScreenOn)) {
                                Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] action=FIRE intervalMs=$interval")
                                advanceToNextCandidate("TIMER_FIRED")
                                mainHandler.postDelayed(this, interval)
                                return
                            }
                        }
                        cancelAutoNextTimer("TIMER_CONDITIONS_CHANGED")
                    }
                }
            }
            autoNextRunnable = runnable
            mainHandler.postDelayed(runnable, interval)
        }
    }

    private fun cancelAutoNextTimer(reason: String) {
        autoNextRunnable?.let {
            mainHandler.removeCallbacks(it)
            autoNextRunnable = null
            armedIntervalMs = 0L
            Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] action=CANCEL reason=$reason")
        }
    }

    private fun syncActiveWidgetIds() {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AndroidHomeVocabularyWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids != null) {
            activeWidgetIds.clear()
            for (id in ids) {
                activeWidgetIds.add(id)
            }
        }
    }

    private fun isScreenInteractive(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isInteractive ?: true
    }

    companion object {
        private const val TAG_WIDGET = "HomeWidget"
        private const val TAG_SCHEDULER = "HomeWidgetScheduler"
        private const val TAG_INIT = "HomeWidgetInit"
        private const val TAG_ACTION = "HomeWidgetQuickAction"
        private const val TAG_AUDIO_PREF = "HomeWidgetAudioPreference"
        private const val TAG_AUDIO_PLAY = "HomeWidgetAudioPlayback"
        private const val TAG_NAV = "HomeWidgetNavigation"
    }
}
