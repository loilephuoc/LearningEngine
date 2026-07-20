package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class LearningItemFingerprintFactoryTest {

    private val factory =
        LearningItemFingerprintFactory()

    @Test
    fun `identifiers and enabled state do not affect learning item fingerprint`() {
        val firstContent =
            createContent(
                id =
                    "content-1"
            )

        val secondContent =
            createContent(
                id =
                    "content-2"
            )

        assertEquals(
            factory.create(
                learningItem =
                    createLearningItem(
                        id =
                            "item-1",
                        contentId =
                            firstContent.id,
                        isEnabled =
                            true
                    ),
                content =
                    firstContent
            ),
            factory.create(
                learningItem =
                    createLearningItem(
                        id =
                            "item-2",
                        contentId =
                            secondContent.id,
                        isEnabled =
                            false
                    ),
                content =
                    secondContent
            )
        )
    }

    @Test
    fun `learning mode affects fingerprint`() {
        val content =
            createContent(
                id =
                    "content-1"
            )

        assertNotEquals(
            factory.create(
                learningItem =
                    createLearningItem(
                        id =
                            "item-1",
                        contentId =
                            content.id,
                        mode =
                            LearningMode.MEANING_RECOGNITION
                    ),
                content =
                    content
            ),
            factory.create(
                learningItem =
                    createLearningItem(
                        id =
                            "item-2",
                        contentId =
                            content.id,
                        mode =
                            LearningMode.MEANING_RECALL
                    ),
                content =
                    content
            )
        )
    }

    @Test
    fun `content reference mismatch is rejected`() {
        val content =
            createContent(
                id =
                    "content-1"
            )

        assertFailsWith<IllegalArgumentException> {
            factory.create(
                learningItem =
                    createLearningItem(
                        id =
                            "item-1",
                        contentId =
                            ContentId(
                                "content-2"
                            )
                    ),
                content =
                    content
            )
        }
    }

    private fun createContent(
        id: String
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
                        "Hello"
                )
        )

    private fun createLearningItem(
        id: String,
        contentId: ContentId,
        mode: LearningMode =
            LearningMode.MEANING_RECOGNITION,
        isEnabled: Boolean =
            true
    ): LearningItem =
        LearningItem(
            id =
                LearningItemId(
                    id
                ),
            contentId =
                contentId,
            mode =
                mode,
            isEnabled =
                isEnabled
        )
}