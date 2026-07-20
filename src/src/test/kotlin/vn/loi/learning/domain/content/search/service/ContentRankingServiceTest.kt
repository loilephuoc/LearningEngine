package vn.loi.learning.domain.content.search.service

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.search.model.ContentSearchQuery

class ContentRankingServiceTest {

    private val rankingService = ContentRankingService()

    @Test
    fun `primary text match receives highest score`() {
        val content = Content(
            id = ContentId("content-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "hello world"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hello"
            )
        )

        assertEquals(100, score)
    }

    @Test
    fun `translated text match receives second score`() {
        val content = Content(
            id = ContentId("content-2"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment",
                translatedText = "cuộc gặp và hẹn khám"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "cuộc hẹn"
            )
        )

        assertEquals(90, score)
    }

    @Test
    fun `group match receives third score`() {
        val content = Content(
            id = ContentId("content-3"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            metadata = ContentMetadata(
                group = "Hospital English"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital"
            )
        )

        assertEquals(80, score)
    }

    @Test
    fun `section match receives fourth score`() {
        val content = Content(
            id = ContentId("content-4"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            metadata = ContentMetadata(
                section = "Medical Conversation"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "medical"
            )
        )

        assertEquals(70, score)
    }

    @Test
    fun `lesson match receives fifth score`() {
        val content = Content(
            id = ContentId("content-5"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            metadata = ContentMetadata(
                lesson = "Hospital Visit"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital"
            )
        )

        assertEquals(60, score)
    }

    @Test
    fun `custom field match receives sixth score`() {
        val content = Content(
            id = ContentId("content-6"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            customFields = ContentCustomFields(
                fields = setOf(
                    ContentCustomField(
                        id = ContentFieldId("topic"),
                        value = "Hospital Vocabulary"
                    )
                )
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital"
            )
        )

        assertEquals(50, score)
    }

    @Test
    fun `non matching content receives zero score`() {
        val content = Content(
            id = ContentId("content-7"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment",
                translatedText = "cuộc hẹn"
            ),
            metadata = ContentMetadata(
                group = "Medical English",
                section = "Clinic Conversation",
                lesson = "Booking a Visit"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "airport"
            )
        )

        assertEquals(0, score)
    }

    @Test
    fun `primary text still wins for multi term query`() {
        val content = Content(
            id = ContentId("content-multi-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "hospital booking and appointment"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital appointment"
            )
        )

        assertEquals(100, score)
    }

    @Test
    fun `primary text phrase match receives bonus`() {
        val content = Content(
            id = ContentId("content-phrase-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "please book an appointment today"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "book an appointment"
            )
        )

        assertEquals(105, score)
    }

    @Test
    fun `translated text phrase match receives bonus`() {
        val content = Content(
            id = ContentId("content-phrase-2"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "make a reservation",
                translatedText = "đặt một cuộc hẹn tại bệnh viện"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "cuộc hẹn"
            )
        )

        assertEquals(95, score)
    }

    @Test
    fun `group phrase match receives bonus`() {
        val content = Content(
            id = ContentId("content-phrase-group"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            metadata = ContentMetadata(
                group = "Advanced Hospital English Course"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital english"
            )
        )

        assertEquals(85, score)
    }

    @Test
    fun `section phrase match receives bonus`() {
        val content = Content(
            id = ContentId("content-phrase-section"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            metadata = ContentMetadata(
                section = "Everyday Medical Conversation Practice"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "medical conversation"
            )
        )

        assertEquals(75, score)
    }

    @Test
    fun `lesson phrase match receives bonus`() {
        val content = Content(
            id = ContentId("content-phrase-lesson"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            metadata = ContentMetadata(
                lesson = "Booking a Hospital Visit Today"
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital visit"
            )
        )

        assertEquals(65, score)
    }

    @Test
    fun `custom field phrase match receives bonus`() {
        val content = Content(
            id = ContentId("content-phrase-custom-field"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "appointment"
            ),
            customFields = ContentCustomFields(
                fields = setOf(
                    ContentCustomField(
                        id = ContentFieldId("topic"),
                        value = "Advanced Hospital Vocabulary Course"
                    )
                )
            )
        )

        val score = rankingService.score(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital vocabulary"
            )
        )

        assertEquals(55, score)
    }

}




