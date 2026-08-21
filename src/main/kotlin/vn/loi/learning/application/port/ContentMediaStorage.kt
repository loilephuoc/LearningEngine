package vn.loi.learning.application.port

import java.nio.file.Path
import vn.loi.learning.application.contentmedia.ContentMediaAsset

interface ContentMediaStorage {

    fun store(
        packageName: String,
        fileName: String,
        content: ByteArray
    ): ContentMediaAsset

    fun resolve(
        relativePath: String
    ): Path?

    fun resolvePackageDirectory(
        packageName: String
    ): Path? = null

    fun exists(
        relativePath: String
    ): Boolean

    fun deletePackageNamespace(
        packageName: String
    ): Boolean = false

    fun storeStream(
        packageName: String,
        fileName: String,
        source: Path
    ): ContentMediaAsset =
        store(packageName, fileName, java.nio.file.Files.readAllBytes(source))
}