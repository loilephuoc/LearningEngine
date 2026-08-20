package vn.loi.learning.android.study

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.loi.learning.application.study.DailyStudyBudgetLimits
import vn.loi.learning.android.platform.coordinatedApply

interface AndroidStudyPreferenceStore {
    fun load(): DailyStudyBudgetLimits
    fun save(limits: DailyStudyBudgetLimits)
    fun loadTypingViMuted(): Boolean = false
    fun saveTypingViMuted(muted: Boolean) = Unit
    fun loadContinuousSkim(): Boolean = false
    fun saveContinuousSkim(enabled: Boolean) = Unit
    fun loadInsightsScopePackageId(): String? = null
    fun saveInsightsScopePackageId(packageId: String?) = Unit
}

class AndroidStudyPreferencesController(private val store: AndroidStudyPreferenceStore) {
    private val mutableLimits = MutableStateFlow(store.load())
    val limits: StateFlow<DailyStudyBudgetLimits> = mutableLimits.asStateFlow()
    private val mutableContinuousSkim = MutableStateFlow(store.loadContinuousSkim())
    val continuousSkim: StateFlow<Boolean> = mutableContinuousSkim.asStateFlow()
    fun current(): DailyStudyBudgetLimits = mutableLimits.value
    fun typingViMuted(): Boolean = store.loadTypingViMuted()
    fun updateTypingViMuted(muted: Boolean) = store.saveTypingViMuted(muted)
    fun continuousSkimEnabled(): Boolean = mutableContinuousSkim.value
    fun updateContinuousSkim(enabled: Boolean) {
        store.saveContinuousSkim(enabled)
        mutableContinuousSkim.value = enabled
    }
    fun insightsScopePackageId(): String? = store.loadInsightsScopePackageId()
    fun updateInsightsScopePackageId(packageId: String?) {
        store.saveInsightsScopePackageId(packageId)
    }
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
        preferences.edit().putInt(KEY_NEW, limits.newPerDay).putInt(KEY_REVIEW, limits.reviewPerDay).coordinatedApply()
    }
    override fun loadTypingViMuted() = preferences.getBoolean(KEY_TYPING_VI_MUTED, false)
    override fun saveTypingViMuted(muted: Boolean) {
        preferences.edit().putBoolean(KEY_TYPING_VI_MUTED, muted).coordinatedApply()
    }
    override fun loadContinuousSkim() = preferences.getBoolean(KEY_CONTINUOUS_SKIM, false)
    override fun saveContinuousSkim(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CONTINUOUS_SKIM, enabled).coordinatedApply()
    }
    override fun loadInsightsScopePackageId(): String? = preferences.getString(KEY_INSIGHTS_SCOPE_PACKAGE_ID, null)
    override fun saveInsightsScopePackageId(packageId: String?) {
        if (packageId == null) {
            preferences.edit().remove(KEY_INSIGHTS_SCOPE_PACKAGE_ID).coordinatedApply()
        } else {
            preferences.edit().putString(KEY_INSIGHTS_SCOPE_PACKAGE_ID, packageId).coordinatedApply()
        }
    }
    private companion object {
        const val FILE_NAME = "learning-engine-study"
        const val KEY_NEW = "daily.new"
        const val KEY_REVIEW = "daily.review"
        const val KEY_TYPING_VI_MUTED = "typing.vi-autoplay-muted"
        const val KEY_CONTINUOUS_SKIM = "study.continuous-skim"
        const val KEY_INSIGHTS_SCOPE_PACKAGE_ID = "dashboard.insights.package_id"
    }
}
