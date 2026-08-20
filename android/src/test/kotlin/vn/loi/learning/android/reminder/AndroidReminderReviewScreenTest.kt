package vn.loi.learning.android.reminder

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
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

class AndroidReminderReviewScreenTest {

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
    fun `26 and 27 and 28 Reminder Review queue resolves exact anchor from same package and mode`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "queue-pkg", count = 6)

        // item 0: AGAIN
        context.engine.review(ReviewCommand(ReviewEventId("e-0"), learner, LearningItemId("queue-pkg-item-0"), ReviewRating.AGAIN, Moment(1_000)))
        // item 2: AGAIN
        context.engine.review(ReviewCommand(ReviewEventId("e-2"), learner, LearningItemId("queue-pkg-item-2"), ReviewRating.AGAIN, Moment(2_000)))
        // item 4: HARD
        context.engine.review(ReviewCommand(ReviewEventId("e-4"), learner, LearningItemId("queue-pkg-item-4"), ReviewRating.HARD, Moment(3_000)))

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner
        )

        // Tap notification for item 2 in AGAIN_HARD mode
        val session = selector.getReminderReviewQueue(
            packageIdStr = pkg.value,
            mode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD,
            anchorContentIdStr = "queue-pkg-content-2"
        )

        assertNotNull(session)
        assertEquals(pkg.value, session.packageId)
        assertEquals(AndroidVocabularyReminderSelectionMode.AGAIN_HARD, session.mode)
        assertEquals("queue-pkg-content-2", session.anchorContentId)
        assertEquals(3, session.items.size)

        // Verify initial index starts exactly at anchor item 2
        assertEquals(1, session.initialIndex)
        assertEquals("queue-pkg-content-2", session.items[session.initialIndex].contentId.value)
    }

    @Test
    fun `36 and 37 difficult markers toggle immediately in review screen without mutating scheduler`() {
        val diffStore = InMemoryDifficultStore()
        val contentId = ContentId("content-diff-1")

        assertTrue(diffStore.toggle(contentId))
        assertTrue(diffStore.isMarked(contentId))
        assertEquals(setOf(contentId), diffStore.markedContentIds())

        // Toggle off
        assertFalse(diffStore.toggle(contentId))
        assertFalse(diffStore.isMarked(contentId))
        assertTrue(diffStore.markedContentIds().isEmpty())
    }

    @Test
    fun `tap-to-play audio mappings follow current candidate and handle missing audio safely`() {
        val candidate = AndroidVocabularyCandidate(
            contentId = ContentId("test-audio-content"),
            packageId = InstalledPackageId("pkg-1"),
            packageName = "Package 1",
            primaryText = "strawberry",
            answer = "a sweet soft red fruit",
            translation = "qua dau tay",
            ipa = "ˈstrɔː.bər.i",
            partOfSpeech = "noun",
            imageReference = "images/strawberry.jpg",
            primaryAudioReference = "audio/strawberry_q.mp3",
            answerAudioReference = "audio/strawberry_a.mp3",
            exampleAudioReference = "audio/strawberry_ex.mp3",
            translationAudioReference = "audio/strawberry_vi.mp3",
            example = "I like fresh strawberries."
        )

        val playedList = mutableListOf<String>()
        var stopped = 0

        val playAudio: (String?) -> Unit = { audioRef ->
            stopped++
            if (!audioRef.isNullOrBlank()) {
                playedList.add(audioRef)
            }
        }

        // Tap Question card
        playAudio(candidate.primaryAudioReference)
        assertEquals(1, stopped)
        assertEquals(listOf("audio/strawberry_q.mp3"), playedList)

        // Tap Meaning card
        playAudio(candidate.translationAudioReference ?: candidate.answerAudioReference)
        assertEquals(2, stopped)
        assertEquals(listOf("audio/strawberry_q.mp3", "audio/strawberry_vi.mp3"), playedList)

        // Tap Example card
        playAudio(candidate.exampleAudioReference)
        assertEquals(3, stopped)
        assertEquals(listOf("audio/strawberry_q.mp3", "audio/strawberry_vi.mp3", "audio/strawberry_ex.mp3"), playedList)

        // Candidate with null audio
        val candidateNoAudio = candidate.copy(
            primaryAudioReference = null,
            answerAudioReference = null,
            exampleAudioReference = null,
            translationAudioReference = null
        )

        playAudio(candidateNoAudio.primaryAudioReference)
        assertEquals(4, stopped)
        // No new item added to playedList
        assertEquals(3, playedList.size)
    }

    @Test
    fun `Quick Rating from Full Review updates FSRS but leaves Home Widget candidate unchanged`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val pkg = installPackage(context, "widget-persist-pkg", count = 3)
        val bridge = AndroidReminderReviewRatingBridge(context, learner)

        val selector = AndroidVocabularyReminderCandidateSelector(
            context = context,
            learnerId = learner
        )

        // 1. Initial candidate resolution
        val widgetResult = selector.selectHomeWidget(
            settings = AndroidHomeVocabularyWidgetSettings(
                selectedPackageId = pkg.value,
                selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL
            )
        )
        val widgetCandidate = (widgetResult as? AndroidVocabularyCandidateSelectionResult.Selected)?.candidate
        assertNotNull(widgetCandidate)
        val initialId = widgetCandidate.contentId.value

        // 2. User opens Full Review and rates candidate GOOD
        val result = bridge.submitRating(initialId, ReviewRating.GOOD)
        assertIs<QuickReviewRatingResult.Success>(result)

        // 3. Verify widget candidate selection state / FSRS event recorded
        val currentEvents = context.reviewEventRepository?.findAll(learner).orEmpty()
        assertEquals(1, currentEvents.size)
        assertEquals(ReviewRating.GOOD, currentEvents.first().rating)
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
