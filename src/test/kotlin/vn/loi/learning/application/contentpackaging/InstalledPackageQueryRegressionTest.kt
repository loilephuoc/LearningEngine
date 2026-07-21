package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class InstalledPackageQueryRegressionTest {

    @Test
    fun `repeated query returns equal snapshots`() {
        val service =
            serviceWithPackages()

        val first =
            service.query()

        val second =
            service.query()

        assertEquals(
            first,
            second
        )

        assertNotSame(
            first,
            second
        )
    }

    @Test
    fun `search preserves package index ordering`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                "OPD3"
            )

        assertEquals(
            listOf(
                "package-a",
                "package-c",
                "package-b"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `format filtering preserves package index ordering`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByFormat(
                "opd3"
            )

        assertEquals(
            listOf(
                "package-a",
                "package-c",
                "package-b"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `library filtering preserves package index ordering`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByLibraryIds(
                listOf(
                    "library-b",
                    "library-a",
                    "library-c"
                )
            )

        assertEquals(
            listOf(
                "package-a",
                "package-c",
                "package-b"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `dependency filtering preserves package index ordering`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryWithDependencies()

        assertEquals(
            listOf(
                "package-a",
                "package-b"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `projected library ids remain sorted`() {
        val service =
            serviceWithPackages()

        val item =
            service.findById(
                "package-a"
            )!!

        assertEquals(
            listOf(
                "library-a",
                "library-z"
            ),
            item.libraryIds
        )
    }

    private fun serviceWithPackages():
            InstalledPackageQueryService {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "package-b",
                name =
                    "Vocabulary",
                version =
                    "2.0.0",
                libraryIds =
                    setOf(
                        "library-b"
                    ),
                dependencies =
                    setOf(
                        dependency(
                            "core-b"
                        )
                    )
            )
        )

        repository.save(
            contentPackage(
                id =
                    "package-c",
                name =
                    "Vocabulary",
                version =
                    "1.0.0",
                libraryIds =
                    setOf(
                        "library-c"
                    )
            )
        )

        repository.save(
            contentPackage(
                id =
                    "package-a",
                name =
                    "Vocabulary",
                version =
                    "1.0.0",
                libraryIds =
                    linkedSetOf(
                        "library-z",
                        "library-a"
                    ),
                dependencies =
                    setOf(
                        dependency(
                            "core-a"
                        )
                    )
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
        libraryIds: Set<String>,
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

    private fun dependency(
        packageName: String
    ): PackageDependency =
        PackageDependency(
            packageName =
                packageName,
            minimumVersion =
                "1.0.0"
        )
}