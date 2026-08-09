package vn.loi.learning.android

import android.app.Application
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.ui.AndroidThemeController
import vn.loi.learning.android.ui.SharedPreferencesThemeStore
import vn.loi.learning.android.study.AndroidStudyPreferencesController
import vn.loi.learning.android.study.SharedPreferencesStudyPreferenceStore

class LearningEngineAndroidApplication : Application() {
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
