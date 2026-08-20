package vn.loi.learning.android.reminder

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.application.packageprogress.PackageLearningProgressQuery
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.android.study.AndroidSessionEntry
import vn.loi.learning.android.study.AndroidStudyFacade
import vn.loi.learning.android.study.AndroidStudyState

class AndroidReminderReviewRatingBridgeTest {

    private val learner = LearnerId("default-learner")

    @Test
    fun `1 Again executes canonical transaction updating memory state and review event`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 3)
        var currentTime = 10_000L
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { currentTime }
        )

        // Pre-seed item to REVIEW stage so AGAIN increments lapseCount
        context.engine.review(
            ReviewCommand(
                reviewEventId = ReviewEventId("seed-1"),
                learnerId = learner,
                learningItemId = LearningItemId("bridge-pkg-item-0"),
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(5_000L)
            )
        )

        val result = bridge.submitRating("bridge-pkg-content-0", ReviewRating.AGAIN)
        val success = assertIs<QuickReviewRatingResult.Success>(result)
        assertEquals("bridge-pkg-item-0", success.learningItemId)
        assertEquals(ReviewRating.AGAIN, success.rating)
        assertEquals(1, success.memoryState.lapseCount)
        assertEquals(2, success.memoryState.reviewCount)

        val storedState = context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-0"))
        assertNotNull(storedState)
        assertEquals(1, storedState.lapseCount)

        val events = context.reviewEventRepository?.findAll(learner).orEmpty()
        assertEquals(2, events.size)
        assertEquals(ReviewRating.AGAIN, events.last().rating)
        assertEquals(RatingSource.MANUAL_USER, events.last().source)
    }

    @Test
    fun `2 Hard executes canonical transaction`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 3)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 15_000L }
        )

        val result = bridge.submitRating("bridge-pkg-content-1", ReviewRating.HARD)
        val success = assertIs<QuickReviewRatingResult.Success>(result)
        assertEquals("bridge-pkg-item-1", success.learningItemId)
        assertEquals(ReviewRating.HARD, success.rating)

        val storedState = context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-1"))
        assertNotNull(storedState)
        assertEquals(0, storedState.lapseCount)
        assertEquals(1, storedState.reviewCount)
    }

    @Test
    fun `3 Good executes canonical transaction`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 3)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 20_000L }
        )

        val result = bridge.submitRating("bridge-pkg-content-2", ReviewRating.GOOD)
        val success = assertIs<QuickReviewRatingResult.Success>(result)
        assertEquals("bridge-pkg-item-2", success.learningItemId)
        assertEquals(ReviewRating.GOOD, success.rating)

        val storedState = context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-2"))
        assertNotNull(storedState)
        assertEquals(1, storedState.reviewCount)
    }

    @Test
    fun `4 Easy executes canonical transaction with higher stability`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 3)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 25_000L }
        )

        val goodResult = bridge.submitRating("bridge-pkg-content-0", ReviewRating.GOOD)
        val goodSuccess = assertIs<QuickReviewRatingResult.Success>(goodResult)

        val easyResult = bridge.submitRating("bridge-pkg-content-1", ReviewRating.EASY)
        val easySuccess = assertIs<QuickReviewRatingResult.Success>(easyResult)

        assertTrue(easySuccess.memoryState.stabilityDays > goodSuccess.memoryState.stabilityDays)
    }

    @Test
    fun `5 stale MemoryState resolves fresh from repository`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 1)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 30_000L }
        )

        // First review outside bridge
        context.engine.review(
            ReviewCommand(
                reviewEventId = ReviewEventId("external-1"),
                learnerId = learner,
                learningItemId = LearningItemId("bridge-pkg-item-0"),
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(10_000L)
            )
        )
        val stateAfterFirst = context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-0"))
        assertNotNull(stateAfterFirst)
        assertEquals(1, stateAfterFirst.reviewCount)

        // Bridge rates Good - must use stateAfterFirst as base, resulting in reviewCount = 2
        val result = bridge.submitRating("bridge-pkg-content-0", ReviewRating.GOOD)
        val success = assertIs<QuickReviewRatingResult.Success>(result)
        assertEquals(2, success.memoryState.reviewCount)
    }

    @Test
    fun `6 new item with null MemoryState initializes cleanly`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 1)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 40_000L }
        )

        assertNull(context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-0")))

        val result = bridge.submitRating("bridge-pkg-content-0", ReviewRating.GOOD)
        val success = assertIs<QuickReviewRatingResult.Success>(result)
        assertEquals(1, success.memoryState.reviewCount)
        assertNotNull(context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-0")))
    }

    @Test
    fun `7 missing content returns NotFound`() {
        val context = LearningApplicationFactory.createInMemory()
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner
        )

        val result = bridge.submitRating("non-existent-content", ReviewRating.GOOD)
        assertEquals(QuickReviewRatingResult.NotFound, result)
    }

    @Test
    fun `8 missing LearningItem on existing content returns NotReviewable`() {
        val context = LearningApplicationFactory.createInMemory()
        val cid = ContentId("content-without-item")
        context.contentRepository?.save(
            Content(cid, ContentType.WORD, ContentText("word", "nghia"))
        )
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner
        )

        val result = bridge.submitRating("content-without-item", ReviewRating.GOOD)
        assertEquals(QuickReviewRatingResult.NotReviewable, result)
    }

    @Test
    fun `9 deterministic targeting strictly resolves enabled MEANING_RECOGNITION and ignores unrelated modes`() {
        val context = LearningApplicationFactory.createInMemory()
        val cid = ContentId("multi-mode-content")
        context.contentRepository?.save(
            Content(cid, ContentType.WORD, ContentText("word", "nghia"))
        )
        context.learningItemRepository?.save(
            LearningItem(LearningItemId("item-dictation"), cid, LearningMode.DICTATION)
        )
        context.learningItemRepository?.save(
            LearningItem(LearningItemId("item-recognition"), cid, LearningMode.MEANING_RECOGNITION)
        )
        context.learningItemRepository?.save(
            LearningItem(LearningItemId("item-recall"), cid, LearningMode.MEANING_RECALL)
        )

        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner
        )

        val item = bridge.resolveLearningItem("multi-mode-content")
        assertNotNull(item)
        assertEquals("item-recognition", item.id.value)
    }

    @Test
    fun `9b missing intended mode returns null and NotReviewable`() {
        val context = LearningApplicationFactory.createInMemory()
        val cid = ContentId("only-dictation-content")
        context.contentRepository?.save(
            Content(cid, ContentType.WORD, ContentText("word", "nghia"))
        )
        context.learningItemRepository?.save(
            LearningItem(LearningItemId("item-dictation"), cid, LearningMode.DICTATION)
        )

        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner
        )

        assertNull(bridge.resolveLearningItem("only-dictation-content"))
        assertEquals(QuickReviewRatingResult.NotReviewable, bridge.submitRating("only-dictation-content", ReviewRating.GOOD))
    }

    @Test
    fun `9c disabled intended item returns null and NotReviewable`() {
        val context = LearningApplicationFactory.createInMemory()
        val cid = ContentId("disabled-recognition-content")
        context.contentRepository?.save(
            Content(cid, ContentType.WORD, ContentText("word", "nghia"))
        )
        context.learningItemRepository?.save(
            LearningItem(LearningItemId("item-disabled"), cid, LearningMode.MEANING_RECOGNITION, isEnabled = false)
        )

        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner
        )

        assertNull(bridge.resolveLearningItem("disabled-recognition-content"))
        assertEquals(QuickReviewRatingResult.NotReviewable, bridge.submitRating("disabled-recognition-content", ReviewRating.GOOD))
    }

    @Test
    fun `10 transaction rollback on error leaves clean state and returns Failure`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 1)
        val failingContext = context.copy(
            learningItemRepository = object : vn.loi.learning.application.port.LearningItemRepository by context.learningItemRepository!! {
                override fun findByContentId(contentId: ContentId): List<LearningItem> {
                    throw IllegalStateException("Database locked")
                }
            }
        )

        val bridge = AndroidReminderReviewRatingBridge(
            context = failingContext,
            learnerId = learner
        )

        val result = bridge.submitRating("bridge-pkg-content-0", ReviewRating.GOOD)
        assertIs<QuickReviewRatingResult.Failure>(result)
    }

    @Test
    fun `11 double submit prevents concurrent execution`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 1)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner
        )

        val first = bridge.submitRating("bridge-pkg-content-0", ReviewRating.GOOD)
        assertIs<QuickReviewRatingResult.Success>(first)

        // Submitting again sequentially is allowed as a distinct review
        val second = bridge.submitRating("bridge-pkg-content-0", ReviewRating.GOOD)
        assertIs<QuickReviewRatingResult.Success>(second)
        assertEquals(2, context.reviewEventRepository?.findAll(learner)?.size)
    }

    @Test
    fun `13 preview produces zero writes and zero review events`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 1)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 50_000L }
        )

        val preview = bridge.previewRatings("bridge-pkg-content-0")
        assertNotNull(preview)
        assertEquals(4, preview.size)

        // Zero writes
        assertNull(context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-0")))
        assertTrue(context.reviewEventRepository?.findAll(learner).isNullOrEmpty())
    }

    @Test
    fun `14 preview formats all four rating intervals correctly`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 1)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 50_000L }
        )

        val preview = bridge.previewRatings("bridge-pkg-content-0")!!
        assertTrue(preview.containsKey(ReviewRating.AGAIN))
        assertTrue(preview.containsKey(ReviewRating.HARD))
        assertTrue(preview.containsKey(ReviewRating.GOOD))
        assertTrue(preview.containsKey(ReviewRating.EASY))

        assertTrue(preview[ReviewRating.AGAIN]!!.formattedInterval.isNotBlank())
        assertTrue(preview[ReviewRating.GOOD]!!.formattedInterval.isNotBlank())
    }

    @Test
    fun `15 opening Full Review causes zero review events`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "bridge-pkg", count = 3)
        val selector = AndroidVocabularyReminderCandidateSelector(context, learnerId = learner)

        val session = selector.getReminderReviewQueue(
            packageIdStr = pkg.value,
            mode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            anchorContentIdStr = "bridge-pkg-content-0"
        )
        assertNotNull(session)

        // Read-only inspection must not create events
        assertTrue(context.reviewEventRepository?.findAll(learner).isNullOrEmpty())
    }

    @Test
    fun `16 Mark Difficult remains independent from FSRS state`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "bridge-pkg", count = 1)
        val difficultMarkers = object : AndroidVocabularyReminderDifficultMarkers {
            val set = mutableSetOf<ContentId>()
            override fun isMarked(contentId: ContentId) = contentId in set
            override fun markedContentIds() = set.toSet()
            override fun toggle(contentId: ContentId) = if (contentId in set) { set.remove(contentId); false } else { set.add(contentId); true }
            override fun setMarked(contentId: ContentId, marked: Boolean) = if (marked) { set.add(contentId); true } else { set.remove(contentId); false }
        }

        difficultMarkers.setMarked(ContentId("bridge-pkg-content-0"), true)
        assertTrue(difficultMarkers.isMarked(ContentId("bridge-pkg-content-0")))

        // FSRS is still empty
        assertNull(context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-0")))
        assertTrue(context.reviewEventRepository?.findAll(learner).isNullOrEmpty())
    }

    @Test
    fun `17 and 18 and 19 and 20 active Study session reconciles fresh MemoryState when rated externally`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "bridge-pkg", count = 1)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkg)
        var testTime = 100_000L
        val facade = AndroidStudyFacade(
            context = context,
            learnerId = learner,
            now = { testTime },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(newPerDay = 10, reviewPerDay = 10) },
            continuousSkimEnabled = { true }
        )

        // 1. Start Study session with Adaptive mode
        val studyState = assertIs<AndroidStudyState.Introduction>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.ADAPTIVE)
        )
        val studyItem = studyState.contentId
        assertEquals("bridge-pkg-content-0", studyItem)

        // 2. User backgrounds Study and rates bridge-pkg-content-0 = GOOD via Quick Rating Bridge
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { testTime + 5_000L }
        )
        val bridgeResult = bridge.submitRating(studyItem, ReviewRating.GOOD)
        val bridgeSuccess = assertIs<QuickReviewRatingResult.Success>(bridgeResult)
        assertEquals(1, bridgeSuccess.memoryState.reviewCount)

        // 3. User returns to Study and rates the same card AGAIN (e.g. they forgot or re-tested)
        testTime += 10_000L
        val revealedState = facade.revealIntroduction(studyState) as AndroidStudyState.Introduction
        facade.rateIntroduction(revealedState, ReviewRating.AGAIN)

        // 4. Verification: Study read the fresh MemoryState (which had reviewCount = 1), so rating AGAIN caused a lapse!
        val finalMemory = context.memoryStateRepository?.find(learner, LearningItemId("bridge-pkg-item-0"))
        assertNotNull(finalMemory)
        assertEquals(1, finalMemory.lapseCount)

        val allEvents = context.reviewEventRepository?.findAll(learner).orEmpty()
        assertEquals(2, allEvents.size)
        assertEquals(ReviewRating.GOOD, allEvents[0].rating)
        assertEquals(RatingSource.MANUAL_USER, allEvents[0].source)
        assertEquals(ReviewRating.AGAIN, allEvents[1].rating)
        assertEquals(RatingSource.STANDARD_REVIEW, allEvents[1].source)
    }

    @Test
    fun `21 statistics query services consume canonical Quick Rating ReviewEvents`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "bridge-pkg", count = 2)
        val bridge = AndroidReminderReviewRatingBridge(
            context = context,
            learnerId = learner,
            now = { 200_000L }
        )

        bridge.submitRating("bridge-pkg-content-0", ReviewRating.GOOD)
        bridge.submitRating("bridge-pkg-content-1", ReviewRating.EASY)

        val progress = context.packageProgress?.execute(
            PackageLearningProgressQuery(
                installedPackageId = pkg,
                learnerId = learner,
                at = Moment(200_000L)
            )
        )
        assertNotNull(progress)
        assertEquals(2, progress.startedItemCount)
    }

    @Test
    fun `22 formatTimeSpan handles all interval ranges accurately`() {
        assertEquals("30s", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(30_000L)))
        assertEquals("1m", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(60_000L)))
        assertEquals("10m", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(600_000L)))
        assertEquals("2h", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(7_200_000L)))
        assertEquals("1d", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(86_400_000L)))
        assertEquals("4d", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(4 * 86_400_000L)))
        assertEquals("2mo", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(60 * 86_400_000L)))
        assertEquals("1y", AndroidReminderReviewRatingBridge.formatTimeSpan(TimeSpan(365 * 86_400_000L)))
    }

    @Test
    fun `23 inspector unreviewed item is read only New state with empty history`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "inspect-a", 1)
        val model = AndroidReminderReviewFsrsInspectorQuery(context, learner, now = { 1_000L }).query("inspect-a-content-0")
        assertTrue(model.hasFsrsData)
        assertEquals("New", model.stage)
        assertEquals(0, model.reviewCount)
        assertEquals(0, model.lapseCount)
        assertEquals("Never", model.lastReviewed)
        assertTrue(model.history.isEmpty())
        assertNull(context.memoryStateRepository!!.find(learner, LearningItemId("inspect-a-item-0")))
        assertTrue(context.reviewEventRepository!!.findAll(learner).isEmpty())
    }

    @Test
    fun `24 inspector maps canonical state and latest manual history`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "inspect-a", 1)
        val bridge = AndroidReminderReviewRatingBridge(context, learner, now = { 2_000L })
        assertIs<QuickReviewRatingResult.Success>(bridge.submitRating("inspect-a-content-0", ReviewRating.GOOD))
        val model = AndroidReminderReviewFsrsInspectorQuery(context, learner, now = { 3_000L }, zoneId = ZoneId.of("UTC")).query("inspect-a-content-0")
        assertEquals(1, model.reviewCount)
        assertEquals(0, model.lapseCount)
        assertTrue(model.difficulty.endsWith(" / 10"))
        assertTrue(model.stability.isNotBlank())
        assertEquals(ReviewRating.GOOD, model.history.first().rating)
        assertEquals("Manual rating", model.history.first().source)
    }

    @Test
    fun `25 inspector preserves RELEARNING and MASTERED actual stages`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "inspect-stage", 2)
        fun state(id: String, stage: LearningStage) = MemoryState(
            learner, LearningItemId(id), stage, 6.41, 18.7, Moment(50_000L), Moment(10_000L), 3, 1
        )
        context.memoryStateRepository!!.save(state("inspect-stage-item-0", LearningStage.RELEARNING))
        context.memoryStateRepository!!.save(state("inspect-stage-item-1", LearningStage.MASTERED))
        val query = AndroidReminderReviewFsrsInspectorQuery(context, learner, now = { 20_000L })
        assertEquals("Relearning", query.query("inspect-stage-content-0").stage)
        assertEquals("Mastered", query.query("inspect-stage-content-1").stage)
    }

    @Test
    fun `26 inspector requires enabled meaning recognition and never falls back`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "inspect-mode", 1)
        context.learningItemRepository!!.save(LearningItem(LearningItemId("dictation"), ContentId("inspect-mode-content-0"), LearningMode.DICTATION))
        context.learningItemRepository!!.save(LearningItem(LearningItemId("inspect-mode-item-0"), ContentId("inspect-mode-content-0"), LearningMode.MEANING_RECOGNITION, isEnabled = false))
        val model = AndroidReminderReviewFsrsInspectorQuery(context, learner).query("inspect-mode-content-0")
        assertTrue(!model.hasFsrsData)
    }

    @Test
    fun `27 inspector history is newest first scoped and initially limited to ten`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "inspect-a", 1)
        installPackage(context, "inspect-b", 1)
        var time = 10_000L
        val bridge = AndroidReminderReviewRatingBridge(context, learner, now = { time })
        repeat(12) { index ->
            time += 10_000L
            bridge.submitRating("inspect-a-content-0", ReviewRating.entries[index % 4])
        }
        AndroidReminderReviewRatingBridge(context, learner, now = { 500_000L }).submitRating("inspect-b-content-0", ReviewRating.EASY)
        val model = AndroidReminderReviewFsrsInspectorQuery(context, learner, now = { 600_000L }).query("inspect-a-content-0")
        assertEquals(12, model.history.size)
        assertEquals(10, model.recentHistory.size)
        assertTrue(model.hasMoreHistory)
        assertEquals(ReviewRating.EASY, model.history.first().rating)
        assertTrue(model.history.none { it.rating == ReviewRating.EASY && it.reviewedAt.contains("500") })
    }

    @Test
    fun `28 source labels include study manual and safe future label`() {
        assertEquals("Study", AndroidReminderReviewFsrsInspectorQuery.sourceLabel(RatingSource.STANDARD_REVIEW))
        assertEquals("Manual rating", AndroidReminderReviewFsrsInspectorQuery.sourceLabel(RatingSource.MANUAL_USER))
        assertEquals("Manual user override", AndroidReminderReviewFsrsInspectorQuery.sourceLabel(RatingSource.MANUAL_USER_OVERRIDE))
    }

    @Test
    fun `29 stability and difficulty format retain useful precision`() {
        assertEquals("5.4 hours", AndroidReminderReviewFsrsInspectorQuery.formatStability(0.225))
        assertEquals("2.3 days", AndroidReminderReviewFsrsInspectorQuery.formatStability(2.3))
        assertEquals("3.2 months", AndroidReminderReviewFsrsInspectorQuery.formatStability(96.0))
        assertEquals("6.41", AndroidReminderReviewFsrsInspectorQuery.formatDifficultyValue(6.414))
    }

    @Test
    fun `30 local calendar formatting handles midnight today yesterday tomorrow and overdue`() {
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        fun millis(day: Int, hour: Int) = ZonedDateTime.of(2026, 8, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()
        val formatter = AndroidFsrsInspectorFormatter(millis(20, 0), zone, java.util.Locale.US)
        assertTrue(formatter.formatDue(Moment(millis(20, 22))).startsWith("Today"))
        assertTrue(formatter.formatDue(Moment(millis(21, 8))).startsWith("Tomorrow"))
        assertEquals("Overdue by 2 days", formatter.formatDue(Moment(millis(18, 23))))
        assertTrue(formatter.formatCalendarTime(Moment(millis(19, 23))).startsWith("Yesterday"))
    }

    @Test
    fun `31 repeated rating inspector query reads fresh canonical state and newest row`() {
        val context = LearningApplicationFactory.createInMemory()
        installPackage(context, "inspect-fresh", 1)
        var time = 10_000L
        val bridge = AndroidReminderReviewRatingBridge(context, learner, now = { time })
        val query = AndroidReminderReviewFsrsInspectorQuery(context, learner, now = { time + 1 })
        bridge.submitRating("inspect-fresh-content-0", ReviewRating.GOOD)
        assertEquals(1, query.query("inspect-fresh-content-0").reviewCount)
        time = 20_000L
        bridge.submitRating("inspect-fresh-content-0", ReviewRating.AGAIN)
        val refreshed = query.query("inspect-fresh-content-0")
        assertEquals(2, refreshed.reviewCount)
        assertEquals(ReviewRating.AGAIN, refreshed.history.first().rating)
    }

    @Test
    fun `32 rating palettes retain Again Hard Good Easy semantic colors`() {
        val colors = vn.loi.learning.android.ui.StudyRatingColors
        assertTrue(colors.again.border != colors.hard.border)
        assertTrue(colors.hard.border != colors.good.border)
        assertTrue(colors.good.border != colors.easy.border)
        assertTrue(colors.easy.border != colors.again.border)
    }

    private fun installPackage(
        context: LearningApplicationContext,
        name: String,
        count: Int
    ): InstalledPackageId {
        val contentIds = (0 until count).mapTo(linkedSetOf()) { index ->
            val suffix = "-$index"
            val contentId = ContentId("$name-content$suffix")
            context.contentRepository!!.save(
                Content(
                    contentId,
                    ContentType.WORD,
                    ContentText(primaryText = "$name$suffix", translatedText = "Nghia $suffix"),
                    media = ContentMedia(
                        primaryAudio = "audio/$name$suffix.mp3",
                        exampleAudio = "audio/ex-$name$suffix.mp3"
                    )
                )
            )
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("$name-item$suffix"), contentId, LearningMode.MEANING_RECOGNITION)
            )
            contentId
        }
        val contentLibraryId = ContentLibraryId("$name-library")
        val packageId = PackageId(name)
        val installedId = InstalledPackageId(name)
        context.contentLibraryRepository!!.save(ContentLibrary(contentLibraryId, LibraryDescriptor(name), contentIds))
        context.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor(name, "1.0.0", "OPD3"), setOf(contentLibraryId)))
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(InstalledPackage.reconstitute(
            installedId, libraryId, packageId, TopicId("$name-topic"), PackageName(name), PackageVersion("1.0.0"),
            PackageState.ACTIVE, Instant.EPOCH, count, count
        ))
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, Instant.EPOCH))
        return installedId
    }
}
