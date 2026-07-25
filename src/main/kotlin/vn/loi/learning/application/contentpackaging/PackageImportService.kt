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
    private val installedPackageRepository: vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null,
    private val contentPackageRepository: vn.loi.learning.application.port.ContentPackageRepository? = null,
    private val installedContentConflictValidator:
    InstalledContentConflictValidator =
        InstalledContentConflictValidator(
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository,
            installedPackageRepository =
                installedPackageRepository,
            contentPackageRepository =
                contentPackageRepository,
            contentLibraryRepository =
                contentLibraryRepository
        )
) {

    fun importAll(
        catalogId: PackageCatalogId,
        cancellationSignal: PackageImportCancellationSignal? = null
    ): List<PackageImportResult> =
        packageScanner.scan()
            .map { candidate ->
                cancellationSignal?.checkCancelled()
                importCandidate(
                    catalogId = catalogId,
                    candidate = candidate,
                    cancellationSignal = cancellationSignal
                )
            }

    fun importAll(
        catalogId: PackageCatalogId
    ): List<PackageImportResult> = importAll(catalogId, null)

    fun importAllDetailed(
        catalogId: PackageCatalogId,
        cancellationSignal: PackageImportCancellationSignal? = null
    ): PackageImportBatchResult {
        val startTime = System.currentTimeMillis()
        reportProgress(PackageImportProgressStage.SCANNING, message = "Scanning selected package...")
        cancellationSignal?.checkCancelled()

        val successfulImports = mutableListOf<PackageImportResult>()
        val failures = mutableListOf<PackageImportFailure>()

        val candidates = packageScanner.scan()
        candidates.forEach { candidate ->
            cancellationSignal?.checkCancelled()
            try {
                successfulImports += importCandidate(
                    catalogId = catalogId,
                    candidate = candidate,
                    cancellationSignal = cancellationSignal
                )
            } catch (exception: Exception) {
                failures += PackageImportFailure.from(
                    source = candidate.source,
                    exception = exception
                )
                if (exception is PackageImportCancelledException) {
                    throw exception
                }
            }
        }

        if (failures.isEmpty() && successfulImports.isNotEmpty()) {
            reportProgress(PackageImportProgressStage.COMPLETED, message = "Completed")
        }

        PackageImportDiagnostics.logTerminal(
            result = "Success (${successfulImports.size} packages, ${failures.size} failures)",
            durationMs = System.currentTimeMillis() - startTime
        )

        return PackageImportBatchResult(
            successfulImports = successfulImports,
            failures = failures
        )
    }

    fun importAllDetailed(
        catalogId: PackageCatalogId
    ): PackageImportBatchResult = importAllDetailed(catalogId, null)

    fun importCandidate(
        catalogId: PackageCatalogId,
        candidate: PackageScanCandidate,
        cancellationSignal: PackageImportCancellationSignal? = null
    ): PackageImportResult {
        cancellationSignal?.checkCancelled()
        val contentPackage = packageInstaller.install(candidate)

        reportProgress(PackageImportProgressStage.PACKAGE_INSTALLED, message = "Package descriptor validated")
        cancellationSignal?.checkCancelled()

        val importedContent = packageContentImporter.importContent(
            candidate = candidate,
            progressListener = { event ->
                reportProgress(event.stage, event.processed, event.total, event.message)
            },
            cancellationSignal = cancellationSignal
        )

        reportProgress(
            stage = PackageImportProgressStage.CONTENT_IMPORTED,
            processed = importedContent.contents.size,
            total = importedContent.contents.size,
            message = "Parsed ${importedContent.contents.size} items"
        )
        cancellationSignal?.checkCancelled()

        val packageValidationReport = packageValidator.validate(
            descriptor = contentPackage.descriptor,
            importedContent = importedContent
        )

        val installedConflictReport = installedContentConflictValidator.validate(importedContent)

        val validationReport = PackageValidationReport(
            issues = packageValidationReport.issues + installedConflictReport.issues
        )

        if (!validationReport.isValid) {
            throw InvalidPackageException(validationReport)
        }

        val registeredPackage = contentPackage.registerAll(
            importedContent.libraries.map { library -> library.id }.toSet()
        )

        val registrationCommand = RegisterContentPackageCommand(
            catalogId = catalogId,
            contentPackage = registeredPackage
        )

        packageRegistrationOperation.ensureCanRegister(registrationCommand)

        cancellationSignal?.checkCancelled()

        val result = transactionRunner.runInTransaction {
            cancellationSignal?.checkCancelled()
            if (importedContent.libraries.isNotEmpty()) {
                contentLibraryRepository?.saveAll(importedContent.libraries)
            }

            if (importedContent.contents.isNotEmpty()) {
                contentRepository.saveAll(importedContent.contents)
                reportProgress(
                    stage = PackageImportProgressStage.SAVING_CONTENT,
                    processed = importedContent.contents.size,
                    total = importedContent.contents.size
                )
            }

            if (importedContent.learningItems.isNotEmpty()) {
                learningItemRepository.saveAll(importedContent.learningItems)
                reportProgress(
                    stage = PackageImportProgressStage.SAVING_LEARNING_ITEMS,
                    processed = importedContent.learningItems.size,
                    total = importedContent.learningItems.size
                )
            }

            reportProgress(PackageImportProgressStage.REGISTERING_PACKAGE, message = "Registering package")
            packageRegistrationOperation.execute(registrationCommand)

            PackageImportResult(
                contentPackage = registeredPackage,
                importedLibraryCount = importedContent.importedLibraryCount,
                importedContentCount = importedContent.contents.size,
                importedLearningItemCount = importedContent.learningItems.size,
                report = importedContent.report,
                warnings = importedContent.warnings
            )
        }

        return result
    }

    fun importCandidate(
        catalogId: PackageCatalogId,
        candidate: PackageScanCandidate
    ): PackageImportResult = importCandidate(catalogId, candidate, null)

    private fun reportProgress(
        stage: PackageImportProgressStage,
        processed: Int = 0,
        total: Int = 0,
        message: String? = null
    ) {
        progressListener?.onProgress(
            PackageImportProgressEvent(
                stage = stage,
                processed = processed,
                total = total,
                message = message
            )
        )
    }
}
