package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.infrastructure.persistence.record.PackageRecord

class PackageRecordMapperTest {

    @Test
    fun `maps content package to record and back`() {
        val contentPackage = ContentPackage(
            id = PackageId("package-english"),
            descriptor = PackageDescriptor(
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD3"
            ),
            libraryIds = setOf(
                ContentLibraryId("library-vocabulary"),
                ContentLibraryId("library-conversations")
            )
        )

        val record =
            PackageRecordMapper.toRecord(contentPackage)

        val restored =
            PackageRecordMapper.toDomain(record)

        assertEquals(contentPackage, restored)
    }

    @Test
    fun `migrates package record without topic identity deterministically`() {
        val legacyRecord =
            PackageRecord(
                id = "legacy-package",
                name = "Legacy Medical Physics",
                version = "1",
                format = "OPD3"
            )

        val first =
            PackageRecordMapper.toDomain(
                legacyRecord
            )
        val restarted =
            PackageRecordMapper.toDomain(
                legacyRecord
            )

        assertEquals(
            TopicId.deriveForLegacyPackage(
                packageName = legacyRecord.name,
                packageFormat = legacyRecord.format
            ),
            first.topicId
        )
        assertEquals(first.topicId, restarted.topicId)
        assertEquals(
            first.topicId.value,
            PackageRecordMapper.toRecord(first).topicId
        )
    }

    @Test
    fun `persisted topic identity survives display metadata changes`() {
        val original =
            ContentPackage(
                id = PackageId("package-original"),
                descriptor =
                    PackageDescriptor(
                        name = "Original display name",
                        version = "1",
                        format = "OPD3"
                    )
            )
        val renamed =
            original.copy(
                descriptor =
                    original.descriptor.copy(
                        name = "Renamed display name"
                    )
            )

        assertEquals(
            original.topicId,
            PackageRecordMapper.toDomain(
                PackageRecordMapper.toRecord(
                    renamed
                )
            ).topicId
        )
    }
}
