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
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
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
    private val installedPackageRepository: InstalledPackageRepository? = null,
    private val contentPackageRepository: ContentPackageRepository? = null,
    private val contentLibraryRepository: ContentLibraryRepository? = null
) {

    fun validate(
        importedContent: ImportedPackageContent
    ): PackageValidationReport {
        val installedPackages = installedPackageRepository?.findAll().orEmpty()
        val activeOrArchivedInstalledPackages = installedPackages.filter {
            it.state == PackageState.ACTIVE || it.state == PackageState.ARCHIVED
        }

        // Live Package Keys are strictly sourced from InstalledPackageRepository (if present)
        val canonicalLivePackageKeys = activeOrArchivedInstalledPackages
            .flatMapTo(HashSet()) { listOf(it.packageId.value, it.id.value, it.name.value) }

        // If installedPackageRepository is null or has no installed packages, fall back to contentPackageRepository
        val activeOrArchivedPackageIds = if (installedPackageRepository != null && installedPackages.isNotEmpty()) {
            canonicalLivePackageKeys
        } else {
            contentPackageRepository?.findAll().orEmpty().mapTo(HashSet()) { it.id.value }
        }

        val liveContentIds = HashSet<vn.loi.learning.domain.content.model.ContentId>()
        if (contentLibraryRepository != null) {
            val liveLibraryIds = HashSet<String>()
            liveLibraryIds.addAll(activeOrArchivedPackageIds)
            contentPackageRepository?.findAll().orEmpty()
                .filter { cp -> cp.id.value in activeOrArchivedPackageIds || cp.libraryIds.any { lib -> lib.value in activeOrArchivedPackageIds } }
                .flatMap { it.libraryIds }
                .forEach { liveLibraryIds.add(it.value) }

            contentLibraryRepository.findAll()
                .filter { lib -> lib.id.value in liveLibraryIds || liveLibraryIds.any { libId -> lib.id.value.contains(libId) } }
                .forEach { lib -> liveContentIds.addAll(lib.contentIds) }
        }

        val allInstalledContents = contentRepository.findAll()
        val installedContents = if (installedPackageRepository != null || contentPackageRepository != null || contentLibraryRepository != null) {
            allInstalledContents.filter { content ->
                content.id in liveContentIds ||
                activeOrArchivedPackageIds.any { pkgId -> content.id.value.startsWith(pkgId) || content.id.value.contains("-$pkgId-") }
            }
        } else {
            allInstalledContents
        }
        val installedContentIds = installedContents.mapTo(HashSet()) { it.id }
        val installedContentsByFingerprint = installedContents.groupBy(contentFingerprintFactory::create)

        val allLearningItems = learningItemRepository.findAllEnabled()
        val installedLearningItems = if (installedPackageRepository != null) {
            allLearningItems.filter { it.contentId in installedContentIds }
        } else {
            allLearningItems
        }
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