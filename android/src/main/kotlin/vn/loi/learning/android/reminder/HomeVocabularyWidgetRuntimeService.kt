package vn.loi.learning.android.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import vn.loi.learning.android.LearningEngineAndroidApplication
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

interface HomeWidgetRuntimeClock {
    fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean = false)
    fun cancel(reason: String)
    fun isArmed(): Boolean
    fun armedInterval(): Long
}

class HomeVocabularyWidgetRuntimeService : Service(), HomeWidgetRuntimeClock {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null
    private var currentIntervalMs: Long = 0L
    private var lastScheduledElapsedRealtime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        instance = this
        val app = application as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.registerRuntimeClock(this)
        logFgsState("SERVICE_CREATED", "STOPPED", false, 0L)
        Log.i(
            TAG,
            "[HOME_WIDGET_RUNTIME_SERVICE_START] pid=${Process.myPid()} action=ON_CREATE"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? LearningEngineAndroidApplication
        val coordinator = app?.homeVocabularyWidgetCoordinator

        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (coordinator?.shouldRuntimeServiceRun() != true) {
            Log.i(
                TAG,
                "[HOME_WIDGET_RUNTIME_SERVICE_STOP] pid=${Process.myPid()} reason=REQUIREMENT_NOT_MET action=STOP_SELF"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        if (!startAsForeground()) {
            return START_NOT_STICKY
        }

        coordinator.registerRuntimeClock(this)
        logFgsState("SERVICE_STARTED", if (isArmed()) "ARMED" else "PAUSED", isArmed(), currentIntervalMs)
        coordinator.reconcileRuntimeClock("SERVICE_STARTED")

        return START_STICKY
    }

    private fun startAsForeground(): Boolean {
        val notification = buildServiceNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            stopSelf()
            false
        }
    }

    override fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
        mainHandler.post {
            val app = application as? LearningEngineAndroidApplication
            val coordinator = app?.homeVocabularyWidgetCoordinator

            if (!freshInterval && tickRunnable != null && currentIntervalMs == intervalMs) {
                return@post
            }

            cancelInternal("RESCHEDULE (fresh=$freshInterval): $reason")

            currentIntervalMs = intervalMs
            lastScheduledElapsedRealtime = SystemClock.elapsedRealtime()

            val runnable = object : Runnable {
                override fun run() {
                    if (tickRunnable !== this) return
                    val now = SystemClock.elapsedRealtime()
                    logFgsState("CLOCK_TICK", "ARMED", true, currentIntervalMs)
                    Log.i(
                        TAG,
                        "[HOME_WIDGET_RUNTIME_TICK] pid=${Process.myPid()} intervalMs=$currentIntervalMs elapsedRealtime=$now deviceState=${coordinator?.currentDeviceState} foregroundState=${coordinator?.homeForegroundState} widgetCount=${coordinator?.getActiveWidgetCount()}"
                    )

                    coordinator?.onRuntimeTick()

                    // Re-arm next interval if clock is still eligible (screen on, unlocked, widgets exist, auto-next on)
                    if (tickRunnable === this) {
                        if (coordinator?.isRuntimeClockEligible() == true) {
                            lastScheduledElapsedRealtime = SystemClock.elapsedRealtime()
                            mainHandler.postDelayed(this, currentIntervalMs)
                        } else {
                            cancelInternal("GATE_CLOSED_AFTER_TICK")
                        }
                    }
                }
            }

            tickRunnable = runnable
            logFgsState("CLOCK_SCHEDULE: $reason", "ARMED", true, intervalMs)
            Log.i(
                TAG,
                "[HOME_WIDGET_RUNTIME_RESUME] pid=${Process.myPid()} intervalMs=$intervalMs reason=$reason deviceState=${coordinator?.currentDeviceState} foregroundState=${coordinator?.homeForegroundState} widgetCount=${coordinator?.getActiveWidgetCount()}"
            )
            mainHandler.postDelayed(runnable, intervalMs)
        }
    }

    override fun cancel(reason: String) {
        mainHandler.post {
            cancelInternal(reason)
        }
    }

    private fun cancelInternal(reason: String) {
        val wasArmed = tickRunnable != null
        tickRunnable?.let { mainHandler.removeCallbacks(it) }
        tickRunnable = null
        val prevInterval = currentIntervalMs
        currentIntervalMs = 0L

        if (wasArmed) {
            val app = application as? LearningEngineAndroidApplication
            val coordinator = app?.homeVocabularyWidgetCoordinator
            logFgsState("CLOCK_PAUSE: $reason", "PAUSED", false, 0L)
            Log.i(
                TAG,
                "[HOME_WIDGET_RUNTIME_PAUSE] pid=${Process.myPid()} reason=$reason deviceState=${coordinator?.currentDeviceState} foregroundState=${coordinator?.homeForegroundState} widgetCount=${coordinator?.getActiveWidgetCount()}"
            )
        }
    }

    override fun isArmed(): Boolean = tickRunnable != null
    override fun armedInterval(): Long = currentIntervalMs

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        logFgsState("CLOCK_CANCEL: SERVICE_DESTROYED", "STOPPED", false, 0L)
        cancelInternal("SERVICE_DESTROYED")
        instance = null
        super.onDestroy()
    }

    fun logFgsState(reason: String, clockState: String, scheduled: Boolean, intervalMs: Long) {
        val app = application as? LearningEngineAndroidApplication
        val coordinator = app?.homeVocabularyWidgetCoordinator
        val hasWidgets = coordinator?.hasActiveWidgets() ?: false
        val autoNextEnabled = coordinator?.isAutoNextEnabled() ?: false
        val runtimeAllowed = coordinator?.shouldHomeWidgetAutoNextRun() ?: false

        Log.i(
            TAG,
            "[HOME_WIDGET_FGS_STATE] pid=${Process.myPid()} serviceInstance=${instance != null} clockState=$clockState scheduled=$scheduled intervalMs=$intervalMs deviceState=${coordinator?.currentDeviceState} foregroundState=${coordinator?.homeForegroundState} hasWidgets=$hasWidgets autoNextEnabled=$autoNextEnabled runtimeAllowed=$runtimeAllowed reason=$reason"
        )
    }

    private fun buildServiceNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(getString(R.string.fgs_notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fgs_channel_desc)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "HomeWidgetScheduler"
        const val CHANNEL_ID = "home_vocabulary_widget_runtime_channel"
        const val NOTIFICATION_ID = 4002
        const val ACTION_START = "vn.loi.learning.android.action.START_HOME_WIDGET_RUNTIME"
        const val ACTION_STOP = "vn.loi.learning.android.action.STOP_HOME_WIDGET_RUNTIME"

        @Volatile
        var instance: HomeVocabularyWidgetRuntimeService? = null
            private set

        fun ensureRunning(context: Context) {
            if (instance != null) return
            try {
                val intent = Intent(context, HomeVocabularyWidgetRuntimeService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to ensure HomeVocabularyWidgetRuntimeService is running: ${e.message}")
            }
        }

        fun ensureStopped(context: Context) {
            try {
                val intent = Intent(context, HomeVocabularyWidgetRuntimeService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to ensure HomeVocabularyWidgetRuntimeService is stopped: ${e.message}")
            }
        }
    }
}
