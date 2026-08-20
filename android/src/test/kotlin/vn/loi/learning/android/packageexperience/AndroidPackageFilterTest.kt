package vn.loi.learning.android.packageexperience

import org.junit.Test
import kotlin.test.*
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.android.reminder.AndroidVocabularyReminderDifficultMarkers
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class AndroidPackageFilterTest {

    private class InMemoryDifficultMarkers : AndroidVocabularyReminderDifficultMarkers {
        private val marked = ConcurrentHashMap.newKeySet<ContentId>()
        override fun isMarked(contentId: ContentId): Boolean = marked.contains(contentId)
        override fun markedContentIds(): Set<ContentId> = marked.toSet()
        override fun toggle(contentId: ContentId): Boolean {
            return if (marked.contains(contentId)) {
                marked.remove(contentId)
                false
            } else {
                marked.add(contentId)
                true
            }
        }
        override fun setMarked(contentId: ContentId, markedVal: Boolean): Boolean {
            if (markedVal) marked.add(contentId) else marked.remove(contentId)
            return markedVal
        }
    }

    private val testZone = ZoneId.of("UTC")
    // Reference time: 2026-08-20T12:00:00Z
    private val nowEpoch = 1787227200000L
    // Start of today: 2026-08-20T00:00:00Z = 1787184000000L
    private val startOfToday = 1787184000000L

    private fun createMemoryState(
        learningItemId: String,
        stage: LearningStage,
        dueAtEpoch: Long,
        learnerId: LearnerId = LearnerId("default-learner")
    ): MemoryState = MemoryState(
        learnerId = learnerId,
        learningItemId = LearningItemId(learningItemId),
        stage = stage,
        difficulty = 5.0,
        stabilityDays = 2.5,
        dueAt = Moment(dueAtEpoch),
        lastReviewedAt = if (stage == LearningStage.NEW) null else Moment(nowEpoch - 86400000L),
        reviewCount = if (stage == LearningStage.NEW) 0 else 3,
        lapseCount = 0
    )

    private fun setupTestEnvironment(
        itemsCount: Int = 10,
        packageIdStr: String = "pkg-test",
        existingContext: vn.loi.learning.infrastructure.LearningApplicationContext? = null
    ): Triple<vn.loi.learning.infrastructure.LearningApplicationContext, InstalledPackageId, InMemoryDifficultMarkers> {
        val ctx = existingContext ?: LearningApplicationFactory.createInMemory()
        val libraryId = ContentLibraryId("lib-$packageIdStr")
        val domainLibraryId = requireNotNull(ctx.defaultLibraryId)
        val packageId = PackageId(packageIdStr)
        val installedId = InstalledPackageId(packageIdStr)

        val contentIds = (1..itemsCount).map { ContentId("c-$packageIdStr-$it") }
        contentIds.forEachIndexed { idx, cid ->
            val num = idx + 1
            val lessonName = if (num <= 5) "Unit 1" else "Unit 2"
            ctx.contentRepository!!.save(
                Content(
                    id = cid,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "word$num",
                        translatedText = "answer$num",
                        pronunciation = "/wɜːd$num/",
                        exampleText = "example $num",
                        exampleTranslation = "ví dụ $num"
                    ),
                    metadata = ContentMetadata(lesson = lessonName)
                )
            )
            val learningItem = LearningItem(
                id = LearningItemId("item-$packageIdStr-$num"),
                contentId = cid,
                mode = LearningMode.MEANING_RECOGNITION
            )
            ctx.learningItemRepository!!.save(learningItem)
        }

        ctx.contentLibraryRepository!!.save(ContentLibrary(libraryId, LibraryDescriptor("Library $packageIdStr"), contentIds.toSet()))
        ctx.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor("Package $packageIdStr", "1.0.0", "OPD3"), setOf(libraryId)))
        ctx.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                installedId, domainLibraryId, packageId, TopicId("topic-1"),
                PackageName("Package $packageIdStr"), PackageVersion("1.0.0"), PackageState.ACTIVE, Instant.EPOCH, itemsCount, 0
            )
        )
        val libRepo = requireNotNull(ctx.domainLibraryRepository)
        libRepo.save(requireNotNull(libRepo.findById(domainLibraryId)).registerEntry(installedId, packageId, Instant.EPOCH))

        val markers = InMemoryDifficultMarkers()
        return Triple(ctx, installedId, markers)
    }

    @Test
    fun `1 All returns all items`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(10)
        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })

        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.ALL))
        )
        assertEquals(10, state.allRows.size)
        assertEquals(10, state.visibleRows.size)
    }

    @Test
    fun `2 NEW filter returns only items with NEW stage or no memory state`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(6)
        // c-1: no memory state -> NEW
        // c-2: MemoryState stage NEW -> NEW
        // c-3: MemoryState stage LEARNING
        // c-4: MemoryState stage REVIEW (future)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.NEW, nowEpoch + 100000))
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-3", LearningStage.LEARNING, nowEpoch + 100000))
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-4", LearningStage.REVIEW, nowEpoch + 100000))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.NEW))
        )
        // items 1, 2, 5, 6 are NEW
        assertEquals(4, state.visibleRows.size)
        assertTrue(state.visibleRows.all { it.fsrsStatus == AndroidContentFsrsStatus.NEW })
    }

    @Test
    fun `3 LEARNING filter returns LEARNING and RELEARNING items`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(4)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.LEARNING, nowEpoch + 1000))
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.RELEARNING, nowEpoch + 1000))
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-3", LearningStage.REVIEW, nowEpoch + 100000))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.LEARNING))
        )
        assertEquals(2, state.visibleRows.size)
        assertTrue(state.visibleRows.all { it.fsrsStatus == AndroidContentFsrsStatus.LEARNING })
    }

    @Test
    fun `4 REVIEW filter returns REVIEW stage items independently of due time`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(4)
        // c-1: future review (due tomorrow)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.REVIEW, nowEpoch + 86400000L))
        // c-2: due today
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.REVIEW, nowEpoch - 1000L))
        // c-3: overdue
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-3", LearningStage.REVIEW, startOfToday - 86400000L))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.REVIEW))
        )
        assertEquals(
            setOf("c-pkg-test-1", "c-pkg-test-2", "c-pkg-test-3"),
            state.visibleRows.map { it.contentId }.toSet()
        )
        assertEquals(AndroidContentFsrsStatus.REVIEW, state.visibleRows.first().fsrsStatus)
    }

    @Test
    fun `5 DUE filter includes items due today and overdue items`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        // c-1: due today (after startOfToday but <= nowEpoch)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.REVIEW, startOfToday + 3600000L))
        // c-2: overdue (before startOfToday)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.REVIEW, startOfToday - 86400000L))
        // c-3: future review
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-3", LearningStage.REVIEW, nowEpoch + 86400000L))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE))
        )
        assertEquals(2, state.visibleRows.size)
        val ids = state.visibleRows.map { it.contentId }.toSet()
        assertEquals(setOf("c-pkg-test-1", "c-pkg-test-2"), ids)
    }

    @Test
    fun `6 OVERDUE filter returns only items due before start of today`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        // c-1: due today (after startOfToday but <= nowEpoch)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.REVIEW, startOfToday + 3600000L))
        // c-2: overdue (before startOfToday)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.REVIEW, startOfToday - 86400000L))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.OVERDUE))
        )
        assertEquals(1, state.visibleRows.size)
        assertEquals("c-pkg-test-2", state.visibleRows[0].contentId)
        assertEquals(AndroidContentFsrsStatus.OVERDUE, state.visibleRows[0].fsrsStatus)
    }

    @Test
    fun `7 Difficult filter returns only marked items`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        markers.setMarked(ContentId("c-pkg-test-2"), true)
        markers.setMarked(ContentId("c-pkg-test-4"), true)

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(difficultOnly = true))
        )
        assertEquals(2, state.visibleRows.size)
        val ids = state.visibleRows.map { it.contentId }.toSet()
        assertEquals(setOf("c-pkg-test-2", "c-pkg-test-4"), ids)
    }

    @Test
    fun `8 Due plus Difficult composition returns intersection`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        // c-1: Due + Difficult -> MATCH
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.REVIEW, nowEpoch - 1000L))
        markers.setMarked(ContentId("c-pkg-test-1"), true)

        // c-2: Due + Not Difficult -> NO MATCH
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.REVIEW, nowEpoch - 1000L))

        // c-3: Future + Difficult -> NO MATCH
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-3", LearningStage.REVIEW, nowEpoch + 86400000L))
        markers.setMarked(ContentId("c-pkg-test-3"), true)

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE, difficultOnly = true))
        )
        assertEquals(1, state.visibleRows.size)
        assertEquals("c-pkg-test-1", state.visibleRows[0].contentId)
    }

    @Test
    fun `9 Lesson plus Due composition`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(8)
        // c-1 (Unit 1): Due -> MATCH
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.REVIEW, nowEpoch - 1000L))
        // c-6 (Unit 2): Due -> NO MATCH (wrong lesson)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-6", LearningStage.REVIEW, nowEpoch - 1000L))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE, selectedLesson = "Unit 1"))
        )
        assertEquals(1, state.visibleRows.size)
        assertEquals("c-pkg-test-1", state.visibleRows[0].contentId)
    }

    @Test
    fun `10 Search plus Due composition`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        // c-1 (word1): Due -> MATCH
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.REVIEW, nowEpoch - 1000L))
        // c-2 (word2): Due -> NO MATCH (search word1)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.REVIEW, nowEpoch - 1000L))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(query = "word1", fsrsFilter = AndroidFsrsFilter.DUE))
        )
        assertEquals(1, state.visibleRows.size)
        assertEquals("c-pkg-test-1", state.visibleRows[0].contentId)
    }

    @Test
    fun `11 Search plus Difficult composition`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        markers.setMarked(ContentId("c-pkg-test-1"), true)
        markers.setMarked(ContentId("c-pkg-test-2"), true)

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(query = "word2", difficultOnly = true))
        )
        assertEquals(1, state.visibleRows.size)
        assertEquals("c-pkg-test-2", state.visibleRows[0].contentId)
    }

    @Test
    fun `12 Lesson plus Search plus Due plus Difficult full composition`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(10)
        // c-1 (Unit 1, word1): Due + Difficult -> MATCH
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.REVIEW, nowEpoch - 1000L))
        markers.setMarked(ContentId("c-pkg-test-1"), true)

        // c-6 (Unit 2, word6): Due + Difficult -> NO MATCH (wrong lesson)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-6", LearningStage.REVIEW, nowEpoch - 1000L))
        markers.setMarked(ContentId("c-pkg-test-6"), true)

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(
                pkgId,
                AndroidPackageFilterSpec(
                    query = "word",
                    fsrsFilter = AndroidFsrsFilter.DUE,
                    difficultOnly = true,
                    selectedLesson = "Unit 1"
                )
            )
        )
        assertEquals(1, state.visibleRows.size)
        assertEquals("c-pkg-test-1", state.visibleRows[0].contentId)
    }

    @Test
    fun `13 No MemoryState rule treats item as NEW`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(3)
        // Zero memory states saved
        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(facade.openPackage(pkgId))
        assertTrue(state.allRows.all { it.fsrsStatus == AndroidContentFsrsStatus.NEW })
    }

    @Test
    fun `14 RELEARNING mapping to LEARNING status`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(2)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.RELEARNING, nowEpoch + 1000))
        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(facade.openPackage(pkgId))
        assertEquals(AndroidContentFsrsStatus.LEARNING, state.allRows[0].fsrsStatus)
    }

    @Test
    fun `15 MASTERED mapping to REVIEW or DUE`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(2)
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-1", LearningStage.MASTERED, nowEpoch + 86400000L))
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-test-2", LearningStage.MASTERED, nowEpoch - 1000L))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(facade.openPackage(pkgId))
        assertEquals(AndroidContentFsrsStatus.REVIEW, state.allRows[0].fsrsStatus)
        assertEquals(AndroidContentFsrsStatus.DUE, state.allRows[1].fsrsStatus)
    }

    @Test
    fun `16 Package A and B isolation`() {
        val (ctx, pkgA, markers) = setupTestEnvironment(4, "pkg-a")
        setupTestEnvironment(4, "pkg-b", ctx)
        // Save due state for pkg-a item 1 and pkg-b item 1
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-a-1", LearningStage.REVIEW, nowEpoch - 1000L))
        ctx.memoryStateRepository!!.save(createMemoryState("item-pkg-b-1", LearningStage.REVIEW, nowEpoch - 1000L))

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val stateA = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgA, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE))
        )
        assertEquals(1, stateA.visibleRows.size)
        assertEquals("c-pkg-a-1", stateA.visibleRows[0].contentId)
    }

    @Test
    fun `17 Unmarking difficult item while Difficult filter active removes it from visible rows`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(4)
        markers.setMarked(ContentId("c-pkg-test-1"), true)
        markers.setMarked(ContentId("c-pkg-test-2"), true)

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(difficultOnly = true))
        )
        assertEquals(2, state.visibleRows.size)

        // Toggle difficult for item 1
        val isNowDifficult = facade.toggleDifficult(ContentId("c-pkg-test-1"))
        assertFalse(isNowDifficult)

        val updatedAll = state.allRows.map { row ->
            if (row.contentId == "c-pkg-test-1") row.copy(isDifficult = isNowDifficult) else row
        }
        val updatedVisible = facade.applyFilters(updatedAll, state.filterSpec)
        assertEquals(1, updatedVisible.size)
        assertEquals("c-pkg-test-2", updatedVisible[0].contentId)
    }

    @Test
    fun `18 Difficult toggle creates zero ReviewEvents`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(3)
        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val eventsBefore = ctx.reviewEventRepository?.findAll(LearnerId("default-learner")).orEmpty().size

        facade.toggleDifficult(ContentId("c-pkg-test-1"))
        val eventsAfter = ctx.reviewEventRepository?.findAll(LearnerId("default-learner")).orEmpty().size
        assertEquals(eventsBefore, eventsAfter)
    }

    @Test
    fun `19 Filter query creates zero MemoryState writes`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val statesBefore = ctx.memoryStateRepository?.findAll().orEmpty().size

        facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE, difficultOnly = true))
        val statesAfter = ctx.memoryStateRepository?.findAll().orEmpty().size
        assertEquals(statesBefore, statesAfter)
    }

    @Test
    fun `20 Empty result state when no items match filters`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(5)
        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE))
        )
        assertEquals(5, state.allRows.size)
        assertEquals(0, state.visibleRows.size)
        assertTrue(state.filterSpec.isFiltered)
    }

    @Test
    fun `21 Time refresh classification changes status when time advances`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(2)
        // Item due at nowEpoch + 1000 (future -> REVIEW)
        val memState = createMemoryState("item-pkg-test-1", LearningStage.REVIEW, nowEpoch + 1000)
        ctx.memoryStateRepository!!.save(memState)

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        assertEquals(AndroidContentFsrsStatus.REVIEW, facade.resolveFsrsStatus(memState, nowEpoch, startOfToday))

        // When time advances past due time -> DUE
        val advancedTime = nowEpoch + 5000
        assertEquals(AndroidContentFsrsStatus.DUE, facade.resolveFsrsStatus(memState, advancedTime, startOfToday))
    }

    @Test
    fun `22 Stress test with large dataset computes filter in linear time`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(1000, "large-pkg")
        // Mark 200 items as difficult
        (1..200).forEach { markers.setMarked(ContentId("c-large-pkg-$it"), true) }
        // Set 300 items as Due
        (1..300).forEach {
            ctx.memoryStateRepository!!.save(
                createMemoryState("item-large-pkg-$it", LearningStage.REVIEW, nowEpoch - 1000)
            )
        }

        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
        val state = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(
                pkgId,
                AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE, difficultOnly = true)
            )
        )
        assertEquals(1000, state.allRows.size)
        // Intersection of first 200 (difficult) and first 300 (due) = 200 items
        assertEquals(200, state.visibleRows.size)
    }

    @Test
    fun `23 authoritative mode uses meaning recognition without arbitrary fallback`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(1)
        val contentId = ContentId("c-pkg-test-1")
        ctx.learningItemRepository!!.save(
            LearningItem(LearningItemId("item-unrelated"), contentId, LearningMode.MEANING_RECALL)
        )
        ctx.memoryStateRepository!!.save(
            createMemoryState("item-unrelated", LearningStage.REVIEW, nowEpoch - 1000L)
        )
        ctx.memoryStateRepository!!.save(
            createMemoryState("item-pkg-test-1", LearningStage.REVIEW, nowEpoch + 86400000L)
        )

        val state = assertIs<AndroidPackageContentState.Content>(
            AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })
                .openPackage(pkgId)
        )
        assertEquals(AndroidContentFsrsStatus.REVIEW, state.allRows.single().fsrsStatus)
    }

    @Test
    fun `24 media filter composes with Due`() {
        val rows = listOf(
            row("due-missing", "one", hasAudio = false, status = AndroidContentFsrsStatus.DUE),
            row("due-audio", "two", hasAudio = true, status = AndroidContentFsrsStatus.DUE),
            row("new-missing", "three", hasAudio = false, status = AndroidContentFsrsStatus.NEW)
        )
        val facade = AndroidPackageFacade(LearningApplicationFactory.createInMemory())

        val visible = facade.applyFilters(
            rows,
            AndroidPackageFilterSpec(
                fsrsFilter = AndroidFsrsFilter.DUE,
                mediaFilter = BrowserMediaFilter.MISSING_AUDIO
            )
        )

        assertEquals(listOf("due-missing"), visible.map { it.contentId })
    }

    @Test
    fun `25 due learning item follows Study due semantics while retaining learning stage`() {
        val (ctx, pkgId, markers) = setupTestEnvironment(2)
        ctx.memoryStateRepository!!.save(
            createMemoryState("item-pkg-test-1", LearningStage.LEARNING, nowEpoch - 1000L)
        )
        val facade = AndroidPackageFacade(ctx, difficultMarkers = markers, now = { nowEpoch }, zoneId = { testZone })

        val due = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.DUE))
        )
        val learning = assertIs<AndroidPackageContentState.Content>(
            facade.openPackage(pkgId, AndroidPackageFilterSpec(fsrsFilter = AndroidFsrsFilter.LEARNING))
        )

        assertEquals(listOf("c-pkg-test-1"), due.visibleRows.map { it.contentId })
        assertEquals(listOf("c-pkg-test-1"), learning.visibleRows.map { it.contentId })
    }

    private fun row(
        id: String,
        question: String,
        hasAudio: Boolean,
        status: AndroidContentFsrsStatus
    ) = AndroidPackageContentRow(
        contentId = id,
        question = question,
        answer = question,
        lesson = "Lesson",
        group = null,
        section = null,
        pronunciation = "",
        partOfSpeech = "",
        hasImage = false,
        hasAudio = hasAudio,
        imageRef = null,
        audioRef = null,
        index = 1,
        searchableText = question,
        fsrsStatus = status
    )
}
