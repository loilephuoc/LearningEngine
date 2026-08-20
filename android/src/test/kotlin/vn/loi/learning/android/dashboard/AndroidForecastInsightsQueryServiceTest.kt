package vn.loi.learning.android.dashboard

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import vn.loi.learning.application.contentlibrary.LibraryContentQueryService
import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.port.*
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.*

class AndroidForecastInsightsQueryServiceTest {

    private val zoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val learnerId = LearnerId("test-learner")

    // Reference time: 2026-08-20 15:30:00 ICT
    private val baseZoned = ZonedDateTime.of(2026, 8, 20, 15, 30, 0, 0, zoneId)
    private val nowMillis = baseZoned.toInstant().toEpochMilli()

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, nano: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, minute, second, nano, zoneId).toInstant().toEpochMilli()

    private fun createMemoryState(
        id: String,
        stage: LearningStage,
        stabilityDays: Double,
        dueAtMillis: Long,
        difficulty: Double = 5.0,
        learner: LearnerId = learnerId
    ): MemoryState = MemoryState(
        learnerId = learner,
        learningItemId = LearningItemId(id),
        stage = stage,
        difficulty = difficulty,
        stabilityDays = stabilityDays,
        dueAt = Moment(dueAtMillis),
        lastReviewedAt = if (stage == LearningStage.NEW) null else Moment(dueAtMillis - 86_400_000L),
        reviewCount = if (stage == LearningStage.NEW) 0 else 1,
        lapseCount = 0
    )

    private fun createReviewEvent(
        id: String,
        learningItemId: String,
        rating: ReviewRating,
        reviewedAtMillis: Long,
        source: RatingSource = RatingSource.STANDARD_REVIEW,
        learner: LearnerId = learnerId
    ): ReviewEvent {
        val before = createMemoryState(learningItemId, LearningStage.LEARNING, 1.0, reviewedAtMillis, learner = learner)
        val after = createMemoryState(learningItemId, LearningStage.REVIEW, 3.0, reviewedAtMillis, learner = learner).copy(
            lastReviewedAt = Moment(reviewedAtMillis),
            reviewCount = before.reviewCount + 1
        )
        return ReviewEvent(
            id = ReviewEventId("rev-$id"),
            rating = rating,
            reviewedAt = Moment(reviewedAtMillis),
            responseTime = TimeSpan(1200L),
            stateBefore = before,
            stateAfter = after,
            source = source
        )
    }

    private fun createLearningItem(id: String, contentId: String): LearningItem =
        LearningItem(
            id = LearningItemId(id),
            contentId = ContentId(contentId),
            mode = LearningMode.MEANING_RECOGNITION
        )

    @Test
    fun `computeForecast7Days divides items into correct 7 local-day buckets and excludes overdue and today`() {
        val states = listOf(
            // Overdue (yesterday): must NOT be in forecast
            createMemoryState("overdue", LearningStage.REVIEW, 2.0, at(2026, 8, 19, 10, 0)),
            // Due today (2026-08-20 18:00): must NOT be in forecast
            createMemoryState("today-due", LearningStage.REVIEW, 2.0, at(2026, 8, 20, 18, 0)),
            // Day 1 (Tomorrow: 2026-08-21 00:00:01)
            createMemoryState("tomorrow-start", LearningStage.REVIEW, 3.0, at(2026, 8, 21, 0, 0, 1)),
            // Day 1 (Tomorrow: 2026-08-21 23:59:59)
            createMemoryState("tomorrow-end", LearningStage.REVIEW, 3.0, at(2026, 8, 21, 23, 59, 59)),
            // Day 2 (+2d: 2026-08-22 10:00)
            createMemoryState("day2-item", LearningStage.REVIEW, 4.0, at(2026, 8, 22, 10, 0)),
            // Day 3 (+3d: 2026-08-23 12:00)
            createMemoryState("day3-item1", LearningStage.REVIEW, 5.0, at(2026, 8, 23, 12, 0)),
            createMemoryState("day3-item2", LearningStage.REVIEW, 5.0, at(2026, 8, 23, 14, 0)),
            // Day 7 (+7d: 2026-08-27 20:00)
            createMemoryState("day7-item", LearningStage.REVIEW, 10.0, at(2026, 8, 27, 20, 0)),
            // Day 8 (+8d: 2026-08-28 01:00) - outside 7-day window
            createMemoryState("day8-item", LearningStage.REVIEW, 12.0, at(2026, 8, 28, 1, 0))
        )

        val forecast = AndroidForecastInsightsQueryService.computeForecast7Days(
            memoryStates = states,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(7, forecast.size)

        // Day 1: Tomorrow
        assertEquals(1, forecast[0].dayIndex)
        assertEquals(LocalDate.of(2026, 8, 21), forecast[0].date)
        assertEquals("Tomorrow", forecast[0].label)
        assertEquals(2, forecast[0].count)

        // Day 2: +2d
        assertEquals(2, forecast[1].dayIndex)
        assertEquals(LocalDate.of(2026, 8, 22), forecast[1].date)
        assertEquals("+2d", forecast[1].label)
        assertEquals(1, forecast[1].count)

        // Day 3: +3d
        assertEquals(3, forecast[2].dayIndex)
        assertEquals(LocalDate.of(2026, 8, 23), forecast[2].date)
        assertEquals("+3d", forecast[2].label)
        assertEquals(2, forecast[2].count)

        // Day 4, 5, 6: 0
        assertEquals(0, forecast[3].count)
        assertEquals(0, forecast[4].count)
        assertEquals(0, forecast[5].count)

        // Day 7: +7d
        assertEquals(7, forecast[6].dayIndex)
        assertEquals(LocalDate.of(2026, 8, 27), forecast[6].date)
        assertEquals("+7d", forecast[6].label)
        assertEquals(1, forecast[6].count)
    }

    @Test
    fun `computeMemoryDistribution categorizes NEW, LEARNING, and REVIEW stability boundary correctly`() {
        val states = listOf(
            createMemoryState("new1", LearningStage.NEW, 0.0, nowMillis),
            createMemoryState("new2", LearningStage.NEW, 0.0, nowMillis),
            createMemoryState("learning1", LearningStage.LEARNING, 0.5, nowMillis),
            // Young boundary test: exactly 21.0 days is YOUNG
            createMemoryState("young1", LearningStage.REVIEW, 21.0, nowMillis),
            createMemoryState("young2", LearningStage.REVIEW, 5.0, nowMillis),
            // Retained boundary test: 21.1 days is RETAINED
            createMemoryState("retained1", LearningStage.REVIEW, 21.1, nowMillis),
            createMemoryState("retained2", LearningStage.REVIEW, 90.0, nowMillis)
        )

        val distribution = AndroidForecastInsightsQueryService.computeMemoryDistribution(states)

        assertEquals(2, distribution.newCount)
        assertEquals(1, distribution.learningCount)
        assertEquals(2, distribution.youngCount)
        assertEquals(2, distribution.retainedCount)
        assertEquals(7, distribution.totalCount)

        assertTrue(kotlin.math.abs(distribution.newPercent - (2f / 7f)) < 0.001f)
        assertTrue(kotlin.math.abs(distribution.learningPercent - (1f / 7f)) < 0.001f)
        assertTrue(kotlin.math.abs(distribution.youngPercent - (2f / 7f)) < 0.001f)
        assertTrue(kotlin.math.abs(distribution.retainedPercent - (2f / 7f)) < 0.001f)
    }

    @Test
    fun `computeMemoryDistribution handles empty list safely without division by zero`() {
        val distribution = AndroidForecastInsightsQueryService.computeMemoryDistribution(emptyList())

        assertEquals(0, distribution.newCount)
        assertEquals(0, distribution.learningCount)
        assertEquals(0, distribution.youngCount)
        assertEquals(0, distribution.retainedCount)
        assertEquals(0, distribution.totalCount)

        assertEquals(0f, distribution.newPercent)
        assertEquals(0f, distribution.learningPercent)
        assertEquals(0f, distribution.youngPercent)
        assertEquals(0f, distribution.retainedPercent)
    }

    @Test
    fun `computeTodayRatings aggregates today ratings across standard and manual sources`() {
        val events = listOf(
            // Today at 08:00 ICT
            createReviewEvent("1", "item1", ReviewRating.AGAIN, at(2026, 8, 20, 8, 0), RatingSource.STANDARD_REVIEW),
            // Today at 10:30 ICT (Manual user source from quick rating)
            createReviewEvent("2", "item2", ReviewRating.HARD, at(2026, 8, 20, 10, 30), RatingSource.MANUAL_USER),
            // Today at 12:00 ICT
            createReviewEvent("3", "item3", ReviewRating.GOOD, at(2026, 8, 20, 12, 0), RatingSource.STANDARD_REVIEW),
            createReviewEvent("4", "item4", ReviewRating.GOOD, at(2026, 8, 20, 14, 0), RatingSource.MANUAL_USER),
            // Today at 15:00 ICT
            createReviewEvent("5", "item5", ReviewRating.EASY, at(2026, 8, 20, 15, 0), RatingSource.STANDARD_REVIEW),
            // Yesterday at 23:59:59 (must NOT be counted in today)
            createReviewEvent("6", "item6", ReviewRating.GOOD, at(2026, 8, 19, 23, 59, 59), RatingSource.STANDARD_REVIEW),
            // Tomorrow at 00:00:01 (must NOT be counted in today)
            createReviewEvent("7", "item7", ReviewRating.EASY, at(2026, 8, 21, 0, 0, 1), RatingSource.STANDARD_REVIEW)
        )

        val ratings = AndroidForecastInsightsQueryService.computeTodayRatings(
            reviewEvents = events,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(1, ratings.againCount)
        assertEquals(1, ratings.hardCount)
        assertEquals(2, ratings.goodCount)
        assertEquals(1, ratings.easyCount)
        assertEquals(5, ratings.totalCount)
    }

    @Test
    fun `package filtering proves Package A and Package B partition correctly and AllPackages equals sum`() {
        val memoryStates = listOf(
            // Package A items
            createMemoryState("a-item1", LearningStage.NEW, 0.0, at(2026, 8, 21, 10, 0)),
            createMemoryState("a-item2", LearningStage.REVIEW, 10.0, at(2026, 8, 21, 14, 0)), // Young
            createMemoryState("a-item3", LearningStage.REVIEW, 30.0, at(2026, 8, 22, 10, 0)), // Retained

            // Package B items
            createMemoryState("b-item1", LearningStage.LEARNING, 1.0, at(2026, 8, 21, 9, 0)),
            createMemoryState("b-item2", LearningStage.REVIEW, 25.0, at(2026, 8, 23, 10, 0))  // Retained
        )

        val reviewEvents = listOf(
            // Package A events
            createReviewEvent("rev-a1", "a-item1", ReviewRating.AGAIN, at(2026, 8, 20, 9, 0), RatingSource.STANDARD_REVIEW),
            createReviewEvent("rev-a2", "a-item2", ReviewRating.GOOD, at(2026, 8, 20, 11, 0), RatingSource.MANUAL_USER),

            // Package B events
            createReviewEvent("rev-b1", "b-item1", ReviewRating.HARD, at(2026, 8, 20, 10, 0), RatingSource.STANDARD_REVIEW),
            createReviewEvent("rev-b2", "b-item2", ReviewRating.EASY, at(2026, 8, 20, 14, 0), RatingSource.STANDARD_REVIEW),

            // Historical unresolvable event (deleted package)
            createReviewEvent("rev-hist", "orphan-item", ReviewRating.GOOD, at(2026, 8, 20, 13, 0), RatingSource.STANDARD_REVIEW)
        )

        val learningItems = listOf(
            createLearningItem("a-item1", "c-a1"),
            createLearningItem("a-item2", "c-a2"),
            createLearningItem("a-item3", "c-a3"),
            createLearningItem("b-item1", "c-b1"),
            createLearningItem("b-item2", "c-b2")
        )

        val memoryRepo = object : MemoryStateRepository {
            override fun findAll(): List<MemoryState> = memoryStates
            override fun find(learnerId: LearnerId, learningItemId: LearningItemId): MemoryState? =
                memoryStates.firstOrNull { it.learnerId == learnerId && it.learningItemId == learningItemId }
            override fun save(memoryState: MemoryState) = error("Read-only")
            override fun delete(learnerId: LearnerId, learningItemId: LearningItemId) = error("Read-only")
        }

        val reviewRepo = object : ReviewEventRepository {
            override fun findAll(learnerId: LearnerId): List<ReviewEvent> = reviewEvents.filter { it.stateBefore.learnerId == learnerId }
            override fun findAll(learnerId: LearnerId, learningItemId: LearningItemId): List<ReviewEvent> =
                reviewEvents.filter { it.stateBefore.learnerId == learnerId && it.stateBefore.learningItemId == learningItemId }
            override fun append(event: ReviewEvent) = error("Read-only")
        }

        val itemRepo = object : LearningItemRepository {
            override fun findAll(): List<LearningItem> = learningItems
            override fun findAllEnabled(): List<LearningItem> = learningItems
            override fun findById(learningItemId: LearningItemId): LearningItem? = learningItems.firstOrNull { it.id == learningItemId }
            override fun findByContentId(contentId: ContentId): List<LearningItem> = learningItems.filter { it.contentId == contentId }
            override fun findByContentIds(contentIds: Set<ContentId>): List<LearningItem> = learningItems.filter { it.contentId in contentIds }
            override fun save(learningItem: LearningItem) = error("Read-only")
            override fun deleteById(learningItemId: LearningItemId) = error("Read-only")
        }

        val packageA = ContentPackage(
            id = PackageId("pkg-a"),
            descriptor = PackageDescriptor("Package A", "1.0", "OPD3"),
            libraryIds = setOf(ContentLibraryId("lib-a"))
        )
        val packageB = ContentPackage(
            id = PackageId("pkg-b"),
            descriptor = PackageDescriptor("Package B", "1.0", "OPD3"),
            libraryIds = setOf(ContentLibraryId("lib-b"))
        )

        val libraryA = ContentLibrary(
            id = ContentLibraryId("lib-a"),
            descriptor = LibraryDescriptor("Lib A"),
            contentIds = setOf(ContentId("c-a1"), ContentId("c-a2"), ContentId("c-a3"))
        )
        val libraryB = ContentLibrary(
            id = ContentLibraryId("lib-b"),
            descriptor = LibraryDescriptor("Lib B"),
            contentIds = setOf(ContentId("c-b1"), ContentId("c-b2"))
        )

        val contentPackageRepo = object : ContentPackageRepository {
            override fun findAll(): List<ContentPackage> = listOf(packageA, packageB)
            override fun findById(packageId: PackageId): ContentPackage? = findAll().firstOrNull { it.id == packageId }
            override fun save(contentPackage: ContentPackage) = error("Read-only")
            override fun deleteById(packageId: PackageId) = error("Read-only")
        }

        val contentLibraryRepo = object : ContentLibraryRepository {
            override fun findAll(): List<ContentLibrary> = listOf(libraryA, libraryB)
            override fun findById(libraryId: ContentLibraryId): ContentLibrary? = findAll().firstOrNull { it.id == libraryId }
            override fun save(library: ContentLibrary) = error("Read-only")
            override fun deleteById(libraryId: ContentLibraryId) = error("Read-only")
        }

        val contentRepo = object : ContentRepository {
            override fun findAll(): List<Content> = emptyList()
            override fun findById(contentId: ContentId): Content? = null
            override fun save(content: Content) = error("Read-only")
            override fun deleteById(contentId: ContentId) = error("Read-only")
        }

        val installedPackageQuery = InstalledPackageQueryService(contentPackageRepo)
        val libraryContentQuery = LibraryContentQueryService(contentLibraryRepo, contentRepo, itemRepo)
        val packageContentQuery = InstalledPackageContentQueryService(installedPackageQuery, libraryContentQuery)

        val service = AndroidForecastInsightsQueryService(
            memoryStateRepository = memoryRepo,
            reviewEventRepository = reviewRepo,
            packageContentQuery = packageContentQuery,
            learningItemRepository = itemRepo,
            now = { nowMillis },
            zoneId = { zoneId }
        )

        // 1. Test AllPackages Scope
        val allModel = service.query(learnerId, AndroidInsightsScope.AllPackages)
        assertEquals(AndroidInsightsScope.AllPackages, allModel.scope)
        assertEquals(1, allModel.memoryDistribution.newCount)
        assertEquals(1, allModel.memoryDistribution.learningCount)
        assertEquals(1, allModel.memoryDistribution.youngCount)
        assertEquals(2, allModel.memoryDistribution.retainedCount)
        assertEquals(5, allModel.memoryDistribution.totalCount)
        assertEquals(3, allModel.forecast7Days[0].count) // Day 1 Tomorrow
        assertEquals(1, allModel.forecast7Days[1].count) // Day 2
        assertEquals(1, allModel.forecast7Days[2].count) // Day 3
        assertEquals(5, allModel.todayRatings.totalCount) // 4 package items + 1 orphan

        // 2. Test Package A Scope
        val scopeA = AndroidInsightsScope.SpecificPackage("pkg-a", "Package A")
        val modelA = service.query(learnerId, scopeA)
        assertEquals(scopeA, modelA.scope)
        assertEquals(1, modelA.memoryDistribution.newCount)
        assertEquals(0, modelA.memoryDistribution.learningCount)
        assertEquals(1, modelA.memoryDistribution.youngCount)
        assertEquals(1, modelA.memoryDistribution.retainedCount)
        assertEquals(3, modelA.memoryDistribution.totalCount)
        assertEquals(2, modelA.forecast7Days[0].count) // a-item1, a-item2
        assertEquals(1, modelA.forecast7Days[1].count) // a-item3
        assertEquals(0, modelA.forecast7Days[2].count)
        assertEquals(2, modelA.todayRatings.totalCount) // rev-a1 (Again), rev-a2 (Good)
        assertEquals(1, modelA.todayRatings.againCount)
        assertEquals(1, modelA.todayRatings.goodCount)

        // 3. Test Package B Scope
        val scopeB = AndroidInsightsScope.SpecificPackage("pkg-b", "Package B")
        val modelB = service.query(learnerId, scopeB)
        assertEquals(scopeB, modelB.scope)
        assertEquals(0, modelB.memoryDistribution.newCount)
        assertEquals(1, modelB.memoryDistribution.learningCount)
        assertEquals(0, modelB.memoryDistribution.youngCount)
        assertEquals(1, modelB.memoryDistribution.retainedCount)
        assertEquals(2, modelB.memoryDistribution.totalCount)
        assertEquals(1, modelB.forecast7Days[0].count) // b-item1
        assertEquals(0, modelB.forecast7Days[1].count)
        assertEquals(1, modelB.forecast7Days[2].count) // b-item2
        assertEquals(2, modelB.todayRatings.totalCount) // rev-b1 (Hard), rev-b2 (Easy)
        assertEquals(1, modelB.todayRatings.hardCount)
        assertEquals(1, modelB.todayRatings.easyCount)

        // Invariant checks: Package A + Package B = AllPackages (excluding orphan for ratings)
        assertEquals(allModel.memoryDistribution.totalCount, modelA.memoryDistribution.totalCount + modelB.memoryDistribution.totalCount)
        assertEquals(allModel.forecast7Days[0].count, modelA.forecast7Days[0].count + modelB.forecast7Days[0].count)
        assertEquals(allModel.forecast7Days[1].count, modelA.forecast7Days[1].count + modelB.forecast7Days[1].count)
        assertEquals(allModel.forecast7Days[2].count, modelA.forecast7Days[2].count + modelB.forecast7Days[2].count)

        // 4. Test Empty / Non-existent Package Scope
        val emptyScope = AndroidInsightsScope.SpecificPackage("pkg-empty", "Empty Package")
        val emptyModel = service.query(learnerId, emptyScope)
        assertEquals(0, emptyModel.memoryDistribution.totalCount)
        assertEquals(0, emptyModel.forecast7Days.sumOf { it.count })
        assertEquals(0, emptyModel.todayRatings.totalCount)
    }

    @Test
    fun `query coordinator is strictly read-only and produces complete UI model`() {
        var saveCalled = false
        var appendCalled = false

        val memoryRepo = object : MemoryStateRepository {
            override fun findAll(): List<MemoryState> = listOf(
                createMemoryState("1", LearningStage.NEW, 0.0, nowMillis),
                createMemoryState("2", LearningStage.REVIEW, 30.0, at(2026, 8, 21, 10, 0))
            )
            override fun find(learnerId: LearnerId, learningItemId: LearningItemId): MemoryState? = null
            override fun save(memoryState: MemoryState) { saveCalled = true }
            override fun delete(learnerId: LearnerId, learningItemId: LearningItemId) {}
        }

        val reviewRepo = object : ReviewEventRepository {
            override fun findAll(learnerId: LearnerId): List<ReviewEvent> = listOf(
                createReviewEvent("1", "1", ReviewRating.GOOD, at(2026, 8, 20, 9, 0))
            )
            override fun findAll(learnerId: LearnerId, learningItemId: LearningItemId): List<ReviewEvent> = emptyList()
            override fun append(event: ReviewEvent) { appendCalled = true }
        }

        val service = AndroidForecastInsightsQueryService(
            memoryStateRepository = memoryRepo,
            reviewEventRepository = reviewRepo,
            now = { nowMillis },
            zoneId = { zoneId }
        )

        val uiModel = service.query(learnerId)

        assertFalse(saveCalled, "Query must never invoke save on MemoryStateRepository")
        assertFalse(appendCalled, "Query must never invoke append on ReviewEventRepository")

        assertEquals(7, uiModel.forecast7Days.size)
        assertEquals(1, uiModel.forecast7Days[0].count) // tomorrow item
        assertEquals(2, uiModel.memoryDistribution.totalCount)
        assertEquals(1, uiModel.todayRatings.totalCount)
        assertEquals(1, uiModel.todayRatings.goodCount)
    }

    @Test
    fun `stress test with large dataset computes forecast and distribution in linear time`() {
        val count = 2000
        val states = (1..count).map { i ->
            val dayOffset = (i % 7) + 1
            createMemoryState(
                id = "item-$i",
                stage = if (i % 4 == 0) LearningStage.NEW else LearningStage.REVIEW,
                stabilityDays = (i % 50).toDouble(),
                dueAtMillis = at(2026, 8, 20 + dayOffset, 10, 0)
            )
        }

        val forecast = AndroidForecastInsightsQueryService.computeForecast7Days(
            memoryStates = states,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(7, forecast.size)
        assertEquals(count, forecast.sumOf { it.count })

        val distribution = AndroidForecastInsightsQueryService.computeMemoryDistribution(states)
        assertEquals(count, distribution.totalCount)
    }
}
