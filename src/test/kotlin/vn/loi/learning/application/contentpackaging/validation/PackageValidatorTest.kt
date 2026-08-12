package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class PackageValidatorTest {

    @Test
    fun `validator aggregates issues from all validators`() {
        val validator =
            PackageValidator()

        val report =
            validator.validate(
                PackageDescriptor(
                    name =
                        "Demo",
                    version =
                        "invalid",
                    format =
                        "ZIP"
                ),
                ImportedPackageContent(
                    contents =
                        listOf(
                            content(
                                "c1"
                            ),
                            content(
                                "c1"
                            )
                        ),
                    learningItems =
                        listOf(
                            learningItem(
                                id =
                                    "i1",
                                contentId =
                                    ContentId(
                                        "missing"
                                    )
                            )
                        )
                )
            )

        assertEquals(4, report.errors.size)

        assertEquals(
            setOf(
                "UNSUPPORTED_PACKAGE_FORMAT",
                "INVALID_PACKAGE_VERSION",
                "DUPLICATE_CONTENT_ID",
                "MISSING_LEARNING_ITEM_CONTENT"
            ),
            report.errors
                .map { issue ->
                    issue.code
                }
                .toSet()
        )

        assertEquals(
            setOf("DUPLICATE_CONTENT_FINGERPRINT"),
            report.warnings.map { issue -> issue.code }.toSet()
        )
    }

    private fun content(
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
                        "hello"
                )
        )

    private fun learningItem(
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
