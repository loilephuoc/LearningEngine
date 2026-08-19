package vn.loi.learning.android.reminder

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

class AndroidLockScreenVocabularyInvariantTest {

    @Test
    fun `Lock-screen candidate selection and review queue resolution guarantee absolute zero FSRS and ReviewEvent mutation`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "lock-inv-pkg", count = 5)

        // Seed 1 existing review
        context.engine.review(
            ReviewCommand(
                ReviewEventId("rev-init-1"),
                learner,
                LearningItemId("lock-inv-pkg-item-0"),
                ReviewRating.GOOD,
                Moment(1_000)
            )
        )

        val reviewEventsBefore = context.reviewEventRepository!!.findAll(learner)
        val memoryStatesBefore = context.memoryStateRepository!!.findAll()
        val learningItemsBefore = context.learningItemRepository!!.findByContentIds(
            (0 until 5).map { ContentId("lock-inv-pkg-content-$it") }.toSet()
        )

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner,
            now = { 50_000_000L }
        )

        // Query across all lockscreen modes
        AndroidLockScreenVocabularyMode.entries.forEach { mode ->
            val settings = AndroidLockScreenVocabularySettings(
                enabled = true,
                selectedPackageId = pkg.value,
                selectionMode = mode
            )
            selector.selectLockScreen(settings)
            selector.getReviewQueue(pkg.value, mode.name, "lock-inv-pkg-content-0")
        }

        val reviewEventsAfter = context.reviewEventRepository!!.findAll(learner)
        val memoryStatesAfter = context.memoryStateRepository!!.findAll()
        val learningItemsAfter = context.learningItemRepository!!.findByContentIds(
            (0 until 5).map { ContentId("lock-inv-pkg-content-$it") }.toSet()
        )

        // INVARIANT ASSERTIONS: Absolute 0 mutation!
        assertEquals(reviewEventsBefore.size, reviewEventsAfter.size, "ReviewEvents count must be identical")
        assertEquals(reviewEventsBefore, reviewEventsAfter, "ReviewEvents must be completely identical")
        assertEquals(memoryStatesBefore.size, memoryStatesAfter.size, "MemoryStates count must be identical")
        assertEquals(memoryStatesBefore, memoryStatesAfter, "MemoryStates must be completely identical")
        assertEquals(learningItemsBefore, learningItemsAfter, "LearningItems must be completely identical")
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
                        primaryAudio = "audio/$name$suffix.mp3"
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
