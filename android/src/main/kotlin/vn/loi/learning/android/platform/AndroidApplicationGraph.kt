package vn.loi.learning.android.platform

import android.content.Context
import java.nio.file.Path
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

/** Android composition root over the existing persisted engine and media/import authorities. */
class AndroidApplicationGraph private constructor(
    val engine: LearningApplicationContext,
    val media: ContentMediaStorage,
    val directories: AndroidPlatformDirectories
) {
    fun importPackages() =
        engine.packageImporter(directories.importDirectory)
            .importAllDetailed(PackageCatalogId("android-imports"))

    companion object {
        fun create(context: Context): AndroidApplicationGraph {
            val root = Path.of(context.filesDir.absolutePath, "learning-engine")
            val directories = AndroidPlatformDirectories(
                dataDirectory = root.resolve("data"),
                mediaDirectory = root.resolve("media"),
                importDirectory = root.resolve("imports")
            )
            directories.create()
            return AndroidApplicationGraph(
                engine = LearningApplicationFactory.createPersisted(directories.dataDirectory),
                media = JvmContentMediaStorage(directories.mediaDirectory),
                directories = directories
            )
        }
    }
}

data class AndroidPlatformDirectories(
    val dataDirectory: Path,
    val mediaDirectory: Path,
    val importDirectory: Path
) {
    fun create() {
        listOf(dataDirectory, mediaDirectory, importDirectory)
            .forEach(java.nio.file.Files::createDirectories)
    }
}
