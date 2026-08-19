package vn.loi.learning.android.reminder

import java.time.Instant
import kotlin.test.*
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

class AndroidLockScreenVocabularyCandidateSelectorTest {

    private val learnerId = LearnerId("test-learner")

    @Test
    fun `NEW_UNSEEN selects only contents where all sibling items have zero review history`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "pkg-unseen", count = 4)

        // item 0: completely unreviewed (NEW_UNSEEN candidate)
        // item 1: has review event on primary item
        context.engine.review(
            ReviewCommand(
                ReviewEventId("rev-1"),
                learnerId,
                LearningItemId("pkg-unseen-item-1-0"),
                ReviewRating.GOOD,
                Moment(1_000)
            )
        )

        // item 2: has a sibling item with a review event
        // (primary item pkg-unseen-item-2-0 unreviewed, but sibling pkg-unseen-item-2-1 reviewed)
        context.learningItemRepository!!.save(
            LearningItem(LearningItemId("pkg-unseen-item-2-1"), ContentId("pkg-unseen-content-2"), LearningMode.DICTATION)
        )
        context.engine.review(
            ReviewCommand(
                ReviewEventId("rev-2-sibling"),
                learnerId,
                LearningItemId("pkg-unseen-item-2-1"),
                ReviewRating.AGAIN,
                Moment(1_000)
            )
        )

        // item 3: disabled learning item
        context.learningItemRepository!!.save(
            LearningItem(LearningItemId("pkg-unseen-item-3-0"), ContentId("pkg-unseen-content-3"), LearningMode.MEANING_RECOGNITION, isEnabled = false)
        )

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learnerId,
            now = { 50_000_000L }
        )

        val settings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidLockScreenVocabularyMode.NEW_UNSEEN
        )

        val result = selector.selectLockScreen(settings)
        assertTrue(result is AndroidVocabularyCandidateSelectionResult.Selected)
        // Only content-0 should be eligible!
        assertEquals("pkg-unseen-content-0", result.candidate.contentId.value)
    }

    @Test
    fun `lock-screen recent exclusion queue maintains limit of 8 items and relaxes properly`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "pkg-recent", count = 10)

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learnerId,
            now = { 50_000_000L }
        )

        val settings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidLockScreenVocabularyMode.RANDOM_ALL
        )

        val seen = mutableSetOf<String>()
        repeat(8) {
            val result = selector.selectLockScreen(settings)
            assertTrue(result is AndroidVocabularyCandidateSelectionResult.Selected)
            seen.add(result.candidate.contentId.value)
        }
        // All 8 picks should be distinct because pool size is 10 > 8
        assertEquals(8, seen.size)
    }

    private class InMemoryShuffleBagStore : LockScreenShuffleBagStore {
        val map = mutableMapOf<String, LockScreenShuffleBagState>()
        override fun load(contextKey: String): LockScreenShuffleBagState? = map[contextKey]
        override fun save(contextKey: String, state: LockScreenShuffleBagState): Boolean {
            map[contextKey] = state
            return true
        }
        override fun clear(contextKey: String): Boolean {
            map.remove(contextKey)
            return true
        }
    }

    @Test
    fun `shuffle bag consumes all candidates without repeating until pool is exhausted`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "pkg-bag", count = 5)
        val bagStore = InMemoryShuffleBagStore()

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learnerId,
            shuffleBagStore = bagStore,
            now = { 50_000_000L }
        )

        val settings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidLockScreenVocabularyMode.RANDOM_ALL
        )

        val firstCyclePicks = (0 until 5).map {
            val res = selector.selectLockScreen(settings)
            assertTrue(res is AndroidVocabularyCandidateSelectionResult.Selected)
            res.candidate.contentId.value
        }

        // All 5 picks in the first cycle must be unique!
        assertEquals(5, firstCyclePicks.toSet().size)

        // 6th pick starts new cycle (bag replenished)
        val sixthPick = (selector.selectLockScreen(settings) as AndroidVocabularyCandidateSelectionResult.Selected).candidate.contentId.value
        // First of new cycle must not equal the last presented (5th pick)
        assertNotEquals(firstCyclePicks.last(), sixthPick)
    }

    @Test
    fun `shuffle bag state survives store reload and process restart simulation`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "pkg-persist-bag", count = 6)
        val bagStore = InMemoryShuffleBagStore()

        val selector1 = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learnerId,
            shuffleBagStore = bagStore,
            now = { 50_000_000L }
        )

        val settings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidLockScreenVocabularyMode.RANDOM_ALL
        )

        val pick1 = (selector1.selectLockScreen(settings) as AndroidVocabularyCandidateSelectionResult.Selected).candidate.contentId.value
        val pick2 = (selector1.selectLockScreen(settings) as AndroidVocabularyCandidateSelectionResult.Selected).candidate.contentId.value

        // Simulate process restart by creating new selector instance sharing same bag store
        val selector2 = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learnerId,
            shuffleBagStore = bagStore,
            now = { 50_000_000L }
        )

        val pick3 = (selector2.selectLockScreen(settings) as AndroidVocabularyCandidateSelectionResult.Selected).candidate.contentId.value
        val pick4 = (selector2.selectLockScreen(settings) as AndroidVocabularyCandidateSelectionResult.Selected).candidate.contentId.value

        val allPicks = setOf(pick1, pick2, pick3, pick4)
        // All 4 picks across selectors must be distinct!
        assertEquals(4, allPicks.size)
    }

    @Test
    fun `getReviewQueue with NEW_UNSEEN mode builds valid review session with anchor`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = installPackage(context, "pkg-queue", count = 3)

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learnerId,
            now = { 50_000_000L }
        )

        val session = selector.getReviewQueue(
            packageIdStr = pkg.value,
            modeStr = "NEW_UNSEEN",
            anchorContentIdStr = "pkg-queue-content-1"
        )

        assertNotNull(session)
        assertEquals("pkg-queue-content-1", session.anchorContentId)
        assertEquals(3, session.items.size)
        assertEquals(1, session.initialIndex)
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
                        image = "images/$name$suffix.png"
                    )
                )
            )
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("$name-item$suffix-0"), contentId, LearningMode.MEANING_RECOGNITION)
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
