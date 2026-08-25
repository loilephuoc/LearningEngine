package vn.loi.learning.android.family.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import vn.loi.learning.android.LearningEngineAndroidApplication

class FamilyAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.i(TAG, "[FamilyWidget] onReceive action=$action")

        when (action) {
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_UPDATE_FAMILY_WIDGET -> {
                updateAll(context)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.i(TAG, "[FamilyWidget] onUpdate widgetCount=${appWidgetIds.size}")
        val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
        val snapshot = runCatching { app.familyRepository.snapshot.value }.getOrNull() ?: return

        for (appWidgetId in appWidgetIds) {
            val views = FamilyAppWidgetRenderer.render(context, appWidgetManager, appWidgetId, snapshot)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
        val snapshot = runCatching { app.familyRepository.snapshot.value }.getOrNull() ?: return
        val views = FamilyAppWidgetRenderer.render(context, appWidgetManager, appWidgetId, snapshot)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val TAG = "FamilyAppWidget"
        const val ACTION_UPDATE_FAMILY_WIDGET = "vn.loi.learning.android.family.widget.ACTION_UPDATE_FAMILY_WIDGET"

        fun updateAll(context: Context) {
            val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
            val snapshot = runCatching { app.familyRepository.snapshot.value }.getOrNull() ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, FamilyAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            if (appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    val views = FamilyAppWidgetRenderer.render(context, appWidgetManager, appWidgetId, snapshot)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }

            FamilyMonthAppWidgetProvider.updateAll(context)
        }
    }
}
