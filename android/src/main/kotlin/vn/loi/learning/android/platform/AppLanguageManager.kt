package vn.loi.learning.android.platform

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

import androidx.annotation.StringRes
import vn.loi.learning.android.R

enum class AppLanguage(val code: String, val label: String, @StringRes val labelRes: Int) {
    ENGLISH("en", "English", R.string.settings_language_en),
    VIETNAMESE("vi", "Tiếng Việt", R.string.settings_language_vi);

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

object AppLanguageManager {
    private const val PREFS_NAME = "learning_engine_language_prefs"
    private const val KEY_LANGUAGE = "app_language"

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        val lang = AppLanguage.fromCode(code)
        _currentLanguage.value = lang
        applyLocale(context, lang)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        if (_currentLanguage.value == language) return
        _currentLanguage.value = language
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        applyLocale(context, language)
    }

    fun applyLocale(context: Context, language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList(locale)
            } catch (_: Throwable) {}
        }

        val resources = context.resources
        val config = Configuration(resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.setLocale(locale)
        }
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
