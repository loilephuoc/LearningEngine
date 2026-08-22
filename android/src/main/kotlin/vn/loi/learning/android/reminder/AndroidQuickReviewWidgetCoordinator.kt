package vn.loi.learning.android.reminder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R
import vn.loi.learning.domain.study.memory.model.ReviewRating

data class QuickReviewWidgetState(val packageId: String? = null, val contentId: String? = null, val index: Int = 0, val revealed: Boolean = false)

internal fun advanceQuickReviewWidget(state: QuickReviewWidgetState, size: Int): QuickReviewWidgetState =
    if (size <= 0) state.copy(contentId = null, index = 0, revealed = false)
    else state.copy(contentId = null, index = (state.index + 1) % size, revealed = false)

class AndroidQuickReviewWidgetCoordinator(
    private val context: Context,
    private val selector: AndroidVocabularyReminderCandidateSelector,
    private val ratingBridge: AndroidReminderReviewRatingBridge
) {
    private val prefs = context.getSharedPreferences("quick_review_widget_instances", Context.MODE_PRIVATE)
    private val manager get() = AppWidgetManager.getInstance(context)

    fun update(ids: IntArray = manager.getAppWidgetIds(ComponentName(context, AndroidQuickReviewWidgetProvider::class.java))) =
        ids.forEach(::render)

    fun delete(ids: IntArray) {
        prefs.edit().apply { ids.forEach { remove(key(it)) } }.apply()
    }

    fun reveal(id: Int, expectedContentId: String?) {
        val state = resolve(id) ?: return render(id)
        if (state.contentId != expectedContentId) return render(id)
        save(id, state.copy(revealed = true)); render(id)
    }

    fun next(id: Int, expectedContentId: String?) {
        val resolved = resolveQueue(id) ?: return render(id)
        if (resolved.first.contentId != expectedContentId) return render(id)
        save(id, advanceQuickReviewWidget(resolved.first, resolved.second.items.size)); render(id)
    }

    fun rate(id: Int, expectedContentId: String?, rating: ReviewRating) {
        val contentId = expectedContentId ?: return render(id)
        val resolved = resolveQueue(id) ?: return render(id)
        val state = resolved.first
        if (!state.revealed || state.contentId != contentId) return render(id)
        if (ratingBridge.submitRating(contentId, rating) is QuickReviewRatingResult.Success) {
            save(id, advanceQuickReviewWidget(state, resolved.second.items.size))
        }
        render(id)
    }

    private fun activePackageId(): String? {
        val app = context.applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication ?: return null
        val engine = app.graph.engine
        val libraryId = engine.defaultLibraryId ?: return null
        return engine.domainLibraryRepository?.findById(libraryId)?.activePackageId?.value
    }

    private fun resolveQueue(id: Int): Pair<QuickReviewWidgetState, AndroidReminderReviewSession>? {
        val packageId = activePackageId() ?: return null
        val queue = selector.getReviewQueue(packageId, AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED.name, null) ?: return null
        val old = load(id).takeIf { it.packageId == packageId } ?: QuickReviewWidgetState(packageId = packageId)
        val index = old.index.mod(queue.items.size)
        val candidate = queue.items[index]
        val resolved = old.copy(packageId = packageId, contentId = candidate.contentId.value, index = index)
        return resolved to queue
    }

    private fun resolve(id: Int): QuickReviewWidgetState? = resolveQueue(id)?.first

    private fun render(id: Int) {
        val views = RemoteViews(context.packageName, R.layout.quick_review_widget)
        val resolved = runCatching { resolveQueue(id) }.getOrNull()
        if (resolved == null) {
            views.setTextViewText(R.id.quick_review_word, "Chưa có từ để ôn")
            views.setTextViewText(R.id.quick_review_meaning, "Hãy chọn gói học chính trong Learning Engine.")
            views.setViewVisibility(R.id.quick_review_reveal, View.GONE)
            views.setViewVisibility(R.id.quick_review_next, View.GONE)
            val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.quick_review_root, open)
            manager.updateAppWidget(id, views)
            return
        }
        val (state, queue) = resolved
        val candidate = queue.items[state.index]
        save(id, state)
        views.setTextViewText(R.id.quick_review_package, candidate.packageName)
        views.setTextViewText(R.id.quick_review_word, candidate.primaryText)
        views.setTextViewText(R.id.quick_review_pronunciation, listOfNotNull(candidate.ipa?.let { "/$it/" }, candidate.partOfSpeech).joinToString(" · "))
        val meaning = candidate.translation ?: candidate.answer ?: "Chưa có nghĩa"
        views.setTextViewText(R.id.quick_review_meaning, if (state.revealed) meaning else "Chạm Hiện đáp án để xem nghĩa")
        views.setTextViewText(R.id.quick_review_example, candidate.example.orEmpty())
        views.setViewVisibility(R.id.quick_review_example, if (state.revealed && !candidate.example.isNullOrBlank()) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.quick_review_reveal, if (state.revealed) View.GONE else View.VISIBLE)
        listOf(R.id.quick_review_again, R.id.quick_review_hard, R.id.quick_review_good, R.id.quick_review_easy).forEach {
            views.setViewVisibility(it, if (state.revealed) View.VISIBLE else View.GONE)
        }
        bind(views, R.id.quick_review_reveal, AndroidQuickReviewWidgetProvider.ACTION_REVEAL, id, state.contentId)
        bind(views, R.id.quick_review_next, AndroidQuickReviewWidgetProvider.ACTION_NEXT, id, state.contentId)
        bind(views, R.id.quick_review_again, AndroidQuickReviewWidgetProvider.ACTION_AGAIN, id, state.contentId)
        bind(views, R.id.quick_review_hard, AndroidQuickReviewWidgetProvider.ACTION_HARD, id, state.contentId)
        bind(views, R.id.quick_review_good, AndroidQuickReviewWidgetProvider.ACTION_GOOD, id, state.contentId)
        bind(views, R.id.quick_review_easy, AndroidQuickReviewWidgetProvider.ACTION_EASY, id, state.contentId)
        manager.updateAppWidget(id, views)
    }

    private fun bind(views: RemoteViews, viewId: Int, action: String, widgetId: Int, contentId: String?) {
        val intent = Intent(context, AndroidQuickReviewWidgetProvider::class.java).apply {
            this.action = action
            putExtra(AndroidQuickReviewWidgetProvider.EXTRA_WIDGET_ID, widgetId)
            putExtra(AndroidQuickReviewWidgetProvider.EXTRA_CONTENT_ID, contentId)
        }
        views.setOnClickPendingIntent(viewId, PendingIntent.getBroadcast(context, widgetId * 10 + viewId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    }

    private fun key(id: Int) = "widget.$id"
    private fun load(id: Int): QuickReviewWidgetState {
        val raw = prefs.getString(key(id), null) ?: return QuickReviewWidgetState()
        val parts = raw.split('|')
        return QuickReviewWidgetState(parts.getOrNull(0)?.ifBlank { null }, parts.getOrNull(1)?.ifBlank { null }, parts.getOrNull(2)?.toIntOrNull() ?: 0, parts.getOrNull(3) == "1")
    }
    private fun save(id: Int, state: QuickReviewWidgetState) {
        prefs.edit().putString(key(id), "${state.packageId.orEmpty()}|${state.contentId.orEmpty()}|${state.index}|${if (state.revealed) 1 else 0}").apply()
    }
}
