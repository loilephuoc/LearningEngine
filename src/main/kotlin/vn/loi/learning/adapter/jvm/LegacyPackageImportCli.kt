package vn.loi.learning.adapter.jvm

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.PersistedLegacyPackageImportFactory

/**
 * CLI import package legacy JSON + PKG.
 */
object LegacyPackageImportCli {

    fun run(
        persistenceDirectory: Path,
        sourceDirectory: Path,
        mediaDirectory: Path,
        catalogId: PackageCatalogId
    ): Int {
        require(
            Files.isDirectory(
                sourceDirectory
            )
        ) {
            "Legacy source directory does not exist: $sourceDirectory"
        }

        Files.createDirectories(
            persistenceDirectory
        )

        Files.createDirectories(
            mediaDirectory
        )

        val workflow =
            PersistedLegacyPackageImportFactory.create(
                persistenceDirectory =
                    persistenceDirectory,
                sourceDirectory =
                    sourceDirectory,
                mediaDirectory =
                    mediaDirectory
            )

        val result =
            workflow.execute(
                catalogId
            )

        println("=== LEGACY PACKAGE IMPORT ===")
        println(
            "Source directory: $sourceDirectory"
        )
        println(
            "Media directory: $mediaDirectory"
        )
        println(
            "Persistence directory: $persistenceDirectory"
        )
        println(
            "Catalog ID: $catalogId"
        )
        println(
            "Scanned candidates: " +
                    result.scannedCandidateCount
        )
        println(
            "Imported packages: " +
                    result.importedPackageCount
        )
        println(
            "Imported contents: " +
                    result.importedContentCount
        )
        println(
            "Imported learning items: " +
                    result.importedLearningItemCount
        )

        result.importedPackages.forEachIndexed {
                index,
                packageResult ->

            println()
            println(
                "Package #${index + 1}"
            )
            println(
                "ID: ${packageResult.contentPackage.id}"
            )
            println(
                "Name: ${packageResult.contentPackage.name}"
            )
            println(
                "Version: ${packageResult.contentPackage.version}"
            )
            println(
                "Format: ${packageResult.contentPackage.format}"
            )
            println(
                "Contents: " +
                        packageResult.importedContentCount
            )
            println(
                "Learning items: " +
                        packageResult.importedLearningItemCount
            )

            if (
                packageResult.warnings.isNotEmpty()
            ) {
                println(
                    "Warnings:"
                )

                packageResult.warnings.forEach { warning ->
                    println(
                        "- $warning"
                    )
                }
            }
        }

        return result.importedPackageCount
    }
}