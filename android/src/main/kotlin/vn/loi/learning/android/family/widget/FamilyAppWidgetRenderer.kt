package vn.loi.learning.android.family.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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

object FamilyAppWidgetRenderer {

    fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        snapshot: FamilyLocalSnapshot,
        now: LocalDateTime = LocalDateTime.now()
    ): RemoteViews {
        return runCatching {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)

            val calendar = AstronomicalVietnameseLunarCalendar()
            val projectionService = CalendarProjectionService(calendar)
            val state = FamilyWidgetModelBuilder.build(snapshot, calendar, projectionService, now)

            when {
                minWidth < 180 || minHeight < 110 -> renderSmall(context, state, now.toLocalDate())
                minWidth < 260 || minHeight < 170 -> renderMedium(context, state, now.toLocalDate())
                else -> renderLarge(context, state, now.toLocalDate())
            }
        }.getOrElse { e ->
            android.util.Log.e("FamilyAppWidget", "Error rendering family widget $appWidgetId", e)
            renderFallback(context, now.toLocalDate())
        }
    }

    private fun renderFallback(context: Context, today: LocalDate): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.family_widget_medium)
        views.setTextViewText(R.id.widget_solar_weekday, "Ngày đáng nhớ")
        views.setTextViewText(R.id.widget_solar_date, "")
        views.setTextViewText(R.id.widget_lunar_date, "")
        views.setViewVisibility(R.id.widget_item_1, View.GONE)
        views.setViewVisibility(R.id.widget_item_2, View.GONE)
        views.setViewVisibility(R.id.widget_item_3, View.GONE)
        views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
        views.setTextViewText(R.id.widget_empty_message, "Chạm để mở ứng dụng")

        val intent = createOpenFamilyIntent(context, today, 999)
        views.setOnClickPendingIntent(R.id.widget_root, intent)
        views.setOnClickPendingIntent(R.id.widget_empty_message, intent)
        return views
    }

    private fun createOpenFamilyIntent(context: Context, date: LocalDate, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = FamilyNotificationPublisher.ACTION_OPEN_FAMILY
            putExtra(AndroidFamilyReminderScheduler.EXTRA_OCCURRENCE_DATE, date.toString())
            data = Uri.parse("learning://family/calendar?date=$date&req=$requestCode")
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
        quickAddType: String? = "CHOICE"
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = FamilyNotificationPublisher.ACTION_OPEN_FAMILY_QUICK_ADD
            putExtra(AndroidFamilyReminderScheduler.EXTRA_OCCURRENCE_DATE, date.toString())
            if (quickAddType != null) {
                putExtra(FamilyNotificationPublisher.EXTRA_QUICK_ADD_TYPE, quickAddType)
            }
            data = Uri.parse("learning://family/quick_add?type=$quickAddType&req=$requestCode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun renderSmall(context: Context, state: FamilyWidgetState, today: LocalDate): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.family_widget_small)

        views.setTextViewText(R.id.widget_solar_weekday, state.todaySolarWeekday)
        views.setTextViewText(R.id.widget_solar_date, state.todaySolarFull)
        views.setTextViewText(R.id.widget_lunar_date, state.todayLunarFormatted)

        val headerPendingIntent = createOpenFamilyIntent(context, today, 100)
        views.setOnClickPendingIntent(R.id.widget_root, headerPendingIntent)

        val topItem = state.allItemsOrdered.firstOrNull()
        if (topItem != null) {
            views.setViewVisibility(R.id.widget_item_1, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty_message, View.GONE)

            views.setTextViewText(R.id.widget_item_1_icon, topItem.iconText)
            views.setTextViewText(R.id.widget_item_1_title, topItem.title)
            views.setTextViewText(R.id.widget_item_1_badge, topItem.badgeText)

            val badgeColor = when (topItem.semantic) {
                FamilyWidgetItemSemantic.TASK_OVERDUE -> Color.parseColor("#D32F2F")
                FamilyWidgetItemSemantic.BIRTHDAY -> Color.parseColor("#1976D2")
                FamilyWidgetItemSemantic.EVENT -> Color.parseColor("#D84315")
                FamilyWidgetItemSemantic.TASK_DUE_TODAY -> Color.parseColor("#1976D2")
                FamilyWidgetItemSemantic.TASK_UPCOMING -> Color.parseColor("#5F6368")
            }
            views.setTextColor(R.id.widget_item_1_badge, badgeColor)

            val itemPendingIntent = createOpenFamilyIntent(context, topItem.date, 101)
            views.setOnClickPendingIntent(R.id.widget_item_1, itemPendingIntent)
        } else {
            views.setViewVisibility(R.id.widget_item_1, View.GONE)
            views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
        }

        return views
    }

    private fun renderMedium(context: Context, state: FamilyWidgetState, today: LocalDate): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.family_widget_medium)

        views.setTextViewText(R.id.widget_solar_weekday, state.todaySolarWeekday)
        views.setTextViewText(R.id.widget_solar_date, state.todaySolarFull)
        views.setTextViewText(R.id.widget_lunar_date, state.todayLunarFormatted)

        val calendarPendingIntent = createOpenFamilyIntent(context, today, 200)
        val quickAddPendingIntent = createQuickAddFamilyIntent(context, today, 250)
        views.setOnClickPendingIntent(R.id.widget_header_container, calendarPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_btn_view_calendar, calendarPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_btn_quick_add, quickAddPendingIntent)

        val items = state.allItemsOrdered.take(3)
        if (items.isEmpty()) {
            views.setViewVisibility(R.id.widget_item_1, View.GONE)
            views.setViewVisibility(R.id.widget_item_2, View.GONE)
            views.setViewVisibility(R.id.widget_item_3, View.GONE)
            views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_empty_message, View.GONE)

            val itemViews = listOf(R.id.widget_item_1 to Pair(R.id.widget_item_1_icon, Pair(R.id.widget_item_1_title, R.id.widget_item_1_badge)),
                R.id.widget_item_2 to Pair(R.id.widget_item_2_icon, Pair(R.id.widget_item_2_title, R.id.widget_item_2_badge)),
                R.id.widget_item_3 to Pair(R.id.widget_item_3_icon, Pair(R.id.widget_item_3_title, R.id.widget_item_3_badge)))

            for (i in 0..2) {
                val (layoutId, fields) = itemViews[i]
                val (iconId, textIds) = fields
                val (titleId, badgeId) = textIds

                if (i < items.size) {
                    val item = items[i]
                    views.setViewVisibility(layoutId, View.VISIBLE)
                    views.setTextViewText(iconId, item.iconText)
                    views.setTextViewText(titleId, item.title)
                    views.setTextViewText(badgeId, item.badgeText)

                    val badgeColor = when (item.semantic) {
                        FamilyWidgetItemSemantic.TASK_OVERDUE -> Color.parseColor("#D32F2F")
                        FamilyWidgetItemSemantic.BIRTHDAY -> Color.parseColor("#1976D2")
                        FamilyWidgetItemSemantic.EVENT -> Color.parseColor("#D84315")
                        FamilyWidgetItemSemantic.TASK_DUE_TODAY -> Color.parseColor("#1976D2")
                        FamilyWidgetItemSemantic.TASK_UPCOMING -> Color.parseColor("#5F6368")
                    }
                    views.setTextColor(badgeId, badgeColor)

                    val itemPendingIntent = createOpenFamilyIntent(context, item.date, 201 + i)
                    views.setOnClickPendingIntent(layoutId, itemPendingIntent)
                } else {
                    views.setViewVisibility(layoutId, View.GONE)
                }
            }
        }

        return views
    }

    private fun renderLarge(context: Context, state: FamilyWidgetState, today: LocalDate): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.family_widget_large)

        views.setTextViewText(R.id.widget_solar_weekday, state.todaySolarWeekday)
        views.setTextViewText(R.id.widget_solar_date, state.todaySolarFull)
        views.setTextViewText(R.id.widget_lunar_date, state.todayLunarFormatted)
        views.setTextViewText(R.id.widget_summary_text, state.todaySummary)

        val calendarPendingIntent = createOpenFamilyIntent(context, today, 300)
        val quickAddPendingIntent = createQuickAddFamilyIntent(context, today, 350)
        views.setOnClickPendingIntent(R.id.widget_header_container, calendarPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_btn_view_calendar, calendarPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_btn_quick_add, quickAddPendingIntent)

        val todayItems = state.todayItems.take(3)
        val upcomingItems = state.upcomingItems.take(3)

        if (todayItems.isEmpty() && upcomingItems.isEmpty()) {
            views.setViewVisibility(R.id.widget_section_today_header, View.GONE)
            views.setViewVisibility(R.id.widget_item_1, View.GONE)
            views.setViewVisibility(R.id.widget_item_2, View.GONE)
            views.setViewVisibility(R.id.widget_item_3, View.GONE)
            views.setViewVisibility(R.id.widget_section_upcoming_header, View.GONE)
            views.setViewVisibility(R.id.widget_item_4, View.GONE)
            views.setViewVisibility(R.id.widget_item_5, View.GONE)
            views.setViewVisibility(R.id.widget_item_6, View.GONE)
            views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_empty_message, View.GONE)

            // Section 1: Today
            if (todayItems.isNotEmpty()) {
                views.setViewVisibility(R.id.widget_section_today_header, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_section_today_header, View.GONE)
            }

            val todayItemViews = listOf(
                R.id.widget_item_1 to Pair(R.id.widget_item_1_icon, Pair(R.id.widget_item_1_title, R.id.widget_item_1_badge)),
                R.id.widget_item_2 to Pair(R.id.widget_item_2_icon, Pair(R.id.widget_item_2_title, R.id.widget_item_2_badge)),
                R.id.widget_item_3 to Pair(R.id.widget_item_3_icon, Pair(R.id.widget_item_3_title, R.id.widget_item_3_badge))
            )

            for (i in 0..2) {
                val (layoutId, fields) = todayItemViews[i]
                val (iconId, textIds) = fields
                val (titleId, badgeId) = textIds

                if (i < todayItems.size) {
                    val item = todayItems[i]
                    views.setViewVisibility(layoutId, View.VISIBLE)
                    views.setTextViewText(iconId, item.iconText)
                    views.setTextViewText(titleId, item.title)
                    views.setTextViewText(badgeId, item.badgeText)

                    val badgeColor = when (item.semantic) {
                        FamilyWidgetItemSemantic.TASK_OVERDUE -> Color.parseColor("#D32F2F")
                        FamilyWidgetItemSemantic.BIRTHDAY -> Color.parseColor("#1976D2")
                        FamilyWidgetItemSemantic.EVENT -> Color.parseColor("#D84315")
                        FamilyWidgetItemSemantic.TASK_DUE_TODAY -> Color.parseColor("#1976D2")
                        FamilyWidgetItemSemantic.TASK_UPCOMING -> Color.parseColor("#5F6368")
                    }
                    views.setTextColor(badgeId, badgeColor)

                    val itemPendingIntent = createOpenFamilyIntent(context, item.date, 301 + i)
                    views.setOnClickPendingIntent(layoutId, itemPendingIntent)
                } else {
                    views.setViewVisibility(layoutId, View.GONE)
                }
            }

            // Section 2: Upcoming
            if (upcomingItems.isNotEmpty()) {
                views.setViewVisibility(R.id.widget_section_upcoming_header, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_section_upcoming_header, View.GONE)
            }

            val upcomingItemViews = listOf(
                R.id.widget_item_4 to Pair(R.id.widget_item_4_icon, Pair(R.id.widget_item_4_title, R.id.widget_item_4_badge)),
                R.id.widget_item_5 to Pair(R.id.widget_item_5_icon, Pair(R.id.widget_item_5_title, R.id.widget_item_5_badge)),
                R.id.widget_item_6 to Pair(R.id.widget_item_6_icon, Pair(R.id.widget_item_6_title, R.id.widget_item_6_badge))
            )

            for (i in 0..2) {
                val (layoutId, fields) = upcomingItemViews[i]
                val (iconId, textIds) = fields
                val (titleId, badgeId) = textIds

                if (i < upcomingItems.size) {
                    val item = upcomingItems[i]
                    views.setViewVisibility(layoutId, View.VISIBLE)
                    views.setTextViewText(iconId, item.iconText)
                    views.setTextViewText(titleId, item.title)
                    views.setTextViewText(badgeId, item.badgeText)

                    val badgeColor = when (item.semantic) {
                        FamilyWidgetItemSemantic.TASK_OVERDUE -> Color.parseColor("#D32F2F")
                        FamilyWidgetItemSemantic.BIRTHDAY -> Color.parseColor("#1976D2")
                        FamilyWidgetItemSemantic.EVENT -> Color.parseColor("#D84315")
                        FamilyWidgetItemSemantic.TASK_DUE_TODAY -> Color.parseColor("#1976D2")
                        FamilyWidgetItemSemantic.TASK_UPCOMING -> Color.parseColor("#5F6368")
                    }
                    views.setTextColor(badgeId, badgeColor)

                    val itemPendingIntent = createOpenFamilyIntent(context, item.date, 311 + i)
                    views.setOnClickPendingIntent(layoutId, itemPendingIntent)
                } else {
                    views.setViewVisibility(layoutId, View.GONE)
                }
            }
        }

        return views
    }
}
