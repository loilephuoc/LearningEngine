package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageImportServiceLibraryPersistenceTest {

    @Test
    fun `import candidate persists imported libraries when repository is provided`() {
        val candidate =
            PackageScanCandidate(
                source = "C:/packages/library.opd3"
            )

        val contentPackage =
            ContentPackage(
                id = PackageId("package-library"),
                descriptor =
                    PackageDescriptor(
                        name = "Library Package",
                        version = "1.0.0",
                        format = "OPD3"
                    )
            )

        val content =
            Content(
                id = ContentId("content-library"),
                type = ContentType.WORD,
                text =
                    ContentText(
                        primaryText = "library",
                        translatedText = "thu vien"
                    )
            )

        val learningItem =
            LearningItem(
                id = LearningItemId("item-library"),
                contentId = content.id,
                mode = LearningMode.MEANING_RECOGNITION
            )

        val firstLibrary =
            ContentLibrary(
                id = ContentLibraryId("library-first"),
                descriptor =
                    LibraryDescriptor(
                        "First Library"
                    ),
                contentIds = setOf(content.id)
            )

        val secondLibrary =
            ContentLibrary(
                id = ContentLibraryId("library-second"),
                descriptor =
                    LibraryDescriptor(
                        "Second Library"
                    ),
                contentIds = setOf(content.id)
            )

        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val service =
            PackageImportService(
                packageScanner =
                    PackageScanner {
                        emptyList()
                    },
                packageInstaller =
                    PackageInstaller {
                        contentPackage
                    },
                packageContentImporter =
                    PackageContentImporter {
                        ImportedPackageContent(
                            contents = listOf(content),
                            learningItems = listOf(learningItem),
                            libraries =
                                listOf(
                                    firstLibrary,
                                    secondLibrary
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
                    InMemoryTransactionRunner(),
                contentLibraryRepository =
                    contentLibraryRepository
            )

        val result =
            service.importCandidate(
                catalogId =
                    PackageCatalogId("installed-packages"),
                candidate = candidate
            )

        assertEquals(
            2,
            contentLibraryRepository.count()
        )

        assertEquals(
            firstLibrary,
            contentLibraryRepository.findById(
                firstLibrary.id
            )
        )

        assertEquals(
            secondLibrary,
            contentLibraryRepository.findById(
                secondLibrary.id
            )
        )

        assertEquals(
            setOf(
                firstLibrary.id,
                secondLibrary.id
            ),
            result.contentPackage.libraryIds
        )

        assertEquals(
            2,
            result.importedLibraryCount
        )

        assertNotNull(
            result.contentPackage
        )
    }
}