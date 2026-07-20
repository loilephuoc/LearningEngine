package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.validation.InvalidPackageException
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

class PackageImportServiceMixedFailureTest {

    @Test
    fun `import all stops at first invalid package and keeps earlier successful package`() {
        val firstCandidate =
            PackageScanCandidate(
                source = "C:/packages/first.opd3"
            )

        val invalidCandidate =
            PackageScanCandidate(
                source = "C:/packages/invalid.opd3"
            )

        val thirdCandidate =
            PackageScanCandidate(
                source = "C:/packages/third.opd3"
            )

        val firstPackage =
            ContentPackage(
                id = PackageId("package-first"),
                descriptor =
                    PackageDescriptor(
                        name = "First Package",
                        version = "1.0.0",
                        format = "OPD3"
                    )
            )

        val invalidPackage =
            ContentPackage(
                id = PackageId("package-invalid"),
                descriptor =
                    PackageDescriptor(
                        name = "Invalid Package",
                        version = "1.0.0",
                        format = "OPD3"
                    )
            )

        val thirdPackage =
            ContentPackage(
                id = PackageId("package-third"),
                descriptor =
                    PackageDescriptor(
                        name = "Third Package",
                        version = "1.0.0",
                        format = "OPD3"
                    )
            )

        val firstContent =
            Content(
                id = ContentId("content-first"),
                type = ContentType.WORD,
                text =
                    ContentText(
                        primaryText = "first",
                        translatedText = "thu nhat"
                    )
            )

        val firstLearningItem =
            LearningItem(
                id = LearningItemId("item-first"),
                contentId = firstContent.id,
                mode = LearningMode.MEANING_RECOGNITION
            )

        val firstLibrary =
            ContentLibrary(
                id = ContentLibraryId("library-first"),
                descriptor = LibraryDescriptor("First Library"),
                contentIds = setOf(firstContent.id)
            )

        val missingContentId =
            ContentId("missing-content")

        val invalidLearningItem =
            LearningItem(
                id = LearningItemId("item-invalid"),
                contentId = missingContentId,
                mode = LearningMode.MEANING_RECOGNITION
            )

        val thirdContent =
            Content(
                id = ContentId("content-third"),
                type = ContentType.WORD,
                text =
                    ContentText(
                        primaryText = "third",
                        translatedText = "thu ba"
                    )
            )

        val thirdLearningItem =
            LearningItem(
                id = LearningItemId("item-third"),
                contentId = thirdContent.id,
                mode = LearningMode.MEANING_RECOGNITION
            )

        val thirdLibrary =
            ContentLibrary(
                id = ContentLibraryId("library-third"),
                descriptor = LibraryDescriptor("Third Library"),
                contentIds = setOf(thirdContent.id)
            )

        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        val installedSources =
            mutableListOf<String>()

        val importedSources =
            mutableListOf<String>()

        val service =
            PackageImportService(
                packageScanner =
                    PackageScanner {
                        listOf(
                            firstCandidate,
                            invalidCandidate,
                            thirdCandidate
                        )
                    },
                packageInstaller =
                    PackageInstaller { candidate ->
                        installedSources += candidate.source

                        when (candidate) {
                            firstCandidate ->
                                firstPackage

                            invalidCandidate ->
                                invalidPackage

                            thirdCandidate ->
                                thirdPackage

                            else ->
                                error(
                                    "Unexpected candidate: ${candidate.source}"
                                )
                        }
                    },
                packageContentImporter =
                    PackageContentImporter { candidate ->
                        importedSources += candidate.source

                        when (candidate) {
                            firstCandidate ->
                                ImportedPackageContent(
                                    contents = listOf(firstContent),
                                    learningItems = listOf(firstLearningItem),
                                    libraries = listOf(firstLibrary)
                                )

                            invalidCandidate ->
                                ImportedPackageContent(
                                    contents = emptyList(),
                                    learningItems = listOf(invalidLearningItem),
                                    libraries = emptyList()
                                )

                            thirdCandidate ->
                                ImportedPackageContent(
                                    contents = listOf(thirdContent),
                                    learningItems = listOf(thirdLearningItem),
                                    libraries = listOf(thirdLibrary)
                                )

                            else ->
                                error(
                                    "Unexpected candidate: ${candidate.source}"
                                )
                        }
                    },
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
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

        val catalogId =
            PackageCatalogId("installed-packages")

        assertFailsWith<InvalidPackageException> {
            service.importAll(catalogId)
        }

        assertEquals(
            listOf(
                firstCandidate.source,
                invalidCandidate.source
            ),
            installedSources
        )

        assertEquals(
            listOf(
                firstCandidate.source,
                invalidCandidate.source
            ),
            importedSources
        )

        assertEquals(
            firstContent,
            contentRepository.findById(firstContent.id)
        )

        assertEquals(
            firstLearningItem,
            learningItemRepository.findById(firstLearningItem.id)
        )

        assertNull(
            contentRepository.findById(thirdContent.id)
        )

        assertNull(
            learningItemRepository.findById(thirdLearningItem.id)
        )

        assertNotNull(
            contentPackageRepository.findById(firstPackage.id)
        )

        assertNull(
            contentPackageRepository.findById(invalidPackage.id)
        )

        assertNull(
            contentPackageRepository.findById(thirdPackage.id)
        )

        val catalog =
            assertNotNull(
                packageCatalogRepository.findById(catalogId)
            )

        assertTrue(
            catalog.contains(firstPackage.id)
        )

        assertTrue(
            !catalog.contains(invalidPackage.id)
        )

        assertTrue(
            !catalog.contains(thirdPackage.id)
        )

        assertEquals(
            1,
            catalog.packageCount
        )
    }
}