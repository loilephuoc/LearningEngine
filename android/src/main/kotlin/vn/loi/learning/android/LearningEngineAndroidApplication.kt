package vn.loi.learning.android

import android.app.Application
import vn.loi.learning.android.platform.AndroidApplicationGraph

class LearningEngineAndroidApplication : Application() {
    val graph: AndroidApplicationGraph by lazy { AndroidApplicationGraph.create(this) }
}
