package vn.loi.learning.desktop.ui.studio

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.net.URI

/**
 * Utility hỗ trợ trích xuất và kiểm tra file khi thực hiện Drag & Drop từ Windows Explorer / Desktop.
 */
object DragDropUtils {

    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
    val AUDIO_EXTENSIONS = setOf("mp3", "wav", "aiff")

    /**
     * Trích xuất danh sách [File] hợp lệ từ AWT [Transferable].
     * Hỗ trợ 3 định dạng truyền tải trên Windows/Linux/macOS:
     * 1. [DataFlavor.javaFileListFlavor] (Default Windows Explorer drop)
     * 2. text/uri-list (URI file list từ browser / file manager)
     * 3. [DataFlavor.stringFlavor] (File path hoặc file:// URI chuỗi)
     */
    fun extractFiles(transferable: Transferable): List<File> {
        val files = mutableListOf<File>()

        // 1. Standard javaFileListFlavor
        try {
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val list = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                list?.filterIsInstance<File>()?.let { files.addAll(it) }
            }
        } catch (_: Exception) {}

        // 2. text/uri-list
        if (files.isEmpty()) {
            try {
                val uriListFlavor = DataFlavor("text/uri-list;class=java.lang.String")
                if (transferable.isDataFlavorSupported(uriListFlavor)) {
                    val uriListStr = transferable.getTransferData(uriListFlavor) as? String
                    if (!uriListStr.isNullOrBlank()) {
                        uriListStr.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !it.startsWith("#") }
                            .forEach { line ->
                                try {
                                    val file = parseFileFromUriOrPath(line)
                                    if (file != null && file.exists()) files.add(file)
                                } catch (_: Exception) {}
                            }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. String flavor fallback
        if (files.isEmpty()) {
            try {
                if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    val str = transferable.getTransferData(DataFlavor.stringFlavor) as? String
                    if (!str.isNullOrBlank()) {
                        str.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { line ->
                                try {
                                    val file = parseFileFromUriOrPath(line)
                                    if (file != null && file.exists()) files.add(file)
                                } catch (_: Exception) {}
                            }
                    }
                }
            } catch (_: Exception) {}
        }

        return files
    }

    private fun parseFileFromUriOrPath(input: String): File? {
        val cleaned = input.trim().removeSurrounding("\"", "\"")
        if (cleaned.isBlank()) return null
        return try {
            if (cleaned.startsWith("file:", ignoreCase = true)) {
                File(URI(cleaned))
            } else {
                File(cleaned)
            }
        } catch (_: Exception) {
            try {
                File(cleaned)
            } catch (_: Exception) { null }
        }
    }

    fun isSupportedImage(file: File): Boolean {
        return file.isFile && file.extension.lowercase() in IMAGE_EXTENSIONS
    }

    fun isSupportedAudio(file: File): Boolean {
        return file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS
    }
}
