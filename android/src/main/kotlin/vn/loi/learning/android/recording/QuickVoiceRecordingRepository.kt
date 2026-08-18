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
