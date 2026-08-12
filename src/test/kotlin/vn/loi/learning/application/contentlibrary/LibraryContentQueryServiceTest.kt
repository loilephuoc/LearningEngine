package vn.loi.learning.application.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.application.port.ContentLibraryRepository

class LibraryContentQueryServiceTest {

    @Test
    fun `descriptor groups use one library snapshot and preserve missing duplicate shared empty and deterministic ordering`() {
        val libraries = InMemoryContentLibraryRepository()
        var findAllCalls = 0
        var findByIdCalls = 0
        val countingLibraries = object : ContentLibraryRepository by libraries {
            override fun findAll(): List<ContentLibrary> {
                findAllCalls++
                return libraries.findAll()
            }

            override fun findById(libraryId: ContentLibraryId): ContentLibrary? {
                findByIdCalls++
                return libraries.findById(libraryId)
            }
        }
        val contents = InMemoryContentRepository()
        val firstId = ContentId("content-a")
        val secondId = ContentId("content-b")
        contents.save(Content(firstId, ContentType.WORD, ContentText("Alpha", "A")))
        contents.save(Content(secondId, ContentType.WORD, ContentText("Beta", "B")))
        val sharedId = ContentLibraryId("shared")
        val emptyId = ContentLibraryId("empty")
        libraries.save(ContentLibrary(sharedId, LibraryDescriptor("Shared"), setOf(secondId, firstId)))
        libraries.save(ContentLibrary(emptyId, LibraryDescriptor("Empty"), emptySet()))
        val query = LibraryContentQueryService(countingLibraries, contents, InMemoryLearningItemRepository())

        val result = query.queryDescriptorGroups(
            linkedMapOf(
                "package-a" to listOf(sharedId, ContentLibraryId("missing"), sharedId),
                "package-b" to listOf(sharedId),
                "package-empty" to listOf(emptyId)
            )
        )

        assertEquals(listOf("content-a", "content-b"), result.getValue("package-a").map { it.id })
        assertEquals(result.getValue("package-a"), result.getValue("package-b"))
        assertTrue(result.getValue("package-empty").isEmpty())
        assertEquals(1, findAllCalls)
        assertEquals(0, findByIdCalls)
    }

    private val contentLibraryRepository =
        InMemoryContentLibraryRepository()

    private val contentRepository =
        InMemoryContentRepository()

    private val learningItemRepository =
        InMemoryLearningItemRepository()

    private val service =
        LibraryContentQueryService(
            contentLibraryRepository =
                contentLibraryRepository,
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository
        )

    @Test
    fun `query returns empty list when library does not exist`() {
        val result =
            service.query(
                ContentLibraryId("unknown-library")
            )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `query returns library contents ordered by display name`() {
        val vocabularyContentId =
            ContentId("content-vocabulary")

        val conversationContentId =
            ContentId("content-conversation")

        contentRepository.save(
            Content(
                id = vocabularyContentId,
                type = ContentType.WORD,
                text =
                    ContentText(
                        primaryText = "Vocabulary",
                        translatedText = "Từ vựng"
                    )
            )
        )

        contentRepository.save(
            Content(
                id = conversationContentId,
                type = ContentType.DIALOGUE,
                text =
                    ContentText(
                        primaryText = "Conversation",
                        translatedText = "Hội thoại"
                    )
            )
        )

        learningItemRepository.save(
            LearningItem(
                id =
                    LearningItemId(
                        "conversation-listening"
                    ),
                contentId = conversationContentId,
                mode =
                    LearningMode.LISTENING_RECOGNITION
            )
        )

        learningItemRepository.save(
            LearningItem(
                id =
                    LearningItemId(
                        "conversation-speaking"
                    ),
                contentId = conversationContentId,
                mode =
                    LearningMode.SPEAKING_RECALL
            )
        )

        learningItemRepository.save(
            LearningItem(
                id =
                    LearningItemId(
                        "conversation-disabled"
                    ),
                contentId = conversationContentId,
                mode =
                    LearningMode.SHADOWING,
                isEnabled = false
            )
        )

        contentLibraryRepository.save(
            ContentLibrary(
                id =
                    ContentLibraryId(
                        "english-library"
                    ),
                descriptor =
                    LibraryDescriptor(
                        name = "English"
                    ),
                contentIds =
                    setOf(
                        vocabularyContentId,
                        conversationContentId
                    )
            )
        )

        val result =
            service.query(
                ContentLibraryId("english-library")
            )

        assertEquals(
            expected = 2,
            actual = result.size
        )

        assertEquals(
            expected =
                LibraryContentItem(
                    id = "content-conversation",
                    title = "Conversation",
                    type = "DIALOGUE",
                    primaryText = "Conversation",
                    translatedText = "Hội thoại",
                    learningItemCount = 2
                ),
            actual = result[0]
        )

        assertEquals(
            expected = "Vocabulary",
            actual = result[1].title
        )

        assertEquals(
            expected = 0,
            actual = result[1].learningItemCount
        )
    }

    @Test
    fun `query ignores missing content referenced by library`() {
        contentLibraryRepository.save(
            ContentLibrary(
                id =
                    ContentLibraryId(
                        "incomplete-library"
                    ),
                descriptor =
                    LibraryDescriptor(
                        name = "Incomplete"
                    ),
                contentIds =
                    setOf(
                        ContentId("missing-content")
                    )
            )
        )

        val result =
            service.query(
                ContentLibraryId(
                    "incomplete-library"
                )
            )

        assertTrue(result.isEmpty())
    }
}
