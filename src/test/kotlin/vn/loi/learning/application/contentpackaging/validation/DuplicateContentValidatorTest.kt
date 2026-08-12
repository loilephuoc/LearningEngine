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

class DuplicateContentValidatorTest {

    private val validator =
        DuplicateContentValidator()

    @Test
    fun `unique package content is valid`() {
        val firstContent =
            createContent(
                id =
                    "content-1",
                primaryText =
                    "Hello"
            )

        val secondContent =
            createContent(
                id =
                    "content-2",
                primaryText =
                    "Goodbye"
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            firstContent,
                            secondContent
                        ),
                    learningItems =
                        listOf(
                            createLearningItem(
                                id =
                                    "item-1",
                                contentId =
                                    firstContent.id
                            ),
                            createLearningItem(
                                id =
                                    "item-2",
                                contentId =
                                    secondContent.id
                            )
                        )
                )
            )

        assertTrue(
            report.isValid
        )
    }

    @Test
    fun `same semantic content with different identifiers is rejected`() {
        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            createContent(
                                id =
                                    "content-1",
                                primaryText =
                                    "Hello"
                            ),
                            createContent(
                                id =
                                    "content-2",
                                primaryText =
                                    "Hello"
                            )
                        ),
                    learningItems =
                        emptyList()
                )
            )

        assertTrue(report.isValid)
        assertEquals(
            listOf(
                "DUPLICATE_CONTENT_FINGERPRINT"
            ),
            report.warnings.map { issue ->
                issue.code
            }
        )
    }

    @Test
    fun `same semantic learning item with different identifiers is rejected`() {
        val firstContent =
            createContent(
                id =
                    "content-1",
                primaryText =
                    "Hello"
            )

        val secondContent =
            createContent(
                id =
                    "content-2",
                primaryText =
                    "Hello"
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            firstContent,
                            secondContent
                        ),
                    learningItems =
                        listOf(
                            createLearningItem(
                                id =
                                    "item-1",
                                contentId =
                                    firstContent.id
                            ),
                            createLearningItem(
                                id =
                                    "item-2",
                                contentId =
                                    secondContent.id
                            )
                        )
                )
            )

        assertTrue(report.isValid)
        assertEquals(
            setOf(
                "DUPLICATE_CONTENT_FINGERPRINT",
                "DUPLICATE_LEARNING_ITEM_FINGERPRINT"
            ),
            report.warnings
                .map { issue ->
                    issue.code
                }
                .toSet()
        )
    }

    private fun createContent(
        id: String,
        primaryText: String
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
                        primaryText
                )
        )

    private fun createLearningItem(
        id: String,
        contentId: ContentId
    ): LearningItem =
        LearningItem(
            id =
                LearningItemId(
                    id
                ),
            contentId =
                contentId,
            mode =
                LearningMode.MEANING_RECOGNITION
        )
}
