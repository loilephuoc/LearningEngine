package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageImportServiceDeterminismTest {

    @Test
    fun `import all is deterministic across repeated executions`() {

        fun createService(): PackageImportService {

            val candidateA =
                PackageScanCandidate("A.opd3")

            val candidateB =
                PackageScanCandidate("B.opd3")

            return PackageImportService(
                packageScanner =
                    PackageScanner {
                        listOf(candidateA, candidateB)
                    },
                packageInstaller =
                    PackageInstaller { candidate ->
                        ContentPackage(
                            id = PackageId(candidate.source),
                            descriptor =
                                PackageDescriptor(
                                    name = candidate.source,
                                    version = "1.0.0",
                                    format = "OPD3"
                                )
                        )
                    },
                packageContentImporter =
                    PackageContentImporter { candidate ->

                        val content =
                            Content(
                                id = ContentId(candidate.source),
                                type = ContentType.WORD,
                                text =
                                    ContentText(
                                        primaryText = candidate.source,
                                        translatedText = candidate.source
                                    )
                            )

                        ImportedPackageContent(
                            contents =
                                listOf(content),
                            learningItems =
                                listOf(
                                    LearningItem(
                                        id =
                                            LearningItemId(
                                                candidate.source
                                            ),
                                        contentId =
                                            content.id,
                                        mode =
                                            LearningMode.MEANING_RECOGNITION
                                    )
                                ),
                            libraries =
                                listOf(
                                    ContentLibrary(
                                        id =
                                            ContentLibraryId(
                                                candidate.source
                                            ),
                                        descriptor =
                                            LibraryDescriptor(
                                                candidate.source
                                            ),
                                        contentIds =
                                            setOf(
                                                content.id
                                            )
                                    )
                                )
                        )
                    },
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
        }

        val first =
            createService()
                .importAll(
                    PackageCatalogId("catalog")
                )
                .map {
                    it.contentPackage.id.value
                }

        val second =
            createService()
                .importAll(
                    PackageCatalogId("catalog")
                )
                .map {
                    it.contentPackage.id.value
                }

        assertEquals(first, second)
        assertEquals(
            listOf(
                "A.opd3",
                "B.opd3"
            ),
            first
        )
    }
}