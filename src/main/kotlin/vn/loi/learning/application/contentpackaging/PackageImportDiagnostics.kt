package vn.loi.learning.application.contentpackaging

import java.nio.file.Path

/**
 * Developer diagnostic logger cho import workflow.
 * Được kích hoạt khi System property `learning.import.debug` hoặc biến môi trường `LEARNING_ENGINE_DEBUG_IMPORT` = "true".
 */
object PackageImportDiagnostics {
    val isDebugEnabled: Boolean
        get() = System.getProperty("learning.import.debug") == "true" ||
                System.getenv("LEARNING_ENGINE_DEBUG_IMPORT") == "true"

    fun log(stage: String, details: String) {
        if (isDebugEnabled) {
            println("[IMPORT_DIAGNOSTICS] [$stage] $details")
        }
    }

    fun logStart(sourcePath: Path) {
        if (isDebugEnabled) {
            val sizeMb = try {
                if (java.nio.file.Files.isRegularFile(sourcePath)) {
                    java.nio.file.Files.size(sourcePath) / (1024.0 * 1024.0)
                } else 0.0
            } catch (e: Exception) { 0.0 }
            println("[IMPORT_DIAGNOSTICS] [START] Source: $sourcePath, Size: %.2f MB".format(sizeMb))
        }
    }

    fun logResolvedPair(jsonPath: Path, pkgPath: Path) {
        if (isDebugEnabled) {
            val jsonSize = try { java.nio.file.Files.size(jsonPath) / 1024.0 } catch (e: Exception) { 0.0 }
            val pkgSize = try { java.nio.file.Files.size(pkgPath) / (1024.0 * 1024.0) } catch (e: Exception) { 0.0 }
            println("[IMPORT_DIAGNOSTICS] [RESOLVED_PAIR] JSON: $jsonPath (%.1f KB), PKG: $pkgPath (%.2f MB)".format(jsonSize, pkgSize))
        }
    }

    fun logTerminal(result: String, durationMs: Long) {
        if (isDebugEnabled) {
            println("[IMPORT_DIAGNOSTICS] [TERMINAL] Outcome: $result, Elapsed: ${durationMs}ms")
        }
    }
}
