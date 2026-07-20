package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId

class LibraryContentQueryService(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository
) {

    fun query(
        libraryId: ContentLibraryId
    ): List<LibraryContentItem> {
        val library =
            contentLibraryRepository.findById(libraryId)
                ?: return emptyList()

        return library.contentIds
            .mapNotNull(contentRepository::findById)
            .sortedBy { content ->
                content.displayName.lowercase()
            }
            .map { content ->
                LibraryContentItem(
                    id = content.id.value,
                    title = content.displayName,
                    type = content.type.name,
                    primaryText = content.text.primaryText,
                    translatedText =
                        content.text.translatedText,
                    learningItemCount =
                        learningItemRepository
                            .findByContentId(content.id)
                            .count { learningItem ->
                                learningItem.isEnabled
                            }
                )
            }
    }
}