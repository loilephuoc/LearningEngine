package vn.loi.learning.android.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId
import vn.loi.learning.android.LearningEngineAndroidApplication
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

class DailyLearningReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "onReceive action=$action")

        val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
        val prefsController = app.dailyNotificationPreferencesController
        val scheduler = app.dailyNotificationScheduler
        val notificationHelper = app.dailyNotificationHelper

        when (action) {
            ACTION_DUE_REVIEW_ALARM -> {
                handleDueReviewAlarm(app, prefsController, notificationHelper)
                scheduler.reconcile()
            }
            ACTION_INACTIVITY_ALARM -> {
                handleInactivityAlarm(app, prefsController, notificationHelper)
                scheduler.reconcile()
            }
            DailyLearningNotificationHelper.ACTION_DISABLE_DUE_REMINDER -> {
                Log.i(TAG, "User clicked Ẩn loại nhắc này for Due Review")
                prefsController.updateDueReview(enabled = false)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(DailyLearningNotificationHelper.NOTIFICATION_ID_DUE_REVIEW)
                scheduler.reconcile()
            }
            DailyLearningNotificationHelper.ACTION_DISABLE_INACTIVITY_REMINDER -> {
                Log.i(TAG, "User clicked Ẩn loại nhắc này for Inactivity")
                prefsController.updateInactivity(enabled = false)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(DailyLearningNotificationHelper.NOTIFICATION_ID_INACTIVITY)
                scheduler.reconcile()
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                Log.i(TAG, "System event $action -> reconciling daily notification schedules")
                scheduler.reconcile()
            }
        }
    }

    private fun handleDueReviewAlarm(
        app: LearningEngineAndroidApplication,
        prefsController: DailyLearningNotificationPreferencesController,
        notificationHelper: DailyLearningNotificationHelper
    ) {
        val settings = prefsController.current().dueReview
        if (!settings.enabled) return

        val todayStr = LocalDate.now().toString()
        if (settings.lastNotifiedDate == todayStr) {
            Log.d(TAG, "Due review notification already sent today ($todayStr), skipping")
            return
        }

        val engine = app.graph.engine
        val libraryId = engine.defaultLibraryId ?: return
        val activePackageId = engine.domainLibraryRepository?.findById(libraryId)?.activePackageId ?: return
        val pkg = engine.installedPackageRepository?.findById(activePackageId)
            ?.takeIf { it.libraryId == libraryId && it.state == PackageState.ACTIVE }
            ?: return

        val contents = engine.packageContentQuery?.getContentsForPackage(activePackageId).orEmpty()
        val contentIds = contents.map { ContentId(it.id) }.toSet()
        if (contentIds.isEmpty()) return

        val dueCount = engine.dailyStudyBudget?.execute(
            LearnerId("default-learner"),
            app.studyPreferencesController.current(),
            Moment(System.currentTimeMillis()),
            ZoneId.systemDefault(),
            contentIds
        )?.dueReviewCount ?: 0

        if (dueCount > 0) {
            val posted = notificationHelper.postDueReviewNotification(
                packageName = pkg.name.value,
                dueCount = dueCount,
                packageId = activePackageId.value
            )
            if (posted) {
                prefsController.updateDueReview(lastNotifiedDate = todayStr)
                Log.i(TAG, "Posted due review notification: dueCount=$dueCount package=${pkg.name.value}")
            }
        } else {
            Log.i(TAG, "No due reviews found (dueCount=0) for package ${pkg.name.value}, skipping notification")
        }
    }

    private fun handleInactivityAlarm(
        app: LearningEngineAndroidApplication,
        prefsController: DailyLearningNotificationPreferencesController,
        notificationHelper: DailyLearningNotificationHelper
    ) {
        val fullSettings = prefsController.current()
        val settings = fullSettings.inactivity
        if (!settings.enabled) return

        val todayStr = LocalDate.now().toString()
        if (settings.lastNotifiedDate == todayStr) {
            Log.d(TAG, "Inactivity notification already sent today ($todayStr), skipping")
            return
        }

        val nowMillis = System.currentTimeMillis()
        val daysInactive = ((nowMillis - fullSettings.lastActivityTimestamp) / (24 * 3600 * 1000L)).toInt()
        if (daysInactive < settings.thresholdDays) {
            Log.d(TAG, "Inactivity ($daysInactive days) below threshold (${settings.thresholdDays} days), skipping")
            return
        }

        val engine = app.graph.engine
        val libraryId = engine.defaultLibraryId ?: return
        val activePackageId = engine.domainLibraryRepository?.findById(libraryId)?.activePackageId ?: return
        val pkg = engine.installedPackageRepository?.findById(activePackageId)
            ?.takeIf { it.libraryId == libraryId && it.state == PackageState.ACTIVE }
            ?: return

        val contents = engine.packageContentQuery?.getContentsForPackage(activePackageId).orEmpty()
        val contentIds = contents.map { ContentId(it.id) }.toSet()
        if (contentIds.isEmpty()) return

        val budget = engine.dailyStudyBudget?.execute(
            LearnerId("default-learner"),
            app.studyPreferencesController.current(),
            Moment(nowMillis),
            ZoneId.systemDefault(),
            contentIds
        )
        val hasNewCandidates = (budget?.eligibleNewContentCount ?: 0) > 0

        if (hasNewCandidates) {
            val posted = notificationHelper.postInactivityReminderNotification(
                packageName = pkg.name.value,
                daysInactive = daysInactive,
                packageId = activePackageId.value
            )
            if (posted) {
                prefsController.updateInactivity(lastNotifiedDate = todayStr)
                Log.i(TAG, "Posted inactivity reminder: daysInactive=$daysInactive package=${pkg.name.value}")
            }
        } else {
            Log.i(TAG, "No eligible new candidates in package ${pkg.name.value}, skipping inactivity notification")
        }
    }

    companion object {
        private const val TAG = "DailyReminderReceiver"
        const val ACTION_DUE_REVIEW_ALARM = "vn.loi.learning.android.action.DUE_REVIEW_ALARM"
        const val ACTION_INACTIVITY_ALARM = "vn.loi.learning.android.action.INACTIVITY_ALARM"
    }
}
