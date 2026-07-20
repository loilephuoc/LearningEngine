package vn.loi.learning.application.contentpackaging

import java.nio.file.Paths
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * Workflow import package legacy JSON + PKG.
 *
 * Khác PackageImportService dành cho bundle package mới,
 * workflow này tự tạo descriptor từ tên file JSON legacy.
 *
 * Toàn bộ thay đổi repository của một package được thực hiện
 * trong cùng transaction.
 */
class LegacyPackageImportService(
    private val packageContentImporter: LegacyPackageContentImporter,
    private val packageIdGenerator: PackageIdGenerator,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val packageRegistrationOperation: PackageRegistrationOperation,
    private val transactionRunner: TransactionRunner,
    private val contentLibraryRepository: ContentLibraryRepository? = null
) {

    fun importAll(
        catalogId: PackageCatalogId,
        candidates: List<LegacyPackageCandidate>
    ): List<PackageImportResult> =
        candidates.map { candidate ->
            importCandidate(
                catalogId = catalogId,
                candidate = candidate
            )
        }

    fun importCandidate(
        catalogId: PackageCatalogId,
        candidate: LegacyPackageCandidate
    ): PackageImportResult {
        val descriptor =
            createDescriptor(
                candidate
            )

        val contentPackage =
            ContentPackage(
                id =
                    packageIdGenerator.generate(
                        descriptor
                    ),
                descriptor =
                    descriptor
            )

        val importedContent =
            packageContentImporter.importContent(
                candidate
            )

        val registeredPackage =
            contentPackage.registerAll(
                importedContent.libraries
                    .map { library ->
                        library.id
                    }
                    .toSet()
            )

        return transactionRunner.runInTransaction {
            contentLibraryRepository?.saveAll(
                importedContent.libraries
            )

            contentRepository.saveAll(
                importedContent.contents
            )

            learningItemRepository.saveAll(
                importedContent.learningItems
            )

            packageRegistrationOperation.execute(
                RegisterContentPackageCommand(
                    catalogId =
                        catalogId,
                    contentPackage =
                        registeredPackage
                )
            )

            PackageImportResult(
                contentPackage =
                    registeredPackage,
                importedLibraryCount =
                    importedContent.importedLibraryCount,
                importedContentCount =
                    importedContent.contents.size,
                importedLearningItemCount =
                    importedContent.learningItems.size,
                report =
                    importedContent.report,
                warnings =
                    importedContent.warnings
            )
        }
    }

    private fun createDescriptor(
        candidate: LegacyPackageCandidate
    ): PackageDescriptor {
        val jsonFileName =
            Paths.get(
                candidate.jsonSource
            )
                .fileName
                .toString()

        val extensionSeparatorIndex =
            jsonFileName.lastIndexOf('.')

        require(
            extensionSeparatorIndex > 0
        ) {
            "Legacy package JSON source must have a file extension: ${candidate.jsonSource}"
        }

        val packageName =
            jsonFileName.substring(
                startIndex = 0,
                endIndex = extensionSeparatorIndex
            )

        return PackageDescriptor(
            name =
                packageName,
            version =
                LEGACY_VERSION,
            format =
                LEGACY_FORMAT
        )
    }

    private companion object {

        const val LEGACY_VERSION =
            "1"

        const val LEGACY_FORMAT =
            "LEGACY_OPD3"
    }
}