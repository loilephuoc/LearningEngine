package vn.loi.learning.application.contentpackaging

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.validation.InvalidPackageException
import vn.loi.learning.application.contentpackaging.validation.PackageValidationIssue
import vn.loi.learning.application.contentpackaging.validation.PackageValidationReport
import vn.loi.learning.application.contentpackaging.validation.PackageValidationSeverity
import vn.loi.learning.domain.content.packaging.model.PackageId

class PackageImportFailureTest {

    @Test
    fun `validation failure preserves issue codes and recovery guidance`() {
        val failure =
            PackageImportFailure.from(
                source = "C:/packages/broken.opd3",
                exception =
                    InvalidPackageException(
                        PackageValidationReport(
                            issues =
                                listOf(
                                    PackageValidationIssue(
                                        code = "MISSING_CONTENT",
                                        message = "Learning item references missing content.",
                                        severity =
                                            PackageValidationSeverity.ERROR
                                    ),
                                    PackageValidationIssue(
                                        code = "MISSING_CONTENT",
                                        message = "Another item references missing content.",
                                        severity =
                                            PackageValidationSeverity.ERROR
                                    )
                                )
                        )
                    )
            )

        assertEquals(
            PackageImportFailureKind.VALIDATION,
            failure.kind
        )
        assertEquals(
            "PACKAGE_VALIDATION_FAILED",
            failure.code
        )
        assertEquals(
            listOf("MISSING_CONTENT"),
            failure.detailCodes
        )
        assertTrue(
            failure.message.contains(
                "Learning item references missing content."
            )
        )
        assertTrue(
            failure.recoveryAction.contains(
                "invalid package was not persisted"
            )
        )
    }

    @Test
    fun `duplicate package receives conflict recovery guidance`() {
        val failure =
            PackageImportFailure.from(
                source = "C:/packages/existing.opd3",
                exception =
                    DuplicatePackageException(
                        PackageId("package-existing")
                    )
            )

        assertEquals(
            PackageImportFailureKind.DUPLICATE,
            failure.kind
        )
        assertEquals(
            "PACKAGE_ALREADY_INSTALLED",
            failure.code
        )
        assertTrue(
            failure.recoveryAction.contains(
                "conflicting installed package"
            )
        )
    }

    @Test
    fun `file access failure explains directory recovery`() {
        val failure =
            PackageImportFailure.from(
                source = "C:/packages/missing.opd3",
                exception =
                    IOException("The file disappeared.")
            )

        assertEquals(
            PackageImportFailureKind.FILE_ACCESS,
            failure.kind
        )
        assertEquals(
            "PACKAGE_FILE_ACCESS_FAILED",
            failure.code
        )
        assertTrue(
            failure.recoveryAction.contains(
                "application can read the selected directory"
            )
        )
    }

    @Test
    fun `illegal package argument is classified as malformed`() {
        val failure =
            PackageImportFailure.from(
                source = "C:/packages/malformed.opd3",
                exception =
                    IllegalArgumentException(
                        "Manifest version is blank."
                    )
            )

        assertEquals(
            PackageImportFailureKind.MALFORMED_PACKAGE,
            failure.kind
        )
        assertEquals(
            "PACKAGE_MALFORMED",
            failure.code
        )
        assertTrue(
            failure.recoveryAction.contains(
                "export or download the package again"
            )
        )
    }

    @Test
    fun `unknown failure has stable fallback diagnostic`() {
        val failure =
            PackageImportFailure.from(
                source = "C:/packages/unknown.opd3",
                exception =
                    IllegalStateException()
            )

        assertEquals(
            PackageImportFailureKind.UNEXPECTED,
            failure.kind
        )
        assertEquals(
            "PACKAGE_IMPORT_FAILED",
            failure.code
        )
        assertTrue(failure.message.isNotBlank())
        assertTrue(failure.recoveryAction.isNotBlank())
    }
}
