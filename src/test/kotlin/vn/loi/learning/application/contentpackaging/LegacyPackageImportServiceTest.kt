package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.contentpackaging.Sha256PackageIdGenerator
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class LegacyPackageImportServiceTest {

    @Test
    fun `service imports and registers legacy package`() {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        val registrationOperation =
            PackageRegistrationOperation(
                contentPackageRepository =
                    contentPackageRepository,
                packageCatalogRepository =
                    packageCatalogRepository
            )

        val service =
            LegacyPackageImportService(
                packageContentImporter =
                    LegacyPackageContentImporter {
                        ImportedPackageContent(
                            contents = emptyList(),
                            learningItems = emptyList(),
                            warnings =
                                listOf(
                                    "Legacy warning"
                                )
                        )
                    },
                packageIdGenerator =
                    Sha256PackageIdGenerator(),
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                packageRegistrationOperation =
                    registrationOperation,
                transactionRunner =
                    InMemoryTransactionRunner()
            )

        val catalogId =
            PackageCatalogId(
                "legacy-catalog"
            )

        val result =
            service.importCandidate(
                catalogId = catalogId,
                candidate =
                    LegacyPackageCandidate(
                        jsonSource =
                            "C:/packages/2000Cau.json",
                        mediaSource =
                            "C:/packages/2000Cau.pkg"
                    )
            )

        assertEquals(
            "2000Cau",
            result.contentPackage.name
        )

        assertEquals(
            "1",
            result.contentPackage.version
        )

        assertEquals(
            "LEGACY_OPD3",
            result.contentPackage.format
        )

        assertEquals(
            0,
            result.importedContentCount
        )

        assertEquals(
            0,
            result.importedLearningItemCount
        )

        assertEquals(
            listOf(
                "Legacy warning"
            ),
            result.warnings
        )

        assertEquals(
            1,
            contentPackageRepository.count()
        )

        val catalog =
            packageCatalogRepository.findById(
                catalogId
            )

        assertNotNull(
            catalog
        )

        assertTrue(
            result.contentPackage.id in
                    catalog.packageIds
        )
    }

    @Test
    fun `service imports all candidates in stable input order`() {
        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        val service =
            LegacyPackageImportService(
                packageContentImporter =
                    LegacyPackageContentImporter {
                        ImportedPackageContent(
                            contents = emptyList(),
                            learningItems = emptyList()
                        )
                    },
                packageIdGenerator =
                    Sha256PackageIdGenerator(),
                contentRepository =
                    InMemoryContentRepository(),
                learningItemRepository =
                    InMemoryLearningItemRepository(),
                packageRegistrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository =
                            contentPackageRepository,
                        packageCatalogRepository =
                            packageCatalogRepository
                    ),
                transactionRunner =
                    InMemoryTransactionRunner()
            )

        val results =
            service.importAll(
                catalogId =
                    PackageCatalogId(
                        "legacy-catalog"
                    ),
                candidates =
                    listOf(
                        LegacyPackageCandidate(
                            jsonSource =
                                "C:/packages/2000Cau.json",
                            mediaSource =
                                "C:/packages/2000Cau.pkg"
                        ),
                        LegacyPackageCandidate(
                            jsonSource =
                                "C:/packages/ShortStories.json",
                            mediaSource =
                                "C:/packages/ShortStories.pkg"
                        )
                    )
            )

        assertEquals(
            listOf(
                "2000Cau",
                "ShortStories"
            ),
            results.map { result ->
                result.contentPackage.name
            }
        )

        assertEquals(
            2,
            contentPackageRepository.count()
        )
    }
}