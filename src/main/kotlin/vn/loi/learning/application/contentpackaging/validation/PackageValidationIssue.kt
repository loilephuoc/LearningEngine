package vn.loi.learning.application.contentpackaging.validation

data class PackageValidationIssue(
    val code: String,
    val message: String,
    val severity: PackageValidationSeverity
) {

    init {
        require(code.isNotBlank()) {
            "Package validation issue code must not be blank."
        }

        require(message.isNotBlank()) {
            "Package validation issue message must not be blank."
        }
    }
}
