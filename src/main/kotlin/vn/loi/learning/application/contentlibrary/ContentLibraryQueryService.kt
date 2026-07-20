package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.LearningItemRepository

class ContentLibraryQueryService(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val learningItemRepository: LearningItemRepository
) {

    fun query(): List<ContentLibrarySummary> =
        contentLibraryRepository
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
                            learningItemRepository
                                .findByContentId(contentId)
                                .count { learningItem ->
                                    learningItem.isEnabled
                                }
                        }
                )
            }
}