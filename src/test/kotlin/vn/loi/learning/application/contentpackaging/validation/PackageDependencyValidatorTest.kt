package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class PackageDependencyValidatorTest {

    @Test
    fun `installed compatible dependency is accepted`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "core-package-1",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )
        )

        val report =
            PackageDependencyValidator(
                contentPackageRepository =
                    repository
            ).validate(
                descriptor(
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

        assertTrue(
            report.isValid
        )
    }

    @Test
    fun `missing dependency is rejected`() {
        val report =
            PackageDependencyValidator(
                contentPackageRepository =
                    InMemoryContentPackageRepository()
            ).validate(
                descriptor(
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
            )

        assertEquals(
            "MISSING_PACKAGE_DEPENDENCY",
            report.errors.single().code
        )
    }

    @Test
    fun `installed incompatible dependency is rejected`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "core-package-1",
                name =
                    "Core English",
                version =
                    "1.0.0"
            )
        )

        val report =
            PackageDependencyValidator(
                contentPackageRepository =
                    repository
            ).validate(
                descriptor(
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
            )

        assertEquals(
            "INCOMPATIBLE_PACKAGE_DEPENDENCY",
            report.errors.single().code
        )
    }

    @Test
    fun `invalid dependency range is rejected`() {
        val report =
            PackageDependencyValidator(
                contentPackageRepository =
                    InMemoryContentPackageRepository()
            ).validate(
                descriptor(
                    dependencies =
                        setOf(
                            PackageDependency(
                                packageName =
                                    "Core English",
                                minimumVersion =
                                    "3.0.0",
                                maximumVersion =
                                    "2.0.0"
                            )
                        )
                )
            )

        assertEquals(
            "INVALID_DEPENDENCY_VERSION_RANGE",
            report.errors.single().code
        )
    }

    private fun descriptor(
        dependencies: Set<PackageDependency>
    ): PackageDescriptor =
        PackageDescriptor(
            name =
                "Vocabulary Extension",
            version =
                "1.0.0",
            format =
                "OPD3",
            dependencies =
                dependencies
        )

    private fun contentPackage(
        id: String,
        name: String,
        version: String
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
                        "OPD3"
                )
        )
}