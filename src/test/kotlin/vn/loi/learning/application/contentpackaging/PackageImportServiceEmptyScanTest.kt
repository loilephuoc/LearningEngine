package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner
import kotlin.test.assertTrue


class PackageImportServiceEmptyScanTest {

    @Test
    fun `import all returns empty result when scanner finds no packages`() {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        var installerCalled = false
        var importerCalled = false

        val service =
            PackageImportService(
                packageScanner =
                    PackageScanner {
                        emptyList()
                    },
                packageInstaller =
                    PackageInstaller {
                        installerCalled = true
                        error("Installer must not be called")
                    },
                packageContentImporter =
                    PackageContentImporter {
                        importerCalled = true
                        error("Importer must not be called")
                    },
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                packageRegistrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository =
                            contentPackageRepository,
                        packageCatalogRepository =
                            packageCatalogRepository
                    ),
                transactionRunner =
                    InMemoryTransactionRunner()
            )

        val catalogId =
            PackageCatalogId("empty-catalog")

        val results =
            service.importAll(catalogId)

        assertTrue(results.isEmpty())
        assertFalse(installerCalled)
        assertFalse(importerCalled)

        assertEquals(
            0,
            contentRepository.count()
        )

        assertEquals(
            0,
            learningItemRepository.count()
        )

        assertEquals(
            0,
            contentPackageRepository.count()
        )

        assertNull(
            packageCatalogRepository.findById(catalogId)
        )
    }
}