package vn.loi.learning.android.study

import java.time.Instant
import kotlin.test.*
import org.junit.Test
import vn.loi.learning.android.autoplay.AutoPlayContentSelector
import vn.loi.learning.android.autoplay.AutoPlayEngine
import vn.loi.learning.android.autoplay.AutoPlayTimingStateMachineTest
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.study.DailyStudyBudgetLimits
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ActiveStudySessionAutoPlayAuthorityTest {

    private val testTimestamp = 1_700_000_000_000L

    private fun install(
        context: vn.loi.learning.infrastructure.LearningApplicationContext,
        name: String,
        count: Int
    ): InstalledPackageId {
        val contentIds = (0 until count).mapTo(linkedSetOf()) { index ->
            val suffix = "-$index"
            val contentId = ContentId("$name-content$suffix")
            context.contentRepository!!.save(
                Content(contentId, ContentType.WORD, ContentText("$name$suffix", "$name-answer$suffix"))
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
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                installedId, libraryId, packageId, TopicId("$name-topic"), PackageName(name), PackageVersion("1.0.0"),
                PackageState.ACTIVE, Instant.EPOCH, count, count
            )
        )
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, Instant.EPOCH))
        return installedId
    }

    @Test
    fun `1 - Quick Review 19 items in a 100-item package yields exactly 19 in Auto Play`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-qr", count = 100)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 19 learned items
        for (i in 0 until 19) {
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-qr-$i"), learner, LearningItemId("pkg-qr-item-$i"), ReviewRating.GOOD, Moment(testTimestamp - 100_000 + i))
            )
        }

        val facade = AndroidStudyFacade(context, learner, now = { testTimestamp })
        val qrState = facade.start(AndroidSessionEntry.QUICK_REVIEW)
        assertIs<AndroidStudyState.Introduction>(qrState)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        assertEquals(19, autoPlayIds.size, "Quick Review must produce exactly 19 Auto Play items, never all 100 package items")
        val expectedIds = (0 until 19).map { ContentId("pkg-qr-content-$it") }
        assertEquals(expectedIds, autoPlayIds)
    }

    @Test
    fun `2 - Again Hard membership 6 in a 100-item package yields exactly 6 in Auto Play`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-diff", count = 100)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 20 learned items, of which 6 are rated AGAIN
        for (i in 0 until 20) {
            val rating = if (i < 6) ReviewRating.AGAIN else ReviewRating.GOOD
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-diff-$i"), learner, LearningItemId("pkg-diff-item-$i"), rating, Moment(testTimestamp - 100_000 + i))
            )
        }

        val facade = AndroidStudyFacade(context, learner, now = { testTimestamp })
        val diffState = facade.start(AndroidSessionEntry.DIFFICULT)
        assertIs<AndroidStudyState.Introduction>(diffState)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        assertEquals(6, autoPlayIds.size, "Again/Hard must produce exactly 6 Auto Play items")
        val expectedIds = (0 until 6).map { ContentId("pkg-diff-content-$it") }
        assertEquals(expectedIds, autoPlayIds)
    }

    @Test
    fun `3 - Learn New planned queue 50 in a 100-item package yields exactly 50 in Auto Play`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-new", count = 100)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        val facade = AndroidStudyFacade(
            context,
            learner,
            now = { testTimestamp },
            dailyLimits = { DailyStudyBudgetLimits(newPerDay = 50, reviewPerDay = 10) }
        )
        val newState = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        assertIs<AndroidStudyState.Introduction>(newState)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        assertEquals(50, autoPlayIds.size, "Learn New must produce exactly 50 Auto Play items matching planned quota")
    }

    @Test
    fun `4 - New=1 Review=100 with actual effective queue smaller than target yields exact effective membership`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-effective", count = 100)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 5 due review items
        for (i in 0 until 5) {
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-eff-$i"), learner, LearningItemId("pkg-effective-item-$i"), ReviewRating.GOOD, Moment(1_000))
            )
        }

        val facade = AndroidStudyFacade(
            context,
            learner,
            now = { testTimestamp }, // Far in future so all 5 are due
            dailyLimits = { DailyStudyBudgetLimits(newPerDay = 1, reviewPerDay = 100) },
            continuousSkimEnabled = { true }
        )
        val adaptiveState = facade.start(AndroidSessionEntry.REVIEW, StudyMode.ADAPTIVE)
        assertIs<AndroidStudyState.Runtime>(adaptiveState)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        // 1 new + 5 due reviews = 6 items
        assertEquals(6, autoPlayIds.size)
    }

    @Test
    fun `5 - Adaptive active queue N yields exactly N in Auto Play`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-adapt", count = 50)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 8 due review items
        for (i in 0 until 8) {
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-ad-$i"), learner, LearningItemId("pkg-adapt-item-$i"), ReviewRating.GOOD, Moment(1_000))
            )
        }

        val facade = AndroidStudyFacade(
            context,
            learner,
            now = { testTimestamp },
            dailyLimits = { DailyStudyBudgetLimits(newPerDay = 5, reviewPerDay = 10) },
            continuousSkimEnabled = { true }
        )
        val adaptive = facade.start(AndroidSessionEntry.REVIEW, StudyMode.ADAPTIVE)
        assertIs<AndroidStudyState.Runtime>(adaptive)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        // 5 new + 8 reviews = 13 items
        assertEquals(13, autoPlayIds.size)
    }

    @Test
    fun `6 - Typing active session N yields exactly N in Auto Play`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-type", count = 30)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 7 due review items
        for (i in 0 until 7) {
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-type-$i"), learner, LearningItemId("pkg-type-item-$i"), ReviewRating.GOOD, Moment(1_000))
            )
        }

        val facade = AndroidStudyFacade(
            context,
            learner,
            now = { testTimestamp },
            dailyLimits = { DailyStudyBudgetLimits(newPerDay = 1, reviewPerDay = 20) }
        )
        val typing = facade.start(AndroidSessionEntry.REVIEW, StudyMode.TYPING)
        assertIs<AndroidStudyState.Typing>(typing)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        assertEquals(7, autoPlayIds.size)
    }

    @Test
    fun `7 - Latest Session review N yields exactly N in Auto Play`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-latest", count = 40)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        val facade = AndroidStudyFacade(
            context,
            learner,
            now = { testTimestamp },
            dailyLimits = { DailyStudyBudgetLimits(newPerDay = 10, reviewPerDay = 10) }
        )
        // Complete a session of 10 new items
        var state: AndroidStudyState = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        for (i in 0 until 10) {
            val intro = assertIs<AndroidStudyState.Introduction>(state)
            val revealed = facade.revealIntroduction(intro)
            state = facade.rateIntroduction(revealed as AndroidStudyState.Introduction, ReviewRating.GOOD)
        }
        assertIs<AndroidStudyState.Completion>(state)

        // Now start Latest Session practice
        val latestPractice = facade.start(AndroidSessionEntry.LATEST_SESSION)
        assertIs<AndroidStudyState.Runtime>(latestPractice)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        assertEquals(10, autoPlayIds.size)
    }

    @Test
    fun `8 - Learned active review N yields exactly N in Auto Play`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-learned", count = 50)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 25 learned items
        for (i in 0 until 25) {
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-ln-$i"), learner, LearningItemId("pkg-learned-item-$i"), ReviewRating.GOOD, Moment(testTimestamp - 100_000 + i))
            )
        }

        val facade = AndroidStudyFacade(context, learner, now = { testTimestamp })
        val learnedState = facade.start(AndroidSessionEntry.LEARNED)
        assertIs<AndroidStudyState.Runtime>(learnedState)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        assertEquals(25, autoPlayIds.size)
    }

    @Test
    fun `9 - Missing or stale session returns empty list, NEVER falls back to whole package`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-empty", count = 100)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        val facade = AndroidStudyFacade(context, learner, now = { testTimestamp })
        facade.home()

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        assertTrue(autoPlayIds.isEmpty(), "Must be empty when no session is active; never 100 package items")
    }

    @Test
    fun `10 - ReviewHub direct entry resolution yields exact session items`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-hub", count = 100)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 15 learned items, 4 of which are Again
        for (i in 0 until 15) {
            val rating = if (i < 4) ReviewRating.AGAIN else ReviewRating.GOOD
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-hub-$i"), learner, LearningItemId("pkg-hub-item-$i"), rating, Moment(testTimestamp - 100_000 + i))
            )
        }

        val facade = AndroidStudyFacade(context, learner, now = { testTimestamp })
        facade.home()

        val qrIds = facade.resolveReviewEntryAutoPlayContentIds(AndroidSessionEntry.QUICK_REVIEW)
        assertEquals(15, qrIds.size, "Quick Review resolution must be 15, not 100")

        val diffIds = facade.resolveReviewEntryAutoPlayContentIds(AndroidSessionEntry.DIFFICULT)
        assertEquals(4, diffIds.size, "Difficult resolution must be 4, not 100")

        val learnedIds = facade.resolveReviewEntryAutoPlayContentIds(AndroidSessionEntry.LEARNED)
        assertEquals(15, learnedIds.size, "Learned resolution must be 15, not 100")
    }

    @Test
    fun `11 - Active session ordering is preserved deterministically`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-order", count = 30)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        for (i in 0 until 10) {
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-ord-$i"), learner, LearningItemId("pkg-order-item-$i"), ReviewRating.GOOD, Moment(testTimestamp - 100_000 + i))
            )
        }

        val facade = AndroidStudyFacade(context, learner, now = { testTimestamp })
        facade.start(AndroidSessionEntry.QUICK_REVIEW)

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        val expected = (0 until 10).map { ContentId("pkg-order-content-$it") }
        assertEquals(expected, autoPlayIds)
    }

    @Test
    fun `12 - Auto Play resolution and playback causes ZERO FSRS mutation, ZERO ReviewEvents, and ZERO quota consumption`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = install(context, "pkg-zero-mut", count = 50)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        for (i in 0 until 10) {
            context.engine.review(
                ReviewCommand(ReviewEventId("seed-zm-$i"), learner, LearningItemId("pkg-zero-mut-item-$i"), ReviewRating.GOOD, Moment(testTimestamp - 100_000 + i))
            )
        }

        val facade = AndroidStudyFacade(
            context,
            learner,
            now = { testTimestamp },
            dailyLimits = { DailyStudyBudgetLimits(newPerDay = 10, reviewPerDay = 20) }
        )
        facade.start(AndroidSessionEntry.QUICK_REVIEW)

        val memoryBefore = context.memoryStateRepository!!.findAll().filter { it.learnerId == learner }
        val eventsBefore = context.reviewEventRepository!!.findAll(learner)
        val sessionsBefore = context.studySessionRepository!!.findAll()

        val autoPlayIds = facade.activeStudySessionAutoPlayContentIds()
        val selector = AutoPlayContentSelector(context, learner, now = { testTimestamp })
        val items = selector.selectItemsForContentIds(autoPlayIds)
        assertEquals(10, items.size)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)
        engine.start(items, vn.loi.learning.android.autoplay.AutoPlayConfig())
        scheduler.fireNext()
        engine.next()
        engine.previous()
        engine.stop()

        val memoryAfter = context.memoryStateRepository!!.findAll().filter { it.learnerId == learner }
        val eventsAfter = context.reviewEventRepository!!.findAll(learner)
        val sessionsAfter = context.studySessionRepository!!.findAll()

        assertEquals(memoryBefore, memoryAfter)
        assertEquals(eventsBefore, eventsAfter)
        assertEquals(sessionsBefore, sessionsAfter)
    }
}
