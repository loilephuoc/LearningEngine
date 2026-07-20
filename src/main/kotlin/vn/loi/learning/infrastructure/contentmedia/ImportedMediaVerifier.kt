package vn.loi.learning.infrastructure.contentmedia

import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.model.Content

class ImportedMediaVerifier(
    private val mediaStorage: ContentMediaStorage
) {

    fun verify(
        contents: List<Content>
    ) {
        contents.forEach { content ->
            verify(content.media.primaryAudio)
            verify(content.media.translatedAudio)
            verify(content.media.image)
            verify(content.media.exampleAudio)
            verify(content.media.exampleTranslatedAudio)
        }
    }

    private fun verify(
        relativePath: String?
    ) {
        if (relativePath == null) {
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