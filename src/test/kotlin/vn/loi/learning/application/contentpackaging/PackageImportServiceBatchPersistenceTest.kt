package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
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
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageImportServiceBatchPersistenceTest {

    @Test
    fun `bundle import persists libraries contents and learning items in batches`() {
        val candidate =
            PackageScanCandidate(
                source =
                    "C:/packages/batch.opd3"
            )

        val firstContent =
            createContent(
                id =
                    "content-1",
                text =
                    "One"
            )

        val secondContent =
            createContent(
                id =
                    "content-2",
                text =
                    "Two"
            )

        val firstLearningItem =
            createLearningItem(
                id =
                    "item-1",
                contentId =
                    firstContent.id
            )

        val secondLearningItem =
            createLearningItem(
                id =
                    "item-2",
                contentId =
                    secondContent.id
            )

        val library =
            ContentLibrary(
                id =
                    ContentLibraryId(
                        "library-1"
                    ),
                descriptor =
                    LibraryDescriptor(
                        "Batch Library"
                    ),
                contentIds =
                    setOf(
                        firstContent.id,
                        secondContent.id
                    )
            )

        val contentRepository =
            CountingContentRepository()

        val learningItemRepository =
            CountingLearningItemRepository()

        val contentLibraryRepository =
            CountingContentLibraryRepository()

        val service =
            PackageImportService(
                packageScanner =
                    PackageScanner {
                        listOf(
                            candidate
                        )
                    },
                packageInstaller =
                    PackageInstaller {
                        ContentPackage(
                            id =
                                PackageId(
                                    "package-batch"
                                ),
                            descriptor =
                                PackageDescriptor(
                                    name =
                                        "Batch Package",
                                    version =
                                        "1.0.0",
                                    format =
                                        "OPD3"
                                )
                        )
                    },
                packageContentImporter =
                    PackageContentImporter {
                        ImportedPackageContent(
                            contents =
                                listOf(
                                    firstContent,
                                    secondContent
                                ),
                            learningItems =
                                listOf(
                                    firstLearningItem,
                                    secondLearningItem
                                ),
                            libraries =
                                listOf(
                                    library
                                )
                        )
                    },
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                contentLibraryRepository =
                    contentLibraryRepository,
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

        service.importCandidate(
            catalogId =
                PackageCatalogId(
                    "installed-packages"
                ),
            candidate =
                candidate
        )

        assertEquals(
            1,
            contentLibraryRepository.saveAllCallCount
        )

        assertEquals(
            0,
            contentLibraryRepository.saveCallCount
        )

        assertEquals(
            1,
            contentRepository.saveAllCallCount
        )

        assertEquals(
            0,
            contentRepository.saveCallCount
        )

        assertEquals(
            1,
            learningItemRepository.saveAllCallCount
        )

        assertEquals(
            0,
            learningItemRepository.saveCallCount
        )

        assertEquals(
            listOf(
                firstContent,
                secondContent
            ),
            contentRepository.findAll()
        )

        assertEquals(
            listOf(
                firstLearningItem,
                secondLearningItem
            ),
            learningItemRepository.findAllEnabled()
        )

        assertEquals(
            listOf(
                library
            ),
            contentLibraryRepository.findAll()
        )
    }

    @Test
    fun `batch persistence reports completed totals once per collection`() {
        val candidate =
            PackageScanCandidate(
                source =
                    "C:/packages/progress.opd3"
            )

        val contents =
            listOf(
                createContent(
                    id =
                        "content-1",
                    text =
                        "One"
                ),
                createContent(
                    id =
                        "content-2",
                    text =
                        "Two"
                ),
                createContent(
                    id =
                        "content-3",
                    text =
                        "Three"
                )
            )

        val learningItems =
            contents.mapIndexed { index, content ->
                createLearningItem(
                    id =
                        "item-${index + 1}",
                    contentId =
                        content.id
                )
            }

        val progressEvents =
            mutableListOf<PackageImportProgressEvent>()

        val service =
            PackageImportService(
                packageScanner =
                    PackageScanner {
                        emptyList()
                    },
                packageInstaller =
                    PackageInstaller {
                        ContentPackage(
                            id =
                                PackageId(
                                    "package-progress"
                                ),
                            descriptor =
                                PackageDescriptor(
                                    name =
                                        "Progress Package",
                                    version =
                                        "1.0.0",
                                    format =
                                        "OPD3"
                                )
                        )
                    },
                packageContentImporter =
                    PackageContentImporter {
                        ImportedPackageContent(
                            contents =
                                contents,
                            learningItems =
                                learningItems,
                            libraries =
                                emptyList()
                        )
                    },
                contentRepository =
                    CountingContentRepository(),
                learningItemRepository =
                    CountingLearningItemRepository(),
                packageRegistrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository =
                            InMemoryContentPackageRepository(),
                        packageCatalogRepository =
                            InMemoryPackageCatalogRepository()
                    ),
                transactionRunner =
                    InMemoryTransactionRunner(),
                progressListener =
                    PackageImportProgressListener { event ->
                        progressEvents +=
                            event
                    }
            )

        service.importCandidate(
            catalogId =
                PackageCatalogId(
                    "installed-packages"
                ),
            candidate =
                candidate
        )

        val contentSavingEvents =
            progressEvents.filter { event ->
                event.stage ==
                        PackageImportProgressStage.SAVING_CONTENT
            }

        val learningItemSavingEvents =
            progressEvents.filter { event ->
                event.stage ==
                        PackageImportProgressStage.SAVING_LEARNING_ITEMS
            }

        assertEquals(
            listOf(
                PackageImportProgressEvent(
                    stage =
                        PackageImportProgressStage.SAVING_CONTENT,
                    processed =
                        3,
                    total =
                        3
                )
            ),
            contentSavingEvents
        )

        assertEquals(
            listOf(
                PackageImportProgressEvent(
                    stage =
                        PackageImportProgressStage.SAVING_LEARNING_ITEMS,
                    processed =
                        3,
                    total =
                        3
                )
            ),
            learningItemSavingEvents
        )
    }

    private fun createContent(
        id: String,
        text: String
    ): Content =
        Content(
            id =
                ContentId(
                    id
                ),
            type =
                ContentType.WORD,
            text =
                ContentText(
                    primaryText =
                        text
                )
        )

    private fun createLearningItem(
        id: String,
        contentId: ContentId
    ): LearningItem =
        LearningItem(
            id =
                LearningItemId(
                    id
                ),
            contentId =
                contentId,
            mode =
                LearningMode.MEANING_RECOGNITION
        )

    private class CountingContentRepository :
        ContentRepository {

        private val contentsById =
            linkedMapOf<ContentId, Content>()

        var saveCallCount: Int = 0
            private set

        var saveAllCallCount: Int = 0
            private set

        override fun findById(
            contentId: ContentId
        ): Content? =
            contentsById[contentId]

        override fun save(
            content: Content
        ) {
            saveCallCount += 1
            contentsById[content.id] =
                content
        }

        override fun saveAll(
            contents: List<Content>
        ) {
            saveAllCallCount += 1

            contents.forEach { content ->
                contentsById[content.id] =
                    content
            }
        }

        override fun deleteById(
            contentId: ContentId
        ) {
            contentsById.remove(
                contentId
            )
        }

        override fun findAll(): List<Content> =
            contentsById.values.toList()
    }

    private class CountingLearningItemRepository :
        LearningItemRepository {

        private val learningItemsById =
            linkedMapOf<LearningItemId, LearningItem>()

        var saveCallCount: Int = 0
            private set

        var saveAllCallCount: Int = 0
            private set

        override fun findById(
            learningItemId: LearningItemId
        ): LearningItem? =
            learningItemsById[learningItemId]

        override fun findByContentId(
            contentId: ContentId
        ): List<LearningItem> =
            learningItemsById.values
                .filter { learningItem ->
                    learningItem.contentId ==
                            contentId
                }

        override fun findAllEnabled(): List<LearningItem> =
            learningItemsById.values
                .filter { learningItem ->
                    learningItem.isEnabled
                }

        override fun save(
            learningItem: LearningItem
        ) {
            saveCallCount += 1

            learningItemsById[learningItem.id] =
                learningItem
        }

        override fun saveAll(
            learningItems: List<LearningItem>
        ) {
            saveAllCallCount += 1

            learningItems.forEach { learningItem ->
                learningItemsById[learningItem.id] =
                    learningItem
            }
        }

        override fun deleteById(
            learningItemId: LearningItemId
        ) {
            learningItemsById.remove(
                learningItemId
            )
        }
    }

    private class CountingContentLibraryRepository :
        ContentLibraryRepository {

        private val librariesById =
            linkedMapOf<ContentLibraryId, ContentLibrary>()

        var saveCallCount: Int = 0
            private set

        var saveAllCallCount: Int = 0
            private set

        override fun findById(
            libraryId: ContentLibraryId
        ): ContentLibrary? =
            librariesById[libraryId]

        override fun save(
            library: ContentLibrary
        ) {
            saveCallCount += 1

            librariesById[library.id] =
                library
        }

        override fun saveAll(
            libraries: List<ContentLibrary>
        ) {
            saveAllCallCount += 1

            libraries.forEach { library ->
                librariesById[library.id] =
                    library
            }
        }

        override fun deleteById(
            libraryId: ContentLibraryId
        ) {
            librariesById.remove(
                libraryId
            )
        }

        override fun findAll(): List<ContentLibrary> =
            librariesById.values.toList()
    }
}