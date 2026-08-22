package vn.loi.learning.android.platform

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import vn.loi.learning.android.recording.QuickVoiceRecordingRepository
import vn.loi.learning.android.recording.VoiceRecordingItem
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Descriptor
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult

class AndroidPortableBackupRoundTripTest {
    @Test
    fun `real Android supplement pipeline round trips preferences recordings and background`() {
        val root = Files.createTempDirectory("android-platform-roundtrip-")
        try {
            val files = root.resolve("files").toFile().apply(File::mkdirs)
            val cache = root.resolve("cache").toFile().apply(File::mkdirs)
            val context = TestContext(files, cache)
            val recordings = QuickVoiceRecordingRepository.forTesting(files)
            val recordingFile = files.toPath().resolve("recordings/quick_voice/original.m4a")
            Files.createDirectories(recordingFile.parent)
            Files.write(recordingFile, byteArrayOf(1, 2, 3))
            runBlocking { recordings.save(VoiceRecordingItem("r1", "original.m4a", recordingFile.toString(), 1, 2, 3)) }
            context.getSharedPreferences("learning-engine-study", Context.MODE_PRIVATE).edit().putInt("daily.new", 42).commit()
            Files.write(files.toPath().resolve("lockscreen_custom_bg.png"), byteArrayOf(9, 8, 7))

            val data = files.toPath().resolve("learning-engine/data")
            val media = data.resolve("media")
            Files.createDirectories(media)
            LearningApplicationFactory.createPersisted(data, false)
            val snapshot = AndroidPortableBackupSnapshot(context, recordings)
            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = files.toPath().resolve("learning-engine/backups"),
                stagedDomainValidator = { roots -> LearningApplicationFactory.validatePersisted(requireNotNull(roots["data"])) }
            )
            val archive = root.resolve("android-full.lebak")
            manager.createPortableBackupV2(
                archive,
                PortableBackupV2Descriptor("android-test", sourcePlatform = "android"),
                contributor = snapshot
            )
            manager.validatePortableBackupV2(archive)

            context.getSharedPreferences("learning-engine-study", Context.MODE_PRIVATE).edit().putInt("daily.new", 7).commit()
            Files.delete(recordingFile)
            Files.write(files.toPath().resolve("lockscreen_custom_bg.png"), byteArrayOf(0))

            val result = manager.restorePortableBackupV2(
                archive,
                contributorForSafetyBackup = snapshot,
                consumer = snapshot,
                selectedPackageIds = null
            )

            assertIs<PortableBackupV2RestoreResult.Success>(result)
            assertEquals(42, context.getSharedPreferences("learning-engine-study", 0).getInt("daily.new", -1))
            assertTrue(Files.exists(recordingFile))
            assertEquals(listOf("r1"), runBlocking { recordings.getAll() }.map { it.id })
            assertTrue(Files.readAllBytes(files.toPath().resolve("lockscreen_custom_bg.png")).contentEquals(byteArrayOf(9, 8, 7)))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

private class TestContext(
    private val files: File,
    private val cache: File
) : ContextWrapper(null) {
    private val stores = mutableMapOf<String, MemoryPreferences>()
    override fun getFilesDir(): File = files
    override fun getCacheDir(): File = cache
    override fun getApplicationContext(): Context = this
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = stores.getOrPut(name) { MemoryPreferences() }
}

private class MemoryPreferences : SharedPreferences {
    private val values = linkedMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor(values)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private class Editor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val updates = linkedMapOf<String, Any?>()
        private var clear = false
        override fun putString(key: String?, value: String?) = apply { updates[requireNotNull(key)] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { updates[requireNotNull(key)] = values?.toSet() }
        override fun putInt(key: String?, value: Int) = apply { updates[requireNotNull(key)] = value }
        override fun putLong(key: String?, value: Long) = apply { updates[requireNotNull(key)] = value }
        override fun putFloat(key: String?, value: Float) = apply { updates[requireNotNull(key)] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { updates[requireNotNull(key)] = value }
        override fun remove(key: String?) = apply { updates[requireNotNull(key)] = REMOVED }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean { applyChanges(); return true }
        override fun apply() = applyChanges()
        private fun applyChanges() {
            if (clear) values.clear()
            updates.forEach { (key, value) -> if (value === REMOVED) values.remove(key) else values[key] = value }
        }
        private companion object { val REMOVED = Any() }
    }
}
