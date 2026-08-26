package vn.loi.learning.desktop.ui.studio

import java.awt.Toolkit
import java.awt.datatransfer.Transferable

internal sealed interface ClipboardImageSnapshotResult {
    data class Ready(val snapshot: BrowserDropSnapshot) : ClipboardImageSnapshotResult
    data object NoImage : ClipboardImageSnapshotResult
    data class Failure(val message: String) : ClipboardImageSnapshotResult
}

/** Snapshots supported system-clipboard image payloads before any asynchronous work begins. */
internal class ClipboardImageExtractor(
    private val browserExtractor: BrowserImageDropExtractor = BrowserImageDropExtractor()
) {
    fun snapshotSystemClipboard(): ClipboardImageSnapshotResult {
        val transferable = try {
            Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
        } catch (_: Exception) {
            return ClipboardImageSnapshotResult.Failure("Clipboard image could not be read.")
        } ?: return ClipboardImageSnapshotResult.NoImage
        return snapshot(transferable)
    }

    internal fun snapshot(transferable: Transferable): ClipboardImageSnapshotResult {
        val owned = try {
            browserExtractor.snapshot(transferable)
        } catch (failure: BrowserImageDropException) {
            return if (failure.message?.contains("does not contain a supported image") == true) {
                ClipboardImageSnapshotResult.NoImage
            } else {
                ClipboardImageSnapshotResult.Failure(failure.message ?: "Clipboard image could not be read.")
            }
        }
        return if (browserExtractor.hasImageCandidate(owned)) {
            ClipboardImageSnapshotResult.Ready(owned)
        } else {
            ClipboardImageSnapshotResult.NoImage
        }
    }

    suspend fun extract(snapshot: BrowserDropSnapshot): ExtractedDroppedImage =
        browserExtractor.extract(snapshot)
}
