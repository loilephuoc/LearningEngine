package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import vn.loi.learning.application.contentpackaging.AmbiguousOpd3JsonPairException
import vn.loi.learning.application.contentpackaging.LegacyPackageCandidate
import vn.loi.learning.application.contentpackaging.MissingOpd3JsonPairException

class JvmOpd3PairResolver(
    private val listFiles: (Path) -> List<Path> = { directory ->
        Files.list(directory).use { paths -> paths.filter(Files::isRegularFile).toList() }
    }
) {

    fun resolve(packageFile: Path): LegacyPackageCandidate {
        val fileName = packageFile.fileName.toString()
        val separator = fileName.lastIndexOf('.')
        val baseName = if (separator > 0) fileName.substring(0, separator) else fileName
        val parent = packageFile.toAbsolutePath().parent
            ?: throw MissingOpd3JsonPairException(packageFile.toString())

        val dirFiles = listFiles(parent)

        return if (fileName.endsWith(".json", ignoreCase = true)) {
            val expectedPkg = "$baseName.pkg".lowercase(Locale.ROOT)
            val pkgMatches = dirFiles.filter { candidate ->
                candidate.fileName.toString().lowercase(Locale.ROOT) == expectedPkg
            }.sorted()

            if (pkgMatches.isEmpty()) {
                throw MissingOpd3JsonPairException(packageFile.toString())
            }
            if (pkgMatches.size > 1) {
                throw AmbiguousOpd3JsonPairException(packageFile.toString())
            }

            LegacyPackageCandidate(
                jsonSource = packageFile.toString(),
                mediaSource = pkgMatches.single().toString()
            )
        } else {
            val expectedJson = "$baseName.json".lowercase(Locale.ROOT)
            val jsonMatches = dirFiles.filter { candidate ->
                candidate.fileName.toString().lowercase(Locale.ROOT) == expectedJson
            }.sorted()

            if (jsonMatches.isEmpty()) {
                throw MissingOpd3JsonPairException(packageFile.toString())
            }
            if (jsonMatches.size > 1) {
                throw AmbiguousOpd3JsonPairException(packageFile.toString())
            }

            LegacyPackageCandidate(
                jsonSource = jsonMatches.single().toString(),
                mediaSource = packageFile.toString()
            )
        }
    }
}
