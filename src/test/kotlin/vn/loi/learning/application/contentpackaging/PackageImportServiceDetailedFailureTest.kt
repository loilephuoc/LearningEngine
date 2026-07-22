package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.validation.InvalidPackageException
import vn.loi.learning.application.contentpackaging.validation.PackageValidationIssue
import vn.loi.learning.application.contentpackaging.validation.PackageValidationReport
import vn.loi.learning.application.contentpackaging.validation.PackageValidationSeverity
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageImportServiceDetailedFailureTest {

    @Test
    fun `detailed import returns structured actionable failure for each candidate`() {
        val candidate =
            PackageScanCandidate(
                source = "C:/packages/broken.opd3"
            )

        var contentImporterCalled = false

        val service =
            PackageImportService(
                packageScanner =
                    PackageScanner {
                        listOf(candidate)
                    },
                packageInstaller =
                    PackageInstaller {
                        throw InvalidPackageException(
                            PackageValidationReport(
                                issues =
                                    listOf(
                                        PackageValidationIssue(
                                            code = "INVALID_MANIFEST",
                                            message =
                                                "Manifest format is unsupported.",
                                            severity =
                                                PackageValidationSeverity.ERROR
                                        )
                                    )
                            )
                        )
                    },
                packageContentImporter =
                    PackageContentImporter {
                        contentImporterCalled = true
                        ImportedPackageContent(
                            contents = emptyList(),
                            learningItems = emptyList()
                        )
                    },
                contentRepository =
                    InMemoryContentRepository(),
                learningItemRepository =
                    InMemoryLearningItemRepository(),
                packageRegistrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository =
                            InMemoryContentPackageRepository(),
                        packageCatalogRepository =
                            InMemoryPackageCatalogRepository()
                    ),
                transactionRunner =
                    InMemoryTransactionRunner()
            )

        val result =
            service.importAllDetailed(
                PackageCatalogId("desktop-content-library")
            )

        assertEquals(1, result.discoveredPackageCount)
        assertTrue(result.hasFailures)
        assertTrue(result.successfulImports.isEmpty())
        assertTrue(!contentImporterCalled)

        val failure = result.failures.single()

        assertEquals(candidate.source, failure.source)
        assertEquals(
            PackageImportFailureKind.VALIDATION,
            failure.kind
        )
        assertEquals(
            listOf("INVALID_MANIFEST"),
            failure.detailCodes
        )
        assertTrue(
            failure.message.contains(
                "Manifest format is unsupported."
            )
        )
        assertTrue(
            failure.recoveryAction.contains(
                "The invalid package was not persisted."
            )
        )
    }
}
