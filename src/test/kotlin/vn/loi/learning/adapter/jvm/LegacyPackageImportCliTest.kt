package vn.loi.learning.adapter.jvm

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

class LegacyPackageImportCliTest {

    @Test
    fun `cli returns zero when source directory has no legacy packages`() {
        val rootDirectory =
            createTempDirectory(
                prefix =
                    "legacy-package-import-cli-"
            )

        val persistenceDirectory =
            rootDirectory.resolve(
                "persistence"
            )

        val sourceDirectory =
            rootDirectory.resolve(
                "source"
            )

        val mediaDirectory =
            rootDirectory.resolve(
                "media"
            )

        Files.createDirectories(
            sourceDirectory
        )

        val importedPackageCount =
            LegacyPackageImportCli.run(
                persistenceDirectory =
                    persistenceDirectory,
                sourceDirectory =
                    sourceDirectory,
                mediaDirectory =
                    mediaDirectory,
                catalogId =
                    PackageCatalogId(
                        "legacy-test-catalog"
                    )
            )

        assertEquals(
            0,
            importedPackageCount
        )

        assertTrue(
            Files.isDirectory(
                persistenceDirectory
            )
        )

        assertTrue(
            Files.isDirectory(
                mediaDirectory
            )
        )
    }
}