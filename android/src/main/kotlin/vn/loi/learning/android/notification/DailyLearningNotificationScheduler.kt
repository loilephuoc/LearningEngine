package vn.loi.learning.android.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DailyLearningNotificationScheduler(
    private val context: Context,
    private val preferencesController: DailyLearningNotificationPreferencesController
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun reconcile() {
        val settings = preferencesController.current()
        scheduleDueReview(settings.dueReview)
        scheduleInactivity(settings.inactivity)
    }

    fun scheduleDueReview(settings: DailyDueReviewReminderSettings) {
        val am = alarmManager ?: return
        val intent = Intent(context, DailyLearningReminderReceiver::class.java).apply {
            action = DailyLearningReminderReceiver.ACTION_DUE_REVIEW_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DUE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settings.enabled) {
            am.cancel(pendingIntent)
            return
        }

        val triggerMillis = computeNextTriggerMillis(settings.hour, settings.minute, settings.lastNotifiedDate)
        AlarmManagerCompat.setAndAllowWhileIdle(
            am,
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    fun scheduleInactivity(settings: DailyInactivityReminderSettings) {
        val am = alarmManager ?: return
        val intent = Intent(context, DailyLearningReminderReceiver::class.java).apply {
            action = DailyLearningReminderReceiver.ACTION_INACTIVITY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_INACTIVITY_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settings.enabled) {
            am.cancel(pendingIntent)
            return
        }

        val triggerMillis = computeNextTriggerMillis(settings.hour, settings.minute, settings.lastNotifiedDate)
        AlarmManagerCompat.setAndAllowWhileIdle(
            am,
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    companion object {
        const val REQUEST_CODE_DUE_ALARM = 9001
        const val REQUEST_CODE_INACTIVITY_ALARM = 9002

        fun computeNextTriggerMillis(
            hour: Int,
            minute: Int,
            lastNotifiedDate: String?,
            nowDateTime: LocalDateTime = LocalDateTime.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val todayDate = nowDateTime.toLocalDate()
            val targetTimeToday = LocalDateTime.of(todayDate, LocalTime.of(hour, minute, 0))

            val alreadyNotifiedToday = lastNotifiedDate == todayDate.toString()
            val targetDateTime = if (alreadyNotifiedToday || nowDateTime.isAfter(targetTimeToday) || nowDateTime.isEqual(targetTimeToday)) {
                targetTimeToday.plusDays(1)
            } else {
                targetTimeToday
            }
            return targetDateTime.atZone(zoneId).toInstant().toEpochMilli()
        }
    }
}
