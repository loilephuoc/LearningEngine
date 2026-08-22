package vn.loi.learning.android.reminder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import vn.loi.learning.android.LearningEngineAndroidApplication
import vn.loi.learning.domain.study.memory.model.ReviewRating

class AndroidQuickReviewWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        coordinator(context)?.update(ids)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        coordinator(context)?.delete(ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val expected = intent.getStringExtra(EXTRA_CONTENT_ID)
        when (intent.action) {
            ACTION_REVEAL -> coordinator(context)?.reveal(id, expected)
            ACTION_NEXT -> coordinator(context)?.next(id, expected)
            ACTION_AGAIN -> coordinator(context)?.rate(id, expected, ReviewRating.AGAIN)
            ACTION_HARD -> coordinator(context)?.rate(id, expected, ReviewRating.HARD)
            ACTION_GOOD -> coordinator(context)?.rate(id, expected, ReviewRating.GOOD)
            ACTION_EASY -> coordinator(context)?.rate(id, expected, ReviewRating.EASY)
        }
    }

    private fun coordinator(context: Context) =
        (context.applicationContext as? LearningEngineAndroidApplication)?.quickReviewWidgetCoordinator

    companion object {
        const val ACTION_REVEAL = "vn.loi.learning.android.action.QUICK_WIDGET_REVEAL"
        const val ACTION_NEXT = "vn.loi.learning.android.action.QUICK_WIDGET_NEXT"
        const val ACTION_AGAIN = "vn.loi.learning.android.action.QUICK_WIDGET_AGAIN"
        const val ACTION_HARD = "vn.loi.learning.android.action.QUICK_WIDGET_HARD"
        const val ACTION_GOOD = "vn.loi.learning.android.action.QUICK_WIDGET_GOOD"
        const val ACTION_EASY = "vn.loi.learning.android.action.QUICK_WIDGET_EASY"
        const val EXTRA_WIDGET_ID = "quick_review_widget_id"
        const val EXTRA_CONTENT_ID = "quick_review_content_id"
    }
}
