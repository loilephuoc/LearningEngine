package vn.loi.learning.desktop.notification

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import vn.loi.learning.domain.content.model.ContentId

fun interface DesktopVocabularyReminderMarkedReadSource {
    fun isMarked(contentId: ContentId): Boolean
}

interface DesktopVocabularyReminderDifficultMarkers : DesktopVocabularyReminderMarkedReadSource {
    fun markedContentIds(): Set<ContentId>
    fun toggle(contentId: ContentId): Boolean
}

class DesktopVocabularyReminderDifficultStore(private val filePath: Path) :
    DesktopVocabularyReminderDifficultMarkers {
    private val lock = Any()
    private var loaded: LinkedHashSet<ContentId>? = null

    override fun isMarked(contentId: ContentId) = synchronized(lock) { loadLocked().contains(contentId) }
    override fun markedContentIds(): Set<ContentId> = synchronized(lock) { loadLocked().toSet() }

    override fun toggle(contentId: ContentId): Boolean = synchronized(lock) {
        val next = LinkedHashSet(loadLocked())
        val marked = if (!next.add(contentId)) { next.remove(contentId); false } else true
        saveLocked(next)
        loaded = next
        marked
    }

    private fun loadLocked(): LinkedHashSet<ContentId> {
        loaded?.let { return it }
        val result = linkedSetOf<ContentId>()
        if (Files.isRegularFile(filePath)) {
            runCatching {
                Files.readAllLines(filePath, StandardCharsets.UTF_8).forEach { line ->
                    if (!line.startsWith(PREFIX)) return@forEach
                    val decoded = String(Base64.getUrlDecoder().decode(line.removePrefix(PREFIX)), StandardCharsets.UTF_8)
                    result += ContentId(decoded)
                }
            }.onFailure { result.clear() }
        }
        return result.also { loaded = it }
    }

    private fun saveLocked(ids: Set<ContentId>) {
        val target = filePath.toAbsolutePath().normalize()
        val parent = requireNotNull(target.parent)
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, "desktop-vocabulary-difficult.", ".tmp")
        try {
            val text = ids.map(ContentId::value).sorted().joinToString(separator = "\n", postfix = if (ids.isEmpty()) "" else "\n") {
                PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(StandardCharsets.UTF_8))
            }
            Files.writeString(temporary, text, StandardCharsets.UTF_8)
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { Files.deleteIfExists(temporary) }
    }

    companion object {
        const val FILE_NAME = "desktop-vocabulary-reminder-difficult.properties"
        private const val PREFIX = "content.id.base64="
    }
}
