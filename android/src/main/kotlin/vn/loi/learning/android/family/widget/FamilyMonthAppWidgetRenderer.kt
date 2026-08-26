package vn.loi.learning.android.family.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalDateTime
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R
import vn.loi.learning.android.family.AndroidFamilyReminderScheduler
import vn.loi.learning.android.family.AstronomicalVietnameseLunarCalendar
import vn.loi.learning.android.family.CalendarProjectionService
import vn.loi.learning.android.family.FamilyLocalSnapshot
import vn.loi.learning.android.family.FamilyNotificationPublisher
import vn.loi.learning.android.ui.FamilyWidgetColors

object FamilyMonthAppWidgetRenderer {

    private val CELL_IDS = intArrayOf(
        R.id.cell_0, R.id.cell_1, R.id.cell_2, R.id.cell_3, R.id.cell_4, R.id.cell_5, R.id.cell_6,
        R.id.cell_7, R.id.cell_8, R.id.cell_9, R.id.cell_10, R.id.cell_11, R.id.cell_12, R.id.cell_13,
        R.id.cell_14, R.id.cell_15, R.id.cell_16, R.id.cell_17, R.id.cell_18, R.id.cell_19, R.id.cell_20,
        R.id.cell_21, R.id.cell_22, R.id.cell_23, R.id.cell_24, R.id.cell_25, R.id.cell_26, R.id.cell_27,
        R.id.cell_28, R.id.cell_29, R.id.cell_30, R.id.cell_31, R.id.cell_32, R.id.cell_33, R.id.cell_34,
        R.id.cell_35, R.id.cell_36, R.id.cell_37, R.id.cell_38, R.id.cell_39, R.id.cell_40, R.id.cell_41
    )

    private val SOLAR_IDS = intArrayOf(
        R.id.solar_0, R.id.solar_1, R.id.solar_2, R.id.solar_3, R.id.solar_4, R.id.solar_5, R.id.solar_6,
        R.id.solar_7, R.id.solar_8, R.id.solar_9, R.id.solar_10, R.id.solar_11, R.id.solar_12, R.id.solar_13,
        R.id.solar_14, R.id.solar_15, R.id.solar_16, R.id.solar_17, R.id.solar_18, R.id.solar_19, R.id.solar_20,
        R.id.solar_21, R.id.solar_22, R.id.solar_23, R.id.solar_24, R.id.solar_25, R.id.solar_26, R.id.solar_27,
        R.id.solar_28, R.id.solar_29, R.id.solar_30, R.id.solar_31, R.id.solar_32, R.id.solar_33, R.id.solar_34,
        R.id.solar_35, R.id.solar_36, R.id.solar_37, R.id.solar_38, R.id.solar_39, R.id.solar_40, R.id.solar_41
    )

    private val LUNAR_IDS = intArrayOf(
        R.id.lunar_0, R.id.lunar_1, R.id.lunar_2, R.id.lunar_3, R.id.lunar_4, R.id.lunar_5, R.id.lunar_6,
        R.id.lunar_7, R.id.lunar_8, R.id.lunar_9, R.id.lunar_10, R.id.lunar_11, R.id.lunar_12, R.id.lunar_13,
        R.id.lunar_14, R.id.lunar_15, R.id.lunar_16, R.id.lunar_17, R.id.lunar_18, R.id.lunar_19, R.id.lunar_20,
        R.id.lunar_21, R.id.lunar_22, R.id.lunar_23, R.id.lunar_24, R.id.lunar_25, R.id.lunar_26, R.id.lunar_27,
        R.id.lunar_28, R.id.lunar_29, R.id.lunar_30, R.id.lunar_31, R.id.lunar_32, R.id.lunar_33, R.id.lunar_34,
        R.id.lunar_35, R.id.lunar_36, R.id.lunar_37, R.id.lunar_38, R.id.lunar_39, R.id.lunar_40, R.id.lunar_41
    )

    private val MARKER_IDS = intArrayOf(
        R.id.marker_0, R.id.marker_1, R.id.marker_2, R.id.marker_3, R.id.marker_4, R.id.marker_5, R.id.marker_6,
        R.id.marker_7, R.id.marker_8, R.id.marker_9, R.id.marker_10, R.id.marker_11, R.id.marker_12, R.id.marker_13,
        R.id.marker_14, R.id.marker_15, R.id.marker_16, R.id.marker_17, R.id.marker_18, R.id.marker_19, R.id.marker_20,
        R.id.marker_21, R.id.marker_22, R.id.marker_23, R.id.marker_24, R.id.marker_25, R.id.marker_26, R.id.marker_27,
        R.id.marker_28, R.id.marker_29, R.id.marker_30, R.id.marker_31, R.id.marker_32, R.id.marker_33, R.id.marker_34,
        R.id.marker_35, R.id.marker_36, R.id.marker_37, R.id.marker_38, R.id.marker_39, R.id.marker_40, R.id.marker_41
    )

    fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        snapshot: FamilyLocalSnapshot,
        now: LocalDateTime = LocalDateTime.now()
    ): RemoteViews {
        return runCatching {
            val displayedYearMonth = FamilyMonthWidgetPreferences.getDisplayedYearMonth(context, appWidgetId)
            val calendar = AstronomicalVietnameseLunarCalendar()
            val projectionService = CalendarProjectionService(calendar)
            val state = FamilyMonthWidgetModelBuilder.build(
                snapshot = snapshot,
                calendar = calendar,
                projectionService = projectionService,
                now = now,
                targetYearMonth = displayedYearMonth
            )

            val views = RemoteViews(context.packageName, R.layout.family_month_widget)
            views.setTextViewText(R.id.widget_month_title, state.monthHeaderTitle)

            // Month Navigation: Prev / Next / Today
            val prevIntent = Intent(context, FamilyMonthAppWidgetProvider::class.java).apply {
                action = FamilyMonthAppWidgetProvider.ACTION_PREV_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("learning://widget/month/prev/$appWidgetId")
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                7100 + (appWidgetId % 1000),
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev_month, prevPendingIntent)

            val nextIntent = Intent(context, FamilyMonthAppWidgetProvider::class.java).apply {
                action = FamilyMonthAppWidgetProvider.ACTION_NEXT_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("learning://widget/month/next/$appWidgetId")
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                7200 + (appWidgetId % 1000),
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next_month, nextPendingIntent)

            val todayIntent = Intent(context, FamilyMonthAppWidgetProvider::class.java).apply {
                action = FamilyMonthAppWidgetProvider.ACTION_TODAY_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("learning://widget/month/today/$appWidgetId")
            }
            val todayPendingIntent = PendingIntent.getBroadcast(
                context,
                7300 + (appWidgetId % 1000),
                todayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_today, todayPendingIntent)

            // Header title opens Family Calendar at first of displayed month
            val headerTitlePendingIntent = createOpenFamilyIntent(
                context,
                displayedYearMonth.atDay(1),
                5000 + (appWidgetId % 1000),
                appWidgetId
            )
            views.setOnClickPendingIntent(R.id.widget_month_title, headerTitlePendingIntent)

            // Quick Add '+' Button
            val quickAddPendingIntent = createQuickAddFamilyIntent(
                context,
                state.today,
                8800 + (appWidgetId % 1000),
                "CHOICE",
                appWidgetId
            )
            views.setOnClickPendingIntent(R.id.widget_btn_quick_add, quickAddPendingIntent)

            // Bind 42 Cells
            for (i in 0 until 42) {
                val cell = state.cells.getOrNull(i) ?: continue
                val cellId = CELL_IDS[i]
                val solarId = SOLAR_IDS[i]
                val lunarId = LUNAR_IDS[i]
                val markerId = MARKER_IDS[i]

                views.setTextViewText(solarId, cell.solarDayText)
                views.setTextViewText(lunarId, cell.lunarDayText)

                if (cell.isCurrentMonth) {
                    val isSunday = (i % 7 == 6)
                    val solarTint = if (isSunday) FamilyWidgetColors.solarSunday.toArgb() else FamilyWidgetColors.solarNormal.toArgb()
                    views.setInt(solarId, "setTextColor", solarTint)
                    views.setInt(lunarId, "setTextColor", FamilyWidgetColors.lunarCurrentMonth.toArgb())
                } else {
                    views.setInt(solarId, "setTextColor", FamilyWidgetColors.solarOtherMonth.toArgb())
                    views.setInt(lunarId, "setTextColor", FamilyWidgetColors.lunarOtherMonth.toArgb())
                }

                when {
                    cell.isToday -> {
                        views.setInt(cellId, "setBackgroundResource", R.drawable.bg_family_month_day_today)
                    }
                    cell.isOccupied && cell.isCurrentMonth -> {
                        views.setInt(cellId, "setBackgroundResource", R.drawable.bg_family_month_day_occupied)
                    }
                    else -> {
                        views.setInt(cellId, "setBackgroundResource", 0)
                    }
                }

                if (cell.isOccupied && cell.isCurrentMonth && cell.markerText.isNotEmpty()) {
                    views.setViewVisibility(markerId, View.VISIBLE)
                    views.setTextViewText(markerId, cell.markerText)
                    val markerTint = when {
                        cell.hasBirthday -> FamilyWidgetColors.birthday.toArgb()
                        cell.hasEvent -> FamilyWidgetColors.event.toArgb()
                        else -> FamilyWidgetColors.taskOverdue.toArgb()
                    }
                    views.setInt(markerId, "setTextColor", markerTint)
                } else {
                    views.setViewVisibility(markerId, View.GONE)
                }

                val cellPendingIntent = createOpenFamilyIntent(
                    context,
                    cell.date,
                    6000 + (appWidgetId % 100) * 50 + i,
                    appWidgetId
                )
                views.setOnClickPendingIntent(cellId, cellPendingIntent)
            }

            views
        }.getOrElse { e ->
            android.util.Log.e("FamilyMonthWidget", "Error rendering month widget $appWidgetId", e)
            renderFallback(context, now.toLocalDate())
        }
    }

    private fun renderFallback(context: Context, today: LocalDate): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.family_month_widget)
        views.setTextViewText(R.id.widget_month_title, "Lịch Ngày đáng nhớ")
        val intent = createOpenFamilyIntent(context, today, 998, 0)
        views.setOnClickPendingIntent(R.id.widget_root, intent)
        return views
    }

    private fun createOpenFamilyIntent(
        context: Context,
        date: LocalDate,
        requestCode: Int,
        appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = FamilyNotificationPublisher.ACTION_OPEN_FAMILY
            putExtra(AndroidFamilyReminderScheduler.EXTRA_OCCURRENCE_DATE, date.toString())
            data = Uri.parse("learning://family/calendar?widgetId=$appWidgetId&date=$date&req=$requestCode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createQuickAddFamilyIntent(
        context: Context,
        date: LocalDate,
        requestCode: Int,
        quickAddType: String? = "CHOICE",
        appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = FamilyNotificationPublisher.ACTION_OPEN_FAMILY_QUICK_ADD
            putExtra(AndroidFamilyReminderScheduler.EXTRA_OCCURRENCE_DATE, date.toString())
            if (quickAddType != null) {
                putExtra(FamilyNotificationPublisher.EXTRA_QUICK_ADD_TYPE, quickAddType)
            }
            data = Uri.parse("learning://family/quick_add?widgetId=$appWidgetId&type=$quickAddType&req=$requestCode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
