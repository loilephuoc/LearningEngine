package vn.loi.learning.android.study

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant
import java.time.ZoneId
import vn.loi.learning.application.learningdashboard.LearningDashboardQuery
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.recall.*
import vn.loi.learning.application.session.*
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.application.packageprogress.StudySessionProgressSource
import vn.loi.learning.application.packageprogress.StudyStatisticsScope
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext

data class AndroidSessionEntryAvailability(
    val canStartReview: Boolean,
    val canResume: Boolean,
    val canStartLatestSessionPractice: Boolean,
    val canStartDifficultPractice: Boolean,
    val canStartLearnedReview: Boolean
)

sealed interface AndroidHomePrimaryAction {
    data class Resume(val sessionId: String) : AndroidHomePrimaryAction
    data object ReviewDue : AndroidHomePrimaryAction
    data object StartLearning : AndroidHomePrimaryAction
    data object OpenLibrary : AndroidHomePrimaryAction
}

internal fun selectHomePrimaryAction(
    activeSessionId: String?,
    dueCount: Int,
    hasStudyScope: Boolean
): AndroidHomePrimaryAction = when {
    activeSessionId != null -> AndroidHomePrimaryAction.Resume(activeSessionId)
    dueCount > 0 && hasStudyScope -> AndroidHomePrimaryAction.ReviewDue
    hasStudyScope -> AndroidHomePrimaryAction.StartLearning
    else -> AndroidHomePrimaryAction.OpenLibrary
}

data class AndroidHomeUiModel(
    val primaryAction: AndroidHomePrimaryAction,
    val contextTitle: String?,
    val installedPackageCount: Int,
    val dueCount: Int,
    val overdueCount: Int,
    val reviewedToday: Int,
    val accuracyPercent: Int?,
    val activeMemoryCount: Int,
    val totalMemoryCount: Int
) {
    val hasContent: Boolean get() = installedPackageCount > 0
    val hasDueReview: Boolean get() = dueCount > 0
    val learningProgress: Float get() =
        if (totalMemoryCount == 0) 0f else activeMemoryCount.toFloat() / totalMemoryCount
}

enum class AndroidSessionEntry { REVIEW, LATEST_SESSION, DIFFICULT, LEARNED }

data class AndroidStudySessionHud(
    val newCompleted: Int,
    val newTarget: Int,
    val newConfiguredTarget: Int,
    val reviewCompleted: Int,
    val reviewTarget: Int,
    val reviewConfiguredTarget: Int,
    val totalLearned: Int,
    val dueCount: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int
)

internal fun StudyHeaderStatistics.toAndroidStudySessionHud() = AndroidStudySessionHud(
    newCompleted = newCompleted,
    newTarget = newEffectiveWorkload,
    newConfiguredTarget = newConfiguredTarget,
    reviewCompleted = reviewCompleted,
    reviewTarget = reviewEffectiveWorkload,
    reviewConfiguredTarget = reviewConfiguredTarget,
    totalLearned = total,
    dueCount = dueCount,
    againCount = againCount,
    hardCount = hardCount,
    goodCount = goodCount,
    easyCount = easyCount
)

sealed interface AndroidStudyState {
    data object Loading : AndroidStudyState
    data class Home(val availability: AndroidSessionEntryAvailability, val model: AndroidHomeUiModel) : AndroidStudyState
    sealed interface Runtime : AndroidStudyState {
        val hud: AndroidStudySessionHud? get() = null
        val plan: RecallPlan? get() = null
        val completed: Boolean
        val outcome: RecallOutcome?
        val pronunciation: String? get() = null
        val meaning: String? get() = null
        val example: String? get() = null
        val translation: String? get() = null
        val resolvedPromptAudio: String? get() = null
        val resolvedExpectedAnswerAudio: String? get() = null
        val resolvedMeaningAudio: String? get() = null
        val resolvedExampleEnglishAudio: String? get() = null
        val resolvedExampleVietnameseAudio: String? get() = null
        val resolvedAnswerAudio: String? get() = resolvedExpectedAnswerAudio
        val resolvedExampleAudio: String? get() = resolvedExampleEnglishAudio
        val resolvedExampleTranslationAudio: String? get() = resolvedExampleVietnameseAudio
        val resolvedAudio: String? get() = resolvedPromptAudio
        val resolvedImage: String? get() = null
        val currentPosition: Int? get() = null
        val totalItems: Int? get() = null
        val contextTitle: String? get() = null
    }
    data class Introduction(
        val sessionId: String,
        val learningItemId: String,
        val contentId: String,
        val answerText: String = "",
        val revealedStage: Boolean = false,
        val originKind: SessionItemOrigin = SessionItemOrigin.NEW,
        override val pronunciation: String? = null,
        override val meaning: String? = null,
        val partOfSpeech: String? = null,
        override val example: String? = null,
        override val translation: String? = null,
        override val resolvedPromptAudio: String? = null,
        override val resolvedExpectedAnswerAudio: String? = null,
        override val resolvedMeaningAudio: String? = null,
        override val resolvedExampleEnglishAudio: String? = null,
        override val resolvedExampleVietnameseAudio: String? = null,
        override val resolvedImage: String? = null,
        override val currentPosition: Int? = null,
        override val totalItems: Int? = null,
        override val contextTitle: String? = null,
        override val hud: AndroidStudySessionHud? = null,
        override val plan: RecallPlan? = null,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null
    ) : Runtime {
        val answer: String get() = answerText
        val revealed: Boolean get() = revealedStage
        val origin: SessionItemOrigin get() = originKind
    }
    data class Typing(
        override val plan: RecallPlan,
        val prompt: String,
        val answer: String = "",
        val evaluation: TypingAnswerEvaluationStatus = TypingAnswerEvaluationStatus.EMPTY,
        val revealed: Boolean = false,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val meaning: String? = null,
        override val example: String? = null,
        override val translation: String? = null,
        override val resolvedPromptAudio: String? = null,
        override val resolvedExpectedAnswerAudio: String? = null,
        override val resolvedMeaningAudio: String? = null,
        override val resolvedExampleEnglishAudio: String? = null,
        override val resolvedExampleVietnameseAudio: String? = null,
        override val resolvedImage: String? = null,
        override val currentPosition: Int? = null,
        override val totalItems: Int? = null,
        override val contextTitle: String? = null,
        override val hud: AndroidStudySessionHud? = null
    ) : Runtime
    data class MultipleChoice(
        override val plan: RecallPlan,
        val question: String,
        val choices: List<RecallChoice>,
        val selectedChoiceId: String? = null,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val meaning: String? = null,
        override val example: String? = null,
        override val translation: String? = null,
        override val resolvedPromptAudio: String? = null,
        override val resolvedExpectedAnswerAudio: String? = null,
        override val resolvedMeaningAudio: String? = null,
        override val resolvedExampleEnglishAudio: String? = null,
        override val resolvedExampleVietnameseAudio: String? = null,
        override val resolvedImage: String? = null,
        override val currentPosition: Int? = null,
        override val totalItems: Int? = null,
        override val contextTitle: String? = null,
        override val hud: AndroidStudySessionHud? = null
    ) : Runtime
    data class Listening(
        override val plan: RecallPlan,
        val audioPath: String?,
        val answer: String = "",
        val audioUnavailable: Boolean = audioPath == null,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val meaning: String? = null,
        override val example: String? = null,
        override val translation: String? = null,
        override val resolvedPromptAudio: String? = audioPath,
        override val resolvedExpectedAnswerAudio: String? = null,
        override val resolvedMeaningAudio: String? = null,
        override val resolvedExampleEnglishAudio: String? = null,
        override val resolvedExampleVietnameseAudio: String? = null,
        override val resolvedImage: String? = null,
        override val currentPosition: Int? = null,
        override val totalItems: Int? = null,
        override val contextTitle: String? = null,
        override val hud: AndroidStudySessionHud? = null
    ) : Runtime
    data class ImageRecall(
        override val plan: RecallPlan,
        val imagePath: String?,
        val answer: String = "",
        val imageUnavailable: Boolean = imagePath == null,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val meaning: String? = null,
        override val example: String? = null,
        override val translation: String? = null,
        override val resolvedPromptAudio: String? = null,
        override val resolvedExpectedAnswerAudio: String? = null,
        override val resolvedMeaningAudio: String? = null,
        override val resolvedExampleEnglishAudio: String? = null,
        override val resolvedExampleVietnameseAudio: String? = null,
        override val resolvedImage: String? = imagePath,
        override val currentPosition: Int? = null,
        override val totalItems: Int? = null,
        override val contextTitle: String? = null,
        override val hud: AndroidStudySessionHud? = null
    ) : Runtime
    data class ExampleCompletion(
        override val plan: RecallPlan,
        val prefix: String,
        val blank: String,
        val suffix: String,
        val answer: String = "",
        val revealed: Boolean = false,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val meaning: String? = null,
        override val example: String? = null,
        override val translation: String? = null,
        override val resolvedPromptAudio: String? = null,
        override val resolvedExpectedAnswerAudio: String? = null,
        override val resolvedMeaningAudio: String? = null,
        override val resolvedExampleEnglishAudio: String? = null,
        override val resolvedExampleVietnameseAudio: String? = null,
        override val resolvedImage: String? = null,
        override val currentPosition: Int? = null,
        override val totalItems: Int? = null,
        override val contextTitle: String? = null,
        override val hud: AndroidStudySessionHud? = null
    ) : Runtime
    data class Completion(val sessionId: String, val canUndo: Boolean) : AndroidStudyState
    data class Failed(
        val message: String,
        val retrySessionId: String? = null,
        val retryable: Boolean = true
    ) : AndroidStudyState
}

/** Thin platform facade: Shared Application owns planning, evaluation, learning and queue mutation. */
class AndroidStudyFacade(
    private val context: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val now: () -> Long = System::currentTimeMillis,
    private val resolveMedia: (String) -> String? = { null }
) {
    private val typingEvaluator = TypingAnswerEvaluator()
    private val attemptSequence = AtomicLong()
    private val submittedPlans = mutableSetOf<RecallPlanId>()
    private val submittedItems = mutableSetOf<String>()
    private var currentItem: NextSessionItem? = null

    fun home(): AndroidStudyState.Home {
        val active = reconcileActiveSession()
        val scope = currentScope()
        val packages = context.installedPackages.query()
        val availability = scope?.let {
            context.engine.getLearnEntryReviewAvailability(it, Moment(now()))
        }
        val nowMillis = now()
        val startOfDay = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
            .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dashboard = context.dashboard.query(
            LearningDashboardQuery(
                learnerId = learnerId,
                activityFrom = Moment(startOfDay),
                activityUntil = Moment(nowMillis + 1),
                at = Moment(nowMillis),
                forecastWindowEnds = emptyList()
            )
        )
        val due = dashboard.scheduling.dueStatistics
        val progress = dashboard.activity.progress
        val memories = dashboard.memory.stageCounts
        val primaryAction = selectHomePrimaryAction(active?.id?.value, due.dueCount, scope != null)
        return AndroidStudyState.Home(
            AndroidSessionEntryAvailability(
                canStartReview = scope != null && active == null,
                canResume = active != null,
                canStartLatestSessionPractice = availability?.latestCompletedNewItems is LatestCompletedNewItemsAvailability.Available && active == null,
                canStartDifficultPractice = availability?.difficultItems is DifficultItemsReviewAvailability.Available && active == null,
                canStartLearnedReview = availability?.learnedItems is LearnedItemsReviewAvailability.Available && active == null
            ),
            model = AndroidHomeUiModel(
                primaryAction = primaryAction,
                contextTitle = (active?.installedPackageId ?: scope?.installedPackageId)
                    ?.let { packageId -> packages.firstOrNull { it.id == packageId.value }?.name }
                    ?: packages.firstOrNull()?.name,
                installedPackageCount = packages.size,
                dueCount = due.dueCount,
                overdueCount = due.overdueCount,
                reviewedToday = progress.totalReviews,
                accuracyPercent = progress.accuracy?.let { (it * 100).toInt() },
                activeMemoryCount = memories.activeMemories,
                totalMemoryCount = memories.totalMemories
            )
        )
    }

    fun start(entry: AndroidSessionEntry, mode: StudyMode = StudyMode.ADAPTIVE): AndroidStudyState {
        val scope = currentScope() ?: return AndroidStudyState.Failed("No active content package.")
        val requestedAt = Moment(now())
        val session = when (entry) {
            AndroidSessionEntry.REVIEW -> context.engine.startSession(
                StartStudySessionCommand(
                    SessionId(UUID.randomUUID().toString()), learnerId, requestedAt,
                    installedPackageId = scope.installedPackageId, topicId = scope.topicId,
                    studyMode = mode
                )
            )
            AndroidSessionEntry.LATEST_SESSION -> when (val result = context.engine.startLatestCompletedNewItemsReview(
                StartLatestCompletedNewItemsReviewRequest(scope, requestedAt)
            )) {
                is StartLatestCompletedNewItemsReviewResult.Accepted -> result.session
                else -> return AndroidStudyState.Failed("Latest-session Practice is unavailable.")
            }
            AndroidSessionEntry.DIFFICULT -> when (val result = context.engine.startDifficultItemsReview(
                StartDifficultItemsReviewRequest(scope, requestedAt)
            )) {
                is StartDifficultItemsReviewResult.Accepted -> result.session
                else -> return AndroidStudyState.Failed("Again/Hard Practice is unavailable.")
            }
            AndroidSessionEntry.LEARNED -> when (val result = context.engine.startLearnedItemsReview(
                StartLearnedItemsReviewRequest(scope, requestedAt)
            )) {
                is StartLearnedItemsReviewResult.Accepted -> result.session
                else -> return AndroidStudyState.Failed("Learned-item Review is unavailable.")
            }
        }
        return load(session.id.value)
    }

    fun load(restoredSessionId: String? = null): AndroidStudyState {
        reconcileActiveSession()
        val session = restoredSessionId?.let(::SessionId)?.let(context.engine::getSession)
            ?: context.engine.getActiveSession(learnerId)
            ?: return home()
        if (session.status == SessionStatus.FINISHED) return completeExhaustedSession(session)
        val next = context.engine.getNextSessionItem(session.id, Moment(now()))
            ?: return completeExhaustedSession(session)
        currentItem = next
        if (next.item.isNew && next.item.content.id !in next.session.introducedContentIds) {
            return attachHud(buildIntroduction(next, revealed = next.session.answerRevealed), next.session)
        }
        val plan = createPlan(next) ?: return AndroidStudyState.Failed("Shared recall planning is unavailable.")
        return attachHud(present(plan), next.session)
    }

    fun loadExact(sessionId: String): AndroidStudyState {
        reconcileActiveSession()
        val session = context.engine.getSession(SessionId(sessionId))
            ?: return AndroidStudyState.Failed("Study session is unavailable. Return to Library and try again.", sessionId)
        if (session.status == SessionStatus.FINISHED) return completeExhaustedSession(session)
        val next = context.engine.getNextSessionItem(session.id, Moment(now()))
            ?: return completeExhaustedSession(session)
        currentItem = next
        if (next.item.isNew && next.item.content.id !in next.session.introducedContentIds) {
            return attachHud(buildIntroduction(next, revealed = next.session.answerRevealed), next.session)
        }
        val plan = createPlan(next)
            ?: return AndroidStudyState.Failed("Shared recall planning is unavailable for this session.", sessionId)
        return attachHud(present(plan), next.session)
    }

    private fun attachHud(state: AndroidStudyState, session: StudySession): AndroidStudyState {
        val runtime = state as? AndroidStudyState.Runtime ?: return state
        val query = context.studyHeaderStatistics ?: return state
        val queue = context.engine.getStudyQueueProgress(session.id) ?: return state
        val scope = resolveStatisticsScope(session) ?: return state
        val source = StudySessionProgressSource(
            sessionId = session.id.value,
            newConfiguredTarget = session.policy.newItemLimit,
            reviewConfiguredTarget = session.policy.reviewItemLimit,
            newEffectiveWorkload = queue.effectiveNewWorkload,
            reviewEffectiveWorkload = queue.effectiveReviewWorkload,
            newCompleted = session.newItemsReviewed,
            reviewCompleted = session.reviewItemsReviewed,
            remainingLearningItemIds = queue.remainingLearningItemIds.toSet(),
            remainingItemOrigins = queue.itemOrigins,
            remainingItemContentIds = queue.itemContentIds
        )
        val hud = runCatching { query.execute(scope, source, learnerId).toAndroidStudySessionHud() }
            .getOrNull() ?: return state
        return when (runtime) {
            is AndroidStudyState.Introduction -> runtime.copy(hud = hud)
            is AndroidStudyState.Typing -> runtime.copy(hud = hud)
            is AndroidStudyState.MultipleChoice -> runtime.copy(hud = hud)
            is AndroidStudyState.Listening -> runtime.copy(hud = hud)
            is AndroidStudyState.ImageRecall -> runtime.copy(hud = hud)
            is AndroidStudyState.ExampleCompletion -> runtime.copy(hud = hud)
        }
    }

    private fun resolveStatisticsScope(session: StudySession): StudyStatisticsScope? {
        session.installedPackageId?.let { packageId ->
            val contentIds = context.packageContentQuery
                ?.getContentsForPackage(packageId)
                ?.mapTo(linkedSetOf()) { ContentId(it.id) }
                ?: return null
            return StudyStatisticsScope("package:${packageId.value}", contentIds)
        }
        return session.includedContentIds.takeIf { it.isNotEmpty() }
            ?.let { StudyStatisticsScope("session:${session.id.value}", it) }
    }

    fun revealIntroduction(state: AndroidStudyState.Introduction): AndroidStudyState {
        val session = context.engine.completeContentIntroduction(
            sessionId = SessionId(state.sessionId),
            contentId = ContentId(state.contentId),
            learningItemId = LearningItemId(state.learningItemId)
        )
        currentItem = currentItem?.copy(session = session)
        return state.copy(revealedStage = true)
    }

    fun rateIntroduction(state: AndroidStudyState.Introduction, rating: ReviewRating): AndroidStudyState {
        val submissionKey = "${state.sessionId}:${state.learningItemId}"
        if (submissionKey in submittedItems) return state
        submittedItems += submissionKey
        return try {
            val sessionId = SessionId(state.sessionId)
            val learningItemId = LearningItemId(state.learningItemId)
            val updatedSession = if (state.revealed) {
                requireNotNull(context.engine.getSession(sessionId)) { "Study session is unavailable." }
            } else {
                context.engine.completeContentIntroduction(
                    sessionId = sessionId,
                    contentId = ContentId(state.contentId),
                    learningItemId = learningItemId
                )
            }
            currentItem = currentItem?.copy(session = updatedSession)
            context.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId(UUID.randomUUID().toString()),
                    learningItemId = learningItemId,
                    rating = rating,
                    reviewedAt = Moment(now()),
                    ratingSource = RatingSource.STANDARD_REVIEW
                )
            )
            load(state.sessionId)
        } catch (failure: RuntimeException) {
            submittedItems -= submissionKey
            throw failure
        }
    }

    private fun buildIntroduction(next: NextSessionItem, revealed: Boolean): AndroidStudyState.Introduction {
        val content = next.item.content
        val pronunciation = content.text.pronunciation
        val meaning = content.text.translatedText
        val example = content.text.exampleText
        val translation = content.text.exampleTranslation
        val answer = content.text.primaryText
        val partOfSpeech = content.metadata.tags.firstOrNull { it.startsWith("pos:") }?.removePrefix("pos:")
            ?: content.metadata.tags.firstOrNull { it in setOf("noun", "verb", "adjective", "adverb") }
        val promptAudio = content.media.primaryAudio?.let(resolveMedia)
        val expectedAnswerAudio = content.media.primaryAudio?.let(resolveMedia)
        val meaningAudio = content.media.translatedAudio?.let(resolveMedia)
        val exampleEnglishAudio = content.media.exampleAudio?.let(resolveMedia)
        val exampleVietnameseAudio = content.media.exampleTranslatedAudio?.let(resolveMedia)
        val mediaImage = content.media.image?.let(resolveMedia)
        val currentPos = next.progress?.currentPosition
        val totalCount = next.progress?.totalItemCount
        val title = next.session.installedPackageId?.value?.let { pkgId ->
            runCatching { context.installedPackages.query().firstOrNull { it.id == pkgId }?.name }.getOrNull()
        } ?: runCatching { context.installedPackages.query().firstOrNull()?.name }.getOrNull()

        return AndroidStudyState.Introduction(
            sessionId = next.session.id.value,
            learningItemId = next.item.learningItem.id.value,
            contentId = content.id.value,
            answerText = answer,
            revealedStage = revealed,
            originKind = next.origin,
            pronunciation = pronunciation,
            meaning = meaning,
            partOfSpeech = partOfSpeech,
            example = example,
            translation = translation,
            resolvedPromptAudio = promptAudio,
            resolvedExpectedAnswerAudio = expectedAnswerAudio,
            resolvedMeaningAudio = meaningAudio,
            resolvedExampleEnglishAudio = exampleEnglishAudio,
            resolvedExampleVietnameseAudio = exampleVietnameseAudio,
            resolvedImage = mediaImage,
            currentPosition = currentPos,
            totalItems = totalCount,
            contextTitle = title
        )
    }

    fun updateAnswer(state: AndroidStudyState.Runtime, answer: String): AndroidStudyState.Runtime = when (state) {
        is AndroidStudyState.Introduction -> state
        is AndroidStudyState.Typing -> {
            if (state.completed || state.revealed) state else state.copy(
                answer = answer,
                evaluation = typingEvaluator.evaluate(TypingRecallPrompt(state.plan?.answerContract?.canonicalAnswer.orEmpty()), answer).status
            )
        }
        is AndroidStudyState.Listening -> if (state.completed) state else state.copy(answer = answer)
        is AndroidStudyState.ImageRecall -> if (state.completed) state else state.copy(answer = answer)
        is AndroidStudyState.ExampleCompletion -> if (state.completed || state.revealed) state else state.copy(answer = answer)
        is AndroidStudyState.MultipleChoice -> state
    }

    fun submitTypingIfCorrect(state: AndroidStudyState.Typing): AndroidStudyState {
        val evaluation = typingEvaluator.evaluate(TypingRecallPrompt(state.plan.answerContract.canonicalAnswer), state.answer)
        return if (!evaluation.isCorrect) state.copy(evaluation = evaluation.status)
        else execute(state, typedSubmission(state.plan, state.answer))
    }

    fun choose(state: AndroidStudyState.MultipleChoice, choiceId: String): AndroidStudyState =
        execute(state.copy(selectedChoiceId = choiceId), RecallSubmission.Choice(submissionContext(state.plan), choiceId))

    fun submitText(state: AndroidStudyState.Runtime, typedAnswer: String? = null): AndroidStudyState {
        val answerToUse = typedAnswer ?: when (state) {
            is AndroidStudyState.Introduction -> return state
            is AndroidStudyState.Typing -> state.answer
            is AndroidStudyState.Listening -> state.answer
            is AndroidStudyState.ImageRecall -> state.answer
            is AndroidStudyState.ExampleCompletion -> state.answer
            is AndroidStudyState.MultipleChoice -> return state
        }
        if (answerToUse.isBlank()) return state
        val updatedState = updateAnswer(state, answerToUse)
        val plan = state.plan ?: return state
        return execute(updatedState, typedSubmission(plan, answerToUse))
    }

    fun reveal(state: AndroidStudyState.Runtime, typedAnswer: String? = null): AndroidStudyState {
        if (state is AndroidStudyState.Introduction) return revealIntroduction(state)
        val updatedState = if (typedAnswer != null) updateAnswer(state, typedAnswer) else state
        val plan = state.plan ?: return state
        return execute(updatedState, RecallSubmission.Reveal(submissionContext(plan, RecallAssistance.ANSWER_REVEALED)))
    }

    fun next(state: AndroidStudyState.Runtime): AndroidStudyState {
        val plan = state.plan
        return if (state is AndroidStudyState.Introduction) load(state.sessionId)
        else if (state.completed && plan != null) load(plan.sessionId.value) else state
    }

    fun overridePracticeRating(state: AndroidStudyState.Runtime, rating: ReviewRating): AndroidStudyState {
        val item = currentItem ?: return state
        if (item.session.policy.evaluationPolicy != SessionEvaluationPolicy.PRACTICE_ONLY) return state
        val currentRating = context.engine.getContentLearningState(learnerId, item.item.content.id).latestEffectiveRating
        context.engine.overridePracticeItemRating(
            ManualRatingOverrideCommand(
                item.session.id, ReviewEventId(UUID.randomUUID().toString()), item.item.learningItem.id,
                currentRating, rating, Moment(now())
            )
        )
        return load(item.session.id.value)
    }

    fun undo(state: AndroidStudyState): AndroidStudyState {
        val sessionId = when (state) {
            is AndroidStudyState.Runtime -> state.plan?.sessionId ?: (state as? AndroidStudyState.Introduction)?.let { SessionId(it.sessionId) }
            is AndroidStudyState.Completion -> SessionId(state.sessionId)
            else -> return state
        } ?: return state
        return when (context.engine.undoLatestSessionReview(sessionId)) {
            UndoLatestSessionReviewResult.NothingToUndo -> state
            is UndoLatestSessionReviewResult.Undone -> {
                submittedItems.removeAll { it.startsWith("${sessionId.value}:") }
                load(sessionId.value)
            }
        }
    }

    internal fun present(plan: RecallPlan): AndroidStudyState {
        val item = currentItem
        val content = item?.item?.content
        val pronunciation = content?.text?.pronunciation
        val meaning = content?.text?.translatedText
        val example = content?.text?.exampleText
        val translation = content?.text?.exampleTranslation
        val promptAudio = content?.media?.primaryAudio?.let(resolveMedia)
        val expectedAnswerAudio = content?.media?.primaryAudio?.let(resolveMedia)
        val meaningAudio = content?.media?.translatedAudio?.let(resolveMedia)
        val exampleEnglishAudio = content?.media?.exampleAudio?.let(resolveMedia)
        val exampleVietnameseAudio = content?.media?.exampleTranslatedAudio?.let(resolveMedia)
        val mediaImage = content?.media?.image?.let(resolveMedia)
        val currentPos = item?.progress?.currentPosition
        val totalCount = item?.progress?.totalItemCount
        val title = item?.session?.installedPackageId?.value?.let { pkgId ->
            runCatching { context.installedPackages.query().firstOrNull { it.id == pkgId }?.name }.getOrNull()
        } ?: runCatching { context.installedPackages.query().firstOrNull()?.name }.getOrNull()

        return when (val prompt = plan.prompt) {
            is RecallPrompt.Typing -> AndroidStudyState.Typing(
                plan, prompt.sourceText,
                pronunciation = pronunciation, meaning = meaning, example = example, translation = translation,
                resolvedPromptAudio = promptAudio, resolvedExpectedAnswerAudio = expectedAnswerAudio,
                resolvedMeaningAudio = meaningAudio, resolvedExampleEnglishAudio = exampleEnglishAudio,
                resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                resolvedImage = mediaImage,
                currentPosition = currentPos, totalItems = totalCount, contextTitle = title
            )
            is RecallPrompt.MultipleChoice -> AndroidStudyState.MultipleChoice(
                plan, prompt.question, prompt.choices,
                pronunciation = pronunciation, meaning = meaning, example = example, translation = translation,
                resolvedPromptAudio = promptAudio, resolvedExpectedAnswerAudio = expectedAnswerAudio,
                resolvedMeaningAudio = meaningAudio, resolvedExampleEnglishAudio = exampleEnglishAudio,
                resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                resolvedImage = mediaImage,
                currentPosition = currentPos, totalItems = totalCount, contextTitle = title
            )
            is RecallPrompt.Listening -> {
                val audioPath = resolveMedia(prompt.audio.value) ?: promptAudio
                AndroidStudyState.Listening(
                    plan, audioPath,
                    pronunciation = pronunciation, meaning = meaning, example = example, translation = translation,
                    resolvedPromptAudio = audioPath, resolvedExpectedAnswerAudio = expectedAnswerAudio,
                    resolvedMeaningAudio = meaningAudio, resolvedExampleEnglishAudio = exampleEnglishAudio,
                    resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                    resolvedImage = mediaImage,
                    currentPosition = currentPos, totalItems = totalCount, contextTitle = title
                )
            }
            is RecallPrompt.ImageRecall -> {
                val imagePath = resolveMedia(prompt.image.value) ?: mediaImage
                AndroidStudyState.ImageRecall(
                    plan, imagePath,
                    pronunciation = pronunciation, meaning = meaning, example = example, translation = translation,
                    resolvedPromptAudio = promptAudio, resolvedExpectedAnswerAudio = expectedAnswerAudio,
                    resolvedMeaningAudio = meaningAudio, resolvedExampleEnglishAudio = exampleEnglishAudio,
                    resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                    resolvedImage = imagePath,
                    currentPosition = currentPos, totalItems = totalCount, contextTitle = title
                )
            }
            is RecallPrompt.ExampleCompletion -> AndroidStudyState.ExampleCompletion(
                plan,
                prompt.example.substring(0, prompt.targetSpan.startInclusive),
                prompt.example.substring(prompt.targetSpan.startInclusive, prompt.targetSpan.endExclusive),
                prompt.example.substring(prompt.targetSpan.endExclusive),
                pronunciation = pronunciation, meaning = meaning, example = example, translation = translation,
                resolvedPromptAudio = promptAudio, resolvedExpectedAnswerAudio = expectedAnswerAudio,
                resolvedMeaningAudio = meaningAudio, resolvedExampleEnglishAudio = exampleEnglishAudio,
                resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                resolvedImage = mediaImage,
                currentPosition = currentPos, totalItems = totalCount, contextTitle = title
            )
            else -> AndroidStudyState.Failed("${plan.mode.wireId} is not available on Android.")
        }
    }

    private fun execute(state: AndroidStudyState.Runtime, submission: RecallSubmission): AndroidStudyState {
        val plan = state.plan ?: return state
        if (state.completed || plan.planId in submittedPlans) return state
        val item = currentItem ?: return AndroidStudyState.Failed("Study item is unavailable.")
        submittedPlans += plan.planId
        val strategyContext = strategyContext(item.session)
        val result = context.engine.executeRecall(
            RecallExecutionRequest(plan, submission, evaluationContext = strategyContext)
        ) as? RecallExecutionResult.Completed
            ?: return AndroidStudyState.Failed("Shared recall execution rejected the attempt.")
        val learning = context.engine.executeRecallLearning(
            RecallLearningExecutionRequest(
                result.result, item.session.id, item.item.learningItem.id, learnerId,
                item.item.content.id, strategyContext
            )
        )
        if (learning !is RecallLearningExecutionResult.Committed && learning !is RecallLearningExecutionResult.PracticeRecorded) {
            return AndroidStudyState.Failed("Shared learning execution rejected the attempt.")
        }
        val updatedState = when (state) {
            is AndroidStudyState.Introduction -> state
            is AndroidStudyState.Typing -> state.copy(
                revealed = submission is RecallSubmission.Reveal, completed = true, outcome = result.result.outcome
            )
            is AndroidStudyState.MultipleChoice -> state.copy(completed = true, outcome = result.result.outcome)
            is AndroidStudyState.Listening -> state.copy(completed = true, outcome = result.result.outcome)
            is AndroidStudyState.ImageRecall -> state.copy(completed = true, outcome = result.result.outcome)
            is AndroidStudyState.ExampleCompletion -> state.copy(
                revealed = submission is RecallSubmission.Reveal, completed = true, outcome = result.result.outcome
            )
        }
        val committedSession = context.engine.getSession(item.session.id) ?: item.session
        return attachHud(updatedState, committedSession)
    }

    private fun submissionContext(plan: RecallPlan, assistance: RecallAssistance = RecallAssistance.NONE) =
        RecallSubmissionContext(
            plan.planId, RecallAttemptId("android-${attemptSequence.incrementAndGet()}"), plan.learnerId,
            plan.contentId, plan.sessionId, plan.mode, Moment(maxOf(now(), plan.generatedAt.epochMillis)),
            setOf(assistance), RecallPlatformKind.ANDROID
        )

    private fun typedSubmission(plan: RecallPlan, answer: String) =
        RecallSubmission.TypedText(submissionContext(plan), answer)

    private fun createPlan(next: NextSessionItem): RecallPlan? {
        val generatedAt = Moment(now())
        val scope = context.contentRepository?.findByIds(next.session.includedContentIds).orEmpty()
            .ifEmpty { listOf(next.item.content) }
        val result = context.engine.createProductionRecallPlan(
            ProductionRecallPlanRequest(
                learnerId, next.item.content, next.item.learningItem.id, next.session.id,
                RecallAttemptNonce("${next.session.totalReviews}-${next.item.learningItem.id.value}-${next.progress?.currentPosition ?: 0}"),
                MultipleChoiceScopeId(next.session.id.value), scope, null, null,
                RecallPromotionEvidenceContext(strategyContext(next.session)),
                planPolicy = RecallPlanPolicy(
                    sourceLanguage = RecallLanguageTag("en"),
                    targetLanguage = RecallLanguageTag("vi"),
                    punctuationPolicy = PunctuationPolicy.EXACT
                ),
                deterministicSeed = RecallDeterministicSeed(
                    (next.session.id.value + next.item.learningItem.id.value + next.session.totalReviews).hashCode().toLong()
                ),
                generatedAt = generatedAt,
                recentModeHistory = next.session.recallModeHistory,
                studyMode = next.session.studyMode
            )
        )
        return (result as? ProductionRecallPlanResult.Created)?.plan
    }

    private fun currentScope(): LearnEntryScope? {
        val libraryId = context.defaultLibraryId ?: return null
        val pkg = context.installedPackageRepository
            ?.findAllByLibraryIdAndState(libraryId, PackageState.ACTIVE)
            ?.sortedBy { it.id.value }
            ?.firstOrNull() ?: return null
        return LearnEntryScope(learnerId, pkg.id, pkg.topicId)
    }

    private fun completeExhaustedSession(session: StudySession): AndroidStudyState.Completion {
        val completed = if (session.status == SessionStatus.ACTIVE) {
            context.engine.finishSession(session.id, Moment(now()))
        } else session
        return AndroidStudyState.Completion(completed.id.value, completed.undoableReview != null)
    }

    private fun reconcileActiveSession(): StudySession? =
        context.activeStudySessionScopeReconciler?.reconcile(learnerId, Moment(now()))
            ?: context.engine.getActiveSession(learnerId)

    private fun strategyContext(session: StudySession) =
        if (session.policy.evaluationPolicy == SessionEvaluationPolicy.PRACTICE_ONLY)
            RecallStrategyContext.PRACTICE_ONLY else RecallStrategyContext.EVALUATIVE
}
