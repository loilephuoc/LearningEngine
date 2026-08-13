package vn.loi.learning.android

import android.app.Application
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.ui.AndroidThemeController
import vn.loi.learning.android.ui.SharedPreferencesThemeStore
import vn.loi.learning.android.study.AndroidStudyPreferencesController
import vn.loi.learning.android.study.SharedPreferencesStudyPreferenceStore
import vn.loi.learning.android.platform.AndroidStartupTrace
import vn.loi.learning.infrastructure.persistence.json.JsonPersistenceTrace

class LearningEngineAndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidStartupTrace.enabled = BuildConfig.DEBUG
        JsonPersistenceTrace.enabled = BuildConfig.DEBUG
    }

    val themeController: AndroidThemeController by lazy {
        AndroidThemeController(SharedPreferencesThemeStore(this))
    }
    val studyPreferencesController: AndroidStudyPreferencesController by lazy {
        AndroidStudyPreferencesController(SharedPreferencesStudyPreferenceStore(this))
    }

    private val graphOwner by lazy {
        SingleInstanceOwner { AndroidApplicationGraph.create(this) }
    }

    val graph: AndroidApplicationGraph
        get() = graphOwner.value
}

internal class SingleInstanceOwner<T>(
    create: () -> T
) {
    val value: T by lazy(LazyThreadSafetyMode.SYNCHRONIZED, create)
}
