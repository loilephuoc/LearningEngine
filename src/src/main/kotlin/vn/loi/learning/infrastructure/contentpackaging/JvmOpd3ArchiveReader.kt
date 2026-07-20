package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

/**
 * Implementation JVM mở file OPD3 bằng ZipFile.
 */
class JvmOpd3ArchiveReader : Opd3ArchiveReader {

    override fun open(
        packageFile: Path
    ): ZipFile {
        require(Files.exists(packageFile)) {
            "Package file does not exist: $packageFile"
        }

        require(Files.isRegularFile(packageFile)) {
            "Package path must be a regular file: $packageFile"
        }

        return ZipFile(packageFile.toFile())
    }
}