package vn.loi.learning.android.reminder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import vn.loi.learning.android.LearningEngineAndroidApplication

class AndroidHomeVocabularyWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
        val coordinator = app.homeVocabularyWidgetCoordinator

        val appWidgetId = intent.getIntExtra(EXTRA_APP_WIDGET_ID, 0)
        val packageId = intent.getStringExtra(EXTRA_PACKAGE_ID)
        val contentId = intent.getStringExtra(EXTRA_CONTENT_ID)
        val trigger = intent.getStringExtra(EXTRA_TRIGGER) ?: "MANUAL_PLAY"

        when (action) {
            ACTION_WIDGET_TOGGLE_DIFFICULT -> {
                Log.i(TAG, "[HomeWidgetBroadcast] action=TOGGLE_DIFFICULT appWidgetId=$appWidgetId candidateId=$contentId")
                coordinator.toggleDifficult(appWidgetId, packageId, contentId)
            }
            ACTION_WIDGET_TOGGLE_AUDIO -> {
                Log.i(TAG, "[HomeWidgetBroadcast] action=TOGGLE_AUDIO appWidgetId=$appWidgetId candidateId=$contentId")
                coordinator.toggleAutoAudio(appWidgetId, packageId, contentId)
            }
            ACTION_WIDGET_MANUAL_PLAY -> {
                Log.i(TAG, "[HomeWidgetBroadcast] action=MANUAL_PLAY appWidgetId=$appWidgetId candidateId=$contentId trigger=$trigger")
                coordinator.playManualAudio(appWidgetId, packageId, contentId, trigger)
            }
            ACTION_WIDGET_PREVIOUS -> {
                Log.i(TAG, "[HomeWidgetBroadcast] action=PREVIOUS appWidgetId=$appWidgetId candidateId=$contentId")
                coordinator.goToPreviousCandidate("USER_TAP_PREVIOUS")
            }
            ACTION_WIDGET_NEXT -> {
                Log.i(TAG, "[HomeWidgetBroadcast] action=NEXT appWidgetId=$appWidgetId candidateId=$contentId")
                coordinator.goToNextCandidate("USER_TAP_NEXT")
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.i(TAG, "[HomeWidget] action=UPDATE widgetCount=${appWidgetIds.size} ids=${appWidgetIds.joinToString()}")
        val app = context.applicationContext as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.onWidgetsUpdate(appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.i(TAG, "[HomeWidget] action=OPTIONS_CHANGED widgetId=$appWidgetId")
        val app = context.applicationContext as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.onWidgetOptionsChanged(appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.i(TAG, "[HomeWidget] action=DELETED widgetCount=${appWidgetIds.size} ids=${appWidgetIds.joinToString()}")
        val app = context.applicationContext as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.onWidgetsDeleted(appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.i(TAG, "[HomeWidget] action=FIRST_WIDGET_ENABLED")
        val app = context.applicationContext as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.onFirstWidgetEnabled()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i(TAG, "[HomeWidget] action=LAST_WIDGET_DISABLED")
        val app = context.applicationContext as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.onLastWidgetDisabled()
    }

    companion object {
        private const val TAG = "HomeWidget"

        const val ACTION_WIDGET_TOGGLE_DIFFICULT = "vn.loi.learning.android.action.HOME_WIDGET_TOGGLE_DIFFICULT"
        const val ACTION_WIDGET_TOGGLE_AUDIO = "vn.loi.learning.android.action.HOME_WIDGET_TOGGLE_AUDIO"
        const val ACTION_WIDGET_MANUAL_PLAY = "vn.loi.learning.android.action.HOME_WIDGET_MANUAL_PLAY"
        const val ACTION_WIDGET_PREVIOUS = "vn.loi.learning.android.action.HOME_WIDGET_PREVIOUS"
        const val ACTION_WIDGET_NEXT = "vn.loi.learning.android.action.HOME_WIDGET_NEXT"

        const val EXTRA_APP_WIDGET_ID = "extra_app_widget_id"
        const val EXTRA_PACKAGE_ID = "extra_package_id"
        const val EXTRA_CONTENT_ID = "extra_content_id"
        const val EXTRA_TRIGGER = "extra_trigger"
    }
}
