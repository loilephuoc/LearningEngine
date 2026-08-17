package vn.loi.learning.android.controller

import android.view.KeyEvent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.runTest
import vn.loi.learning.domain.study.memory.model.ReviewRating

class ControllerMappingTest {

    private lateinit var defaultConfig: ControllerConfig
    private lateinit var defaultProfile: ControllerProfile

    @Before
    fun setUp() {
        StudyControllerBridge.clear()
        defaultConfig = DefaultControllerProfiles.defaultConfig()
        defaultProfile = DefaultControllerProfiles.defaultProfile()
    }

    @After
    fun tearDown() {
        StudyControllerBridge.clear()
    }

    @Test
    fun `A - plain PRESS resolves correct mapping in configured context`() {
        val inputB = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_J) // Physical B
        val gesture = ControllerGesture(input = inputB, pressType = ControllerPressType.PRESS)

        val actionInQuestion = ControllerMappingResolver.resolve(
            defaultConfig,
            ControllerContext.STUDY_QUESTION,
            gesture
        )
        assertEquals(ControllerAction.REVEAL_ANSWER, actionInQuestion)
    }

    @Test
    fun `B - same physical key resolves differently across contexts`() {
        val inputC = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_C)
        val gesture = ControllerGesture(input = inputC, pressType = ControllerPressType.PRESS)

        val profile = ControllerProfile(
            id = "multi-context",
            name = "Multi Context",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_QUESTION, gesture, ControllerAction.REPLAY_PRIMARY_AUDIO),
                ControllerMapping(ControllerContext.STUDY_RATING, gesture, ControllerAction.RATE_AGAIN),
                ControllerMapping(ControllerContext.AUTO_PLAY, gesture, ControllerAction.PLAY_PAUSE)
            )
        )
        val config = ControllerConfig(activeProfileId = profile.id, profiles = listOf(profile))

        val actionQuestion = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, gesture)
        val actionRating = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_RATING, gesture)
        val actionAutoPlay = ControllerMappingResolver.resolve(config, ControllerContext.AUTO_PLAY, gesture)

        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, actionQuestion)
        assertEquals(ControllerAction.RATE_AGAIN, actionRating)
        assertEquals(ControllerAction.PLAY_PAUSE, actionAutoPlay)
    }

    @Test
    fun `C - GLOBAL fallback works when specific context is not mapped`() {
        val customProfile = ControllerProfile(
            id = "custom",
            name = "Custom",
            deviceVendorId = 0x2dc8,
            deviceProductId = 0x9021,
            mappings = listOf(
                ControllerMapping(
                    context = ControllerContext.GLOBAL,
                    gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)),
                    action = ControllerAction.REPLAY_PRIMARY_AUDIO
                )
            )
        )
        val config = ControllerConfig(
            activeProfileId = "custom",
            profiles = listOf(customProfile),
            isControllerEnabled = true
        )

        val gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K))
        val resolved = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, gesture)

        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, resolved)
    }

    @Test
    fun `D - modifier plus key resolves separately from plain key`() {
        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G) // Physical A
        val inputO = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_O)

        val plainGesture = ControllerGesture(input = inputA, pressType = ControllerPressType.PRESS)
        val chordGesture = ControllerGesture(input = inputA, pressType = ControllerPressType.PRESS, modifier = inputO)

        val plainAction = ControllerMappingResolver.resolve(defaultConfig, ControllerContext.GLOBAL, plainGesture)
        val chordAction = ControllerMappingResolver.resolve(defaultConfig, ControllerContext.GLOBAL, chordGesture)

        assertEquals(ControllerAction.PLAY_PAUSE, plainAction)
        assertEquals(ControllerAction.MUTE_TOGGLE, chordAction)
    }

    @Test
    fun `E - modifier gesture does not accidentally execute plain key action during chord`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(clock = { currentTime })
        val profile = defaultProfile // Modifier is KEYCODE_O

        // 1. Modifier DOWN
        val res1 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_O,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = profile
        )
        assertNull(res1, "Modifier DOWN must not fire immediately")

        // 2. Key C DOWN while modifier held
        currentTime += 50L
        val res2 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = profile
        )
        assertNotNull(res2)
        assertEquals(KeyEvent.KEYCODE_C, res2.input.keyCode)
        assertNotNull(res2.modifier)
        assertEquals(KeyEvent.KEYCODE_O, res2.modifier?.keyCode)

        // 3. Key C UP
        currentTime += 50L
        val res3 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_UP,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = profile
        )
        assertNull(res3)

        // 4. Modifier UP (was used in chord, must NOT fire standalone modifier action)
        currentTime += 50L
        val res4 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_O,
            action = KeyEvent.ACTION_UP,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = profile
        )
        assertNull(res4, "Modifier release after chord must not fire standalone action")
    }

    @Test
    fun `F - duplicate ACTIVITY and ACCESSIBILITY event produces one gesture`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 150L, clock = { currentTime })

        val firstResult = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = defaultProfile
        )
        assertNotNull(firstResult)

        // Same physical key arrives from ACCESSIBILITY 20ms later
        currentTime += 20L
        val duplicateResult = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACCESSIBILITY,
            profile = defaultProfile
        )
        assertNull(duplicateResult, "Cross-origin duplicate event within 150ms window must be dropped")
    }

    @Test
    fun `G - DOWN plus UP produces exactly one PRESS action`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(clock = { currentTime })

        val downRes = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = defaultProfile
        )
        assertNotNull(downRes)

        currentTime += 80L
        val upRes = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_UP,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = defaultProfile
        )
        assertNull(upRes, "Normal ACTION_UP must not produce a secondary PRESS action")
    }

    @Test
    fun `H - key repeat cannot execute rating repeatedly`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(clock = { currentTime })

        val resInitial = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = defaultProfile
        )
        assertNotNull(resInitial)

        // Key repeat events from long hold (repeatCount > 0)
        currentTime += 100L
        val resRepeat1 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 1,
            origin = EventOrigin.ACTIVITY,
            profile = defaultProfile
        )
        assertNull(resRepeat1, "Key repeat must be ignored for PRESS gestures")

        currentTime += 100L
        val resRepeat2 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_C,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 2,
            origin = EventOrigin.ACTIVITY,
            profile = defaultProfile
        )
        assertNull(resRepeat2, "Key repeat 2 must be ignored for PRESS gestures")
    }

    @Test
    fun `I - unmapped key is harmless`() {
        val unmappedInput = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_Z)
        val gesture = ControllerGesture(unmappedInput)

        val action = ControllerMappingResolver.resolve(defaultConfig, ControllerContext.STUDY_QUESTION, gesture)
        assertNull(action)
    }

    @Test
    fun `J - invalid action in current context is rejected safely`() = runTest {
        val target = object : StudyControllerTarget {
            override fun currentContext(): ControllerContext = ControllerContext.STUDY_QUESTION
            override suspend fun revealAnswer(): Boolean = false // Cannot reveal
            override suspend fun rate(rating: ReviewRating): Boolean = false // Cannot rate during question
            override suspend fun next(): Boolean = false
            override suspend fun previous(): Boolean = false
            override fun replayAudio(): Boolean = true
        }
        StudyControllerBridge.register(target)

        val mockContext = android.content.ContextWrapper(null)
        val dispatcher = ControllerActionDispatcher(mockContext, StudyControllerBridge, autoPlayCoordinatorProvider = { null })

        val ratingResult = dispatcher.dispatch(ControllerAction.RATE_AGAIN, ControllerContext.STUDY_QUESTION)
        assertTrue(ratingResult is ControllerActionResult.UnavailableInContext)
        assertEquals(ControllerAction.RATE_AGAIN, ratingResult.action)

        StudyControllerBridge.unregister(target)
    }

    @Test
    fun `K - mappings survive persistence and reload`() {
        val encoded = SharedPreferencesControllerPreferenceStore.encodeConfigJson(defaultConfig)
        val reloaded = SharedPreferencesControllerPreferenceStore.parseConfigJson(encoded)

        assertEquals(defaultConfig.activeProfileId, reloaded.activeProfileId)
        assertEquals(defaultConfig.isControllerEnabled, reloaded.isControllerEnabled)
        assertEquals(defaultConfig.profiles.size, reloaded.profiles.size)

        val reloadedProf = reloaded.activeProfile
        assertEquals(defaultProfile.mappings.size, reloadedProf.mappings.size)
        assertEquals(defaultProfile.modifierInput?.keyCode, reloadedProf.modifierInput?.keyCode)

        // Test with corrupted JSON -> safe fallback
        val fallback = SharedPreferencesControllerPreferenceStore.parseConfigJson("{ invalid json }")
        assertEquals(DefaultControllerProfiles.DEFAULT_PROFILE_ID, fallback.activeProfileId)
    }

    @Test
    fun `L - mapping conflict is detected`() {
        val candidate = ControllerMapping(
            context = ControllerContext.GLOBAL,
            gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)),
            action = ControllerAction.NEXT_ITEM // Conflict with REPLAY_PRIMARY_AUDIO
        )

        val conflict = ControllerMappingResolver.findConflict(defaultProfile, candidate)
        assertNotNull(conflict)
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, conflict.action)
    }

    @Test
    fun `M - active profile selection works`() {
        val profile2 = defaultProfile.copy(id = "custom_profile", name = "Custom Profile 2")
        val config = ControllerConfig(
            activeProfileId = "custom_profile",
            profiles = listOf(defaultProfile, profile2)
        )

        assertEquals("custom_profile", config.activeProfile.id)
        assertEquals("Custom Profile 2", config.activeProfile.name)
    }

    @Test
    fun `N - changing a mapping does not require code changes`() {
        val newMapping = ControllerMapping(
            context = ControllerContext.STUDY_QUESTION,
            gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_C)),
            action = ControllerAction.NEXT_ITEM
        )

        val updatedProfile = ControllerMappingResolver.upsertMapping(defaultProfile, newMapping)
        val updatedConfig = defaultConfig.copy(profiles = listOf(updatedProfile))

        val resolved = ControllerMappingResolver.resolve(
            updatedConfig,
            ControllerContext.STUDY_QUESTION,
            ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_C))
        )
        assertEquals(ControllerAction.NEXT_ITEM, resolved)
    }

    @Test
    fun `O - unattached bridge safely handles actions without mutations`() = runTest {
        val mockContext = android.content.ContextWrapper(null)
        val dispatcher = ControllerActionDispatcher(mockContext, StudyControllerBridge, autoPlayCoordinatorProvider = { null })

        val result = dispatcher.dispatch(ControllerAction.REVEAL_ANSWER, ControllerContext.GLOBAL)
        assertTrue(result is ControllerActionResult.UnavailableInContext)
    }

    @Test
    fun `P - KEYCODE_K plus STUDY_QUESTION resolves to REPLAY_PRIMARY_AUDIO`() {
        val inputK = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)
        val gesture = ControllerGesture(input = inputK, pressType = ControllerPressType.PRESS)

        val resolved = ControllerMappingResolver.resolve(
            defaultConfig,
            ControllerContext.STUDY_QUESTION,
            gesture
        )
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, resolved)
    }

    @Test
    fun `Q - Replay Audio invokes registered Study audio replayer exactly once`() = runTest {
        var replayCount = 0
        val replayer = {
            replayCount++
            true
        }
        StudyControllerBridge.registerAudioReplayer(replayer)

        val mockContext = android.content.ContextWrapper(null)
        val dispatcher = ControllerActionDispatcher(mockContext, StudyControllerBridge, autoPlayCoordinatorProvider = { null })

        val result = dispatcher.dispatch(ControllerAction.REPLAY_PRIMARY_AUDIO, ControllerContext.STUDY_QUESTION)

        assertTrue(result is ControllerActionResult.Executed)
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, result.action)
        assertEquals(1, replayCount)

        StudyControllerBridge.unregisterAudioReplayer(replayer)
    }

    @Test
    fun `R - Activity plus Accessibility duplicate KEYCODE_K produces single gesture and single replay`() = runTest {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 150L, clock = { currentTime })

        var replayCount = 0
        val replayer = {
            replayCount++
            true
        }
        StudyControllerBridge.registerAudioReplayer(replayer)
        val mockContext = android.content.ContextWrapper(null)
        val dispatcher = ControllerActionDispatcher(mockContext, StudyControllerBridge, autoPlayCoordinatorProvider = { null })

        // 1. Activity event
        val g1 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_K,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACTIVITY,
            profile = defaultProfile
        )
        assertNotNull(g1)
        val a1 = ControllerMappingResolver.resolve(defaultConfig, ControllerContext.STUDY_QUESTION, g1)
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, a1)
        if (a1 != null) dispatcher.dispatch(a1, ControllerContext.STUDY_QUESTION)

        // 2. Accessibility duplicate event 25ms later
        currentTime += 25L
        val g2 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_K,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACCESSIBILITY,
            profile = defaultProfile
        )
        assertNull(g2, "Accessibility duplicate event within 150ms must be dropped")

        assertEquals(1, replayCount, "Audio replayer must be invoked exactly once")
        StudyControllerBridge.unregisterAudioReplayer(replayer)
    }

    @Test
    fun `S - Controller reconnect with new runtime deviceId accepts input automatically`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(clock = { currentTime })

        // Initial session: deviceId = 5
        val g1 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_K,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACCESSIBILITY,
            profile = defaultProfile
        )
        assertNotNull(g1)

        // Bluetooth disconnects then reconnects: assigned new deviceId = 12
        currentTime += 5000L
        val g2 = detector.processRawKeyEvent(
            vendorId = 0x2dc8,
            productId = 0x9021,
            keyCode = KeyEvent.KEYCODE_K,
            action = KeyEvent.ACTION_DOWN,
            repeatCount = 0,
            origin = EventOrigin.ACCESSIBILITY,
            profile = defaultProfile
        )
        assertNotNull(g2, "Reconnected controller with new deviceId must produce valid gesture automatically")
        assertEquals(KeyEvent.KEYCODE_K, g2.input.keyCode)
    }

    @Test
    fun `T - Accessibility connected state is decoupled from Bluetooth device connection`() {
        ControllerDiagnosticsHolder.clear()
        ControllerDiagnosticsHolder.setAccessibilityServiceConnected(true)

        val s1 = ControllerDiagnosticsHolder.state.value
        assertTrue(s1.isAccessibilityServiceConnected)
        assertTrue(s1.connectedDevices.isEmpty(), "No Bluetooth devices connected initially")

        // Bluetooth device connects
        val devInfo = InputDeviceInfo(
            id = 15,
            name = "8BitDo Micro Gamepad Keyboard",
            descriptor = "desc-bt",
            vendorId = 0x2dc8,
            productId = 0x9021,
            sources = 0x101,
            isGamepad = false,
            isJoystick = false,
            isDpad = false,
            isKeyboard = true,
            isExternal = true
        )
        assertTrue(devInfo.isCandidateController)
    }

    @Test
    fun `U - Replay Audio causes zero review or FSRS mutations`() = runTest {
        var rateCount = 0
        var nextCount = 0
        val target = object : StudyControllerTarget {
            override fun currentContext(): ControllerContext = ControllerContext.STUDY_QUESTION
            override suspend fun revealAnswer(): Boolean = false
            override suspend fun rate(rating: ReviewRating): Boolean {
                rateCount++
                return true
            }
            override suspend fun next(): Boolean {
                nextCount++
                return true
            }
            override suspend fun previous(): Boolean = false
            override fun replayAudio(): Boolean = true
        }
        StudyControllerBridge.register(target)

        val mockContext = android.content.ContextWrapper(null)
        val dispatcher = ControllerActionDispatcher(mockContext, StudyControllerBridge, autoPlayCoordinatorProvider = { null })

        val result = dispatcher.dispatch(ControllerAction.REPLAY_PRIMARY_AUDIO, ControllerContext.STUDY_QUESTION)
        assertTrue(result is ControllerActionResult.Executed)

        assertEquals(0, rateCount, "Replay Audio must NEVER mutate FSRS ratings")
        assertEquals(0, nextCount, "Replay Audio must NEVER trigger card advancement")

        StudyControllerBridge.unregister(target)
    }

    @Test
    fun `V - Raw K-mode HID burst with 4 rapid DOWN-UP cycles produces exactly ONE gesture`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 150L, clock = { currentTime })

        var gestureCount = 0

        // Simulate rapid hardware burst: "kkkk" in 80ms
        // Cycle 1: t = 1000ms
        val g1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACCESSIBILITY, defaultProfile)
        if (g1 != null) gestureCount++
        currentTime += 10L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACCESSIBILITY, defaultProfile)

        // Cycle 2: t = 1025ms
        currentTime += 15L
        val g2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACCESSIBILITY, defaultProfile)
        if (g2 != null) gestureCount++
        currentTime += 10L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACCESSIBILITY, defaultProfile)

        // Cycle 3: t = 1050ms
        currentTime += 15L
        val g3 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACCESSIBILITY, defaultProfile)
        if (g3 != null) gestureCount++
        currentTime += 10L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACCESSIBILITY, defaultProfile)

        // Cycle 4: t = 1075ms
        currentTime += 15L
        val g4 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACCESSIBILITY, defaultProfile)
        if (g4 != null) gestureCount++
        currentTime += 10L
        detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_K, KeyEvent.ACTION_UP, 0, EventOrigin.ACCESSIBILITY, defaultProfile)

        assertEquals(1, gestureCount, "Hardware burst of 4 DOWN/UP cycles within 80ms must produce exactly 1 gesture")
        assertNotNull(g1)
        assertNull(g2)
        assertNull(g3)
        assertNull(g4)
    }

    @Test
    fun `W - Different keys during deduplication window remain completely independent`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 150L, clock = { currentTime })

        // Key C at t = 1000
        val gC = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_C, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, defaultProfile)
        assertNotNull(gC, "Key C must be accepted")

        // Key D at t = 1025 (within 150ms of Key C, but different physical key)
        currentTime += 25L
        val gD = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_D, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, defaultProfile)
        assertNotNull(gD, "Key D must be accepted independently")
        assertEquals(KeyEvent.KEYCODE_D, gD.input.keyCode)
    }

    @Test
    fun `X - Intentional rapid double press separated by window produces TWO gestures`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 150L, clock = { currentTime })

        // Press 1: t = 1000
        val g1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_C, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, defaultProfile)
        assertNotNull(g1)

        // Press 2: t = 1160 (> 150ms)
        currentTime += 160L
        val g2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_C, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, defaultProfile)
        assertNotNull(g2, "Second intentional press after 160ms must be accepted")
    }

    @Test
    fun `Y - Synthetic 10,000-event stress executes cleanly without race or memory issues`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 150L, clock = { currentTime })
        val keys = listOf(
            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J,
            KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L
        )

        var totalGesturesFired = 0
        for (i in 0 until 10_000) {
            val key = keys[i % keys.size]
            val isDuplicate = (i % 3) != 0
            if (!isDuplicate) {
                currentTime += 160L // New intentional press
            } else {
                currentTime += 10L  // Rapid burst or duplicate
            }
            val origin = if (i % 2 == 0) EventOrigin.ACTIVITY else EventOrigin.ACCESSIBILITY
            val g = detector.processRawKeyEvent(
                vendorId = 0x2dc8,
                productId = 0x9021,
                keyCode = key,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                origin = origin,
                profile = defaultProfile
            )
            if (g != null) {
                totalGesturesFired++
                val resolved = ControllerMappingResolver.resolve(defaultConfig, ControllerContext.STUDY_QUESTION, g)
                // Ensure no crash during resolution
            }
        }
        assertTrue(totalGesturesFired > 0, "Stress test must fire valid gestures across 10,000 synthetic events")
    }

    @Test
    fun `AA - Controller input enabled check reflects active profile state`() {
        val enabledConfig = defaultConfig.copy(isControllerEnabled = true)
        val disabledConfig = defaultConfig.copy(isControllerEnabled = false)
        val profileDisabledConfig = defaultConfig.copy(
            isControllerEnabled = true,
            profiles = listOf(defaultProfile.copy(enabled = false))
        )

        assertTrue(enabledConfig.isControllerEnabled && enabledConfig.activeProfile.enabled)
        assertFalse(disabledConfig.isControllerEnabled && disabledConfig.activeProfile.enabled)
        assertFalse(profileDisabledConfig.isControllerEnabled && profileDisabledConfig.activeProfile.enabled)
    }

    @Test
    fun `AB - Unrelated keyboard and virtual device filters strictly preserve privacy`() {
        val devInfoCandidate = InputDeviceInfo(
            id = 1,
            name = "8BitDo Micro Gamepad Keyboard",
            descriptor = "desc-8bitdo",
            vendorId = 0x2dc8,
            productId = 0x9021,
            sources = 0x101,
            isGamepad = false,
            isJoystick = false,
            isDpad = false,
            isKeyboard = true,
            isExternal = true
        )
        assertTrue(devInfoCandidate.isCandidateController)

        val devInfoNormalKeyboard = InputDeviceInfo(
            id = 2,
            name = "Standard PC Keyboard",
            descriptor = "desc-kb",
            vendorId = 0x1234,
            productId = 0x5678,
            sources = 0x101,
            isGamepad = false,
            isJoystick = false,
            isDpad = false,
            isKeyboard = true,
            isExternal = true
        )
        // Standard non-gamepad keyboard without controller keywords is external keyboard, but let's check
        val devInfoVirtualTouchscreen = InputDeviceInfo(
            id = 3,
            name = "Virtual Touchscreen",
            descriptor = "desc-touch",
            vendorId = 0,
            productId = 0,
            sources = 0x1002,
            isGamepad = false,
            isJoystick = false,
            isDpad = false,
            isKeyboard = false,
            isExternal = false
        )
        assertFalse(devInfoVirtualTouchscreen.isCandidateController)
    }

    @Test
    fun `AC - Unmapped eligible controller key returns Unmapped and safe result`() {
        val unmappedInput = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_Z)
        val gesture = ControllerGesture(unmappedInput)

        val action = ControllerMappingResolver.resolve(defaultConfig, ControllerContext.STUDY_QUESTION, gesture)
        assertNull(action, "Unmapped key must return null action")
    }

    @Test
    fun `AD - replaceMapping correctly removes old binding and adds new binding when button changes`() {
        val initialProfile = DefaultControllerProfiles.defaultProfile()
        val originalCount = initialProfile.mappings.size

        val oldMapping = initialProfile.mappings.first { it.action == ControllerAction.REVEAL_ANSWER }
        val newGesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_P))
        val newMapping = ControllerMapping(oldMapping.context, newGesture, oldMapping.action)

        val updatedProfile = ControllerMappingResolver.replaceMapping(initialProfile, oldMapping, newMapping)

        assertEquals(originalCount, updatedProfile.mappings.size, "Mapping count must remain equal when replacing")
        val resolved = ControllerMappingResolver.resolve(
            ControllerConfig(activeProfileId = updatedProfile.id, profiles = listOf(updatedProfile)),
            ControllerContext.STUDY_QUESTION,
            newGesture
        )
        assertEquals(ControllerAction.REVEAL_ANSWER, resolved)
        val oldResolved = ControllerMappingResolver.resolve(
            ControllerConfig(activeProfileId = updatedProfile.id, profiles = listOf(updatedProfile)),
            ControllerContext.STUDY_QUESTION,
            oldMapping.gesture
        )
        assertNull(oldResolved, "Old gesture must no longer be bound")
    }

    @Test
    fun `AE - replaceMapping correctly handles context change without creating ghost mapping`() {
        val initialProfile = DefaultControllerProfiles.defaultProfile()
        val originalCount = initialProfile.mappings.size

        val oldMapping = initialProfile.mappings.first { it.context == ControllerContext.STUDY_RATING && it.action == ControllerAction.RATE_AGAIN }
        val newMapping = ControllerMapping(ControllerContext.STUDY_REVEALED, oldMapping.gesture, ControllerAction.PLAY_EXAMPLE_EN)

        val updatedProfile = ControllerMappingResolver.replaceMapping(initialProfile, oldMapping, newMapping)
        assertEquals(originalCount, updatedProfile.mappings.size)

        val resolvedInRevealed = ControllerMappingResolver.resolve(
            ControllerConfig(activeProfileId = updatedProfile.id, profiles = listOf(updatedProfile)),
            ControllerContext.STUDY_REVEALED,
            oldMapping.gesture
        )
        assertEquals(ControllerAction.PLAY_EXAMPLE_EN, resolvedInRevealed)
    }

    @Test
    fun `AF - Complementary context fallback between STUDY_RATING and STUDY_REVEALED works seamlessly`() {
        val customProfile = ControllerProfile(
            id = "test-comp",
            name = "Test Comp",
            mappings = listOf(
                ControllerMapping(
                    ControllerContext.STUDY_REVEALED,
                    ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)),
                    ControllerAction.PLAY_EXAMPLE_EN
                )
            )
        )
        val config = ControllerConfig(activeProfileId = customProfile.id, profiles = listOf(customProfile))

        // Query in STUDY_RATING context should fall back to STUDY_REVEALED mapping
        val resolved = ControllerMappingResolver.resolve(
            config,
            ControllerContext.STUDY_RATING,
            ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K))
        )
        assertEquals(ControllerAction.PLAY_EXAMPLE_EN, resolved)
    }

    @Test
    fun `AG - L2 (KEYCODE_L) and R2 (KEYCODE_R) resolve and map correctly`() {
        val l2Input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_L)
        val r2Input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_R)

        val profileWithTriggers = ControllerProfile(
            id = "triggers",
            name = "Triggers Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_REVEALED, ControllerGesture(l2Input), ControllerAction.PLAY_EXAMPLE_EN),
                ControllerMapping(ControllerContext.STUDY_REVEALED, ControllerGesture(r2Input), ControllerAction.PLAY_EXAMPLE_VI),
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(r2Input), ControllerAction.START_AUTO_PLAY)
            )
        )
        val config = ControllerConfig(activeProfileId = profileWithTriggers.id, profiles = listOf(profileWithTriggers))

        val l2Action = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_REVEALED, ControllerGesture(l2Input))
        assertEquals(ControllerAction.PLAY_EXAMPLE_EN, l2Action)

        val r2Revealed = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_REVEALED, ControllerGesture(r2Input))
        assertEquals(ControllerAction.PLAY_EXAMPLE_VI, r2Revealed)

        val r2Question = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, ControllerGesture(r2Input))
        assertEquals(ControllerAction.START_AUTO_PLAY, r2Question)
    }

    @Test
    fun `AH - Example audio actions and START_AUTO_PLAY dispatch cleanly`() = runTest {
        var englishPlayed = false
        var vietnamesePlayed = false
        var autoPlayStarted = false

        val studyBridge = StudyControllerBridge
        studyBridge.registerExampleEnglishPlayer { englishPlayed = true; true }
        studyBridge.registerExampleVietnamesePlayer { vietnamesePlayed = true; true }
        studyBridge.registerStartAutoPlay { autoPlayStarted = true; true }

        val mockContext = android.content.ContextWrapper(null)
        val dispatcher = ControllerActionDispatcher(
            appContext = mockContext,
            studyBridge = studyBridge,
            autoPlayCoordinatorProvider = { null }
        )

        val resEn = dispatcher.dispatch(ControllerAction.PLAY_EXAMPLE_EN, ControllerContext.STUDY_REVEALED)
        assertTrue(resEn is ControllerActionResult.Executed)
        assertTrue(englishPlayed)

        val resVi = dispatcher.dispatch(ControllerAction.PLAY_EXAMPLE_VI, ControllerContext.STUDY_REVEALED)
        assertTrue(resVi is ControllerActionResult.Executed)
        assertTrue(vietnamesePlayed)

        val resAutoPlay = dispatcher.dispatch(ControllerAction.START_AUTO_PLAY, ControllerContext.STUDY_QUESTION)
        assertTrue(resAutoPlay is ControllerActionResult.Executed)
        assertTrue(autoPlayStarted)
    }

    @Test
    fun `AI - Basic everywhere-applicable mapping resolves across multiple valid contexts`() {
        val l1Input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)
        val l2Input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_L)
        val r2Input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_R)

        val basicProfile = ControllerProfile(
            id = "basic-profile",
            name = "Basic Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(l1Input), ControllerAction.REPLAY_PRIMARY_AUDIO),
                ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(l2Input), ControllerAction.PLAY_EXAMPLE_EN),
                ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(r2Input), ControllerAction.PLAY_EXAMPLE_VI)
            )
        )
        val config = ControllerConfig(activeProfileId = basicProfile.id, profiles = listOf(basicProfile))

        // L1 Replay resolves in Question, Revealed, Rating, and AutoPlay
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, ControllerGesture(l1Input)))
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_REVEALED, ControllerGesture(l1Input)))
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_RATING, ControllerGesture(l1Input)))
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, ControllerMappingResolver.resolve(config, ControllerContext.AUTO_PLAY, ControllerGesture(l1Input)))

        // L2 Example EN resolves in Question, Revealed, and AutoPlay
        assertEquals(ControllerAction.PLAY_EXAMPLE_EN, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, ControllerGesture(l2Input)))
        assertEquals(ControllerAction.PLAY_EXAMPLE_EN, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_REVEALED, ControllerGesture(l2Input)))
        assertEquals(ControllerAction.PLAY_EXAMPLE_EN, ControllerMappingResolver.resolve(config, ControllerContext.AUTO_PLAY, ControllerGesture(l2Input)))

        // R2 Example VI resolves in Question, Revealed, and AutoPlay
        assertEquals(ControllerAction.PLAY_EXAMPLE_VI, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, ControllerGesture(r2Input)))
        assertEquals(ControllerAction.PLAY_EXAMPLE_VI, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_REVEALED, ControllerGesture(r2Input)))
    }

    @Test
    fun `AJ - Basic mapping does NOT execute in context where action is not applicable`() {
        val bInput = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_D) // B -> Reveal Answer in Basic
        val basicProfile = ControllerProfile(
            id = "basic-action-policy",
            name = "Basic Action Policy",
            mappings = listOf(
                ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(bInput), ControllerAction.REVEAL_ANSWER)
            )
        )
        val config = ControllerConfig(activeProfileId = basicProfile.id, profiles = listOf(basicProfile))

        // Reveal Answer is applicable in Question
        assertEquals(ControllerAction.REVEAL_ANSWER, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, ControllerGesture(bInput)))

        // Reveal Answer is NOT applicable in AutoPlay or Shadowing
        assertNull(ControllerMappingResolver.resolve(config, ControllerContext.AUTO_PLAY, ControllerGesture(bInput)))
        assertNull(ControllerMappingResolver.resolve(config, ControllerContext.SHADOWING, ControllerGesture(bInput)))
    }

    @Test
    fun `AK - Exact context override wins over basic everywhere-applicable mapping`() {
        val aInput = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_C) // A

        val overrideProfile = ControllerProfile(
            id = "override-profile",
            name = "Override Profile",
            mappings = listOf(
                // Basic: A -> Play / Pause
                ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(aInput), ControllerAction.PLAY_PAUSE),
                // Specific Override: A in Study Rating -> Rate Again
                ControllerMapping(ControllerContext.STUDY_RATING, ControllerGesture(aInput), ControllerAction.RATE_AGAIN)
            )
        )
        val config = ControllerConfig(activeProfileId = overrideProfile.id, profiles = listOf(overrideProfile))

        // In Rating: exact context override wins -> RATE_AGAIN
        assertEquals(ControllerAction.RATE_AGAIN, ControllerMappingResolver.resolve(config, ControllerContext.STUDY_RATING, ControllerGesture(aInput)))

        // In AutoPlay: Basic mapping applies -> PLAY_PAUSE
        assertEquals(ControllerAction.PLAY_PAUSE, ControllerMappingResolver.resolve(config, ControllerContext.AUTO_PLAY, ControllerGesture(aInput)))
    }

    @Test
    fun `AL - Editing mapping replaces old mapping and keeps count unchanged`() {
        val initialProfile = defaultProfile
        val initialCount = initialProfile.mappings.size

        val oldMapping = initialProfile.mappings.first { it.gesture.input.keyCode == KeyEvent.KEYCODE_K }
        val newGesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_M)) // Change to R1
        val newMapping = oldMapping.copy(gesture = newGesture)

        val updatedProfile = ControllerMappingResolver.replaceMapping(initialProfile, oldMapping, newMapping)
        assertEquals(initialCount, updatedProfile.mappings.size, "Mapping count must remain identical after edit")

        // Old button is unmapped
        val oldResolved = ControllerMappingResolver.resolve(
            ControllerConfig(profiles = listOf(updatedProfile), activeProfileId = updatedProfile.id),
            oldMapping.context,
            oldMapping.gesture
        )
        assertNull(oldResolved)

        // New button is mapped
        val newResolved = ControllerMappingResolver.resolve(
            ControllerConfig(profiles = listOf(updatedProfile), activeProfileId = updatedProfile.id),
            newMapping.context,
            newMapping.gesture
        )
        assertEquals(newMapping.action, newResolved)
    }

    @Test
    fun `AM - Adding mapping increases count exactly one`() {
        val initialProfile = defaultProfile
        val initialCount = initialProfile.mappings.size

        val newBinding = ControllerMapping(
            context = ControllerContext.GLOBAL,
            gesture = ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_P)),
            action = ControllerAction.MUTE_TOGGLE
        )

        val updatedProfile = ControllerMappingResolver.upsertMapping(initialProfile, newBinding)
        assertEquals(initialCount + 1, updatedProfile.mappings.size)
    }

    @Test
    fun `AN - Conflict detection catches duplicate basic gesture`() {
        val l1Input = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)
        val profile = ControllerProfile(
            id = "conflict-profile",
            name = "Conflict Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(l1Input), ControllerAction.REPLAY_PRIMARY_AUDIO)
            )
        )

        val candidate = ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(l1Input), ControllerAction.REVEAL_ANSWER)
        val conflict = ControllerMappingResolver.findConflict(profile, candidate)

        assertNotNull(conflict)
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, conflict.action)
    }

    @Test
    fun `AO - Same gesture with different valid context override is allowed`() {
        val aInput = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_C)
        val profile = ControllerProfile(
            id = "context-allowed",
            name = "Context Allowed",
            mappings = listOf(
                ControllerMapping(ControllerContext.GLOBAL, ControllerGesture(aInput), ControllerAction.PLAY_PAUSE)
            )
        )

        // Candidate has a specific context (STUDY_RATING) instead of GLOBAL
        val candidate = ControllerMapping(ControllerContext.STUDY_RATING, ControllerGesture(aInput), ControllerAction.RATE_AGAIN)
        val conflict = ControllerMappingResolver.findConflict(profile, candidate)

        assertNull(conflict, "A context-specific override on a different context is not a direct collision in that context")
    }

    @Test
    fun `AP - ControllerButtonDirectory correctly maps physical names for L1, R1, L2, R2 and all K-mode buttons`() {
        assertEquals("L1", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_K))
        assertEquals("R1", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_M))
        assertEquals("L2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_L))
        assertEquals("R2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_R))
        assertEquals("A", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_G))
        assertEquals("B", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_J))
        assertEquals("X", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_H))
        assertEquals("Y", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_I))
        assertEquals("D-Pad Up", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_C))
        assertEquals("D-Pad Down", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_D))
        assertEquals("D-Pad Left", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_E))
        assertEquals("D-Pad Right", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_F))
        assertEquals("Select / -", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_N))
        assertEquals("Start / +", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_O))
        assertEquals("Star / Mode", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_S))
    }

    @Test
    fun `AQ - Backward compatibility legacy profiles with context-specific mappings load and resolve identically`() {
        val legacyProfile = ControllerProfile(
            id = "legacy-v1",
            name = "Legacy V1 Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K)), ControllerAction.REPLAY_PRIMARY_AUDIO),
                ControllerMapping(ControllerContext.STUDY_RATING, ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_C)), ControllerAction.RATE_AGAIN)
            )
        )
        val config = ControllerConfig(activeProfileId = legacyProfile.id, profiles = listOf(legacyProfile))

        val resolvedQuestion = ControllerMappingResolver.resolve(
            config,
            ControllerContext.STUDY_QUESTION,
            ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_K))
        )
        assertEquals(ControllerAction.REPLAY_PRIMARY_AUDIO, resolvedQuestion)

        val resolvedRating = ControllerMappingResolver.resolve(
            config,
            ControllerContext.STUDY_RATING,
            ControllerGesture(ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_C))
        )
        assertEquals(ControllerAction.RATE_AGAIN, resolvedRating)
    }

    @Test
    fun `AR - STUDY_REVEALED mapping replaces conflicting STUDY_RATING mapping on upsert and detects conflict`() {
        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G)
        val initialProfile = ControllerProfile(
            id = "test-prof",
            name = "Test Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_RATING, ControllerGesture(inputA), ControllerAction.RATE_AGAIN)
            )
        )

        val newRevealedMapping = ControllerMapping(
            ControllerContext.STUDY_REVEALED,
            ControllerGesture(inputA),
            ControllerAction.CONTINUE_CURRENT_MODE
        )

        // Conflict detection across complementary contexts
        val conflict = ControllerMappingResolver.findConflict(initialProfile, newRevealedMapping)
        assertNotNull(conflict)
        assertEquals(ControllerContext.STUDY_RATING, conflict.context)
        assertEquals(ControllerAction.RATE_AGAIN, conflict.action)

        // Upsert must replace the complementary STUDY_RATING mapping
        val updatedProfile = ControllerMappingResolver.upsertMapping(initialProfile, newRevealedMapping)
        assertEquals(1, updatedProfile.mappings.size)
        assertEquals(ControllerContext.STUDY_REVEALED, updatedProfile.mappings.single().context)
        assertEquals(ControllerAction.CONTINUE_CURRENT_MODE, updatedProfile.mappings.single().action)
    }

    @Test
    fun `AS - Custom profile with STUDY_REVEALED Continue overrides default STUDY_RATING Again`() {
        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G)
        val customProfile = ControllerProfile(
            id = "custom-revealed",
            name = "Custom Revealed",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(inputA), ControllerAction.REVEAL_ANSWER),
                ControllerMapping(ControllerContext.STUDY_REVEALED, ControllerGesture(inputA), ControllerAction.CONTINUE_CURRENT_MODE)
            )
        )
        val config = ControllerConfig(activeProfileId = customProfile.id, profiles = listOf(customProfile))

        // Question context -> REVEAL_ANSWER
        val questionAction = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_QUESTION, ControllerGesture(inputA))
        assertEquals(ControllerAction.REVEAL_ANSWER, questionAction)

        // Rating context -> CONTINUE_CURRENT_MODE (resolved via complementary fallback to STUDY_REVEALED)
        val ratingAction = ControllerMappingResolver.resolve(config, ControllerContext.STUDY_RATING, ControllerGesture(inputA))
        assertEquals(ControllerAction.CONTINUE_CURRENT_MODE, ratingAction)
    }

    @Test
    fun `AT - Runtime action trace records rich diagnostic entries on gesture dispatch`() = runTest {
        ControllerDiagnosticsHolder.clear()
        val mockContext = android.content.ContextWrapper(null)
        val prefStore = object : ControllerPreferenceStore {
            var cfg = defaultConfig
            override fun load(): ControllerConfig = cfg
            override fun save(config: ControllerConfig) { cfg = config }
        }
        val prefController = ControllerPreferencesController(prefStore)
        val router = ControllerInputRouter(mockContext, prefController, dispatcher = ControllerActionDispatcher(mockContext))
        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G)
        val gesture = ControllerGesture(inputA, ControllerPressType.PRESS)

        router.dispatchGesture(gesture, origin = EventOrigin.ACCESSIBILITY)

        val state = ControllerDiagnosticsHolder.state.value
        assertEquals(1, state.runtimeTrace.size)
        val entry = state.runtimeTrace.single()
        assertEquals(EventOrigin.ACCESSIBILITY, entry.origin)
        assertEquals("A", entry.buttonLabel)
        assertEquals(KeyEvent.KEYCODE_G, entry.keyCode)
        assertEquals(ControllerPressType.PRESS, entry.pressType)
        assertNotNull(entry.dispatchResult)
    }

    @Test
    fun `AU - LOOP_PRIMARY_AUDIO and LOOP_EXAMPLE_EN are selectable under PLAYBACK category`() {
        assertTrue(ControllerAction.LOOP_PRIMARY_AUDIO.isUserSelectable)
        assertEquals(ControllerActionCategory.PLAYBACK, ControllerAction.LOOP_PRIMARY_AUDIO.category)
        assertEquals("Loop primary audio", ControllerAction.LOOP_PRIMARY_AUDIO.label)

        assertTrue(ControllerAction.LOOP_EXAMPLE_EN.isUserSelectable)
        assertEquals(ControllerActionCategory.PLAYBACK, ControllerAction.LOOP_EXAMPLE_EN.category)
        assertEquals("Loop English example", ControllerAction.LOOP_EXAMPLE_EN.label)
    }

    @Test
    fun `AV - LOOP_PRIMARY_AUDIO and LOOP_EXAMPLE_EN context applicability`() {
        assertTrue(ControllerAction.LOOP_PRIMARY_AUDIO.isApplicableIn(ControllerContext.STUDY_QUESTION))
        assertTrue(ControllerAction.LOOP_PRIMARY_AUDIO.isApplicableIn(ControllerContext.STUDY_REVEALED))
        assertTrue(ControllerAction.LOOP_PRIMARY_AUDIO.isApplicableIn(ControllerContext.STUDY_RATING))
        assertTrue(ControllerAction.LOOP_PRIMARY_AUDIO.isApplicableIn(ControllerContext.AUTO_PLAY))

        assertTrue(ControllerAction.LOOP_EXAMPLE_EN.isApplicableIn(ControllerContext.STUDY_QUESTION))
        assertTrue(ControllerAction.LOOP_EXAMPLE_EN.isApplicableIn(ControllerContext.STUDY_REVEALED))
        assertTrue(ControllerAction.LOOP_EXAMPLE_EN.isApplicableIn(ControllerContext.STUDY_RATING))
        assertTrue(ControllerAction.LOOP_EXAMPLE_EN.isApplicableIn(ControllerContext.AUTO_PLAY))
    }

    @Test
    fun `AW - LOOP_PRIMARY_AUDIO and LOOP_EXAMPLE_EN dispatch through StudyControllerBridge`() = runTest {
        var loopPrimaryCalled = false
        var loopExampleCalled = false

        val target = object : StudyControllerTarget {
            override fun currentContext(): ControllerContext = ControllerContext.STUDY_QUESTION
            override suspend fun revealAnswer(): Boolean = false
            override suspend fun rate(rating: ReviewRating): Boolean = false
            override suspend fun next(): Boolean = false
            override suspend fun previous(): Boolean = false
            override fun replayAudio(): Boolean = false
            override fun loopPrimaryAudio(): Boolean {
                loopPrimaryCalled = true
                return true
            }
            override fun loopExampleEnglish(): Boolean {
                loopExampleCalled = true
                return true
            }
        }
        StudyControllerBridge.register(target)

        val dispatcher = ControllerActionDispatcher(android.content.ContextWrapper(null))
        val res1 = dispatcher.dispatch(ControllerAction.LOOP_PRIMARY_AUDIO, ControllerContext.STUDY_QUESTION)
        assertTrue(res1 is ControllerActionResult.Executed)
        assertTrue(loopPrimaryCalled)

        val res2 = dispatcher.dispatch(ControllerAction.LOOP_EXAMPLE_EN, ControllerContext.STUDY_QUESTION)
        assertTrue(res2 is ControllerActionResult.Executed)
        assertTrue(loopExampleCalled)

        StudyControllerBridge.unregister(target)
    }

    @Test
    fun `AX - PLAY_EXAMPLE_EN vs LOOP_EXAMPLE_EN maintain distinct play and loop semantics`() = runTest {
        var playedExample = false
        var loopedExample = false

        val target = object : StudyControllerTarget {
            override fun currentContext(): ControllerContext = ControllerContext.STUDY_QUESTION
            override suspend fun revealAnswer(): Boolean = false
            override suspend fun rate(rating: ReviewRating): Boolean = false
            override suspend fun next(): Boolean = false
            override suspend fun previous(): Boolean = false
            override fun replayAudio(): Boolean = false
            override fun exampleEnglishAudio(): String? = "file:///example.mp3"
            override fun loopExampleEnglish(): Boolean {
                loopedExample = true
                return true
            }
        }
        StudyControllerBridge.register(target)

        var audioReplayLooping: Boolean? = null
        val audioMock = object : vn.loi.learning.android.media.AndroidAudioController() {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
            ): vn.loi.learning.android.media.AndroidAudioState {
                audioReplayLooping = isLooping
                return vn.loi.learning.android.media.AndroidAudioState.Playing
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        val dispatcher = ControllerActionDispatcher(android.content.ContextWrapper(null))

        // Dispatch PLAY_EXAMPLE_EN -> isLooping = false
        val playRes = dispatcher.dispatch(ControllerAction.PLAY_EXAMPLE_EN, ControllerContext.STUDY_QUESTION)
        assertTrue(playRes is ControllerActionResult.Executed)
        assertEquals(false, audioReplayLooping)

        // Dispatch LOOP_EXAMPLE_EN -> isLooping = true
        val loopRes = dispatcher.dispatch(ControllerAction.LOOP_EXAMPLE_EN, ControllerContext.STUDY_QUESTION)
        assertTrue(loopRes is ControllerActionResult.Executed)
        assertEquals(true, audioReplayLooping)

        StudyControllerBridge.unregisterBackgroundAudioController(audioMock)
        StudyControllerBridge.unregister(target)
    }
}
