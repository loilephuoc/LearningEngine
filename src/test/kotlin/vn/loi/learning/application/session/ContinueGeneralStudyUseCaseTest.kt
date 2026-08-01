package vn.loi.learning.application.session

import java.time.Instant
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
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
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.application.continuousreview.ContinuousReviewRecoveryResult

class ContinueGeneralStudyUseCaseTest {
    private val learner = LearnerId("learner-1")
    private val packageId = InstalledPackageId("package-1")
    private val topicId = TopicId("topic-1")
    private val policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 1)

    @Test
    fun `completed general session accepts exactly one ordinary next session`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 1)
        val preceding = completedSession(context, SessionId("preceding-1"))
        val request = request(preceding.id)

        val first =
            assertIs<GeneralStudyContinuationResult.Accepted>(
                context.engine.continueGeneralStudy(request)
            )
        val repeated =
            assertIs<GeneralStudyContinuationResult.Accepted>(
                context.engine.continueGeneralStudy(
                    request.copy(requestedAt = Moment(3_000L))
                )
            )

        assertEquals(first.session.id, repeated.session.id)
        assertEquals(false, first.alreadyAccepted)
        assertEquals(true, repeated.alreadyAccepted)
        assertEquals(1, first.queue.totalItemCount)
        assertEquals(preceding, context.engine.getSession(preceding.id))
        assertEquals(first.session, context.engine.getSession(first.session.id))
        assertEquals(2, context.studySessionRepository!!.findAll().size)
    }

    @Test
    fun `persisted repeated continuation after recreation reuses accepted session`() {
        val directory = Files.createTempDirectory("general-continuation")
        try {
            val firstContext = LearningApplicationFactory.createPersisted(directory)
            registerPackage(firstContext, itemCount = 1)
            val preceding =
                completedSession(firstContext, SessionId("persisted-preceding"))
            val first =
                assertIs<GeneralStudyContinuationResult.Accepted>(
                    firstContext.engine.continueGeneralStudy(request(preceding.id))
                )

            val restartedContext = LearningApplicationFactory.createPersisted(directory)
            val repeated =
                assertIs<GeneralStudyContinuationResult.Accepted>(
                    restartedContext.engine.continueGeneralStudy(
                        request(preceding.id).copy(requestedAt = Moment(9_000L))
                    )
                )

            assertEquals(first.session.id, repeated.session.id)
            assertEquals(true, repeated.alreadyAccepted)
            assertEquals(2, restartedContext.studySessionRepository!!.findAll().size)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `fresh composition continues ordinary completion without presentation snapshot for exact intent scope`() {
        val directory = Files.createTempDirectory("continuous-review-restart")
        try {
            val first = LearningApplicationFactory.createPersisted(directory)
            registerPackage(first, itemCount = 1)
            val predecessor = completedSession(first, SessionId("ordinary-no-snapshot"))
            assertNull(predecessor.completionSnapshot)
            first.engine.enableContinuousReview(learner, packageId, topicId, Moment(2_100L))

            val restarted = LearningApplicationFactory.createPersisted(directory)
            val result = assertIs<ContinuousReviewRecoveryResult.Continued>(
                restarted.engine.recoverContinuousReview(
                    learnerId = learner,
                    installedPackageId = packageId,
                    topicId = topicId,
                    recoveredAt = Moment(3_000L)
                )
            )
            val repeated = assertIs<ContinuousReviewRecoveryResult.ResumedExisting>(
                restarted.engine.recoverContinuousReview(
                    learnerId = learner,
                    installedPackageId = packageId,
                    topicId = topicId,
                    recoveredAt = Moment(4_000L)
                )
            )

            assertEquals(result.accepted.session.id, repeated.recovery.session.id)
            assertEquals(2, restarted.studySessionRepository!!.findAll().size)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `planner no-work accepts no next session`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 0)
        val preceding = completedSession(context, SessionId("preceding-no-work"))

        val result = context.engine.continueGeneralStudy(request(preceding.id))

        assertEquals(GeneralStudyContinuationResult.NoWork, result)
        assertEquals(preceding, context.engine.getSession(preceding.id))
        assertNull(context.engine.getActiveSession(learner))
        assertEquals(1, context.studySessionRepository!!.findAll().size)
    }

    @Test
    fun `active preceding session is rejected`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 1)
        val sessionId = SessionId("active-preceding")
        context.engine.startSession(startCommand(sessionId))

        val result =
            assertIs<GeneralStudyContinuationResult.Rejected>(
                context.engine.continueGeneralStudy(request(sessionId))
            )

        assertEquals(
            GeneralStudyContinuationRejection.PRECEDING_SESSION_NOT_COMPLETED,
            result.reason
        )
        assertEquals(sessionId, context.engine.getActiveSession(learner)?.id)
    }

    @Test
    fun `preceding session belonging to another learner is rejected`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 1)
        val preceding = completedSession(context, SessionId("preceding-other-learner"))

        val result =
            assertIs<GeneralStudyContinuationResult.Rejected>(
                context.engine.continueGeneralStudy(
                    request(preceding.id).copy(learnerId = LearnerId("learner-2"))
                )
            )

        assertEquals(GeneralStudyContinuationRejection.LEARNER_MISMATCH, result.reason)
        assertNull(context.engine.getActiveSession(LearnerId("learner-2")))
    }

    @Test
    fun `scope differing from authoritative preceding session is rejected`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 1)
        val preceding = completedSession(context, SessionId("preceding-scope"))

        val result =
            assertIs<GeneralStudyContinuationResult.Rejected>(
                context.engine.continueGeneralStudy(
                    request(preceding.id).copy(topicId = TopicId("other-topic"))
                )
            )

        assertEquals(GeneralStudyContinuationRejection.SCOPE_MISMATCH, result.reason)
        assertNull(context.engine.getActiveSession(learner))
    }

    private fun completedSession(
        context: LearningApplicationContext,
        sessionId: SessionId
    ) =
        context.engine
            .startSession(startCommand(sessionId))
            .let {
                context.engine.finishSession(
                    sessionId = sessionId,
                    finishedAt = Moment(2_000L)
                )
            }

    private fun startCommand(sessionId: SessionId) =
        StartStudySessionCommand(
            sessionId = sessionId,
            learnerId = learner,
            startedAt = Moment(1_000L),
            policy = policy,
            topicId = topicId,
            installedPackageId = packageId
        )

    private fun request(precedingSessionId: SessionId) =
        ContinueGeneralStudyRequest(
            precedingSessionId = precedingSessionId,
            learnerId = learner,
            requestedAt = Moment(2_500L),
            policy = policy,
            installedPackageId = packageId,
            topicId = topicId
        )

    private fun registerPackage(
        context: LearningApplicationContext,
        itemCount: Int
    ) {
        val contentPackageId = PackageId(packageId.value)
        val libraryId = ContentLibraryId("library-1")
        val contentIds = (1..itemCount).map { ContentId("content-$it") }
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = packageId,
                libraryId = LibraryId("default-library"),
                packageId = contentPackageId,
                topicId = topicId,
                name = PackageName("Package"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.ofEpochMilli(500L),
                contentCount = itemCount,
                learningItemCount = itemCount
            )
        )
        context.contentLibraryRepository!!.save(
            ContentLibrary(
                id = libraryId,
                descriptor = LibraryDescriptor("Package"),
                contentIds = contentIds.toSet()
            )
        )
        context.contentPackageRepository!!.save(
            ContentPackage(
                id = contentPackageId,
                descriptor = PackageDescriptor("Package", "1.0", "OPD3"),
                libraryIds = setOf(libraryId),
                topicId = topicId
            )
        )
        contentIds.forEachIndexed { index, contentId ->
            context.engine.registerContent(
                Content(
                    id = contentId,
                    type = ContentType.WORD,
                    text = ContentText("Question ${index + 1}", "Answer")
                )
            )
            context.engine.registerLearningItem(
                LearningItem(
                    id = LearningItemId("item-${index + 1}"),
                    contentId = contentId,
                    mode = LearningMode.MEANING_RECOGNITION
                )
            )
        }
    }
}
