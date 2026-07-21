package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageImportServiceDetailedResultTest {

    @Test
    fun `detailed import reports every failed candidate without stopping the directory scan`() {
        val candidates =
            listOf(
                PackageScanCandidate("C:/packages/broken.opd3"),
                PackageScanCandidate("C:/packages/unsupported.pkg")
            )

        val service =
            PackageImportService(
                packageScanner =
                    PackageScanner {
                        candidates
                    },
                packageInstaller =
                    PackageInstaller { candidate ->
                        error("Cannot install ${candidate.source}")
                    },
                packageContentImporter =
                    PackageContentImporter {
                        error("Importer must not be called")
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

        assertEquals(2, result.discoveredPackageCount)
        assertTrue(result.successfulImports.isEmpty())
        assertEquals(candidates.map(PackageScanCandidate::source), result.failures.map(PackageImportFailure::source))
        assertTrue(result.failures.all { failure -> failure.message.startsWith("Cannot install") })
    }
}
