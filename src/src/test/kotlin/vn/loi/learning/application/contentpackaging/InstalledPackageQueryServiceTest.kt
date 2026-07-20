package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository

class InstalledPackageQueryServiceTest {

    @Test
    fun `returns only catalog packages in display order`() {
        val catalogId = PackageCatalogId("installed-packages")
        val englishId = PackageId("english-package")
        val conversationId = PackageId("conversation-package")
        val orphanId = PackageId("orphan-package")
        val packageRepository = InMemoryContentPackageRepository()
        val catalogRepository = InMemoryPackageCatalogRepository()

        packageRepository.save(
            ContentPackage(
                id = englishId,
                descriptor = PackageDescriptor(
                    name = "Vocabulary",
                    version = "1.0.0",
                    format = "OPD3"
                )
            )
        )
        packageRepository.save(
            ContentPackage(
                id = conversationId,
                descriptor = PackageDescriptor(
                    name = "Conversations",
                    version = "2.0.0",
                    format = "OPD3"
                )
            )
        )
        packageRepository.save(
            ContentPackage(
                id = orphanId,
                descriptor = PackageDescriptor(
                    name = "Orphan",
                    version = "1.0.0",
                    format = "OPD3"
                )
            )
        )
        catalogRepository.save(
            PackageCatalog(
                id = catalogId,
                packageIds = setOf(englishId, conversationId)
            )
        )

        val result =
            InstalledPackageQueryService(
                contentPackageRepository = packageRepository,
                packageCatalogRepository = catalogRepository
            ).findAll(catalogId)

        assertEquals(
            listOf(conversationId, englishId),
            result.map(InstalledPackageSummary::id)
        )
        assertEquals(
            listOf("Conversations", "Vocabulary"),
            result.map(InstalledPackageSummary::name)
        )
        assertEquals(0, result.sumOf(InstalledPackageSummary::libraryCount))
    }
}
