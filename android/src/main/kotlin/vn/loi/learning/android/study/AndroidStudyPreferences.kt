package vn.loi.learning.android.study

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.loi.learning.application.study.DailyStudyBudgetLimits

interface AndroidStudyPreferenceStore {
    fun load(): DailyStudyBudgetLimits
    fun save(limits: DailyStudyBudgetLimits)
}

class AndroidStudyPreferencesController(private val store: AndroidStudyPreferenceStore) {
    private val mutableLimits = MutableStateFlow(store.load())
    val limits: StateFlow<DailyStudyBudgetLimits> = mutableLimits.asStateFlow()
    fun current(): DailyStudyBudgetLimits = mutableLimits.value
    fun updateNew(value: Int): Boolean = update(value, mutableLimits.value.reviewPerDay)
    fun updateReview(value: Int): Boolean = update(mutableLimits.value.newPerDay, value)
    private fun update(newLimit: Int, reviewLimit: Int): Boolean = runCatching {
        DailyStudyBudgetLimits(newLimit, reviewLimit)
    }.fold(
        onSuccess = { updated -> store.save(updated); mutableLimits.value = updated; true },
        onFailure = { false }
    )
}

class SharedPreferencesStudyPreferenceStore(context: Context) : AndroidStudyPreferenceStore {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    override fun load() = DailyStudyBudgetLimits(
        preferences.getInt(KEY_NEW, 20), preferences.getInt(KEY_REVIEW, 100)
    )
    override fun save(limits: DailyStudyBudgetLimits) {
        preferences.edit().putInt(KEY_NEW, limits.newPerDay).putInt(KEY_REVIEW, limits.reviewPerDay).apply()
    }
    private companion object {
        const val FILE_NAME = "learning-engine-study"
        const val KEY_NEW = "daily.new"
        const val KEY_REVIEW = "daily.review"
    }
}
