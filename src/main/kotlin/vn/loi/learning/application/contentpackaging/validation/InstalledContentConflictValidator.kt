package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.ContentFingerprintFactory
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.LearningItemFingerprintFactory
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository

/**
 * Phát hiện xung đột định danh và nội dung giữa package đang import
 * với dữ liệu đã được cài đặt trong Learning Engine.
 *
 * Validator chỉ đọc repository. Việc quyết định dừng import thuộc
 * PackageImportService.
 */
class InstalledContentConflictValidator(
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val contentFingerprintFactory: ContentFingerprintFactory =
        ContentFingerprintFactory(),
    private val learningItemFingerprintFactory: LearningItemFingerprintFactory =
        LearningItemFingerprintFactory(
            contentFingerprintFactory =
                contentFingerprintFactory
        )
) {

    fun validate(
        importedContent: ImportedPackageContent
    ): PackageValidationReport {
        val installedContents =
            contentRepository.findAll()

        val installedContentsByFingerprint =
            installedContents.groupBy(
                contentFingerprintFactory::create
            )

        val importedContentsById =
            importedContent.contents
                .associateBy { content ->
                    content.id
                }

        val issues =
            buildList {
                importedContent.contents
                    .map { content ->
                        content.id
                    }
                    .distinct()
                    .filter { contentId ->
                        contentRepository.findById(
                            contentId
                        ) != null
                    }
                    .forEach { contentId ->
                        add(
                            PackageValidationIssue(
                                code =
                                    "CONTENT_ID_ALREADY_INSTALLED",
                                message =
                                    "Content ID is already installed: $contentId.",
                                severity =
                                    PackageValidationSeverity.ERROR
                            )
                        )
                    }

                importedContent.learningItems
                    .map { learningItem ->
                        learningItem.id
                    }
                    .distinct()
                    .filter { learningItemId ->
                        learningItemRepository.findById(
                            learningItemId
                        ) != null
                    }
                    .forEach { learningItemId ->
                        add(
                            PackageValidationIssue(
                                code =
                                    "LEARNING_ITEM_ID_ALREADY_INSTALLED",
                                message =
                                    "Learning item ID is already installed: $learningItemId.",
                                severity =
                                    PackageValidationSeverity.ERROR
                            )
                        )
                    }

                importedContent.contents
                    .groupBy(
                        contentFingerprintFactory::create
                    )
                    .forEach { (fingerprint, importedMatches) ->
                        val importedIds =
                            importedMatches
                                .map { content ->
                                    content.id
                                }
                                .toSet()

                        val installedMatches =
                            installedContentsByFingerprint[fingerprint]
                                .orEmpty()
                                .filter { installedContent ->
                                    installedContent.id !in importedIds
                                }

                        if (installedMatches.isNotEmpty()) {
                            add(
                                PackageValidationIssue(
                                    code =
                                        "CONTENT_ALREADY_INSTALLED",
                                    message =
                                        "Content ${
                                            importedMatches.joinToString { content ->
                                                content.id.toString()
                                            }
                                        } duplicates installed content ${
                                            installedMatches.joinToString { content ->
                                                content.id.toString()
                                            }
                                        }.",
                                    severity =
                                        PackageValidationSeverity.ERROR
                                )
                            )
                        }
                    }

                importedContent.learningItems
                    .forEach { importedLearningItem ->
                        val importedContentValue =
                            importedContentsById[
                                importedLearningItem.contentId
                            ] ?: return@forEach

                        val contentFingerprint =
                            contentFingerprintFactory.create(
                                importedContentValue
                            )

                        val importedFingerprint =
                            learningItemFingerprintFactory.create(
                                learningItem =
                                    importedLearningItem,
                                content =
                                    importedContentValue
                            )

                        val installedMatches =
                            installedContentsByFingerprint[
                                contentFingerprint
                            ]
                                .orEmpty()
                                .flatMap { installedContent ->
                                    learningItemRepository
                                        .findByContentId(
                                            installedContent.id
                                        )
                                        .map { installedLearningItem ->
                                            installedLearningItem to
                                                    installedContent
                                        }
                                }
                                .filter { (
                                              installedLearningItem,
                                              installedContent
                                          ) ->
                                    installedLearningItem.id !=
                                            importedLearningItem.id &&
                                            learningItemFingerprintFactory.create(
                                                learningItem =
                                                    installedLearningItem,
                                                content =
                                                    installedContent
                                            ) ==
                                            importedFingerprint
                                }

                        if (installedMatches.isNotEmpty()) {
                            add(
                                PackageValidationIssue(
                                    code =
                                        "LEARNING_ITEM_ALREADY_INSTALLED",
                                    message =
                                        "Learning item ${importedLearningItem.id} duplicates installed learning item ${
                                            installedMatches.joinToString { (
                                                                                learningItem,
                                                                                _
                                                                            ) ->
                                                learningItem.id.toString()
                                            }
                                        }.",
                                    severity =
                                        PackageValidationSeverity.ERROR
                                )
                            )
                        }
                    }
            }

        return PackageValidationReport(
            issues =
                issues
        )
    }
}