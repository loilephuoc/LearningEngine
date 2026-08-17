package vn.loi.learning.android.controller

import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe, narrow system-action bridge connecting ControllerActionDispatcher
 * to platform accessibility capabilities (e.g. Lock Screen) without leaking service references.
 */
object ControllerSystemActionBridge {

    private val lockScreenHandlerRef = AtomicReference<(() -> Boolean)?>(null)

    fun registerLockScreenHandler(handler: () -> Boolean) {
        lockScreenHandlerRef.set(handler)
    }

    fun unregisterLockScreenHandler(handler: () -> Boolean) {
        lockScreenHandlerRef.compareAndSet(handler, null)
    }

    fun lockScreen(): Boolean {
        return lockScreenHandlerRef.get()?.invoke() ?: false
    }

    fun isLockScreenAvailable(): Boolean {
        return lockScreenHandlerRef.get() != null
    }

    fun clear() {
        lockScreenHandlerRef.set(null)
    }
}
