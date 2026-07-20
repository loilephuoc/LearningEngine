package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.contentpackaging.PackageRegistrationOperation
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageImportWorkflowIntegrationTest {

    @Test
    fun `imports scanned OPD3 package through complete workflow`() {
        val directory =
            Files.createTempDirectory(
                "package-import-workflow-test"
            )

        val packageFile =
            directory.resolve("workflow.opd3")

        try {
            ZipOutputStream(
                Files.newOutputStream(packageFile)
            ).use { zip ->
                writeEntry(
                    zip = zip,
                    name = "manifest.json",
                    content =
                        """
                        {
                          "name": "Workflow Package",
                          "version": "1.0.0",
                          "format": "OPD3",
                          "contentCount": 1,
                          "learningItemCount": 1
                        }
                        """.trimIndent()
                )

                writeEntry(
                    zip = zip,
                    name = "metadata.json",
                    content =
                        """
                        {
                          "name": "Workflow Package"
                        }
                        """.trimIndent()
                )

                writeEntry(
                    zip = zip,
                    name = "contents.json",
                    content =
                        """
                        {
                          "contents": [
                            {
                              "id": "workflow-content",
                              "type": "WORD",
                              "primaryText": "workflow",
                              "translatedText": "quy trinh",
                              "tags": [],
                              "customFields": {}
                            }
                          ]
                        }
                        """.trimIndent()
                )

                writeEntry(
                    zip = zip,
                    name = "learning-items.json",
                    content =
                        """
                        {
                          "learningItems": [
                            {
                              "id": "workflow-learning-item",
                              "contentId": "workflow-content",
                              "mode": "MEANING_RECOGNITION",
                              "isEnabled": true
                            }
                          ]
                        }
                        """.trimIndent()
                )
            }

            val contentRepository =
                InMemoryContentRepository()

            val learningItemRepository =
                InMemoryLearningItemRepository()

            val contentLibraryRepository =
                InMemoryContentLibraryRepository()

            val contentPackageRepository =
                InMemoryContentPackageRepository()

            val packageCatalogRepository =
                InMemoryPackageCatalogRepository()

            val service =
                PackageImportService(
                    packageScanner =
                        ContentPackageImportFactory.createScanner(
                            directory
                        ),
                    packageInstaller =
                        ContentPackageImportFactory.createInstaller(),
                    packageContentImporter =
                        ContentPackageImportFactory
                            .createContentImporter(),
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
                        InMemoryTransactionRunner(),
                    contentLibraryRepository =
                        contentLibraryRepository
                )

            val catalogId =
                PackageCatalogId("workflow-catalog")

            val results =
                service.importAll(catalogId)

            assertEquals(1, results.size)

            val result =
                results.single()

            assertEquals(
                1,
                result.importedContentCount
            )

            assertEquals(
                1,
                result.importedLearningItemCount
            )

            assertEquals(
                1,
                contentRepository.count()
            )

            assertEquals(
                1,
                learningItemRepository.count()
            )

            assertEquals(
                1,
                contentPackageRepository.count()
            )

            val catalog =
                assertNotNull(
                    packageCatalogRepository.findById(
                        catalogId
                    )
                )

            assertTrue(
                catalog.contains(
                    result.contentPackage.id
                )
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun writeEntry(
        zip: ZipOutputStream,
        name: String,
        content: String
    ) {
        zip.putNextEntry(
            ZipEntry(name)
        )

        zip.write(
            content.toByteArray(
                Charsets.UTF_8
            )
        )

        zip.closeEntry()
    }
}