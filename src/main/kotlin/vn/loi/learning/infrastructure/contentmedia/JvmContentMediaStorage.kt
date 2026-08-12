package vn.loi.learning.infrastructure.contentmedia

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage

class JvmContentMediaStorage(
    rootDirectory: Path
) : ContentMediaStorage {

    private val rootDirectory: Path =
        rootDirectory
            .toAbsolutePath()
            .normalize()

    init {
        Files.createDirectories(
            this.rootDirectory
        )
    }

    override fun store(
        packageName: String,
        fileName: String,
        content: ByteArray
    ): ContentMediaAsset {
        require(packageName.isNotBlank()) {
            "Package name must not be blank."
        }

        require(fileName.isNotBlank()) {
            "Media file name must not be blank."
        }

        val packageDirectory =
            resolvePackageDirectory(
                packageName
            )

        val targetPath =
            packageDirectory
                .resolve(fileName)
                .normalize()

        require(
            targetPath.startsWith(
                packageDirectory
            )
        ) {
            "Media file must remain inside its package directory: $fileName"
        }

        Files.createDirectories(
            requireNotNull(
                targetPath.parent
            )
        )

        Files.write(
            targetPath,
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )

        return ContentMediaAsset(
            packageName = packageName,
            fileName = fileName,
            relativePath =
                toPortableRelativePath(
                    targetPath
                )
        )
    }

    override fun resolve(
        relativePath: String
    ): Path? {
        if (relativePath.isBlank()) {
            return null
        }

        val cleanRelativePath =
            relativePath
                .trim()
                .replace('\\', '/')
                .removePrefix("./")

        val exactCandidatePaths = listOf(
            cleanRelativePath,
            cleanRelativePath.removePrefix("media/"),
            cleanRelativePath.removePrefix("/media/")
        ).distinct()

        // Legacy packages occasionally contain an image reference whose extension
        // does not match the actual stored media file (for example .png in JSON
        // while the package contains the same asset as .jpg). Prefer the exact
        // reference first, then try the same basename with compatible image
        // extensions so previews, Open/Show in Folder, and image copy can still
        // resolve the real file.
        val compatibleImageExtensions = listOf("jpg", "jpeg", "png", "webp")
        val candidatePaths = buildList {
            addAll(exactCandidatePaths)

            exactCandidatePaths.forEach { candidate ->
                val extension = candidate.substringAfterLast('.', missingDelimiterValue = "").lowercase()
                if (extension in compatibleImageExtensions) {
                    val withoutExtension =
                        candidate.substringBeforeLast('.', missingDelimiterValue = candidate)
                    compatibleImageExtensions
                        .filterNot { it == extension }
                        .forEach { alternativeExtension ->
                            add("$withoutExtension.$alternativeExtension")
                        }
                }
            }
        }.distinct()

        for (candidate in candidatePaths) {
            val resolvedPath =
                rootDirectory
                    .resolve(candidate)
                    .normalize()

            if (resolvedPath.startsWith(rootDirectory) && Files.isRegularFile(resolvedPath)) {
                return resolvedPath
            }

            val parentMedia = rootDirectory.parent?.resolve("media")
            if (parentMedia != null && parentMedia != rootDirectory) {
                val fallbackPath = parentMedia.resolve(candidate).normalize()
                if (fallbackPath.startsWith(parentMedia) && Files.isRegularFile(fallbackPath)) {
                    return fallbackPath
                }
            }
        }

        if (Files.isDirectory(rootDirectory)) {
            runCatching {
                Files.list(rootDirectory).use { stream ->
                    val pkgDirs = stream.filter { Files.isDirectory(it) }.toList()
                    for (pkgDir in pkgDirs) {
                        for (candidate in candidatePaths) {
                            val subPath = pkgDir.resolve(candidate).normalize()
                            if (subPath.startsWith(pkgDir) && Files.isRegularFile(subPath)) {
                                return subPath
                            }
                        }
                    }
                }
            }
        }

        val directPath = runCatching { Path.of(cleanRelativePath) }.getOrNull()
        if (directPath != null && Files.isRegularFile(directPath)) {
            return directPath
        }

        return null
    }

    override fun exists(
        relativePath: String
    ): Boolean =
        resolve(relativePath) != null

    private fun resolvePackageDirectory(
        packageName: String
    ): Path {
        val packageDirectory =
            rootDirectory
                .resolve(packageName)
                .normalize()

        require(
            packageDirectory.parent ==
                    rootDirectory
        ) {
            "Package name must be a single directory name: $packageName"
        }

        Files.createDirectories(
            packageDirectory
        )

        return packageDirectory
    }

    private fun toPortableRelativePath(
        absolutePath: Path
    ): String =
        rootDirectory
            .relativize(
                absolutePath
            )
            .joinToString("/") {
                it.toString()
            }
}
