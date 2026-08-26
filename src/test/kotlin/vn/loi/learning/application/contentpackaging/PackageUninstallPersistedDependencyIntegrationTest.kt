package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.PersistedLearningPlatformFactory
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository

class PackageUninstallPersistedDependencyIntegrationTest {

    @Test
    fun `persisted uninstall rejection preserves package and catalog files`() {
        val directory =
            Files.createTempDirectory(
                "persisted-dependent-uninstall"
            )

        try {
            val repositories =
                createRepositories(
                    directory
                )

            val catalogId =
                PackageCatalogId(
                    "installed-packages"
                )

            val corePackageId =
                PackageId(
                    "core-english"
                )

            val dependentPackageId =
                PackageId(
                    "vocabulary-extension"
                )

            val corePackage =
                createPackage(
                    id =
                        corePackageId,
                    name =
                        "Core English",
                    version =
                        "2.0.0"
                )

            val dependentPackage =
                createPackage(
                    id =
                        dependentPackageId,
                    name =
                        "Vocabulary Extension",
                    version =
                        "1.0.0",
                    dependencies =
                        setOf(
                            PackageDependency(
                                packageName =
                                    "Core English",
                                minimumVersion =
                                    "2.0.0"
                            )
                        )
                )

            val catalog =
                PackageCatalog(
                    id =
                        catalogId,
                    packageIds =
                        setOf(
                            corePackageId,
                            dependentPackageId
                        )
                )

            repositories.packageRepository.save(
                corePackage
            )

            repositories.packageRepository.save(
                dependentPackage
            )

            repositories.catalogRepository.save(
                catalog
            )

            val exception =
                assertFailsWith<
                        PackageRequiredByInstalledPackagesException
                        > {
                    PersistedLearningPlatformFactory
                        .createPersistedUninstaller(
                            directory
                        )
                        .execute(
                            UninstallContentPackageCommand(
                                catalogId =
                                    catalogId,
                                packageId =
                                    corePackageId
                            )
                        )
                }

            assertEquals(
                corePackageId,
                exception.packageId
            )

            assertEquals(
                setOf(
                    dependentPackageId
                ),
                exception.dependentPackageIds
            )

            val reopened =
                createRepositories(
                    directory
                )

            assertEquals(
                corePackage,
                reopened.packageRepository.findById(
                    corePackageId
                )
            )

            assertEquals(
                dependentPackage,
                reopened.packageRepository.findById(
                    dependentPackageId
                )
            )

            assertEquals(
                catalog,
                reopened.catalogRepository.findById(
                    catalogId
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `persisted uninstall succeeds when another compatible provider remains`() {
        val directory =
            Files.createTempDirectory(
                "persisted-alternative-provider-uninstall"
            )

        try {
            val repositories =
                createRepositories(
                    directory
                )

            val catalogId =
                PackageCatalogId(
                    "installed-packages"
                )

            val removedProviderId =
                PackageId(
                    "core-english-legacy"
                )

            val remainingProviderId =
                PackageId(
                    "core-english-current"
                )

            val dependentPackageId =
                PackageId(
                    "vocabulary-extension"
                )

            val removedProvider =
                createPackage(
                    id =
                        removedProviderId,
                    name =
                        "Core English",
                    version =
                        "2.0.0"
                )

            val remainingProvider =
                createPackage(
                    id =
                        remainingProviderId,
                    name =
                        "Core English",
                    version =
                        "3.0.0"
                )

            val dependentPackage =
                createPackage(
                    id =
                        dependentPackageId,
                    name =
                        "Vocabulary Extension",
                    version =
                        "1.0.0",
                    dependencies =
                        setOf(
                            PackageDependency(
                                packageName =
                                    "Core English",
                                minimumVersion =
                                    "2.0.0"
                            )
                        )
                )

            repositories.packageRepository.save(
                removedProvider
            )

            repositories.packageRepository.save(
                remainingProvider
            )

            repositories.packageRepository.save(
                dependentPackage
            )

            repositories.catalogRepository.save(
                PackageCatalog(
                    id =
                        catalogId,
                    packageIds =
                        setOf(
                            removedProviderId,
                            remainingProviderId,
                            dependentPackageId
                        )
                )
            )

            PersistedLearningPlatformFactory
                .createPersistedUninstaller(
                    directory
                )
                .execute(
                    UninstallContentPackageCommand(
                        catalogId =
                            catalogId,
                        packageId =
                            removedProviderId
                    )
                )

            val reopened =
                createRepositories(
                    directory
                )

            assertEquals(
                null,
                reopened.packageRepository.findById(
                    removedProviderId
                )
            )

            assertEquals(
                remainingProvider,
                reopened.packageRepository.findById(
                    remainingProviderId
                )
            )

            assertEquals(
                dependentPackage,
                reopened.packageRepository.findById(
                    dependentPackageId
                )
            )

            assertEquals(
                setOf(
                    remainingProviderId,
                    dependentPackageId
                ),
                assertNotNull(
                    reopened.catalogRepository.findById(
                        catalogId
                    )
                ).packageIds
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `persisted uninstall is rejected when remaining provider is outside required range`() {
        val directory =
            Files.createTempDirectory(
                "persisted-incompatible-provider-uninstall"
            )

        try {
            val repositories =
                createRepositories(
                    directory
                )

            val catalogId =
                PackageCatalogId(
                    "installed-packages"
                )

            val removedProviderId =
                PackageId(
                    "core-english-compatible"
                )

            val incompatibleProviderId =
                PackageId(
                    "core-english-incompatible"
                )

            val dependentPackageId =
                PackageId(
                    "vocabulary-extension"
                )

            val removedProvider =
                createPackage(
                    id =
                        removedProviderId,
                    name =
                        "Core English",
                    version =
                        "2.5.0"
                )

            val incompatibleProvider =
                createPackage(
                    id =
                        incompatibleProviderId,
                    name =
                        "Core English",
                    version =
                        "4.0.0"
                )

            val dependentPackage =
                createPackage(
                    id =
                        dependentPackageId,
                    name =
                        "Vocabulary Extension",
                    version =
                        "1.0.0",
                    dependencies =
                        setOf(
                            PackageDependency(
                                packageName =
                                    "Core English",
                                minimumVersion =
                                    "2.0.0",
                                maximumVersion =
                                    "3.0.0"
                            )
                        )
                )

            val originalCatalog =
                PackageCatalog(
                    id =
                        catalogId,
                    packageIds =
                        setOf(
                            removedProviderId,
                            incompatibleProviderId,
                            dependentPackageId
                        )
                )

            repositories.packageRepository.save(
                removedProvider
            )

            repositories.packageRepository.save(
                incompatibleProvider
            )

            repositories.packageRepository.save(
                dependentPackage
            )

            repositories.catalogRepository.save(
                originalCatalog
            )

            val exception =
                assertFailsWith<
                        PackageRequiredByInstalledPackagesException
                        > {
                    PersistedLearningPlatformFactory
                        .createPersistedUninstaller(
                            directory
                        )
                        .execute(
                            UninstallContentPackageCommand(
                                catalogId =
                                    catalogId,
                                packageId =
                                    removedProviderId
                            )
                        )
                }

            assertEquals(
                removedProviderId,
                exception.packageId
            )

            assertEquals(
                setOf(
                    dependentPackageId
                ),
                exception.dependentPackageIds
            )

            val reopened =
                createRepositories(
                    directory
                )

            assertEquals(
                removedProvider,
                reopened.packageRepository.findById(
                    removedProviderId
                )
            )

            assertEquals(
                incompatibleProvider,
                reopened.packageRepository.findById(
                    incompatibleProviderId
                )
            )

            assertEquals(
                dependentPackage,
                reopened.packageRepository.findById(
                    dependentPackageId
                )
            )

            assertEquals(
                originalCatalog,
                reopened.catalogRepository.findById(
                    catalogId
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun createRepositories(
        directory: Path
    ): Repositories {
        val app = vn.loi.learning.infrastructure.LearningApplicationFactory.createPersisted(directory, false)
        return Repositories(
            packageRepository = app.contentPackageRepository!!,
            catalogRepository = app.packageCatalog!!
        )
    }

    private fun createPackage(
        id: PackageId,
        name: String,
        version: String,
        dependencies: Set<PackageDependency> =
            emptySet()
    ): ContentPackage =
        ContentPackage(
            id =
                id,
            descriptor =
                PackageDescriptor(
                    name =
                        name,
                    version =
                        version,
                    format =
                        "OPD3",
                    dependencies =
                        dependencies
                )
        )

    private data class Repositories(
        val packageRepository: vn.loi.learning.application.port.ContentPackageRepository,
        val catalogRepository: vn.loi.learning.application.port.PackageCatalogRepository
    )
}