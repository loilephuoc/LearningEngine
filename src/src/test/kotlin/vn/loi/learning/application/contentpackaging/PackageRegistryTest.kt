package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageRegistryTest {

    @Test
    fun `registry installs and registers package`() {
        val candidate = PackageScanCandidate(
            source = "C:/packages/english.opd3"
        )

        val contentPackage = ContentPackage(
            id = PackageId("package-english"),
            descriptor = PackageDescriptor(
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD3"
            )
        )

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        val registerUseCase =
            RegisterContentPackageUseCase(
                registrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository = contentPackageRepository,
                        packageCatalogRepository = packageCatalogRepository
                    ),
                transactionRunner = InMemoryTransactionRunner()
            )

        val registry = PackageRegistry(
            packageInstaller = PackageInstaller { receivedCandidate ->
                assertEquals(candidate, receivedCandidate)
                contentPackage
            },
            registerContentPackageUseCase = registerUseCase
        )

        val catalogId = PackageCatalogId("installed-packages")

        val result = registry.register(
            catalogId = catalogId,
            candidate = candidate
        )

        assertEquals(contentPackage, result)
        assertEquals(
            contentPackage,
            contentPackageRepository.findById(contentPackage.id)
        )

        val catalog =
            packageCatalogRepository.findById(catalogId)

        assertNotNull(catalog)
        assertEquals(
            setOf(contentPackage.id),
            catalog.packageIds
        )
    }
}
