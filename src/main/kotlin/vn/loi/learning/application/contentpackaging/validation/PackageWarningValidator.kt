package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.ImportedPackageContent

class PackageWarningValidator {

    fun validate(
        importedContent: ImportedPackageContent
    ): PackageValidationReport {
        val issues = importedContent.warnings.map { warning ->
            PackageValidationIssue(
                code = "PACKAGE_IMPORT_WARNING",
                message = warning,
                severity = PackageValidationSeverity.WARNING
            )
        }

        return PackageValidationReport(
            issues = issues
        )
    }
}
