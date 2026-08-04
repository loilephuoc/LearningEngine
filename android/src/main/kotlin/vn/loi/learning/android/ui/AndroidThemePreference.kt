package vn.loi.learning.android.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AndroidThemeMode(val label: String) {
    FOLLOW_SYSTEM("Follow system"), LIGHT("Light"), DARK("Dark");

    fun resolveDark(systemDark: Boolean): Boolean = when (this) {
        FOLLOW_SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

interface AndroidThemePreferenceStore {
    fun load(): AndroidThemeMode
    fun save(mode: AndroidThemeMode)
}

class AndroidThemeController(
    private val store: AndroidThemePreferenceStore
) {
    private val mutableMode = MutableStateFlow(store.load())
    val mode: StateFlow<AndroidThemeMode> = mutableMode.asStateFlow()

    fun setMode(mode: AndroidThemeMode) {
        if (mode == mutableMode.value) return
        store.save(mode)
        mutableMode.value = mode
    }
}

class SharedPreferencesThemeStore(context: Context) : AndroidThemePreferenceStore {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun load(): AndroidThemeMode =
        preferences.getString(KEY_MODE, null)
            ?.let { persisted -> AndroidThemeMode.entries.firstOrNull { it.name == persisted } }
            ?: AndroidThemeMode.FOLLOW_SYSTEM

    override fun save(mode: AndroidThemeMode) {
        preferences.edit().putString(KEY_MODE, mode.name).apply()
    }

    private companion object {
        const val FILE_NAME = "learning-engine-presentation"
        const val KEY_MODE = "theme.mode"
    }
}
