package vn.loi.learning.android.reminder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.util.Log
import vn.loi.learning.android.LearningEngineAndroidApplication

class AndroidHomeVocabularyWidgetProvider : AppWidgetProvider() {

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
    }
}
