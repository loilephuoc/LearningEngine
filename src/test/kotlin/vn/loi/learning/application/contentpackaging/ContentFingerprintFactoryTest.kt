package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class ContentFingerprintFactoryTest {

    private val factory =
        ContentFingerprintFactory()

    @Test
    fun `identifier does not affect content fingerprint`() {
        assertEquals(
            factory.create(
                createContent(
                    id =
                        "content-1"
                )
            ),
            factory.create(
                createContent(
                    id =
                        "content-2"
                )
            )
        )
    }

    @Test
    fun `semantic content change affects fingerprint`() {
        assertNotEquals(
            factory.create(
                createContent(
                    id =
                        "content-1"
                )
            ),
            factory.create(
                createContent(
                    id =
                        "content-2",
                    primaryText =
                        "Goodbye"
                )
            )
        )
    }

    @Test
    fun `set ordering does not affect fingerprint`() {
        val first =
            createContent(
                id =
                    "content-1",
                tags =
                    linkedSetOf(
                        "common",
                        "greeting"
                    ),
                customFields =
                    linkedSetOf(
                        ContentCustomField(
                            id =
                                ContentFieldId(
                                    "level"
                                ),
                            value =
                                "A1"
                        ),
                        ContentCustomField(
                            id =
                                ContentFieldId(
                                    "topic"
                                ),
                            value =
                                "Greeting"
                        )
                    )
            )

        val second =
            createContent(
                id =
                    "content-2",
                tags =
                    linkedSetOf(
                        "greeting",
                        "common"
                    ),
                customFields =
                    linkedSetOf(
                        ContentCustomField(
                            id =
                                ContentFieldId(
                                    "topic"
                                ),
                            value =
                                "Greeting"
                        ),
                        ContentCustomField(
                            id =
                                ContentFieldId(
                                    "level"
                                ),
                            value =
                                "A1"
                        )
                    )
            )

        assertEquals(
            factory.create(
                first
            ),
            factory.create(
                second
            )
        )
    }

    private fun createContent(
        id: String,
        primaryText: String = "Hello",
        tags: Set<String> = emptySet(),
        customFields: Set<ContentCustomField> = emptySet()
    ): Content =
        Content(
            id =
                ContentId(
                    id
                ),
            type =
                ContentType.WORD,
            text =
                ContentText(
                    primaryText =
                        primaryText,
                    translatedText =
                        "Xin chào"
                ),
            metadata =
                ContentMetadata(
                    tags =
                        tags
                ),
            customFields =
                ContentCustomFields(
                    fields =
                        customFields
                )
        )
}