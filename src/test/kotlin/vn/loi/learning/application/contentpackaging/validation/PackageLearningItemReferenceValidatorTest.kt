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

class PackageLearningItemReferenceValidatorTest {

    private val validator = PackageLearningItemReferenceValidator()

    @Test
    fun `learning item referencing existing content produces valid report`() {
        val content = content("content-1")

        val report = validator.validate(
            ImportedPackageContent(
                contents = listOf(content),
                learningItems = listOf(
                    learningItem(
                        id = "item-1",
                        contentId = content.id
                    )
                )
            )
        )

        assertTrue(report.isValid)
        assertTrue(report.issues.isEmpty())
    }

    @Test
    fun `learning item referencing missing content produces error`() {
        val report = validator.validate(
            ImportedPackageContent(
                contents = emptyList(),
                learningItems = listOf(
                    learningItem(
                        id = "item-1",
                        contentId = ContentId("missing-content")
                    )
                )
            )
        )

        assertEquals(
            listOf("MISSING_LEARNING_ITEM_CONTENT"),
            report.errors.map { issue -> issue.code }
        )
    }

    @Test
    fun `all missing content references are accumulated`() {
        val report = validator.validate(
            ImportedPackageContent(
                contents = listOf(content("content-1")),
                learningItems = listOf(
                    learningItem(
                        id = "item-1",
                        contentId = ContentId("missing-content-1")
                    ),
                    learningItem(
                        id = "item-2",
                        contentId = ContentId("content-1")
                    ),
                    learningItem(
                        id = "item-3",
                        contentId = ContentId("missing-content-2")
                    )
                )
            )
        )

        assertEquals(
            listOf(
                "MISSING_LEARNING_ITEM_CONTENT",
                "MISSING_LEARNING_ITEM_CONTENT"
            ),
            report.errors.map { issue -> issue.code }
        )

        assertEquals(
            listOf(
                "Learning item item-1 references missing content missing-content-1.",
                "Learning item item-3 references missing content missing-content-2."
            ),
            report.errors.map { issue -> issue.message }
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
