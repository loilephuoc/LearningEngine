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
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class InstalledContentConflictValidatorTest {

    @Test
    fun `new identifiers produce valid report`() {
        val validator =
            InstalledContentConflictValidator(
                contentRepository =
                    InMemoryContentRepository(),
                learningItemRepository =
                    InMemoryLearningItemRepository()
            )

        val content =
            createContent(
                "content-1"
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            content
                        ),
                    learningItems =
                        listOf(
                            createLearningItem(
                                id =
                                    "item-1",
                                contentId =
                                    content.id
                            )
                        )
                )
            )

        assertTrue(
            report.isValid
        )

        assertTrue(
            report.issues.isEmpty()
        )
    }

    @Test
    fun `installed content and learning item identifiers produce errors`() {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val content =
            createContent(
                "content-1"
            )

        val learningItem =
            createLearningItem(
                id =
                    "item-1",
                contentId =
                    content.id
            )

        contentRepository.save(
            content
        )

        learningItemRepository.save(
            learningItem
        )

        val validator =
            InstalledContentConflictValidator(
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            content
                        ),
                    learningItems =
                        listOf(
                            learningItem
                        )
                )
            )

        assertEquals(
            listOf(
                "CONTENT_ID_ALREADY_INSTALLED",
                "LEARNING_ITEM_ID_ALREADY_INSTALLED"
            ),
            report.errors.map { issue ->
                issue.code
            }
        )
    }

    @Test
    fun `duplicate identifiers inside package produce one installed conflict each`() {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val content =
            createContent(
                "content-1"
            )

        val learningItem =
            createLearningItem(
                id =
                    "item-1",
                contentId =
                    content.id
            )

        contentRepository.save(
            content
        )

        learningItemRepository.save(
            learningItem
        )

        val validator =
            InstalledContentConflictValidator(
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository
            )

        val report =
            validator.validate(
                ImportedPackageContent(
                    contents =
                        listOf(
                            content,
                            content
                        ),
                    learningItems =
                        listOf(
                            learningItem,
                            learningItem
                        )
                )
            )

        assertEquals(
            2,
            report.errors.size
        )
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