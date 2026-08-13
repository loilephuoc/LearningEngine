package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId

class LibraryContentQueryService(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository
) {
    fun contentIdsForLibraries(libraryIds: Collection<ContentLibraryId>): Set<ContentId> =
        libraryIds.asSequence()
            .mapNotNull(contentLibraryRepository::findById)
            .flatMap { it.contentIds.asSequence() }
            .toCollection(linkedSetOf())

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

    fun queryForLibraries(
        libraryIds: Collection<ContentLibraryId>
    ): List<LibraryContentItem> =
        queryForLibraries(libraryIds, includeLearningItemCounts = true)

    fun queryDescriptorsForLibraries(
        libraryIds: Collection<ContentLibraryId>
    ): List<LibraryContentItem> =
        queryForLibraries(libraryIds, includeLearningItemCounts = false)

    fun queryDescriptorGroups(
        libraryIdsByGroup: Map<String, Collection<ContentLibraryId>>
    ): Map<String, List<LibraryContentItem>> {
        if (libraryIdsByGroup.isEmpty()) return emptyMap()
        val librariesById = contentLibraryRepository.findAll().associateBy { it.id }
        val contentIdsByGroup = libraryIdsByGroup.mapValues { (_, libraryIds) ->
            libraryIds.mapNotNull(librariesById::get)
                .flatMapTo(linkedSetOf()) { it.contentIds }
        }
        val unionContentIds = contentIdsByGroup.values.flatten().toSet()
        val allContents = contentRepository.findAll()
        val sortedContents = allContents.filter { it.id in unionContentIds }.sortedWith(contentHierarchyComparator)
        val result = contentIdsByGroup.mapValues { (_, contentIds) ->
            sortedContents.asSequence()
                .filter { it.id in contentIds }
                .map { it.toLibraryContentItem(learningItemCount = 0) }
                .toList()
        }
        return result
    }

    private fun queryForLibraries(
        libraryIds: Collection<ContentLibraryId>,
        includeLearningItemCounts: Boolean
    ): List<LibraryContentItem> {
        if (libraryIds.isEmpty()) return emptyList()

        val contentIds = libraryIds
            .mapNotNull { contentLibraryRepository.findById(it) }
            .flatMap { library -> library.contentIds }
            .toSet()

        if (contentIds.isEmpty()) return emptyList()

        val enabledItemCounts =
            if (includeLearningItemCounts) {
                learningItemRepository.findAllEnabled()
                    .groupingBy { item -> item.contentId }
                    .eachCount()
            } else {
                emptyMap()
            }

        return contentRepository
            .findAll()
            .asSequence()
            .filter { content -> content.id in contentIds }
            .sortedWith(contentHierarchyComparator)
            .map { content -> content.toLibraryContentItem(enabledItemCounts[content.id] ?: 0) }
            .toList()
    }

    private fun Content.toLibraryContentItem(learningItemCount: Int) =
        LibraryContentItem(
            id = id.value,
            title = displayName,
            type = type.name,
            group = metadata.group,
            section = metadata.section,
            lesson = metadata.lesson,
            primaryText = text.primaryText,
            translatedText = text.translatedText,
            learningItemCount = learningItemCount,
            imagePath = media.image
        )

    private val contentHierarchyComparator =
        compareBy<Content>(
            { normalizedHierarchyValue(it.metadata.group) },
            { normalizedHierarchyValue(it.metadata.section) },
            { normalizedHierarchyValue(it.metadata.lesson) },
            { it.displayName.lowercase() },
            { it.id.value }
        )

    private fun normalizedHierarchyValue(
        value: String?
    ): String =
        value
            ?.trim()
            ?.lowercase()
            .orEmpty()
}
