package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class InstalledPackageQueryServiceTest {

    @Test
    fun `returns installed packages as presentation items ordered by name`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            ContentPackage(
                id = PackageId("package-vocabulary"),
                descriptor =
                    PackageDescriptor(
                        name = "Vocabulary",
                        version = "2.0.0",
                        format = "OPD3"
                    ),
                libraryIds =
                    setOf(
                        ContentLibraryId("vocabulary-library")
                    )
            )
        )

        repository.save(
            ContentPackage(
                id = PackageId("package-conversations"),
                descriptor =
                    PackageDescriptor(
                        name = "Conversations",
                        version = "1.0.0",
                        format = "OPD3"
                    ),
                libraryIds =
                    setOf(
                        ContentLibraryId("conversation-library-1"),
                        ContentLibraryId("conversation-library-2")
                    )
            )
        )

        val service =
            InstalledPackageQueryService(
                contentPackageRepository = repository
            )

        val result =
            service.query()

        assertEquals(
            2,
            result.size
        )

        assertEquals(
            InstalledPackageItem(
                id = "package-conversations",
                name = "Conversations",
                version = "1.0.0",
                format = "OPD3",
                libraryCount = 2
            ),
            result[0]
        )

        assertEquals(
            InstalledPackageItem(
                id = "package-vocabulary",
                name = "Vocabulary",
                version = "2.0.0",
                format = "OPD3",
                libraryCount = 1
            ),
            result[1]
        )
    }

    @Test
    fun `returns empty list when no package is installed`() {
        val service =
            InstalledPackageQueryService(
                contentPackageRepository =
                    InMemoryContentPackageRepository()
            )

        assertEquals(
            emptyList(),
            service.query()
        )
    }
}