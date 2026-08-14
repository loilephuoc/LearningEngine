package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository

class PackageImportServiceCommitFailureProgressTest {

    @Test
    fun `lifecycle completes before media publish and failure rolls back prepared media`() {
        val events = mutableListOf<String>()
        val contentPackage = ContentPackage(
            PackageId("package-lifecycle-failure"),
            PackageDescriptor("Lifecycle Failure", "1.0.0", "OPD3")
        )
        val service = PackageImportService(
            packageScanner = PackageScanner { emptyList() },
            packageInstaller = PackageInstaller { contentPackage },
            packageContentImporter = PackageContentImporter {
                ImportedPackageContent(
                    contents = emptyList(),
                    learningItems = emptyList(),
                    onCommit = { events += "media-commit" },
                    onRollback = { events += "media-rollback" }
                )
            },
            contentRepository = InMemoryContentRepository(),
            learningItemRepository = InMemoryLearningItemRepository(),
            packageRegistrationOperation = PackageRegistrationOperation(
                InMemoryContentPackageRepository(),
                InMemoryPackageCatalogRepository()
            ),
            transactionRunner = object : TransactionRunner {
                override fun <T> runInTransaction(block: () -> T): T {
                    events += "transaction-start"
                    return block().also { events += "transaction-complete" }
                }
            },
            packageLifecycleCompletion = {
                events += "lifecycle"
                error("injected lifecycle failure")
            }
        )

        assertFailsWith<IllegalStateException> {
            service.importCandidate(
                PackageCatalogId("lifecycle-failure-catalog"),
                PackageScanCandidate("C:/packages/lifecycle-failure.opd3")
            )
        }

        assertEquals(
            listOf("transaction-start", "lifecycle", "media-rollback"),
            events
        )
    }

    @Test
    fun `completed is not reported when transaction fails after executing block`() {
        val candidate =
            PackageScanCandidate(
                source = "C:/packages/commit-failure.opd3"
            )

        val contentPackage =
            ContentPackage(
                id = PackageId("package-commit-failure"),
                descriptor =
                    PackageDescriptor(
                        name = "Commit Failure Package",
                        version = "1.0.0",
                        format = "OPD3"
                    )
            )

        val progressEvents =
            mutableListOf<PackageImportProgressEvent>()

        val transactionRunner =
            object : TransactionRunner {
                override fun <T> runInTransaction(
                    block: () -> T
                ): T {
                    block()
                    throw IllegalStateException(
                        "transaction commit failed"
                    )
                }
            }

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
                            contents = emptyList(),
                            learningItems = emptyList(),
                            libraries = emptyList()
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
                    transactionRunner,
                progressListener =
                    PackageImportProgressListener { event ->
                        progressEvents += event
                    }
            )

        assertFailsWith<IllegalStateException> {
            service.importCandidate(
                catalogId =
                    PackageCatalogId(
                        "commit-failure-catalog"
                    ),
                candidate = candidate
            )
        }

        assertEquals(
            listOf(
                PackageImportProgressStage.PACKAGE_INSTALLED,
                PackageImportProgressStage.CONTENT_IMPORTED,
                PackageImportProgressStage.REGISTERING_PACKAGE
            ),
            progressEvents.map { event ->
                event.stage
            }
        )

        assertTrue(
            progressEvents.none { event ->
                event.stage ==
                        PackageImportProgressStage.COMPLETED
            }
        )
    }
}
