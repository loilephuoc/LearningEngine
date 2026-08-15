package vn.loi.learning.adapter.jvm

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Comparator
import kotlinx.serialization.json.*
import vn.loi.learning.application.integrity.RecoverLegacyOpd2Package
import vn.loi.learning.application.port.Opd2RecoveryMediaPort
import vn.loi.learning.application.port.PreparedOpd2RecoveryMedia

class JvmOpd2RecoveryMediaPort(
    private val sourceJson: Path,
    private val imageSource: Path,
    private val audioSource: Path,
    private val mediaRoot: Path,
    private val failureHook: (phase: String, index: Int?) -> Unit = { _, _ -> }
) : Opd2RecoveryMediaPort {
    override fun prepare(expectedCanonicalReferences: Set<String>): PreparedOpd2RecoveryMedia {
        failureHook("source-mapping", null)
        require(sha256(sourceJson) == SOURCE_SHA256) { "OPD2 source JSON checksum changed." }
        val rows = Json.parseToJsonElement(String(Files.readAllBytes(sourceJson), StandardCharsets.UTF_8)).jsonArray
        require(rows.size == RecoverLegacyOpd2Package.EXPECTED_CONTENTS) { "OPD2 source row count changed." }
        val sourceReferences = rows.flatMap { element ->
            val row = element.jsonObject
            SOURCE_SLOTS.mapNotNull { slot -> row[slot]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
        }
        val indexes = mapOf(
            "image" to indexFiles(imageSource),
            "audio" to indexFiles(audioSource)
        )
        val missing = linkedSetOf<String>()
        val candidatesByReference = linkedMapOf<String, Path>()
        var recoverableReferences = 0
        sourceReferences.forEach { reference ->
            val fileName = Path.of(reference.replace('\\', '/')).fileName.toString()
            val index = if (fileName.endsWith(".mp3", ignoreCase = true)) indexes.getValue("audio") else indexes.getValue("image")
            val candidates = index[fileName.lowercase()].orEmpty()
            when {
                candidates.isEmpty() -> missing += fileName
                candidates.size > 1 -> error("Ambiguous OPD2 media source: $fileName")
                else -> {
                    recoverableReferences++
                    candidatesByReference.putIfAbsent("$PACKAGE_ROOT/$fileName", candidates.single())
                }
            }
        }
        require(missing == RecoverLegacyOpd2Package.EXPECTED_MISSING_MEDIA) { "OPD2 missing-media evidence changed." }
        require(recoverableReferences == RecoverLegacyOpd2Package.EXPECTED_RECOVERABLE_MEDIA_REFERENCES)
        require(candidatesByReference.keys == expectedCanonicalReferences) { "Persisted OPD2 media references disagree with source evidence." }
        require(candidatesByReference.size == RecoverLegacyOpd2Package.EXPECTED_PUBLISHED_MEDIA_FILES)

        val staging = Files.createTempDirectory("opd2-recovery-")
        try {
            candidatesByReference.entries.forEachIndexed { index, (reference, source) ->
                failureHook("staging", index)
                val target = safeResolve(staging, reference)
                Files.createDirectories(target.parent)
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES)
                failureHook("media-hash", index)
                require(sha256(source) == sha256(target)) { "Staged OPD2 media hash mismatch: $reference" }
            }
            return Prepared(staging, candidatesByReference.keys.toList(), recoverableReferences, missing)
        } catch (failure: Throwable) {
            deleteTree(staging)
            throw failure
        }
    }

    private inner class Prepared(
        private val staging: Path,
        private val references: List<String>,
        override val recoverableReferenceCount: Int,
        override val missingReferences: Set<String>
    ) : PreparedOpd2RecoveryMedia {
        private val published = mutableListOf<Path>()
        private var completed = false
        override val publishedFileCount: Int get() = references.size

        override fun publish() {
            references.forEachIndexed { index, reference ->
                failureHook("media-publish", index)
                val source = safeResolve(staging, reference)
                val target = safeResolve(mediaRoot, reference)
                require(!Files.exists(target)) { "OPD2 media destination already exists: $reference" }
                Files.createDirectories(target.parent)
                Files.copy(source, target)
                published.add(target)
            }
        }

        override fun verifyPublished() {
            references.forEachIndexed { index, reference ->
                failureHook("published-hash", index)
                require(sha256(safeResolve(staging, reference)) == sha256(safeResolve(mediaRoot, reference))) {
                    "Published OPD2 media hash mismatch: $reference"
                }
            }
        }

        override fun rollbackPublished() {
            published.asReversed().forEach(Files::deleteIfExists)
            val packageRoot = mediaRoot.resolve(PACKAGE_ROOT)
            if (Files.isDirectory(packageRoot) && Files.list(packageRoot).use { !it.findAny().isPresent }) {
                Files.deleteIfExists(packageRoot)
            }
            published.clear()
        }

        override fun complete() { completed = true }

        override fun close() {
            if (!completed && published.isNotEmpty()) rollbackPublished()
            deleteTree(staging)
        }
    }

    private fun indexFiles(root: Path): Map<String, List<Path>> {
        require(Files.isDirectory(root)) { "OPD2 media source directory is missing: $root" }
        return Files.walk(root).use { paths ->
            paths.filter(Files::isRegularFile).toList().groupBy { it.fileName.toString().lowercase() }
        }
    }

    private fun safeResolve(root: Path, relative: String): Path {
        val normalizedRoot = root.toAbsolutePath().normalize()
        val resolved = normalizedRoot.resolve(relative.replace('/', java.io.File.separatorChar)).normalize()
        require(resolved.startsWith(normalizedRoot)) { "OPD2 media path escapes its root." }
        return resolved
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02X".format(it) }
    }

    private fun deleteTree(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }

    private companion object {
        const val SOURCE_SHA256 = "0F6A8F7770E83C817BABFA2F9567680304A60BD776EE9D1CC7DF6131BB4F1FC3"
        const val PACKAGE_ROOT = "OPD_2nd"
        val SOURCE_SLOTS = listOf("image", "audio", "audio_vi", "example_audio", "example_audio_vi")
    }
}
