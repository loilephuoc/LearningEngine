package vn.loi.learning.desktop.platform

import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path

object DesktopFileActions {
    fun showInFolder(path: Path) {
        val normalizedPath = path.toAbsolutePath().normalize()

        if (!Files.isRegularFile(normalizedPath)) return

        val file = normalizedPath.toFile()

        runCatching {
            ProcessBuilder(
                "explorer.exe",
                "/select,",
                file.absolutePath
            ).start()
        }.onFailure {
            runCatching {
                val parent = file.parentFile ?: return@runCatching

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(parent)
                }
            }
        }
    }
}
