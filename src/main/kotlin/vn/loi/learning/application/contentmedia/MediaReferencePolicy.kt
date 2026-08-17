package vn.loi.learning.application.contentmedia

import vn.loi.learning.application.port.ContentMediaStorage

object MediaReferencePolicy {
    val NO_IMAGE_SENTINEL_FILENAMES = setOf(
        "no_image.jpg",
        "no_image.jpeg",
        "no_image.png",
        "no_image.webp"
    )

    fun isNoImageSentinel(path: String?): Boolean {
        if (path.isNullOrBlank()) return true
        val fileName = path.trim().replace('\\', '/').substringAfterLast('/').lowercase()
        return fileName in NO_IMAGE_SENTINEL_FILENAMES
    }

    fun canonicalizeMediaReference(ref: String?, mediaStorage: ContentMediaStorage?): String? {
        if (ref.isNullOrBlank()) return null
        if (isNoImageSentinel(ref)) return null
        val cleanRef = ref.trim().replace('\\', '/')
        if (mediaStorage == null) return cleanRef
        val resolved = mediaStorage.resolve(cleanRef) ?: return cleanRef
        val resolvedFileName = resolved.fileName.toString()
        val refFileName = cleanRef.substringAfterLast('/')
        if (!refFileName.equals(resolvedFileName, ignoreCase = true)) {
            val parent = cleanRef.substringBeforeLast('/', missingDelimiterValue = "")
            return if (parent.isNotEmpty()) "$parent/$resolvedFileName" else resolvedFileName
        }
        return cleanRef
    }
}
