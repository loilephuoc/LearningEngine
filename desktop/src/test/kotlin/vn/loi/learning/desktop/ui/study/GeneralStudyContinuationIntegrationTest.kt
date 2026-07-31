package vn.loi.learning.desktop.ui.study

import java.time.Instant
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import vn.loi.learning.application.session.StartStudySessionCommand
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
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class GeneralStudyContinuationIntegrationTest {

    @Test
    fun `correct Typing completion reveals then rates GOOD once from Question`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemIds = registerPackage(context, itemCount = 2)
        val learner = LearnerId("default-learner")
        itemIds.forEachIndexed { index, itemId ->
            context.engine.review(
                vn.loi.learning.application.review.ReviewCommand(
                    ReviewEventId("typing-seed-$index"),
                    learner,
                    itemId,
                    ReviewRating.AGAIN,
                    Moment(index.toLong() + 1)
                )
            )
        }
        val facade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 0, reviewItemLimit = 2)
                }
            )
        val question = facade.startStudy()
        val token = assertNotNull(question.experienceRotationContext)
        val currentItemId = LearningItemId(requireNotNull(question.currentLearningItemId))
        val historyBefore = context.engine.getReviewHistory(learner, currentItemId).size
        val goodRequest = typingSuccessRequest(question, 1L)

        assertEquals(ReviewWorkspaceState.Question, question.workspaceState)
        assertFailsWith<IllegalArgumentException> {
            facade.review(ReviewRating.GOOD)
        }
        assertFailsWith<IllegalArgumentException> {
            facade.completeCorrectTypingRecall(
                goodRequest.copy(
                    decision =
                        goodRequest.decision.copy(rating = ReviewRating.EASY)
                )
            )
        }

        val flowCoordinator = DesktopLearningFlowCoordinator()
        flowCoordinator.synchronize(question)
        var revealedBeforeRating = false
        val next =
            facade.completeCorrectTypingRecall(goodRequest) { revealed ->
                revealedBeforeRating =
                    revealed.workspaceState == ReviewWorkspaceState.AnswerRevealed &&
                        flowCoordinator.synchronize(revealed).learningFlowProgress?.isRatingReady == true
            }

        assertTrue(revealedBeforeRating)
        assertEquals(historyBefore + 1, context.engine.getReviewHistory(learner, currentItemId).size)
        assertEquals(1, next.reviewItemsReviewed)
        assertNotEquals(question.currentLearningItemId, next.currentLearningItemId)
        assertEquals(ReviewWorkspaceState.Question, next.workspaceState)

        val duplicate =
            facade.completeCorrectTypingRecall(goodRequest)
        assertEquals(historyBefore + 1, context.engine.getReviewHistory(learner, currentItemId).size)
        assertEquals(next.currentLearningItemId, duplicate.currentLearningItemId)
        assertEquals(1, duplicate.reviewItemsReviewed)
    }

    @Test
    fun `correct Typing completion of final item reaches Completion`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemId = registerPackage(context, itemCount = 1).single()
        val learner = LearnerId("default-learner")
        context.engine.review(
            vn.loi.learning.application.review.ReviewCommand(
                ReviewEventId("typing-final-seed"),
                learner,
                itemId,
                ReviewRating.AGAIN,
                Moment(1)
            )
        )
        val facade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 0, reviewItemLimit = 1)
                }
            )
        val question = facade.startStudy()

        val completed =
            facade.completeCorrectTypingRecall(
                typingSuccessRequest(question, 1L)
            )

        assertTrue(completed.sessionCompleted)
        assertEquals(ReviewWorkspaceState.Completed, completed.workspaceState)
        assertEquals(1, completed.reviewItemsReviewed)
        assertEquals(
            2,
            context.engine.getReviewHistory(learner, itemId).size
        )
    }

    @Test
    fun `failed Typing completion after reveal is retryable without duplicate review`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemId = registerPackage(context, itemCount = 1).single()
        val learner = LearnerId("default-learner")
        context.engine.review(
            vn.loi.learning.application.review.ReviewCommand(
                ReviewEventId("typing-retry-seed"),
                learner,
                itemId,
                ReviewRating.AGAIN,
                Moment(1)
            )
        )
        val facade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 0, reviewItemLimit = 1)
                }
            )
        val question = facade.startStudy()
        val token = assertNotNull(question.experienceRotationContext)
        val historyBefore = context.engine.getReviewHistory(learner, itemId).size

        assertFailsWith<IllegalStateException> {
            facade.completeCorrectTypingRecall(typingSuccessRequest(question, 1L)) {
                error("simulated flow synchronization failure")
            }
        }

        val revealed = facade.load()
        assertEquals(ReviewWorkspaceState.AnswerRevealed, revealed.workspaceState)
        assertEquals(TypingRatingMode.AUTOMATIC_PENDING, revealed.typingRatingMode)
        assertEquals(historyBefore, context.engine.getReviewHistory(learner, itemId).size)

        val retried =
            facade.completeCorrectTypingRecall(typingSuccessRequest(question, 1L))
        assertTrue(retried.sessionCompleted)
        assertEquals(historyBefore + 1, context.engine.getReviewHistory(learner, itemId).size)
    }

    @Test
    fun `measured exact Typing persists Hard and Easy policy ratings through real sessions`() {
        listOf(
            Triple(ReviewRating.AGAIN, 14_000L, ReviewRating.HARD),
            Triple(ReviewRating.GOOD, 1_500L, ReviewRating.EASY)
        ).forEachIndexed { index, (seedRating, elapsed, expectedRating) ->
            val context = LearningApplicationFactory.createInMemory()
            val itemId = registerPackage(context, itemCount = 1).single()
            val learner = LearnerId("default-learner")
            context.engine.review(
                vn.loi.learning.application.review.ReviewCommand(
                    ReviewEventId("typing-policy-seed-$index"),
                    learner,
                    itemId,
                    seedRating,
                    Moment(1)
                )
            )
            val facade =
                StudyFacade(
                    context,
                    sessionPolicyProvider = {
                        SessionPolicy(newItemLimit = 0, reviewItemLimit = 1)
                    }
                )
            val question = facade.startStudy()
            val request =
                typingSuccessRequest(
                    state = question,
                    revision = 1L,
                    totalElapsedMillis = elapsed,
                    recallLatencyMillis = if (expectedRating == ReviewRating.EASY) 500L else 3_000L
                )

            val completed = facade.completeCorrectTypingRecall(request)

            assertTrue(completed.sessionCompleted)
            assertEquals(
                expectedRating,
                context.engine.getReviewHistory(learner, itemId).last().rating
            )
        }
    }

    @Test
    fun `Typing Reveal forces Again and legacy rating attempts commit once through real session`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemIds = registerPackage(context, itemCount = 2)
        val learner = LearnerId("default-learner")
        itemIds.forEachIndexed { index, itemId ->
            context.engine.review(
                vn.loi.learning.application.review.ReviewCommand(
                    ReviewEventId("typing-forced-seed-$index"),
                    learner,
                    itemId,
                    ReviewRating.GOOD,
                    Moment(index.toLong() + 1)
                )
            )
        }
        val facade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 0, reviewItemLimit = 2)
                }
            )
        val question = facade.startStudy()
        val currentId = LearningItemId(assertNotNull(question.currentLearningItemId))
        val historyBefore = context.engine.getReviewHistory(learner, currentId).size
        val revealRequest = typingRevealRequest(question)

        val revealed = facade.revealTypingRecall(revealRequest)
        assertEquals(TypingRatingMode.FORCED_AGAIN, revealed.typingRatingMode)
        assertEquals(ReviewWorkspaceState.AnswerRevealed, revealed.workspaceState)

        val next = facade.review(ReviewRating.EASY)

        assertEquals(
            ReviewRating.AGAIN,
            context.engine.getReviewHistory(learner, currentId).last().rating
        )
        assertEquals(historyBefore + 1, context.engine.getReviewHistory(learner, currentId).size)
        assertNotEquals(question.currentLearningItemId, next.currentLearningItemId)

        facade.completeRevealedTypingRecallAsAgain(revealRequest)
        assertEquals(historyBefore + 1, context.engine.getReviewHistory(learner, currentId).size)
    }

    @Test
    fun `final Typing Forced Again reaches authoritative session completion`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemId = registerPackage(context, itemCount = 1).single()
        val learner = LearnerId("default-learner")
        context.engine.review(
            vn.loi.learning.application.review.ReviewCommand(
                ReviewEventId("typing-forced-final-seed"),
                learner,
                itemId,
                ReviewRating.GOOD,
                Moment(1)
            )
        )
        val facade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 0, reviewItemLimit = 1)
                }
            )
        val question = facade.startStudy()
        val request = typingRevealRequest(question)
        facade.revealTypingRecall(request)

        val completed = facade.completeRevealedTypingRecallAsAgain(request)

        assertTrue(completed.sessionCompleted)
        assertEquals(ReviewWorkspaceState.Completed, completed.workspaceState)
        assertEquals(
            ReviewRating.AGAIN,
            context.engine.getReviewHistory(learner, itemId).last().rating
        )
    }

    @Test
    fun `Review header advances coherently fourteen to thirteen to twelve`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemIds = registerPackage(context, itemCount = 14)
        val learner = LearnerId("default-learner")
        itemIds.forEachIndexed { index, itemId ->
            context.engine.review(
                vn.loi.learning.application.review.ReviewCommand(
                    ReviewEventId("seed-review-$index"),
                    learner,
                    itemId,
                    ReviewRating.AGAIN,
                    Moment(index.toLong() + 1)
                )
            )
        }
        var facade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 0, reviewItemLimit = 20)
                }
            )

        var state = facade.startStudy()
        state = facade.refreshHeaderStatistics(state)
        assertEquals(0, availableHeader(state).reviewCompleted)
        assertEquals(14, availableHeader(state).reviewEffectiveWorkload)

        state = facade.revealAnswer()
        state = facade.review(ReviewRating.GOOD)
        assertEquals(1, state.reviewItemsReviewed)
        assertEquals(1, availableHeader(state).reviewCompleted)

        facade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 0, reviewItemLimit = 20)
                }
            )
        state = facade.enterStudy()
        state = facade.refreshHeaderStatistics(state)
        assertEquals(1, availableHeader(state).reviewCompleted)

        state = facade.revealAnswer()
        state = facade.review(ReviewRating.HARD)
        assertEquals(2, state.reviewItemsReviewed)
        assertEquals(2, availableHeader(state).reviewCompleted)

        val queue =
            context.engine.requireStudyQueueProgress(
                assertNotNull(context.engine.getActiveSession(learner)).id
            )
        assertEquals(14, queue.effectiveReviewWorkload)
        assertEquals(2, queue.completedItemCount)

        state = facade.undoLatestReview()
        assertEquals(1, state.reviewItemsReviewed)
        assertEquals(1, availableHeader(state).reviewCompleted)
    }

    @Test
    fun `Study entry replaces stale goal session and retains matching goal session`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 60)
        var policy = SessionPolicy(newItemLimit = 50, reviewItemLimit = 200)
        val facade = StudyFacade(context, sessionPolicyProvider = { policy })
        var state = facade.startStudy()
        val original = assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))
        val originalHeader =
            assertIs<StudyHeaderStatisticsState.Available>(
                facade.refreshHeaderStatistics(state).headerStatistics
            ).value

        assertEquals(50, originalHeader.newConfiguredTarget)
        assertEquals(200, originalHeader.reviewConfiguredTarget)
        assertEquals(50, context.engine.requireStudyQueueProgress(original.id).totalItemCount)

        policy = SessionPolicy(newItemLimit = 10, reviewItemLimit = 20)
        state = facade.enterStudy()
        val replacement = assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))
        val replacementHeader =
            assertIs<StudyHeaderStatisticsState.Available>(
                facade.refreshHeaderStatistics(state).headerStatistics
            ).value

        assertNotEquals(original.id, replacement.id)
        assertEquals(SessionStatus.FINISHED, context.engine.getSession(original.id)?.status)
        assertEquals(SessionPolicy(10, 20), replacement.policy)
        assertEquals(0, replacement.newItemsReviewed)
        assertEquals(0, replacement.reviewItemsReviewed)
        assertEquals(10, context.engine.requireStudyQueueProgress(replacement.id).totalItemCount)
        assertEquals(10, replacementHeader.newConfiguredTarget)
        assertEquals(20, replacementHeader.reviewConfiguredTarget)

        repeat(2) {
            state = facade.revealAnswer()
            state = facade.review(ReviewRating.GOOD)
        }
        val progressed = assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))
        assertEquals(2, progressed.newItemsReviewed)

        state = facade.enterStudy()
        assertEquals(
            progressed.id,
            context.engine.getActiveSession(LearnerId("default-learner"))?.id
        )
        assertEquals(2, state.newItemsReviewed)

        policy = SessionPolicy(newItemLimit = 5, reviewItemLimit = 50)
        state = facade.enterStudy()
        val secondReplacement =
            assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))
        val secondHeader =
            assertIs<StudyHeaderStatisticsState.Available>(
                facade.refreshHeaderStatistics(state).headerStatistics
            ).value

        assertNotEquals(progressed.id, secondReplacement.id)
        assertEquals(0, state.newItemsReviewed)
        assertEquals(0, state.reviewItemsReviewed)
        assertEquals(5, context.engine.requireStudyQueueProgress(secondReplacement.id).totalItemCount)
        assertEquals(5, secondHeader.newConfiguredTarget)
        assertEquals(50, secondHeader.reviewConfiguredTarget)
    }

    @Test
    fun `recreated facade replaces recovered active session with latest goals`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 20)
        val firstFacade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 10, reviewItemLimit = 20)
                }
            )
        firstFacade.startStudy()
        val stale = assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))

        val restartedFacade =
            StudyFacade(
                context,
                sessionPolicyProvider = {
                    SessionPolicy(newItemLimit = 5, reviewItemLimit = 50)
                }
            )
        val entered = restartedFacade.enterStudy()
        val replacement = assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))

        assertNotEquals(stale.id, replacement.id)
        assertEquals(SessionStatus.FINISHED, context.engine.getSession(stale.id)?.status)
        assertEquals(SessionPolicy(5, 50), replacement.policy)
        assertEquals(0, entered.newItemsReviewed)
        assertEquals(0, entered.reviewItemsReviewed)
    }

    @Test
    fun `new session uses fresh policy for queue and header instead of previous goals`() {
        val context = LearningApplicationFactory.createInMemory()
        registerPackage(context, itemCount = 15)
        var policy = SessionPolicy(newItemLimit = 5, reviewItemLimit = 20)
        val facade = StudyFacade(context, sessionPolicyProvider = { policy })
        var state = facade.startStudy()
        val firstSession = assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))

        assertEquals(5, firstSession.policy.newItemLimit)
        assertEquals(20, firstSession.policy.reviewItemLimit)
        assertEquals(5, context.engine.requireStudyQueueProgress(firstSession.id).totalItemCount)

        policy = SessionPolicy(newItemLimit = 10, reviewItemLimit = 50)
        facade.load()
        assertEquals(5, context.engine.getSession(firstSession.id)?.policy?.newItemLimit)
        assertEquals(20, context.engine.getSession(firstSession.id)?.policy?.reviewItemLimit)

        repeat(5) {
            state = facade.revealAnswer()
            state = facade.review(ReviewRating.GOOD)
        }
        assertTrue(state.sessionCompleted)

        state = facade.continueGeneralStudyAfterCompletion()
        val secondSession = assertNotNull(context.engine.getActiveSession(LearnerId("default-learner")))
        val header =
            assertIs<StudyHeaderStatisticsState.Available>(
                facade.refreshHeaderStatistics(state).headerStatistics
            ).value

        assertNotEquals(firstSession.id, secondSession.id)
        assertEquals(10, secondSession.policy.newItemLimit)
        assertEquals(50, secondSession.policy.reviewItemLimit)
        assertEquals(10, context.engine.requireStudyQueueProgress(secondSession.id).totalItemCount)
        assertEquals(secondSession.policy.newItemLimit, header.newConfiguredTarget)
        assertEquals(secondSession.policy.reviewItemLimit, header.reviewConfiguredTarget)
    }

    @Test
    fun `general completion starts a new session from durable memory and next new item`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemIds = registerPackage(context, itemCount = 5)
        val learnerId = LearnerId("default-learner")
        val completedSessionId = SessionId("completed-general-session")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = completedSessionId,
                learnerId = learnerId,
                startedAt = Moment(1_000L),
                policy = SessionPolicy(newItemLimit = 4, reviewItemLimit = 100),
                installedPackageId = InstalledPackageId("general-package"),
                topicId = TopicId("general-topic")
            )
        )
        val facade = StudyFacade(context)
        var state = facade.load()

        repeat(4) {
            assertEquals(LearningStage.NEW, state.learningStage)
            val diagnostics = assertNotNull(state.learningStageDiagnostics)
            assertEquals(state.currentLearningItemId, diagnostics.learningItemId)
            assertEquals(LearningStage.NEW, diagnostics.stage)
            assertEquals(0, diagnostics.reviewCount)
            assertFalse(diagnostics.hasPersistedMemoryState)
            state = facade.revealAnswer()
            state = facade.review(ReviewRating.GOOD)
        }

        assertTrue(state.sessionCompleted)
        assertEquals(4, state.newItemsReviewed)
        assertEquals(0, state.reviewItemsReviewed)
        assertTrue(state.schedulerFeedback?.stageTransition?.contains("NEW", ignoreCase = true) == true)
        assertTrue(state.schedulerFeedback?.stageTransition?.contains("REVIEW", ignoreCase = true) == true)

        val continued = facade.continueGeneralStudyAfterCompletion()
        val newSession = assertNotNull(context.engine.getActiveSession(learnerId))

        assertNotEquals(completedSessionId, newSession.id)
        assertEquals(
            SessionId(
                UUID.nameUUIDFromBytes(
                    "general-study-continuation:${completedSessionId.value}"
                        .toByteArray(StandardCharsets.UTF_8)
                ).toString()
            ),
            newSession.id
        )
        assertNotNull(context.engine.getSession(completedSessionId))
        assertNotNull(context.engine.getStudyQueue(completedSessionId))
        assertFalse(continued.sessionCompleted)
        assertTrue(continued.hasActiveSession)
        assertEquals(LearningStage.NEW, continued.learningStage)
        assertEquals(itemIds.last(), LearningItemId(assertNotNull(continued.currentLearningItemId)))
        itemIds.take(4).forEach { itemId ->
            assertEquals(LearningStage.REVIEW, context.engine.getMemoryState(learnerId, itemId)?.stage)
        }

        val refreshed = facade.load()
        assertFalse(refreshed.sessionCompleted)
        assertEquals(newSession.id, context.engine.getActiveSession(learnerId)?.id)
    }

    @Test
    fun `general continuation with no candidate returns idle without completion loop`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemId = registerPackage(context, itemCount = 1).single()
        val facade = StudyFacade(context)
        var state = facade.startStudy()
        state = facade.revealAnswer()
        state = facade.review(ReviewRating.GOOD)
        assertTrue(state.sessionCompleted)
        val completedItem = assertNotNull(context.learningItemRepository?.findById(itemId))
        context.learningItemRepository?.save(completedItem.copy(isEnabled = false))

        val continued = facade.continueGeneralStudyAfterCompletion()

        assertFalse(continued.sessionCompleted)
        assertFalse(continued.hasActiveSession)
        assertTrue(continued.message.contains("No learning items"))
        assertNull(context.engine.getActiveSession(LearnerId("default-learner")))
        assertNotNull(
            context.studySessionRepository
                ?.findAll()
                ?.singleOrNull { it.status == SessionStatus.FINISHED }
        )
        val reloaded = facade.load()
        assertFalse(reloaded.sessionCompleted)
        assertFalse(reloaded.hasActiveSession)
    }

    private fun registerPackage(
        context: LearningApplicationContext,
        itemCount: Int
    ): List<LearningItemId> {
        val packageId = PackageId("general-package")
        val installedPackageId = InstalledPackageId(packageId.value)
        val topicId = TopicId("general-topic")
        val libraryId = ContentLibraryId("general-library")
        val contentIds = (1..itemCount).map { ContentId("content-$it") }
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = installedPackageId,
                libraryId = LibraryId("default-library"),
                packageId = packageId,
                topicId = topicId,
                name = PackageName("General"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.ofEpochMilli(500L),
                contentCount = itemCount,
                learningItemCount = itemCount
            )
        )
        context.contentLibraryRepository!!.save(
            ContentLibrary(libraryId, LibraryDescriptor("General"), contentIds.toSet())
        )
        context.contentPackageRepository!!.save(
            ContentPackage(
                packageId,
                PackageDescriptor("General", "1.0", "OPD3"),
                setOf(libraryId),
                topicId
            )
        )
        return contentIds.mapIndexed { index, contentId ->
            context.engine.registerContent(
                Content(contentId, ContentType.WORD, ContentText("Question ${index + 1}", "Answer"))
            )
            LearningItemId("item-${index + 1}").also { itemId ->
                context.engine.registerLearningItem(
                    LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
                )
            }
        }
    }

    private fun availableHeader(state: StudyUiState) =
        assertIs<StudyHeaderStatisticsState.Available>(state.headerStatistics).value

    private fun typingSuccessRequest(
        state: StudyUiState,
        revision: Long,
        totalElapsedMillis: Long = 3_000L,
        recallLatencyMillis: Long = 1_000L
    ): TypingRecallSuccessRequest {
        val context = assertNotNull(state.experienceRotationContext)
        val reviewContext = assertNotNull(state.currentItemReviewContext)
        val metrics =
            TypingAttemptMetrics(
                context = context,
                attemptGeneration = revision,
                startedAtMillis = 0L,
                firstInputAtMillis = recallLatencyMillis,
                completedAtMillis = totalElapsedMillis,
                totalElapsedMillis = totalElapsedMillis,
                recallLatencyMillis = recallLatencyMillis,
                typingDurationMillis = (totalElapsedMillis - recallLatencyMillis).coerceAtLeast(0L),
                canonicalCodePointCount = 5,
                materialInputChangeCount = 5,
                mismatchEventCount = 0,
                correctionEventCount = 0,
                hadMismatch = false,
                revealUsed = false,
                completedExactly = true,
                finalInputCodePointCount = 5,
                itemOrigin = reviewContext.origin,
                learningStage = state.learningStage,
                previousRating = reviewContext.previousRating
            )
        return TypingRecallSuccessRequest(
            context = context,
            inputRevision = revision,
            metrics = metrics,
            decision = TypingAutoRatingPolicy.decide(metrics)
        )
    }

    private fun typingRevealRequest(state: StudyUiState): TypingRecallRevealRequest {
        val context = assertNotNull(state.experienceRotationContext)
        val reviewContext = assertNotNull(state.currentItemReviewContext)
        val metrics =
            TypingAttemptMetrics(
                context = context,
                attemptGeneration = 1L,
                startedAtMillis = 0L,
                firstInputAtMillis = 1_000L,
                completedAtMillis = 3_000L,
                totalElapsedMillis = 3_000L,
                recallLatencyMillis = 1_000L,
                typingDurationMillis = 2_000L,
                canonicalCodePointCount = 5,
                materialInputChangeCount = 2,
                mismatchEventCount = 1,
                correctionEventCount = 0,
                hadMismatch = true,
                revealUsed = true,
                completedExactly = false,
                finalInputCodePointCount = 2,
                itemOrigin = reviewContext.origin,
                learningStage = state.learningStage,
                previousRating = reviewContext.previousRating
            )
        return TypingRecallRevealRequest(context, 1L, metrics)
    }
}
