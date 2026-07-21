package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class InstalledPackageMetadataFilterTest {

    @Test
    fun `queries packages by format without case sensitivity`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByFormat(
                "opd3"
            )

        assertEquals(
            listOf(
                "conversation-package",
                "medical-package"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `trims format before filtering`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByFormat(
                "  legacy-json  "
            )

        assertEquals(
            listOf(
                "legacy-package"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `blank format returns empty list`() {
        val service =
            serviceWithPackages()

        assertTrue(
            service.queryByFormat(
                " "
            ).isEmpty()
        )
    }

    @Test
    fun `queries packages containing libraries`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryWithLibraries()

        assertEquals(
            listOf(
                "conversation-package",
                "medical-package"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `queries packages without libraries`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryWithoutLibraries()

        assertEquals(
            listOf(
                "legacy-package"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `queries packages containing dependencies`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryWithDependencies()

        assertEquals(
            listOf(
                "medical-package"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `queries packages without dependencies`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryWithoutDependencies()

        assertEquals(
            listOf(
                "conversation-package",
                "legacy-package"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    private fun serviceWithPackages():
            InstalledPackageQueryService {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "medical-package",
                name =
                    "Medical English",
                version =
                    "2.5.0",
                format =
                    "OPD3",
                libraryIds =
                    setOf(
                        "medical-library"
                    ),
                dependencies =
                    setOf(
                        PackageDependency(
                            packageName =
                                "medical-core",
                            minimumVersion =
                                "2.0.0"
                        )
                    )
            )
        )

        repository.save(
            contentPackage(
                id =
                    "conversation-package",
                name =
                    "Conversations",
                version =
                    "1.0.0",
                format =
                    "OPD3",
                libraryIds =
                    setOf(
                        "conversation-library"
                    )
            )
        )

        repository.save(
            contentPackage(
                id =
                    "legacy-package",
                name =
                    "Legacy Vocabulary",
                version =
                    "1.0.0",
                format =
                    "LEGACY-JSON"
            )
        )

        return InstalledPackageQueryService(
            contentPackageRepository =
                repository
        )
    }

    private fun contentPackage(
        id: String,
        name: String,
        version: String,
        format: String,
        libraryIds: Set<String> =
            emptySet(),
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
                        format,
                    dependencies =
                        dependencies
                ),
            libraryIds =
                libraryIds.mapTo(
                    linkedSetOf()
                ) { libraryId ->
                    ContentLibraryId(
                        libraryId
                    )
                }
        )
}