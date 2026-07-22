package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.Content

class LibraryContentQueryService(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository
) {

    fun query(
        libraryId: ContentLibraryId
    ): List<LibraryContentItem> {
        val library =
            contentLibraryRepository.findById(
                libraryId
            ) ?: return emptyList()

        val contentIds = library.contentIds
        val enabledItemCounts = learningItemRepository.findAllEnabled()
            .groupingBy { item -> item.contentId }
            .eachCount()

        return contentRepository
            .findAll()
            .asSequence()
            .filter { content -> content.id in contentIds }
            .sortedWith(
                compareBy<Content>(
                    { content ->
                        normalizedHierarchyValue(
                            content.metadata.group
                        )
                    },
                    { content ->
                        normalizedHierarchyValue(
                            content.metadata.section
                        )
                    },
                    { content ->
                        normalizedHierarchyValue(
                            content.metadata.lesson
                        )
                    },
                    { content ->
                        content.displayName.lowercase()
                    },
                    { content ->
                        content.id.value
                    }
                )
            )
            .map { content ->
                LibraryContentItem(
                    id = content.id.value,
                    title = content.displayName,
                    type = content.type.name,
                    group = content.metadata.group,
                    section = content.metadata.section,
                    lesson = content.metadata.lesson,
                    primaryText =
                        content.text.primaryText,
                    translatedText =
                        content.text.translatedText,
                    learningItemCount =
                        enabledItemCounts[content.id] ?: 0,
                    imagePath = content.media.image
                )
            }
            .toList()
    }

    private fun normalizedHierarchyValue(
        value: String?
    ): String =
        value
            ?.trim()
            ?.lowercase()
            .orEmpty()
}
