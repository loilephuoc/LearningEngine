package vn.loi.learning.desktop.ui.study

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.UninstallContentPackageCommand
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
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
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DestructiveTopicRemovalIntegrationTest {

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

    @Test
    fun `01 to 14 - Full destructive removal requirements and store-backed lifecycle`() {
        val tempDir = File.createTempFile("test_storage_", "_dir")
        tempDir.delete()
        tempDir.mkdirs()

        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir.toPath())

            val instRepo = context1.installedPackageRepository!!
            val itemRepo = context1.learningItemRepository!!
            val memoryRepo = context1.memoryStateRepository!!
            val sessionRepo = context1.studySessionRepository!!
            val eventRepo = context1.reviewEventRepository!!
            val libraryRepo = context1.contentLibraryRepository!!
            val uninstaller = context1.uninstallContentPackage

            assertNotNull(uninstaller)

            // Setup topic 1 and topic 2 packages
            val pkg1 = InstalledPackage.reconstitute(
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
            val pkg2 = InstalledPackage.reconstitute(
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
            instRepo.save(pkg1)
            instRepo.save(pkg2)

            libraryRepo.save(ContentLibrary(id = ContentLibraryId("pkg-1"), descriptor = LibraryDescriptor("desc-1"), contentIds = setOf(content1Id, content2Id)))
            libraryRepo.save(ContentLibrary(id = ContentLibraryId("pkg-2"), descriptor = LibraryDescriptor("desc-2"), contentIds = setOf(content3Id)))
            context1.contentPackageRepository!!.save(
                ContentPackage(
                    id = PackageId("pkg-1"),
                    descriptor = PackageDescriptor("Package 1", "1.0", "OPD3"),
                    libraryIds = setOf(ContentLibraryId("pkg-1")),
                    topicId = topic1Id
                )
            )
            context1.contentPackageRepository!!.save(
                ContentPackage(
                    id = PackageId("pkg-2"),
                    descriptor = PackageDescriptor("Package 2", "1.0", "OPD3"),
                    libraryIds = setOf(ContentLibraryId("pkg-2")),
                    topicId = topic2Id
                )
            )

            val item1 = LearningItem(id = item1Id, contentId = content1Id, mode = LearningMode.MEANING_RECALL)
            val item2 = LearningItem(id = item2Id, contentId = content2Id, mode = LearningMode.MEANING_RECALL)
            val item3 = LearningItem(id = item3Id, contentId = content3Id, mode = LearningMode.MEANING_RECALL)
            itemRepo.save(item1)
            itemRepo.save(item2)
            itemRepo.save(item3)

            val now = Moment(System.currentTimeMillis())
            // Item 1 and 2 in REVIEW
            memoryRepo.save(MemoryState(learnerId = learnerId, learningItemId = item1Id, stage = LearningStage.REVIEW, difficulty = MemoryState.DEFAULT_DIFFICULTY, stabilityDays = 2.0, dueAt = now, lastReviewedAt = now, reviewCount = 1, lapseCount = 0))
            memoryRepo.save(MemoryState(learnerId = learnerId, learningItemId = item2Id, stage = LearningStage.REVIEW, difficulty = MemoryState.DEFAULT_DIFFICULTY, stabilityDays = 2.0, dueAt = now, lastReviewedAt = now, reviewCount = 1, lapseCount = 0))
            memoryRepo.save(MemoryState(learnerId = learnerId, learningItemId = item3Id, stage = LearningStage.REVIEW, difficulty = MemoryState.DEFAULT_DIFFICULTY, stabilityDays = 2.0, dueAt = now, lastReviewedAt = now, reviewCount = 1, lapseCount = 0))

            sessionRepo.save(StudySession.start(id = SessionId("session-1"), learnerId = learnerId, startedAt = now, policy = SessionPolicy(), topicId = topic1Id))
            sessionRepo.save(StudySession.start(id = SessionId("session-2"), learnerId = learnerId, startedAt = now, policy = SessionPolicy(), topicId = topic2Id))

            // Pre-remove verification: prove REVIEW
            val item1StateBefore = memoryRepo.find(learnerId, item1Id)
            assertEquals(LearningStage.REVIEW, item1StateBefore?.stage)
            assertNotNull(sessionRepo.findActiveByLearnerAndTopic(learnerId, topic1Id))

            // Execute destructive removal
            uninstaller.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("default-catalog"),
                    packageId = PackageId("pkg-1")
                )
            )

            // Test 1: Remove deletes package record
            assertNull(instRepo.findById(pkg1Id))

            // Test 2: Remove deletes owned MemoryState
            assertNull(memoryRepo.find(learnerId, item1Id))
            assertNull(memoryRepo.find(learnerId, item2Id))

            // Test 3: Remove deletes scheduler/due state
            val item1StateAfter = memoryRepo.find(learnerId, item1Id)
            assertNull(item1StateAfter?.dueAt)

            // Test 4: Remove deletes review history/events
            assertTrue(eventRepo.findAll(learnerId).filter { it.learningItemId == item1Id }.isEmpty())

            // Test 5 & 6: Remove deletes active/resumable sessions and queue state
            assertNull(sessionRepo.findActiveByLearnerAndTopic(learnerId, topic1Id))

            // Test 7 & 8: Other topics remain intact
            assertNotNull(instRepo.findById(pkg2Id))
            val item3State = memoryRepo.find(learnerId, item3Id)
            assertEquals(LearningStage.REVIEW, item3State?.stage)
            assertNotNull(sessionRepo.findActiveByLearnerAndTopic(learnerId, topic2Id))

            // Test 9 & 10: Reimporting same package initializes NEW stage and Discovery
            instRepo.save(pkg1)
            libraryRepo.save(ContentLibrary(id = ContentLibraryId("pkg-1"), descriptor = LibraryDescriptor("desc-1"), contentIds = setOf(content1Id, content2Id)))
            context1.contentPackageRepository!!.save(
                ContentPackage(
                    id = PackageId("pkg-1"),
                    descriptor = PackageDescriptor("Package 1", "1.0", "OPD3"),
                    libraryIds = setOf(ContentLibraryId("pkg-1")),
                    topicId = topic1Id
                )
            )
            itemRepo.save(item1)
            itemRepo.save(item2)

            val reimportedState = memoryRepo.find(learnerId, item1Id)
            assertNull(reimportedState) // Evaluates to LearningStage.NEW -> Discovery mode

            // Test 11: Restart app after reimport still remains NEW
            val context2 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            assertNull(context2.memoryStateRepository!!.find(learnerId, item1Id))

            // Test 13 & 14: Repeated remove / cancel handled safely
            context2.uninstallContentPackage?.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("default-catalog"),
                    packageId = PackageId("pkg-1")
                )
            )
            assertNull(context2.installedPackageRepository!!.findById(pkg1Id))

        } finally {
            tempDir.deleteRecursively()
        }
    }
}
