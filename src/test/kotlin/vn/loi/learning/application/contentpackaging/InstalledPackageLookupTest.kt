package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class InstalledPackageLookupTest {

    @Test
    fun `finds installed package by exact id`() {
        val service =
            serviceWithPackages()

        val result =
            service.findById(
                "medical-package"
            )

        assertEquals(
            "Medical English",
            result?.name
        )
    }

    @Test
    fun `returns null for missing package id`() {
        val service =
            serviceWithPackages()

        assertNull(
            service.findById(
                "missing-package"
            )
        )
    }

    @Test
    fun `returns null for blank package id`() {
        val service =
            serviceWithPackages()

        assertNull(
            service.findById(
                " "
            )
        )
    }

    @Test
    fun `searches package name without case sensitivity`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                "MEDICAL"
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
    fun `searches package id`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                "conversation-package"
            )

        assertEquals(
            listOf(
                "conversation-package"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `searches associated library id`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                "medical-library"
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
    fun `searches package version`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                "2.5.0"
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
    fun `blank search returns complete ordered index`() {
        val service =
            serviceWithPackages()

        val result =
            service.search(
                " "
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
    fun `search with no matches returns empty list`() {
        val service =
            serviceWithPackages()

        assertEquals(
            emptyList(),
            service.search(
                "not-installed"
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
                libraryId =
                    "medical-library"
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
                libraryId =
                    "conversation-library"
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
        libraryId: String
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
                setOf(
                    ContentLibraryId(
                        libraryId
                    )
                )
        )
}