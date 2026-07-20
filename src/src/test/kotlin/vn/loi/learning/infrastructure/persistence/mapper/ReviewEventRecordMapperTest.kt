package vn.loi.learning.infrastructure.persistence.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord
import vn.loi.learning.testing.fixtures.ReviewFixtures

class ReviewEventRecordMapperTest {

    @Test
    fun `review event survives round trip with response time`() {
        val original =
            ReviewFixtures.event()

        val record =
            ReviewEventRecordMapper.toRecord(original)

        val restored =
            ReviewEventRecordMapper.toDomain(record)

        assertEquals(original, restored)
    }

    @Test
    fun `review event survives round trip without response time`() {
        val original =
            ReviewFixtures.event(
                responseTime = null
            )

        val record =
            ReviewEventRecordMapper.toRecord(original)

        val restored =
            ReviewEventRecordMapper.toDomain(record)

        assertEquals(original, restored)
        assertNull(restored.responseTime)
    }

    @Test
    fun `toRecord writes current schema version`() {
        val event =
            ReviewFixtures.event()

        val record =
            ReviewEventRecordMapper.toRecord(event)

        assertEquals(
            ReviewEventRecord.CURRENT_SCHEMA_VERSION,
            record.schemaVersion
        )
    }

    @Test
    fun `unsupported schema version throws IllegalArgumentException`() {
        val validRecord =
            ReviewEventRecordMapper.toRecord(
                ReviewFixtures.event()
            )

        val unsupportedRecord =
            validRecord.copy(
                schemaVersion =
                    ReviewEventRecord.CURRENT_SCHEMA_VERSION + 1
            )

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            ReviewEventRecordMapper.toDomain(
                unsupportedRecord
            )
        }
    }

    @Test
    fun `unknown review rating throws IllegalArgumentException`() {
        val validRecord =
            ReviewEventRecordMapper.toRecord(
                ReviewFixtures.event()
            )

        val invalidRecord =
            validRecord.copy(
                rating = "UNKNOWN_RATING"
            )

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            ReviewEventRecordMapper.toDomain(
                invalidRecord
            )
        }
    }
}