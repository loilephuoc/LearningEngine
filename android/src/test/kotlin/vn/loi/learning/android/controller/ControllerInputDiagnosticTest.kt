package vn.loi.learning.android.controller

import android.view.InputDevice
import android.view.KeyEvent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class ControllerInputDiagnosticTest {

    @Before
    fun setUp() {
        ControllerDiagnosticsHolder.clear()
        ControllerDiagnosticsHolder.setTestingMode(TestingMode.S)
        ControllerDiagnosticsHolder.setForeground(true)
        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(false)
    }

    @Test
    fun `device info classifies gamepad, joystick, dpad, and candidate status`() {
        val gamepadInfo = InputDeviceInfo(
            id = 1,
            name = "8BitDo Micro Gamepad",
            descriptor = "desc-123",
            vendorId = 0x2dc8,
            productId = 0x9020,
            sources = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK,
            isGamepad = true,
            isJoystick = true,
            isDpad = false,
            isKeyboard = false,
            isExternal = true
        )

        assertTrue(gamepadInfo.isCandidateController)
        assertEquals(listOf("GAMEPAD", "JOYSTICK"), gamepadInfo.sourceLabels)

        val keyboardInfo = InputDeviceInfo(
            id = 2,
            name = "8BitDo Micro Keyboard Mode",
            descriptor = "desc-456",
            vendorId = 0x2dc8,
            productId = 0x9021,
            sources = InputDevice.SOURCE_KEYBOARD,
            isGamepad = false,
            isJoystick = false,
            isDpad = false,
            isKeyboard = true,
            isExternal = true
        )

        assertTrue(keyboardInfo.isCandidateController)
        assertEquals(listOf("KEYBOARD"), keyboardInfo.sourceLabels)

        val touchInfo = InputDeviceInfo(
            id = 3,
            name = "touchscreen",
            descriptor = "desc-789",
            vendorId = 0,
            productId = 0,
            sources = InputDevice.SOURCE_TOUCHSCREEN,
            isGamepad = false,
            isJoystick = false,
            isDpad = false,
            isKeyboard = false,
            isExternal = false
        )

        assertFalse(touchInfo.isCandidateController)
        assertEquals(listOf("OTHER"), touchInfo.sourceLabels)
    }

    @Test
    fun `key event normalization captures all required fields, origin tag, and formats log line`() {
        val eventActivity = ControllerInputEvent(
            timestamp = 1700000000000L,
            deviceId = 5,
            deviceName = "8BitDo Micro",
            descriptor = "test-desc",
            vendorId = 0x2dc8,
            productId = 0x9020,
            sources = InputDevice.SOURCE_GAMEPAD,
            source = InputDevice.SOURCE_GAMEPAD,
            sourceName = "GAMEPAD",
            action = KeyEvent.ACTION_DOWN,
            actionName = "DOWN",
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            keyCodeName = "KEYCODE_BUTTON_A",
            scanCode = 304,
            repeatCount = 0,
            metaState = 0,
            flags = 0,
            isCandidateController = true,
            origin = EventOrigin.ACTIVITY,
            testingMode = TestingMode.S
        )

        val logActivity = eventActivity.toLogLine()
        assertTrue(logActivity.contains("[S]"))
        assertTrue(logActivity.contains("[ACTIVITY]"))
        assertTrue(logActivity.contains("DOWN"))
        assertTrue(logActivity.contains("KEYCODE_BUTTON_A"))
        assertTrue(logActivity.contains("keyCode=96"))
        assertTrue(logActivity.contains("scanCode=304"))
        assertTrue(logActivity.contains("source=GAMEPAD"))
        assertTrue(logActivity.contains("8BitDo Micro"))

        val eventAccessibility = eventActivity.copy(
            origin = EventOrigin.ACCESSIBILITY,
            testingMode = TestingMode.K,
            keyCode = KeyEvent.KEYCODE_C,
            keyCodeName = "KEYCODE_C"
        )
        val logAccessibility = eventAccessibility.toLogLine()
        assertTrue(logAccessibility.contains("[K]"))
        assertTrue(logAccessibility.contains("[ACCESSIBILITY]"))
        assertTrue(logAccessibility.contains("KEYCODE_C"))
    }

    @Test
    fun `diagnostics holder records events, counts DOWN actions by origin, and updates lastEvent`() {
        val eventActA = ControllerInputEvent(
            timestamp = 1000L,
            deviceId = 1,
            deviceName = "8BitDo Micro",
            descriptor = "desc",
            vendorId = 0x2dc8,
            productId = 0x9020,
            sources = InputDevice.SOURCE_GAMEPAD,
            source = InputDevice.SOURCE_GAMEPAD,
            sourceName = "GAMEPAD",
            action = KeyEvent.ACTION_DOWN,
            actionName = "DOWN",
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            keyCodeName = "KEYCODE_BUTTON_A",
            scanCode = 304,
            repeatCount = 0,
            metaState = 0,
            flags = 0,
            isCandidateController = true,
            origin = EventOrigin.ACTIVITY,
            testingMode = TestingMode.S
        )
        val eventAccA = eventActA.copy(
            origin = EventOrigin.ACCESSIBILITY,
            keyCode = KeyEvent.KEYCODE_C,
            keyCodeName = "KEYCODE_C"
        )

        val state1 = ControllerDiagnosticsState(
            events = listOf(eventActA, eventAccA),
            keyCounts = mapOf("ACTIVITY: KEYCODE_BUTTON_A" to 1, "ACCESSIBILITY: KEYCODE_C" to 1),
            lastEvent = eventAccA
        )

        assertEquals(2, state1.events.size)
        assertEquals(1, state1.keyCounts["ACTIVITY: KEYCODE_BUTTON_A"])
        assertEquals(1, state1.keyCounts["ACCESSIBILITY: KEYCODE_C"])
        assertNotNull(state1.lastEvent)
        assertEquals(KeyEvent.KEYCODE_C, state1.lastEvent?.keyCode)

        // Clear log
        ControllerDiagnosticsHolder.clear()
        val cleared = ControllerDiagnosticsHolder.state.value
        assertTrue(cleared.events.isEmpty())
        assertTrue(cleared.keyCounts.isEmpty())
        assertNull(cleared.lastEvent)
    }

    @Test
    fun `diagnostics holder mode switching, accessibility connection, and foreground lifecycle`() {
        ControllerDiagnosticsHolder.setTestingMode(TestingMode.D)
        assertEquals(TestingMode.D, ControllerDiagnosticsHolder.state.value.testingMode)

        ControllerDiagnosticsHolder.setTestingMode(TestingMode.K)
        assertEquals(TestingMode.K, ControllerDiagnosticsHolder.state.value.testingMode)

        ControllerDiagnosticsHolder.setForeground(false)
        assertFalse(ControllerDiagnosticsHolder.state.value.isForeground)

        ControllerDiagnosticsHolder.setForeground(true)
        assertTrue(ControllerDiagnosticsHolder.state.value.isForeground)

        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(true)
        assertTrue(ControllerDiagnosticsHolder.state.value.isAccessibilityServiceConnected)

        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(false)
        assertFalse(ControllerDiagnosticsHolder.state.value.isAccessibilityServiceConnected)
    }

    @Test
    fun `generate diagnostics summary includes mode, app state, accessibility service, devices, counts and events`() {
        val sampleState = ControllerDiagnosticsState(
            testingMode = TestingMode.K,
            isForeground = false,
            isAccessibilityServiceConnected = true,
            connectedDevices = listOf(
                InputDeviceInfo(
                    id = 10,
                    name = "8BitDo Micro K",
                    descriptor = "micro-k-desc",
                    vendorId = 0x2dc8,
                    productId = 0x9021,
                    sources = InputDevice.SOURCE_KEYBOARD,
                    isGamepad = false,
                    isJoystick = false,
                    isDpad = false,
                    isKeyboard = true,
                    isExternal = true
                )
            ),
            events = listOf(
                ControllerInputEvent(
                    timestamp = 1000L,
                    deviceId = 10,
                    deviceName = "8BitDo Micro K",
                    descriptor = "micro-k-desc",
                    vendorId = 0x2dc8,
                    productId = 0x9021,
                    sources = InputDevice.SOURCE_KEYBOARD,
                    source = InputDevice.SOURCE_KEYBOARD,
                    sourceName = "KEYBOARD",
                    action = KeyEvent.ACTION_DOWN,
                    actionName = "DOWN",
                    keyCode = KeyEvent.KEYCODE_J,
                    keyCodeName = "KEYCODE_J",
                    scanCode = 36,
                    repeatCount = 0,
                    metaState = 0,
                    flags = 0,
                    isCandidateController = true,
                    origin = EventOrigin.ACCESSIBILITY,
                    testingMode = TestingMode.K
                )
            ),
            keyCounts = mapOf("ACCESSIBILITY: KEYCODE_J" to 1)
        )

        val summary = ControllerInputDiagnostic.generateDiagnosticsSummary(sampleState)
        assertTrue(summary.contains("Testing Mode: K (Keyboard)"))
        assertTrue(summary.contains("App State: BACKGROUND"))
        assertTrue(summary.contains("Accessibility Service: CONNECTED"))
        assertTrue(summary.contains("8BitDo Micro K"))
        assertTrue(summary.contains("ACCESSIBILITY: KEYCODE_J: 1"))
        assertTrue(summary.contains("[K][ACCESSIBILITY] DOWN KEYCODE_J"))
    }
}
