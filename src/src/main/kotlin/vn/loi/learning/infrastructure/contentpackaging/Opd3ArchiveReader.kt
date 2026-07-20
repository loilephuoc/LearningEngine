package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import java.util.zip.ZipFile

/**
 * Mở một package OPD3 dưới dạng ZIP archive.
 *
 * Reader chỉ chịu trách nhiệm mở archive.
 * Việc đọc manifest hoặc dữ liệu bên trong được giao
 * cho các component chuyên biệt khác.
 */
fun interface Opd3ArchiveReader {

    fun open(
        packageFile: Path
    ): ZipFile
}