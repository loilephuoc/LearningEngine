package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord
import vn.loi.learning.infrastructure.persistence.store.ContentLibraryStore

class JsonContentLibraryStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : ContentLibraryStore {

    override fun loadAll(): List<ContentLibraryRecord> =
        JsonFileReader.read(
            filePath =
                filePath,
            emptyValue =
                emptyList()
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<ContentLibraryRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<ContentLibraryRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(
        records: List<ContentLibraryRecord>
    ) {
        val content =
            JsonPersistenceCodec.encode(
                records =
                    records
            ) { envelope ->
                json.encodeToString(
                    envelope
                )
            }

        JsonFileWriter.write(
            filePath =
                filePath,
            content =
                content
        )
    }

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