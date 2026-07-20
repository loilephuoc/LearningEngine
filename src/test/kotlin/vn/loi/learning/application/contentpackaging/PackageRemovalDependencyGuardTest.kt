package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class PackageRemovalDependencyGuardTest {

    @Test
    fun `allows removing package that is not required`() {
        val repository =
            InMemoryContentPackageRepository()

        val removablePackage =
            contentPackage(
                id =
                    "optional-package",
                name =
                    "Optional Package",
                version =
                    "1.0.0"
            )

        repository.save(
            removablePackage
        )

        PackageRemovalDependencyGuard(
            contentPackageRepository =
                repository
        ).ensureCanRemove(
            removablePackage.id
        )
    }

    @Test
    fun `rejects removing the only compatible dependency`() {
        val repository =
            InMemoryContentPackageRepository()

        val corePackage =
            contentPackage(
                id =
                    "core-package-1",
                name =
                    "Core English",
                version =
                    "1.5.0"
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
                                "1.0.0",
                            maximumVersion =
                                "2.0.0"
                        )
                    )
            )

        repository.save(
            corePackage
        )

        repository.save(
            extensionPackage
        )

        val exception =
            assertFailsWith<PackageRequiredByInstalledPackagesException> {
                PackageRemovalDependencyGuard(
                    contentPackageRepository =
                        repository
                ).ensureCanRemove(
                    corePackage.id
                )
            }

        assertEquals(
            corePackage.id,
            exception.packageId
        )

        assertEquals(
            setOf(
                extensionPackage.id
            ),
            exception.dependentPackageIds
        )
    }

    @Test
    fun `allows removing dependency when compatible alternative remains`() {
        val repository =
            InMemoryContentPackageRepository()

        val removedPackage =
            contentPackage(
                id =
                    "core-package-1",
                name =
                    "Core English",
                version =
                    "1.0.0"
            )

        val alternativePackage =
            contentPackage(
                id =
                    "core-package-2",
                name =
                    "Core English",
                version =
                    "1.5.0"
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
                                "1.0.0",
                            maximumVersion =
                                "2.0.0"
                        )
                    )
            )

        repository.save(
            removedPackage
        )

        repository.save(
            alternativePackage
        )

        repository.save(
            extensionPackage
        )

        PackageRemovalDependencyGuard(
            contentPackageRepository =
                repository
        ).ensureCanRemove(
            removedPackage.id
        )
    }

    @Test
    fun `allows removing dependency when compatible additional package will replace it`() {
        val repository =
            InMemoryContentPackageRepository()

        val removedPackage =
            contentPackage(
                id =
                    "core-package-1",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )

        val replacementPackage =
            contentPackage(
                id =
                    "core-package-2",
                name =
                    "Core English",
                version =
                    "2.0.0"
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
                                "1.0.0",
                            maximumVersion =
                                "2.5.0"
                        )
                    )
            )

        repository.save(
            removedPackage
        )

        repository.save(
            extensionPackage
        )

        PackageRemovalDependencyGuard(
            contentPackageRepository =
                repository
        ).ensureCanRemove(
            packageId =
                removedPackage.id,
            additionalPackages =
                listOf(
                    replacementPackage
                )
        )
    }

    @Test
    fun `rejects removal when additional replacement package is incompatible`() {
        val repository =
            InMemoryContentPackageRepository()

        val removedPackage =
            contentPackage(
                id =
                    "core-package-1",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )

        val incompatibleReplacement =
            contentPackage(
                id =
                    "core-package-2",
                name =
                    "Core English",
                version =
                    "3.0.0"
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
                                "1.0.0",
                            maximumVersion =
                                "2.0.0"
                        )
                    )
            )

        repository.save(
            removedPackage
        )

        repository.save(
            extensionPackage
        )

        val exception =
            assertFailsWith<PackageRequiredByInstalledPackagesException> {
                PackageRemovalDependencyGuard(
                    contentPackageRepository =
                        repository
                ).ensureCanRemove(
                    packageId =
                        removedPackage.id,
                    additionalPackages =
                        listOf(
                            incompatibleReplacement
                        )
                )
            }

        assertEquals(
            removedPackage.id,
            exception.packageId
        )

        assertEquals(
            setOf(
                extensionPackage.id
            ),
            exception.dependentPackageIds
        )
    }

    @Test
    fun `rejects removal when remaining alternative is incompatible`() {
        val repository =
            InMemoryContentPackageRepository()

        val removedPackage =
            contentPackage(
                id =
                    "core-package-2",
                name =
                    "Core English",
                version =
                    "2.0.0"
            )

        val incompatibleAlternative =
            contentPackage(
                id =
                    "core-package-1",
                name =
                    "Core English",
                version =
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
                                "2.0.0"
                        )
                    )
            )

        repository.save(
            removedPackage
        )

        repository.save(
            incompatibleAlternative
        )

        repository.save(
            extensionPackage
        )

        val exception =
            assertFailsWith<PackageRequiredByInstalledPackagesException> {
                PackageRemovalDependencyGuard(
                    contentPackageRepository =
                        repository
                ).ensureCanRemove(
                    removedPackage.id
                )
            }

        assertEquals(
            setOf(
                extensionPackage.id
            ),
            exception.dependentPackageIds
        )
    }

    @Test
    fun `ignores dependency with different package name`() {
        val repository =
            InMemoryContentPackageRepository()

        val removedPackage =
            contentPackage(
                id =
                    "grammar-package",
                name =
                    "Grammar Package",
                version =
                    "1.0.0"
            )

        val extensionPackage =
            contentPackage(
                id =
                    "vocabulary-extension",
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

        repository.save(
            removedPackage
        )

        repository.save(
            extensionPackage
        )

        PackageRemovalDependencyGuard(
            contentPackageRepository =
                repository
        ).ensureCanRemove(
            removedPackage.id
        )
    }

    @Test
    fun `missing package can be removed idempotently`() {
        PackageRemovalDependencyGuard(
            contentPackageRepository =
                InMemoryContentPackageRepository()
        ).ensureCanRemove(
            PackageId(
                "missing-package"
            )
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