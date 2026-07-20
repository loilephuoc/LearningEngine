package vn.loi.learning.infrastructure.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.io.path.createTempDirectory
import vn.loi.learning.application.contentpackaging.PackageExportBundle
import vn.loi.learning.application.contentpackaging.PackageExportFile
import java.io.File
import java.util.zip.ZipFile

class PackageZipWriterTest {

    @Test
    fun `writes bundle as zip`() {
        val zip = File(createTempDirectory().toFile(), "package.zip")
        val bundle = PackageExportBundle(
            listOf(
                PackageExportFile("metadata.json", "{}"),
                PackageExportFile("manifest.json", "{}")
            )
        )

        val writer = PackageZipWriter()
        writer.write(bundle, zip)
        assertEquals(true, writer.exists(zip))

        assertEquals(2, PackageZipWriter().fileCount(bundle))
        assertEquals(true, PackageZipWriter().validate(bundle, zip))
        assertEquals(true, zip.exists())
        assertEquals(true, PackageZipWriter().zipSize(zip) > 0)
        assertEquals(true, PackageZipWriter().isValid(zip))



        ZipFile(zip).use { archive ->
            assertEquals("{}", archive.getInputStream(archive.getEntry("metadata.json")).readBytes().toString(Charsets.UTF_8))
            assertEquals("{}", archive.getInputStream(archive.getEntry("manifest.json")).readBytes().toString(Charsets.UTF_8))
        }
        zip.delete()
    }
}
