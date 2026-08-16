package vn.loi.learning.android.autoplay

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.android.ui.AndroidRootDestination

class AutoPlayNavigationAndRouteTest {
    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin", relative))

    @Test
    fun `Auto Play is NOT added as a root destination in bottom navigation`() {
        val rootDestinations = AndroidRootDestination.entries.map { it.route }
        assertFalse(rootDestinations.contains("autoplay"), "AutoPlay must NOT be a bottom navigation root tab")
        assertEquals(5, AndroidRootDestination.entries.size)
    }

    @Test
    fun `HomeScreen contains Auto Play action card and does not embed rating controls`() {
        val studyScreenSrc = source("vn/loi/learning/android/study/StudyScreen.kt")
        assertTrue(studyScreenSrc.contains("item(\"autoplay\")"))
        assertTrue(studyScreenSrc.contains("AutoPlayCard"))
        assertTrue(studyScreenSrc.contains("onAutoPlay: () -> Unit"))
    }

    @Test
    fun `MainActivity routes autoplay with dedicated screen and hides bottom nav`() {
        val mainSrc = source("vn/loi/learning/android/MainActivity.kt")
        assertTrue(mainSrc.contains("currentRoute == \"autoplay\" -> false"))
        assertTrue(mainSrc.contains("composable(\"autoplay\""))
        assertTrue(mainSrc.contains("onAutoPlay = { navController.navigate(\"autoplay\")"))
    }
}
