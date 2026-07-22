package vn.loi.learning.desktop.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class DesktopDiagnosticExportException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

object DesktopDiagnosticExporter {
    const val FILE_NAME: String = "learning-engine-diagnostics.txt"

    fun export(diagnostics: DesktopRuntimeDiagnostics, target: Path): Path {
        val normalized = target.toAbsolutePath().normalize()
        val parent = normalized.parent
            ?: throw DesktopDiagnosticExportException("Diagnostic export requires a parent directory.")

        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw DesktopDiagnosticExportException("Diagnostic export directory does not exist.")
        }
        if (Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw DesktopDiagnosticExportException("Diagnostic export target already exists.")
        }

        val temporary = Files.createTempFile(parent, ".learning-engine-diagnostics-", ".tmp")
        try {
            Files.writeString(
                temporary,
                content(diagnostics),
                StandardCharsets.UTF_8
            )
            try {
                Files.move(temporary, normalized, StandardCopyOption.ATOMIC_MOVE)
            } catch (unsupported: AtomicMoveNotSupportedException) {
                Files.move(temporary, normalized)
            }
        } catch (failure: DesktopDiagnosticExportException) {
            throw failure
        } catch (failure: Exception) {
            throw DesktopDiagnosticExportException("Could not export runtime diagnostics.", failure)
        } finally {
            Files.deleteIfExists(temporary)
        }
        return normalized
    }

    fun content(diagnostics: DesktopRuntimeDiagnostics): String =
        buildString {
            appendLine("Learning Engine diagnostic export")
            appendLine("Schema: 1")
            append(diagnostics.supportSummary())
            appendLine()
        }
}
