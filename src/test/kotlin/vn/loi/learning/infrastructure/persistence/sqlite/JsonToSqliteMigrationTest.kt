package vn.loi.learning.infrastructure.persistence.sqlite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.infrastructure.persistence.json.*
import vn.loi.learning.infrastructure.persistence.record.*

class JsonToSqliteMigrationTest {

    @Test
    fun `migration transfers all legacy JSON records to SQLite and is idempotent`(@TempDir tempDir: Path) {
        val contentsPath = tempDir.resolve("contents.json")
        val itemsPath = tempDir.resolve("learning-items.json")
        val memoryStatesPath = tempDir.resolve("memory-states.json")
        val reviewEventsPath = tempDir.resolve("review-events.json")

        val contentStore = JsonContentStore(contentsPath)
        val itemStore = JsonLearningItemStore(itemsPath)
        val memoryStore = JsonMemoryStateStore(memoryStatesPath)
        val reviewStore = JsonReviewEventStore(reviewEventsPath)

        contentStore.saveAll(listOf(
            ContentRecord(
                id = "cnt-legacy-1",
                type = "WORD",
                primaryText = "sunshine",
                translatedText = "anh nang",
                pronunciation = null,
                exampleText = null,
                exampleTranslation = null,
                customFields = mapOf("pos" to "noun")
            )
        ))

        itemStore.saveAll(listOf(
            LearningItemRecord(
                id = "item-legacy-1",
                contentId = "cnt-legacy-1",
                mode = "MEANING_RECOGNITION",
                isEnabled = true
            )
        ))

        memoryStore.save(listOf(
            MemoryStateRecord(
                schemaVersion = 1,
                learnerId = "learner-legacy",
                learningItemId = "item-legacy-1",
                stage = "LEARNING",
                difficulty = 4.0,
                stabilityDays = 3.0,
                dueAtEpochMillis = 1750000000000L,
                lastReviewedAtEpochMillis = 1740000000000L,
                reviewCount = 2,
                lapseCount = 0
            )
        ))

        reviewStore.saveAll(listOf(
            ReviewEventRecord(
                schemaVersion = 1,
                id = "rev-legacy-1",
                rating = "GOOD",
                reviewedAtEpochMillis = 1740000000000L,
                responseTimeMillis = 1200L,
                stateBefore = MemoryStateRecord(
                    schemaVersion = 1,
                    learnerId = "learner-legacy",
                    learningItemId = "item-legacy-1",
                    stage = "NEW",
                    difficulty = 4.0,
                    stabilityDays = 1.0,
                    dueAtEpochMillis = 1740000000000L,
                    lastReviewedAtEpochMillis = null,
                    reviewCount = 0,
                    lapseCount = 0
                ),
                stateAfter = MemoryStateRecord(
                    schemaVersion = 1,
                    learnerId = "learner-legacy",
                    learningItemId = "item-legacy-1",
                    stage = "LEARNING",
                    difficulty = 4.0,
                    stabilityDays = 3.0,
                    dueAtEpochMillis = 1750000000000L,
                    lastReviewedAtEpochMillis = 1740000000000L,
                    reviewCount = 1,
                    lapseCount = 0
                ),
                source = "STANDARD_REVIEW"
            )
        ))

        val dbFile = tempDir.resolve("learning_engine.db")
        val database = SqliteDatabaseFactory.createFromFile(dbFile)

        // First migration
        val report = JsonToSqliteMigrationService.migrateIfNeeded(tempDir, database)
        assertTrue(report.migrated)
        assertEquals(1, report.contentCount)
        assertEquals(1, report.learningItemCount)
        assertEquals(1, report.memoryStateCount)
        assertEquals(1, report.reviewEventCount)

        // Verify SQLite data
        val contentRepo = SqliteContentRepository(database)
        val loadedContent = contentRepo.findById(ContentId("cnt-legacy-1"))
        assertNotNull(loadedContent)
        assertEquals("sunshine", loadedContent!!.text.primaryText)
        assertEquals("anh nang", loadedContent.text.translatedText)

        val itemRepo = SqliteLearningItemRepository(database)
        val loadedItem = itemRepo.findById(LearningItemId("item-legacy-1"))
        assertNotNull(loadedItem)
        assertEquals(ContentId("cnt-legacy-1"), loadedItem!!.contentId)

        val memoryRepo = SqliteMemoryStateRepository(database)
        val loadedMemory = memoryRepo.find(LearnerId("learner-legacy"), LearningItemId("item-legacy-1"))
        assertNotNull(loadedMemory)
        assertEquals(4.0, loadedMemory!!.difficulty)

        // Verify JSON files are NOT deleted
        assertTrue(Files.exists(contentsPath))
        assertTrue(Files.exists(itemsPath))
        assertTrue(Files.exists(memoryStatesPath))
        assertTrue(Files.exists(reviewEventsPath))

        // Idempotency: Running migration again should do nothing
        val secondReport = JsonToSqliteMigrationService.migrateIfNeeded(tempDir, database)
        assertFalse(secondReport.migrated)
    }
}
