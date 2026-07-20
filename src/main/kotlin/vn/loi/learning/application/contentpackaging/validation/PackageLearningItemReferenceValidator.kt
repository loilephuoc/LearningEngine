package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.ImportedPackageContent

class PackageLearningItemReferenceValidator {

    fun validate(
        importedContent: ImportedPackageContent
    ): PackageValidationReport {
        val existingContentIds = importedContent.contents
            .map { content -> content.id }
            .toSet()

        val issues = importedContent.learningItems
            .filter { learningItem ->
                learningItem.contentId !in existingContentIds
            }
            .map { learningItem ->
                PackageValidationIssue(
                    code = "MISSING_LEARNING_ITEM_CONTENT",
                    message = "Learning item ${learningItem.id} references missing content ${learningItem.contentId}.",
                    severity = PackageValidationSeverity.ERROR
                )
            }

        return PackageValidationReport(
            issues = issues
        )
    }
}
