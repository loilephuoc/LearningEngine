package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.contentpackaging.validation.InvalidPackageException
import vn.loi.learning.application.contentpackaging.validation.PackageValidator
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

/**
 * Điều phối toàn bộ workflow import Content Package.
 *
 * Việc scan, đọc và chuyển đổi package được thực hiện trước transaction.
 * Toàn bộ thay đổi repository của từng package được ghi trong một
 * transaction duy nhất.
 */
class PackageImportService(
    private val packageScanner: PackageScanner,
    private val packageInstaller: PackageInstaller,
    private val packageContentImporter: PackageContentImporter,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val packageRegistrationOperation: PackageRegistrationOperation,
    private val transactionRunner: TransactionRunner,
    private val contentLibraryRepository: ContentLibraryRepository? = null,
    private val progressListener: PackageImportProgressListener? = null,
    private val packageValidator: PackageValidator = PackageValidator()
) {

    fun importAll(
        catalogId: PackageCatalogId
    ): List<PackageImportResult> =
        packageScanner.scan().map { candidate ->
            importCandidate(
                catalogId = catalogId,
                candidate = candidate
            )
        }

    fun importCandidate(
        catalogId: PackageCatalogId,
        candidate: PackageScanCandidate
    ): PackageImportResult {
        val contentPackage =
            packageInstaller.install(candidate)

        reportProgress(
            PackageImportProgressStage.PACKAGE_INSTALLED
        )

        val importedContent =
            packageContentImporter.importContent(candidate)

        reportProgress(
            stage = PackageImportProgressStage.CONTENT_IMPORTED,
            processed = importedContent.contents.size,
            total = importedContent.contents.size
        )

        val validationReport =
            packageValidator.validate(
                descriptor = contentPackage.descriptor,
                importedContent = importedContent
            )

        if (!validationReport.isValid) {
            throw InvalidPackageException(validationReport)
        }

        val registeredPackage =
            contentPackage.registerAll(
                importedContent.libraries
                    .map { library -> library.id }
                    .toSet()
            )

        val result =
            transactionRunner.runInTransaction {
                importedContent.libraries.forEach { library ->
                    contentLibraryRepository?.save(library)
                }

                importedContent.contents.forEachIndexed { index, content ->
                    contentRepository.save(content)

                    reportProgress(
                        stage = PackageImportProgressStage.SAVING_CONTENT,
                        processed = index + 1,
                        total = importedContent.contents.size
                    )
                }

                importedContent.learningItems.forEachIndexed { index, learningItem ->
                    learningItemRepository.save(learningItem)

                    reportProgress(
                        stage = PackageImportProgressStage.SAVING_LEARNING_ITEMS,
                        processed = index + 1,
                        total = importedContent.learningItems.size
                    )
                }

                reportProgress(
                    PackageImportProgressStage.REGISTERING_PACKAGE
                )

                packageRegistrationOperation.execute(
                    RegisterContentPackageCommand(
                        catalogId = catalogId,
                        contentPackage = registeredPackage
                    )
                )

                PackageImportResult(
                    contentPackage = registeredPackage,
                    importedLibraryCount =
                        importedContent.importedLibraryCount,
                    importedContentCount =
                        importedContent.contents.size,
                    importedLearningItemCount =
                        importedContent.learningItems.size,
                    report = importedContent.report,
                    warnings = importedContent.warnings
                )
            }

        reportProgress(
            PackageImportProgressStage.COMPLETED
        )

        return result
    }

    private fun reportProgress(
        stage: PackageImportProgressStage,
        processed: Int = 0,
        total: Int = 0
    ) {
        progressListener?.onProgress(
            PackageImportProgressEvent(
                stage = stage,
                processed = processed,
                total = total
            )
        )
    }
}