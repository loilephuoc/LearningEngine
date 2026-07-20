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

    fun exists(
        relativePath: String
    ): Boolean
}