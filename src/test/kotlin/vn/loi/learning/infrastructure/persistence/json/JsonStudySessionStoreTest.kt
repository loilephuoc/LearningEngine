package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.infrastructure.persistence.mapper.StudySessionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

class JsonStudySessionStoreTest {

    @Test
    fun `loadAll returns empty list when file does not exist`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-sessions.json")

            val store =
                JsonStudySessionStore(filePath)

            assertTrue(
                store.loadAll().isEmpty()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `loadAll rejects a blank existing file`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-sessions.json")

            Files.writeString(
                filePath,
                "   "
            )

            val store =
                JsonStudySessionStore(filePath)

            val failure =
                assertFailsWith<InvalidJsonPersistenceException> {
                    store.loadAll()
                }

            assertEquals(
                JsonPersistenceFailureKind.BLANK,
                failure.failureKind
            )

            assertEquals(
                "study session",
                failure.recordType
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `saveAll writes records to json file`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-sessions.json")

            val record =
                createRecord(
                    SessionId("session-1")
                )

            val store =
                JsonStudySessionStore(filePath)

            store.saveAll(
                listOf(record)
            )

            assertTrue(filePath.exists())

            assertEquals(
                expected = listOf(record),
                actual = store.loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `records survive creating a new store instance`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-sessions.json")

            val first =
                createRecord(
                    SessionId("session-1")
                )

            val second =
                createRecord(
                    SessionId("session-2")
                )

            JsonStudySessionStore(filePath)
                .saveAll(
                    listOf(first, second)
                )

            val reopened =
                JsonStudySessionStore(filePath)

            assertEquals(
                expected = listOf(first, second),
                actual = reopened.loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `practice policy survives serialization and reopening`() {
        val directory = Files.createTempDirectory("learning-engine-practice-policy")
        try {
            val filePath = directory.resolve("study-sessions.json")
            val record = StudySessionRecordMapper.toRecord(
                StudySession.start(
                    SessionId("practice-restart"),
                    LearnerId("learner-1"),
                    Moment(1_000L),
                    SessionPolicy(
                        evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
                        practiceLoopPolicy = PracticeLoopPolicy.LOOP_FIXED_MEMBERSHIP_SHUFFLED
                    )
                )
            )

            JsonStudySessionStore(filePath).saveAll(listOf(record))
            val restored = StudySessionRecordMapper.toDomain(
                JsonStudySessionStore(filePath).loadAll().single()
            )

            assertEquals(SessionEvaluationPolicy.PRACTICE_ONLY, restored.policy.evaluationPolicy)
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `saveAll replaces previous snapshot`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-sessions.json")

            val first =
                createRecord(
                    SessionId("session-1")
                )

            val second =
                createRecord(
                    SessionId("session-2")
                )

            val store =
                JsonStudySessionStore(filePath)

            store.saveAll(
                listOf(first)
            )

            store.saveAll(
                listOf(second)
            )

            assertEquals(
                expected = listOf(second),
                actual =
                    JsonStudySessionStore(filePath)
                        .loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    private fun createRecord(
        sessionId: SessionId
    ): StudySessionRecord =
        StudySessionRecordMapper.toRecord(
            StudySession.start(
                id = sessionId,
                learnerId = LearnerId("learner-1"),
                startedAt = Moment(1_000L),
                policy = SessionPolicy(
                    newItemLimit = 10,
                    reviewItemLimit = 10
                ),
                topicId = TopicId("topic-1")
            )
        )

    private fun deleteDirectoryRecursively(
        directory: java.nio.file.Path
    ) {
        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::deleteIfExists)
    }
}
