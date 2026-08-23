package vn.loi.learning.android.sync

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.Test

class AndroidSyncNavigationContractTest {
    private val mainRoot = Path.of(System.getProperty("user.dir"), "src/main")

    @Test fun `settings entry opens one private single activity route with standard back`() {
        val main = Files.readString(mainRoot.resolve("kotlin/vn/loi/learning/android/MainActivity.kt"))
        val settings = Files.readString(mainRoot.resolve("kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt"))
        assertContains(settings, "onSyncSettings")
        assertContains(settings, "R.string.settings_sync")
        assertContains(main, "navController.navigate(\"sync_settings\")")
        assertContains(main, "composable(\"sync_settings\"")
        assertContains(main, "AndroidSyncSettingsScreen(syncViewModel) { navController.popBackStack() }")
    }

    @Test fun `sync navigation adds no manifest component or automatic lifecycle trigger`() {
        val manifest = Files.readString(mainRoot.resolve("AndroidManifest.xml"))
        val main = Files.readString(mainRoot.resolve("kotlin/vn/loi/learning/android/MainActivity.kt"))
        assertFalse(manifest.contains("sync_settings"))
        val route = main.substringAfter("composable(\"sync_settings\"").substringBefore("composable(\"backup_restore\"")
        assertFalse(route.contains("syncNow("))
        assertFalse(route.contains("LaunchedEffect"))
    }
}
