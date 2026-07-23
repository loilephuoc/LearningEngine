package vn.loi.learning.domain.content.topic.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import vn.loi.learning.domain.content.model.ContentId

class TopicIdTest {

    @Test
    fun `legacy package derivation ignores release version path and ordering`() {
        val first =
            TopicId.deriveForLegacyPackage(
                packageName = "Medical Physics",
                packageFormat = "opd3"
            )
        val restored =
            TopicId.deriveForLegacyPackage(
                packageName = "Medical Physics",
                packageFormat = "OPD3"
            )

        assertEquals(first, restored)
    }

    @Test
    fun `unpackaged content receives deterministic isolated topic identity`() {
        val first =
            TopicId.deriveForUnpackagedContent(
                ContentId("content-a")
            )

        assertEquals(
            first,
            TopicId.deriveForUnpackagedContent(
                ContentId("content-a")
            )
        )
        assertNotEquals(
            first,
            TopicId.deriveForUnpackagedContent(
                ContentId("content-b")
            )
        )
    }
}
