package vn.loi.learning.application.topic

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Resolves the durable installed-topic identity for content selected by a consumer.
 *
 * Package ownership is authoritative. The deterministic unpackaged fallback preserves
 * compatibility for locally registered content that predates package installation.
 */
class TopicQueryService(
    private val contentPackageRepository:
    ContentPackageRepository,
    private val contentLibraryRepository:
    ContentLibraryRepository,
    private val contentRepository:
    ContentRepository
) {

    fun requireByContentId(
        contentId: ContentId,
        compatibleScopeContentIds: Set<ContentId> =
            setOf(contentId)
    ): TopicSelection {
        requireNotNull(
            contentRepository.findById(
                contentId
            )
        ) {
            "Content $contentId does not exist."
        }

        val owningLibraryIds =
            contentLibraryRepository
                .findAll()
                .asSequence()
                .filter { library ->
                    library.contains(
                        contentId
                    )
                }
                .map { library ->
                    library.id
                }
                .toSet()

        val owningPackages =
            contentPackageRepository
                .findAll()
                .filter { contentPackage ->
                    contentPackage.libraryIds
                        .any(owningLibraryIds::contains)
                }

        val topicIds =
            owningPackages
                .map { contentPackage ->
                    contentPackage.topicId
                }
                .distinct()

        require(topicIds.size <= 1) {
            "Content $contentId belongs to multiple installed topics: " +
                topicIds.joinToString { topicId ->
                    topicId.value
                }
        }

        val contentPackage =
            owningPackages
                .minByOrNull { installedPackage ->
                    installedPackage.id.value
                }

        return TopicSelection(
            id =
                topicIds.singleOrNull()
                    ?: TopicId.deriveForUnpackagedContent(
                        compatibleScopeContentIds
                            .ifEmpty {
                                setOf(contentId)
                            }
                            .minBy { scopedContentId ->
                                scopedContentId.value
                            }
                    ),
            installedPackageId =
                contentPackage?.id?.value
        )
    }
}
