package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class InstalledPackageLibraryLookupTest {

    @Test
    fun `finds package containing library`() {
        val service =
            serviceWithPackages()

        val result =
            service.findByLibraryId(
                "medical-core-library"
            )

        assertEquals(
            "medical-package",
            result?.id
        )
    }

    @Test
    fun `trims library id before lookup`() {
        val service =
            serviceWithPackages()

        val result =
            service.findByLibraryId(
                "  conversation-library  "
            )

        assertEquals(
            "conversation-package",
            result?.id
        )
    }

    @Test
    fun `returns null when library is not installed`() {
        val service =
            serviceWithPackages()

        assertNull(
            service.findByLibraryId(
                "missing-library"
            )
        )
    }

    @Test
    fun `returns null for blank library id`() {
        val service =
            serviceWithPackages()

        assertNull(
            service.findByLibraryId(
                " "
            )
        )
    }

    @Test
    fun `queries packages containing any requested library`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByLibraryIds(
                listOf(
                    "conversation-library",
                    "medical-core-library"
                )
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
    fun `duplicate and blank library ids do not duplicate results`() {
        val service =
            serviceWithPackages()

        val result =
            service.queryByLibraryIds(
                listOf(
                    " ",
                    "medical-library",
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
    fun `empty library selection returns empty list`() {
        val service =
            serviceWithPackages()

        assertTrue(
            service.queryByLibraryIds(
                emptyList()
            ).isEmpty()
        )
    }

    @Test
    fun `installed item reports exact library membership`() {
        val item =
            serviceWithPackages()
                .findById(
                    "medical-package"
                )!!

        assertTrue(
            item.containsLibrary(
                "medical-library"
            )
        )

        assertEquals(
            false,
            item.containsLibrary(
                "MEDICAL-LIBRARY"
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

        repository.save(
            contentPackage(
                id =
                    "empty-package",
                name =
                    "Empty Package",
                version =
                    "1.0.0",
                libraryIds =
                    emptySet()
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
        libraryIds: Set<String>
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