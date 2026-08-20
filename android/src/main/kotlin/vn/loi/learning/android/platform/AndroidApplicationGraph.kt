package vn.loi.learning.android.platform

import android.content.Context
import java.nio.file.Path
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Descriptor
import vn.loi.learning.android.BuildConfig

/** Android composition root over the existing persisted engine and media/import authorities. */
class AndroidApplicationGraph internal constructor(
    val engine: LearningApplicationContext,
    val media: ContentMediaStorage,
    val recovery: JvmLearningDataRecoveryManager,
    val directories: AndroidPlatformDirectories,
    private val portableBackupSnapshot: AndroidPortableBackupSnapshot? = null
) {
    fun importPackages() =
        engine.packageImporter(directories.importDirectory)
            .importAllDetailed(PackageCatalogId("android-imports"))

    fun createPortableBackup(target: Path): Path = recovery.createPortableBackupV2(
        target = target,
        descriptor = PortableBackupV2Descriptor(
            appVersion = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            sourcePlatform = "android",
            learnerIds = listOf("default-learner")
        ),
        contributor = portableBackupSnapshot
    )

    companion object {
        fun create(context: Context): AndroidApplicationGraph {
            return AndroidStartupTrace.measured("application_graph_create") {
            val root = Path.of(context.filesDir.absolutePath, "learning-engine")
            val directories = AndroidPlatformDirectories(
                dataDirectory = root.resolve("data"),
                // LearningApplicationFactory persists imported OPD3 media below its persistence root.
                mediaDirectory = root.resolve("data").resolve("media"),
                importDirectory = root.resolve("imports")
            )
            directories.create()
            val engine = AndroidStartupTrace.measured("learning_application_factory_create_persisted") {
                LearningApplicationFactory.createPersisted(
                    directories.dataDirectory,
                    reconcilePartOfSpeechRegistryOnCreate = false
                )
            }
            AndroidApplicationGraph(
                engine = engine,
                media = JvmContentMediaStorage(directories.mediaDirectory),
                recovery = JvmLearningDataRecoveryManager(
                    roots = mapOf("data" to directories.dataDirectory, "media" to directories.mediaDirectory),
                    safetyDirectory = directories.backupDirectory,
                    gate = requireNotNull(engine.recoveryOperationGate),
                    stagedDomainValidator = { roots ->
                        LearningApplicationFactory.validatePersisted(requireNotNull(roots["data"]))
                    }
                ),
                directories = directories,
                portableBackupSnapshot = AndroidPortableBackupSnapshot(context.applicationContext)
            )
            }
        }
    }
}

data class AndroidPlatformDirectories(
    val dataDirectory: Path,
    val mediaDirectory: Path,
    val importDirectory: Path
) {
    val rootDirectory: Path get() = dataDirectory.parent
    val backupDirectory: Path get() = rootDirectory.resolve("backups")
    fun create() {
        listOf(dataDirectory, mediaDirectory, importDirectory, backupDirectory)
            .forEach(java.nio.file.Files::createDirectories)
    }
}
