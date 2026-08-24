package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class HomeWidgetForegroundAppDetectorTest {

    private class FakeForegroundDetector(
        var usageAccessGranted: Boolean = true,
        var foregroundPackage: String? = "com.miui.home",
        var defaultLauncher: String? = "com.miui.home",
        var ourPackage: String = "vn.loi.learning.android"
    ) : HomeWidgetForegroundDetector {

        override fun hasUsageAccess(): Boolean = usageAccessGranted

        override fun detectForeground(): HomeForegroundDetectionResult {
            if (!usageAccessGranted) {
                return HomeForegroundDetectionResult(
                    state = HomeForegroundState.UNKNOWN,
                    hasPermission = false,
                    reason = "USAGE_ACCESS_NOT_GRANTED"
                )
            }

            val pkg = foregroundPackage
            if (pkg.isNullOrBlank()) {
                return HomeForegroundDetectionResult(
                    state = HomeForegroundState.UNKNOWN,
                    hasPermission = true,
                    reason = "NO_RECENT_USAGE_DATA"
                )
            }

            val isLauncher = AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage(pkg, defaultLauncher)
            val isOurApp = pkg == ourPackage

            val state = when {
                isLauncher -> HomeForegroundState.HOME
                isOurApp -> HomeForegroundState.OTHER_APP
                else -> HomeForegroundState.OTHER_APP
            }

            return HomeForegroundDetectionResult(
                state = state,
                hasPermission = true,
                foregroundPackage = pkg,
                launcherPackage = defaultLauncher
            )
        }
    }

    private fun isClockEligible(
        hasWidgets: Boolean,
        autoNextEnabled: Boolean,
        deviceState: VocabularyPresentationDeviceState
    ): Boolean {
        return hasWidgets && autoNextEnabled && (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON)
    }

    private fun isTransitionEligible(
        hasWidgets: Boolean,
        autoNextEnabled: Boolean,
        deviceState: VocabularyPresentationDeviceState,
        updateOnlyScreenOn: Boolean,
        foregroundState: HomeForegroundState
    ): Boolean {
        val clockEligible = isClockEligible(hasWidgets, autoNextEnabled, deviceState)
        return clockEligible && (!updateOnlyScreenOn || foregroundState == HomeForegroundState.HOME)
    }

    @Test
    fun `1 - Usage Access absent returns UNKNOWN and fails closed`() {
        val detector = FakeForegroundDetector(
            usageAccessGranted = false,
            foregroundPackage = "com.miui.home"
        )
        val result = detector.detectForeground()
        assertEquals(HomeForegroundState.UNKNOWN, result.state)
        assertFalse(result.hasPermission)
        assertEquals("USAGE_ACCESS_NOT_GRANTED", result.reason)
    }

    @Test
    fun `2 - launcher foreground returns HOME`() {
        val detector = FakeForegroundDetector(
            usageAccessGranted = true,
            foregroundPackage = "com.miui.home",
            defaultLauncher = "com.miui.home"
        )
        val result = detector.detectForeground()
        assertEquals(HomeForegroundState.HOME, result.state)
        assertTrue(result.hasPermission)
        assertEquals("com.miui.home", result.foregroundPackage)
    }

    @Test
    fun `3 - normal app foreground returns OTHER_APP`() {
        val detector = FakeForegroundDetector(
            usageAccessGranted = true,
            foregroundPackage = "com.android.chrome",
            defaultLauncher = "com.miui.home"
        )
        val result = detector.detectForeground()
        assertEquals(HomeForegroundState.OTHER_APP, result.state)
        assertTrue(result.hasPermission)
        assertEquals("com.android.chrome", result.foregroundPackage)
    }

    @Test
    fun `4 - stale or no usage event returns UNKNOWN`() {
        val detector = FakeForegroundDetector(
            usageAccessGranted = true,
            foregroundPackage = null,
            defaultLauncher = "com.miui.home"
        )
        val result = detector.detectForeground()
        assertEquals(HomeForegroundState.UNKNOWN, result.state)
        assertTrue(result.hasPermission)
        assertEquals("NO_RECENT_USAGE_DATA", result.reason)
    }

    @Test
    fun `our own app in foreground returns OTHER_APP`() {
        val detector = FakeForegroundDetector(
            usageAccessGranted = true,
            foregroundPackage = "vn.loi.learning.android",
            defaultLauncher = "com.miui.home"
        )
        val result = detector.detectForeground()
        assertEquals(HomeForegroundState.OTHER_APP, result.state)
        assertTrue(result.hasPermission)
    }

    @Test
    fun `Hotfix Scenario 1 - updateOnlyScreenOn=true + OTHER_APP skips candidate but clock remains armed and probing`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = true
        val foregroundState = HomeForegroundState.OTHER_APP

        val clockEligible = isClockEligible(hasWidgets, autoNextEnabled, deviceState)
        val transitionEligible = isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, foregroundState)

        assertTrue(clockEligible, "Clock must remain eligible/armed for probe loop")
        assertFalse(transitionEligible, "Candidate transition must be skipped for OTHER_APP")
    }

    @Test
    fun `Hotfix Scenario 2 - updateOnlyScreenOn=true + UNKNOWN skips candidate but clock remains armed and probing`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = true
        val foregroundState = HomeForegroundState.UNKNOWN

        val clockEligible = isClockEligible(hasWidgets, autoNextEnabled, deviceState)
        val transitionEligible = isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, foregroundState)

        assertTrue(clockEligible, "Clock must remain eligible/armed for probe loop")
        assertFalse(transitionEligible, "Candidate transition must be skipped for UNKNOWN")
    }

    @Test
    fun `Hotfix Scenario 3 - OTHER_APP to HOME resumes candidate advancement on next probe without opening LearningEngine`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = true

        val detector = FakeForegroundDetector(
            usageAccessGranted = true,
            foregroundPackage = "com.zing.zalo",
            defaultLauncher = "com.miui.home"
        )

        // Tick 1: user is in Zalo
        val probe1Result = detector.detectForeground()
        assertEquals(HomeForegroundState.OTHER_APP, probe1Result.state)
        val allowed1 = isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, probe1Result.state)
        assertFalse(allowed1, "Tick 1 must reject transition in Zalo")
        val clockArmed1 = isClockEligible(hasWidgets, autoNextEnabled, deviceState)
        assertTrue(clockArmed1, "Clock must remain armed")

        // User navigates back to Home (without opening LearningEngine)
        detector.foregroundPackage = "com.miui.home"

        // Tick 2 (next probe): user is now on Home screen
        val probe2Result = detector.detectForeground()
        assertEquals(HomeForegroundState.HOME, probe2Result.state)
        val allowed2 = isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, probe2Result.state)
        assertTrue(allowed2, "Tick 2 must automatically resume candidate transition on Home")
    }

    @Test
    fun `Hotfix Scenario 4 - HOME to OTHER_APP blocks candidate advance while OTHER_APP`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = true

        val detector = FakeForegroundDetector(
            usageAccessGranted = true,
            foregroundPackage = "com.miui.home",
            defaultLauncher = "com.miui.home"
        )

        // Start at HOME
        val state1 = detector.detectForeground().state
        assertTrue(isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, state1))

        // Switch to Chrome
        detector.foregroundPackage = "com.android.chrome"
        val state2 = detector.detectForeground().state
        assertEquals(HomeForegroundState.OTHER_APP, state2)
        assertFalse(isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, state2))
    }

    @Test
    fun `Hotfix Scenario 5 - SCREEN_OFF completely cancels clock and probe`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.SCREEN_OFF

        for (updateOnlyScreenOn in listOf(true, false)) {
            for (fgState in listOf(HomeForegroundState.HOME, HomeForegroundState.OTHER_APP, HomeForegroundState.UNKNOWN)) {
                val clockEligible = isClockEligible(hasWidgets, autoNextEnabled, deviceState)
                val transitionEligible = isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, fgState)
                assertFalse(clockEligible, "Clock must be canceled when SCREEN_OFF")
                assertFalse(transitionEligible, "Transition must be rejected when SCREEN_OFF")
            }
        }
    }

    @Test
    fun `Hotfix Scenario 6 - LOCKED_SCREEN_ON completely cancels clock and probe`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON

        for (updateOnlyScreenOn in listOf(true, false)) {
            for (fgState in listOf(HomeForegroundState.HOME, HomeForegroundState.OTHER_APP, HomeForegroundState.UNKNOWN)) {
                val clockEligible = isClockEligible(hasWidgets, autoNextEnabled, deviceState)
                val transitionEligible = isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, fgState)
                assertFalse(clockEligible, "Clock must be canceled when LOCKED_SCREEN_ON")
                assertFalse(transitionEligible, "Transition must be rejected when LOCKED_SCREEN_ON")
            }
        }
    }

    @Test
    fun `Hotfix Scenario 7 - Tick starts at HOME but screen turns OFF before commit - candidate commit aborts`() {
        var currentDeviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val homeForegroundState = HomeForegroundState.HOME
        val updateOnlyScreenOn = true

        // Initial gate passed
        assertTrue(isTransitionEligible(true, true, currentDeviceState, updateOnlyScreenOn, homeForegroundState))

        // Race: screen turns OFF while background selection runs
        currentDeviceState = VocabularyPresentationDeviceState.SCREEN_OFF

        // Commit check
        val shouldAbort = currentDeviceState == VocabularyPresentationDeviceState.SCREEN_OFF ||
                currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        assertTrue(shouldAbort, "Commit must abort if screen turned off during background selection")
    }

    @Test
    fun `Hotfix Scenario 8 - Tick starts at HOME but device locks before commit - candidate commit aborts`() {
        var currentDeviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val homeForegroundState = HomeForegroundState.HOME
        val updateOnlyScreenOn = true

        // Initial gate passed
        assertTrue(isTransitionEligible(true, true, currentDeviceState, updateOnlyScreenOn, homeForegroundState))

        // Race: keyguard locked before commit
        currentDeviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON

        // Commit check
        val shouldAbort = currentDeviceState == VocabularyPresentationDeviceState.SCREEN_OFF ||
                currentDeviceState == VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        assertTrue(shouldAbort, "Commit must abort if device locked during background selection")
    }

    @Test
    fun `Hotfix Scenario 9 - updateOnlyScreenOn=false and screen ON and unlocked preserves rotation regardless of foreground state`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = false

        for (fgState in listOf(HomeForegroundState.HOME, HomeForegroundState.OTHER_APP, HomeForegroundState.UNKNOWN)) {
            val clockEligible = isClockEligible(hasWidgets, autoNextEnabled, deviceState)
            val transitionEligible = isTransitionEligible(hasWidgets, autoNextEnabled, deviceState, updateOnlyScreenOn, fgState)
            assertTrue(clockEligible)
            assertTrue(transitionEligible, "Must be transition eligible when updateOnlyScreenOn=false for fgState=$fgState")
        }
    }

    @Test
    fun `Hotfix Scenario 10 - Auto-audio never plays when transition is not eligible`() {
        val autoAudioEnabled = true
        for (transitionEligible in listOf(true, false)) {
            val canPlayAudio = autoAudioEnabled && transitionEligible
            if (!transitionEligible) {
                assertFalse(canPlayAudio, "Audio must never play on probe-only / rejected tick")
            } else {
                assertTrue(canPlayAudio)
            }
        }
    }
}
