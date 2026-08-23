package vn.loi.learning.desktop.sync

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.Properties
import vn.loi.learning.infrastructure.sync.supabase.SupabaseConfiguration

data class DesktopSupabaseConnection(val projectUrl: String, val publishableKey: String) {
    fun validated() = SupabaseConfiguration(projectUrl, publishableKey)
    fun maskedKey(): String = if (publishableKey.length <= 8) "••••••••" else publishableKey.take(4) + "••••" + publishableKey.takeLast(4)
}

class DesktopSupabaseConnectionStore(private val file: Path) {
    fun load(): DesktopSupabaseConnection? {
        if (!Files.isRegularFile(file)) return null
        val properties = Properties().apply { Files.newBufferedReader(file, StandardCharsets.UTF_8).use(::load) }
        val connection = DesktopSupabaseConnection(
            properties.getProperty("project.url")?.trim().orEmpty(),
            properties.getProperty("publishable.key")?.trim().orEmpty()
        )
        connection.validated()
        return connection
    }

    fun save(connection: DesktopSupabaseConnection) {
        val normalized = connection.validated()
        val target = file.toAbsolutePath().normalize()
        Files.createDirectories(requireNotNull(target.parent))
        val temporary = Files.createTempFile(target.parent, "supabase-sync.", ".tmp")
        try {
            Files.writeString(temporary, "schema.version=1\nproject.url=${normalized.baseUri}\npublishable.key=${connection.publishableKey}\n", StandardCharsets.UTF_8)
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temporary) }
    }

    companion object { const val FILE_NAME = "supabase-sync.properties" }
}
