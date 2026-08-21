package vn.loi.learning.android.controller

import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe, narrow system-action bridge connecting ControllerActionDispatcher
 * to platform accessibility capabilities (e.g. Lock Screen) without leaking service references.
 */
object ControllerSystemActionBridge {

    private val lockScreenHandlerRef = AtomicReference<(() -> Boolean)?>(null)
    private val foregroundPackageQueryRef = AtomicReference<(() -> String?)?>(null)
    private val homeSurfaceReconcilerRef = AtomicReference<(() -> vn.loi.learning.android.reminder.HomeSurfaceState)?>(null)

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

    fun registerForegroundPackageQuery(query: () -> String?) {
        foregroundPackageQueryRef.set(query)
    }

    fun unregisterForegroundPackageQuery(query: () -> String?) {
        foregroundPackageQueryRef.compareAndSet(query, null)
    }

    fun unregisterForegroundPackageQuery() {
        foregroundPackageQueryRef.set(null)
    }

    fun registerHomeSurfaceReconciler(reconciler: () -> vn.loi.learning.android.reminder.HomeSurfaceState) {
        homeSurfaceReconcilerRef.set(reconciler)
    }

    fun unregisterHomeSurfaceReconciler(reconciler: () -> vn.loi.learning.android.reminder.HomeSurfaceState) {
        homeSurfaceReconcilerRef.compareAndSet(reconciler, null)
    }

    fun unregisterHomeSurfaceReconciler() {
        homeSurfaceReconcilerRef.set(null)
    }

    fun reconcileHomeSurface(): vn.loi.learning.android.reminder.HomeSurfaceState {
        return homeSurfaceReconcilerRef.get()?.invoke() ?: vn.loi.learning.android.reminder.HomeSurfaceState.UNKNOWN
    }

    fun isHomeSurfaceReconcilerAvailable(): Boolean {
        return homeSurfaceReconcilerRef.get() != null
    }

    fun clear() {
        lockScreenHandlerRef.set(null)
        foregroundPackageQueryRef.set(null)
        homeSurfaceReconcilerRef.set(null)
    }
}
