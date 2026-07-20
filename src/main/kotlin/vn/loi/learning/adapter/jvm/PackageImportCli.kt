package vn.loi.learning.adapter.jvm

import java.nio.file.Path
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.PersistedLearningPlatformFactory

object PackageImportCli {

    fun run(
        persistenceDirectory: Path,
        packageDirectory: Path,
        catalogId: PackageCatalogId
    ): Int {
        val platform =
            PersistedLearningPlatformFactory.createPlatform(
                persistenceDirectory =
                    persistenceDirectory,
                packageDirectory =
                    packageDirectory
            )

        val results =
            platform.packageImportService.importAll(
                catalogId
            )

        println("=== PACKAGE IMPORT ===")
        println("Package directory: $packageDirectory")
        println("Persistence directory: $persistenceDirectory")
        println("Catalog ID: $catalogId")
        println("Imported packages: ${results.size}")

        results.forEachIndexed { index, result ->
            println()
            println("Package #${index + 1}")
            println("ID: ${result.contentPackage.id}")
            println("Name: ${result.contentPackage.name}")
            println("Version: ${result.contentPackage.version}")
            println("Format: ${result.contentPackage.format}")
            println(
                "Libraries: ${result.importedLibraryCount}"
            )
            println(
                "Contents: ${result.importedContentCount}"
            )
            println(
                "Learning items: " +
                        result.importedLearningItemCount
            )

            if (result.warnings.isNotEmpty()) {
                println("Warnings:")

                result.warnings.forEach { warning ->
                    println("- $warning")
                }
            }
        }

        return results.size
    }
}