package vn.loi.learning.android.family.widget

import android.content.Context
import java.time.YearMonth

object FamilyMonthWidgetPreferences {
    private const val PREFS_NAME = "family_month_widget_prefs"
    private const val KEY_YEAR_PREFIX = "widget_year_"
    private const val KEY_MONTH_PREFIX = "widget_month_"

    fun getDisplayedYearMonth(context: Context, appWidgetId: Int): YearMonth {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = YearMonth.now()
        val year = prefs.getInt("$KEY_YEAR_PREFIX$appWidgetId", now.year)
        val month = prefs.getInt("$KEY_MONTH_PREFIX$appWidgetId", now.monthValue)
        return runCatching { YearMonth.of(year, month) }.getOrDefault(now)
    }

    fun setDisplayedYearMonth(context: Context, appWidgetId: Int, yearMonth: YearMonth) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("$KEY_YEAR_PREFIX$appWidgetId", yearMonth.year)
            .putInt("$KEY_MONTH_PREFIX$appWidgetId", yearMonth.monthValue)
            .apply()
    }

    fun resetToCurrentMonth(context: Context, appWidgetId: Int): YearMonth {
        val now = YearMonth.now()
        setDisplayedYearMonth(context, appWidgetId, now)
        return now
    }

    fun clearWidget(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("$KEY_YEAR_PREFIX$appWidgetId")
            .remove("$KEY_MONTH_PREFIX$appWidgetId")
            .apply()
    }

    fun clearWidgets(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in appWidgetIds) {
            editor.remove("$KEY_YEAR_PREFIX$id")
            editor.remove("$KEY_MONTH_PREFIX$id")
        }
        editor.apply()
    }
}
