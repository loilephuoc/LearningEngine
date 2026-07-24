package vn.loi.learning.infrastructure.contentmedia

import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.model.Content

class ImportedMediaVerifier(
    private val mediaStorage: ContentMediaStorage
) {

    fun verify(
        contents: List<Content>,
        cancellationSignal: PackageImportCancellationSignal? = null
    ) {
        cancellationSignal?.checkCancelled()
        val checkedPaths = HashSet<String>()
        contents.forEachIndexed { index, content ->
            if (index % 100 == 0) {
                cancellationSignal?.checkCancelled()
            }
            verifyPath(content.media.primaryAudio, checkedPaths)
            verifyPath(content.media.translatedAudio, checkedPaths)
            verifyPath(content.media.image, checkedPaths)
            verifyPath(content.media.exampleAudio, checkedPaths)
            verifyPath(content.media.exampleTranslatedAudio, checkedPaths)
        }
    }

    private fun verifyPath(
        relativePath: String?,
        checkedPaths: MutableSet<String>
    ) {
        if (relativePath == null || !checkedPaths.add(relativePath)) {
            return
        }

        require(
            mediaStorage.exists(
                relativePath
            )
        ) {
            "Missing imported media: $relativePath"
        }
    }
}