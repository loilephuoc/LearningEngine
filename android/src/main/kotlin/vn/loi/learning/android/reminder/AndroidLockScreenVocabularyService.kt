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
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import vn.loi.learning.android.LearningEngineAndroidApplication
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

data class ForegroundServiceRequirement(
    val lockScreenRequired: Boolean,
    val unlockedReminderRequired: Boolean,
    val homeWidgetRequired: Boolean
) {
    val required: Boolean
        get() = lockScreenRequired || unlockedReminderRequired || homeWidgetRequired
}

class AndroidLockScreenVocabularyService : Service() {

    override fun onCreate() {
        super.onCreate()
        createServiceChannel()
        val req = evaluateRequirement(this)
        if (!req.required) {
            Log.i(TAG, "[ReminderServiceLifecycle] reason=SERVICE_ON_CREATE required=false action=STOP_SELF")
            stopSelf()
            return
        }
        if (startAsForeground()) {
            val app = application as? LearningEngineAndroidApplication
            app?.lockScreenVocabularyCoordinator?.start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val req = evaluateRequirement(this)
        if (!req.required) {
            Log.i(TAG, "[ReminderServiceLifecycle] reason=SERVICE_ON_START required=false action=STOP_SELF")
            stopSelf()
            return START_NOT_STICKY
        }
        Log.i(TAG, "[ReminderServiceLifecycle] reason=SERVICE_ON_START required=true action=CONTINUE")
        if (startAsForeground()) {
            val app = application as? LearningEngineAndroidApplication
            app?.lockScreenVocabularyCoordinator?.start()
            app?.lockScreenVocabularyCoordinator?.reconcileUnlockedReminderSchedule("SERVICE_STARTED")
            return START_STICKY
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground(): Boolean {
        val now = System.currentTimeMillis()
        val app = application as? LearningEngineAndroidApplication
        val settings = app?.reminderPreferencesController?.current()
        val isIndefinite = settings != null && settings.enabled && settings.unlockedPausedIndefinitely
        val isTimedPaused = settings != null && settings.enabled && settings.unlockedPausedUntilEpochMillis > now
        val isPaused = isIndefinite || isTimedPaused

        val notification = buildServiceNotification(
            context = this,
            isPaused = isPaused,
            isIndefinite = isIndefinite,
            pausedUntilEpochMillis = settings?.unlockedPausedUntilEpochMillis ?: 0L,
            now = now
        )

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

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lock-screen Vocabulary Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains the lock-screen vocabulary feature so a card can be presented when screen wakes."
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = application as? LearningEngineAndroidApplication
        app?.lockScreenVocabularyCoordinator?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LockScreenVocabService"
        const val CHANNEL_ID = "lockscreen_vocabulary_service"
        const val NOTIFICATION_ID = 20261
        const val LEGACY_PAUSE_NOTIFICATION_ID = 20311

        const val FGS_RESUME_ACTION_REQUEST_CODE = 4050
        const val FGS_RESUME_BODY_REQUEST_CODE = 4051

        fun evaluateRequirement(context: Context): ForegroundServiceRequirement {
            val app = context.applicationContext as? LearningEngineAndroidApplication
            val lockRequired = app?.reminderPreferencesController?.currentLockScreen()?.enabled == true
            val reminderRequired = app?.reminderPreferencesController?.current()?.enabled == true
            val widgetRequired = (app?.homeVocabularyWidgetCoordinator?.hasActiveWidgets() == true) &&
                (app?.reminderPreferencesController?.currentHomeWidget()?.autoNextEnabled == true)
            return ForegroundServiceRequirement(
                lockScreenRequired = lockRequired,
                unlockedReminderRequired = reminderRequired,
                homeWidgetRequired = widgetRequired
            )
        }

        fun reconcile(context: Context, reason: String): ForegroundServiceRequirement {
            val req = evaluateRequirement(context)
            Log.i(
                TAG,
                "[ReminderServiceLifecycle] reason=$reason lockRequired=${req.lockScreenRequired} reminderRequired=${req.unlockedReminderRequired} widgetRequired=${req.homeWidgetRequired} required=${req.required} action=${if (req.required) "START" else "STOP"}"
            )
            if (req.required) {
                start(context)
            } else {
                stop(context)
            }
            return req
        }

        fun buildServiceNotification(
            context: Context,
            isPaused: Boolean,
            isIndefinite: Boolean = false,
            pausedUntilEpochMillis: Long,
            now: Long
        ): Notification {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            // Migration cleanup: cancel legacy secondary notification if present
            manager?.cancel(LEGACY_PAUSE_NOTIFICATION_ID)

            if (isPaused) {
                val contentText = if (isIndefinite) {
                    "Đã tạm dừng vô thời hạn"
                } else {
                    val remainingMinutes = ((pausedUntilEpochMillis - now) / 60_000L).coerceAtLeast(1L)
                    val timeStr = Instant.ofEpochMilli(pausedUntilEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))

                    val durationText = when (remainingMinutes) {
                        60L -> "1 hour"
                        else -> "$remainingMinutes min"
                    }
                    "Paused for $durationText · until $timeStr"
                }

                // Body tap -> Resume now
                val bodyResumeIntent = Intent(context, AndroidVocabularyReminderResumeReceiver::class.java).apply {
                    action = AndroidVocabularyReminderNotificationHelper.ACTION_RESUME_UNLOCKED_NOW
                    putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_RESUME_SOURCE, "FGS_NOTIFICATION_BODY")
                }
                val bodyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    FGS_RESUME_BODY_REQUEST_CODE,
                    bodyResumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Action button tap -> Resume now
                val actionResumeIntent = Intent(context, AndroidVocabularyReminderResumeReceiver::class.java).apply {
                    action = AndroidVocabularyReminderNotificationHelper.ACTION_RESUME_UNLOCKED_NOW
                    putExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_RESUME_SOURCE, "FGS_NOTIFICATION_ACTION")
                }
                val actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    FGS_RESUME_ACTION_REQUEST_CODE,
                    actionResumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Custom RemoteViews for visible collapsed Resume now button
                val remoteViews = RemoteViews(context.packageName, R.layout.notification_unlocked_pause_status).apply {
                    setTextViewText(R.id.pause_notif_title, "Vocabulary Reminder Service")
                    setTextViewText(R.id.pause_notif_text, contentText)
                    setOnClickPendingIntent(R.id.pause_resume_now, actionPendingIntent)
                }

                Log.i(TAG, "[ReminderServiceNotification] state=PAUSED isIndefinite=$isIndefinite pausedUntil=$pausedUntilEpochMillis remainingMs=${pausedUntilEpochMillis - now} action=UPDATE")

                return NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_reminder)
                    .setContentTitle("Vocabulary Reminder Service")
                    .setContentText(contentText)
                    .setContentIntent(bodyPendingIntent)
                    .setCustomContentView(remoteViews)
                    .setCustomBigContentView(remoteViews)
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .addAction(
                        R.drawable.ic_notification_reminder,
                        "Resume now",
                        actionPendingIntent
                    )
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setOngoing(true)
                    .build()
            } else {
                val contentIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                Log.i(TAG, "[ReminderServiceNotification] state=ACTIVE action=UPDATE")

                return NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_reminder)
                    .setContentTitle("Vocabulary Reminder Service")
                    .setContentText("Ôn tập màn hình khóa và nhắc từ vựng đang hoạt động.")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(contentIntent)
                    .setOngoing(true)
                    .build()
            }
        }

        fun updateNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val now = System.currentTimeMillis()
            val app = context.applicationContext as? LearningEngineAndroidApplication
            val settings = app?.reminderPreferencesController?.current()
            val isIndefinite = settings != null && settings.enabled && settings.unlockedPausedIndefinitely
            val isTimedPaused = settings != null && settings.enabled && settings.unlockedPausedUntilEpochMillis > now
            val isPaused = isIndefinite || isTimedPaused

            val notification = buildServiceNotification(
                context = context,
                isPaused = isPaused,
                isIndefinite = isIndefinite,
                pausedUntilEpochMillis = settings?.unlockedPausedUntilEpochMillis ?: 0L,
                now = now
            )
            manager.notify(NOTIFICATION_ID, notification)
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, AndroidLockScreenVocabularyService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start AndroidLockScreenVocabularyService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AndroidLockScreenVocabularyService::class.java)
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to stop AndroidLockScreenVocabularyService: ${e.message}", e)
            }
        }
    }
}
