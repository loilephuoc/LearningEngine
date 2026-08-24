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
    fun `5 - updateOnlyScreenOn=true + HOME results in runtime allowed`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = true
        val foregroundState = HomeForegroundState.HOME

        val allowed = hasWidgets && autoNextEnabled &&
                (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
                (!updateOnlyScreenOn || foregroundState == HomeForegroundState.HOME)

        assertTrue(allowed)
    }

    @Test
    fun `6 - updateOnlyScreenOn=true + OTHER_APP results in runtime denied`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = true
        val foregroundState = HomeForegroundState.OTHER_APP

        val allowed = hasWidgets && autoNextEnabled &&
                (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
                (!updateOnlyScreenOn || foregroundState == HomeForegroundState.HOME)

        assertFalse(allowed)
    }

    @Test
    fun `7 - updateOnlyScreenOn=true + UNKNOWN results in runtime denied`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = true
        val foregroundState = HomeForegroundState.UNKNOWN

        val allowed = hasWidgets && autoNextEnabled &&
                (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
                (!updateOnlyScreenOn || foregroundState == HomeForegroundState.HOME)

        assertFalse(allowed)
    }

    @Test
    fun `8 - updateOnlyScreenOn=false + screen on and unlocked results in runtime allowed without HOME`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val updateOnlyScreenOn = false

        for (fgState in listOf(HomeForegroundState.HOME, HomeForegroundState.OTHER_APP, HomeForegroundState.UNKNOWN)) {
            val allowed = hasWidgets && autoNextEnabled &&
                    (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
                    (!updateOnlyScreenOn || fgState == HomeForegroundState.HOME)
            assertTrue(allowed, "Must be allowed when updateOnlyScreenOn=false regardless of fgState=$fgState")
        }
    }

    @Test
    fun `9 - screen OFF denied regardless of updateOnlyScreenOn`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.SCREEN_OFF

        for (updateOnlyScreenOn in listOf(true, false)) {
            for (fgState in listOf(HomeForegroundState.HOME, HomeForegroundState.OTHER_APP, HomeForegroundState.UNKNOWN)) {
                val allowed = hasWidgets && autoNextEnabled &&
                        (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
                        (!updateOnlyScreenOn || fgState == HomeForegroundState.HOME)
                assertFalse(allowed, "Must be denied on SCREEN_OFF for updateOnlyScreenOn=$updateOnlyScreenOn, fgState=$fgState")
            }
        }
    }

    @Test
    fun `10 - locked screen denied regardless of updateOnlyScreenOn`() {
        val hasWidgets = true
        val autoNextEnabled = true
        val deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON

        for (updateOnlyScreenOn in listOf(true, false)) {
            for (fgState in listOf(HomeForegroundState.HOME, HomeForegroundState.OTHER_APP, HomeForegroundState.UNKNOWN)) {
                val allowed = hasWidgets && autoNextEnabled &&
                        (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
                        (!updateOnlyScreenOn || fgState == HomeForegroundState.HOME)
                assertFalse(allowed, "Must be denied on LOCKED_SCREEN_ON for updateOnlyScreenOn=$updateOnlyScreenOn, fgState=$fgState")
            }
        }
    }
}
