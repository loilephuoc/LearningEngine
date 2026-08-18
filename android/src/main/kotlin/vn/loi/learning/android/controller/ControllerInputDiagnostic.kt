package vn.loi.learning.android.controller

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ControllerInputDiagnostic {

    const val TAG = "LEController"
    private const val MAX_EVENTS = 100
    private const val MOTION_DEADZONE = 0.25f

    fun formatSource(source: Int): String = buildList {
        if ((source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) add("GAMEPAD")
        if ((source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) add("JOYSTICK")
        if ((source and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) add("DPAD")
        if ((source and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) add("KEYBOARD")
        if (isEmpty()) add("OTHER")
    }.joinToString("|")

    fun formatAction(action: Int): String = when (action) {
        KeyEvent.ACTION_DOWN -> "DOWN"
        KeyEvent.ACTION_UP -> "UP"
        KeyEvent.ACTION_MULTIPLE -> "MULTIPLE"
        MotionEvent.ACTION_MOVE -> "MOTION_MOVE"
        MotionEvent.ACTION_DOWN -> "MOTION_DOWN"
        MotionEvent.ACTION_UP -> "MOTION_UP"
        else -> "ACTION_$action"
    }

    fun isCandidateControllerDevice(device: InputDevice?): Boolean {
        if (device == null) return false
        if (device.isVirtual) return false
        val vendorId = device.vendorId
        val productId = device.productId
        if (vendorId == 0x2dc8 || productId == 0x9020 || productId == 0x9021) return true

        val sources = device.sources
        val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        val isDpad = (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        val name = device.name.orEmpty()
        val isNamedController = name.contains("8BitDo", ignoreCase = true) ||
            name.contains("Micro", ignoreCase = true) ||
            name.contains("Gamepad", ignoreCase = true) ||
            name.contains("Controller", ignoreCase = true) ||
            name.contains("Joy-Con", ignoreCase = true) ||
            name.contains("Wireless Controller", ignoreCase = true)

        val isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            device.isExternal
        } else {
            !device.isVirtual
        }

        return isGamepad || isJoystick || isNamedController || (isExternal && isDpad)
    }

    fun isCandidateControllerDevice(info: InputDeviceInfo): Boolean {
        if (info.vendorId == 0x2dc8 || info.productId == 0x9020 || info.productId == 0x9021) return true
        val isNamedController = info.name.contains("8BitDo", ignoreCase = true) ||
            info.name.contains("Micro", ignoreCase = true) ||
            info.name.contains("Gamepad", ignoreCase = true) ||
            info.name.contains("Controller", ignoreCase = true) ||
            info.name.contains("Joy-Con", ignoreCase = true) ||
            info.name.contains("Wireless Controller", ignoreCase = true)
        return info.isGamepad || info.isJoystick || isNamedController || (info.isExternal && info.isDpad)
    }

    fun classifyDevice(device: InputDevice): InputDeviceInfo {
        val sources = device.sources
        val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        val isDpad = (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        val isKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
        val isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            device.isExternal
        } else {
            !device.isVirtual
        }

        return InputDeviceInfo(
            id = device.id,
            name = device.name ?: "Unknown Device",
            descriptor = runCatching { device.descriptor }.getOrDefault(""),
            vendorId = device.vendorId,
            productId = device.productId,
            sources = sources,
            isGamepad = isGamepad,
            isJoystick = isJoystick,
            isDpad = isDpad,
            isKeyboard = isKeyboard,
            isExternal = isExternal
        )
    }

    fun listConnectedDevices(inputManager: InputManager?): List<InputDeviceInfo> {
        val deviceIds = InputDevice.getDeviceIds() ?: return emptyList()
        return deviceIds.toList().mapNotNull { id ->
            val dev = if (inputManager != null) inputManager.getInputDevice(id) else InputDevice.getDevice(id)
            dev?.let(::classifyDevice)
        }.sortedWith(
            compareByDescending<InputDeviceInfo> { it.isCandidateController }
                .thenBy { it.id }
        )
    }

    fun normalizeKeyEvent(
        event: KeyEvent,
        testingMode: TestingMode,
        origin: EventOrigin = EventOrigin.ACTIVITY
    ): ControllerInputEvent {
        val device = event.device
        val deviceName = device?.name ?: "Unknown (id=${event.deviceId})"
        val descriptor = runCatching { device?.descriptor }.getOrNull() ?: ""
        val vendorId = device?.vendorId ?: 0
        val productId = device?.productId ?: 0
        val sources = device?.sources ?: event.source
        val source = event.source
        val keyCode = event.keyCode
        val keyCodeName = runCatching { KeyEvent.keyCodeToString(keyCode) }.getOrDefault("KEY_$keyCode")
        val scanCode = event.scanCode
        val isCandidate = isCandidateControllerDevice(device) || (source and InputDevice.SOURCE_GAMEPAD) != 0

        return ControllerInputEvent(
            timestamp = System.currentTimeMillis(),
            deviceId = event.deviceId,
            deviceName = deviceName,
            descriptor = descriptor,
            vendorId = vendorId,
            productId = productId,
            sources = sources,
            source = source,
            sourceName = formatSource(source),
            action = event.action,
            actionName = formatAction(event.action),
            keyCode = keyCode,
            keyCodeName = keyCodeName,
            scanCode = scanCode,
            repeatCount = event.repeatCount,
            metaState = event.metaState,
            flags = event.flags,
            isCandidateController = isCandidate,
            origin = origin,
            testingMode = testingMode
        )
    }

    fun normalizeMotionEvent(event: MotionEvent, testingMode: TestingMode): List<ControllerInputEvent> {
        val device = event.device
        val deviceName = device?.name ?: "Unknown (id=${event.deviceId})"
        val descriptor = runCatching { device?.descriptor }.getOrNull() ?: ""
        val vendorId = device?.vendorId ?: 0
        val productId = device?.productId ?: 0
        val sources = device?.sources ?: event.source
        val source = event.source
        val isCandidate = isCandidateControllerDevice(device) || (source and InputDevice.SOURCE_JOYSTICK) != 0

        val results = mutableListOf<ControllerInputEvent>()

        // Check Hat X / Hat Y (D-pad on many controllers)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        if (Math.abs(hatX) >= MOTION_DEADZONE || Math.abs(hatY) >= MOTION_DEADZONE) {
            results.add(
                ControllerInputEvent(
                    timestamp = System.currentTimeMillis(),
                    deviceId = event.deviceId,
                    deviceName = deviceName,
                    descriptor = descriptor,
                    vendorId = vendorId,
                    productId = productId,
                    sources = sources,
                    source = source,
                    sourceName = formatSource(source),
                    action = event.action,
                    actionName = "MOTION_HAT",
                    keyCode = -1,
                    keyCodeName = "AXIS_HAT",
                    scanCode = 0,
                    repeatCount = 0,
                    metaState = event.metaState,
                    flags = event.flags,
                    isCandidateController = isCandidate,
                    hatX = hatX,
                    hatY = hatY,
                    testingMode = testingMode
                )
            )
        }

        // Check Stick X / Y
        val stickX = event.getAxisValue(MotionEvent.AXIS_X)
        val stickY = event.getAxisValue(MotionEvent.AXIS_Y)
        if (Math.abs(stickX) >= MOTION_DEADZONE || Math.abs(stickY) >= MOTION_DEADZONE) {
            results.add(
                ControllerInputEvent(
                    timestamp = System.currentTimeMillis(),
                    deviceId = event.deviceId,
                    deviceName = deviceName,
                    descriptor = descriptor,
                    vendorId = vendorId,
                    productId = productId,
                    sources = sources,
                    source = source,
                    sourceName = formatSource(source),
                    action = event.action,
                    actionName = "MOTION_STICK",
                    keyCode = -1,
                    keyCodeName = "AXIS_STICK",
                    scanCode = 0,
                    repeatCount = 0,
                    metaState = event.metaState,
                    flags = event.flags,
                    isCandidateController = isCandidate,
                    axis = MotionEvent.AXIS_X,
                    axisValue = stickX,
                    hatX = stickX,
                    hatY = stickY,
                    testingMode = testingMode
                )
            )
        }

        // Check L2 / R2 analog triggers (AXIS_LTRIGGER, AXIS_RTRIGGER, AXIS_BRAKE, AXIS_GAS, AXIS_Z, AXIS_RZ)
        val lTrigger = Math.max(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val rTrigger = Math.max(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
        if (lTrigger >= MOTION_DEADZONE) {
            results.add(
                ControllerInputEvent(
                    timestamp = System.currentTimeMillis(),
                    deviceId = event.deviceId,
                    deviceName = deviceName,
                    descriptor = descriptor,
                    vendorId = vendorId,
                    productId = productId,
                    sources = sources,
                    source = source,
                    sourceName = formatSource(source),
                    action = event.action,
                    actionName = "MOTION_LTRIGGER",
                    keyCode = -1,
                    keyCodeName = "AXIS_LTRIGGER",
                    scanCode = 0,
                    repeatCount = 0,
                    metaState = event.metaState,
                    flags = event.flags,
                    isCandidateController = isCandidate,
                    axis = MotionEvent.AXIS_LTRIGGER,
                    axisValue = lTrigger,
                    testingMode = testingMode
                )
            )
        }
        if (rTrigger >= MOTION_DEADZONE) {
            results.add(
                ControllerInputEvent(
                    timestamp = System.currentTimeMillis(),
                    deviceId = event.deviceId,
                    deviceName = deviceName,
                    descriptor = descriptor,
                    vendorId = vendorId,
                    productId = productId,
                    sources = sources,
                    source = source,
                    sourceName = formatSource(source),
                    action = event.action,
                    actionName = "MOTION_RTRIGGER",
                    keyCode = -1,
                    keyCodeName = "AXIS_RTRIGGER",
                    scanCode = 0,
                    repeatCount = 0,
                    metaState = event.metaState,
                    flags = event.flags,
                    isCandidateController = isCandidate,
                    axis = MotionEvent.AXIS_RTRIGGER,
                    axisValue = rTrigger,
                    testingMode = testingMode
                )
            )
        }

        return results
    }

    fun generateDiagnosticsSummary(state: ControllerDiagnosticsState): String = buildString {
        appendLine("=== 8BITDO / CONTROLLER HARDWARE DIAGNOSTICS SUMMARY ===")
        appendLine("Testing Mode: ${state.testingMode.label}")
        appendLine("App State: ${if (state.isForeground) "FOREGROUND" else "BACKGROUND"}")
        appendLine("Accessibility Service: ${if (state.isAccessibilityServiceConnected) "CONNECTED" else "DISCONNECTED"}")
        appendLine()
        appendLine("--- Connected Devices (${state.connectedDevices.size}) ---")
        state.connectedDevices.forEach { dev ->
            appendLine("• [id=${dev.id}] ${dev.name}")
            appendLine("  VendorId: 0x${dev.vendorId.toString(16)} ProductId: 0x${dev.productId.toString(16)}")
            appendLine("  Sources: ${dev.sourceLabels.joinToString(", ")} (raw=0x${dev.sources.toString(16)})")
            appendLine("  Descriptor: ${dev.descriptor}")
            appendLine("  Candidate Controller: ${dev.isCandidateController}")
        }
        appendLine()
        appendLine("--- Key Press Counts ---")
        if (state.keyCounts.isEmpty()) {
            appendLine("(No keys recorded yet)")
        } else {
            state.keyCounts.entries.sortedByDescending { it.value }.forEach { (key, count) ->
                appendLine("  $key: $count")
            }
        }
        appendLine()
        appendLine("--- Recent Events (Last ${state.events.size}) ---")
        state.events.takeLast(30).reversed().forEach { event ->
            appendLine(event.toLogLine())
        }
        appendLine("=========================================================")
    }
}

object ControllerDiagnosticsHolder {

    private val _state = MutableStateFlow(ControllerDiagnosticsState())
    val state: StateFlow<ControllerDiagnosticsState> = _state.asStateFlow()

    fun setTestingMode(mode: TestingMode) {
        _state.update { it.copy(testingMode = mode) }
    }

    fun setForeground(isForeground: Boolean) {
        _state.update { it.copy(isForeground = isForeground) }
    }

    fun setLifecycleState(lifecycle: String) {
        _state.update { it.copy(lifecycleState = lifecycle) }
    }

    fun setAccessibilityServiceConnected(connected: Boolean) {
        _state.update { it.copy(isAccessibilityServiceConnected = connected) }
    }

    fun refreshDevices(context: Context) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
        val devices = ControllerInputDiagnostic.listConnectedDevices(inputManager)
        _state.update { it.copy(connectedDevices = devices) }
    }

    fun registerInputDeviceListener(context: Context) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return
        refreshDevices(context)
        inputManager.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                refreshDevices(context)
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                refreshDevices(context)
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                refreshDevices(context)
            }
        }, null)
    }

    fun recordKeyEvent(
        event: KeyEvent,
        origin: EventOrigin = EventOrigin.ACTIVITY
    ): ControllerInputEvent {
        val mode = _state.value.testingMode
        val normalized = ControllerInputDiagnostic.normalizeKeyEvent(event, mode, origin)
        Log.d(ControllerInputDiagnostic.TAG, normalized.toLogLine())

        _state.update { current ->
            val newEvents = (current.events + normalized).takeLast(100)
            val keyLabel = if (normalized.action == KeyEvent.ACTION_DOWN) "${normalized.origin.name}: ${normalized.keyCodeName}" else null
            val newCounts = if (keyLabel != null) {
                val existing = current.keyCounts[keyLabel] ?: 0
                current.keyCounts + (keyLabel to (existing + 1))
            } else {
                current.keyCounts
            }
            current.copy(
                events = newEvents,
                keyCounts = newCounts,
                lastEvent = normalized
            )
        }
        return normalized
    }

    fun recordMotionEvent(event: MotionEvent): List<ControllerInputEvent> {
        val mode = _state.value.testingMode
        val normalizedList = ControllerInputDiagnostic.normalizeMotionEvent(event, mode)
        if (normalizedList.isNotEmpty()) {
            for (normalized in normalizedList) {
                Log.d(ControllerInputDiagnostic.TAG, normalized.toLogLine())
            }
            _state.update { current ->
                val newEvents = (current.events + normalizedList).takeLast(100)
                current.copy(
                    events = newEvents,
                    lastEvent = normalizedList.lastOrNull() ?: current.lastEvent
                )
            }
        }
        return normalizedList
    }

    const val TRACE_TAG = "LearningEngineControllerTrace"
    private const val MAX_TRACE_ENTRIES = 50

    fun setGlobalMute(muted: Boolean) {
        _state.update { it.copy(isGlobalMuted = muted) }
    }

    fun setActiveStudyPlayback(playback: ActiveStudyPlayback?) {
        _state.update { it.copy(activeStudyPlayback = playback) }
    }

    fun recordVolumeResult(
        direction: String,
        result: vn.loi.learning.android.media.SystemVolumeAdjustmentResult,
        status: vn.loi.learning.android.media.SystemVolumeStatus
    ) {
        val summary = when (result) {
            is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Success ->
                "$direction: ${result.before} -> ${result.after} (min=${result.min}, max=${result.max})"
            is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Boundary ->
                "$direction: Boundary - ${result.message}"
            is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.FixedVolume ->
                "$direction: Fixed - ${result.message}"
            is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.NoChange ->
                "$direction: NoChange - ${result.message}"
            is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Error ->
                "$direction: Error - ${result.message}"
        }
        _state.update {
            it.copy(
                systemVolumeStatus = status,
                lastVolumeAction = summary
            )
        }
    }

    fun updateQuickVoiceRecorderDiagnostic(
        state: vn.loi.learning.android.recording.QuickVoiceRecorderState,
        recordingsCount: Int,
        latestRecording: vn.loi.learning.android.recording.VoiceRecordingItem?
    ) {
        _state.update {
            it.copy(
                quickVoiceRecorderState = state,
                quickVoiceRecordingsCount = recordingsCount,
                latestVoiceRecording = latestRecording
            )
        }
    }

    fun recordQuickVoiceTrace(
        event: vn.loi.learning.android.recording.QuickVoiceTraceEvent,
        detail: String
    ) {
        val entry = QuickVoiceTraceEntry(
            event = event,
            detail = detail
        )
        Log.i(TRACE_TAG, entry.toLogLine())
        _state.update { current ->
            val newTrace = (current.quickVoiceTrace + entry).takeLast(MAX_TRACE_ENTRIES)
            current.copy(quickVoiceTrace = newTrace)
        }
    }

    fun recordRuntimeTrace(entry: ControllerRuntimeTraceEntry) {
        Log.i(TRACE_TAG, entry.toLogLine())
        _state.update { current ->
            val newTrace = (current.runtimeTrace + entry).takeLast(MAX_TRACE_ENTRIES)
            current.copy(runtimeTrace = newTrace)
        }
    }

    fun recordAudioTrace(entry: StudyAudioTraceEntry) {
        Log.i(TRACE_TAG, entry.toLogLine())
        _state.update { current ->
            val newTrace = (current.audioTrace + entry).takeLast(MAX_TRACE_ENTRIES)
            current.copy(audioTrace = newTrace)
        }
    }

    fun setLastDispatchedResult(result: ControllerActionResult?) {
        _state.update { it.copy(lastDispatchedResult = result) }
    }

    fun clear() {
        _state.update {
            it.copy(
                events = emptyList(),
                keyCounts = emptyMap(),
                runtimeTrace = emptyList(),
                audioTrace = emptyList(),
                quickVoiceTrace = emptyList(),
                lastEvent = null,
                lastDispatchedResult = null,
                activeStudyPlayback = null,
                lastVolumeAction = null,
                quickVoiceRecorderState = vn.loi.learning.android.recording.QuickVoiceRecorderState.Idle(),
                quickVoiceRecordingsCount = 0,
                latestVoiceRecording = null
            )
        }
    }
}
