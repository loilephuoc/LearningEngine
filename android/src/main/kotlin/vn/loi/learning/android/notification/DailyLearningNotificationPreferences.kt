package vn.loi.learning.android.notification

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.loi.learning.android.platform.coordinatedCommit

data class DailyDueReviewReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
    val lastNotifiedDate: String? = null
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
}

data class DailyInactivityReminderSettings(
    val enabled: Boolean = false,
    val thresholdDays: Int = 3,
    val hour: Int = 9,
    val minute: Int = 0,
    val lastNotifiedDate: String? = null
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
}

data class DailyLearningNotificationSettings(
    val dueReview: DailyDueReviewReminderSettings = DailyDueReviewReminderSettings(),
    val inactivity: DailyInactivityReminderSettings = DailyInactivityReminderSettings(),
    val lastActivityTimestamp: Long = System.currentTimeMillis()
)

interface DailyLearningNotificationPreferenceStore {
    fun load(): DailyLearningNotificationSettings
    fun save(settings: DailyLearningNotificationSettings): Boolean
}

class SharedPreferencesDailyLearningNotificationStore(
    context: Context,
    private val prefsName: String = "learning_daily_notifications"
) : DailyLearningNotificationPreferenceStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun load(): DailyLearningNotificationSettings {
        val dueEnabled = prefs.getBoolean("due_review_enabled", false)
        val dueHour = prefs.getInt("due_review_hour", 20).coerceIn(0, 23)
        val dueMinute = prefs.getInt("due_review_minute", 0).coerceIn(0, 59)
        val dueLastDate = prefs.getString("due_review_last_date", null)

        val inactEnabled = prefs.getBoolean("inactivity_enabled", false)
        val inactThreshold = prefs.getInt("inactivity_threshold_days", 3).let {
            if (it in listOf(1, 2, 3, 5, 7)) it else 3
        }
        val inactHour = prefs.getInt("inactivity_hour", 9).coerceIn(0, 23)
        val inactMinute = prefs.getInt("inactivity_minute", 0).coerceIn(0, 59)
        val inactLastDate = prefs.getString("inactivity_last_date", null)

        val lastActivity = prefs.getLong("last_activity_timestamp", System.currentTimeMillis())

        return DailyLearningNotificationSettings(
            dueReview = DailyDueReviewReminderSettings(dueEnabled, dueHour, dueMinute, dueLastDate),
            inactivity = DailyInactivityReminderSettings(inactEnabled, inactThreshold, inactHour, inactMinute, inactLastDate),
            lastActivityTimestamp = lastActivity
        )
    }

    override fun save(settings: DailyLearningNotificationSettings): Boolean {
        return prefs.edit().apply {
            putBoolean("due_review_enabled", settings.dueReview.enabled)
            putInt("due_review_hour", settings.dueReview.hour)
            putInt("due_review_minute", settings.dueReview.minute)
            putString("due_review_last_date", settings.dueReview.lastNotifiedDate)

            putBoolean("inactivity_enabled", settings.inactivity.enabled)
            putInt("inactivity_threshold_days", settings.inactivity.thresholdDays)
            putInt("inactivity_hour", settings.inactivity.hour)
            putInt("inactivity_minute", settings.inactivity.minute)
            putString("inactivity_last_date", settings.inactivity.lastNotifiedDate)

            putLong("last_activity_timestamp", settings.lastActivityTimestamp)
        }.coordinatedCommit()
    }
}

class DailyLearningNotificationPreferencesController(
    private val store: DailyLearningNotificationPreferenceStore
) {
    private val mutableSettings = MutableStateFlow(store.load())
    val settings: StateFlow<DailyLearningNotificationSettings> = mutableSettings.asStateFlow()

    fun current(): DailyLearningNotificationSettings = mutableSettings.value

    fun updateDueReview(enabled: Boolean? = null, hour: Int? = null, minute: Int? = null, lastNotifiedDate: String? = null): Boolean {
        val current = mutableSettings.value
        val updated = current.copy(
            dueReview = current.dueReview.copy(
                enabled = enabled ?: current.dueReview.enabled,
                hour = hour?.coerceIn(0, 23) ?: current.dueReview.hour,
                minute = minute?.coerceIn(0, 59) ?: current.dueReview.minute,
                lastNotifiedDate = lastNotifiedDate ?: current.dueReview.lastNotifiedDate
            )
        )
        val saved = store.save(updated)
        if (saved) mutableSettings.value = updated
        return saved
    }

    fun updateInactivity(enabled: Boolean? = null, thresholdDays: Int? = null, hour: Int? = null, minute: Int? = null, lastNotifiedDate: String? = null): Boolean {
        val current = mutableSettings.value
        val updated = current.copy(
            inactivity = current.inactivity.copy(
                enabled = enabled ?: current.inactivity.enabled,
                thresholdDays = thresholdDays?.let { if (it in listOf(1, 2, 3, 5, 7)) it else current.inactivity.thresholdDays } ?: current.inactivity.thresholdDays,
                hour = hour?.coerceIn(0, 23) ?: current.inactivity.hour,
                minute = minute?.coerceIn(0, 59) ?: current.inactivity.minute,
                lastNotifiedDate = lastNotifiedDate ?: current.inactivity.lastNotifiedDate
            )
        )
        val saved = store.save(updated)
        if (saved) mutableSettings.value = updated
        return saved
    }

    fun recordActivity(timestamp: Long = System.currentTimeMillis()): Boolean {
        val current = mutableSettings.value
        val updated = current.copy(lastActivityTimestamp = timestamp)
        val saved = store.save(updated)
        if (saved) mutableSettings.value = updated
        return saved
    }
}
