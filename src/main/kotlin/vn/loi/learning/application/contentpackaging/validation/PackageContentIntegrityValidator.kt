package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.ImportedPackageContent

class PackageContentIntegrityValidator {

    fun validate(
        importedContent: ImportedPackageContent
    ): PackageValidationReport {
        val issues = mutableListOf<PackageValidationIssue>()

        importedContent.contents
            .groupBy { content -> content.id }
            .filterValues { contents -> contents.size > 1 }
            .keys
            .forEach { contentId ->
                issues += PackageValidationIssue(
                    code = "DUPLICATE_CONTENT_ID",
                    message = "Duplicate content ID: $contentId.",
                    severity = PackageValidationSeverity.ERROR
                )
            }

        importedContent.learningItems
            .groupBy { learningItem -> learningItem.id }
            .filterValues { learningItems -> learningItems.size > 1 }
            .keys
            .forEach { learningItemId ->
                issues += PackageValidationIssue(
                    code = "DUPLICATE_LEARNING_ITEM_ID",
                    message = "Duplicate learning item ID: $learningItemId.",
                    severity = PackageValidationSeverity.ERROR
                )
            }

        return PackageValidationReport(
            issues = issues
        )
    }
}
