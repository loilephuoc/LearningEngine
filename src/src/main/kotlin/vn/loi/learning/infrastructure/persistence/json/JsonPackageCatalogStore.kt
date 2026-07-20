package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord
import vn.loi.learning.infrastructure.persistence.store.PackageCatalogStore

class JsonPackageCatalogStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
 ) : PackageCatalogStore {

    override fun loadAll(): List<PackageCatalogRecord> {
        if (Files.notExists(filePath)) return emptyList()

        val content = Files.readString(filePath)
        if (content.isBlank()) return emptyList()

        return json.decodeFromString(content)
    }

    override fun saveAll(records: List<PackageCatalogRecord>) {
        filePath.parent?.let(Files::createDirectories)

        val temporaryFile = filePath.resolveSibling(filePath.fileName.toString() + ".tmp")

        Files.writeString(
            temporaryFile,
            json.encodeToString(records)
        )

        try {
            Files.move(
                temporaryFile,
                filePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(
                temporaryFile,
                filePath,
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    companion object {
        private fun defaultJson(): Json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}


