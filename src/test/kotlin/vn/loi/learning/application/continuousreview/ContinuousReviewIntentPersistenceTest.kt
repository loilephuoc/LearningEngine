package vn.loi.learning.application.continuousreview

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.json.InvalidJsonPersistenceException
import vn.loi.learning.infrastructure.persistence.json.JsonContinuousReviewIntentRepository
import vn.loi.learning.infrastructure.persistence.json.UnsupportedJsonPersistenceSchemaException

class ContinuousReviewIntentPersistenceTest {
    private val learner = LearnerId("learner-1")
    private val packageId = InstalledPackageId("package-1")
    private val topicId = TopicId("topic-1")

    @Test
    fun `legacy absence defaults to disabled and enable survives fresh composition`() {
        val directory = Files.createTempDirectory("continuous-review-intent")
        try {
            val first = LearningApplicationFactory.createPersisted(directory)
            assertNull(first.engine.getContinuousReviewIntent(learner))

            first.engine.enableContinuousReview(learner, packageId, topicId, Moment(10L))
            val restarted = LearningApplicationFactory.createPersisted(directory)
            val restored = restarted.engine.getContinuousReviewIntent(learner)!!

            assertTrue(restored.enabled)
            assertEquals(packageId, restored.installedPackageId)
            assertEquals(topicId, restored.topicId)
            assertEquals(Moment(10L), restored.updatedAt)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `disable is durable and preserves exact scope`() {
        val context = LearningApplicationFactory.createInMemory()
        context.engine.enableContinuousReview(learner, packageId, null, Moment(10L))

        val disabled = context.engine.disableContinuousReview(learner, Moment(20L))!!

        assertFalse(disabled.enabled)
        assertEquals(packageId, disabled.installedPackageId)
        assertNull(disabled.topicId)
        assertEquals(disabled, context.engine.getContinuousReviewIntent(learner))
    }

    @Test
    fun `malformed persistence is rejected without rewrite`() {
        val directory = Files.createTempDirectory("continuous-review-corrupt")
        val file = directory.resolve("continuous-review-intents.json")
        try {
            Files.writeString(file, "{broken")
            val before = Files.readAllBytes(file)
            assertFailsWith<InvalidJsonPersistenceException> {
                JsonContinuousReviewIntentRepository(file).findByLearner(learner)
            }
            assertTrue(before.contentEquals(Files.readAllBytes(file)))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `unsupported schema is rejected without rewrite`() {
        val directory = Files.createTempDirectory("continuous-review-schema")
        val file = directory.resolve("continuous-review-intents.json")
        try {
            Files.writeString(file, """{"schemaVersion":99,"records":[]}""")
            val before = Files.readAllBytes(file)
            assertFailsWith<UnsupportedJsonPersistenceSchemaException> {
                JsonContinuousReviewIntentRepository(file).findByLearner(learner)
            }
            assertTrue(before.contentEquals(Files.readAllBytes(file)))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
