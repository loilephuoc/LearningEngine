package vn.loi.learning.android

import android.app.Application
import vn.loi.learning.android.platform.AndroidApplicationGraph

class LearningEngineAndroidApplication : Application() {
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
