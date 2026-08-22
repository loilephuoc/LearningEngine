package vn.loi.learning.android.recovery

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidRestoreIntentInvariantTest {
    @Test
    fun `selected packages are always passed as an explicit selective restore intent`() {
        val source = source("vn/loi/learning/android/recovery/BackupRestoreScreen.kt")

        assertTrue(source.contains("viewModel.confirmRestore(state.stagedFile, state.selectedPackageIds)"))
        assertFalse(source.contains("if (isAll) null else state.selectedPackageIds"))
    }

    @Test
    fun `library ViewModel identity follows the reloaded application graph generation`() {
        val source = source("vn/loi/learning/android/MainActivity.kt")

        assertTrue(source.contains("viewModel<AndroidLibraryViewModel>(key = \"library-graph-\$graphRetry\")"))
        assertTrue(source.contains("app.reloadApplicationGraph()"))
        assertTrue(source.contains("graphRetry += 1"))
    }

    @Test
    fun `restore success reports semantic records instead of archive entries`() {
        val source = source("vn/loi/learning/android/recovery/BackupRestoreScreen.kt")

        assertTrue(source.contains("state.restoredCounts.learningItems"))
        assertTrue(source.contains("state.restoredCounts.mediaFiles"))
        assertFalse(source.contains("restoredEntriesCount} items restored"))
    }

    private fun source(relative: String): String {
        val moduleRoot = Path.of(System.getProperty("user.dir"))
        val candidate = moduleRoot.resolve("src/main/kotlin").resolve(relative)
        return Files.readString(candidate)
    }
}
