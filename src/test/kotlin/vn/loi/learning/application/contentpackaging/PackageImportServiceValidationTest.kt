package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.validation.InvalidPackageException
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository

class PackageImportServiceValidationTest {

    @Test
    fun `invalid package is rejected before transaction and persistence`() {
        val candidate =
            PackageScanCandidate(
                source =
                    "C:/packages/invalid.opd3"
            )

        val contentPackage =
            ContentPackage(
                id =
                    PackageId(
                        "package-invalid"
                    ),
                descriptor =
                    PackageDescriptor(
                        name =
                            "Invalid Package",
                        version =
                            "1.0.0",
                        format =
                            "ZIP"
                    )
            )

        val content =
            Content(
                id =
                    ContentId(
                        "content-one"
                    ),
                type =
                    ContentType.WORD,
                text =
                    ContentText(
                        primaryText =
                            "hello"
                    )
            )

        val learningItem =
            LearningItem(
                id =
                    LearningItemId(
                        "item-one"
                    ),
                contentId =
                    content.id,
                mode =
                    LearningMode.MEANING_RECOGNITION
            )

        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        var transactionStarted =
            false

        val transactionRunner =
            object : TransactionRunner {
                override fun <T> runInTransaction(
                    block: () -> T
                ): T {
                    transactionStarted =
                        true

                    return block()
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
                            contents =
                                listOf(
                                    content
                                ),
                            learningItems =
                                listOf(
                                    learningItem
                                ),
                            libraries =
                                emptyList()
                        )
                    },
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                packageRegistrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository =
                            contentPackageRepository,
                        packageCatalogRepository =
                            packageCatalogRepository
                    ),
                transactionRunner =
                    transactionRunner
            )

        val exception =
            assertFailsWith<InvalidPackageException> {
                service.importCandidate(
                    catalogId =
                        PackageCatalogId(
                            "validation-catalog"
                        ),
                    candidate =
                        candidate
                )
            }

        assertEquals(
            listOf(
                "UNSUPPORTED_PACKAGE_FORMAT"
            ),
            exception.report.errors.map { issue ->
                issue.code
            }
        )

        assertFalse(
            transactionStarted
        )

        assertEquals(
            0,
            contentRepository.count()
        )

        assertEquals(
            0,
            learningItemRepository.count()
        )

        assertEquals(
            0,
            contentPackageRepository.count()
        )

        assertTrue(
            packageCatalogRepository.findById(
                PackageCatalogId(
                    "validation-catalog"
                )
            ) == null
        )
    }

    @Test
    fun `missing package dependency is rejected before transaction and persistence`() {
        val candidate =
            PackageScanCandidate(
                source =
                    "C:/packages/dependent.opd3"
            )

        val missingDependency =
            PackageDependency(
                packageName =
                    "Core English",
                minimumVersion =
                    "2.0.0"
            )

        val contentPackage =
            ContentPackage(
                id =
                    PackageId(
                        "dependent-package"
                    ),
                descriptor =
                    PackageDescriptor(
                        name =
                            "Vocabulary Extension",
                        version =
                            "1.0.0",
                        format =
                            "OPD3",
                        dependencies =
                            setOf(
                                missingDependency
                            )
                    )
            )

        val content =
            Content(
                id =
                    ContentId(
                        "dependent-content"
                    ),
                type =
                    ContentType.WORD,
                text =
                    ContentText(
                        primaryText =
                            "hello"
                    )
            )

        val learningItem =
            LearningItem(
                id =
                    LearningItemId(
                        "dependent-item"
                    ),
                contentId =
                    content.id,
                mode =
                    LearningMode.MEANING_RECOGNITION
            )

        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        var transactionStarted =
            false

        val transactionRunner =
            object : TransactionRunner {
                override fun <T> runInTransaction(
                    block: () -> T
                ): T {
                    transactionStarted =
                        true

                    return block()
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
                            contents =
                                listOf(
                                    content
                                ),
                            learningItems =
                                listOf(
                                    learningItem
                                ),
                            libraries =
                                emptyList()
                        )
                    },
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                packageRegistrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository =
                            contentPackageRepository,
                        packageCatalogRepository =
                            packageCatalogRepository
                    ),
                transactionRunner =
                    transactionRunner
            )

        val exception =
            assertFailsWith<MissingPackageDependenciesException> {
                service.importCandidate(
                    catalogId =
                        PackageCatalogId(
                            "dependency-validation-catalog"
                        ),
                    candidate =
                        candidate
                )
            }

        assertEquals(
            contentPackage.id,
            exception.packageId
        )

        assertEquals(
            setOf(
                missingDependency
            ),
            exception.missingDependencies
        )

        assertFalse(
            transactionStarted
        )

        assertEquals(
            0,
            contentRepository.count()
        )

        assertEquals(
            0,
            learningItemRepository.count()
        )

        assertEquals(
            0,
            contentPackageRepository.count()
        )

        assertTrue(
            packageCatalogRepository.findById(
                PackageCatalogId(
                    "dependency-validation-catalog"
                )
            ) == null
        )
    }
}