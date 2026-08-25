package vn.loi.learning.android.family

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import vn.loi.learning.android.BuildConfig
import vn.loi.learning.android.LearningEngineAndroidApplication
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

enum class FamilyAlarmPrecision { EXACT, INEXACT_FALLBACK }

fun selectFamilyAlarmPrecision(sdkInt: Int, canScheduleExactAlarms: Boolean): FamilyAlarmPrecision =
    if (sdkInt < Build.VERSION_CODES.S || canScheduleExactAlarms) FamilyAlarmPrecision.EXACT
    else FamilyAlarmPrecision.INEXACT_FALLBACK

class AndroidFamilyReminderScheduler(private val context: Context) : FamilyReminderScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    val precision: FamilyAlarmPrecision
        get() = selectFamilyAlarmPrecision(
            Build.VERSION.SDK_INT,
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        )

    override fun scheduledKeys(): Set<String> = preferences.getStringSet(KEY_ACTIVE, emptySet()).orEmpty().toSet()

    override fun schedule(reminder: ScheduledReminder) {
        val pending = deliveryPendingIntent(reminder.scheduleKey, reminder.requestCode, reminder)
        val triggerMillis = reminder.triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (precision == FamilyAlarmPrecision.EXACT) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
        }
        updateKeys(scheduledKeys() + reminder.scheduleKey)
    }

    override fun cancel(scheduleKey: String) {
        val requestCode = FamilyReminderIdentity.stableId(scheduleKey)
        alarmManager.cancel(deliveryPendingIntent(scheduleKey, requestCode, null))
        updateKeys(scheduledKeys() - scheduleKey)
    }

    fun markDelivered(scheduleKey: String) = updateKeys(scheduledKeys() - scheduleKey)

    fun scheduleDebugTest(delaySeconds: Long = 10L) {
        check(BuildConfig.DEBUG) { "Debug notification helper is unavailable in release builds" }
        val key = "debug-family-notification"
        val intent = Intent(context, FamilyReminderReceiver::class.java).apply {
            action = FamilyReminderReceiver.ACTION_DEBUG_TEST
            putExtra(EXTRA_SCHEDULE_KEY, key)
            putExtra(EXTRA_NOTIFICATION_ID, DEBUG_NOTIFICATION_ID)
            putExtra(EXTRA_TITLE, "🔔 Thông báo thử Ngày đáng nhớ")
            putExtra(EXTRA_BODY, "Thông báo thử đã được gửi thành công.")
            putExtra(EXTRA_OCCURRENCE_DATE, LocalDate.now().toString())
        }
        val pending = PendingIntent.getBroadcast(context, DEBUG_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val trigger = System.currentTimeMillis() + delaySeconds * 1000L
        if (precision == FamilyAlarmPrecision.EXACT) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    private fun deliveryPendingIntent(key: String, requestCode: Int, reminder: ScheduledReminder?): PendingIntent {
        val intent = Intent(context, FamilyReminderReceiver::class.java).apply {
            action = FamilyReminderReceiver.ACTION_DELIVER
            putExtra(EXTRA_SCHEDULE_KEY, key)
            reminder?.let {
                putExtra(EXTRA_NOTIFICATION_ID, it.notificationId)
                putExtra(EXTRA_TITLE, it.title)
                putExtra(EXTRA_BODY, it.body)
                putExtra(EXTRA_OCCURRENCE_DATE, it.occurrenceDate.toString())
                putExtra(EXTRA_TARGET_TYPE, it.targetType.name)
                putExtra(EXTRA_TARGET_ID, it.targetId)
            }
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun updateKeys(keys: Set<String>) {
        preferences.edit().putStringSet(KEY_ACTIVE, keys.toSet()).apply()
    }

    companion object {
        private const val PREFERENCES = "family_reminder_schedules_v1"
        private const val KEY_ACTIVE = "active_schedule_keys"
        private const val DEBUG_REQUEST_CODE = 893001
        private const val DEBUG_NOTIFICATION_ID = 893002
        const val EXTRA_SCHEDULE_KEY = "family_schedule_key"
        const val EXTRA_NOTIFICATION_ID = "family_notification_id"
        const val EXTRA_TITLE = "family_notification_title"
        const val EXTRA_BODY = "family_notification_body"
        const val EXTRA_OCCURRENCE_DATE = "family_occurrence_date"
        const val EXTRA_TARGET_TYPE = "family_target_type"
        const val EXTRA_TARGET_ID = "family_target_id"
    }
}

class FamilyNotificationPublisher(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Ngày đáng nhớ", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Nhắc sinh nhật, sự kiện và công việc"
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun publish(intent: Intent): Boolean {
        if (!hasPermission()) return false
        val id = intent.getIntExtra(AndroidFamilyReminderScheduler.EXTRA_NOTIFICATION_ID, 0).takeIf { it != 0 } ?: return false
        val date = intent.getStringExtra(AndroidFamilyReminderScheduler.EXTRA_OCCURRENCE_DATE)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_FAMILY
            putExtra(AndroidFamilyReminderScheduler.EXTRA_OCCURRENCE_DATE, date)
            putExtra(AndroidFamilyReminderScheduler.EXTRA_TARGET_TYPE, intent.getStringExtra(AndroidFamilyReminderScheduler.EXTRA_TARGET_TYPE))
            putExtra(AndroidFamilyReminderScheduler.EXTRA_TARGET_ID, intent.getStringExtra(AndroidFamilyReminderScheduler.EXTRA_TARGET_ID))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(context, id, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val body = intent.getStringExtra(AndroidFamilyReminderScheduler.EXTRA_BODY).orEmpty()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(intent.getStringExtra(AndroidFamilyReminderScheduler.EXTRA_TITLE).orEmpty())
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "important_dates_reminders"
        const val ACTION_OPEN_FAMILY = "vn.loi.learning.android.action.OPEN_FAMILY_REMINDER"
        const val ACTION_OPEN_FAMILY_QUICK_ADD = "vn.loi.learning.android.action.OPEN_FAMILY_QUICK_ADD"
        const val EXTRA_QUICK_ADD_TYPE = "EXTRA_QUICK_ADD_TYPE"
    }
}

class FamilyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val incoming = intent ?: return
        val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
        when (incoming.action) {
            ACTION_DELIVER -> {
                // Android-delivered alarms may be slightly late; delivery remains valid.
                app.familyNotificationPublisher.publish(incoming)
                incoming.getStringExtra(AndroidFamilyReminderScheduler.EXTRA_SCHEDULE_KEY)?.let(app.familyReminderScheduler::markDelivered)
                app.familyReminderReconciler.reconcile()
            }
            ACTION_DEBUG_TEST -> app.familyNotificationPublisher.publish(incoming)
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> app.familyReminderReconciler.reconcile(forceReschedule = true)
        }
    }

    companion object {
        const val ACTION_DELIVER = "vn.loi.learning.android.action.FAMILY_REMINDER_DELIVER"
        const val ACTION_DEBUG_TEST = "vn.loi.learning.android.action.FAMILY_REMINDER_DEBUG_TEST"
    }
}
