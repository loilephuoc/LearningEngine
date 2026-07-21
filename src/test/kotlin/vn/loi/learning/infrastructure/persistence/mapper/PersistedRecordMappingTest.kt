package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.infrastructure.persistence.record.ContentRecord
import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord

class PersistedRecordMappingTest {

    @Test
    fun `learning item failure includes record type id and cause`() {
        val failure =
            assertFailsWith<InvalidPersistedRecordException> {
                LearningItemRecordMapper.toDomain(
                    LearningItemRecord(
                        id = "item-broken-mode",
                        contentId = "content-1",
                        mode = "REMOVED_MODE",
                        isEnabled = true
                    )
                )
            }

        assertEquals("learning-item", failure.recordType)
        assertEquals("item-broken-mode", failure.recordId)
        assertTrue(failure.message.orEmpty().contains("REMOVED_MODE"))
        assertIs<IllegalArgumentException>(failure.cause)
    }

    @Test
    fun `content failure identifies incompatible persisted record`() {
        val failure =
            assertFailsWith<InvalidPersistedRecordException> {
                ContentRecordMapper.toDomain(
                    ContentRecord(
                        id = "content-broken-type",
                        type = "REMOVED_TYPE",
                        primaryText = "hello",
                        translatedText = null,
                        pronunciation = null,
                        exampleText = null,
                        exampleTranslation = null
                    )
                )
            }

        assertEquals("content", failure.recordType)
        assertEquals("content-broken-type", failure.recordId)
        assertTrue(failure.message.orEmpty().contains("REMOVED_TYPE"))
    }

    @Test
    fun `existing contextual failure is not double wrapped`() {
        val original =
            InvalidPersistedRecordException(
                recordType = "content",
                recordId = "content-1",
                cause = IllegalArgumentException("bad value")
            )

        val actual =
            assertFailsWith<InvalidPersistedRecordException> {
                mapPersistedRecord(
                    recordType = "outer",
                    recordId = "outer-1"
                ) {
                    throw original
                }
            }

        assertTrue(actual === original)
    }
}
