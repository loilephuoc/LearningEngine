package vn.loi.learning.desktop.tts

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Manages isolated temporary preview files for Desktop TTS.
 * Ensures preview files do not pollute permanent media storage and are cleaned up on replacement/dispose.
 */
class TtsPreviewStore(
    private val tempDirectory: Path = Files.createTempDirectory("learning-engine-tts-preview")
) : AutoCloseable {

    @Volatile
    private var lastPreviewFile: Path? = null

    init {
        Files.createDirectories(tempDirectory)
    }

    /**
     * Allocates a new temporary preview output file and cleans up the previous preview file.
     */
    fun createPreviewFile(): Path {
        cleanPrevious()
        val fileName = "preview_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.mp3"
        val newPath = tempDirectory.resolve(fileName).normalize()
        lastPreviewFile = newPath
        return newPath
    }

    /**
     * Cleans up the previous preview file if it exists.
     */
    fun cleanPrevious() {
        lastPreviewFile?.let { path ->
            try {
                Files.deleteIfExists(path)
            } catch (_: Exception) {
            }
        }
        lastPreviewFile = null
    }

    /**
     * Cleans all temporary files in this preview store.
     */
    fun clearAll() {
        cleanPrevious()
        try {
            if (Files.isDirectory(tempDirectory)) {
                Files.walk(tempDirectory).use { stream ->
                    stream.filter { it != tempDirectory }
                        .sorted(Comparator.reverseOrder())
                        .forEach { path ->
                            try {
                                Files.deleteIfExists(path)
                            } catch (_: Exception) {
                            }
                        }
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun close() {
        clearAll()
        try {
            Files.deleteIfExists(tempDirectory)
        } catch (_: Exception) {
        }
    }
}
