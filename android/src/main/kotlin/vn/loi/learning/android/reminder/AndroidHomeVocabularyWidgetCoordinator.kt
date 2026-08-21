package vn.loi.learning.android.reminder

import android.app.KeyguardManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
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
import vn.loi.learning.android.controller.ControllerDiagnosticsHolder

enum class HomeSurfaceState {
    VISIBLE,
    HIDDEN,
    UNKNOWN
}

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

    @Volatile
    private var runtimeClock: HomeWidgetRuntimeClock? = null

    fun registerRuntimeClock(clock: HomeWidgetRuntimeClock) {
        runtimeClock = clock
    }

    fun unregisterRuntimeClock(clock: HomeWidgetRuntimeClock) {
        if (runtimeClock === clock) {
            runtimeClock = null
        }
    }

    fun shouldRuntimeServiceRun(): Boolean {
        syncActiveWidgetIds()
        val settings = preferencesController.currentHomeWidget()
        return activeWidgetIds.isNotEmpty() && settings.autoNextEnabled
    }

    fun isAutoNextEnabled(): Boolean {
        return preferencesController.currentHomeWidget().autoNextEnabled
    }

    fun getActiveWidgetCount(): Int {
        return synchronized(stateLock) {
            syncActiveWidgetIds()
            activeWidgetIds.size
        }
    }

    fun reconcileRuntimeServiceLifetime(reason: String) {
        if (shouldRuntimeServiceRun()) {
            if (runtimeClock == null && HomeVocabularyWidgetRuntimeService.instance == null) {
                HomeVocabularyWidgetRuntimeService.ensureRunning(context)
            }
        } else {
            HomeVocabularyWidgetRuntimeService.ensureStopped(context)
        }
    }

    fun onRuntimeTick() {
        synchronized(stateLock) {
            syncActiveWidgetIds()
            val settings = preferencesController.currentHomeWidget()
            val hasWidgets = activeWidgetIds.isNotEmpty()
            val runtimeAllowed = isHomeWidgetRuntimeAllowed()

            if (!hasWidgets || !settings.autoNextEnabled || (settings.updateOnlyScreenOn && !runtimeAllowed)) {
                Log.w(
                    TAG_SCHEDULER,
                    "[HOME_WIDGET_RUNTIME_GATE_REJECT] pid=${android.os.Process.myPid()} deviceState=$currentDeviceState homeSurfaceState=$homeSurfaceState widgetCount=${activeWidgetIds.size} autoNextEnabled=${settings.autoNextEnabled} runtimeAllowed=$runtimeAllowed"
                )
                return
            }

            Log.i(
                TAG_SCHEDULER,
                "[HOME_WIDGET_TIMER_FIRE] intervalMillis=${settings.clampedIntervalMillis} candidateId=${currentCandidate?.contentId?.value}"
            )
            Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] action=FIRE intervalMs=${settings.clampedIntervalMillis}")
            advanceToNextCandidate("RUNTIME_SERVICE_TICK")
        }
    }

    private var currentCycleToken: Long = 0L
    private var lastAutoPlayedCandidateId: String? = null
    private var lastAutoPlayedCycleToken: Long = 0L

    private var mediaPlayer: MediaPlayer? = null

    var currentDeviceState: VocabularyPresentationDeviceState = VocabularyPresentationDeviceState.SCREEN_OFF
        private set

    @Volatile
    var homeSurfaceState: HomeSurfaceState = HomeSurfaceState.UNKNOWN
        private set

    val homeSurfaceVisible: Boolean
        get() = homeSurfaceState == HomeSurfaceState.VISIBLE

    fun isHomeWidgetRuntimeAllowed(): Boolean {
        return currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE
    }

    @Volatile
    private var cachedDefaultLauncherPackage: String? = null
    @Volatile
    private var lastLauncherResolveTimeMs: Long = 0L

    fun resolveDefaultLauncherPackage(forceRefresh: Boolean = false): String? {
        val now = System.currentTimeMillis()
        val cached = cachedDefaultLauncherPackage
        if (!forceRefresh && cached != null && (now - lastLauncherResolveTimeMs < LAUNCHER_CACHE_TTL_MS)) {
            return cached
        }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val pkg = resolveInfo?.activityInfo?.packageName
        cachedDefaultLauncherPackage = pkg
        lastLauncherResolveTimeMs = now
        return pkg
    }

    fun isLauncherPackage(pkg: String?, defaultLauncher: String? = resolveDefaultLauncherPackage()): Boolean {
        return Companion.isLauncherPackage(pkg, defaultLauncher)
    }

    fun isTransientSystemPackage(pkg: String?): Boolean {
        return Companion.isTransientSystemPackage(pkg)
    }

    fun setHomeSurfaceState(state: HomeSurfaceState, reason: String) {
        synchronized(stateLock) {
            val oldState = homeSurfaceState
            homeSurfaceState = state
            val wasAllowed = (currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && oldState == HomeSurfaceState.VISIBLE)
            val nowAllowed = isHomeWidgetRuntimeAllowed()

            Log.i(
                TAG_SCHEDULER,
                "[HomeWidgetHomeSurface] from=$oldState to=$state reason=$reason runtimeWasAllowed=$wasAllowed runtimeNowAllowed=$nowAllowed"
            )

            val reasonTag = when (state) {
                HomeSurfaceState.VISIBLE -> "HOME_VISIBLE"
                HomeSurfaceState.HIDDEN -> "HOME_HIDDEN"
                HomeSurfaceState.UNKNOWN -> "HOME_UNKNOWN"
            }

            if (wasAllowed && !nowAllowed) {
                stopAudioPlayback()
            }

            reconcileRuntimeClock("$reasonTag: $reason")
        }
    }

    fun setHomeSurfaceVisible(visible: Boolean, reason: String) {
        setHomeSurfaceState(
            if (visible) HomeSurfaceState.VISIBLE else HomeSurfaceState.HIDDEN,
            reason
        )
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    val isLocked = isKeyguardLocked()
                    val targetState = if (isLocked) {
                        VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
                    } else {
                        VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
                    }
                    transitionDeviceState(targetState, "SCREEN_ON(isLocked=$isLocked)")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    transitionDeviceState(VocabularyPresentationDeviceState.SCREEN_OFF, "SCREEN_OFF")
                }
                Intent.ACTION_USER_PRESENT -> {
                    transitionDeviceState(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, "USER_PRESENT")
                }
            }
        }
    }

    fun transitionDeviceState(newState: VocabularyPresentationDeviceState, reason: String) {
        synchronized(stateLock) {
            val oldState = currentDeviceState
            if (oldState == newState && newState != VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) {
                return
            }
            currentDeviceState = newState

            val actionDesc = when (newState) {
                VocabularyPresentationDeviceState.SCREEN_OFF -> "CANCEL_TIMER_STOP_AUDIO"
                VocabularyPresentationDeviceState.LOCKED_SCREEN_ON -> "KEEP_PAUSED"
                VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON -> "EVALUATE_HOME_VISIBILITY"
            }

            val settings = preferencesController.currentHomeWidget()
            val interval = settings.clampedIntervalMillis

            Log.i(
                TAG_SCHEDULER,
                "[HomeWidgetDeviceState] from=$oldState to=$newState reason=$reason action=$actionDesc intervalMs=$interval"
            )

            when (newState) {
                VocabularyPresentationDeviceState.SCREEN_OFF -> {
                    stopAudioPlayback()
                    reconcileRuntimeClock("DEVICE_SCREEN_OFF: $reason")
                }
                VocabularyPresentationDeviceState.LOCKED_SCREEN_ON -> {
                    stopAudioPlayback()
                    reconcileRuntimeClock("DEVICE_LOCKED: $reason")
                }
                VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON -> {
                    val resolvedState = vn.loi.learning.android.controller.ControllerSystemActionBridge.reconcileHomeSurface()
                    if (resolvedState != HomeSurfaceState.UNKNOWN) {
                        homeSurfaceState = resolvedState
                    }
                    reconcileRuntimeClock("DEVICE_UNLOCKED: $reason")
                    reconcilePostUnlockHomeState()
                }
            }
        }
    }

    fun reconcilePostUnlockHomeState() {
        mainHandler.postDelayed({
            synchronized(stateLock) {
                if (currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) {
                    val resolvedState = vn.loi.learning.android.controller.ControllerSystemActionBridge.reconcileHomeSurface()
                    if (resolvedState != HomeSurfaceState.UNKNOWN) {
                        homeSurfaceState = resolvedState
                    }
                    reconcileRuntimeClock("POST_UNLOCK_RECONCILE")
                }
            }
        }, 150)
    }

    fun handleScreenOff() = transitionDeviceState(VocabularyPresentationDeviceState.SCREEN_OFF, "DIRECT_SCREEN_OFF")
    fun handleScreenOn(isLocked: Boolean) = transitionDeviceState(
        if (isLocked) VocabularyPresentationDeviceState.LOCKED_SCREEN_ON else VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON,
        "DIRECT_SCREEN_ON"
    )
    fun handleUserPresent() = transitionDeviceState(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, "DIRECT_USER_PRESENT")

    fun start() {
        if (!started.compareAndSet(false, true)) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(screenStateReceiver, filter)

        // Evaluate initial device state
        val initialState = resolveCurrentDeviceState()
        transitionDeviceState(initialState, "START_INITIAL_EVALUATION")

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

        var lastObservedSettings: AndroidHomeVocabularyWidgetSettings? = preferencesController.currentHomeWidget()

        // Observe settings changes
        coordinatorScope.launch {
            preferencesController.homeWidgetSettings.collectLatest { newSettings ->
                val prev = lastObservedSettings
                lastObservedSettings = newSettings
                if (prev == null || prev.hasVisualOrScheduleChanges(newSettings)) {
                    synchronized(stateLock) {
                        reconcileAutoNextTimer("SETTINGS_UPDATED")
                        reRenderAllWidgets("SETTINGS_UPDATED")
                    }
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
                AndroidLockScreenVocabularyService.reconcile(context, "ALL_WIDGETS_REMOVED")
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
            AndroidLockScreenVocabularyService.reconcile(context, "FIRST_WIDGET_ENABLED")
        }
    }

    fun onLastWidgetDisabled() {
        synchronized(stateLock) {
            Log.i(TAG_WIDGET, "[HomeWidget] action=LAST_WIDGET_DISABLED")
            activeWidgetIds.clear()
            cancelAutoNextTimer("LAST_WIDGET_DISABLED")
            stopAudioPlayback()
            AndroidLockScreenVocabularyService.reconcile(context, "LAST_WIDGET_DISABLED")
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
        reconcileRuntimeServiceLifetime("MANUAL_NEXT")
        reconcileRuntimeClock("USER_NAV_NEXT", freshInterval = true)
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
        reconcileRuntimeServiceLifetime("MANUAL_PREVIOUS")
        reconcileRuntimeClock("USER_NAV_PREVIOUS", freshInterval = true)
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

        if (!isHomeWidgetRuntimeAllowed()) {
            Log.i(
                TAG_AUDIO_PLAY,
                "[HomeWidgetAudioPlayback] candidateId=$candidateId trigger=CANDIDATE_TRANSITION played=false skipReason=HOME_SURFACE_NOT_ALLOWED state=$currentDeviceState homeSurfaceState=$homeSurfaceState"
            )
            return
        }

        val isPopupActive = try {
            val app = context.applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
            app?.reminderOverlayController?.isShowing == true
        } catch (_: Throwable) {
            false
        }

        if (isPopupActive) {
            Log.i(
                TAG_AUDIO_PLAY,
                "[HomeWidgetAudioPlayback] appWidgetId=0 candidateId=$candidateId trigger=CANDIDATE_TRANSITION autoAudioEnabled=true played=false skipReason=POPUP_ACTIVE generation=$cycle"
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

        synchronized(stateLock) {
            stopAudioPlayback()

            try {
                val mp = MediaPlayer().apply {
                    setDataSource(audioPath)
                    prepare()
                    setOnCompletionListener { player ->
                        synchronized(stateLock) {
                            if (mediaPlayer == player) {
                                mediaPlayer?.release()
                                mediaPlayer = null
                            }
                        }
                    }
                    setOnErrorListener { player, what, extra ->
                        Log.e(TAG_AUDIO_PLAY, "[HomeWidgetAudioPlayback] error what=$what extra=$extra candidateId=$candidateId")
                        synchronized(stateLock) {
                            if (mediaPlayer == player) {
                                mediaPlayer?.release()
                                mediaPlayer = null
                            }
                        }
                        true
                    }
                    start()
                }
                mediaPlayer = mp
                Log.i(TAG_AUDIO_PLAY, "[HomeWidgetAudioPlayback] candidateId=$candidateId action=PLAY status=SUCCESS trigger=$trigger")
            } catch (e: Throwable) {
                Log.e(TAG_AUDIO_PLAY, "[HomeWidgetAudioPlayback] candidateId=$candidateId action=PLAY status=FAILED error=${e.message}", e)
                stopAudioPlayback()
            }
        }
    }

    fun stopAudioPlayback() {
        synchronized(stateLock) {
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

    fun reconcileRuntimeClock(reason: String, freshInterval: Boolean = false) {
        synchronized(stateLock) {
            syncActiveWidgetIds()
            val settings = preferencesController.currentHomeWidget()
            val hasWidgets = activeWidgetIds.isNotEmpty()
            val homeWidgetRuntimeAllowed = isHomeWidgetRuntimeAllowed()
            val shouldRunTimer = hasWidgets && settings.autoNextEnabled && (!settings.updateOnlyScreenOn || homeWidgetRuntimeAllowed)
            val interval = settings.clampedIntervalMillis

            val clockState = when {
                !hasWidgets || !settings.autoNextEnabled -> "STOPPED"
                !shouldRunTimer -> "PAUSED"
                else -> "ARMED"
            }

            Log.i(
                TAG_SCHEDULER,
                "[HOME_WIDGET_FGS_STATE] pid=${android.os.Process.myPid()} serviceInstance=${HomeVocabularyWidgetRuntimeService.instance != null} clockState=$clockState scheduled=$shouldRunTimer intervalMs=${if (shouldRunTimer) interval else 0L} deviceState=$currentDeviceState homeSurfaceState=$homeSurfaceState hasWidgets=$hasWidgets autoNextEnabled=${settings.autoNextEnabled} runtimeAllowed=$homeWidgetRuntimeAllowed reason=$reason (fresh=$freshInterval)"
            )
            Log.i(
                TAG_SCHEDULER,
                "[HOME_WIDGET_RUNTIME] deviceState=$currentDeviceState homeSurfaceState=$homeSurfaceState hasWidgets=$hasWidgets autoNextEnabled=${settings.autoNextEnabled} intervalMillis=$interval runtimeAllowed=$homeWidgetRuntimeAllowed timerScheduled=${isTimerScheduled()} reason=$reason (fresh=$freshInterval)"
            )

            reconcileRuntimeServiceLifetime(reason)

            if (!shouldRunTimer) {
                if (currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON) {
                    Log.i(TAG_SCHEDULER, "[HomeWidgetAutoNextTimer] state=LOCKED_SCREEN_ON action=SUPPRESSED")
                } else if (currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && homeSurfaceState != HomeSurfaceState.VISIBLE) {
                    Log.i(TAG_SCHEDULER, "[HomeWidgetAutoNextTimer] state=UNLOCKED_OTHER_APP homeSurfaceState=$homeSurfaceState action=SUPPRESSED")
                }
                cancelAutoNextTimer(reason)
                return
            }

            val clock = runtimeClock
            if (clock != null) {
                clock.schedule(interval, reason, freshInterval)
                autoNextRunnable?.let { mainHandler.removeCallbacks(it) }
                autoNextRunnable = null
                armedIntervalMs = 0L
                return
            }

            if (!freshInterval && autoNextRunnable != null && armedIntervalMs == interval) {
                return
            }

            cancelAutoNextTimer("RE_ARM (fresh=$freshInterval)")
            armedIntervalMs = interval
            Log.i(TAG_SCHEDULER, "[HOME_WIDGET_TIMER_SCHEDULE] intervalMillis=$interval reason=$reason (fresh=$freshInterval)")
            Log.i(TAG_SCHEDULER, "[HomeWidgetAutoNextTimer] state=UNLOCKED_SCREEN_ON homeSurfaceState=$homeSurfaceState action=START intervalMs=$interval reason=$reason")

            val runnable = object : Runnable {
                override fun run() {
                    synchronized(stateLock) {
                        if (runtimeClock != null) {
                            autoNextRunnable = null
                            return
                        }
                        if (activeWidgetIds.isNotEmpty()) {
                            val curSettings = preferencesController.currentHomeWidget()
                            val curRuntimeAllowed = isHomeWidgetRuntimeAllowed()
                            if (curSettings.autoNextEnabled && (!curSettings.updateOnlyScreenOn || curRuntimeAllowed)) {
                                Log.i(TAG_SCHEDULER, "[HOME_WIDGET_TIMER_FIRE] intervalMillis=$interval candidateId=${currentCandidate?.contentId?.value}")
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

    fun reconcileAutoNextTimer(reason: String) = reconcileRuntimeClock(reason)

    fun isTimerScheduled(): Boolean {
        return runtimeClock?.isArmed() == true || autoNextRunnable != null
    }

    private fun cancelAutoNextTimer(reason: String) {
        val wasArmed = isTimerScheduled() || armedIntervalMs != 0L
        runtimeClock?.cancel(reason)
        autoNextRunnable?.let {
            mainHandler.removeCallbacks(it)
        }
        autoNextRunnable = null
        armedIntervalMs = 0L
        if (wasArmed) {
            Log.i(TAG_SCHEDULER, "[HOME_WIDGET_TIMER_CANCEL] reason=$reason")
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

    fun isScreenInteractive(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isInteractive ?: true
    }

    fun isKeyguardLocked(): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }

    fun resolveCurrentDeviceState(): VocabularyPresentationDeviceState {
        val interactive = isScreenInteractive()
        val locked = isKeyguardLocked()
        return when {
            !interactive -> VocabularyPresentationDeviceState.SCREEN_OFF
            locked -> VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
            else -> VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        }
    }

    companion object {
        private const val LAUNCHER_CACHE_TTL_MS = 60_000L
        private const val TAG_WIDGET = "HomeWidget"
        private const val TAG_SCHEDULER = "HomeWidgetScheduler"
        private const val TAG_INIT = "HomeWidgetInit"
        private const val TAG_ACTION = "HomeWidgetQuickAction"
        private const val TAG_AUDIO_PREF = "HomeWidgetAudioPreference"
        private const val TAG_AUDIO_PLAY = "HomeWidgetAudioPlayback"
        private const val TAG_NAV = "HomeWidgetNavigation"

        fun isTransientSystemPackage(pkg: String?): Boolean {
            if (pkg.isNullOrBlank()) return false
            val p = pkg.lowercase()
            if (p == "android" || p.startsWith("android.") || p.startsWith("com.android.systemui") || p.startsWith("com.android.providers.")) return true
            if (p.startsWith("miui.") || (p.startsWith("com.miui.") && p != "com.miui.home" && !p.contains("browser") && !p.contains("calculator") && !p.contains("notes"))) return true
            if (p.startsWith("com.xiaomi.") && !p.contains("shop") && !p.contains("market")) return true
            if (p.startsWith("com.mediatek.") || p.startsWith("com.qualcomm.")) return true
            if (p.contains("inputmethod") || p.contains("keyboard") || p.contains("volumnbutton") || p.contains("volumebutton")) return true
            if (p == "com.google.android.googlequicksearchbox" || p == "com.samsung.android.app.spage" || p == "com.samsung.android.app.cocktailbarservice") return true
            return false
        }

        fun isLauncherPackage(pkg: String?, defaultLauncher: String? = null): Boolean {
            if (pkg.isNullOrBlank()) return false
            if (defaultLauncher != null && pkg == defaultLauncher) return true
            val knownLaunchers = setOf(
                "com.miui.home",
                "com.mi.android.globallauncher",
                "com.sec.android.app.launcher",
                "com.google.android.apps.nexuslauncher",
                "com.android.launcher3",
                "com.android.launcher",
                "com.oppo.launcher",
                "com.huawei.android.launcher",
                "com.transsion.launcher",
                "com.oneplus.launcher"
            )
            if (pkg in knownLaunchers || pkg.endsWith(".launcher") || pkg.endsWith(".home")) return true
            return false
        }
    }
}
