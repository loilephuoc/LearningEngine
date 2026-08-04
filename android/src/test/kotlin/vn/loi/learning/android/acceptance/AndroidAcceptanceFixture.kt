package vn.loi.learning.android.acceptance

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.platform.AndroidContentOperations
import vn.loi.learning.android.platform.AndroidPlatformDirectories
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager

class AndroidAcceptanceFixture private constructor(
    private val root: java.nio.file.Path,
    val directories: AndroidPlatformDirectories,
    val graph: AndroidApplicationGraph
) : AutoCloseable {
    private var nextId = 0

    fun contentOperations(
        dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) = AndroidContentOperations(graph, dispatcher) { "acceptance-op-${++nextId}" }

    fun restartedGraph() = AndroidApplicationGraph(
        LearningApplicationFactory.createPersisted(directories.dataDirectory),
        JvmContentMediaStorage(directories.mediaDirectory),
        JvmLearningDataRecoveryManager(
            mapOf("data" to directories.dataDirectory, "media" to directories.mediaDirectory),
            directories.backupDirectory
        ),
        directories
    )

    override fun close() {
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    companion object {
        fun create(): AndroidAcceptanceFixture {
            val root = createTempDirectory("android-system-acceptance")
            val directories = AndroidPlatformDirectories(
                root.resolve("data"), root.resolve("data/media"), root.resolve("imports")
            ).also { it.create() }
            val graph = AndroidApplicationGraph(
                LearningApplicationFactory.createPersisted(directories.dataDirectory),
                JvmContentMediaStorage(directories.mediaDirectory),
                JvmLearningDataRecoveryManager(
                    mapOf("data" to directories.dataDirectory, "media" to directories.mediaDirectory),
                    directories.backupDirectory
                ),
                directories
            )
            return AndroidAcceptanceFixture(root, directories, graph)
        }
    }
}
