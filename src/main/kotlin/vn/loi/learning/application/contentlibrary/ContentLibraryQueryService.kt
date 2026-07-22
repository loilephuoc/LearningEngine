package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.LearningItemRepository

class ContentLibraryQueryService(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val learningItemRepository: LearningItemRepository
) {

    fun query(): List<ContentLibrarySummary> {
        val enabledCounts = learningItemRepository.findAllEnabled()
            .groupingBy { item -> item.contentId }
            .eachCount()

        return contentLibraryRepository
            .findAll()
            .sortedBy { library ->
                library.name.lowercase()
            }
            .map { library ->
                ContentLibrarySummary(
                    id = library.id.value,
                    name = library.name,
                    contentCount = library.contentCount,
                    learningItemCount =
                        library.contentIds.sumOf { contentId ->
                            enabledCounts[contentId] ?: 0
                        }
                )
            }
    }
}
