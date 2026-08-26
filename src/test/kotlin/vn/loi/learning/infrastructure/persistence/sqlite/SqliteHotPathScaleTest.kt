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
    fun `scale benchmark with 10000 contents and 50000 items executes hot path queries within latency budget`() {
        val database = SqliteDatabaseFactory.createInMemory()
        val contentRepo = SqliteContentRepository(database)
        val itemRepo = SqliteLearningItemRepository(database)
        val memoryRepo = SqliteMemoryStateRepository(database)
        val reviewRepo = SqliteReviewEventRepository(database)

        val totalContents = 10000
        val itemsPerContent = 5
        val totalItems = totalContents * itemsPerContent

        val contents = ArrayList<DomainContent>(totalContents)
        val items = ArrayList<LearningItem>(totalItems)
        val memories = ArrayList<MemoryState>(5000)
        val reviews = ArrayList<ReviewEvent>(5000)

        val learnerId = LearnerId("learner-10k-50k")
        val now = Moment(1700000000000L)

        for (i in 1..totalContents) {
            val contentId = ContentId("cnt-$i")
            contents.add(
                DomainContent(
                    id = contentId,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "vocabulary_item_$i",
                        translatedText = "tu_vung_$i"
                    ),
                    metadata = ContentMetadata(
                        lesson = "Lesson ${i / 100}"
                    )
                )
            )
            val modes = LearningMode.entries.take(itemsPerContent)
            for ((idx, mode) in modes.withIndex()) {
                val itemId = LearningItemId("item-$i-$idx")
                items.add(
                    LearningItem(
                        id = itemId,
                        contentId = contentId,
                        mode = mode,
                        isEnabled = true
                    )
                )
                if (i <= 1000) {
                    val event = vn.loi.learning.testing.fixtures.ReviewFixtures.event(
                        id = ReviewEventId("rev-$i-$idx"),
                        learnerId = learnerId,
                        learningItemId = itemId,
                        rating = ReviewRating.GOOD,
                        reviewedAt = Moment(now.epochMillis - 86400000L),
                        previousReviewCount = 0
                    )
                    val mem = event.stateAfter.copy(dueAt = Moment(now.epochMillis - 1000L))
                    memories.add(mem)
                    reviews.add(event.copy(stateAfter = mem))
                }
            }
        }

        database.transaction {
            contentRepo.saveAll(contents)
            itemRepo.saveAll(items)
            memories.forEach { memoryRepo.save(it) }
            reviews.forEach { reviewRepo.append(it) }
        }

        assertEquals(10000, database.contentQueries.countAll().executeAsOne())
        assertEquals(50000, database.learningItemQueries.countAll().executeAsOne())
        assertEquals(5000, memories.size)

        val budgetService = DailyStudyBudgetQueryService(
            reviewEvents = reviewRepo,
            learningItems = itemRepo,
            memories = memoryRepo
        )

        val scopeContentIds = contents.take(500).map { it.id }.toSet()
        val startNs = System.nanoTime()
        val budget = budgetService.execute(
            learnerId = learnerId,
            limits = DailyStudyBudgetLimits(newPerDay = 20, reviewPerDay = 100),
            at = now,
            zoneId = ZoneId.of("UTC"),
            scopeContentIds = scopeContentIds
        )
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0

        assertNotNull(budget)
        // 500 contents * 5 items = 2500 items, all in first 1000 so all are due
        assertEquals(2500, budget.dueReviewCount)
        println("ScaleTest 10k/50k DailyStudyBudget elapsedMs: $elapsedMs")
        assertTrue(elapsedMs < 500.0, "10k/50k daily budget query must complete under 500ms, took ${elapsedMs}ms")
    }

    @Test
    fun `scale benchmark with 50000 contents and 250000 items verifies indexed query performance`() {
        val database = SqliteDatabaseFactory.createInMemory()
        val contentRepo = SqliteContentRepository(database)
        val itemRepo = SqliteLearningItemRepository(database)

        val totalContents = 50000
        val itemsPerContent = 5
        val totalItems = totalContents * itemsPerContent

        val contents = ArrayList<DomainContent>(totalContents)
        val items = ArrayList<LearningItem>(totalItems)

        for (i in 1..totalContents) {
            val contentId = ContentId("cnt-$i")
            contents.add(
                DomainContent(
                    id = contentId,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "vocab_$i",
                        translatedText = "nghia_$i"
                    ),
                    metadata = ContentMetadata(
                        lesson = "Lesson ${i / 500}"
                    )
                )
            )
            val modes = LearningMode.entries.take(itemsPerContent)
            for ((idx, mode) in modes.withIndex()) {
                val itemId = LearningItemId("item-$i-$idx")
                items.add(
                    LearningItem(
                        id = itemId,
                        contentId = contentId,
                        mode = mode,
                        isEnabled = true
                    )
                )
            }
        }

        database.transaction {
            contentRepo.saveAll(contents)
            itemRepo.saveAll(items)
        }

        assertEquals(50000, database.contentQueries.countAll().executeAsOne())
        assertEquals(250000, database.learningItemQueries.countAll().executeAsOne())

        // Test indexed lookups
        val startLookupNs = System.nanoTime()
        val foundItems = itemRepo.findByContentId(ContentId("cnt-25000"))
        val lookupElapsedMs = (System.nanoTime() - startLookupNs) / 1_000_000.0

        assertEquals(5, foundItems.size)
        println("ScaleTest 50k/250k findByContentId elapsedMs: $lookupElapsedMs")
        assertTrue(lookupElapsedMs < 50.0, "Indexed lookup on 250k items must be < 50ms, took ${lookupElapsedMs}ms")

        val startBatchNs = System.nanoTime()
        val batchContentIds = (1000..1200).map { ContentId("cnt-$it") }.toSet()
        val batchItems = itemRepo.findByContentIds(batchContentIds)
        val batchElapsedMs = (System.nanoTime() - startBatchNs) / 1_000_000.0

        assertEquals(201 * 5, batchItems.size)
        println("ScaleTest 50k/250k batch findByContentIds (201 contents / 1005 items) elapsedMs: $batchElapsedMs")
        assertTrue(batchElapsedMs < 100.0, "Batch lookup on 250k items must be < 100ms, took ${batchElapsedMs}ms")
    }
}
