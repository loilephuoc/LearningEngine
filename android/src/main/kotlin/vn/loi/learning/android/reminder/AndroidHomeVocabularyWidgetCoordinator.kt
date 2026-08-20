package vn.loi.learning.android.reminder

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
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
    private val resolveMedia: (String) -> String? = { null }
) {

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val started = AtomicBoolean(false)
    private val stateLock = Any()

    private val activeWidgetIds = mutableSetOf<Int>()
    private var currentCandidate: AndroidVocabularyCandidate? = null
    private var isPreparingCandidate: Boolean = false
    private var isAdvancingCandidate: Boolean = false
    private var autoNextRunnable: Runnable? = null
    private var armedIntervalMs: Long = 0L

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] event=SCREEN_ON action=RESUME_TIMER")
                    reconcileAutoNextTimer("SCREEN_ON")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG_SCHEDULER, "[HomeWidgetScheduler] event=SCREEN_OFF action=SUSPEND_TIMER")
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
                    preferencesController.updateHomeWidgetSettings(
                        settings.copy(
                            selectedPackageId = settings.selectedPackageId ?: candidate.packageId.value,
                            currentCandidateId = candidate.contentId.value
                        )
                    )
                    Log.i(TAG_INIT, "[HomeWidgetInit] candidateId=${candidate.contentId.value} durationMs=$durationMs action=FIRST_CANDIDATE_READY")

                    syncActiveWidgetIds()
                    if (activeWidgetIds.isNotEmpty()) {
                        reRenderWidgets(activeWidgetIds.toIntArray(), "FIRST_CANDIDATE_READY")
                    }
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
                    preferencesController.updateHomeWidgetSettings(
                        settings.copy(currentCandidateId = candidate.contentId.value)
                    )
                }

                syncActiveWidgetIds()
                if (activeWidgetIds.isNotEmpty()) {
                    reRenderWidgets(activeWidgetIds.toIntArray(), reason)
                }
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
        val manager = AppWidgetManager.getInstance(context)
        for (widgetId in widgetIds) {
            val views = AndroidHomeVocabularyWidgetRenderer.renderWidget(
                context = context,
                candidate = candidate,
                settings = settings,
                resolveMedia = resolveMedia,
                widgetCount = widgetIds.size,
                appWidgetId = widgetId
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
    }
}
