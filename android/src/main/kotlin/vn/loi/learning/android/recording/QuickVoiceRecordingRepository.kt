package vn.loi.learning.android.recording

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

/**
 * Thread-safe persistent repository managing voice recordings metadata and filesystem state.
 */
class QuickVoiceRecordingRepository(
    private val rootDirSupplier: () -> File
) {
    private val TAG = "QuickVoiceRepo"
    private val mutex = Mutex()

    private val _recordings = MutableStateFlow<List<VoiceRecordingItem>>(emptyList())
    val recordings: StateFlow<List<VoiceRecordingItem>> = _recordings.asStateFlow()

    private val _latestRecording = MutableStateFlow<VoiceRecordingItem?>(null)
    val latestRecording: StateFlow<VoiceRecordingItem?> = _latestRecording.asStateFlow()

    private val recordingsDir: File
        get() = File(rootDirSupplier(), "recordings/quick_voice").apply { mkdirs() }

    private val indexFile: File
        get() = File(recordingsDir, "recordings_index.json")

    suspend fun initialize() {
        mutex.withLock {
            reconcileInternal()
        }
    }

    suspend fun getAll(): List<VoiceRecordingItem> {
        return mutex.withLock {
            _recordings.value
        }
    }

    suspend fun getLatest(): VoiceRecordingItem? {
        return mutex.withLock {
            _latestRecording.value
        }
    }

    suspend fun save(item: VoiceRecordingItem) {
        mutex.withLock {
            val current = _recordings.value.filter { it.id != item.id }.toMutableList()
            current.add(0, item)
            current.sortByDescending { it.createdAt }
            _recordings.value = current
            _latestRecording.value = current.firstOrNull()
            writeIndexFile(current)
        }
    }

    suspend fun delete(id: String): Boolean {
        return mutex.withLock {
            val item = _recordings.value.firstOrNull { it.id == id } ?: return@withLock false
            runCatching { item.file.delete() }
            val current = _recordings.value.filter { it.id != id }
            _recordings.value = current
            _latestRecording.value = current.firstOrNull()
            writeIndexFile(current)
            true
        }
    }

    suspend fun reconcile() {
        mutex.withLock {
            reconcileInternal()
        }
    }

    @Volatile
    var isRecordingActive: Boolean = false

    /**
     * Copies only indexed, completed recordings while holding the repository mutation lock.
     * An active MediaRecorder output is not indexed until stop/save and is therefore excluded.
     */
    fun snapshotCompletedForBackup(targetFilesDirectory: Path): List<VoiceRecordingItem> = runBlocking {
        mutex.withLock {
            Files.createDirectories(targetFilesDirectory)
            _recordings.value.filter { it.exists }.map { item ->
                requireSafePortableFilename(item.filename)
                val source = item.file.toPath().toAbsolutePath().normalize()
                val recordingRoot = recordingsDir.toPath().toAbsolutePath().normalize()
                require(source.parent == recordingRoot) { "Recording path escapes the managed recording root." }
                Files.copy(source, targetFilesDirectory.resolve(item.filename))
                item
            }
        }
    }

    fun captureRecordingsState(): Pair<List<VoiceRecordingItem>, Map<String, ByteArray>> = runBlocking {
        mutex.withLock {
            val items = _recordings.value
            val files = if (recordingsDir.exists()) {
                recordingsDir.listFiles { file -> file.isFile && file.name != "recordings_index.json" }?.associate {
                    it.name to it.readBytes()
                }.orEmpty()
            } else emptyMap()
            items to files
        }
    }

    fun captureRecordingsStateToDirectory(rollbackDir: Path?): List<VoiceRecordingItem> = runBlocking {
        mutex.withLock {
            val items = _recordings.value
            if (rollbackDir != null && recordingsDir.exists()) {
                val dir = rollbackDir.resolve("recordings_rollback")
                Files.createDirectories(dir)
                recordingsDir.listFiles { file -> file.isFile && file.name != "recordings_index.json" }?.forEach { src ->
                    Files.copy(src.toPath(), dir.resolve(src.name), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
            }
            items
        }
    }

    fun restoreRecordingsFromBackup(recordingsStagingDirectory: Path) = runBlocking {
        mutex.withLock {
            if (isRecordingActive) throw IllegalStateException("Cannot restore recordings while recording is active.")
            val stagingFiles = recordingsStagingDirectory.resolve("files")
            val stagingIndex = recordingsStagingDirectory.resolve("index.json")
            if (Files.exists(recordingsDir.toPath())) {
                recordingsDir.listFiles()?.forEach { it.delete() }
            }
            recordingsDir.mkdirs()
            val restoredItems = mutableListOf<VoiceRecordingItem>()
            if (Files.isRegularFile(stagingIndex)) {
                val text = Files.newBufferedReader(stagingIndex, java.nio.charset.StandardCharsets.UTF_8).use { it.readText() }
                val parsed = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
                val recordingsArray = parsed?.get("recordings") as? kotlinx.serialization.json.JsonArray
                if (recordingsArray != null) {
                    for (elem in recordingsArray) {
                        val obj = elem as? kotlinx.serialization.json.JsonObject ?: continue
                        val id = obj["id"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: continue
                        val filename = obj["filename"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: continue
                        requireSafePortableFilename(filename)
                        val createdAt = obj["createdAt"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: 0L
                        val durationMs = obj["durationMs"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: 0L
                        val sizeBytes = obj["sizeBytes"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: 0L
                        val sourceFile = stagingFiles.resolve(filename)
                        val targetFile = File(recordingsDir, filename)
                        if (Files.isRegularFile(sourceFile)) {
                            Files.copy(sourceFile, targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                            restoredItems.add(
                                VoiceRecordingItem(
                                    id = id,
                                    filename = filename,
                                    filePath = targetFile.absolutePath,
                                    createdAt = createdAt,
                                    durationMs = durationMs,
                                    sizeBytes = sizeBytes
                                )
                            )
                        }
                    }
                }
            }
            restoredItems.sortByDescending { it.createdAt }
            _recordings.value = restoredItems
            _latestRecording.value = restoredItems.firstOrNull()
            writeIndexFile(restoredItems)
        }
    }

    fun rollbackRecordings(items: List<VoiceRecordingItem>, files: Map<String, ByteArray>) = runBlocking {
        mutex.withLock {
            if (Files.exists(recordingsDir.toPath())) {
                recordingsDir.listFiles()?.forEach { it.delete() }
            }
            recordingsDir.mkdirs()
            files.forEach { (name, bytes) ->
                File(recordingsDir, name).writeBytes(bytes)
            }
            _recordings.value = items
            _latestRecording.value = items.firstOrNull()
            writeIndexFile(items)
        }
    }

    fun rollbackRecordingsFromDirectory(items: List<VoiceRecordingItem>, rollbackDir: Path?) = runBlocking {
        mutex.withLock {
            if (Files.exists(recordingsDir.toPath())) {
                recordingsDir.listFiles()?.forEach { it.delete() }
            }
            recordingsDir.mkdirs()
            if (rollbackDir != null) {
                val dir = rollbackDir.resolve("recordings_rollback")
                if (Files.exists(dir)) {
                    dir.toFile().listFiles { f -> f.isFile }?.forEach { src ->
                        Files.copy(src.toPath(), File(recordingsDir, src.name).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
            _recordings.value = items
            _latestRecording.value = items.firstOrNull()
            writeIndexFile(items)
        }
    }


    private fun reconcileInternal() {
        val loaded = readIndexFile().filter { it.exists }.toMutableList()
        val indexedNames = loaded.map { it.filename }.toSet()

        // Discover any unindexed .m4a files in directory
        val dirFiles = recordingsDir.listFiles { file -> file.isFile && file.extension.equals("m4a", ignoreCase = true) } ?: emptyArray()
        for (file in dirFiles) {
            if (file.name !in indexedNames && file.length() > 0) {
                val timestamp = file.lastModified()
                val discovered = VoiceRecordingItem(
                    id = "rec_${file.nameWithoutExtension}_$timestamp",
                    filename = file.name,
                    filePath = file.absolutePath,
                    createdAt = timestamp,
                    durationMs = 0L, // approximate when discovered
                    sizeBytes = file.length()
                )
                loaded.add(discovered)
            }
        }

        loaded.sortByDescending { it.createdAt }
        _recordings.value = loaded
        _latestRecording.value = loaded.firstOrNull()
        writeIndexFile(loaded)
    }

    private val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun readIndexFile(): List<VoiceRecordingItem> {
        val file = indexFile
        if (!file.exists() || file.length() == 0L) return emptyList()
        return try {
            val jsonStr = file.readText()
            val array = jsonParser.parseToJsonElement(jsonStr)
            val jsonArray = array as? kotlinx.serialization.json.JsonArray ?: return emptyList()
            val list = mutableListOf<VoiceRecordingItem>()
            for (elem in jsonArray) {
                val obj = elem as? kotlinx.serialization.json.JsonObject ?: continue
                val id = obj["id"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: continue
                val filename = obj["filename"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: continue
                val filePath = obj["filePath"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: continue
                val createdAt = obj["createdAt"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: 0L
                val durationMs = obj["durationMs"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: 0L
                val sizeBytes = obj["sizeBytes"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: 0L

                list.add(
                    VoiceRecordingItem(
                        id = id,
                        filename = filename,
                        filePath = filePath,
                        createdAt = createdAt,
                        durationMs = durationMs,
                        sizeBytes = sizeBytes
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error reading recordings index: ${e.message}", e)
            emptyList()
        }
    }

    private fun writeIndexFile(items: List<VoiceRecordingItem>) {
        try {
            val jsonArray = kotlinx.serialization.json.buildJsonArray {
                for (item in items) {
                    add(
                        kotlinx.serialization.json.buildJsonObject {
                            put("id", item.id)
                            put("filename", item.filename)
                            put("filePath", item.filePath)
                            put("createdAt", item.createdAt)
                            put("durationMs", item.durationMs)
                            put("sizeBytes", item.sizeBytes)
                        }
                    )
                }
            }
            indexFile.writeText(jsonParser.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), jsonArray))
        } catch (e: Exception) {
            Log.e(TAG, "Error writing recordings index: ${e.message}", e)
        }
    }

    companion object {
        @Volatile
        private var instance: QuickVoiceRecordingRepository? = null

        fun getInstance(context: Context): QuickVoiceRecordingRepository {
            return instance ?: synchronized(this) {
                instance ?: QuickVoiceRecordingRepository { context.filesDir }.also {
                    instance = it
                }
            }
        }

        fun forTesting(rootDir: File): QuickVoiceRecordingRepository {
            return QuickVoiceRecordingRepository { rootDir }
        }
    }
}

private fun requireSafePortableFilename(filename: String) {
    require(filename.isNotBlank() && filename != "." && filename != "..")
    require('/' !in filename && '\\' !in filename && !Regex("^[A-Za-z]:.*").matches(filename))
}
