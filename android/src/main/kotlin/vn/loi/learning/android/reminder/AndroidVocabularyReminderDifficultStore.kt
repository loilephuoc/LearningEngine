package vn.loi.learning.android.reminder

import android.content.Context
import android.content.SharedPreferences
import vn.loi.learning.domain.content.model.ContentId

fun interface AndroidVocabularyReminderMarkedReadSource {
    fun isMarked(contentId: ContentId): Boolean
}

interface AndroidVocabularyReminderDifficultMarkers : AndroidVocabularyReminderMarkedReadSource {
    fun markedContentIds(): Set<ContentId>
    fun toggle(contentId: ContentId): Boolean
    fun setMarked(contentId: ContentId, marked: Boolean): Boolean
}

class SharedPreferencesVocabularyReminderDifficultStore(
    context: Context,
    private val preferencesName: String = PREFS_NAME
) : AndroidVocabularyReminderDifficultMarkers {
    private val prefs: SharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val lock = Any()
    private var cache: MutableSet<String>? = null

    override fun isMarked(contentId: ContentId): Boolean = synchronized(lock) {
        ensureLoaded().contains(contentId.value)
    }

    override fun markedContentIds(): Set<ContentId> = synchronized(lock) {
        ensureLoaded().map(::ContentId).toSet()
    }

    override fun toggle(contentId: ContentId): Boolean = synchronized(lock) {
        val set = ensureLoaded()
        val isNowMarked = if (set.contains(contentId.value)) {
            set.remove(contentId.value)
            false
        } else {
            set.add(contentId.value)
            true
        }
        persist(set)
        isNowMarked
    }

    override fun setMarked(contentId: ContentId, marked: Boolean): Boolean = synchronized(lock) {
        val set = ensureLoaded()
        val changed = if (marked) set.add(contentId.value) else set.remove(contentId.value)
        if (changed) persist(set)
        marked
    }

    private fun ensureLoaded(): MutableSet<String> {
        cache?.let { return it }
        val loaded = prefs.getStringSet(KEY_DIFFICULT_IDS, emptySet()) ?: emptySet()
        val mutable = HashSet(loaded)
        cache = mutable
        return mutable
    }

    private fun persist(set: Set<String>) {
        prefs.edit().putStringSet(KEY_DIFFICULT_IDS, HashSet(set)).apply()
    }

    companion object {
        const val PREFS_NAME = "learning_engine_reminder_difficult_prefs"
        private const val KEY_DIFFICULT_IDS = "difficult.content.ids"
    }
}
