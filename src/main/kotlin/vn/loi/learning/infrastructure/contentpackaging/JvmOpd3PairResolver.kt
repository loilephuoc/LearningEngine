package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import vn.loi.learning.application.contentpackaging.AmbiguousOpd3JsonPairException
import vn.loi.learning.application.contentpackaging.LegacyPackageCandidate
import vn.loi.learning.application.contentpackaging.MissingOpd3JsonPairException

class JvmOpd3PairResolver(
    private val listFiles: (Path) -> List<Path> = { directory ->
        Files.list(directory).use { paths -> paths.toList() }
    }
) {

    fun resolve(packageFile: Path): LegacyPackageCandidate {
        val fileName = packageFile.fileName.toString()
        val separator = fileName.lastIndexOf('.')
        val baseName = if (separator > 0) fileName.substring(0, separator) else fileName
        val expected = "$baseName.json"
        val parent = packageFile.toAbsolutePath().parent
        val matches = listFiles(parent)
                .asSequence()
                .filter(Files::isRegularFile)
                .filter { candidate ->
                    candidate.fileName.toString().lowercase(Locale.ROOT) ==
                        expected.lowercase(Locale.ROOT)
                }
                .sorted()
                .toList()

        if (matches.isEmpty()) {
            throw MissingOpd3JsonPairException(packageFile.toString())
        }
        if (matches.size > 1) {
            throw AmbiguousOpd3JsonPairException(packageFile.toString())
        }

        return LegacyPackageCandidate(
            jsonSource = matches.single().toString(),
            mediaSource = packageFile.toString()
        )
    }
}
