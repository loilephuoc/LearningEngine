package vn.loi.learning.domain.content.search.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContentSearchQueryTest {

    @Test
    fun `normalized keyword trims surrounding whitespace`() {
        val query = ContentSearchQuery(
            keyword = "  hello world  "
        )

        assertEquals(
            "hello world",
            query.normalizedKeyword
        )
    }

    @Test
    fun `blank keyword is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ContentSearchQuery(
                keyword = "   "
            )
        }
    }

    @Test
    fun `normalized terms split keyword by whitespace`() {
        val query = ContentSearchQuery(
            keyword = "  hospital   english conversation  "
        )

        assertEquals(
            listOf(
                "hospital",
                "english",
                "conversation"
            ),
            query.normalizedTerms
        )
    }

    @Test
    fun `normalized phrase collapses repeated whitespace`() {
        val query = ContentSearchQuery(
            keyword = "  book   an    appointment  "
        )

        assertEquals(
            "book an appointment",
            query.normalizedPhrase
        )
    }
}

