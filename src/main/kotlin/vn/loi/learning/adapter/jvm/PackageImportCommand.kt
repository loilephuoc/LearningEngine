package vn.loi.learning.adapter.jvm

import java.nio.file.Path
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

object PackageImportCommand {

    const val NAME =
        "package-import"

    fun execute(
        args: List<String>
    ): Int {
        require(args.size == 3) {
            usage()
        }

        return PackageImportCli.run(
            persistenceDirectory =
                Path.of(args[0]),
            packageDirectory =
                Path.of(args[1]),
            catalogId =
                PackageCatalogId(args[2])
        )
    }

    fun usage(): String =
        "Usage: package-import " +
                "<persistence-directory> " +
                "<package-directory> " +
                "<catalog-id>"
}