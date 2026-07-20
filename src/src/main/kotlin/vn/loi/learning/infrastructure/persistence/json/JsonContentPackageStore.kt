package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.PackageRecord
import vn.loi.learning.infrastructure.persistence.store.ContentPackageStore

class JsonContentPackageStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : ContentPackageStore {

    override fun loadAll(): List<PackageRecord> {
        if (Files.notExists(filePath)) {
            return emptyList()
        }

        val content =
            Files.readString(filePath)

        if (content.isBlank()) {
            return emptyList()
        }

        return json.decodeFromString(content)
    }

    override fun saveAll(
        records: List<PackageRecord>
    ) {
        filePath.parent?.let(
            Files::createDirectories
        )

        val temporaryFile =
            temporaryFilePath()

        val content =
            json.encodeToString(records)

        Files.writeString(
            temporaryFile,
            content
        )

        try {
            Files.move(
                temporaryFile,
                filePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (
            _: java.nio.file.AtomicMoveNotSupportedException
        ) {
            Files.move(
                temporaryFile,
                filePath,
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun temporaryFilePath(): Path =
        filePath.resolveSibling(
            "${filePath.fileName}.tmp"
        )

    companion object {

        private fun defaultJson(): Json =
            Json {
                prettyPrint = true
                prettyPrintIndent = "  "
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}
