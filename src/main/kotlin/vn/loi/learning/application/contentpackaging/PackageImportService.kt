package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.contentpackaging.validation.InstalledContentConflictValidator
import vn.loi.learning.application.contentpackaging.validation.InvalidPackageException
import vn.loi.learning.application.contentpackaging.validation.PackageValidationReport
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
    private val packageValidator: PackageValidator =
        PackageValidator(),
    private val installedContentConflictValidator:
    InstalledContentConflictValidator =
        InstalledContentConflictValidator(
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository
        )
) {

    fun importAll(
        catalogId: PackageCatalogId
    ): List<PackageImportResult> =
        packageScanner.scan()
            .map { candidate ->
                importCandidate(
                    catalogId =
                        catalogId,
                    candidate =
                        candidate
                )
            }

    fun importAllDetailed(
        catalogId: PackageCatalogId
    ): PackageImportBatchResult {
        val successfulImports =
            mutableListOf<PackageImportResult>()

        val failures =
            mutableListOf<PackageImportFailure>()

        packageScanner.scan()
            .forEach { candidate ->
                try {
                    successfulImports +=
                        importCandidate(
                            catalogId = catalogId,
                            candidate = candidate
                        )
                } catch (exception: Exception) {
                    failures +=
                        PackageImportFailure.from(
                            source = candidate.source,
                            exception = exception
                        )
                }
            }

        return PackageImportBatchResult(
            successfulImports = successfulImports,
            failures = failures
        )
    }

    fun importCandidate(
        catalogId: PackageCatalogId,
        candidate: PackageScanCandidate
    ): PackageImportResult {
        val contentPackage =
            packageInstaller.install(
                candidate
            )

        reportProgress(
            PackageImportProgressStage.PACKAGE_INSTALLED
        )

        val importedContent =
            packageContentImporter.importContent(
                candidate
            )

        reportProgress(
            stage =
                PackageImportProgressStage.CONTENT_IMPORTED,
            processed =
                importedContent.contents.size,
            total =
                importedContent.contents.size
        )

        val packageValidationReport =
            packageValidator.validate(
                descriptor =
                    contentPackage.descriptor,
                importedContent =
                    importedContent
            )

        val installedConflictReport =
            installedContentConflictValidator.validate(
                importedContent
            )

        val validationReport =
            PackageValidationReport(
                issues =
                    packageValidationReport.issues +
                        installedConflictReport.issues
            )

        if (!validationReport.isValid) {
            throw InvalidPackageException(
                validationReport
            )
        }

        val registeredPackage =
            contentPackage.registerAll(
                importedContent.libraries
                    .map { library ->
                        library.id
                    }
                    .toSet()
            )

        val registrationCommand =
            RegisterContentPackageCommand(
                catalogId =
                    catalogId,
                contentPackage =
                    registeredPackage
            )

        packageRegistrationOperation.ensureCanRegister(
            registrationCommand
        )

        val result =
            transactionRunner.runInTransaction {
                if (importedContent.libraries.isNotEmpty()) {
                    contentLibraryRepository?.saveAll(
                        importedContent.libraries
                    )
                }

                if (importedContent.contents.isNotEmpty()) {
                    contentRepository.saveAll(
                        importedContent.contents
                    )

                    reportProgress(
                        stage =
                            PackageImportProgressStage.SAVING_CONTENT,
                        processed =
                            importedContent.contents.size,
                        total =
                            importedContent.contents.size
                    )
                }

                if (importedContent.learningItems.isNotEmpty()) {
                    learningItemRepository.saveAll(
                        importedContent.learningItems
                    )

                    reportProgress(
                        stage =
                            PackageImportProgressStage.SAVING_LEARNING_ITEMS,
                        processed =
                            importedContent.learningItems.size,
                        total =
                            importedContent.learningItems.size
                    )
                }

                reportProgress(
                    PackageImportProgressStage.REGISTERING_PACKAGE
                )

                packageRegistrationOperation.execute(
                    registrationCommand
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
                stage =
                    stage,
                processed =
                    processed,
                total =
                    total
            )
        )
    }
}
