package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.contentpackaging.PackageImportFailureKind
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.contentpackaging.PackageRegistrationOperation
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageArchiveStructureImportIntegrationTest {

    @Test
    fun `malformed archive is not persisted and detailed batch continues`() {
        val directory = Files.createTempDirectory(
            "opd3-structure-import-"
        )

        try {
            writeMinimalPackage(
                directory.resolve("a-invalid.opd3"),
                packageName = "Invalid package",
                extraEntryName = "../unexpected.json"
            )

            writeMinimalPackage(
                directory.resolve("b-valid.opd3"),
                packageName = "Valid package"
            )

            val contentRepository = InMemoryContentRepository()
            val learningItemRepository = InMemoryLearningItemRepository()
            val libraryRepository = InMemoryContentLibraryRepository()
            val packageRepository = InMemoryContentPackageRepository()
            val catalogRepository = InMemoryPackageCatalogRepository()

            val service = PackageImportService(
                packageScanner =
                    ContentPackageImportFactory.createScanner(directory),
                packageInstaller =
                    ContentPackageImportFactory.createInstaller(),
                packageContentImporter =
                    ContentPackageImportFactory.createContentImporter(),
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                packageRegistrationOperation =
                    PackageRegistrationOperation(
                        contentPackageRepository = packageRepository,
                        packageCatalogRepository = catalogRepository
                    ),
                transactionRunner = InMemoryTransactionRunner(),
                contentLibraryRepository = libraryRepository
            )

            val result = service.importAllDetailed(
                PackageCatalogId("structure-catalog")
            )

            assertEquals(2, result.discoveredPackageCount)
            assertEquals(1, result.successfulImports.size)
            assertEquals(1, result.failures.size)
            assertEquals(
                PackageImportFailureKind.MALFORMED_PACKAGE,
                result.failures.single().kind
            )
            assertEquals(
                "PACKAGE_MALFORMED",
                result.failures.single().code
            )
            assertEquals(1, packageRepository.count())
            assertEquals(1, libraryRepository.count())
            assertEquals(0, contentRepository.count())
            assertEquals(0, learningItemRepository.count())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun writeMinimalPackage(
        path: Path,
        packageName: String,
        extraEntryName: String? = null
    ) {
        ZipOutputStream(
            Files.newOutputStream(path)
        ).use { output ->
            writeEntry(
                output,
                "manifest.json",
                """
                {
                  "name": "$packageName",
                  "version": "1.0.0",
                  "format": "OPD3",
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent()
            )
            writeEntry(output, "metadata.json", "{}")
            writeEntry(output, "contents.json", "{\"contents\":[]}")
            writeEntry(
                output,
                "learning-items.json",
                "{\"learningItems\":[]}"
            )

            extraEntryName?.let { entryName ->
                writeEntry(output, entryName, "{}")
            }
        }
    }

    private fun writeEntry(
        output: ZipOutputStream,
        name: String,
        content: String
    ) {
        output.putNextEntry(ZipEntry(name))
        output.write(content.toByteArray(Charsets.UTF_8))
        output.closeEntry()
    }
}
