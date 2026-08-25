package vn.loi.learning.android.family.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import vn.loi.learning.android.LearningEngineAndroidApplication

class FamilyMonthAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.i(TAG, "[FamilyMonthWidget] onReceive action=$action")

        when (action) {
            ACTION_PREV_MONTH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val current = FamilyMonthWidgetPreferences.getDisplayedYearMonth(context, appWidgetId)
                    val prev = current.minusMonths(1)
                    FamilyMonthWidgetPreferences.setDisplayedYearMonth(context, appWidgetId, prev)
                    updateSingleWidget(context, appWidgetId)
                }
            }
            ACTION_NEXT_MONTH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val current = FamilyMonthWidgetPreferences.getDisplayedYearMonth(context, appWidgetId)
                    val next = current.plusMonths(1)
                    FamilyMonthWidgetPreferences.setDisplayedYearMonth(context, appWidgetId, next)
                    updateSingleWidget(context, appWidgetId)
                }
            }
            ACTION_TODAY_MONTH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    FamilyMonthWidgetPreferences.resetToCurrentMonth(context, appWidgetId)
                    updateSingleWidget(context, appWidgetId)
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            FamilyAppWidgetProvider.ACTION_UPDATE_FAMILY_WIDGET -> {
                updateAll(context)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.i(TAG, "[FamilyMonthWidget] onUpdate widgetCount=${appWidgetIds.size}")
        val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
        val snapshot = runCatching { app.familyRepository.snapshot.value }.getOrNull() ?: return

        for (appWidgetId in appWidgetIds) {
            val views = FamilyMonthAppWidgetRenderer.render(context, appWidgetManager, appWidgetId, snapshot)
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
        val views = FamilyMonthAppWidgetRenderer.render(context, appWidgetManager, appWidgetId, snapshot)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.i(TAG, "[FamilyMonthWidget] onDeleted count=${appWidgetIds.size}")
        FamilyMonthWidgetPreferences.clearWidgets(context, appWidgetIds)
    }

    companion object {
        const val TAG = "FamilyMonthAppWidget"
        const val ACTION_PREV_MONTH = "vn.loi.learning.android.family.widget.ACTION_PREV_MONTH"
        const val ACTION_NEXT_MONTH = "vn.loi.learning.android.family.widget.ACTION_NEXT_MONTH"
        const val ACTION_TODAY_MONTH = "vn.loi.learning.android.family.widget.ACTION_TODAY_MONTH"

        fun updateSingleWidget(context: Context, appWidgetId: Int) {
            val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
            val snapshot = runCatching { app.familyRepository.snapshot.value }.getOrNull() ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val views = FamilyMonthAppWidgetRenderer.render(context, appWidgetManager, appWidgetId, snapshot)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAll(context: Context) {
            val app = context.applicationContext as? LearningEngineAndroidApplication ?: return
            val snapshot = runCatching { app.familyRepository.snapshot.value }.getOrNull() ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, FamilyMonthAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            if (appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    val views = FamilyMonthAppWidgetRenderer.render(context, appWidgetManager, appWidgetId, snapshot)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
