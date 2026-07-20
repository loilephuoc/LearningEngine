package vn.loi.learning.infrastructure.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.io.path.createTempDirectory
import vn.loi.learning.application.contentpackaging.PackageExportBundle
import vn.loi.learning.application.contentpackaging.PackageExportFile
import java.io.File

class PackageDirectoryWriterTest {

    @Test
    fun `writes bundle files to directory`() {
        val directory = createTempDirectory().toFile()
        val bundle = PackageExportBundle(
            listOf(
                PackageExportFile("metadata.json", "{}"),
                PackageExportFile("manifest.json", "{}")
            )
        )

        PackageDirectoryWriter().write(bundle, directory)

        assertEquals("{}", File(directory, "metadata.json").readText())
        assertEquals("{}", File(directory, "manifest.json").readText())
        assertEquals(true, PackageDirectoryWriter().exists(bundle, directory))
        assertEquals(2, PackageDirectoryWriter().fileCount(bundle))
        PackageDirectoryWriter().writeFile(bundle.files.first(), directory)
        assertEquals("{}", File(directory, "metadata.json").readText())
        assertEquals(true, PackageDirectoryWriter().validate(bundle, directory))
        assertEquals(true, PackageDirectoryWriter().exists(bundle, directory))
        assertEquals(2, PackageDirectoryWriter().fileCount(bundle))
        PackageDirectoryWriter().writeFile(bundle.files.first(), directory)
        assertEquals("{}", File(directory, "metadata.json").readText())
        assertEquals(true, directory.exists())
        directory.deleteRecursively()
    }
}
