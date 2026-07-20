package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.application.contentpackaging.InvalidPackageFormatException
import vn.loi.learning.application.contentpackaging.InvalidPackageVersionException
import vn.loi.learning.application.contentpackaging.MissingPackageManifestException
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class JvmOpd3PackageDescriptorReaderTest {

    @Test
    fun `reads package descriptor from manifest`() {
        val directory=createTempDirectory("opd3-descriptor")
        val archive=directory.resolve("package.opd3")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"name":"English Elementary","version":"1.0","format":"OPD3"}""".toByteArray())
            zip.closeEntry()
        }
        val reader=JvmOpd3PackageDescriptorReader(JvmOpd3ArchiveReader(),JvmOpd3EntryReader())
        val result=reader.read(PackageScanCandidate(archive.toString()))
        assertEquals(PackageDescriptor("English Elementary","1.0","OPD3"),result)
    }

    @Test
    fun `rejects missing manifest`() {
        val directory=createTempDirectory("opd3-no-manifest")
        val archive=directory.resolve("package.opd3")
        ZipOutputStream(Files.newOutputStream(archive)).use { }
        val reader=JvmOpd3PackageDescriptorReader(JvmOpd3ArchiveReader(),JvmOpd3EntryReader())
        assertFailsWith<MissingPackageManifestException> { reader.read(PackageScanCandidate(archive.toString())) }
    }

    @Test
    fun `rejects invalid package format`() {
        val directory=createTempDirectory("opd3-invalid-format")
        val archive=directory.resolve("package.opd3")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"name":"English Elementary","version":"1.0","format":"OPD2"}""".toByteArray())
            zip.closeEntry()
        }
        val reader=JvmOpd3PackageDescriptorReader(JvmOpd3ArchiveReader(),JvmOpd3EntryReader())
        assertFailsWith<InvalidPackageFormatException> { reader.read(PackageScanCandidate(archive.toString())) }
    }


    @Test
    fun `rejects blank package version`() {
        val directory=createTempDirectory("opd3-blank-version")
        val archive=directory.resolve("package.opd3")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"name":"English Elementary","version":"","format":"OPD3"}""".toByteArray())
            zip.closeEntry()
        }
        val reader=JvmOpd3PackageDescriptorReader(JvmOpd3ArchiveReader(),JvmOpd3EntryReader())
        assertFailsWith<InvalidPackageVersionException> { reader.read(PackageScanCandidate(archive.toString())) }
    }
}





