package vn.loi.learning.infrastructure.persistence

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.contentpackaging.PackageInstaller
import vn.loi.learning.application.contentpackaging.PackageRegistrationOperation
import vn.loi.learning.application.contentpackaging.PackageScanner
import vn.loi.learning.application.contentpackaging.PackageImportProgressListener
import vn.loi.learning.application.contentpackaging.PackageUninstallOperation
import vn.loi.learning.application.contentpackaging.UninstallContentPackageUseCase
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository

import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.infrastructure.PersistedLearningPlatform
import vn.loi.learning.infrastructure.contentpackaging.ContentPackageImportFactory
import vn.loi.learning.infrastructure.persistence.json.JsonContentLibraryStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentStore
import vn.loi.learning.infrastructure.persistence.json.JsonLearningItemStore
import vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner

object PersistedLearningPlatformFactory {

    fun createPlatform(
        persistenceDirectory: Path,
        packageDirectory: Path
    ): PersistedLearningPlatform {
        val contentLibrariesPath =
            persistenceDirectory.resolve(
                CONTENT_LIBRARIES_FILE_NAME
            )

        val contentsPath =
            persistenceDirectory.resolve(
                CONTENTS_FILE_NAME
            )

        val learningItemsPath =
            persistenceDirectory.resolve(
                LEARNING_ITEMS_FILE_NAME
            )

        val contentPackagesPath =
            persistenceDirectory.resolve(
                CONTENT_PACKAGES_FILE_NAME
            )

        val packageCatalogsPath =
            persistenceDirectory.resolve(
                PACKAGE_CATALOGS_FILE_NAME
            )

        val contentLibraryRepository =
            StoreBackedContentLibraryRepository(
                JsonContentLibraryStore(
                    contentLibrariesPath
                )
            )

        val contentRepository =
            StoreBackedContentRepository(
                JsonContentStore(
                    contentsPath
                )
            )

        val learningItemRepository =
            StoreBackedLearningItemRepository(
                JsonLearningItemStore(
                    learningItemsPath
                )
            )

        val contentPackageRepository =
            StoreBackedContentPackageRepository(
                JsonContentPackageStore(
                    contentPackagesPath
                )
            )

        val packageCatalogRepository =
            StoreBackedPackageCatalogRepository(
                JsonPackageCatalogStore(
                    packageCatalogsPath
                )
            )

        val transactionRunner =
            JsonFileTransactionRunner(
                listOf(
                    contentLibrariesPath,
                    contentsPath,
                    learningItemsPath,
                    contentPackagesPath,
                    packageCatalogsPath,
                    persistenceDirectory.resolve(
                        MEMORY_STATES_FILE_NAME
                    ),
                    persistenceDirectory.resolve(
                        REVIEW_EVENTS_FILE_NAME
                    ),
                    persistenceDirectory.resolve(
                        STUDY_SESSIONS_FILE_NAME
                    ),
                    persistenceDirectory.resolve(
                        STUDY_QUEUES_FILE_NAME
                    )
                )
            )

        val packageImportService =
            create(
                packageScanner =
                    ContentPackageImportFactory.createScanner(
                        packageDirectory
                    ),
                packageInstaller =
                    ContentPackageImportFactory.createInstaller(),
                packageContentImporter =
                    ContentPackageImportFactory
                        .createContentImporter(),
                contentLibraryRepository =
                    contentLibraryRepository,
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                contentPackageRepository =
                    contentPackageRepository,
                packageCatalogRepository =
                    packageCatalogRepository,
                transactionRunner =
                    transactionRunner
            )

        val learningEngine =
            PersistedLearningEngineFactory.create(
                persistenceDirectory =
                    persistenceDirectory,
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                transactionRunner =
                    transactionRunner
            )

        return PersistedLearningPlatform(
            learningEngine = learningEngine,
            packageImportService = packageImportService
        )
    }

    fun createPersisted(
        persistenceDirectory: Path,
        packageScanner: PackageScanner,
        packageInstaller: PackageInstaller,
        packageContentImporter: PackageContentImporter
    ): PackageImportService =
        create(
            packageScanner = packageScanner,
            packageInstaller = packageInstaller,
            packageContentImporter = packageContentImporter,
            contentLibraryRepository =
                StoreBackedContentLibraryRepository(
                    JsonContentLibraryStore(
                        persistenceDirectory.resolve(
                            CONTENT_LIBRARIES_FILE_NAME
                        )
                    )
                ),
            contentRepository =
                StoreBackedContentRepository(
                    JsonContentStore(
                        persistenceDirectory.resolve(
                            CONTENTS_FILE_NAME
                        )
                    )
                ),
            learningItemRepository =
                StoreBackedLearningItemRepository(
                    JsonLearningItemStore(
                        persistenceDirectory.resolve(
                            LEARNING_ITEMS_FILE_NAME
                        )
                    )
                ),
            contentPackageRepository =
                StoreBackedContentPackageRepository(
                    JsonContentPackageStore(
                        persistenceDirectory.resolve(
                            CONTENT_PACKAGES_FILE_NAME
                        )
                    )
                ),
            packageCatalogRepository =
                StoreBackedPackageCatalogRepository(
                    JsonPackageCatalogStore(
                        persistenceDirectory.resolve(
                            PACKAGE_CATALOGS_FILE_NAME
                        )
                    )
                ),
            transactionRunner =
                JsonFileTransactionRunner(
                    listOf(
                        persistenceDirectory.resolve(
                            CONTENT_LIBRARIES_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            CONTENTS_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            LEARNING_ITEMS_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            CONTENT_PACKAGES_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            PACKAGE_CATALOGS_FILE_NAME
                        )
                    )
                )
        )

    fun create(
        packageScanner: PackageScanner,
        packageInstaller: PackageInstaller,
        packageContentImporter: PackageContentImporter,
        contentLibraryRepository: ContentLibraryRepository,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        contentPackageRepository: ContentPackageRepository,
        packageCatalogRepository: PackageCatalogRepository,
        transactionRunner: TransactionRunner,
        progressListener: PackageImportProgressListener? = null,
        installedPackageRepository: vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null,
        memoryStateRepository: MemoryStateRepository? = null,
        reviewEventRepository: ReviewEventRepository? = null,
        studySessionRepository: StudySessionRepository? = null,
        studyQueueRepository: vn.loi.learning.application.port.StudyQueueRepository? = null,
        partOfSpeechRegistry: vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry? = null
    ): PackageImportService {
        val packageRegistrationOperation =
            PackageRegistrationOperation(
                contentPackageRepository =
                    contentPackageRepository,
                packageCatalogRepository =
                    packageCatalogRepository
            )

        return PackageImportService(
            packageScanner = packageScanner,
            packageInstaller = packageInstaller,
            packageContentImporter =
                packageContentImporter,
            contentRepository =
                contentRepository,
            contentLibraryRepository =
                contentLibraryRepository,
            learningItemRepository =
                learningItemRepository,
            packageRegistrationOperation =
                packageRegistrationOperation,
            transactionRunner =
                transactionRunner,
            progressListener = progressListener,
            installedPackageRepository = installedPackageRepository,
            contentPackageRepository = contentPackageRepository,
            orphanPackageLearningStateReconciler =
                vn.loi.learning.application.contentpackaging.OrphanPackageLearningStateReconciler(
                    installedPackageRepository = installedPackageRepository,
                    contentPackageRepository = contentPackageRepository,
                    memoryStateRepository = memoryStateRepository,
                    reviewEventRepository = reviewEventRepository,
                    studySessionRepository = studySessionRepository,
                    studyQueueRepository = studyQueueRepository
                ),
            partOfSpeechRegistry = partOfSpeechRegistry
        )
    }

    fun createPersistedUninstaller(
        persistenceDirectory: Path
    ): UninstallContentPackageUseCase =
        createUninstaller(
            contentLibraryRepository =
                StoreBackedContentLibraryRepository(
                    JsonContentLibraryStore(
                        persistenceDirectory.resolve(
                            CONTENT_LIBRARIES_FILE_NAME
                        )
                    )
                ),
            contentRepository =
                StoreBackedContentRepository(
                    JsonContentStore(
                        persistenceDirectory.resolve(
                            CONTENTS_FILE_NAME
                        )
                    )
                ),
            learningItemRepository =
                StoreBackedLearningItemRepository(
                    JsonLearningItemStore(
                        persistenceDirectory.resolve(
                            LEARNING_ITEMS_FILE_NAME
                        )
                    )
                ),
            contentPackageRepository =
                StoreBackedContentPackageRepository(
                    JsonContentPackageStore(
                        persistenceDirectory.resolve(
                            CONTENT_PACKAGES_FILE_NAME
                        )
                    )
                ),
            packageCatalogRepository =
                StoreBackedPackageCatalogRepository(
                    JsonPackageCatalogStore(
                        persistenceDirectory.resolve(
                            PACKAGE_CATALOGS_FILE_NAME
                        )
                    )
                ),
            transactionRunner =
                JsonFileTransactionRunner(
                    listOf(
                        persistenceDirectory.resolve(
                            LEARNING_ITEMS_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            CONTENTS_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            CONTENT_LIBRARIES_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            CONTENT_PACKAGES_FILE_NAME
                        ),
                        persistenceDirectory.resolve(
                            PACKAGE_CATALOGS_FILE_NAME
                        )
                    )
                )
        )

    fun createUninstaller(
        contentLibraryRepository: ContentLibraryRepository,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        contentPackageRepository: ContentPackageRepository,
        packageCatalogRepository: PackageCatalogRepository,
        transactionRunner: TransactionRunner,
        memoryStateRepository: MemoryStateRepository? = null,
        reviewEventRepository: ReviewEventRepository? = null,
        studySessionRepository: StudySessionRepository? = null
    ): UninstallContentPackageUseCase =
        UninstallContentPackageUseCase(
            uninstallOperation =
                PackageUninstallOperation(
                    contentLibraryRepository =
                        contentLibraryRepository,
                    contentRepository =
                        contentRepository,
                    learningItemRepository =
                        learningItemRepository,
                    contentPackageRepository =
                        contentPackageRepository,
                    packageCatalogRepository =
                        packageCatalogRepository,
                    memoryStateRepository =
                        memoryStateRepository,
                    reviewEventRepository =
                        reviewEventRepository,
                    studySessionRepository =
                        studySessionRepository
                ),
            transactionRunner =
                transactionRunner
        )


    private const val CONTENT_LIBRARIES_FILE_NAME =
        "content-libraries.json"

    private const val CONTENTS_FILE_NAME =
        "contents.json"

    private const val LEARNING_ITEMS_FILE_NAME =
        "learning-items.json"

    private const val CONTENT_PACKAGES_FILE_NAME =
        "content-packages.json"

    private const val PACKAGE_CATALOGS_FILE_NAME =
        "package-catalogs.json"

    private const val MEMORY_STATES_FILE_NAME =
        "memory-states.json"

    private const val REVIEW_EVENTS_FILE_NAME =
        "review-events.json"

    private const val STUDY_SESSIONS_FILE_NAME =
        "study-sessions.json"

    private const val STUDY_QUEUES_FILE_NAME =
        "study-queues.json"
}
