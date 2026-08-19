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

class AndroidVocabularyReminderInvariantTest {

    @Test
    fun `39 to 45 Reminder selection, queue resolution, and browsing guarantee absolute zero FSRS and ReviewEvent mutation`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "invariant-pkg", count = 5)

        // Seed 1 existing review
        context.engine.review(
            ReviewCommand(
                ReviewEventId("rev-init-1"),
                learner,
                LearningItemId("invariant-pkg-item-0"),
                ReviewRating.GOOD,
                Moment(1_000)
            )
        )

        // Take snapshot before reminder operations
        val reviewEventsBefore = context.reviewEventRepository!!.findAll(learner)
        val memoryStatesBefore = context.memoryStateRepository!!.findAll()
        val learningItemsBefore = context.learningItemRepository!!.findByContentIds(
            (0 until 5).map { ContentId("invariant-pkg-content-$it") }.toSet()
        )

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner,
            now = { 50_000_000L }
        )

        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidVocabularyReminderSelectionMode.DUE
        )

        // 1. Execute candidate selection
        val selected = selector.select(settings)
        assertNotNull(selected)

        // 2. Build review queue
        val session = selector.getReminderReviewQueue(
            packageIdStr = pkg.value,
            mode = AndroidVocabularyReminderSelectionMode.DUE,
            anchorContentIdStr = "invariant-pkg-content-0"
        )
        assertNotNull(session)

        // 3. Query all other modes
        selector.select(settings.copy(selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL))
        selector.select(settings.copy(selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED))
        selector.select(settings.copy(selectionMode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD))

        // Take snapshot after all reminder operations
        val reviewEventsAfter = context.reviewEventRepository!!.findAll(learner)
        val memoryStatesAfter = context.memoryStateRepository!!.findAll()
        val learningItemsAfter = context.learningItemRepository!!.findByContentIds(
            (0 until 5).map { ContentId("invariant-pkg-content-$it") }.toSet()
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
