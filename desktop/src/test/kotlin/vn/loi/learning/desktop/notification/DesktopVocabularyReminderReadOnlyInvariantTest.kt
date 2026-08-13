package vn.loi.learning.desktop.notification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import vn.loi.learning.application.contentpackaging.InstalledPackageItem
import vn.loi.learning.application.study.ContentLearningState
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
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

class DesktopVocabularyReminderReadOnlyInvariantTest {
    @Test
    fun `repeated selection leaves every learning aggregate collection unchanged`() {
        val packageId = InstalledPackageId("package")
        val content = Content(ContentId("content"), ContentType.WORD, ContentText("Word"))
        val item = LearningItem(LearningItemId("item"), content.id, LearningMode.MEANING_RECOGNITION)
        val learner = LearnerId("learner")
        val memory = MemoryState(learner, item.id, LearningStage.NEW, 5.0, 0.0, Moment(0), null, 0, 0)
        val reviewEvents = mutableListOf<String>()
        val memoryStates = mutableListOf(memory)
        val studySessions = mutableListOf("existing-session")
        val studyQueues = mutableListOf("existing-queue")
        val before = listOf(reviewEvents.toList(), memoryStates.toList(), studySessions.toList(), studyQueues.toList())
        var packageReads = 0
        var contentReads = 0
        var itemReads = 0
        var memoryReads = 0
        var learningStateReads = 0
        val selector = DesktopVocabularyReminderCandidateSelector(
            installedPackages = DesktopInstalledPackageReadSource {
                packageReads++
                InstalledPackageItem("package", "Package", "1", "OPD3", 1)
            },
            packageContents = DesktopPackageContentReadSource { setOf(content.id) },
            contents = DesktopContentReadSource { contentReads++; listOf(content) },
            learningItems = DesktopLearningItemReadSource { itemReads++; listOf(item) },
            memoryStates = DesktopMemoryStateReadSource { memoryReads++; memoryStates.toList() },
            contentLearningStates = DesktopContentLearningStateReadSource { _, _ ->
                learningStateReads++
                mapOf(content.id to ContentLearningState(content.id, setOf(item.id), null))
            },
            learnerId = learner,
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            chooser = DesktopVocabularyCandidateChooser { 0 }
        )
        val settings = DesktopVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = packageId,
            selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL
        )

        repeat(5) {
            assertIs<DesktopVocabularyCandidateSelectionResult.Selected>(selector.select(settings))
        }

        val after = listOf(reviewEvents.toList(), memoryStates.toList(), studySessions.toList(), studyQueues.toList())
        assertEquals(before, after)
        assertEquals(0, reviewEvents.size, "ReviewEvent mutations")
        assertEquals(1, memoryStates.size, "MemoryState collection mutations")
        assertEquals(1, studySessions.size, "StudySession collection mutations")
        assertEquals(1, studyQueues.size, "StudyQueue collection mutations")
        assertEquals(5, packageReads)
        assertEquals(5, contentReads)
        assertEquals(5, itemReads)
        assertEquals(5, memoryReads)
        assertEquals(5, learningStateReads)
    }

    @Test
    fun `scheduled runtime tick selects and dispatches without learning mutations`() {
        val packageId = InstalledPackageId("package")
        val content = Content(ContentId("content"), ContentType.WORD, ContentText("Word"))
        val item = LearningItem(LearningItemId("item"), content.id, LearningMode.MEANING_RECOGNITION)
        val learner = LearnerId("learner")
        val memory = MemoryState(learner, item.id, LearningStage.NEW, 5.0, 0.0, Moment(0), null, 0, 0)
        val reviewEvents = mutableListOf<String>()
        val memoryStates = mutableListOf(memory)
        val studySessions = mutableListOf("session")
        val studyQueues = mutableListOf("queue")
        val before = listOf(reviewEvents.toList(), memoryStates.toList(), studySessions.toList(), studyQueues.toList())
        val selector = DesktopVocabularyReminderCandidateSelector(
            DesktopInstalledPackageReadSource {
                InstalledPackageItem("package", "Package", "1", "OPD3", 1)
            },
            DesktopPackageContentReadSource { setOf(content.id) },
            DesktopContentReadSource { listOf(content) },
            DesktopLearningItemReadSource { listOf(item) },
            DesktopMemoryStateReadSource { memoryStates.toList() },
            DesktopContentLearningStateReadSource { _, _ ->
                mapOf(content.id to ContentLearningState(content.id, setOf(item.id), null))
            },
            learner,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            DesktopVocabularyCandidateChooser { 0 }
        )
        var scheduledAction: (() -> Unit)? = null
        val dispatched = mutableListOf<DesktopVocabularyCandidate>()
        val runtime = DesktopVocabularyReminderRuntime(
            settingsRepository = object : DesktopVocabularyReminderSettingsRepository {
                override fun load() = DesktopVocabularyReminderSettings(
                    enabled = true,
                    selectedPackageId = packageId,
                    selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
                    activeStart = java.time.LocalTime.MIDNIGHT,
                    activeEnd = java.time.LocalTime.MIDNIGHT
                )
                override fun save(settings: DesktopVocabularyReminderSettings) = Unit
            },
            selector = selector,
            sink = object : DesktopVocabularyReminderSink {
                override val isReminderActive = false
                override fun dispatch(candidate: DesktopVocabularyCandidate) {
                    dispatched += candidate
                }
            },
            delayScheduler = DesktopVocabularyReminderDelayScheduler { _, action ->
                scheduledAction = action
                DesktopVocabularyReminderScheduledTask { scheduledAction = null }
            },
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            zoneId = { ZoneOffset.UTC }
        )
        runtime.start()
        requireNotNull(scheduledAction).invoke()
        runtime.close()

        assertEquals(1, dispatched.size)
        assertEquals(before, listOf(reviewEvents.toList(), memoryStates.toList(), studySessions.toList(), studyQueues.toList()))
        assertEquals(0, reviewEvents.size, "ReviewEvent mutations")
        assertEquals(1, memoryStates.size, "MemoryState collection mutations")
        assertEquals(1, studySessions.size, "StudySession collection mutations")
        assertEquals(1, studyQueues.size, "StudyQueue collection mutations")
    }
}
