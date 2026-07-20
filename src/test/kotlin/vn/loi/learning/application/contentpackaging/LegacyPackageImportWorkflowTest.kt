package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.contentpackaging.Sha256PackageIdGenerator
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class LegacyPackageImportWorkflowTest {

    @Test
    fun `workflow scans and imports all legacy candidates`() {
        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        val candidates =
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

        val workflow =
            LegacyPackageImportWorkflow(
                packageScanner =
                    LegacyPackageScanner {
                        candidates
                    },
                packageImportService =
                    LegacyPackageImportService(
                        packageContentImporter =
                            LegacyPackageContentImporter { candidate ->
                                ImportedPackageContent(
                                    contents =
                                        emptyList(),
                                    learningItems =
                                        emptyList(),
                                    warnings =
                                        listOf(
                                            "Imported ${candidate.jsonSource}"
                                        )
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
            )

        val result =
            workflow.execute(
                PackageCatalogId(
                    "legacy-catalog"
                )
            )

        assertEquals(
            2,
            result.scannedCandidateCount
        )

        assertEquals(
            2,
            result.importedPackageCount
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
                "Imported C:/packages/2000Cau.json",
                "Imported C:/packages/ShortStories.json"
            ),
            result.warnings
        )

        assertEquals(
            listOf(
                "2000Cau",
                "ShortStories"
            ),
            result.importedPackages.map { packageResult ->
                packageResult.contentPackage.name
            }
        )

        assertEquals(
            2,
            contentPackageRepository.count()
        )
    }

    @Test
    fun `workflow returns empty result when directory contains no candidates`() {
        val workflow =
            LegacyPackageImportWorkflow(
                packageScanner =
                    LegacyPackageScanner {
                        emptyList()
                    },
                packageImportService =
                    LegacyPackageImportService(
                        packageContentImporter =
                            LegacyPackageContentImporter {
                                error(
                                    "Importer must not be called."
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
                                    InMemoryContentPackageRepository(),
                                packageCatalogRepository =
                                    InMemoryPackageCatalogRepository()
                            ),
                        transactionRunner =
                            InMemoryTransactionRunner()
                    )
            )

        val result =
            workflow.execute(
                PackageCatalogId(
                    "empty-catalog"
                )
            )

        assertEquals(
            0,
            result.scannedCandidateCount
        )

        assertEquals(
            0,
            result.importedPackageCount
        )

        assertEquals(
            emptyList(),
            result.warnings
        )
    }
}