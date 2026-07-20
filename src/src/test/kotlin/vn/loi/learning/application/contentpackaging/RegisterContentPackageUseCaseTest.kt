package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class RegisterContentPackageUseCaseTest {

    private fun createPackage(
        id: String,
        name: String = "Package $id"
    ): ContentPackage =
        ContentPackage(
            id = PackageId(id),
            descriptor = PackageDescriptor(
                name = name,
                version = "1.0.0",
                format = "OPD3"
            )
        )

    private fun createUseCase(
        packageRepository: InMemoryContentPackageRepository,
        catalogRepository: InMemoryPackageCatalogRepository
    ): RegisterContentPackageUseCase =
        RegisterContentPackageUseCase(
            registrationOperation =
                PackageRegistrationOperation(
                    contentPackageRepository = packageRepository,
                    packageCatalogRepository = catalogRepository
                ),
            transactionRunner = InMemoryTransactionRunner()
        )

    @Test
    fun `execute saves package and creates catalog when missing`() {
        val packageRepository =
            InMemoryContentPackageRepository()

        val catalogRepository =
            InMemoryPackageCatalogRepository()

        val useCase =
            createUseCase(
                packageRepository = packageRepository,
                catalogRepository = catalogRepository
            )

        val catalogId =
            PackageCatalogId("catalog-1")

        val contentPackage =
            createPackage("package-1")

        val result =
            useCase.execute(
                RegisterContentPackageCommand(
                    catalogId = catalogId,
                    contentPackage = contentPackage
                )
            )

        assertEquals(
            contentPackage,
            packageRepository.findById(contentPackage.id)
        )

        assertEquals(
            setOf(contentPackage.id),
            result.packageIds
        )

        assertEquals(
            result,
            catalogRepository.findById(catalogId)
        )
    }

    @Test
    fun `execute registers package in existing catalog`() {
        val packageRepository =
            InMemoryContentPackageRepository()

        val catalogRepository =
            InMemoryPackageCatalogRepository()

        val firstPackageId =
            PackageId("package-1")

        val catalogId =
            PackageCatalogId("catalog-1")

        catalogRepository.save(
            PackageCatalog(
                id = catalogId,
                packageIds = setOf(firstPackageId)
            )
        )

        val useCase =
            createUseCase(
                packageRepository = packageRepository,
                catalogRepository = catalogRepository
            )

        val secondPackage =
            createPackage("package-2")

        val result =
            useCase.execute(
                RegisterContentPackageCommand(
                    catalogId = catalogId,
                    contentPackage = secondPackage
                )
            )

        assertEquals(
            setOf(firstPackageId, secondPackage.id),
            result.packageIds
        )

        assertEquals(
            2,
            result.packageCount
        )
    }

    @Test
    fun `registering same package twice does not duplicate package ID`() {
        val packageRepository =
            InMemoryContentPackageRepository()

        val catalogRepository =
            InMemoryPackageCatalogRepository()

        val useCase =
            createUseCase(
                packageRepository = packageRepository,
                catalogRepository = catalogRepository
            )

        val command =
            RegisterContentPackageCommand(
                catalogId = PackageCatalogId("catalog-1"),
                contentPackage = createPackage("package-1")
            )

        useCase.execute(command)
        val result = useCase.execute(command)

        assertEquals(
            1,
            result.packageCount
        )

        assertEquals(
            setOf(command.contentPackage.id),
            result.packageIds
        )

        assertNotNull(
            packageRepository.findById(
                command.contentPackage.id
            )
        )
    }

    @Test
    fun `registering different package with same ID is rejected`() {
        val packageRepository = InMemoryContentPackageRepository()
        val catalogRepository = InMemoryPackageCatalogRepository()
        val useCase = createUseCase(packageRepository, catalogRepository)
        val originalPackage = createPackage(id = "package-1", name = "Original Package")
        val conflictingPackage = createPackage(id = "package-1", name = "Conflicting Package")

        useCase.execute(
            RegisterContentPackageCommand(
                catalogId = PackageCatalogId("catalog-1"),
                contentPackage = originalPackage
            )
        )

        assertFailsWith<DuplicatePackageException> {
            useCase.execute(
                RegisterContentPackageCommand(
                    catalogId = PackageCatalogId("catalog-1"),
                    contentPackage = conflictingPackage
                )
            )
        }

        assertEquals(
            originalPackage,
            packageRepository.findById(originalPackage.id)
        )
    }
}

