package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class InstalledPackageQueryBoundaryTest {

    @Test
    fun `find by id trims surrounding whitespace`() {
        val service =
            serviceWithPackages()

        val result =
            service.findById(
                "  medical-package  "
            )

        assertEquals(
            "Medical English",
            result?.name
        )
    }

    @Test
    fun `find by id remains case sensitive`() {
        val service =
            serviceWithPackages()

        assertNull(
            service.findById(
                "MEDICAL-PACKAGE"
            )
        )
    }

    @Test
    fun `library lookup remains case sensitive`() {
        val service =
            serviceWithPackages()

        assertNull(
            service.findByLibraryId(
                "MEDICAL-LIBRARY"
            )
        )
    }

    @Test
    fun `library collection query ignores blank values`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByLibraryIds(
                listOf(
                    "",
                    " ",
                    "\t",
                    "medical-library"
                )
            )

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
    fun `unknown library collection query returns empty list`() {
        val service =
            serviceWithPackages()

        assertTrue(
            service.queryByLibraryIds(
                listOf(
                    "missing-library-a",
                    "missing-library-b"
                )
            ).isEmpty()
        )
    }

    @Test
    fun `format query returns empty list for unknown format`() {
        val service =
            serviceWithPackages()

        assertTrue(
            service.queryByFormat(
                "UNKNOWN"
            ).isEmpty()
        )
    }

    @Test
    fun `search trims surrounding whitespace`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                "  medical  "
            )

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
    fun `search matches partial library id without case sensitivity`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                "CORE-LIB"
            )

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
    fun `repository changes are visible to later queries`() {
        val repository =
            InMemoryContentPackageRepository()

        val service =
            InstalledPackageQueryService(
                contentPackageRepository =
                    repository
            )

        assertTrue(
            service.query().isEmpty()
        )

        repository.save(
            contentPackage(
                id =
                    "new-package",
                name =
                    "New Package",
                version =
                    "1.0.0",
                libraryIds =
                    setOf(
                        "new-library"
                    )
            )
        )

        assertEquals(
            listOf(
                "new-package"
            ),
            service.query().map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `packages matching multiple requested libraries appear once`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByLibraryIds(
                listOf(
                    "medical-library",
                    "medical-core-library"
                )
            )

        assertEquals(
            listOf(
                "medical-package"
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
                libraryIds =
                    setOf(
                        "medical-library",
                        "medical-core-library"
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
                libraryIds =
                    setOf(
                        "conversation-library"
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
}