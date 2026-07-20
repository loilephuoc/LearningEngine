package vn.loi.learning.application.contentpackaging.validation

class InvalidPackageException(
    val report: PackageValidationReport
) : IllegalArgumentException(
    buildMessage(report)
) {

    companion object {

        private fun buildMessage(
            report: PackageValidationReport
        ): String =
            report.errors.joinToString(
                separator = System.lineSeparator()
            ) { issue ->
                "[${issue.code}] ${issue.message}"
            }
    }
}
