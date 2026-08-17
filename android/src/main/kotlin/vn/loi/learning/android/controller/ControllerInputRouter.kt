package vn.loi.learning.android.controller

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Central router connecting raw hardware key events to gesture detection,
 * mapping resolution, and action dispatching.
 */
class ControllerInputRouter(
    private val appContext: Context,
    private val preferencesController: ControllerPreferencesController,
    val gestureDetector: ControllerGestureDetector = ControllerGestureDetector(),
    val dispatcher: ControllerActionDispatcher = ControllerActionDispatcher(appContext),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    /**
     * Handles an incoming KeyEvent from either ACTIVITY or ACCESSIBILITY.
     */
    fun onKeyEvent(event: KeyEvent, origin: EventOrigin): ControllerActionResult? {
        val config = preferencesController.current()

        // Record into diagnostic state for transparency
        val normalizedEvent = ControllerDiagnosticsHolder.recordKeyEvent(event, origin)

        if (!config.isControllerEnabled) {
            val res = ControllerActionResult.Disabled("Controller input is disabled in settings")
            recordDiagnosticTrace(
                origin = origin,
                keyCode = event.keyCode,
                pressType = ControllerPressType.PRESS,
                action = null,
                result = res,
                gestureLabel = KeyEvent.keyCodeToString(event.keyCode)
            )
            ControllerDiagnosticsHolder.setLastDispatchedResult(res)
            return res
        }

        val activeProfile = config.activeProfile
        if (!activeProfile.enabled) {
            val res = ControllerActionResult.Disabled("Active profile '${activeProfile.name}' is disabled")
            recordDiagnosticTrace(
                origin = origin,
                keyCode = event.keyCode,
                pressType = ControllerPressType.PRESS,
                action = null,
                result = res,
                gestureLabel = KeyEvent.keyCodeToString(event.keyCode)
            )
            ControllerDiagnosticsHolder.setLastDispatchedResult(res)
            return res
        }

        // 1. Drain any previously expired gestures
        coroutineScope.launch {
            drainAndDispatchExpired(origin)
        }

        // 2. Process through gesture detector (deduplication + chord + multi-press arbitration)
        val gesture = gestureDetector.processKeyEvent(event, origin, activeProfile)

        // If buffered for double-press window, schedule a delayed check to drain
        if (gesture == null) {
            coroutineScope.launch {
                delay(320L)
                drainAndDispatchExpired(origin)
            }
            return null
        }

        coroutineScope.launch {
            dispatchGesture(gesture, origin)
        }
        return null
    }

    /**
     * Dispatches a recognized logical gesture to the mapping resolver and action dispatcher.
     * Suspends until any associated Study/Domain state mutation has committed.
     */
    suspend fun dispatchGesture(
        gesture: ControllerGesture,
        origin: EventOrigin = EventOrigin.ACTIVITY
    ): ControllerActionResult {
        val t1 = System.currentTimeMillis()
        val lifecycle = ControllerDiagnosticsHolder.state.value.lifecycleState
        val stateBefore = StudyControllerBridge.describeState()
        val contextBefore = dispatcher.detectCurrentContext()
        val config = preferencesController.current()

        val action = ControllerMappingResolver.resolve(config, contextBefore, gesture)
        val result = if (action == null) {
            val res = ControllerActionResult.Unmapped(gesture)
            Log.d(ControllerInputDiagnostic.TAG, "Unmapped gesture: $gesture in context: $contextBefore")
            res
        } else {
            dispatcher.dispatch(action, contextBefore)
        }
        val t2 = System.currentTimeMillis()

        val stateAfter = StudyControllerBridge.describeState()
        val contextAfter = dispatcher.detectCurrentContext()

        val trace = ControllerRuntimeTraceEntry(
            timestamp = t1,
            origin = origin,
            buttonLabel = ControllerButtonDirectory.getButtonLabel(gesture.input.keyCode),
            keyCode = gesture.input.keyCode,
            pressType = gesture.pressType,
            lifecycleState = lifecycle,
            stateBefore = stateBefore,
            contextBefore = contextBefore,
            resolvedMapping = if (action != null) "$contextBefore + ${gesture.displayLabel} -> $action" else "Unmapped(${gesture.displayLabel})",
            targetInvoked = when (result) {
                is ControllerActionResult.Executed -> result.target
                is ControllerActionResult.UnavailableInContext -> "Unavailable: ${result.reason}"
                is ControllerActionResult.Disabled -> "Disabled: ${result.reason}"
                is ControllerActionResult.Unmapped -> "Unmapped"
                else -> result.toString()
            },
            eventSubmitted = action?.name,
            stateAfter = stateAfter,
            contextAfter = contextAfter,
            dispatchResult = when (result) {
                is ControllerActionResult.Executed -> "COMMITTED"
                is ControllerActionResult.UnavailableInContext -> "UNAVAILABLE"
                is ControllerActionResult.Disabled -> "DISABLED"
                is ControllerActionResult.Unmapped -> "UNMAPPED"
                else -> "OTHER"
            },
            latencyMs = t2 - t1
        )
        ControllerDiagnosticsHolder.recordRuntimeTrace(trace)
        ControllerDiagnosticsHolder.setLastDispatchedResult(result)
        return result
    }

    /**
     * Drains any expired buffered single-press gestures and dispatches them.
     */
    suspend fun drainAndDispatchExpired(origin: EventOrigin = EventOrigin.ACTIVITY): List<ControllerActionResult> {
        val drained = gestureDetector.drainExpiredGestures()
        if (drained.isEmpty()) return emptyList()

        val results = mutableListOf<ControllerActionResult>()
        for (gesture in drained) {
            results.add(dispatchGesture(gesture, origin))
        }
        return results
    }

    private fun recordDiagnosticTrace(
        origin: EventOrigin,
        keyCode: Int,
        pressType: ControllerPressType,
        action: ControllerAction?,
        result: ControllerActionResult,
        gestureLabel: String
    ) {
        val state = StudyControllerBridge.describeState()
        val context = dispatcher.detectCurrentContext()
        val lifecycle = ControllerDiagnosticsHolder.state.value.lifecycleState
        val trace = ControllerRuntimeTraceEntry(
            timestamp = System.currentTimeMillis(),
            origin = origin,
            buttonLabel = ControllerButtonDirectory.getButtonLabel(keyCode),
            keyCode = keyCode,
            pressType = pressType,
            lifecycleState = lifecycle,
            stateBefore = state,
            contextBefore = context,
            resolvedMapping = if (action != null) "$context + $gestureLabel -> $action" else "Unmapped($gestureLabel)",
            targetInvoked = when (result) {
                is ControllerActionResult.Executed -> result.target
                is ControllerActionResult.UnavailableInContext -> "Unavailable: ${result.reason}"
                is ControllerActionResult.Disabled -> "Disabled: ${result.reason}"
                is ControllerActionResult.Unmapped -> "Unmapped"
                else -> result.toString()
            },
            eventSubmitted = action?.name,
            stateAfter = state,
            contextAfter = context,
            dispatchResult = when (result) {
                is ControllerActionResult.Executed -> "COMMITTED"
                is ControllerActionResult.UnavailableInContext -> "UNAVAILABLE"
                is ControllerActionResult.Disabled -> "DISABLED"
                is ControllerActionResult.Unmapped -> "UNMAPPED"
                else -> "OTHER"
            },
            latencyMs = 0L
        )
        ControllerDiagnosticsHolder.recordRuntimeTrace(trace)
    }

    fun isControllerEnabled(): Boolean {
        val config = preferencesController.current()
        return config.isControllerEnabled && config.activeProfile.enabled
    }

    companion object {
        @Volatile
        private var instance: ControllerInputRouter? = null

        fun getInstance(context: Context): ControllerInputRouter {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val app = context.applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
                    val prefController = app?.controllerPreferencesController
                        ?: ControllerPreferencesController(SharedPreferencesControllerPreferenceStore(context.applicationContext))
                    ControllerInputRouter(context.applicationContext, prefController).also {
                        instance = it
                    }
                }
            }
        }
    }
}
