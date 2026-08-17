package vn.loi.learning.desktop.ui.browser.imagereuse

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.application.port.ContentMediaStorage

/**
 * Resolves a media file path using both package identity and image reference.
 *
 * Guarantees:
 * - Package identity is part of the media resolution key, preventing cross-package collision
 *   when different packages contain files with identical names (e.g., belt.jpg).
 * - Checks the specific package directory before falling back to global lookup.
 */
object PackageMediaResolver {

    fun resolve(
        packageName: String?,
        reference: String?,
        mediaStorage: ContentMediaStorage?
    ): Path? {
        if (reference.isNullOrBlank() || mediaStorage == null) return null
        val cleanRef = reference.trim().replace('\\', '/').removePrefix("./")
        if (cleanRef.isBlank()) return null

        val cleanPkg = packageName?.trim()
        if (cleanPkg.isNullOrBlank()) {
            return mediaStorage.resolve(cleanRef)
        }

        if (cleanRef.startsWith("$cleanPkg/", ignoreCase = true)) {
            return mediaStorage.resolve(cleanRef)
        }

        val rawWithoutMedia = cleanRef.removePrefix("media/").removePrefix("/")
        val candidates = listOf(
            "$cleanPkg/$cleanRef",
            "$cleanPkg/$rawWithoutMedia",
            "$cleanPkg/media/$rawWithoutMedia"
        ).distinct()

        for (candidate in candidates) {
            val resolved = mediaStorage.resolve(candidate)
            if (resolved != null && Files.isRegularFile(resolved)) {
                return resolved
            }
        }

        return mediaStorage.resolve(cleanRef)
    }
}
