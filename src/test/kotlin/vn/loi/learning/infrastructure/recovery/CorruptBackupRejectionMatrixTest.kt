package vn.loi.learning.infrastructure.recovery

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationFactory

class CorruptBackupRejectionMatrixTest {
    @Test
    fun `all corrupt archive and domain cases reject before live mutation`() {
        val cases: List<Pair<String, Fixture.() -> Path>> = listOf(
            "invalid zip" to { invalidZip() },
            "missing manifest" to { rewrite("missing-manifest", regenerate = false) { it.remove(JvmLearningDataRecoveryManager.MANIFEST_ENTRY) } },
            "malformed manifest" to { rewrite("malformed-manifest", regenerate = false) { it[manifest] = "not-a-manifest".toByteArray() } },
            "duplicate manifest record" to { rewrite("duplicate-record", regenerate = false) { entries ->
                entries[manifest] = entries.getValue(manifest) + "file.0=data/contents.json\t1\tbad\n".toByteArray()
            } },
            "path traversal" to { rewrite("path-traversal") { it["../escape.json"] = "x".toByteArray() } },
            "absolute path" to { rewrite("absolute") { it["/absolute.json"] = "x".toByteArray() } },
            "unexpected root" to { rewrite("unexpected-root") { it["other/file.json"] = "x".toByteArray() } },
            "wrong count" to { rewrite("wrong-count", regenerate = false) { entries ->
                entries[manifest] = entries.getValue(manifest).toString(StandardCharsets.UTF_8)
                    .replace(Regex("files=\\d+"), "files=999").toByteArray()
            } },
            "wrong size" to { rewrite("wrong-size", regenerate = false) { entries ->
                entries[manifest] = entries.getValue(manifest).toString(StandardCharsets.UTF_8)
                    .replaceFirst(Regex("\\t\\d+\\t"), "\t999999\t").toByteArray()
            } },
            "wrong sha" to { rewrite("wrong-sha", regenerate = false) { entries ->
                entries[manifest] = entries.getValue(manifest).toString(StandardCharsets.UTF_8)
                    .replaceFirst(Regex("[0-9a-f]{64}"), "0".repeat(64)).toByteArray()
            } },
            "missing manifested file" to { rewrite("missing-file", regenerate = false) { it.remove("data/contents.json") } },
            "extra unmanifested file" to { rewrite("extra-file", regenerate = false) { it["data/extra.json"] = "{}".toByteArray() } },
            "zero byte required store" to { rewrite("zero-json") { it["data/contents.json"] = byteArrayOf() } },
            "zero byte media" to { rewrite("zero-media") { it["data/media/empty.bin"] = byteArrayOf() } },
            "truncated json" to { rewrite("truncated-json") { it["data/contents.json"] = "[".toByteArray() } },
            "malformed json" to { rewrite("malformed-json") { it["data/learning-items.json"] = "not-json".toByteArray() } },
            "learning item missing content" to { rewriteText("data/learning-items.json", "content-1", "missing-content") },
            "library missing content" to { rewriteText("data/content-libraries.json", "content-1", "missing-content") },
            "package missing library" to { rewriteText("data/content-packages.json", "library-1", "missing-library") },
            "duplicate repository id" to { rewrite("duplicate-content-id") { entries ->
                val document = Json.parseToJsonElement(entries.getValue("data/contents.json").toString(StandardCharsets.UTF_8)).jsonObject
                val records = document.getValue("records").jsonArray
                entries["data/contents.json"] = JsonObject(document + ("records" to JsonArray(records + records.first()))).toString().toByteArray()
            } },
            "future version" to { rewrite("future-version", regenerate = false) { entries ->
                entries[manifest] = entries.getValue(manifest).toString(StandardCharsets.UTF_8).replace("format=1", "format=999").toByteArray()
            } },
            "malformed media path" to { rewrite("malformed-media") { it["data/media\\bad.bin"] = byteArrayOf(1) } },
            "duplicate archive path" to { duplicateArchivePath() }
        )

        cases.forEach { (label, corruption) -> fixture().use { fixture ->
            val corrupt = fixture.corruption()
            fixture.makeLiveDistinct()
            val before = fixture.snapshot()

            assertFailsWith<LearningDataRecoveryException>(label) { fixture.manager.restore(corrupt, false) }

            assertSnapshot(before, fixture.snapshot(), label)
            assertEquals(0, Files.list(fixture.safety).use { it.count() }, label)
        } }
    }

    private fun fixture() = Fixture(Files.createTempDirectory("corrupt-recovery-"))

    private class Fixture(val root: Path) : AutoCloseable {
        val data = Files.createDirectories(root.resolve("data"))
        val config = Files.createDirectories(root.resolve("config"))
        val safety = Files.createDirectories(config.resolve("backups"))
        val manifest = JvmLearningDataRecoveryManager.MANIFEST_ENTRY
        val manager = JvmLearningDataRecoveryManager(
            mapOf("config" to config, "data" to data), safety,
            stagedDomainValidator = { LearningApplicationFactory.validatePersisted(it.getValue("data")) }
        )
        private val valid = root.resolve("valid.lebak")

        init {
            val context = LearningApplicationFactory.createPersisted(data, false)
            val content = ContentId("content-1")
            val library = ContentLibraryId("library-1")
            context.contentRepository!!.save(Content(content, ContentType.WORD, ContentText("safe-content")))
            context.learningItemRepository!!.save(LearningItem(LearningItemId("item-1"), content, LearningMode.MEANING_RECALL))
            context.contentLibraryRepository!!.save(ContentLibrary(library, LibraryDescriptor("Library"), setOf(content)))
            context.contentPackageRepository!!.save(ContentPackage(PackageId("package-1"), PackageDescriptor("Package", "1", "OPD3"), setOf(library)))
            Files.writeString(config.resolve("settings.properties"), "safe-setting")
            Files.write(data.resolve("media-a.bin"), byteArrayOf(1, 2, 3))
            Files.write(data.resolve("media-b.bin"), byteArrayOf(4, 5, 6))
            manager.createBackup(valid)
        }

        fun invalidZip(): Path = root.resolve("invalid.lebak").also { Files.writeString(it, "not zip") }

        fun rewriteText(path: String, old: String, new: String): Path = rewrite("domain-${new}-${path.substringAfterLast('/')}") {
            it[path] = it.getValue(path).toString(StandardCharsets.UTF_8).replace(old, new).toByteArray()
        }

        fun rewrite(name: String, regenerate: Boolean = true, mutation: (LinkedHashMap<String, ByteArray>) -> Unit): Path {
            val entries = readEntries(valid)
            mutation(entries)
            if (regenerate) entries[manifest] = encodeManifest(entries.filterKeys { it != manifest })
            return root.resolve("$name.lebak").also { writeEntries(it, entries) }
        }

        fun duplicateArchivePath(): Path {
            val duplicateSource = rewrite("duplicate-source") { }
            val bytes = Files.readAllBytes(duplicateSource)
            val first = "data/media-a.bin".toByteArray()
            val second = "data/media-b.bin".toByteArray()
            check(first.size == second.size)
            var replacements = 0
            for (index in 0..bytes.size - second.size) {
                if (second.indices.all { bytes[index + it] == second[it] }) {
                    first.copyInto(bytes, index)
                    replacements++
                }
            }
            check(replacements >= 2)
            return root.resolve("duplicate-path.lebak").also { Files.write(it, bytes) }
        }

        fun makeLiveDistinct() {
            Files.writeString(config.resolve("settings.properties"), "live-setting")
            Files.writeString(data.resolve("live-only.txt"), "live-secret")
        }

        fun snapshot(): Map<String, ByteArray> = listOf("config" to config, "data" to data).flatMap { (prefix, base) ->
            Files.walk(base).use { paths -> paths.filter(Files::isRegularFile).filter { !it.startsWith(safety) }
                .map { "$prefix/${base.relativize(it).toString().replace('\\', '/')}" to Files.readAllBytes(it) }.toList() }
        }.sortedBy { it.first }.toMap()

        override fun close() { root.toFile().deleteRecursively() }
    }

    companion object {
        private fun readEntries(source: Path): LinkedHashMap<String, ByteArray> = linkedMapOf<String, ByteArray>().also { result ->
            ZipFile(source.toFile()).use { zip -> zip.entries().asSequence().forEach { entry ->
                result[entry.name] = zip.getInputStream(entry).readAllBytes()
            } }
        }

        private fun writeEntries(target: Path, entries: Map<String, ByteArray>) {
            ZipOutputStream(Files.newOutputStream(target)).use { zip -> entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name).apply { time = 0L }); zip.write(bytes); zip.closeEntry()
            } }
        }

        private fun encodeManifest(payload: Map<String, ByteArray>): ByteArray = buildString {
            appendLine("format=1")
            appendLine("created=2026-08-15T00:00:00Z")
            appendLine("files=${payload.size}")
            payload.entries.sortedBy { it.key }.forEachIndexed { index, (name, bytes) ->
                appendLine("file.$index=$name\t${bytes.size}\t${sha256(bytes)}")
            }
        }.toByteArray()

        private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        private fun assertSnapshot(expected: Map<String, ByteArray>, actual: Map<String, ByteArray>, label: String) {
            assertEquals(expected.keys, actual.keys, label)
            expected.forEach { (name, bytes) -> assertContentEquals(bytes, actual.getValue(name), "$label:$name") }
        }
    }
}
