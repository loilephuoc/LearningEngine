package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.ContentFingerprintFactory
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.LearningItemFingerprintFactory
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository

/**
 * Phát hiện xung đột định danh và nội dung giữa package đang import
 * với dữ liệu đã được cài đặt trong Learning Engine (AC-6, AC-7).
 *
 * Tối ưu hóa hiệu năng: Đọc danh sách đã cài đặt ONCE từ repository
 * để đạt độ phức tạp O(N) thay vì O(N^2) I/O đĩa.
 */
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

class InstalledContentConflictValidator(
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val contentFingerprintFactory: ContentFingerprintFactory =
        ContentFingerprintFactory(),
    private val learningItemFingerprintFactory: LearningItemFingerprintFactory =
        LearningItemFingerprintFactory(
            contentFingerprintFactory =
                contentFingerprintFactory
        ),
    private val installedPackageRepository: InstalledPackageRepository? = null
) {

    fun validate(
        importedContent: ImportedPackageContent
    ): PackageValidationReport {
        val activeOrArchivedPackageIds = installedPackageRepository?.findAll()
            .orEmpty()
            .filter { it.state == PackageState.ACTIVE || it.state == PackageState.ARCHIVED }
            .mapTo(HashSet()) { it.packageId.value }

        val activeOrArchivedInstIds = installedPackageRepository?.findAll()
            .orEmpty()
            .filter { it.state == PackageState.ACTIVE || it.state == PackageState.ARCHIVED }
            .mapTo(HashSet()) { it.id.value }

        val allInstalledContents = contentRepository.findAll()
        val installedContents = if (installedPackageRepository != null) {
            allInstalledContents.filter { content ->
                val cid = content.id.value
                activeOrArchivedPackageIds.any { pkgId -> cid.startsWith(pkgId) || cid.contains("-$pkgId-") } ||
                        activeOrArchivedInstIds.any { instId -> cid.startsWith(instId) || cid.contains("-$instId-") }
            }
        } else {
            allInstalledContents
        }
        val installedContentIds = installedContents.mapTo(HashSet()) { it.id }
        val installedContentsByFingerprint = installedContents.groupBy(contentFingerprintFactory::create)

        val installedLearningItems = learningItemRepository.findAllEnabled()
        val installedLearningItemIds = installedLearningItems.mapTo(HashSet()) { it.id }
        val installedLearningItemsByContentId = installedLearningItems.groupBy { it.contentId }

        val importedContentsById = importedContent.contents.associateBy { it.id }

        val issues = buildList {
            importedContent.contents
                .map { it.id }
                .distinct()
                .filter { contentId -> contentId in installedContentIds }
                .forEach { contentId ->
                    add(
                        PackageValidationIssue(
                            code = "CONTENT_ID_ALREADY_INSTALLED",
                            message = "Content ID is already installed: $contentId.",
                            severity = PackageValidationSeverity.ERROR
                        )
                    )
                }

            importedContent.learningItems
                .map { it.id }
                .distinct()
                .filter { learningItemId -> learningItemId in installedLearningItemIds }
                .forEach { learningItemId ->
                    add(
                        PackageValidationIssue(
                            code = "LEARNING_ITEM_ID_ALREADY_INSTALLED",
                            message = "Learning item ID is already installed: $learningItemId.",
                            severity = PackageValidationSeverity.ERROR
                        )
                    )
                }

            importedContent.contents
                .groupBy(contentFingerprintFactory::create)
                .forEach { (fingerprint, importedMatches) ->
                    val importedIds = importedMatches.map { it.id }.toSet()
                    val installedMatches = installedContentsByFingerprint[fingerprint]
                        .orEmpty()
                        .filter { installedContent -> installedContent.id !in importedIds }

                    if (installedMatches.isNotEmpty()) {
                        add(
                            PackageValidationIssue(
                                code = "CONTENT_ALREADY_INSTALLED",
                                message = "Content ${
                                    importedMatches.joinToString { it.id.toString() }
                                } duplicates installed content ${
                                    installedMatches.joinToString { it.id.toString() }
                                }.",
                                severity = PackageValidationSeverity.ERROR
                            )
                        )
                    }
                }

            importedContent.learningItems.forEach { importedLearningItem ->
                val importedContentValue = importedContentsById[importedLearningItem.contentId]
                    ?: return@forEach

                val contentFingerprint = contentFingerprintFactory.create(importedContentValue)
                val importedFingerprint = learningItemFingerprintFactory.create(
                    learningItem = importedLearningItem,
                    content = importedContentValue
                )

                val installedMatches = installedContentsByFingerprint[contentFingerprint]
                    .orEmpty()
                    .flatMap { installedContent ->
                        installedLearningItemsByContentId[installedContent.id]
                            .orEmpty()
                            .map { installedLearningItem -> installedLearningItem to installedContent }
                    }
                    .filter { (installedLearningItem, installedContent) ->
                        installedLearningItem.id != importedLearningItem.id &&
                                learningItemFingerprintFactory.create(
                                    learningItem = installedLearningItem,
                                    content = installedContent
                                ) == importedFingerprint
                    }

                if (installedMatches.isNotEmpty()) {
                    add(
                        PackageValidationIssue(
                            code = "LEARNING_ITEM_ALREADY_INSTALLED",
                            message = "Learning item ${importedLearningItem.id} duplicates installed learning item ${
                                installedMatches.joinToString { (learningItem, _) -> learningItem.id.toString() }
                            }.",
                            severity = PackageValidationSeverity.ERROR
                        )
                    )
                }
            }
        }

        return PackageValidationReport(issues = issues)
    }
}