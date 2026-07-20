package vn.loi.learning.infrastructure.contentpackaging

import vn.loi.learning.application.contentpackaging.PackageExportBundle
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Ghi export bundle thành file ZIP.
 */
class PackageZipWriter {

    fun exists(
        zipFile: File
    ): Boolean =
        zipFile.exists()

    fun validate(
        bundle: PackageExportBundle,
        zipFile: File
    ): Boolean =
        zipFile.exists() && bundle.files.isNotEmpty()

    fun zipSize(zipFile: File): Long =
        zipFile.length()

    fun isValid(zipFile: File): Boolean =
        zipFile.exists() && zipFile.length() > 0

    fun delete(zipFile: File): Boolean =
        zipFile.delete()

    fun fileCount(
        bundle: PackageExportBundle
    ): Int =
        bundle.files.size

    fun write(
        bundle: PackageExportBundle,
        zipFile: File
    ) {
        require(bundle.isNotEmpty()) {
            "Cannot write empty export bundle."
        }

        zipFile.parentFile?.mkdirs()

        ZipOutputStream(zipFile.outputStream()).use { zip ->
            bundle.files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.relativePath))
                zip.write(file.content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }
}
