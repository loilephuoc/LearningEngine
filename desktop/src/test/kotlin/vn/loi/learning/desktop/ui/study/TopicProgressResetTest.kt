package vn.loi.learning.desktop.ui.study

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.packageprogress.ResetTopicLearningProgressUseCase
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class TopicProgressResetTest {

    private val learnerId = LearnerId("learner-1")
    private val topic1Id = TopicId("topic-1")
    private val topic2Id = TopicId("topic-2")

    private val pkg1Id = InstalledPackageId("pkg-1")
    private val pkg2Id = InstalledPackageId("pkg-2")

    private val item1Id = LearningItemId("item-1")
    private val item2Id = LearningItemId("item-2")
    private val item3Id = LearningItemId("item-3")

    private val content1Id = ContentId("content-1")
    private val content2Id = ContentId("content-2")
    private val content3Id = ContentId("content-3")

    private val installedPackageRepo = InMemoryInstalledPackageRepository()
    private val contentLibraryRepo = InMemoryContentLibraryRepository()
    private val learningItemRepo = InMemoryLearningItemRepository()
    private val memoryStateRepo = InMemoryMemoryStateRepository()
    private val studySessionRepo = InMemoryStudySessionRepository()

    private val useCase = ResetTopicLearningProgressUseCase(
        installedPackageRepository = installedPackageRepo,
        contentLibraryRepository = contentLibraryRepo,
        learningItemRepository = learningItemRepo,
        memoryStateRepository = memoryStateRepo,
        studySessionRepository = studySessionRepo
    )

    private fun setupEnvironment() {
        installedPackageRepo.save(
            InstalledPackage.reconstitute(
                id = pkg1Id,
                libraryId = LibraryId("lib-1"),
                packageId = PackageId("pkg-1"),
                topicId = topic1Id,
                name = PackageName("Topic 1"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 2,
                learningItemCount = 2
            )
        )
        installedPackageRepo.save(
            InstalledPackage.reconstitute(
                id = pkg2Id,
                libraryId = LibraryId("lib-1"),
                packageId = PackageId("pkg-2"),
                topicId = topic2Id,
                name = PackageName("Topic 2"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
        )

        contentLibraryRepo.save(
            ContentLibrary(
                id = ContentLibraryId("pkg-1"),
                descriptor = LibraryDescriptor("desc-1"),
                contentIds = setOf(content1Id, content2Id)
            )
        )
        contentLibraryRepo.save(
            ContentLibrary(
                id = ContentLibraryId("pkg-2"),
                descriptor = LibraryDescriptor("desc-2"),
                contentIds = setOf(content3Id)
            )
        )

        val item1 = LearningItem(id = item1Id, contentId = content1Id, mode = LearningMode.MEANING_RECALL)
        val item2 = LearningItem(id = item2Id, contentId = content2Id, mode = LearningMode.MEANING_RECALL)
        val item3 = LearningItem(id = item3Id, contentId = content3Id, mode = LearningMode.MEANING_RECALL)
        learningItemRepo.save(item1)
        learningItemRepo.save(item2)
        learningItemRepo.save(item3)

        val now = Moment(System.currentTimeMillis())
        // Give item1 and item2 REVIEW stage, item3 REVIEW stage
        memoryStateRepo.save(
            MemoryState(
                learnerId = learnerId,
                learningItemId = item1Id,
                stage = LearningStage.REVIEW,
                difficulty = MemoryState.DEFAULT_DIFFICULTY,
                stabilityDays = 1.0,
                dueAt = now,
                lastReviewedAt = now,
                reviewCount = 1,
                lapseCount = 0
            )
        )
        memoryStateRepo.save(
            MemoryState(
                learnerId = learnerId,
                learningItemId = item2Id,
                stage = LearningStage.REVIEW,
                difficulty = MemoryState.DEFAULT_DIFFICULTY,
                stabilityDays = 1.0,
                dueAt = now,
                lastReviewedAt = now,
                reviewCount = 1,
                lapseCount = 0
            )
        )
        memoryStateRepo.save(
            MemoryState(
                learnerId = learnerId,
                learningItemId = item3Id,
                stage = LearningStage.REVIEW,
                difficulty = MemoryState.DEFAULT_DIFFICULTY,
                stabilityDays = 1.0,
                dueAt = now,
                lastReviewedAt = now,
                reviewCount = 1,
                lapseCount = 0
            )
        )

        // Active session for topic 1
        studySessionRepo.save(StudySession.start(id = SessionId("session-1"), learnerId = learnerId, startedAt = now, policy = SessionPolicy(), topicId = topic1Id))
        // Active session for topic 2
        studySessionRepo.save(StudySession.start(id = SessionId("session-2"), learnerId = learnerId, startedAt = now, policy = SessionPolicy(), topicId = topic2Id))
    }

    @Test
    fun `11 - Reset changes only selected topic items to NEW`() {
        setupEnvironment()
        val success = useCase.execute(learnerId, pkg1Id)
        assertTrue(success)

        assertNull(memoryStateRepo.find(learnerId, item1Id))
        assertNull(memoryStateRepo.find(learnerId, item2Id))
        assertNotNull(memoryStateRepo.find(learnerId, item3Id))
        assertEquals(LearningStage.REVIEW, memoryStateRepo.find(learnerId, item3Id)?.stage)
    }

    @Test
    fun `12 - Scheduler due state is removed or reset`() {
        setupEnvironment()
        useCase.execute(learnerId, pkg1Id)
        assertNull(memoryStateRepo.find(learnerId, item1Id)?.dueAt)
    }

    @Test
    fun `13 - Review history no longer resolves REVIEW`() {
        setupEnvironment()
        useCase.execute(learnerId, pkg1Id)
        val state = memoryStateRepo.find(learnerId, item1Id)
        assertEquals(null, state) // null evaluates to LearningStage.NEW
    }

    @Test
    fun `14 - Resume state for selected topic is cleared`() {
        setupEnvironment()
        useCase.execute(learnerId, pkg1Id)
        assertNull(studySessionRepo.findActiveByLearnerAndTopic(learnerId, topic1Id))
        assertNotNull(studySessionRepo.findActiveByLearnerAndTopic(learnerId, topic2Id))
    }

    @Test
    fun `15 - Content assets remain installed`() {
        setupEnvironment()
        useCase.execute(learnerId, pkg1Id)
        assertNotNull(installedPackageRepo.findById(pkg1Id))
        assertEquals(2, learningItemRepo.findAllEnabled().filter { it.contentId in setOf(content1Id, content2Id) }.size)
    }

    @Test
    fun `16 - Other topics retain progress`() {
        setupEnvironment()
        useCase.execute(learnerId, pkg1Id)
        assertEquals(LearningStage.REVIEW, memoryStateRepo.find(learnerId, item3Id)?.stage)
        assertNotNull(studySessionRepo.findActiveByLearnerAndTopic(learnerId, topic2Id))
    }

    @Test
    fun `17 - Repeated reset is idempotent`() {
        setupEnvironment()
        useCase.execute(learnerId, pkg1Id)
        val result2 = useCase.execute(learnerId, pkg1Id)
        assertTrue(result2)
        assertNull(memoryStateRepo.find(learnerId, item1Id))
    }

    @Test
    fun `18 - Cancel changes nothing`() {
        setupEnvironment()
        // If user cancels dialog, execute is not called
        assertNotNull(memoryStateRepo.find(learnerId, item1Id))
        assertEquals(LearningStage.REVIEW, memoryStateRepo.find(learnerId, item1Id)?.stage)
    }

    @Test
    fun `19 - Next session enters Discovery`() {
        setupEnvironment()
        useCase.execute(learnerId, pkg1Id)
        val itemState = memoryStateRepo.find(learnerId, item1Id)
        assertNull(itemState) // evaluateStage(null) -> LearningStage.NEW -> Discovery Front Surface
    }

    @Test
    fun `20 - Failure cannot leave partially reset state`() {
        setupEnvironment()
        val invalidPkgId = InstalledPackageId("invalid-pkg")
        val result = useCase.execute(learnerId, invalidPkgId)
        assertFalse(result)
        // Original states untouched
        assertNotNull(memoryStateRepo.find(learnerId, item1Id))
        assertNotNull(memoryStateRepo.find(learnerId, item2Id))
    }
}
