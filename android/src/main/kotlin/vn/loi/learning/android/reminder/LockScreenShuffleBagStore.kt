package vn.loi.learning.android.reminder

import android.content.Context
import android.content.SharedPreferences

data class LockScreenShuffleBagState(
    val cycleVersion: Int = 1,
    val orderedCandidateIds: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val lastPresentedCandidateId: String? = null
)

interface LockScreenShuffleBagStore {
    fun load(contextKey: String): LockScreenShuffleBagState?
    fun save(contextKey: String, state: LockScreenShuffleBagState): Boolean
    fun clear(contextKey: String): Boolean
}

class SharedPreferencesLockScreenShuffleBagStore(
    context: Context,
    prefsName: String = "learning_engine_lockscreen_bag_prefs"
) : LockScreenShuffleBagStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun load(contextKey: String): LockScreenShuffleBagState? {
        val keyPrefix = "bag.$contextKey"
        if (!prefs.contains("$keyPrefix.version")) {
            return null
        }
        val version = prefs.getInt("$keyPrefix.version", 1)
        val idsString = prefs.getString("$keyPrefix.ids", "") ?: ""
        val ids = if (idsString.isNotBlank()) idsString.split(",") else emptyList()
        val index = prefs.getInt("$keyPrefix.index", 0)
        val lastId = prefs.getString("$keyPrefix.last_id", null)?.takeIf { it.isNotBlank() }

        return LockScreenShuffleBagState(
            cycleVersion = version,
            orderedCandidateIds = ids,
            currentIndex = index,
            lastPresentedCandidateId = lastId
        )
    }

    override fun save(contextKey: String, state: LockScreenShuffleBagState): Boolean {
        val keyPrefix = "bag.$contextKey"
        return prefs.edit().apply {
            putInt("$keyPrefix.version", state.cycleVersion)
            putString("$keyPrefix.ids", state.orderedCandidateIds.joinToString(","))
            putInt("$keyPrefix.index", state.currentIndex)
            if (state.lastPresentedCandidateId != null) {
                putString("$keyPrefix.last_id", state.lastPresentedCandidateId)
            } else {
                remove("$keyPrefix.last_id")
            }
        }.commit()
    }

    override fun clear(contextKey: String): Boolean {
        val keyPrefix = "bag.$contextKey"
        return prefs.edit().apply {
            remove("$keyPrefix.version")
            remove("$keyPrefix.ids")
            remove("$keyPrefix.index")
            remove("$keyPrefix.last_id")
        }.commit()
    }
}
