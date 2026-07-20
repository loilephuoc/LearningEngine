package vn.loi.learning.domain.content.search.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.search.model.ContentSearchQuery

class ContentMatcherTest {

    private val matcher = ContentMatcher()

    @Test
    fun `all search terms may match across searchable fields`() {
        val content = Content(
            id = ContentId("content-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "book an appointment"
            ),
            metadata = ContentMetadata(
                group = "Hospital English"
            )
        )

        val result = matcher.matches(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital appointment"
            )
        )

        assertTrue(result)
    }

    @Test
    fun `content does not match when one search term is missing`() {
        val content = Content(
            id = ContentId("content-2"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "book an appointment"
            ),
            metadata = ContentMetadata(
                group = "Hospital English"
            )
        )

        val result = matcher.matches(
            content = content,
            query = ContentSearchQuery(
                keyword = "hospital airport"
            )
        )

        assertFalse(result)
    }
}
