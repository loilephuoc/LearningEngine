package vn.loi.learning.application.contentpackaging.validation

data class PackageValidationReport(
    val issues: List<PackageValidationIssue> = emptyList()
) {

    val errors: List<PackageValidationIssue>
        get() = issues.filter { issue ->
            issue.severity == PackageValidationSeverity.ERROR
        }

    val warnings: List<PackageValidationIssue>
        get() = issues.filter { issue ->
            issue.severity == PackageValidationSeverity.WARNING
        }

    val isValid: Boolean
        get() = errors.isEmpty()

    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()
}
