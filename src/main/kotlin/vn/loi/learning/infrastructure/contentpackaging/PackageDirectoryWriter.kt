package vn.loi.learning.infrastructure.contentpackaging

import vn.loi.learning.application.contentpackaging.PackageExportBundle
import java.io.File

/**
 * Ghi export bundle thành thư mục package vật lý.
 */
class PackageDirectoryWriter {

    companion object {
        const val UTF8 = "UTF-8"
    }

    fun fileCount(bundle: PackageExportBundle): Int =
        bundle.files.size

    fun writeFile(
        file: vn.loi.learning.application.contentpackaging.PackageExportFile,
        directory: File
    ) {
        val target = File(directory, file.relativePath)
        target.parentFile?.mkdirs()
        target.writeText(file.content, Charsets.UTF_8)
    }

    fun write(
        bundle: PackageExportBundle,
        directory: File
    ) {
        require(bundle.isNotEmpty()) {
            "Cannot write empty export bundle."
        }

        directory.mkdirs()

        bundle.files.forEach { file ->
            val target = File(directory, file.relativePath)
            target.parentFile?.mkdirs()
            target.writeText(file.content, Charsets.UTF_8)
        }
    }

    fun validate(
        bundle: PackageExportBundle,
        directory: File
    ): Boolean =
        bundle.files.all { file -> File(directory, file.relativePath).readText(Charsets.UTF_8) == file.content }

    fun exists(
        bundle: PackageExportBundle,
        directory: File
    ): Boolean =
        bundle.files.all { File(directory, it.relativePath).exists() }
}
