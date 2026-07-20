package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class PackageContentIntegrityValidatorTest {

    private val validator = PackageContentIntegrityValidator()

    @Test
    fun `unique content and learning item IDs produce valid report`() {
        val content = content("content-1")
        val learningItem = learningItem(
            id = "item-1",
            contentId = content.id
        )

        val report = validator.validate(
            ImportedPackageContent(
                contents = listOf(content),
                learningItems = listOf(learningItem)
            )
        )

        assertTrue(report.isValid)
        assertTrue(report.issues.isEmpty())
    }

    @Test
    fun `duplicate content ID produces error`() {
        val report = validator.validate(
            ImportedPackageContent(
                contents = listOf(
                    content("content-1"),
                    content("content-1")
                ),
                learningItems = emptyList()
            )
        )

        assertEquals(
            listOf("DUPLICATE_CONTENT_ID"),
            report.errors.map { issue -> issue.code }
        )
    }

    @Test
    fun `duplicate learning item ID produces error`() {
        val contentId = ContentId("content-1")

        val report = validator.validate(
            ImportedPackageContent(
                contents = listOf(content("content-1")),
                learningItems = listOf(
                    learningItem("item-1", contentId),
                    learningItem("item-1", contentId)
                )
            )
        )

        assertEquals(
            listOf("DUPLICATE_LEARNING_ITEM_ID"),
            report.errors.map { issue -> issue.code }
        )
    }

    @Test
    fun `all duplicate ID failures are accumulated`() {
        val contentId = ContentId("content-1")

        val report = validator.validate(
            ImportedPackageContent(
                contents = listOf(
                    content("content-1"),
                    content("content-1")
                ),
                learningItems = listOf(
                    learningItem("item-1", contentId),
                    learningItem("item-1", contentId)
                )
            )
        )

        assertEquals(
            listOf(
                "DUPLICATE_CONTENT_ID",
                "DUPLICATE_LEARNING_ITEM_ID"
            ),
            report.errors.map { issue -> issue.code }
        )
    }

    private fun content(
        id: String
    ): Content =
        Content(
            id = ContentId(id),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "hello"
            )
        )

    private fun learningItem(
        id: String,
        contentId: ContentId
    ): LearningItem =
        LearningItem(
            id = LearningItemId(id),
            contentId = contentId,
            mode = LearningMode.MEANING_RECOGNITION
        )
}
