package vn.loi.learning.android.study

import vn.loi.learning.application.review.ReviewCommand
import org.junit.Test
import kotlin.test.*
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.recall.RecallPlanFactory
import vn.loi.learning.application.recall.RecallPlanFactoryResult
import vn.loi.learning.application.recall.RecallPlanPolicy
import vn.loi.learning.application.recall.RecallPlanRequest
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyFacadeTest {
    @Test fun `session content snapshot is keyed by session and package and invalidated on Home`() {
        val source = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt")
        )
        assertTrue(source.contains("it.sessionId == session.id && it.packageId == session.installedPackageId"))
        val home = source.substringAfter("fun home():").substringBefore("fun start(")
        assertTrue(home.contains("sessionContentSnapshot = null"))
    }
    @Test fun `active 990 content session loads through one canonical bulk repository call`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contents = (1..990).map { index ->
            Content(ContentId("android-content-$index"), ContentType.WORD, ContentText("word $index", "meaning $index"))
        }
        context.contentRepository!!.saveAll(contents)
        val itemId = LearningItemId("android-item-1")
        context.learningItemRepository!!.save(
            LearningItem(itemId, contents.first().id, LearningMode.MEANING_RECOGNITION)
        )
        context.engine.review(ReviewCommand(ReviewEventId("seed-android-item-990"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("android-session-990")
        val session = StudySession.start(
            sessionId, learner, Moment(1_000),
            SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), contents.mapTo(linkedSetOf(), Content::id)
        )
        context.studySessionRepository!!.save(session)
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contents.first().id), configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )
        val countingRepository = CountingContentRepository(context.contentRepository!!)
        val facade = AndroidStudyFacade(context.copy(contentRepository = countingRepository), learner, { 2_000 })

        val startedAt = System.nanoTime()
        val runtime = assertIs<AndroidStudyState.Typing>(facade.load(sessionId.value))
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals(sessionId, runtime.plan.sessionId)
        assertEquals(1, countingRepository.bulkCallCount)
        assertEquals(0, countingRepository.singleCallCount)
        assertEquals(contents.map(Content::id), countingRepository.lastRequestedIds)
        println("ANDROID_UAT_005_METRICS study_initial_load_ms=$elapsedMillis bulk_calls=1 find_by_id_calls=0 scope_size=990")
    }

    @Test fun `exact session handoff loads requested plan and never falls back to active session`() {
        val f=fixture()
        val exact=assertIs<AndroidStudyState.Typing>(f.facade.loadExact("android-session-review"))
        assertEquals("android-session-review",exact.plan.sessionId.value)
        assertIs<AndroidStudyState.Failed>(f.facade.loadExact("stale-session"))
    }
    @Test fun `typing uses shared plan execution bridge completion and undo exactly once`() {
        val f = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
        assertEquals(ReviewRating.GOOD, initial.previousCanonicalRating)
        assertTrue(initial.canonicalRatingTransitionEligible)
        assertNull(initial.attempt?.previousRating)
        assertNull(initial.attempt?.learningStage)
        assertNull(initial.attempt?.previousReviewAtMillis)
        assertEquals("xin chào", initial.prompt)
        assertEquals(TypingAnswerEvaluationStatus.VALID_PREFIX, f.facade.updateAnswer(initial, "hel").let { assertIs<AndroidStudyState.Typing>(it).evaluation })
        val mismatch = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(initial, "hex"))
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, mismatch.evaluation)

        val corrected = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(mismatch, "hello"))
        assertEquals(initial.previousCanonicalRating, corrected.previousCanonicalRating)
        val pending = assertIs<AndroidStudyState.Typing>(f.facade.submitTypingIfCorrect(corrected))
        assertEquals(initial.previousCanonicalRating, pending.previousCanonicalRating)
        assertTrue(pending.completed)
        assertTrue(pending.completionPending)
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
        val completed = assertIs<AndroidStudyState.Typing>(f.facade.commitTypingRating(pending, null))
        assertEquals(initial.previousCanonicalRating, completed.previousCanonicalRating)
        assertTrue(!completed.completionPending)
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
        assertEquals(listOf(RecallMode.TYPING), f.context.engine.getSession(completed.plan.sessionId)!!
            .recallModeHistory.boundedEntries.map(RecallModeHistoryEntry::mode))
        f.facade.commitTypingRating(completed, ReviewRating.HARD)
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
        val completion = assertIs<AndroidStudyState.Completion>(f.facade.next(completed))
        assertEquals("Review", completion.modeFamily)
        assertEquals(1, completion.totalCompleted)
        assertEquals(0, completion.newCompleted)
        assertEquals(1, completion.reviewCompleted)
        assertEquals(SessionStatus.FINISHED, f.context.engine.getSession(SessionId(completion.sessionId))!!.status)
        assertNull(f.context.engine.getActiveSession(f.learner))
        assertIs<AndroidStudyState.Completion>(f.facade.loadExact(completion.sessionId))
        assertNull(f.context.engine.getActiveSession(f.learner))
        assertIs<AndroidStudyState.Typing>(f.facade.undo(completion))
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
    }

    @Test fun `reveal crosses shared lapse path and resume preserves current session`() {
        val f = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
        val resumed = assertIs<AndroidStudyState.Typing>(
            AndroidStudyFacade(f.context, f.learner, now = { 2_000 }).load(initial.plan.sessionId.value)
        )
        assertEquals(initial.plan.planId, resumed.plan.planId)
        val revealed = assertIs<AndroidStudyState.Typing>(f.facade.reveal(initial))
        assertTrue(revealed.revealed)
        assertEquals(RecallOutcome.REVEALED, revealed.outcome)
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
    }

    @Test fun `manual Typing rating replaces pending automatic decision and commits exactly once`() {
        val f = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load()).copy(manualRating = ReviewRating.HARD)
        val exact = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(initial, "hello"))
        val pending = assertIs<AndroidStudyState.Typing>(f.facade.submitTypingIfCorrect(exact))
        assertTrue(pending.completionPending)
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
        assertEquals(ReviewRating.HARD, typingRatingTransitionPresentation(pending)?.finalRating)

        val committed = assertIs<AndroidStudyState.Typing>(f.facade.commitTypingRating(pending, pending.manualRating))
        assertEquals(ReviewRating.HARD, committed.manualRating)
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)

        f.facade.commitTypingRating(committed, ReviewRating.EASY)
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
    }

    @Test fun `android projects MCQ option order and one-shot presentation from shared plan`() {
        val f = fixture()
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val choices = listOf(
            RecallChoice("a", "Alpha", false), RecallChoice("b", "Beta", true),
            RecallChoice("c", "Gamma", false), RecallChoice("d", "Delta", false)
        )
        val state = assertIs<AndroidStudyState.MultipleChoice>(
            f.facade.present(base.copy(mode = RecallMode.MULTIPLE_CHOICE, prompt = RecallPrompt.MultipleChoice("Choose", choices)))
        )
        assertEquals(choices, state.choices)
        assertEquals("Choose", state.question)
    }

    @Test fun `MCQ click submits stable choice once through shared execution and bridge`() {
        val f = fixture()
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val choices = listOf(RecallChoice("a", "Wrong", false), RecallChoice("b", "hello", true))
        val plan = base.copy(
            mode = RecallMode.MULTIPLE_CHOICE,
            prompt = RecallPrompt.MultipleChoice("Choose", choices),
            answerContract = base.answerContract.copy(canonicalAnswer = "b", kind = RecallAnswerKind.CHOICE),
            platformRequirements = RecallPlatformRequirements(requiresChoiceSelection = true)
        )
        val state = assertIs<AndroidStudyState.MultipleChoice>(f.facade.present(plan))
        val completed = assertIs<AndroidStudyState.MultipleChoice>(f.facade.choose(state, "b"))
        assertTrue(completed.completed)
        assertEquals(RecallOutcome.CORRECT, completed.outcome)
        f.facade.choose(completed, "b")
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
    }

    @Test fun `android listening resolves media and represents unavailable audio`() {
        val f = fixture(resolveMedia = { if (it == "audio/word.mp3") "C:/media/word.mp3" else null })
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val resolved = assertIs<AndroidStudyState.Listening>(
            f.facade.present(base.copy(mode = RecallMode.LISTENING, prompt = RecallPrompt.Listening(RecallResourceId("audio/word.mp3"))))
        )
        val missing = assertIs<AndroidStudyState.Listening>(
            f.facade.present(base.copy(mode = RecallMode.LISTENING, prompt = RecallPrompt.Listening(RecallResourceId("missing.mp3"))))
        )
        assertEquals("C:/media/word.mp3", resolved.audioPath)
        assertTrue(missing.audioUnavailable)
    }

    @Test fun `android image represents resolved and unavailable resources`() {
        val f = fixture(resolveMedia = { if (it == "image/prompt.png") "C:/media/prompt.png" else null })
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val resolved = assertIs<AndroidStudyState.ImageRecall>(
            f.facade.present(base.copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("image/prompt.png"))))
        )
        val missing = assertIs<AndroidStudyState.ImageRecall>(
            f.facade.present(base.copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("missing.png"))))
        )
        assertEquals("C:/media/prompt.png", resolved.imagePath)
        assertTrue(missing.imageUnavailable)
    }

    @Test fun `android example preserves exact prefix blank and suffix`() {
        val f = fixture()
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val state = assertIs<AndroidStudyState.ExampleCompletion>(
            f.facade.present(base.copy(
                mode = RecallMode.EXAMPLE_COMPLETION,
                prompt = RecallPrompt.ExampleCompletion("Please make the bed now.", RecallTextSpan(7, 19))
            ))
        )
        assertEquals("Please ", state.prefix)
        assertEquals("make the bed", state.blank)
        assertEquals(" now.", state.suffix)
    }

    @Test fun `practice completion remains isolated and queue policy stays shared`() {
        listOf(
            PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED,
            PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP
        ).forEach { policy ->
            val f = fixture(practiceLoopPolicy = policy)
            val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
            val corrected = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(initial, "hello"))
            assertTrue(assertIs<AndroidStudyState.Typing>(f.facade.submitTypingIfCorrect(corrected)).completed)
            assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
            assertEquals(policy, f.context.studyQueue.get(initial.plan.sessionId)?.practiceLoopPolicy)
            assertNotNull(f.context.engine.getPracticeProgress(initial.plan.sessionId))
        }
    }

    @Test fun `continuous skim transitions exhausted coverage into persisted practice loop`() {
        val f = fixture()
        val facade = AndroidStudyFacade(
            f.context,
            f.learner,
            now = { 2_000 },
            continuousSkimEnabled = { true }
        )
        val initial = assertIs<AndroidStudyState.Typing>(facade.load())
        val exact = assertIs<AndroidStudyState.Typing>(facade.updateAnswer(initial, "hello"))
        val pending = assertIs<AndroidStudyState.Typing>(facade.submitTypingIfCorrect(exact))
        val committed = assertIs<AndroidStudyState.Typing>(facade.commitTypingRating(pending, null))

        val next = facade.next(committed)
        if (next is AndroidStudyState.Failed) error(next.message)
        val practice = assertIs<AndroidStudyState.Typing>(next)
        val session = f.context.engine.getSession(practice.plan.sessionId)!!
        assertEquals(SessionEvaluationPolicy.PRACTICE_ONLY, session.policy.evaluationPolicy)
        assertEquals(
            PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED,
            f.context.studyQueue.get(session.id)?.practiceLoopPolicy
        )
        assertEquals("Skim · Round 2", practice.hud?.skimStatus)
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)

        val practiceExact = assertIs<AndroidStudyState.Typing>(facade.updateAnswer(practice, "hello"))
        val practicePending = assertIs<AndroidStudyState.Typing>(facade.submitTypingIfCorrect(practiceExact))
        assertIs<AndroidStudyState.Typing>(facade.commitTypingRating(practicePending, null))
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
    }

    @Test fun `difficult practice manual Good updates dynamic membership and undo restores it`() {
        val f = fixture(practiceLoopPolicy = PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP)
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
        val afterOverride = f.facade.overridePracticeRating(initial, ReviewRating.GOOD)
        assertIs<AndroidStudyState.Typing>(afterOverride)
        assertEquals(2, f.context.engine.getReviewHistory(f.learner, f.itemId).size)

        assertIs<AndroidStudyState.Typing>(f.facade.undo(afterOverride))
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
        assertEquals(1, f.context.engine.getPracticeProgress(initial.plan.sessionId)?.membershipSize)
    }

    @Test fun `focused difficult practice ignores canonical manual rating callback`() {
        val f = fixture(
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP,
            focusedPracticeKind = vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT
        )
        val initial = assertIs<AndroidStudyState.Introduction>(f.facade.load())
        assertEquals(vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT, initial.focusedPracticeKind)
        assertNull(initial.plan)
        val before = f.context.engine.getReviewHistory(f.learner, f.itemId)
        val memoryBefore = f.context.memoryStateRepository!!.find(f.learner, f.itemId)

        assertEquals(initial, f.facade.overridePracticeRating(initial, ReviewRating.GOOD))
        assertEquals(before, f.context.engine.getReviewHistory(f.learner, f.itemId))
        val revealed = assertIs<AndroidStudyState.Introduction>(f.facade.revealIntroduction(initial))
        assertTrue(revealed.revealed)
        assertEquals(before, f.context.engine.getReviewHistory(f.learner, f.itemId))
        assertEquals(memoryBefore, f.context.memoryStateRepository!!.find(f.learner, f.itemId))

        assertIs<AndroidStudyState.Introduction>(f.facade.next(revealed))
        assertEquals(before, f.context.engine.getReviewHistory(f.learner, f.itemId))
        assertEquals(memoryBefore, f.context.memoryStateRepository!!.find(f.learner, f.itemId))
        assertEquals(
            listOf(f.itemId),
            f.context.studyQueue.get(SessionId("android-session-LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP"))?.fixedPracticeMembership
        )
    }

    @Test fun `focused difficult front advance remains practice only and keeps dynamic membership`() {
        val f = fixture(
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP,
            focusedPracticeKind = vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT
        )
        val initial = assertIs<AndroidStudyState.Introduction>(f.facade.load())
        val sessionId = SessionId(initial.sessionId)
        val reviewBefore = f.context.engine.getReviewHistory(f.learner, f.itemId)
        val memoryBefore = f.context.memoryStateRepository!!.find(f.learner, f.itemId)
        val queueBefore = requireNotNull(f.context.studyQueue.get(sessionId))
        val visitBefore = requireNotNull(initial.presentationVisitId)

        val advanced = assertIs<AndroidStudyState.Introduction>(f.facade.next(initial))
        val queueAfter = requireNotNull(f.context.studyQueue.get(sessionId))

        assertFalse(advanced.revealed)
        assertTrue(requireNotNull(advanced.presentationVisitId) != visitBefore)
        assertTrue(queueAfter.practiceExposureSequence > queueBefore.practiceExposureSequence)
        assertEquals(reviewBefore, f.context.engine.getReviewHistory(f.learner, f.itemId))
        assertEquals(memoryBefore, f.context.memoryStateRepository!!.find(f.learner, f.itemId))
        assertEquals(queueBefore.fixedPracticeMembership, queueAfter.fixedPracticeMembership)
        assertEquals(PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP, queueAfter.practiceLoopPolicy)
    }

    @Test fun `evaluative learned review uses Product Brain and commits one canonical event`() {
        val f = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
        val before = f.context.engine.getReviewHistory(f.learner, f.itemId).size
        val exact = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(initial, "hello"))
        val pending = assertIs<AndroidStudyState.Typing>(f.facade.submitTypingIfCorrect(exact))
        assertTrue(pending.canonicalRatingTransitionEligible)
        assertIs<AndroidStudyState.Typing>(f.facade.commitTypingRating(pending, ReviewRating.HARD))
        val history = f.context.engine.getReviewHistory(f.learner, f.itemId)
        assertEquals(before + 1, history.size)
        assertEquals(ReviewRating.HARD, history.last().rating)
    }

    @Test fun `quick review unrated next mutates only queue while explicit rating commits once`() {
        val f = fixture(
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW,
            focusedPracticeKind = vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW
        )
        val initial = assertIs<AndroidStudyState.Introduction>(f.facade.load())
        assertEquals(ReviewRating.GOOD, initial.latestEffectiveRating)
        assertEquals("Quick Review", initial.hud?.skimStatus)
        val historyBefore = f.context.engine.getReviewHistory(f.learner, f.itemId)
        val memoryBefore = f.context.memoryStateRepository!!.find(f.learner, f.itemId)
        val revealed = assertIs<AndroidStudyState.Introduction>(f.facade.revealIntroduction(initial))
        val skimmed = assertIs<AndroidStudyState.Introduction>(f.facade.next(revealed))
        assertEquals(historyBefore, f.context.engine.getReviewHistory(f.learner, f.itemId))
        assertEquals(memoryBefore, f.context.memoryStateRepository!!.find(f.learner, f.itemId))

        val ratedReveal = assertIs<AndroidStudyState.Introduction>(f.facade.revealIntroduction(skimmed))
        val repeated = assertIs<AndroidStudyState.Introduction>(
            f.facade.rateIntroduction(ratedReveal, ReviewRating.AGAIN)
        )
        val history = f.context.engine.getReviewHistory(f.learner, f.itemId)
        assertEquals(historyBefore.size + 1, history.size)
        assertEquals(ReviewRating.AGAIN, history.last().rating)
        assertEquals(ReviewRating.AGAIN, repeated.latestEffectiveRating)
    }

    @Test fun `quick review commits each explicit canonical rating exactly once`() {
        ReviewRating.entries.forEach { rating ->
            val f = fixture(
                practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW,
                focusedPracticeKind = vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW
            )
            val initial = assertIs<AndroidStudyState.Introduction>(f.facade.load())
            val before = f.context.engine.getReviewHistory(f.learner, f.itemId).size
            val memoryBefore = f.context.memoryStateRepository!!.find(f.learner, f.itemId)
            val revealed = assertIs<AndroidStudyState.Introduction>(f.facade.revealIntroduction(initial))

            assertIs<AndroidStudyState.Introduction>(f.facade.rateIntroduction(revealed, rating))

            val history = f.context.engine.getReviewHistory(f.learner, f.itemId)
            assertEquals(before + 1, history.size, "rating=$rating")
            assertEquals(rating, history.last().rating)
            assertNotEquals(memoryBefore, f.context.memoryStateRepository!!.find(f.learner, f.itemId))
        }
    }

    @Test fun `quick review progress follows canonical queue position pool and round reset`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val sessionId = SessionId("quick-progress-session")
        val contentIds = listOf(ContentId("quick-progress-a"), ContentId("quick-progress-b"))
        val itemIds = listOf(LearningItemId("quick-progress-item-a"), LearningItemId("quick-progress-item-b"))
        itemIds.indices.forEach { index ->
            context.contentRepository!!.save(Content(
                contentIds[index], ContentType.WORD, ContentText("word-$index", "meaning-$index")
            ))
            context.learningItemRepository!!.save(LearningItem(
                itemIds[index], contentIds[index], LearningMode.MEANING_RECOGNITION
            ))
            context.engine.review(ReviewCommand(
                ReviewEventId("quick-progress-seed-$index"), learner, itemIds[index],
                ReviewRating.GOOD, Moment(1_000L + index)
            ))
        }
        context.studySessionRepository!!.save(StudySession.start(
            sessionId, learner, Moment(2_000), SessionPolicy(
                newItemLimit = 0, reviewItemLimit = 2, allowRepeatInSameSession = true,
                evaluationPolicy = SessionEvaluationPolicy.EVALUATIVE,
                practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW,
                focusedPracticeKind = FocusedPracticeKind.QUICK_REVIEW
            ), contentIds.toSet()
        ))
        context.studyQueue.create(
            sessionId, Moment(2_000), itemIds, itemIds.associateWith { SessionItemOrigin.REVIEW },
            itemIds.indices.associate { itemIds[it] to contentIds[it] }, configuredReviewTarget = 2,
            effectiveReviewWorkload = 2, practiceSeed = 7,
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
        )
        val facade = AndroidStudyFacade(context, learner, now = { 3_000 })

        val first = assertIs<AndroidStudyState.Introduction>(facade.load(sessionId.value))
        assertEquals(1, first.quickReviewPassPosition)
        assertEquals(2, first.quickReviewPoolSize)
        val second = assertIs<AndroidStudyState.Introduction>(facade.next(first))
        assertEquals(2, second.quickReviewPassPosition)
        assertEquals(2, second.quickReviewPoolSize)
        val nextRound = assertIs<AndroidStudyState.Introduction>(facade.next(second))
        assertEquals(1, nextRound.quickReviewPassPosition)
        assertEquals(2, nextRound.quickReviewPoolSize)

        val revealed = assertIs<AndroidStudyState.Introduction>(facade.revealIntroduction(nextRound))
        val reinforced = assertIs<AndroidStudyState.Introduction>(
            facade.rateIntroduction(revealed, ReviewRating.AGAIN)
        )
        val queue = context.studyQueue.get(sessionId)!!
        assertEquals(queue.practiceProgress?.position, reinforced.quickReviewPassPosition)
        assertEquals(queue.practiceProgress?.membershipSize, reinforced.quickReviewPoolSize)
        assertTrue(requireNotNull(reinforced.quickReviewPassPosition) <= requireNotNull(reinforced.quickReviewPoolSize))
    }

    private fun fixture(
        resolveMedia: (String) -> String? = { null },
        practiceLoopPolicy: PracticeLoopPolicy? = null,
        focusedPracticeKind: vn.loi.learning.domain.study.session.model.FocusedPracticeKind =
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.NONE
    ): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("android-content")
        val itemId = LearningItemId("android-item")
        context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("hello", "xin chào")))
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        context.engine.review(ReviewCommand(ReviewEventId("seed-android-item"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("android-session-${practiceLoopPolicy?.name ?: "review"}")
        if (practiceLoopPolicy == null) {
            val session = StudySession.start(
                sessionId, learner, Moment(1_000),
                SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId)
            )
            context.studySessionRepository!!.save(session)
            context.studyQueue.create(
                sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1
            )
        } else {
            val session = StudySession.start(
                sessionId, learner, Moment(1_000),
                SessionPolicy(
                    newItemLimit = 0,
                    reviewItemLimit = 1,
                    allowRepeatInSameSession = true,
                    evaluationPolicy = if (practiceLoopPolicy == PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW)
                        SessionEvaluationPolicy.EVALUATIVE else SessionEvaluationPolicy.PRACTICE_ONLY,
                    practiceLoopPolicy = practiceLoopPolicy,
                    focusedPracticeKind = focusedPracticeKind
                ),
                includedContentIds = setOf(contentId),
                studyMode = StudyMode.TYPING
            )
            context.studySessionRepository!!.save(session)
            context.studyQueue.create(
                sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW),
                mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1,
                practiceSeed = 1L, practiceLoopPolicy = practiceLoopPolicy
            )
        }
        return Fixture(context, learner, itemId, AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia))
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val learner: LearnerId,
        val itemId: LearningItemId,
        val facade: AndroidStudyFacade
    )

    private class CountingContentRepository(
        private val delegate: ContentRepository
    ) : ContentRepository by delegate {
        var bulkCallCount = 0
            private set
        var singleCallCount = 0
            private set
        var lastRequestedIds: List<ContentId> = emptyList()
            private set

        override fun findById(contentId: ContentId): Content? {
            singleCallCount += 1
            return delegate.findById(contentId)
        }

        override fun findByIds(contentIds: Collection<ContentId>): List<Content> {
            bulkCallCount += 1
            lastRequestedIds = contentIds.toList()
            return delegate.findByIds(contentIds)
        }
    }
}
