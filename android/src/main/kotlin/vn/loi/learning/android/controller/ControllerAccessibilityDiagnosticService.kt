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
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.eventTypes = info.eventTypes or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        serviceInfo = info

        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(true)
        ControllerSystemActionBridge.registerLockScreenHandler {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
        ControllerSystemActionBridge.registerForegroundPackageQuery {
            try {
                val app = applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
                val coordinator = app?.homeVocabularyWidgetCoordinator
                val rootPkg = rootInActiveWindow?.packageName?.toString()
                if (rootPkg != null && coordinator?.isTransientSystemPackage(rootPkg) == false) {
                    rootPkg
                } else {
                    val appWindowPkg = windows?.firstOrNull {
                        (it.isActive || it.isFocused) && it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION
                    }?.root?.packageName?.toString()
                        ?: windows?.firstOrNull { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }?.root?.packageName?.toString()
                    appWindowPkg ?: rootPkg
                }
            } catch (_: Throwable) {
                null
            }
        }
        ControllerSystemActionBridge.registerHomeSurfaceReconciler {
            computeAuthoritativeHomeSurfaceState()
        }
        val initialState = computeAuthoritativeHomeSurfaceState()
        val app = applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.setHomeSurfaceState(
            initialState,
            "ACCESSIBILITY_SERVICE_CONNECTED"
        )
        Log.d(ControllerInputDiagnostic.TAG, "[ACCESSIBILITY] Service connected and configured with FLAG_REQUEST_FILTER_KEY_EVENTS")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(false)
        ControllerSystemActionBridge.unregisterLockScreenHandler { false }
        ControllerSystemActionBridge.unregisterForegroundPackageQuery { null }
        ControllerSystemActionBridge.unregisterHomeSurfaceReconciler()
        val app = applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.setHomeSurfaceState(
            vn.loi.learning.android.reminder.HomeSurfaceState.UNKNOWN,
            "ACCESSIBILITY_SERVICE_UNBOUND"
        )
        Log.d(ControllerInputDiagnostic.TAG, "[ACCESSIBILITY] Service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(false)
        ControllerSystemActionBridge.unregisterLockScreenHandler { false }
        ControllerSystemActionBridge.unregisterForegroundPackageQuery { null }
        ControllerSystemActionBridge.unregisterHomeSurfaceReconciler()
        val app = applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.setHomeSurfaceState(
            vn.loi.learning.android.reminder.HomeSurfaceState.UNKNOWN,
            "ACCESSIBILITY_SERVICE_DESTROYED"
        )
        Log.d(ControllerInputDiagnostic.TAG, "[ACCESSIBILITY] Service destroyed")
        super.onDestroy()
    }

    fun computeAuthoritativeHomeSurfaceState(rawEventPkg: String? = null): vn.loi.learning.android.reminder.HomeSurfaceState {
        val app = applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
            ?: return vn.loi.learning.android.reminder.HomeSurfaceState.UNKNOWN
        val coordinator = app.homeVocabularyWidgetCoordinator
        val defaultLauncher = coordinator.resolveDefaultLauncherPackage()

        // 1. Inspect interactive windows to resolve the top TYPE_APPLICATION window
        var topAppPkg: String? = null
        try {
            val appWindows = windows?.filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
            val focusedOrActive = appWindows?.firstOrNull { it.isFocused }
                ?: appWindows?.firstOrNull { it.isActive }
                ?: appWindows?.firstOrNull()
            topAppPkg = focusedOrActive?.root?.packageName?.toString()
        } catch (_: Throwable) {
            topAppPkg = null
        }

        val rootPkg = try { rootInActiveWindow?.packageName?.toString() } catch (_: Throwable) { null }

        // An event or root from our own package:
        // Could be our overlay popup (TYPE_APPLICATION_OVERLAY) or our Activity (TYPE_APPLICATION).
        val isOurPackage = rawEventPkg == "vn.loi.learning.android" || rootPkg == "vn.loi.learning.android"

        val isLauncher = (topAppPkg != null && coordinator.isLauncherPackage(topAppPkg, defaultLauncher)) ||
            (topAppPkg == null && (coordinator.isLauncherPackage(rawEventPkg, defaultLauncher) || coordinator.isLauncherPackage(rootPkg, defaultLauncher)))

        val isTransient = coordinator.isTransientSystemPackage(rawEventPkg) || (topAppPkg != null && coordinator.isTransientSystemPackage(topAppPkg))

        Log.i(
            ControllerInputDiagnostic.TAG,
            "[AUTHORITATIVE_HOME_SURFACE] rawEventPkg=$rawEventPkg rootPkg=$rootPkg topAppPkg=$topAppPkg defaultLauncher=$defaultLauncher isLauncher=$isLauncher isTransient=$isTransient isOurPackage=$isOurPackage currentHomeState=${coordinator.homeSurfaceState}"
        )

        return when {
            isLauncher -> vn.loi.learning.android.reminder.HomeSurfaceState.VISIBLE
            isTransient -> coordinator.homeSurfaceState
            topAppPkg != null -> {
                // Top application window is an Activity (e.g. LearningEngine MainActivity, Chrome, or other 3rd party app)
                vn.loi.learning.android.reminder.HomeSurfaceState.HIDDEN
            }
            isOurPackage -> {
                // If windows list could not be retrieved and the event is from our package:
                val isActivityForeground = ControllerDiagnosticsHolder.state.value.isForeground
                if (isActivityForeground) {
                    vn.loi.learning.android.reminder.HomeSurfaceState.HIDDEN
                } else {
                    // Only overlay popup is present above current surface -> preserve existing surface state!
                    coordinator.homeSurfaceState
                }
            }
            rawEventPkg != null && !coordinator.isTransientSystemPackage(rawEventPkg) -> vn.loi.learning.android.reminder.HomeSurfaceState.HIDDEN
            rootPkg != null && !coordinator.isTransientSystemPackage(rootPkg) -> vn.loi.learning.android.reminder.HomeSurfaceState.HIDDEN
            else -> coordinator.homeSurfaceState
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType ?: return
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val rawPkg = event.packageName?.toString()
                ?: try { rootInActiveWindow?.packageName?.toString() } catch (_: Throwable) { null }

            val app = applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication ?: return
            val coordinator = app.homeVocabularyWidgetCoordinator

            val isLocked = coordinator.isKeyguardLocked()
            if (!isLocked) {
                coordinator.transitionDeviceState(
                    vn.loi.learning.android.reminder.VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON,
                    "ACCESSIBILITY_UNLOCKED"
                )
            }

            val state = computeAuthoritativeHomeSurfaceState(rawPkg)
            if (state != coordinator.homeSurfaceState) {
                coordinator.setHomeSurfaceState(state, "ACCESSIBILITY_EVENT: ${rawPkg ?: "WINDOWS_CHANGED"}")
            }
        }
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
