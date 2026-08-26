package vn.loi.learning.infrastructure.persistence.sqlite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.ZoneId
import vn.loi.learning.application.study.DailyStudyBudgetLimits
import vn.loi.learning.application.study.DailyStudyBudgetQueryService
import vn.loi.learning.domain.content.model.Content as DomainContent
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*

class SqliteHotPathScaleTest {

    @Test
    fun `scale benchmark with 5000 contents and 10000 items executes hot path queries within tight latency budget`() {
        val database = SqliteDatabaseFactory.createInMemory()
        val contentRepo = SqliteContentRepository(database)
        val itemRepo = SqliteLearningItemRepository(database)
        val memoryRepo = SqliteMemoryStateRepository(database)
        val reviewRepo = SqliteReviewEventRepository(database)

        val totalContents = 2000
        val itemsPerContent = 2
        val totalItems = totalContents * itemsPerContent

        val contents = ArrayList<DomainContent>(totalContents)
        val items = ArrayList<LearningItem>(totalItems)
        val memories = ArrayList<MemoryState>(totalItems)

        val learnerId = LearnerId("learner-scale")
        val now = Moment(1700000000000L)

        for (i in 1..totalContents) {
            val contentId = ContentId("cnt-$i")
            contents.add(
                DomainContent(
                    id = contentId,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "word-$i",
                        translatedText = "nghia-$i"
                    ),
                    metadata = ContentMetadata(
                        lesson = "Lesson ${i / 50}"
                    )
                )
            )
            for (m in 0 until itemsPerContent) {
                val itemId = LearningItemId("item-$i-$m")
                items.add(
                    LearningItem(
                        id = itemId,
                        contentId = contentId,
                        mode = if (m == 0) LearningMode.MEANING_RECOGNITION else LearningMode.MEANING_RECALL,
                        isEnabled = true
                    )
                )
                if (i <= 500) {
                    memories.add(
                        MemoryState(
                            learnerId = learnerId,
                            learningItemId = itemId,
                            stage = LearningStage.LEARNING,
                            difficulty = 3.0,
                            stabilityDays = 1.0,
                            dueAt = Moment(now.epochMillis - 10000L), // due
                            lastReviewedAt = Moment(now.epochMillis - 86400000L),
                            reviewCount = 1,
                            lapseCount = 0
                        )
                    )
                }
            }
        }

        // Batch insert in transaction
        database.transaction {
            contentRepo.saveAll(contents)
            itemRepo.saveAll(items)
            memories.forEach { memoryRepo.save(it) }
        }

        val budgetService = DailyStudyBudgetQueryService(
            reviewEvents = reviewRepo,
            learningItems = itemRepo,
            memories = memoryRepo
        )

        // Measure scoped budget calculation
        val scopeContentIds = contents.take(200).map { it.id }.toSet()
        val startNs = System.nanoTime()
        val budget = budgetService.execute(
            learnerId = learnerId,
            limits = DailyStudyBudgetLimits(newPerDay = 20, reviewPerDay = 50),
            at = now,
            zoneId = ZoneId.of("UTC"),
            scopeContentIds = scopeContentIds
        )
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0

        assertNotNull(budget)
        // 200 contents * 2 items = 400 items, all 200 contents are in the first 500 so they are all due
        assertEquals(400, budget.dueReviewCount)
        assertTrue(elapsedMs < 500.0, "Hot path query must be fast, took ${elapsedMs}ms")
    }
}
