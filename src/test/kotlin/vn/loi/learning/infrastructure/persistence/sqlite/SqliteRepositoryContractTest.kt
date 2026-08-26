package vn.loi.learning.infrastructure.persistence.sqlite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.application.continuousreview.ContinuousReviewIntent
import vn.loi.learning.infrastructure.persistence.record.KnowledgeGraphRecord
import vn.loi.learning.infrastructure.persistence.record.KnowledgeNodeRecord
import vn.loi.learning.infrastructure.persistence.record.KnowledgeEdgeRecord

class SqliteRepositoryContractTest {

    private val database = SqliteDatabaseFactory.createInMemory()

    @Test
    fun `content repository CRUD and batch queries preserve all fields`() {
        val repo = SqliteContentRepository(database)
        val content = vn.loi.learning.domain.content.model.Content(
            id = ContentId("cnt-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "hello",
                translatedText = "xin chao",
                pronunciation = "həˈloʊ",
                exampleText = "Hello world",
                exampleTranslation = "Xin chao the gioi"
            ),
            media = ContentMedia(
                primaryAudio = "audio/hello.mp3",
                translatedAudio = null,
                image = "images/hello.png",
                exampleAudio = null,
                exampleTranslatedAudio = null
            ),
            metadata = ContentMetadata(
                title = "Greeting",
                group = "Basics",
                section = "Unit 1",
                lesson = "Lesson 1",
                tags = setOf("greeting", "starter"),
                source = "manual"
            ),
            customFields = ContentCustomFields(
                setOf(ContentCustomField(ContentFieldId("part_of_speech"), "noun"))
            )
        )

        repo.save(content)
        val loaded = repo.findById(ContentId("cnt-1"))
        assertNotNull(loaded)
        assertEquals("hello", loaded!!.text.primaryText)
        assertEquals("xin chao", loaded.text.translatedText)
        assertEquals("Lesson 1", loaded.metadata.lesson)
        assertEquals(setOf("greeting", "starter"), loaded.metadata.tags)
        assertEquals("noun", loaded.customFields.fields.first().value)

        val batch = repo.findByIds(listOf(ContentId("cnt-1"), ContentId("non-existent")))
        assertEquals(1, batch.size)
        assertEquals("cnt-1", batch.first().id.value)

        repo.deleteById(ContentId("cnt-1"))
        assertNull(repo.findById(ContentId("cnt-1")))
    }

    @Test
    fun `learning item repository and indexed queries operate correctly`() {
        val repo = SqliteLearningItemRepository(database)
        val item1 = LearningItem(
            id = LearningItemId("item-1"),
            contentId = ContentId("cnt-1"),
            mode = LearningMode.MEANING_RECOGNITION,
            isEnabled = true
        )
        val item2 = LearningItem(
            id = LearningItemId("item-2"),
            contentId = ContentId("cnt-1"),
            mode = LearningMode.MEANING_RECALL,
            isEnabled = false
        )
        repo.saveAll(listOf(item1, item2))

        val allEnabled = repo.findAllEnabled()
        assertEquals(1, allEnabled.size)
        assertEquals("item-1", allEnabled.first().id.value)

        val byContent = repo.findByContentId(ContentId("cnt-1"))
        assertEquals(2, byContent.size)

        val contentIdsMap = repo.findContentIdsByLearningItemIds(setOf(LearningItemId("item-1"), LearningItemId("item-2")))
        assertEquals(2, contentIdsMap.size)
        assertEquals(ContentId("cnt-1"), contentIdsMap[LearningItemId("item-1")])

        repo.deleteByContentIds(setOf(ContentId("cnt-1")))
        assertTrue(repo.findByContentId(ContentId("cnt-1")).isEmpty())
    }

    @Test
    fun `memory state repository queries and composite primary keys work accurately`() {
        val repo = SqliteMemoryStateRepository(database)
        val state = MemoryState(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1"),
            stage = LearningStage.LEARNING,
            difficulty = 3.5,
            stabilityDays = 2.0,
            dueAt = Moment(1700000000000L),
            lastReviewedAt = Moment(1699900000000L),
            reviewCount = 1,
            lapseCount = 0
        )
        repo.save(state)

        val loaded = repo.find(LearnerId("learner-1"), LearningItemId("item-1"))
        assertNotNull(loaded)
        assertEquals(3.5, loaded!!.difficulty)
        assertEquals(1700000000000L, loaded.dueAt.epochMillis)

        val allLearner = repo.findAll(LearnerId("learner-1"))
        assertEquals(1, allLearner.size)

        repo.delete(LearnerId("learner-1"), LearningItemId("item-1"))
        assertNull(repo.find(LearnerId("learner-1"), LearningItemId("item-1")))
    }

    @Test
    fun `review event append, range query, and removeLatest work accurately`() {
        val repo = SqliteReviewEventRepository(database)
        val stateBefore = MemoryState(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1"),
            stage = LearningStage.NEW,
            difficulty = 2.5,
            stabilityDays = 1.0,
            dueAt = Moment(1700000000000L),
            lastReviewedAt = null,
            reviewCount = 0,
            lapseCount = 0
        )
        val stateAfter = stateBefore.copy(
            stage = LearningStage.LEARNING,
            reviewCount = 1,
            lastReviewedAt = Moment(1700000005000L)
        )
        val event = ReviewEvent(
            id = ReviewEventId("rev-1"),
            rating = ReviewRating.GOOD,
            reviewedAt = Moment(1700000005000L),
            responseTime = TimeSpan(1500L),
            stateBefore = stateBefore,
            stateAfter = stateAfter,
            source = RatingSource.STANDARD_REVIEW
        )

        repo.append(event)
        val loaded = repo.findAll(LearnerId("learner-1"))
        assertEquals(1, loaded.size)
        assertEquals("rev-1", loaded.first().id.value)

        repo.removeLatest(event)
        assertTrue(repo.findAll(LearnerId("learner-1")).isEmpty())
    }

    @Test
    fun `study session and study queue repository persistence works`() {
        val sessionRepo = SqliteStudySessionRepository(database)
        val queueRepo = SqliteStudyQueueRepository(database)

        val session = StudySession.start(
            id = SessionId("sess-1"),
            learnerId = LearnerId("learner-1"),
            startedAt = Moment(1700000000000L),
            policy = SessionPolicy(
                newItemLimit = 10,
                reviewItemLimit = 20,
                allowRepeatInSameSession = false
            ),
            includedContentIds = setOf(ContentId("cnt-1")),
            topicId = TopicId("topic-1"),
            installedPackageId = InstalledPackageId("pkg-1")
        )
        sessionRepo.save(session)

        val active = sessionRepo.findActiveByLearner(LearnerId("learner-1"))
        assertNotNull(active)
        assertEquals("sess-1", active!!.id.value)

        val queue = vn.loi.learning.application.session.StudyQueueSnapshot(
            sessionId = SessionId("sess-1"),
            createdAt = Moment(1700000000000L),
            learningItemIds = listOf(LearningItemId("item-1"), LearningItemId("item-2")),
            currentIndex = 0,
            itemOrigins = mapOf(LearningItemId("item-1") to SessionItemOrigin.NEW),
            itemContentIds = mapOf(LearningItemId("item-1") to ContentId("cnt-1"))
        )
        queueRepo.save(queue)

        val loadedQueue = queueRepo.findBySessionId(SessionId("sess-1"))
        assertNotNull(loadedQueue)
        assertEquals(2, loadedQueue!!.learningItemIds.size)

        sessionRepo.deleteById(SessionId("sess-1"))
        queueRepo.deleteBySessionId(SessionId("sess-1"))
        assertNull(sessionRepo.findById(SessionId("sess-1")))
        assertNull(queueRepo.findBySessionId(SessionId("sess-1")))
    }

    @Test
    fun `canonical library and collection repositories work accurately`() {
        val libRepo = SqliteCanonicalLibraryRepository(database)
        val colRepo = SqliteCanonicalCollectionRepository(database)

        val library = Library.reconstitute(
            id = LibraryId("lib-1"),
            name = "English Library",
            entries = listOf(LibraryEntry(InstalledPackageId("inst-1"), PackageId("pkg-1"), Instant.now())),
            activePackageId = InstalledPackageId("inst-1"),
            createdAt = Instant.now()
        )
        libRepo.save(library)
        assertTrue(libRepo.existsById(LibraryId("lib-1")))
        assertEquals("English Library", libRepo.findById(LibraryId("lib-1"))?.name)

        val collection = vn.loi.learning.domain.library.model.Collection.reconstitute(
            id = CollectionId("col-1"),
            libraryId = LibraryId("lib-1"),
            name = CollectionName("Beginner"),
            description = "Beginner words",
            assignedPackageIds = setOf(InstalledPackageId("inst-1")),
            state = CollectionState.ACTIVE,
            createdAt = Instant.now()
        )
        colRepo.save(collection)
        val loadedCol = colRepo.findById(CollectionId("col-1"))
        assertNotNull(loadedCol)
        assertEquals("Beginner", loadedCol!!.name.value)
        assertEquals(1, colRepo.findAllByLibraryId(LibraryId("lib-1")).size)
    }

    @Test
    fun `knowledge graph and continuous review intent repositories work accurately`() {
        val kgRepo = SqliteKnowledgeGraphRepository(database)
        val intentRepo = SqliteContinuousReviewIntentRepository(database)

        val kgRecord = KnowledgeGraphRecord(
            nodes = listOf(KnowledgeNodeRecord("node-1", "concept", "Concept 1")),
            edges = listOf(KnowledgeEdgeRecord("node-1", "node-2", "DEPENDS_ON"))
        )
        kgRepo.save(kgRecord)
        val loadedKg = kgRepo.load()
        assertEquals(1, loadedKg.nodes.size)
        assertEquals("node-1", loadedKg.nodes.first().id)

        val intent = ContinuousReviewIntent(
            learnerId = LearnerId("learner-1"),
            installedPackageId = InstalledPackageId("pkg-1"),
            topicId = TopicId("topic-1"),
            enabled = true,
            updatedAt = Moment(1700000000000L)
        )
        intentRepo.save(intent)
        val loadedIntent = intentRepo.findByLearner(LearnerId("learner-1"))
        assertNotNull(loadedIntent)
        assertTrue(loadedIntent!!.enabled)
    }
}
