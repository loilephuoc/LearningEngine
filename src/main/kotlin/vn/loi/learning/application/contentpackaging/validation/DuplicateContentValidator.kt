package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.ContentFingerprintFactory
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.LearningItemFingerprintFactory

class DuplicateContentValidator(
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
        val contentsById =
            importedContent.contents
                .associateBy { content ->
                    content.id
                }

        val issues =
            buildList {
                importedContent.contents
                    .groupBy(
                        contentFingerprintFactory::create
                    )
                    .values
                    .filter { duplicateContents ->
                        duplicateContents.size > 1
                    }
                    .forEach { duplicateContents ->
                        add(
                            PackageValidationIssue(
                                code =
                                    "DUPLICATE_CONTENT_FINGERPRINT",
                                message =
                                    "Package contains duplicate content: ${
                                        duplicateContents.joinToString { content ->
                                            content.id.toString()
                                        }
                                    }.",
                                severity =
                                    PackageValidationSeverity.ERROR
                            )
                        )
                    }

                importedContent.learningItems
                    .mapNotNull { learningItem ->
                        contentsById[learningItem.contentId]
                            ?.let { content ->
                                learningItem to
                                        learningItemFingerprintFactory.create(
                                            learningItem =
                                                learningItem,
                                            content =
                                                content
                                        )
                            }
                    }
                    .groupBy { itemWithFingerprint ->
                        itemWithFingerprint.second
                    }
                    .values
                    .filter { duplicateLearningItems ->
                        duplicateLearningItems.size > 1
                    }
                    .forEach { duplicateLearningItems ->
                        add(
                            PackageValidationIssue(
                                code =
                                    "DUPLICATE_LEARNING_ITEM_FINGERPRINT",
                                message =
                                    "Package contains duplicate learning items: ${
                                        duplicateLearningItems.joinToString { itemWithFingerprint ->
                                            itemWithFingerprint.first.id.toString()
                                        }
                                    }.",
                                severity =
                                    PackageValidationSeverity.ERROR
                            )
                        )
                    }
            }

        return PackageValidationReport(
            issues =
                issues
        )
    }
}