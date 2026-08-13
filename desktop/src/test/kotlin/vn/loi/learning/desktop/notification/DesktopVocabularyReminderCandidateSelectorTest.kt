package vn.loi.learning.desktop.notification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import vn.loi.learning.application.contentpackaging.InstalledPackageItem
import vn.loi.learning.application.study.ContentLearningState
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

class DesktopVocabularyReminderCandidateSelectorTest {
    @Test
    fun `marked difficult selects only marked enabled resolvable content in selected package`() {
        val fixture = Fixture()
        val marked = fixture.add("marked")
        fixture.marked += marked
        fixture.add("unmarked")
        val other = fixture.add("other", packageId = fixture.packageB)
        fixture.marked += other
        val missing = fixture.add("missing", includeContent = false)
        fixture.marked += missing
        val disabled = fixture.add("disabled", enabled = false)
        fixture.marked += disabled

        assertEquals(marked, fixture.select(DesktopVocabularyReminderSelectionMode.MARKED_DIFFICULT).contentId)
    }

    @Test
    fun `marked difficult supports unseen content without real review history`() {
        val fixture = Fixture()
        val unseen = fixture.add("unseen")
        fixture.marked += unseen
        assertEquals(unseen, fixture.select(DesktopVocabularyReminderSelectionMode.MARKED_DIFFICULT).contentId)
    }
    @Test
    fun `again hard uses latest content-level rating and rejects unseen disabled and suspended-only content`() {
        val fixture = Fixture()
        val again = fixture.add("again", rating = ReviewRating.AGAIN)
        val hard = fixture.add("hard", rating = ReviewRating.HARD)
        fixture.add("became-good", ratings = listOf(ReviewRating.AGAIN, ReviewRating.GOOD))
        fixture.add("became-easy", ratings = listOf(ReviewRating.HARD, ReviewRating.EASY))
        fixture.add("unseen")
        fixture.add("disabled", rating = ReviewRating.AGAIN, enabled = false)
        fixture.add("suspended", rating = ReviewRating.HARD, stage = LearningStage.SUSPENDED)

        assertEquals(again, fixture.select(DesktopVocabularyReminderSelectionMode.AGAIN_HARD).contentId)
        assertEquals(hard, fixture.select(DesktopVocabularyReminderSelectionMode.AGAIN_HARD).contentId)
        assertEquals(again, fixture.select(DesktopVocabularyReminderSelectionMode.AGAIN_HARD).contentId)
    }

    @Test
    fun `due requires learned canonical evidence and canonical due state`() {
        val fixture = Fixture()
        val due = fixture.add("due", rating = ReviewRating.GOOD, dueAt = 900)
        fixture.add("future", rating = ReviewRating.GOOD, dueAt = 1_100)
        fixture.add("new-due", dueAt = 900, stage = LearningStage.NEW)
        fixture.add("suspended-due", rating = ReviewRating.GOOD, dueAt = 900, stage = LearningStage.SUSPENDED)
        fixture.add("disabled-due", rating = ReviewRating.GOOD, dueAt = 900, enabled = false)

        assertEquals(due, fixture.select(DesktopVocabularyReminderSelectionMode.DUE).contentId)
        assertEquals(due, fixture.select(DesktopVocabularyReminderSelectionMode.DUE).contentId)
    }

    @Test
    fun `random learned accepts learned and rejects unseen suspended and disabled`() {
        val fixture = Fixture()
        val learned = fixture.add("learned", rating = ReviewRating.EASY)
        fixture.add("unseen")
        fixture.add("suspended", rating = ReviewRating.GOOD, stage = LearningStage.SUSPENDED)
        fixture.add("disabled", rating = ReviewRating.GOOD, enabled = false)

        assertEquals(learned, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_LEARNED).contentId)
        assertEquals(learned, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_LEARNED).contentId)
    }

    @Test
    fun `random all includes new and learned while skipping disabled suspended and missing content`() {
        val fixture = Fixture()
        val new = fixture.add("new", includeOptionalPresentation = false)
        val learned = fixture.add("learned", rating = ReviewRating.GOOD)
        fixture.add("disabled", enabled = false)
        fixture.add("suspended", stage = LearningStage.SUSPENDED)
        fixture.add("missing", includeContent = false)

        val first = fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL)
        assertEquals(learned, first.contentId)
        val second = fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL)
        assertEquals(new, second.contentId)
        assertNull(second.translation)
        assertNull(second.imageReference)
        assertNull(second.primaryAudioReference)
        assertEquals(learned, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
    }

    @Test
    fun `candidate projects authoritative definition pronunciation pos media and metadata`() {
        val fixture = Fixture()
        fixture.add(
            "projected",
            definition = "A precise answer",
            pronunciation = "/wɜːd/ (noun)",
            translation = "từ",
            image = "images/word.png",
            audio = "audio/word.mp3"
        )

        val candidate = fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL)
        assertEquals("A precise answer", candidate.answer)
        assertEquals("/wɜːd/", candidate.ipa)
        assertEquals("NOUN", candidate.partOfSpeech)
        assertEquals("images/word.png", candidate.imageReference)
        assertEquals("audio/word.mp3", candidate.primaryAudioReference)
        assertEquals("Lesson", candidate.lesson)
        assertEquals("Section", candidate.section)
        assertEquals("Package A", candidate.packageDisplayName)
    }

    @Test
    fun `selection is package scoped and never falls back to another package`() {
        val fixture = Fixture()
        val selected = fixture.add("selected", packageId = fixture.packageA)
        fixture.add("other", packageId = fixture.packageB)

        assertEquals(selected, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
        assertEquals(selected, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
    }

    @Test
    fun `deleted empty and content-empty packages return explicit no-candidate results`() {
        val fixture = Fixture()
        assertEquals(
            DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_EMPTY,
            fixture.noCandidateReason(fixture.selectResult(DesktopVocabularyReminderSelectionMode.RANDOM_ALL))
        )
        fixture.availablePackages.remove(fixture.packageA)
        assertEquals(
            DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE,
            fixture.noCandidateReason(fixture.selectResult(DesktopVocabularyReminderSelectionMode.RANDOM_ALL))
        )
        fixture.availablePackages += fixture.packageA
        fixture.add("missing-content", includeContent = false)
        assertEquals(
            DesktopVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE,
            fixture.noCandidateReason(fixture.selectResult(DesktopVocabularyReminderSelectionMode.RANDOM_ALL))
        )
    }

    @Test
    fun `recent window excludes alternatives then relaxes oldest exclusions`() {
        val fixture = Fixture(recentLimit = 10)
        val a = fixture.add("a")
        val b = fixture.add("b")

        assertEquals(a, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
        assertEquals(b, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
        assertEquals(a, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
    }

    @Test
    fun `one item pool repeats`() {
        val fixture = Fixture()
        val a = fixture.add("a", packageId = fixture.packageA)
        assertEquals(a, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
        assertEquals(a, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
    }

    @Test
    fun `package and mode changes clear recent history`() {
        val fixture = Fixture()
        val a = fixture.add("a", packageId = fixture.packageA, rating = ReviewRating.GOOD)
        fixture.add("z", packageId = fixture.packageA, rating = ReviewRating.GOOD)
        val b = fixture.add("b", packageId = fixture.packageB, rating = ReviewRating.GOOD)
        assertEquals(a, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)

        fixture.selectedPackage = fixture.packageB
        assertEquals(b, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
        fixture.selectedPackage = fixture.packageA
        assertEquals(a, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
        assertEquals(a, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_LEARNED).contentId)
    }

    @Test
    fun `injected chooser makes candidate selection deterministic`() {
        val fixture = Fixture(chooser = DesktopVocabularyCandidateChooser { count -> count - 1 })
        fixture.add("a")
        val b = fixture.add("b")
        assertEquals(b, fixture.select(DesktopVocabularyReminderSelectionMode.RANDOM_ALL).contentId)
    }

    private fun assertNoCandidate(result: DesktopVocabularyCandidateSelectionResult) {
        assertIs<DesktopVocabularyCandidateSelectionResult.NoCandidate>(result)
    }

    private class Fixture(
        recentLimit: Int = 10,
        chooser: DesktopVocabularyCandidateChooser = DesktopVocabularyCandidateChooser { 0 }
    ) {
        val packageA = InstalledPackageId("package-a")
        val packageB = InstalledPackageId("package-b")
        var selectedPackage = packageA
        val availablePackages = mutableSetOf(packageA, packageB)
        val marked = mutableSetOf<ContentId>()
        private val contentIdsByPackage = mutableMapOf(packageA to linkedSetOf<ContentId>(), packageB to linkedSetOf())
        private val contents = linkedMapOf<ContentId, Content>()
        private val items = mutableListOf<LearningItem>()
        private val states = mutableListOf<MemoryState>()
        private val learningStates = mutableMapOf<ContentId, ContentLearningState>()
        private val learnerId = LearnerId("learner")
        private val selector = DesktopVocabularyReminderCandidateSelector(
            installedPackages = DesktopInstalledPackageReadSource { id ->
                id.takeIf { it in availablePackages }?.let {
                    InstalledPackageItem(it.value, if (it == packageA) "Package A" else "Package B", "1", "OPD3", 1)
                }
            },
            packageContents = DesktopPackageContentReadSource { id ->
                if (id in availablePackages) contentIdsByPackage[id]?.toSet() else null
            },
            contents = DesktopContentReadSource { ids -> ids.mapNotNull(contents::get) },
            learningItems = DesktopLearningItemReadSource { ids -> items.filter { it.contentId in ids } },
            memoryStates = DesktopMemoryStateReadSource { states.toList() },
            contentLearningStates = DesktopContentLearningStateReadSource { _, ids ->
                ids.associateWith { id -> learningStates[id] ?: ContentLearningState(id, emptySet(), null) }
            },
            learnerId = learnerId,
            clock = Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC),
            markedContent = DesktopVocabularyReminderMarkedReadSource(marked::contains),
            chooser = chooser,
            recentLimit = recentLimit
        )

        fun add(
            name: String,
            packageId: InstalledPackageId = packageA,
            rating: ReviewRating? = null,
            ratings: List<ReviewRating> = rating?.let(::listOf).orEmpty(),
            enabled: Boolean = true,
            stage: LearningStage = if (rating == null && ratings.isEmpty()) LearningStage.NEW else LearningStage.REVIEW,
            dueAt: Long = 2_000,
            includeContent: Boolean = true,
            includeOptionalPresentation: Boolean = true,
            definition: String? = null,
            pronunciation: String? = null,
            translation: String? = if (includeOptionalPresentation) "Meaning $name" else null,
            image: String? = if (includeOptionalPresentation) "images/$name.png" else null,
            audio: String? = if (includeOptionalPresentation) "audio/$name.mp3" else null
        ): ContentId {
            val contentId = ContentId(name)
            val item = LearningItem(LearningItemId("item-$name"), contentId, LearningMode.MEANING_RECOGNITION, enabled)
            contentIdsByPackage.getValue(packageId) += contentId
            items += item
            if (includeContent) {
                contents[contentId] = Content(
                    contentId,
                    ContentType.WORD,
                    ContentText("Word $name", translation, pronunciation),
                    ContentMedia(primaryAudio = audio, image = image),
                    ContentMetadata(section = "Section", lesson = "Lesson"),
                    ContentCustomFields(
                        definition?.let { setOf(ContentCustomField(ContentFieldId("definition"), it)) }.orEmpty()
                    )
                )
            }
            val eventList = mutableListOf<ReviewEvent>()
            if (ratings.isNotEmpty()) {
                ratings.forEachIndexed { index, eventRating ->
                    val before = reviewedState(item.id, stage, dueAt, index)
                    val after = reviewedState(item.id, stage, dueAt, index + 1)
                    eventList += ReviewEvent(
                        ReviewEventId("event-$name-$index"), eventRating, Moment(500L + index),
                        TimeSpan.ZERO, before, after, RatingSource.STANDARD_REVIEW
                    )
                }
                states += reviewedState(item.id, stage, dueAt, ratings.size)
            } else {
                states += MemoryState(
                    learnerId, item.id, stage, 5.0, 0.0, Moment(dueAt), null, 0, 0
                )
            }
            learningStates[contentId] = ContentLearningState(
                contentId, setOf(item.id), eventList.lastOrNull()
            )
            return contentId
        }

        fun select(mode: DesktopVocabularyReminderSelectionMode): DesktopVocabularyCandidate =
            assertIs<DesktopVocabularyCandidateSelectionResult.Selected>(selectResult(mode)).candidate

        fun selectResult(mode: DesktopVocabularyReminderSelectionMode) = selector.select(
            DesktopVocabularyReminderSettings(true, selectedPackage, mode)
        )

        fun noCandidateReason(result: DesktopVocabularyCandidateSelectionResult) =
            assertIs<DesktopVocabularyCandidateSelectionResult.NoCandidate>(result).reason

        private fun reviewedState(
            itemId: LearningItemId,
            stage: LearningStage,
            dueAt: Long,
            reviewCount: Int
        ) = MemoryState(
            learnerId, itemId, stage, 5.0, 1.0, Moment(dueAt),
            if (reviewCount == 0) null else Moment(499L + reviewCount), reviewCount, 0
        )
    }
}
