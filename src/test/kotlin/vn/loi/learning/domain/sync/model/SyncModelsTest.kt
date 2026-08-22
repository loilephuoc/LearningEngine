package vn.loi.learning.domain.sync.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncModelsTest {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = false }

    @Test
    fun `SyncPackageManifest roundtrips JSON cleanly`() {
        val manifest = SyncPackageManifest(
            formatVersion = 1,
            syncPackageId = "sync-12345",
            createdAtUtc = "2026-08-22T00:00:00Z",
            sourcePlatform = "desktop",
            sourceDeviceId = "pc-office",
            packageIds = listOf("pkg-vocab-1"),
            contentDeltasCount = 3,
            reviewEventsCount = 10,
            mediaAssetsCount = 2,
            totalExpandedBytes = 4096,
            entries = listOf(
                SyncPackageEntry(
                    logicalPath = "content/deltas.json",
                    entryType = "content-deltas",
                    uncompressedSize = 1024,
                    sha256 = "abcd1234"
                )
            )
        )

        val encoded = json.encodeToString(manifest)
        val decoded = json.decodeFromString<SyncPackageManifest>(encoded)

        assertEquals(manifest, decoded)
        assertEquals("desktop", decoded.sourcePlatform)
        assertEquals(1, decoded.entries.size)
    }

    @Test
    fun `ContentDeltaRecord encodes and decodes all field variations`() {
        val delta = ContentDeltaRecord(
            contentId = "c-100",
            operation = ContentDeltaOperation.UPSERT,
            packageId = "pkg-1",
            type = "WORD",
            primaryText = "resilient",
            translatedText = "kiên cường",
            pronunciation = "/rɪˈzɪl.jənt/",
            exampleText = "He is resilient in difficulties.",
            exampleTranslation = "Anh ấy kiên cường trong khó khăn.",
            primaryAudio = "pkg-1/audio/resilient.mp3",
            translatedAudio = null,
            image = "pkg-1/images/resilient.jpg",
            customFields = mapOf("pos" to "ADJECTIVE", "difficulty" to "B2"),
            tags = setOf("oxford3000", "adjective"),
            updatedAtEpochMillis = 1755820800000L
        )

        val encoded = json.encodeToString(delta)
        val decoded = json.decodeFromString<ContentDeltaRecord>(encoded)

        assertEquals(delta, decoded)
        assertEquals("kiên cường", decoded.translatedText)
        assertEquals(ContentDeltaOperation.UPSERT, decoded.operation)
    }

    @Test
    fun `SyncResultSummary serializes conflicts and metrics accurately`() {
        val conflict = SyncConflict(
            conflictType = SyncConflictType.CONTENT_FIELD_COLLISION,
            entityId = "c-100",
            fieldName = "exampleText",
            localValueSummary = "Local example",
            incomingValueSummary = "Remote example",
            resolutionApplied = ConflictResolutionStrategy.MERGE_FIELD_LEVEL,
            detail = "Applied latest remote example while retaining local audio"
        )

        val summary = SyncResultSummary(
            syncPackageId = "sync-999",
            sourcePlatform = "android",
            contentDeltasApplied = 5,
            mediaAssetsAdded = 2,
            reviewEventsMerged = 50,
            reviewEventsDeduplicated = 4,
            conflicts = listOf(conflict),
            success = true,
            message = "Sync applied with 1 conflict resolved."
        )

        val encoded = json.encodeToString(summary)
        val decoded = json.decodeFromString<SyncResultSummary>(encoded)

        assertEquals(summary, decoded)
        assertEquals(1, decoded.conflicts.size)
        assertEquals(ConflictResolutionStrategy.MERGE_FIELD_LEVEL, decoded.conflicts[0].resolutionApplied)
    }
}
