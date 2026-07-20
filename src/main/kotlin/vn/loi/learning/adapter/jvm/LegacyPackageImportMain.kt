package vn.loi.learning.adapter.jvm

import java.nio.file.Path
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

/**
 * Entry point:
 *
 * legacyPackageImport
 * <persistence-directory>
 * <source-directory>
 * <media-directory>
 * <catalog-id>
 */
fun main(
    args: Array<String>
) {
    require(
        args.size == ARGUMENT_COUNT
    ) {
        usage()
    }

    LegacyPackageImportCli.run(
        persistenceDirectory =
            Path.of(
                args[PERSISTENCE_DIRECTORY_INDEX]
            ),
        sourceDirectory =
            Path.of(
                args[SOURCE_DIRECTORY_INDEX]
            ),
        mediaDirectory =
            Path.of(
                args[MEDIA_DIRECTORY_INDEX]
            ),
        catalogId =
            PackageCatalogId(
                args[CATALOG_ID_INDEX]
            )
    )
}

private fun usage(): String =
    "Usage: legacyPackageImport " +
            "<persistence-directory> " +
            "<source-directory> " +
            "<media-directory> " +
            "<catalog-id>"

private const val ARGUMENT_COUNT =
    4

private const val PERSISTENCE_DIRECTORY_INDEX =
    0

private const val SOURCE_DIRECTORY_INDEX =
    1

private const val MEDIA_DIRECTORY_INDEX =
    2

private const val CATALOG_ID_INDEX =
    3