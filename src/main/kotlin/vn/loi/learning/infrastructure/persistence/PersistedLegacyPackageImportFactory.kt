package vn.loi.learning.infrastructure.persistence

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.LegacyPackageImportWorkflow
import vn.loi.learning.application.contentpackaging.PackageRegistrationOperation
import vn.loi.learning.infrastructure.contentpackaging.ContentPackageImportFactory
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentStore
import vn.loi.learning.infrastructure.persistence.json.JsonLearningItemStore
import vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner

/**
 * Composition root dành riêng cho workflow import package legacy.
 */
object PersistedLegacyPackageImportFactory {

    fun create(
        persistenceDirectory: Path,
        sourceDirectory: Path,
        mediaDirectory: Path
    ): LegacyPackageImportWorkflow {
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

        val packageRegistrationOperation =
            PackageRegistrationOperation(
                contentPackageRepository =
                    contentPackageRepository,
                packageCatalogRepository =
                    packageCatalogRepository
            )

        val transactionRunner =
            JsonFileTransactionRunner(
                listOf(
                    contentsPath,
                    learningItemsPath,
                    contentPackagesPath,
                    packageCatalogsPath
                )
            )

        return ContentPackageImportFactory
            .createLegacyImportWorkflow(
                sourceDirectory =
                    sourceDirectory,
                mediaDirectory =
                    mediaDirectory,
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                packageRegistrationOperation =
                    packageRegistrationOperation,
                transactionRunner =
                    transactionRunner
            )
    }

    private const val CONTENTS_FILE_NAME =
        "contents.json"

    private const val LEARNING_ITEMS_FILE_NAME =
        "learning-items.json"

    private const val CONTENT_PACKAGES_FILE_NAME =
        "content-packages.json"

    private const val PACKAGE_CATALOGS_FILE_NAME =
        "package-catalogs.json"
}