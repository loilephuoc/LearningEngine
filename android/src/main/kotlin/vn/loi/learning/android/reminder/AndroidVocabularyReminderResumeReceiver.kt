package vn.loi.learning.android.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import vn.loi.learning.android.LearningEngineAndroidApplication

class AndroidVocabularyReminderResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == AndroidVocabularyReminderNotificationHelper.ACTION_RESUME_UNLOCKED_NOW) {
            val source = intent.getStringExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_RESUME_SOURCE) ?: "NOTIFICATION_ACTION"
            Log.i(TAG, "[UnlockedResumeNow] source=$source action=TAP")
            val app = context.applicationContext as? LearningEngineAndroidApplication
            app?.reminderNotificationHelper?.cancelPauseStatusNotification("RESUME_NOW")
            app?.lockScreenVocabularyCoordinator?.resumeUnlockedNow()
        }
    }

    companion object {
        private const val TAG = "VocabularyReminderResume"
    }
}
