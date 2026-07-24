package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

sealed interface PackageImportSource {
    val displayName: String

    data class Opd3File(
        val file: Path
    ) : PackageImportSource {
        override val displayName: String get() = file.fileName.toString()
    }

    data class LegacyPair(
        val jsonFile: Path,
        val packageFile: Path
    ) : PackageImportSource {
        override val displayName: String get() = jsonFile.fileName.toString().substringBeforeLast('.')
    }
}

class InvalidImportSelectionException(
    message: String
) : RuntimeException(message)

object PackageImportSourceResolver {

    fun resolve(files: List<Path>): PackageImportSource {
        if (files.isEmpty()) {
            throw InvalidImportSelectionException("To import a legacy topic, select its matching .json and .pkg files together.")
        }

        if (files.size == 1) {
            val single = files.single()
            val fileName = single.fileName.toString().lowercase(Locale.ROOT)
            return when {
                fileName.endsWith(".opd3") -> PackageImportSource.Opd3File(single)
                fileName.endsWith(".json") -> {
                    val pkgPath = findCompanionFile(single, ".pkg")
                        ?: throw InvalidImportSelectionException("To import a legacy topic, select its matching .json and .pkg files together.")
                    PackageImportSource.LegacyPair(jsonFile = single, packageFile = pkgPath)
                }
                fileName.endsWith(".pkg") -> {
                    val jsonPath = findCompanionFile(single, ".json")
                        ?: throw InvalidImportSelectionException("To import a legacy topic, select its matching .json and .pkg files together.")
                    PackageImportSource.LegacyPair(jsonFile = jsonPath, packageFile = single)
                }
                else -> throw InvalidImportSelectionException("To import a legacy topic, select its matching .json and .pkg files together.")
            }
        }

        if (files.size == 2) {
            val jsonFile = files.firstOrNull { it.fileName.toString().lowercase(Locale.ROOT).endsWith(".json") }
            val pkgFile = files.firstOrNull { it.fileName.toString().lowercase(Locale.ROOT).endsWith(".pkg") }

            if (jsonFile == null || pkgFile == null) {
                throw InvalidImportSelectionException("To import a legacy topic, select its matching .json and .pkg files together.")
            }

            val jsonBase = jsonFile.fileName.toString().substringBeforeLast('.').lowercase(Locale.ROOT)
            val pkgBase = pkgFile.fileName.toString().substringBeforeLast('.').lowercase(Locale.ROOT)

            if (jsonBase != pkgBase) {
                throw InvalidImportSelectionException("To import a legacy topic, select its matching .json and .pkg files together.")
            }

            return PackageImportSource.LegacyPair(jsonFile = jsonFile, packageFile = pkgFile)
        }

        throw InvalidImportSelectionException("To import a legacy topic, select its matching .json and .pkg files together.")
    }

    private fun findCompanionFile(file: Path, extension: String): Path? {
        val parent = file.toAbsolutePath().parent ?: return null
        val baseName = file.fileName.toString().substringBeforeLast('.').lowercase(Locale.ROOT)
        val targetName = (baseName + extension).lowercase(Locale.ROOT)

        return try {
            Files.list(parent).use { paths ->
                paths.filter(Files::isRegularFile)
                    .filter { it.fileName.toString().lowercase(Locale.ROOT) == targetName }
                    .findFirst()
                    .orElse(null)
            }
        } catch (_: Exception) {
            null
        }
    }
}
