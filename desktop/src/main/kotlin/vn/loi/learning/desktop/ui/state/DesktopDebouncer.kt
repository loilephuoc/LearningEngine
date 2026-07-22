package vn.loi.learning.desktop.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun interface DesktopDebouncer {
    fun submit(action: () -> Unit)
}

object ImmediateDesktopDebouncer : DesktopDebouncer {
    override fun submit(action: () -> Unit) = action()
}

class CoroutineDesktopDebouncer(
    private val scope: CoroutineScope,
    private val delayMillis: Long = 250L
) : DesktopDebouncer {
    private var pending: Job? = null

    override fun submit(action: () -> Unit) {
        pending?.cancel()
        pending = scope.launch {
            delay(delayMillis)
            action()
        }
    }
}
