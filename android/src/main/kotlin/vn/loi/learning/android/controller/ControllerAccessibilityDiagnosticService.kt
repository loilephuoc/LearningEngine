package vn.loi.learning.android.controller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Diagnostic-only AccessibilityService to observe 8BitDo Micro hardware key events
 * in background and screen-off states via official Android FLAG_REQUEST_FILTER_KEY_EVENTS.
 *
 * Privacy & Safety guarantees:
 * - Strictly observes candidate external controllers (8BitDo VID 0x2dc8 / Gamepads).
 * - Ignores built-in touchscreens, virtual keyboards, and standard phone inputs.
 * - Does NOT inspect screen text or window contents.
 * - Does NOT perform gestures, automations, or dispatch learning actions.
 * - Returns false from onKeyEvent to avoid consuming or breaking system input routing.
 */
class ControllerAccessibilityDiagnosticService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        serviceInfo = info

        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(true)
        ControllerSystemActionBridge.registerLockScreenHandler {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
        Log.d(ControllerInputDiagnostic.TAG, "[ACCESSIBILITY] Service connected and configured with FLAG_REQUEST_FILTER_KEY_EVENTS")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(false)
        ControllerSystemActionBridge.unregisterLockScreenHandler { false }
        Log.d(ControllerInputDiagnostic.TAG, "[ACCESSIBILITY] Service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(false)
        ControllerSystemActionBridge.unregisterLockScreenHandler { false }
        Log.d(ControllerInputDiagnostic.TAG, "[ACCESSIBILITY] Service destroyed")
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally no-op: We do NOT inspect window contents or UI events.
    }

    override fun onInterrupt() {
        // Intentionally no-op
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        val pm = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        val isInteractiveBefore = pm?.isInteractive ?: true

        val device = event.device ?: if (event.deviceId > 0) android.view.InputDevice.getDevice(event.deviceId) else null
        val isCandidate = ControllerInputDiagnostic.isCandidateControllerDevice(device)
        if (!isCandidate) {
            return false
        }

        val router = ControllerInputRouter.getInstance(applicationContext)
        val isEnabled = router.isControllerEnabled()

        val dispatchResult = router.onKeyEvent(event, origin = EventOrigin.ACCESSIBILITY)
        val returnVal = isEnabled

        if (!isInteractiveBefore) {
            Log.i(
                ControllerInputDiagnostic.TAG,
                "[SCREEN_OFF_FIRST_PRESS] ts=${System.currentTimeMillis()} isInteractiveBefore=false device=${device?.name} vid=0x${device?.vendorId?.toString(16)} pid=0x${device?.productId?.toString(16)} key=${KeyEvent.keyCodeToString(event.keyCode)}(${event.keyCode}) scanCode=${event.scanCode} action=${if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"} isEnabled=$isEnabled routerResult=$dispatchResult RETURN=$returnVal"
            )
        } else {
            Log.i(
                ControllerInputDiagnostic.TAG,
                "[NO_WAKE_AUDIT] ts=${System.currentTimeMillis()} isInteractive=$isInteractiveBefore device=${device?.name} vid=0x${device?.vendorId?.toString(16)} pid=0x${device?.productId?.toString(16)} key=${KeyEvent.keyCodeToString(event.keyCode)}(${event.keyCode}) scanCode=${event.scanCode} action=${if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"} isEnabled=$isEnabled routerResult=$dispatchResult RETURN=$returnVal"
            )
        }

        return returnVal
    }
}
