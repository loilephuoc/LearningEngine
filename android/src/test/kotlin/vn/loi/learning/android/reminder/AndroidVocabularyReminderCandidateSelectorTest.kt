package vn.loi.learning.android.reminder

import java.time.Instant
import java.util.Random
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

class AndroidVocabularyReminderCandidateSelectorTest {

    private class InMemoryDifficultStore : AndroidVocabularyReminderDifficultMarkers {
        private val marked = mutableSetOf<ContentId>()
        override fun isMarked(contentId: ContentId): Boolean = contentId in marked
        override fun markedContentIds(): Set<ContentId> = marked.toSet()
        override fun toggle(contentId: ContentId): Boolean {
            return if (marked.contains(contentId)) { marked.remove(contentId); false } else { marked.add(contentId); true }
        }
        override fun setMarked(contentId: ContentId, marked: Boolean): Boolean {
            if (marked) this.marked.add(contentId) else this.marked.remove(contentId)
            return marked
        }
    }

    @Test
    fun `13 AGAIN_HARD mode queries only items with AGAIN or HARD latest rating`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "vocab-pkg", count = 5)

        // item 0: AGAIN
        context.engine.review(ReviewCommand(ReviewEventId("e-0"), learner, LearningItemId("vocab-pkg-item-0"), ReviewRating.AGAIN, Moment(1_000)))
        // item 1: HARD
        context.engine.review(ReviewCommand(ReviewEventId("e-1"), learner, LearningItemId("vocab-pkg-item-1"), ReviewRating.HARD, Moment(2_000)))
        // item 2: GOOD
        context.engine.review(ReviewCommand(ReviewEventId("e-2"), learner, LearningItemId("vocab-pkg-item-2"), ReviewRating.GOOD, Moment(3_000)))
        // items 3, 4: unreviewed

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner,
            now = { 10_000_000L }
        )

        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD
        )

        val result = selector.select(settings)
        val selected = assertIs<AndroidVocabularyCandidateSelectionResult.Selected>(result)
        assertTrue(selected.candidate.contentId.value in setOf("vocab-pkg-content-0", "vocab-pkg-content-1"))
    }

    @Test
    fun `14 DUE mode queries due items without mutating scheduler or state`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "due-pkg", count = 3)

        context.engine.review(ReviewCommand(ReviewEventId("e-0"), learner, LearningItemId("due-pkg-item-0"), ReviewRating.GOOD, Moment(1_000)))

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner,
            now = { 10_000_000_000L } // Far in future -> item is due
        )

        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidVocabularyReminderSelectionMode.DUE
        )

        val result = selector.select(settings)
        val selected = assertIs<AndroidVocabularyCandidateSelectionResult.Selected>(result)
        assertEquals("due-pkg-content-0", selected.candidate.contentId.value)

        // Verify read-only invariant: no extra review events created
        assertEquals(1, context.reviewEventRepository!!.findAll(learner).size)
    }

    @Test
    fun `15 RANDOM_LEARNED mode queries all learned items`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "learned-pkg", count = 4)

        context.engine.review(ReviewCommand(ReviewEventId("e-0"), learner, LearningItemId("learned-pkg-item-0"), ReviewRating.EASY, Moment(1_000)))
        context.engine.review(ReviewCommand(ReviewEventId("e-1"), learner, LearningItemId("learned-pkg-item-1"), ReviewRating.GOOD, Moment(2_000)))

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner
        )

        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED
        )

        val result = selector.select(settings)
        val selected = assertIs<AndroidVocabularyCandidateSelectionResult.Selected>(result)
        assertTrue(selected.candidate.contentId.value in setOf("learned-pkg-content-0", "learned-pkg-content-1"))
    }

    @Test
    fun `16 RANDOM_ALL mode queries all items in the package`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "all-pkg", count = 3)

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner
        )

        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL
        )

        val result = selector.select(settings)
        val selected = assertIs<AndroidVocabularyCandidateSelectionResult.Selected>(result)
        assertTrue(selected.candidate.contentId.value.startsWith("all-pkg-content-"))
    }

    @Test
    fun `17 MARKED_DIFFICULT mode queries only items present in difficult markers store`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "diff-pkg", count = 4)

        val diffStore = InMemoryDifficultStore()
        diffStore.setMarked(ContentId("diff-pkg-content-2"), true)

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner,
            difficultMarkers = diffStore
        )

        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT
        )

        val result = selector.select(settings)
        val selected = assertIs<AndroidVocabularyCandidateSelectionResult.Selected>(result)
        assertEquals("diff-pkg-content-2", selected.candidate.contentId.value)
    }

    @Test
    fun `18 empty candidate pool safely returns NoCandidate without throwing`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "empty-diff-pkg", count = 3)

        val diffStore = InMemoryDifficultStore() // 0 marked items

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner,
            difficultMarkers = diffStore
        )

        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = pkg.value,
            selectionMode = AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT
        )

        val result = selector.select(settings)
        val noCandidate = assertIs<AndroidVocabularyCandidateSelectionResult.NoCandidate>(result)
        assertEquals(AndroidVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE, noCandidate.reason)
    }

    @Test
    fun `candidate projection preserves example translation and audio references`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-ex-vi-1")
        context.contentRepository!!.save(
            Content(
                contentId,
                ContentType.WORD,
                ContentText(
                    primaryText = "method",
                    translatedText = "phuong phap",
                    exampleText = "Choose a method that works for you.",
                    exampleTranslation = "Chon mot phuong phap phu hop voi ban."
                ),
                media = ContentMedia(
                    image = "images/method.jpg",
                    primaryAudio = "audio/method.mp3",
                    translatedAudio = "audio/method_vi.mp3",
                    exampleAudio = "audio/method_ex.mp3",
                    exampleTranslatedAudio = "audio/method_ex_vi.mp3"
                )
            )
        )
        context.learningItemRepository!!.save(
            LearningItem(LearningItemId("item-ex-vi-1"), contentId, LearningMode.MEANING_RECOGNITION)
        )
        val contentLibraryId = ContentLibraryId("lib-ex-vi")
        val packageId = PackageId("pkg-ex-vi")
        val installedId = InstalledPackageId("pkg-ex-vi")
        context.contentLibraryRepository!!.save(ContentLibrary(contentLibraryId, LibraryDescriptor("pkg-ex-vi"), setOf(contentId)))
        context.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor("pkg-ex-vi", "1.0.0", "OPD3"), setOf(contentLibraryId)))
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(InstalledPackage.reconstitute(
            installedId, libraryId, packageId, TopicId("topic-ex-vi"), PackageName("pkg-ex-vi"), PackageVersion("1.0.0"),
            PackageState.ACTIVE, Instant.EPOCH, 1, 1
        ))
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, Instant.EPOCH))

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner
        )
        val candidate = (selector.select(
            AndroidVocabularyReminderSettings(enabled = true, selectedPackageId = "pkg-ex-vi", selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL)
        ) as AndroidVocabularyCandidateSelectionResult.Selected).candidate

        assertEquals("method", candidate.primaryText)
        assertEquals("phuong phap", candidate.translation)
        assertEquals("Choose a method that works for you.", candidate.example)
        assertEquals("Chon mot phuong phap phu hop voi ban.", candidate.exampleTranslation)
        assertEquals("images/method.jpg", candidate.imageReference)
        assertEquals("audio/method.mp3", candidate.primaryAudioReference)
        assertEquals("audio/method_vi.mp3", candidate.translationAudioReference)
        assertEquals("audio/method_vi.mp3", candidate.answerAudioReference)
        assertEquals("audio/method_ex.mp3", candidate.exampleAudioReference)
        assertEquals("audio/method_ex_vi.mp3", candidate.exampleTranslationAudioReference)
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
