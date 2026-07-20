package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class PackageInstallationDependencyGuardTest {

    @Test
    fun `package without dependencies can be installed`() {
        val repository =
            InMemoryContentPackageRepository()

        PackageInstallationDependencyGuard(
            contentPackageRepository =
                repository
        ).ensureCanInstall(
            contentPackage(
                id =
                    "standalone-package",
                name =
                    "Standalone Package",
                version =
                    "1.0.0"
            )
        )
    }

    @Test
    fun `package can be installed when compatible dependency exists`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "core-package",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )
        )

        PackageInstallationDependencyGuard(
            contentPackageRepository =
                repository
        ).ensureCanInstall(
            contentPackage(
                id =
                    "extension-package",
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
                                "1.0.0",
                            maximumVersion =
                                "2.0.0"
                        )
                    )
            )
        )
    }

    @Test
    fun `missing dependency is rejected`() {
        val repository =
            InMemoryContentPackageRepository()

        val extensionPackage =
            contentPackage(
                id =
                    "extension-package",
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
                                "1.0.0"
                        )
                    )
            )

        val exception =
            assertFailsWith<MissingPackageDependenciesException> {
                PackageInstallationDependencyGuard(
                    contentPackageRepository =
                        repository
                ).ensureCanInstall(
                    extensionPackage
                )
            }

        assertEquals(
            extensionPackage.id,
            exception.packageId
        )

        assertEquals(
            extensionPackage.descriptor.dependencies,
            exception.missingDependencies
        )
    }

    @Test
    fun `dependency below minimum version is rejected`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "core-package",
                name =
                    "Core English",
                version =
                    "1.0.0"
            )
        )

        val dependency =
            PackageDependency(
                packageName =
                    "Core English",
                minimumVersion =
                    "2.0.0"
            )

        val exception =
            assertFailsWith<MissingPackageDependenciesException> {
                PackageInstallationDependencyGuard(
                    contentPackageRepository =
                        repository
                ).ensureCanInstall(
                    contentPackage(
                        id =
                            "extension-package",
                        name =
                            "Vocabulary Extension",
                        version =
                            "1.0.0",
                        dependencies =
                            setOf(
                                dependency
                            )
                    )
                )
            }

        assertEquals(
            setOf(
                dependency
            ),
            exception.missingDependencies
        )
    }

    @Test
    fun `dependency above maximum version is rejected`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "core-package",
                name =
                    "Core English",
                version =
                    "3.0.0"
            )
        )

        val dependency =
            PackageDependency(
                packageName =
                    "Core English",
                maximumVersion =
                    "2.0.0"
            )

        val exception =
            assertFailsWith<MissingPackageDependenciesException> {
                PackageInstallationDependencyGuard(
                    contentPackageRepository =
                        repository
                ).ensureCanInstall(
                    contentPackage(
                        id =
                            "extension-package",
                        name =
                            "Vocabulary Extension",
                        version =
                            "1.0.0",
                        dependencies =
                            setOf(
                                dependency
                            )
                    )
                )
            }

        assertEquals(
            setOf(
                dependency
            ),
            exception.missingDependencies
        )
    }

    @Test
    fun `all declared dependencies must be satisfied`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "core-package",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )
        )

        val missingDependency =
            PackageDependency(
                packageName =
                    "Pronunciation Pack",
                minimumVersion =
                    "1.0.0"
            )

        val extensionPackage =
            contentPackage(
                id =
                    "extension-package",
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
                                "1.0.0"
                        ),
                        missingDependency
                    )
            )

        val exception =
            assertFailsWith<MissingPackageDependenciesException> {
                PackageInstallationDependencyGuard(
                    contentPackageRepository =
                        repository
                ).ensureCanInstall(
                    extensionPackage
                )
            }

        assertEquals(
            setOf(
                missingDependency
            ),
            exception.missingDependencies
        )
    }

    private fun contentPackage(
        id: String,
        name: String,
        version: String,
        dependencies: Set<PackageDependency> =
            emptySet()
    ): ContentPackage =
        ContentPackage(
            id =
                PackageId(
                    id
                ),
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
}