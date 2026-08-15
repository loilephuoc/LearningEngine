package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopRecoveryRestartContractTest {
    @Test
    fun `successful restore exits before publishing any same process continuation`() {
        val source = Files.readString(workspaceFile("desktop/src/main/kotlin/vn/loi/learning/desktop/DesktopMain.kt"))
        val callback = source.substringAfter("onRestoreBackup = { operationActive ->").substringBefore("}\n                )")
        val restore = callback.indexOf("runtime.recovery.restore(source, operationActive)")
        val exit = callback.indexOf("exitApplication()")
        val returnPath = callback.indexOf("source.toString()")
        assertTrue(restore >= 0)
        assertTrue(exit > restore, "A successful restore must force clean process reload")
        assertTrue(returnPath > exit, "No restored success may be published before exit is requested")
    }

    private fun workspaceFile(relative: String): Path {
        var current = Path.of("").toAbsolutePath()
        repeat(6) {
            val candidate = current.resolve(relative)
            if (Files.exists(candidate)) return candidate
            current = current.parent ?: return@repeat
        }
        error("Workspace file not found: $relative")
    }
}
