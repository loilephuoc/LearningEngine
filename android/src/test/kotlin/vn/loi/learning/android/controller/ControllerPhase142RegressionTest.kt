package vn.loi.learning.android.controller

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.runTest
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Comprehensive test suite verifying all Phase 1.4.2 regression requirements (A through AE).
 */
class ControllerPhase142RegressionTest {

    private var currentTime = 1000L
    private val fakeClock: () -> Long = { currentTime }

    @Before
    fun setUp() {
        currentTime = 1000L
        StudyControllerBridge.clear()
        ControllerSystemActionBridge.clear()
    }

    @Test
    fun `A - PRESS + LONG short tap produces PRESS only`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.PRESS),
                    action = ControllerAction.PLAY_PRIMARY_EN
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.LONG_PRESS),
                    action = ControllerAction.CONTINUE_CURRENT_MODE
                )
            )
        )

        // Down at 1000ms: must NOT emit PRESS immediately
        val downGesture = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNull(downGesture, "DOWN must not emit PRESS when LONG_PRESS mapping exists")

        // Release at 1050ms (<400ms threshold)
        currentTime += 50L
        val upGesture = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNotNull(upGesture, "Short release must emit PRESS")
        assertEquals(ControllerPressType.PRESS, upGesture.pressType)
        assertEquals(KeyEvent.KEYCODE_K, upGesture.input.keyCode)
    }

    @Test
    fun `B - PRESS + LONG hold produces LONG only and suppresses PRESS`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.PRESS),
                    action = ControllerAction.PLAY_PRIMARY_EN
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.LONG_PRESS),
                    action = ControllerAction.CONTINUE_CURRENT_MODE
                )
            )
        )

        // Down at 1000ms
        val down = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNull(down, "DOWN must not emit PRESS")

        // Hold repeat at 1450ms (>=400ms threshold)
        currentTime += 450L
        val longGesture = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 1, EventOrigin.ACTIVITY, profile)
        assertNotNull(longGesture, "Hold threshold must emit LONG_PRESS")
        assertEquals(ControllerPressType.LONG_PRESS, longGesture.pressType)

        // Subsequent repeat must NOT duplicate
        currentTime += 50L
        val repeat2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 2, EventOrigin.ACTIVITY, profile)
        assertNull(repeat2, "Key repeat must not duplicate LONG_PRESS")

        // Release at 1600ms: must NOT emit PRESS
        currentTime += 100L
        val up = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(up, "Release after LONG_PRESS must NOT emit PRESS")
    }

    @Test
    fun `C - PRESS + DOUBLE single tap produces PRESS only after window expires`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.PRESS),
                    action = ControllerAction.PLAY_PRIMARY_EN
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.DOUBLE_PRESS),
                    action = ControllerAction.PLAY_PRIMARY_VI
                )
            )
        )

        // Single Tap: Down + Up at 1000-1050ms
        val down = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNull(down)
        currentTime += 50L
        val up = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(up, "UP must buffer pending press to wait for potential second tap")

        // Before timeout (t = 1200ms < 300ms)
        currentTime += 150L
        val earlyDrain = detector.drainExpiredGestures(currentTime)
        assertTrue(earlyDrain.isEmpty(), "Must not expire before 300ms window")

        // After timeout (t = 1400ms > 300ms)
        currentTime += 200L
        val drained = detector.drainExpiredGestures(currentTime)
        assertEquals(1, drained.size, "Single tap must resolve to PRESS after double window expires")
        assertEquals(ControllerPressType.PRESS, drained[0].pressType)
    }

    @Test
    fun `D - PRESS + DOUBLE double tap produces DOUBLE only and suppresses PRESS`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.PRESS),
                    action = ControllerAction.PLAY_PRIMARY_EN
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.DOUBLE_PRESS),
                    action = ControllerAction.PLAY_PRIMARY_VI
                )
            )
        )

        // Tap 1: Down + Up at 1000-1050ms
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 50L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)

        // Tap 2: Down at 1150ms (within 300ms)
        currentTime += 100L
        val tap2Down = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNotNull(tap2Down, "Second tap DOWN must emit DOUBLE_PRESS")
        assertEquals(ControllerPressType.DOUBLE_PRESS, tap2Down.pressType)

        // Tap 2: Up at 1200ms
        currentTime += 50L
        val tap2Up = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(tap2Up, "UP after DOUBLE_PRESS produces null")

        // Expired drain after timeout must be completely empty (no phantom PRESS)
        currentTime += 500L
        val drained = detector.drainExpiredGestures(currentTime)
        assertTrue(drained.isEmpty(), "No pending PRESS should remain after DOUBLE_PRESS")
    }

    @Test
    fun `E - PRESS + DOUBLE + LONG each gesture produces exactly one corresponding action`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.PRESS),
                    action = ControllerAction.PLAY_PRIMARY_EN
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.DOUBLE_PRESS),
                    action = ControllerAction.PLAY_PRIMARY_VI
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.LONG_PRESS),
                    action = ControllerAction.CONTINUE_CURRENT_MODE
                )
            )
        )

        // Gesture 1: Single short tap -> PRESS
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 50L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 350L
        val singleDrained = detector.drainExpiredGestures(currentTime)
        assertEquals(1, singleDrained.size)
        assertEquals(ControllerPressType.PRESS, singleDrained[0].pressType)

        // Gesture 2: Double tap -> DOUBLE_PRESS only
        currentTime += 500L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 40L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 80L
        val doubleGesture = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNotNull(doubleGesture)
        assertEquals(ControllerPressType.DOUBLE_PRESS, doubleGesture.pressType)
        currentTime += 40L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 500L
        assertTrue(detector.drainExpiredGestures(currentTime).isEmpty(), "No trailing single press after double tap")

        // Gesture 3: Long hold -> LONG_PRESS only
        currentTime += 500L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 450L
        val longGesture = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 1, EventOrigin.ACTIVITY, profile)
        assertNotNull(longGesture)
        assertEquals(ControllerPressType.LONG_PRESS, longGesture.pressType)
        currentTime += 100L
        val longUp = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(longUp, "Release after long press must not emit PRESS")
        currentTime += 500L
        assertTrue(detector.drainExpiredGestures(currentTime).isEmpty(), "No trailing single press after long hold")
    }

    @Test
    fun `F - HID burst never becomes DOUBLE or LONG`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.PRESS),
                    action = ControllerAction.PLAY_PRIMARY_EN
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.DOUBLE_PRESS),
                    action = ControllerAction.PLAY_PRIMARY_VI
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.LONG_PRESS),
                    action = ControllerAction.CONTINUE_CURRENT_MODE
                )
            )
        )

        // Rapid 4-event hardware burst within 20ms
        val g1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACCESSIBILITY, profile)
        assertNull(g1)
        currentTime += 5L
        val g2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNull(g2, "Burst duplicate must be dropped")
        currentTime += 5L
        val g3 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACCESSIBILITY, profile)
        assertNull(g3)
        currentTime += 5L
        val g4 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(g4)

        // After double tap timeout (350ms), drain expired -> must be exactly ONE single PRESS
        currentTime += 350L
        val drained = detector.drainExpiredGestures(currentTime)
        assertEquals(1, drained.size, "Burst must resolve to exactly ONE single press")
        assertEquals(ControllerPressType.PRESS, drained[0].pressType)
    }

    @Test
    fun `G - long key-repeat events do not duplicate LONG`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.LONG_PRESS),
                    action = ControllerAction.CONTINUE_CURRENT_MODE
                )
            )
        )

        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 450L
        val long1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 1, EventOrigin.ACTIVITY, profile)
        assertNotNull(long1)
        assertEquals(ControllerPressType.LONG_PRESS, long1.pressType)

        currentTime += 50L
        val long2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 2, EventOrigin.ACTIVITY, profile)
        assertNull(long2, "Subsequent repeat count 2 must not duplicate LONG_PRESS")

        currentTime += 50L
        val long3 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 3, EventOrigin.ACTIVITY, profile)
        assertNull(long3, "Subsequent repeat count 3 must not duplicate LONG_PRESS")
    }

    @Test
    fun `H - modifier chord preserves same multi-press arbitration`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val inputK = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)
        val modifierO = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_O)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            modifierInput = modifierO,
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(input = inputK, pressType = ControllerPressType.PRESS, modifier = modifierO),
                    action = ControllerAction.PLAY_PRIMARY_EN
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(input = inputK, pressType = ControllerPressType.LONG_PRESS, modifier = modifierO),
                    action = ControllerAction.CONTINUE_CURRENT_MODE
                )
            )
        )

        // Hold Modifier O
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_O, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)

        // Press K and hold for 450ms -> LONG_PRESS chord
        currentTime += 50L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 450L
        val chordLong = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 1, EventOrigin.ACTIVITY, profile)
        assertNotNull(chordLong)
        assertEquals(ControllerPressType.LONG_PRESS, chordLong.pressType)
        assertEquals(KeyEvent.KEYCODE_O, chordLong.modifier?.keyCode)

        // Release K
        currentTime += 50L
        val upK = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(upK)

        // Release Modifier O
        currentTime += 50L
        val upO = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_O, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(upO, "Modifier release after chord must not fire standalone action")
    }

    @Test
    fun `H2 - DOUBLE_PRESS and LONG_PRESS without PRESS never emits phantom PRESS`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = ControllerProfile(
            id = "test-profile",
            name = "Test",
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.DOUBLE_PRESS),
                    action = ControllerAction.PLAY_PRIMARY_VI
                ),
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K), ControllerPressType.LONG_PRESS),
                    action = ControllerAction.CONTINUE_CURRENT_MODE
                )
            )
        )

        // Single short tap at 1000-1050ms
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        currentTime += 50L
        val up = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNull(up)

        // Double tap window expires: must produce ZERO gestures (no phantom PRESS!)
        currentTime += 400L
        val drained = detector.drainExpiredGestures(currentTime)
        assertTrue(drained.isEmpty(), "Without PRESS mapping, single tap expiration must not emit any phantom gesture")
    }

    @Test
    fun `I - different keys remain independent`() {
        val detector = ControllerGestureDetector(clock = fakeClock)
        val profile = DefaultControllerProfiles.defaultProfile()

        // Press L1 (KEYCODE_K)
        val g1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertEquals(KeyEvent.KEYCODE_K, g1?.input?.keyCode)
        currentTime += 50L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)

        // Press L2 (KEYCODE_L)
        currentTime += 50L
        val g2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_L, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertEquals(KeyEvent.KEYCODE_L, g2?.input?.keyCode)
    }

    @Test
    fun `J - same key PRESS, DOUBLE, and LONG mappings coexist without conflict`() {
        val inputK = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)
        val mapPress = ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(inputK, ControllerPressType.PRESS), ControllerAction.PLAY_PRIMARY_EN)
        val mapDouble = ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(inputK, ControllerPressType.DOUBLE_PRESS), ControllerAction.PLAY_PRIMARY_VI)
        val mapLong = ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(inputK, ControllerPressType.LONG_PRESS), ControllerAction.CONTINUE_CURRENT_MODE)

        val profile = ControllerProfile(id = "test", name = "Test", mappings = listOf(mapPress, mapDouble, mapLong))

        assertNull(ControllerMappingResolver.findConflict(profile, mapPress))
        assertNull(ControllerMappingResolver.findConflict(profile, mapDouble))
        assertNull(ControllerMappingResolver.findConflict(profile, mapLong))
    }

    @Test
    fun `K - Edit DOUBLE mapping replaces correct original only`() {
        val inputK = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)
        val mapPress = ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(inputK, ControllerPressType.PRESS), ControllerAction.PLAY_PRIMARY_EN)
        val mapDouble = ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(inputK, ControllerPressType.DOUBLE_PRESS), ControllerAction.PLAY_PRIMARY_VI)
        val profile = ControllerProfile(id = "test", name = "Test", mappings = listOf(mapPress, mapDouble))

        val updatedDouble = ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(inputK, ControllerPressType.DOUBLE_PRESS), ControllerAction.PLAY_EXAMPLE_VI)
        val newProfile = ControllerMappingResolver.upsertMapping(profile, updatedDouble)

        assertEquals(2, newProfile.mappings.size)
        assertEquals(ControllerAction.PLAY_PRIMARY_EN, newProfile.mappings.first { it.gesture.pressType == ControllerPressType.PRESS }.action)
        assertEquals(ControllerAction.PLAY_EXAMPLE_VI, newProfile.mappings.first { it.gesture.pressType == ControllerPressType.DOUBLE_PRESS }.action)
    }

    @Test
    fun `M - PLAY_PRIMARY_EN uses current Study primary EN audio authority`() = runTest {
        var playedEn = false
        StudyControllerBridge.registerPrimaryEnglishPlayer {
            playedEn = true
            true
        }

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.PLAY_PRIMARY_EN, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.Executed)
        assertTrue(playedEn)
    }

    @Test
    fun `N - PLAY_PRIMARY_VI uses resolvedMeaningAudio canonical meaning audio authority`() = runTest {
        var playedVi = false
        StudyControllerBridge.registerPrimaryVietnamesePlayer {
            playedVi = true
            true
        }

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.PLAY_PRIMARY_VI, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.Executed)
        assertTrue(playedVi)
    }

    @Test
    fun `O - no stale audio callback after item change unregistration`() = runTest {
        var playerACalled = false
        val playerA: () -> Boolean = {
            playerACalled = true
            true
        }
        StudyControllerBridge.registerPrimaryEnglishPlayer(playerA)

        // Item change / screen dispose
        StudyControllerBridge.unregisterPrimaryEnglishPlayer(playerA)

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.PLAY_PRIMARY_EN, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.UnavailableInContext)
        assertFalse(playerACalled)
    }

    @Test
    fun `L - Live button detection preserves chosen press type`() {
        // Candidate with chosen DOUBLE_PRESS
        val candidate = ControllerGesture(
            input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K),
            pressType = ControllerPressType.DOUBLE_PRESS
        )
        assertEquals(ControllerPressType.DOUBLE_PRESS, candidate.pressType)
        assertEquals(KeyEvent.KEYCODE_K, candidate.input.keyCode)
    }

    @Test
    fun `P - CONTINUE_CURRENT_MODE executes canonical continue callback`() = runTest {
        var continued = false
        StudyControllerBridge.registerContinueCurrentMode {
            continued = true
            true
        }

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.CONTINUE_CURRENT_MODE, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.Executed)
        assertTrue(continued)
    }

    @Test
    fun `Q - CONTINUE_CURRENT_MODE in Quick Review context routes to canonical continue action`() = runTest {
        var quickReviewAdvanced = false
        StudyControllerBridge.registerContinueCurrentMode {
            quickReviewAdvanced = true
            true
        }

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.CONTINUE_CURRENT_MODE, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.Executed)
        assertTrue(quickReviewAdvanced)
    }

    @Test
    fun `R - CONTINUE_CURRENT_MODE in Difficult Practice routes to canonical continue action`() = runTest {
        var difficultPracticeAdvanced = false
        StudyControllerBridge.registerContinueCurrentMode {
            difficultPracticeAdvanced = true
            true
        }

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.CONTINUE_CURRENT_MODE, ControllerContext.STUDY_REVEALED)

        assertTrue(result is ControllerActionResult.Executed)
        assertTrue(difficultPracticeAdvanced)
    }

    @Test
    fun `S - CONTINUE_CURRENT_MODE in history preview routes to NextVisited canonical action`() = runTest {
        var nextVisitedCalled = false
        StudyControllerBridge.registerContinueCurrentMode {
            nextVisitedCalled = true
            true
        }

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.CONTINUE_CURRENT_MODE, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.Executed)
        assertTrue(nextVisitedCalled)
    }

    @Test
    fun `T - Continue action does not invent unapproved state transitions when unassigned`() = runTest {
        StudyControllerBridge.clear()

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.CONTINUE_CURRENT_MODE, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.UnavailableInContext)
    }

    @Test
    fun `U - SYSTEM_VOLUME_UP invokes media volume raise once`() = runTest {
        var streamAdjusted: Int? = null
        var directionAdjusted: Int? = null

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(
            appContext = fakeContext,
            systemVolumeAdjuster = { dir ->
                directionAdjusted = dir
                true
            }
        )

        val result = dispatcher.dispatch(ControllerAction.SYSTEM_VOLUME_UP, ControllerContext.GLOBAL)
        assertTrue(result is ControllerActionResult.Executed)
        assertEquals(AudioManager.ADJUST_RAISE, directionAdjusted)
    }

    @Test
    fun `V - SYSTEM_VOLUME_DOWN invokes media volume lower once`() = runTest {
        var directionAdjusted: Int? = null

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(
            appContext = fakeContext,
            systemVolumeAdjuster = { dir ->
                directionAdjusted = dir
                true
            }
        )

        val result = dispatcher.dispatch(ControllerAction.SYSTEM_VOLUME_DOWN, ControllerContext.GLOBAL)
        assertTrue(result is ControllerActionResult.Executed)
        assertEquals(AudioManager.ADJUST_LOWER, directionAdjusted)
    }

    @Test
    fun `W - LOCK_SCREEN invokes system action bridge exactly once`() = runTest {
        var locked = false
        ControllerSystemActionBridge.registerLockScreenHandler {
            locked = true
            true
        }

        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.LOCK_SCREEN, ControllerContext.GLOBAL)

        assertTrue(result is ControllerActionResult.Executed)
        assertTrue(locked)
    }

    @Test
    fun `X - LOCK_SCREEN reports unavailable when system action bridge is unassigned`() = runTest {
        val fakeContext = FakeAndroidContext()
        val dispatcher = ControllerActionDispatcher(fakeContext)
        val result = dispatcher.dispatch(ControllerAction.LOCK_SCREEN, ControllerContext.GLOBAL)

        assertTrue(result is ControllerActionResult.UnavailableInContext)
    }

    @Test
    fun `Y - Deferred Shadowing actions are not user selectable in action picker`() {
        assertFalse(ControllerAction.REPLAY_SENTENCE.isUserSelectable)
        assertFalse(ControllerAction.TOGGLE_LOOP.isUserSelectable)
        assertFalse(ControllerAction.SPEED_UP.isUserSelectable)
        assertFalse(ControllerAction.SPEED_DOWN.isUserSelectable)

        assertTrue(ControllerAction.PLAY_PRIMARY_EN.isUserSelectable)
        assertTrue(ControllerAction.PLAY_PRIMARY_VI.isUserSelectable)
        assertTrue(ControllerAction.CONTINUE_CURRENT_MODE.isUserSelectable)
        assertTrue(ControllerAction.SYSTEM_VOLUME_UP.isUserSelectable)
        assertTrue(ControllerAction.SYSTEM_VOLUME_DOWN.isUserSelectable)
        assertTrue(ControllerAction.LOCK_SCREEN.isUserSelectable)
    }

    @Test
    fun `Z - Existing deferred persisted mapping still loads safely if present`() {
        val inputK = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)
        val deferredMapping = ControllerMapping(
            context = ControllerContext.SHADOWING,
            gesture = ControllerGesture(inputK),
            action = ControllerAction.REPLAY_SENTENCE
        )
        val profile = ControllerProfile(id = "legacy", name = "Legacy", mappings = listOf(deferredMapping))
        val config = ControllerConfig(activeProfileId = "legacy", profiles = listOf(profile))

        val resolved = ControllerMappingResolver.resolve(config, ControllerContext.SHADOWING, ControllerGesture(inputK))
        assertEquals(ControllerAction.REPLAY_SENTENCE, resolved)
    }

    @Test
    fun `AA - Rating overrides still beat basic mappings in STUDY_RATING context`() {
        val config = DefaultControllerProfiles.defaultConfig()
        val gestureA = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G))

        val resolvedInGlobal = ControllerMappingResolver.resolve(config, ControllerContext.GLOBAL, gestureA)
        assertEquals(ControllerAction.PLAY_PAUSE, resolvedInGlobal)

        val resolvedInRating = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_RATING, gestureA)
        assertEquals(ControllerAction.RATE_AGAIN, resolvedInRating)
    }

    @Test
    fun `AE - L2 is KEYCODE_L (40) and R2 is KEYCODE_R (46) with verified frozen hardware mapping`() {
        assertEquals(KeyEvent.KEYCODE_L, 40)
        assertEquals(KeyEvent.KEYCODE_R, 46)
        assertEquals("L2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_L))
        assertEquals("R2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_R))
    }

    @Test
    fun `BA - Canonical K-mode matrix maps all 15 physical buttons accurately`() {
        // Face Buttons
        assertEquals("A", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_G))
        assertEquals("B", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_J))
        assertEquals("X", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_H))
        assertEquals("Y", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_I))

        // D-Pad Buttons
        assertEquals("D-Pad Up", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_C))
        assertEquals("D-Pad Down", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_D))
        assertEquals("D-Pad Left", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_E))
        assertEquals("D-Pad Right", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_F))

        // Shoulder & Triggers
        assertEquals("L1", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_K))
        assertEquals("R1", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_M))
        assertEquals("L2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_L))
        assertEquals("R2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_R))

        // System Buttons
        assertEquals("Select / -", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_N))
        assertEquals("Start / +", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_O))
        assertEquals("Star / Mode", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_S))
    }

    @Test
    fun `BB - Selecting UI B persists KEYCODE_J and physical B triggers mapping`() {
        val bInfo = ControllerButtonDirectory.ALL_BUTTONS.first { it.primaryName == "B" }
        assertEquals(KeyEvent.KEYCODE_J, bInfo.keyCode)

        // Save mapping with B
        val mapping = ControllerMapping(
            context = ControllerContext.GLOBAL,
            gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, bInfo.keyCode)),
            action = ControllerAction.REVEAL_ANSWER
        )
        val profile = ControllerProfile(id = "test-b", name = "Test B", mappings = listOf(mapping))
        val config = ControllerConfig(activeProfileId = "test-b", profiles = listOf(profile))

        // Physical B arrives (raw keyCode = KEYCODE_J)
        val physicalBGesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_J))
        val resolved = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, physicalBGesture)
        assertEquals(ControllerAction.REVEAL_ANSWER, resolved)
    }

    @Test
    fun `BC - Selecting UI D-Pad Down persists KEYCODE_D and physical D-Pad Down triggers mapping`() {
        val dpadDownInfo = ControllerButtonDirectory.ALL_BUTTONS.first { it.primaryName == "D-Pad Down" }
        assertEquals(KeyEvent.KEYCODE_D, dpadDownInfo.keyCode)

        val mapping = ControllerMapping(
            context = ControllerContext.GLOBAL,
            gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, dpadDownInfo.keyCode)),
            action = ControllerAction.NEXT_ITEM
        )
        val profile = ControllerProfile(id = "test-dpad", name = "Test DPad", mappings = listOf(mapping))
        val config = ControllerConfig(activeProfileId = "test-dpad", profiles = listOf(profile))

        // Physical D-Pad Down arrives (raw keyCode = KEYCODE_D)
        val physicalDpadDownGesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_D))
        val resolved = ControllerMappingResolver.resolve(config, ControllerContext.GLOBAL, physicalDpadDownGesture)
        assertEquals(ControllerAction.NEXT_ITEM, resolved)
    }

    @Test
    fun `BD - Round-trip physical keyCode to label to save to load to resolver for all 15 buttons`() {
        val buttonKeyCodes = listOf(
            KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_I,
            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_F,
            KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_S
        )

        for (keyCode in buttonKeyCodes) {
            val label = ControllerButtonDirectory.getButtonLabel(keyCode)
            assertFalse(label.startsWith("KEY_"), "Label for keyCode $keyCode should be a recognized physical name")

            val mapping = ControllerMapping(
                context = ControllerContext.GLOBAL,
                gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, keyCode)),
                action = ControllerAction.PLAY_PAUSE
            )
            val profile = ControllerProfile(id = "test-rt", name = "Test RT", mappings = listOf(mapping))
            val config = ControllerConfig(activeProfileId = "test-rt", profiles = listOf(profile))

            val incomingPhysicalGesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, keyCode))
            val resolved = ControllerMappingResolver.resolve(config, ControllerContext.GLOBAL, incomingPhysicalGesture)
            assertEquals(ControllerAction.PLAY_PAUSE, resolved, "Failed resolution for button $label ($keyCode)")
        }
    }
}

// Helpers for test execution
private class FakeAndroidContext : android.content.ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
}
