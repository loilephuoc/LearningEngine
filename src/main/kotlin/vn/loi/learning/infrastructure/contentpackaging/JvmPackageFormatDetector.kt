package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import vn.loi.learning.application.contentpackaging.UnsupportedPackageTypeException

class JvmPackageFormatDetector {

    fun detect(source: Path): JvmPackageFormat {
        val targetPath = if (source.fileName.toString().lowercase(Locale.ROOT).endsWith(".json")) {
            val baseName = source.fileName.toString().substringBeforeLast('.')
            val pkgName = "$baseName.pkg"
            val parent = source.parent
            if (parent != null) {
                val matching = Files.list(parent).use { paths ->
                    paths.filter(Files::isRegularFile)
                        .filter { it.fileName.toString().lowercase(Locale.ROOT) == pkgName.lowercase(Locale.ROOT) }
                        .findFirst()
                        .orElse(null)
                }
                matching ?: source
            } else {
                source
            }
        } else {
            source
        }

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw UnsupportedPackageTypeException(source.toString())
        }

        val signature = Files.newInputStream(targetPath).use { input ->
            input.readNBytes(SIGNATURE_SIZE)
        }

        if (signature.size == SIGNATURE_SIZE && signature.contentEquals(OPD3_SIGNATURE)) {
            return JvmPackageFormat.OPD3_BINARY_PAIR
        }

        if (signature.size == SIGNATURE_SIZE && ZIP_SIGNATURES.any(signature::contentEquals)) {
            return JvmPackageFormat.ZIP_ARCHIVE
        }

        throw UnsupportedPackageTypeException(source.toString())
    }

    private companion object {
        const val SIGNATURE_SIZE = 4

        val OPD3_SIGNATURE = "OPD3".toByteArray(Charsets.US_ASCII)

        val ZIP_SIGNATURES = listOf(
            byteArrayOf(0x50, 0x4b, 0x03, 0x04),
            byteArrayOf(0x50, 0x4b, 0x05, 0x06),
            byteArrayOf(0x50, 0x4b, 0x07, 0x08)
        )
    }
}

enum class JvmPackageFormat {
    ZIP_ARCHIVE,
    OPD3_BINARY_PAIR
}
