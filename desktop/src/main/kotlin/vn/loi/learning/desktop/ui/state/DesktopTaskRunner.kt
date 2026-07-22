package vn.loi.learning.desktop.ui.state

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface DesktopTaskRunner {
    fun <T> run(
        work: () -> T,
        onSuccess: (T) -> Unit,
        onFailure: (Exception) -> Unit
    )

    fun dispatch(action: () -> Unit)
}

object ImmediateDesktopTaskRunner : DesktopTaskRunner {
    override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            onSuccess(work())
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }

    override fun dispatch(action: () -> Unit) = action()
}

class CoroutineDesktopTaskRunner(
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DesktopTaskRunner {
    override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                onSuccess(withContext(workerDispatcher) { work() })
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                onFailure(exception)
            }
        }
    }

    override fun dispatch(action: () -> Unit) {
        scope.launch { action() }
    }
}
