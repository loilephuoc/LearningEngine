package vn.loi.learning.android.autoplay

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AutoPlayPassiveZeroMutationTest {

    @Test
    fun `running auto play produces zero rating, zero ReviewEvent, and zero MemoryState mutation`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = installPackage(context, "test-pkg", count = 5)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // Seed 2 reviewed items (one GOOD, one AGAIN)
        val item1 = LearningItemId("test-pkg-item-0")
        val item2 = LearningItemId("test-pkg-item-1")
        context.engine.review(
            ReviewCommand(ReviewEventId("seed-1"), learner, item1, ReviewRating.GOOD, Moment(1_000))
        )
        context.engine.review(
            ReviewCommand(ReviewEventId("seed-2"), learner, item2, ReviewRating.AGAIN, Moment(2_000))
        )

        // Snapshot domain repositories BEFORE Auto Play
        val memoryStatesBefore = context.memoryStateRepository!!.findAll().filter { it.learnerId == learner }
        val reviewEventsBefore = context.reviewEventRepository!!.findAll(learner)
        val sessionsBefore = context.studySessionRepository!!.findAll()
        val contentStatesBefore = (0 until 5).map { index ->
            val cid = ContentId("test-pkg-content-$index")
            context.engine.getContentLearningState(learner, cid)
        }

        // Initialize Auto Play components
        val selector = AutoPlayContentSelector(context, learner, now = { 3_000 })
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        // Test with LEARNED source
        val learnedItems = selector.selectItems(AutoPlaySource.LEARNED)
        assertEquals(2, learnedItems.size)

        val config = AutoPlayConfig(
            source = AutoPlaySource.LEARNED,
            frontDelayMs = 2000L,
            playAnswerAudio = true,
            playExampleEnglishAudio = true,
            postAnswerDelayMs = 1000L,
            postExampleEnglishDelayMs = 2000L
        )

        engine.start(learnedItems, config)

        // Item 1: front timer expires -> finishes item 1 and moves to item 2
        scheduler.fireNext()

        // Item 2: front timer expires -> finishes item 2 and advances to Cycle 2
        scheduler.fireNext()

        val running = engine.state.value
        assertIs<AutoPlayEngineState.Running>(running)
        assertEquals(2L, engine.currentCycleNumber)

        // Test with DUE source
        val dueItems = selector.selectItems(AutoPlaySource.DUE)
        if (dueItems.isNotEmpty()) {
            engine.start(dueItems, config)
            scheduler.fireNext()
            engine.next()
            engine.stop()
        }

        // Test with RANDOM_ALL source
        val allItems = selector.selectItems(AutoPlaySource.RANDOM_ALL, randomSeed = 42L)
        assertEquals(5, allItems.size)
        engine.start(allItems, config)
        engine.next()
        engine.previous()
        engine.pause()
        engine.resume()
        engine.stop()

        // Verify domain state AFTER Auto Play is 100% UNCHANGED
        val memoryStatesAfter = context.memoryStateRepository!!.findAll().filter { it.learnerId == learner }
        val reviewEventsAfter = context.reviewEventRepository!!.findAll(learner)
        val sessionsAfter = context.studySessionRepository!!.findAll()
        val contentStatesAfter = (0 until 5).map { index ->
            val cid = ContentId("test-pkg-content-$index")
            context.engine.getContentLearningState(learner, cid)
        }

        assertEquals(memoryStatesBefore, memoryStatesAfter, "MemoryStates must remain completely unmutated")
        assertEquals(reviewEventsBefore, reviewEventsAfter, "ReviewEvents must remain completely unmutated")
        assertEquals(sessionsBefore, sessionsAfter, "StudySessions must remain completely unmutated")
        assertEquals(contentStatesBefore, contentStatesAfter, "ContentLearningStates must remain completely unmutated")
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
                    ContentText("$name$suffix", "$name-answer$suffix", pronunciation = "/$name$suffix/"),
                    media = ContentMedia(
                        primaryAudio = "audio/$name$suffix.mp3",
                        exampleAudio = "audio/$name-ex$suffix.mp3"
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
