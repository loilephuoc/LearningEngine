package vn.loi.learning.application.contentpackaging

import java.io.IOException
import vn.loi.learning.application.contentpackaging.validation.InvalidPackageException

/**
 * Non-fail-fast result for directory imports where each discovered package is
 * reported independently. Successful packages remain committed even when a
 * later candidate is incompatible or malformed.
 */
data class PackageImportBatchResult(
    val successfulImports: List<PackageImportResult> = emptyList(),
    val failures: List<PackageImportFailure> = emptyList()
) {
    val discoveredPackageCount: Int
        get() = successfulImports.size + failures.size

    val hasFailures: Boolean
        get() = failures.isNotEmpty()
}

enum class PackageImportFailureKind {
    VALIDATION,
    DUPLICATE,
    FILE_ACCESS,
    MALFORMED_PACKAGE,
    UNEXPECTED
}

/**
 * Actionable diagnostic for one package candidate.
 *
 * [message] remains the user-facing text consumed by existing Desktop
 * presentation code. Structured fields allow later presentation and
 * diagnostic export without parsing that text.
 */
data class PackageImportFailure(
    val source: String,
    val message: String,
    val kind: PackageImportFailureKind =
        PackageImportFailureKind.UNEXPECTED,
    val code: String = "PACKAGE_IMPORT_FAILED",
    val detailCodes: List<String> = emptyList(),
    val recoveryAction: String =
        "Review the package and application logs, then retry the import."
) {
    init {
        require(source.isNotBlank()) {
            "Package import failure source must not be blank."
        }
        require(message.isNotBlank()) {
            "Package import failure message must not be blank."
        }
        require(code.isNotBlank()) {
            "Package import failure code must not be blank."
        }
        require(recoveryAction.isNotBlank()) {
            "Package import recovery action must not be blank."
        }
    }

    companion object {
        fun from(
            source: String,
            exception: Exception
        ): PackageImportFailure {
            val diagnostic =
                when (exception) {
                    is InvalidPackageException ->
                        Diagnostic(
                            kind = PackageImportFailureKind.VALIDATION,
                            code = "PACKAGE_VALIDATION_FAILED",
                            detailCodes =
                                exception.report.errors
                                    .map { issue -> issue.code }
                                    .distinct(),
                            recoveryAction =
                                "Correct the listed package data and retry. " +
                                    "The invalid package was not persisted."
                        )

                    is DuplicatePackageException ->
                        Diagnostic(
                            kind = PackageImportFailureKind.DUPLICATE,
                            code = "PACKAGE_ALREADY_INSTALLED",
                            recoveryAction =
                                "Remove the conflicting installed package or " +
                                    "import a package with a different identity."
                        )

                    is IOException,
                    is SecurityException ->
                        Diagnostic(
                            kind = PackageImportFailureKind.FILE_ACCESS,
                            code = "PACKAGE_FILE_ACCESS_FAILED",
                            recoveryAction =
                                "Check that the file still exists and that the " +
                                    "application can read the selected directory."
                        )

                    is PackageImportException,
                    is IllegalArgumentException ->
                        Diagnostic(
                            kind =
                                PackageImportFailureKind.MALFORMED_PACKAGE,
                            code = "PACKAGE_MALFORMED",
                            recoveryAction =
                                "Verify the package format and required files, " +
                                    "then export or download the package again."
                        )

                    else ->
                        Diagnostic(
                            kind = PackageImportFailureKind.UNEXPECTED,
                            code = "PACKAGE_IMPORT_FAILED",
                            recoveryAction =
                                "Review the package and application logs, then " +
                                    "retry the import."
                        )
                }

            val causeMessage =
                exception.message
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: exception::class.simpleName
                    ?: "Unknown package import error."

            return PackageImportFailure(
                source = source,
                message = causeMessage,
                kind = diagnostic.kind,
                code = diagnostic.code,
                detailCodes = diagnostic.detailCodes,
                recoveryAction = diagnostic.recoveryAction
            )
        }
    }

    private data class Diagnostic(
        val kind: PackageImportFailureKind,
        val code: String,
        val detailCodes: List<String> = emptyList(),
        val recoveryAction: String
    )
}
