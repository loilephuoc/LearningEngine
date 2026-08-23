package vn.loi.learning.android.study

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant
import java.time.ZoneId
import vn.loi.learning.application.study.DailyStudyBudgetLimits
import vn.loi.learning.application.study.DailyStudyBudgetSnapshot
import vn.loi.learning.application.learningdashboard.LearningDashboardQuery
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.typing.*
import vn.loi.learning.application.contentpackaging.browser.LegacyExampleTranslationProjection
import vn.loi.learning.application.recall.*
import vn.loi.learning.application.session.*
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.application.packageprogress.StudySessionProgressSource
import vn.loi.learning.application.packageprogress.StudyStatisticsScope
import vn.loi.learning.application.partofspeech.PartOfSpeechExtractor
import vn.loi.learning.application.contentpackaging.InstalledPackageItem
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.android.platform.AndroidStartupTrace


internal fun resolveIntroductionPartOfSpeech(content: vn.loi.learning.domain.content.model.Content): String? =
    PartOfSpeechExtractor.extract(content)
        .sortedBy { it.source.ordinal }
        .firstNotNullOfOrNull { observation ->
            observation.trimmedValue?.trim()?.takeIf(String::isNotBlank)
                ?: observation.canonical?.value?.takeIf(String::isNotBlank)
        }

data class AndroidSessionEntryAvailability(
    val canStartReview: Boolean,
    val canResume: Boolean,
    val canStartLatestSessionPractice: Boolean,
    val canStartDifficultPractice: Boolean,
    val canStartLearnedReview: Boolean,
    val canLearnNew: Boolean = false,
    val canStartAdaptive: Boolean = false,
    val canStartTyping: Boolean = false,
    val hasActiveSession: Boolean = false
)

sealed interface AndroidHomePrimaryAction {
    data class Resume(val sessionId: String) : AndroidHomePrimaryAction
    data object ReviewDue : AndroidHomePrimaryAction
    data object StartLearning : AndroidHomePrimaryAction
    data object DailyComplete : AndroidHomePrimaryAction
    data object OpenLibrary : AndroidHomePrimaryAction
}

internal fun selectHomePrimaryAction(
    activeSessionId: String?,
    dueCount: Int,
    hasStudyScope: Boolean,
    hasEligibleWork: Boolean = true
): AndroidHomePrimaryAction = when {
    activeSessionId != null -> AndroidHomePrimaryAction.Resume(activeSessionId)
    hasStudyScope && !hasEligibleWork -> AndroidHomePrimaryAction.DailyComplete
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
    val totalMemoryCount: Int,
    val activePackageName: String? = null,
    val activePackageContentCount: Int? = null,
    val dailyBudget: DailyStudyBudgetSnapshot? = null,
    val forecastInsights: vn.loi.learning.android.dashboard.AndroidForecastInsightsUiModel? = null
) {
    val hasContent: Boolean get() = installedPackageCount > 0
    val hasDueReview: Boolean get() = dueCount > 0
    val learningProgress: Float get() =
        if (totalMemoryCount == 0) 0f else activeMemoryCount.toFloat() / totalMemoryCount
}

enum class AndroidSessionEntry { REVIEW, LATEST_SESSION, DIFFICULT, LEARNED, QUICK_REVIEW }

data class AndroidStudyRuntimeIdentity(
    val studyMode: StudyMode,
    val evaluationPolicy: SessionEvaluationPolicy,
    val practiceLoopPolicy: PracticeLoopPolicy,
    val focusedPracticeKind: FocusedPracticeKind
) {
    companion object {
        fun from(session: StudySession) = AndroidStudyRuntimeIdentity(
            studyMode = session.studyMode,
            evaluationPolicy = session.policy.evaluationPolicy,
            practiceLoopPolicy = session.policy.practiceLoopPolicy,
            focusedPracticeKind = session.policy.focusedPracticeKind
        )
    }
}

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
    val easyCount: Int,
    val skimStatus: String? = null,
    val focusedPractice: Boolean = false
)

internal fun StudyHeaderStatistics.toAndroidStudySessionHud(daily: DailyStudyBudgetSnapshot? = null) = AndroidStudySessionHud(
    newCompleted = daily?.newCompletedToday ?: newCompleted,
    newTarget = daily?.limits?.newPerDay ?: newEffectiveWorkload,
    newConfiguredTarget = daily?.limits?.newPerDay ?: newConfiguredTarget,
    reviewCompleted = daily?.reviewCompletedToday ?: reviewCompleted,
    reviewTarget = daily?.limits?.reviewPerDay ?: reviewEffectiveWorkload,
    reviewConfiguredTarget = daily?.limits?.reviewPerDay ?: reviewConfiguredTarget,
    totalLearned = total,
    dueCount = dueCount,
    againCount = againCount,
    hardCount = hardCount,
    goodCount = goodCount,
    easyCount = easyCount
)

sealed interface AndroidStudyState {
    data object Loading : AndroidStudyState
    data class PreparingMode(val mode: StudyMode) : AndroidStudyState
    data class Home(val availability: AndroidSessionEntryAvailability, val model: AndroidHomeUiModel) : AndroidStudyState
    sealed interface Runtime : AndroidStudyState {
        val navigation: AndroidReviewNavigation get() = AndroidReviewNavigation()
        val hud: AndroidStudySessionHud? get() = null
        val plan: RecallPlan? get() = null
        val completed: Boolean
        val outcome: RecallOutcome?
        val pronunciation: String? get() = null
        val partOfSpeech: String? get() = null
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
        val runtimeIdentity: AndroidStudyRuntimeIdentity? get() = null
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
        override val partOfSpeech: String? = null,
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
        val packagePosition: Int? = null,
        val packageTotal: Int? = null,
        val presentationVisitId: String? = null,
        val quickReviewPassPosition: Int? = null,
        val quickReviewPoolSize: Int? = null,
        val latestEffectiveRating: ReviewRating? = null,
        val historyPreview: Boolean = false,
        val compactRatingExit: Boolean = false,
        val focusedPracticeKind: FocusedPracticeKind = FocusedPracticeKind.NONE,
        override val navigation: AndroidReviewNavigation = AndroidReviewNavigation(),
        override val contextTitle: String? = null,
        override val runtimeIdentity: AndroidStudyRuntimeIdentity? = null,
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
        override val partOfSpeech: String? = null,
        val attempt: TypingAttemptState? = null,
        val previousCanonicalRating: ReviewRating? = null,
        val canonicalRatingTransitionEligible: Boolean = false,
        val automaticRating: TypingAutoRatingDecision? = null,
        val manualRating: ReviewRating? = null,
        val completionPending: Boolean = false,
        val viAutoplayMuted: Boolean = false,
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
        override val runtimeIdentity: AndroidStudyRuntimeIdentity? = null,
        override val hud: AndroidStudySessionHud? = null
        , override val navigation: AndroidReviewNavigation = AndroidReviewNavigation()
    ) : Runtime
    data class MultipleChoice(
        override val plan: RecallPlan,
        val question: String,
        val choices: List<RecallChoice>,
        val selectedChoiceId: String? = null,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val partOfSpeech: String? = null,
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
        override val runtimeIdentity: AndroidStudyRuntimeIdentity? = null,
        override val hud: AndroidStudySessionHud? = null
        , override val navigation: AndroidReviewNavigation = AndroidReviewNavigation()
    ) : Runtime
    data class Listening(
        override val plan: RecallPlan,
        val audioPath: String?,
        val answer: String = "",
        val audioUnavailable: Boolean = audioPath == null,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val partOfSpeech: String? = null,
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
        override val runtimeIdentity: AndroidStudyRuntimeIdentity? = null,
        override val hud: AndroidStudySessionHud? = null
        , override val navigation: AndroidReviewNavigation = AndroidReviewNavigation()
    ) : Runtime
    data class ImageRecall(
        override val plan: RecallPlan,
        val imagePath: String?,
        val answer: String = "",
        val imageUnavailable: Boolean = imagePath == null,
        val answerAudioLoopEnabled: Boolean = false,
        override val completed: Boolean = false,
        override val outcome: RecallOutcome? = null,
        override val pronunciation: String? = null,
        override val partOfSpeech: String? = null,
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
        override val runtimeIdentity: AndroidStudyRuntimeIdentity? = null,
        override val hud: AndroidStudySessionHud? = null
        , override val navigation: AndroidReviewNavigation = AndroidReviewNavigation()
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
        override val partOfSpeech: String? = null,
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
        override val runtimeIdentity: AndroidStudyRuntimeIdentity? = null,
        override val hud: AndroidStudySessionHud? = null
        , override val navigation: AndroidReviewNavigation = AndroidReviewNavigation()
    ) : Runtime
    data class Completion(
        val sessionId: String,
        val canUndo: Boolean,
        val dailyBudget: DailyStudyBudgetSnapshot? = null,
        val modeFamily: String = "Study",
        val totalCompleted: Int = 0,
        val newCompleted: Int = 0,
        val reviewCompleted: Int = 0
    ) : AndroidStudyState {
        init {
            require(totalCompleted >= 0 && newCompleted >= 0 && reviewCompleted >= 0)
            require(newCompleted + reviewCompleted == totalCompleted)
        }
    }
    data class Failed(
        val message: String,
        val retrySessionId: String? = null,
        val retryable: Boolean = true
    ) : AndroidStudyState
}

private fun AndroidStudyState.withRuntimeIdentity(session: StudySession?): AndroidStudyState {
    val identity = session?.let(AndroidStudyRuntimeIdentity::from) ?: return this
    return when (this) {
        is AndroidStudyState.Introduction -> copy(runtimeIdentity = identity)
        is AndroidStudyState.Typing -> copy(runtimeIdentity = identity)
        is AndroidStudyState.MultipleChoice -> copy(runtimeIdentity = identity)
        is AndroidStudyState.Listening -> copy(runtimeIdentity = identity)
        is AndroidStudyState.ImageRecall -> copy(runtimeIdentity = identity)
        is AndroidStudyState.ExampleCompletion -> copy(runtimeIdentity = identity)
        else -> this
    }
}

data class AndroidReviewNavigation(
    val canPrevious: Boolean = false,
    val canNext: Boolean = false,
    val historyPreview: Boolean = false,
    val canCorrectRating: Boolean = false,
    val previousRating: ReviewRating? = null
)

/** Thin platform facade: Shared Application owns planning, evaluation, learning and queue mutation. */
class AndroidStudyFacade(
    private val context: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val now: () -> Long = System::currentTimeMillis,
    private val resolveMedia: (String) -> String? = { null },
    private val dailyLimits: () -> DailyStudyBudgetLimits = { DailyStudyBudgetLimits() },
    private val continuousSkimEnabled: () -> Boolean = { false },
    private val zoneId: () -> ZoneId = ZoneId::systemDefault,
    private val onHomeQuery: () -> Unit = {},
    private val getInsightsScopePackageId: () -> String? = { null },
    private val onInsightsScopeChanged: (String?) -> Unit = {},
    private val difficultMarkers: vn.loi.learning.android.reminder.AndroidVocabularyReminderDifficultMarkers? = null
) {
    private data class PendingTypingCompletion(
        val result: RecallResult,
        val automaticRating: ReviewRating
    )

    private val pendingTypingCompletions = mutableMapOf<RecallPlanId, PendingTypingCompletion>()
    private val typingEvaluator = TypingAnswerEvaluator()
    private val attemptSequence = AtomicLong()
    private val submittedPlans = mutableSetOf<RecallPlanId>()
    private val submittedItems = mutableSetOf<String>()
    private var currentItem: NextSessionItem? = null
    private data class SessionContentSnapshot(
        val sessionId: SessionId,
        val packageId: vn.loi.learning.domain.library.model.InstalledPackageId?,
        val contentIds: Set<ContentId>
    )
    private var sessionContentSnapshot: SessionContentSnapshot? = null

    /**
     * Resolves the exact, canonical, ordered ContentId membership of the ACTIVE Study/Review session
     * for passive Auto Play playback.
     *
     * Invariants:
     * - Obey active session/queue planned membership (e.g. Learn New 20, Quick Review 19, Again/Hard 6).
     * - Zero package-wide fallback.
     * - Zero FSRS mutation or ReviewEvent creation.
     */
    fun activeStudySessionAutoPlayContentIds(): List<ContentId> {
        val session = currentItem?.session
            ?: context.engine.getActiveSession(learnerId)
            ?: return emptyList()
        val queue = context.engine.getStudyQueue(session.id)
        if (queue != null) {
            val fromQueue = when {
                queue.fixedPracticeMembership.isNotEmpty() ->
                    queue.fixedPracticeMembership.mapNotNull { queue.itemContentIds[it] }
                queue.learningItemIds.isNotEmpty() ->
                    queue.learningItemIds.mapNotNull { queue.itemContentIds[it] }
                else ->
                    queue.itemContentIds.values.toList()
            }.distinct()
            if (fromQueue.isNotEmpty()) {
                return fromQueue
            }
        }
        return emptyList()
    }

    /**
     * Resolves canonical Auto Play content IDs for a Review mode entry without mutating FSRS.
     */
    fun resolveReviewEntryAutoPlayContentIds(entry: AndroidSessionEntry): List<ContentId> {
        val active = currentItem?.session ?: context.engine.getActiveSession(learnerId)
        if (active != null) {
            val matches = when (entry) {
                AndroidSessionEntry.DIFFICULT -> active.policy.focusedPracticeKind == FocusedPracticeKind.DIFFICULT
                AndroidSessionEntry.LATEST_SESSION -> active.policy.focusedPracticeKind == FocusedPracticeKind.LATEST_SESSION
                AndroidSessionEntry.QUICK_REVIEW -> active.policy.practiceLoopPolicy == PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
                AndroidSessionEntry.LEARNED -> active.policy.focusedPracticeKind == FocusedPracticeKind.NONE &&
                    active.policy.practiceLoopPolicy != PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
                else -> false
            }
            if (matches) {
                val activeIds = activeStudySessionAutoPlayContentIds()
                if (activeIds.isNotEmpty()) return activeIds
            }
        }

        val scope = currentScope() ?: return emptyList()
        val requestedAt = Moment(now())
        val queue = when (entry) {
            AndroidSessionEntry.LATEST_SESSION -> {
                when (val result = context.engine.startLatestCompletedNewItemsReview(
                    StartLatestCompletedNewItemsReviewRequest(scope, requestedAt)
                )) {
                    is StartLatestCompletedNewItemsReviewResult.Accepted -> {
                        context.engine.finishSession(result.session.id, requestedAt, completionProvenance = SessionCompletionProvenance.REPLACED_OR_LEFT)
                        result.queue
                    }
                    else -> null
                }
            }
            AndroidSessionEntry.DIFFICULT -> {
                when (val result = context.engine.startDifficultItemsReview(
                    StartDifficultItemsReviewRequest(scope, requestedAt)
                )) {
                    is StartDifficultItemsReviewResult.Accepted -> {
                        context.engine.finishSession(result.session.id, requestedAt, completionProvenance = SessionCompletionProvenance.REPLACED_OR_LEFT)
                        result.queue
                    }
                    else -> null
                }
            }
            AndroidSessionEntry.LEARNED -> {
                when (val result = context.engine.startLearnedItemsReview(
                    StartLearnedItemsReviewRequest(scope, requestedAt)
                )) {
                    is StartLearnedItemsReviewResult.Accepted -> {
                        context.engine.finishSession(result.session.id, requestedAt, completionProvenance = SessionCompletionProvenance.REPLACED_OR_LEFT)
                        result.queue
                    }
                    else -> null
                }
            }
            AndroidSessionEntry.QUICK_REVIEW -> {
                when (val result = context.engine.startLearnedItemsReview(
                    StartLearnedItemsReviewRequest(scope, requestedAt, PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW)
                )) {
                    is StartLearnedItemsReviewResult.Accepted -> {
                        context.engine.finishSession(result.session.id, requestedAt, completionProvenance = SessionCompletionProvenance.REPLACED_OR_LEFT)
                        result.queue
                    }
                    else -> null
                }
            }
            else -> null
        } ?: return emptyList()

        return when {
            queue.fixedPracticeMembership.isNotEmpty() ->
                queue.fixedPracticeMembership.mapNotNull { queue.itemContentIds[it] }
            queue.learningItemIds.isNotEmpty() ->
                queue.learningItemIds.mapNotNull { queue.itemContentIds[it] }
            else ->
                queue.itemContentIds.values.toList()
        }.distinct()
    }

    /**
     * Returns package-wide content scope for package statistics or library browser.
     */
    fun packageContentScopeIds(): List<ContentId> {
        val scope = currentScope() ?: return emptyList()
        return packageContentIds(scope.installedPackageId).toList()
    }

    private fun resolvePackageName(packageId: vn.loi.learning.domain.library.model.InstalledPackageId): String? {
        return context.installedPackageRepository?.findById(packageId)?.name?.value
            ?: context.installedPackages.findById(packageId.value)?.name
            ?: context.installedPackages.query().firstOrNull { it.id == packageId.value }?.name
    }

    private fun resolveActivePackageName(): String? =
        currentScope()?.installedPackageId?.let(::resolvePackageName)

    private fun resolveActivePackageContentCount(): Int? =
        currentScope()?.installedPackageId?.let { pkgId ->
            context.packageContentQuery?.getContentsForPackage(pkgId)?.size?.takeIf { it > 0 }
                ?: resolveInstalledPackage(pkgId)?.contentCount
        }

    private fun resolveInstalledPackage(packageId: vn.loi.learning.domain.library.model.InstalledPackageId): vn.loi.learning.domain.library.model.InstalledPackage? {
        val libraryId = context.defaultLibraryId ?: LibraryId("default-library")
        return context.installedPackageRepository?.findById(packageId)
            ?.takeIf { it.libraryId == libraryId && it.state == PackageState.ACTIVE }
    }

    private fun resolveAvailableInstalledPackages(): List<InstalledPackageItem> {
        val libraryId = context.defaultLibraryId ?: LibraryId("default-library")
        val active = context.installedPackageRepository?.findAll().orEmpty()
            .filter { it.libraryId == libraryId && it.state == PackageState.ACTIVE }
        return if (active.isNotEmpty()) {
            active.map { pkg ->
                InstalledPackageItem(
                    id = pkg.id.value,
                    name = pkg.name.value,
                    version = pkg.version.value,
                    format = "OPD3",
                    libraryCount = 1
                )
            }
        } else {
            context.installedPackages.query()
        }
    }

    fun home(): AndroidStudyState.Home {
        sessionContentSnapshot = null
        onHomeQuery()
        var active = AndroidStartupTrace.measured("study_home_active_session") {
            context.engine.getActiveSession(learnerId)
        }
        val scope = AndroidStartupTrace.measured("study_home_scope") { currentScope() }
        if (active?.installedPackageId != null && scope?.installedPackageId != null && scope.installedPackageId != active.installedPackageId) {
            context.engine.finishSession(
                requireNotNull(active).id,
                Moment(now()),
                completionProvenance = SessionCompletionProvenance.REPLACED_OR_LEFT
            )
            active = null
        }
        val daily = AndroidStartupTrace.measured("study_home_daily_budget") { scope?.let(::dailyBudget) }

        val packages = AndroidStartupTrace.measured("study_home_packages") { context.installedPackages.query() }

        val availability = AndroidStartupTrace.measured("study_home_availability") { scope?.let {
            context.engine.getLearnEntryReviewAvailability(it, Moment(now()))
        } }
        val nowMillis = now()
        val startOfDay = maxOf(0L, Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
            .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val dashboard = AndroidStartupTrace.measured("study_home_dashboard") { context.dashboard.query(
            LearningDashboardQuery(
                learnerId = learnerId,
                activityFrom = Moment(startOfDay),
                activityUntil = Moment(nowMillis + 1),
                at = Moment(nowMillis),
                forecastWindowEnds = emptyList()
            )
        ) }
        val due = dashboard.scheduling.dueStatistics
        val progress = dashboard.activity.progress
        val memories = dashboard.memory.stageCounts
        val dueCount = daily?.dueReviewCount ?: due.dueCount
        val primaryAction = selectHomePrimaryAction(
            active?.id?.value, dueCount, scope != null, daily?.hasEligibleWork ?: false
        )
        val canonicalPackageName = resolveActivePackageName()
        val canonicalContentCount = resolveActivePackageContentCount()
        val activeSessionPackageName = active?.installedPackageId?.let(::resolvePackageName)

        val persistedPackageId = getInsightsScopePackageId()
        val selectedPackage = persistedPackageId?.let { pid -> packages.firstOrNull { it.id == pid } }
        if (persistedPackageId != null && selectedPackage == null) {
            onInsightsScopeChanged(null)
        }
        val currentInsightsScope = when {
            selectedPackage != null -> vn.loi.learning.android.dashboard.AndroidInsightsScope.SpecificPackage(selectedPackage.id, selectedPackage.name)
            else -> vn.loi.learning.android.dashboard.AndroidInsightsScope.AllPackages
        }
        val allPackagesOption = vn.loi.learning.android.dashboard.AndroidInsightsScopeOption(
            scope = vn.loi.learning.android.dashboard.AndroidInsightsScope.AllPackages,
            label = "All packages",
            isActivePackage = false
        )
        val activeEffectiveId = (scope?.installedPackageId ?: active?.installedPackageId)?.value
        val packageOptions = packages.map { pkg ->
            vn.loi.learning.android.dashboard.AndroidInsightsScopeOption(
                scope = vn.loi.learning.android.dashboard.AndroidInsightsScope.SpecificPackage(pkg.id, pkg.name),
                label = pkg.name,
                isActivePackage = pkg.id == activeEffectiveId
            )
        }
        val availableScopes = listOf(allPackagesOption) + packageOptions

        val forecastInsights = AndroidStartupTrace.measured("study_home_forecast_insights") {
            queryForecastInsights(learnerId, currentInsightsScope, availableScopes)
        }
        return AndroidStudyState.Home(
            AndroidSessionEntryAvailability(
                canStartReview = scope != null && active == null && daily != null && daily.reviewRemainingToday > 0 && daily.dueReviewCount > 0,
                canResume = active != null,
                canStartLatestSessionPractice = availability?.latestCompletedNewItems is LatestCompletedNewItemsAvailability.Available,
                canStartDifficultPractice = availability?.difficultItems is DifficultItemsReviewAvailability.Available,
                canStartLearnedReview = availability?.learnedItems is LearnedItemsReviewAvailability.Available,
                canLearnNew = daily != null && daily.newRemainingToday > 0 && daily.eligibleNewContentCount > 0,
                canStartAdaptive = daily != null && if (continuousSkimEnabled()) {
                    daily.reviewRemainingToday > 0 && daily.dueReviewCount > 0 ||
                        daily.newRemainingToday > 0 && daily.eligibleNewContentCount > 0 ||
                        availability?.learnedItems is LearnedItemsReviewAvailability.Available
                } else {
                    daily.reviewRemainingToday > 0 && daily.dueReviewCount > 0
                },
                canStartTyping = daily != null && daily.reviewRemainingToday > 0 &&
                    availability?.learnedItems is LearnedItemsReviewAvailability.Available,
                hasActiveSession = active != null
            ),
            model = AndroidHomeUiModel(
                primaryAction = primaryAction,
                contextTitle = if (active != null) activeSessionPackageName else canonicalPackageName,
                installedPackageCount = packages.size,
                dueCount = dueCount,
                overdueCount = due.overdueCount,
                reviewedToday = progress.totalReviews,
                accuracyPercent = progress.accuracy?.let { (it * 100).toInt() },
                activeMemoryCount = memories.activeMemories,
                totalMemoryCount = memories.totalMemories,
                activePackageName = canonicalPackageName,
                activePackageContentCount = canonicalContentCount,
                dailyBudget = daily,
                forecastInsights = forecastInsights
            )
        )
    }

    fun start(entry: AndroidSessionEntry, mode: StudyMode = StudyMode.ADAPTIVE): AndroidStudyState {
        val scope = AndroidStartupTrace.measured("study_start_scope") { currentScope() }
            ?: return AndroidStudyState.Failed("No active content package.")
        val requestedAt = Moment(now())
        val actionContentIds = AndroidStartupTrace.measured("study_start_content_ids") {
            packageContentIds(scope.installedPackageId)
        }
        val daily = AndroidStartupTrace.measured("study_start_daily_budget") {
            dailyBudget(scope, requestedAt, actionContentIds)
        }
        val reviewAvailability = AndroidStartupTrace.measured("study_start_availability") {
            context.engine.getLearnEntryReviewAvailability(scope, requestedAt)
        }
        val hasScheduledAdaptiveWork = daily.reviewRemainingToday > 0 && daily.dueReviewCount > 0 ||
            daily.newRemainingToday > 0 && daily.eligibleNewContentCount > 0
        val canStartRequestedMode = when (entry) {
            AndroidSessionEntry.LATEST_SESSION -> reviewAvailability.latestCompletedNewItems is LatestCompletedNewItemsAvailability.Available
            AndroidSessionEntry.DIFFICULT -> reviewAvailability.difficultItems is DifficultItemsReviewAvailability.Available
            AndroidSessionEntry.LEARNED -> reviewAvailability.learnedItems is LearnedItemsReviewAvailability.Available
            AndroidSessionEntry.QUICK_REVIEW -> reviewAvailability.learnedItems is LearnedItemsReviewAvailability.Available
            AndroidSessionEntry.REVIEW -> when (mode) {
            StudyMode.LEARN_NEW -> daily.newRemainingToday > 0 && daily.eligibleNewContentCount > 0
            StudyMode.ADAPTIVE -> if (continuousSkimEnabled()) {
                hasScheduledAdaptiveWork || reviewAvailability.learnedItems is LearnedItemsReviewAvailability.Available
            } else {
                daily.reviewRemainingToday > 0 && daily.dueReviewCount > 0
            }
            StudyMode.TYPING -> daily.reviewRemainingToday > 0 &&
                reviewAvailability.learnedItems is LearnedItemsReviewAvailability.Available
            }
        }
        if (!canStartRequestedMode) {
            return AndroidStudyState.Failed(
                dailyUnavailableMessage(daily)
            )
        }

        // Continue is an explicit action of its own. When the learner explicitly selects a different
        // Study mode, keep all committed learning history but close the old navigation session so the
        // requested mode can actually start. Selecting the same mode still resumes the exact session.
        AndroidStartupTrace.measured("study_start_active_session") { reconcileActiveSession() }?.let { active ->
            if (active.installedPackageId == scope.installedPackageId) {
                if (entry == AndroidSessionEntry.REVIEW &&
                    active.studyMode == mode &&
                    active.policy.focusedPracticeKind == FocusedPracticeKind.NONE &&
                    active.policy.practiceLoopPolicy != PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
                ) {
                    return loadExact(active.id.value)
                }
                context.engine.finishSession(
                    active.id,
                    requestedAt,
                    completionProvenance = SessionCompletionProvenance.REPLACED_OR_LEFT
                )
            }
        }

        val session = AndroidStartupTrace.measured("study_start_session_creation") { when (entry) {
            AndroidSessionEntry.REVIEW -> if (
                mode == StudyMode.ADAPTIVE && continuousSkimEnabled() && !hasScheduledAdaptiveWork
            ) {
                when (val result = context.engine.startLearnedItemsReview(
                    StartLearnedItemsReviewRequest(
                        scope,
                        requestedAt,
                        PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED
                    )
                )) {
                    is StartLearnedItemsReviewResult.Accepted -> result.session
                    else -> return AndroidStudyState.Failed("Continuous Skim is unavailable.")
                }
            } else {
                val dailyPolicy = when (mode) {
                    StudyMode.LEARN_NEW -> SessionPolicy(newItemLimit = daily.newRemainingToday, reviewItemLimit = 0)
                    StudyMode.ADAPTIVE -> if (continuousSkimEnabled()) {
                        SessionPolicy(
                            newItemLimit = daily.newRemainingToday,
                            reviewItemLimit = daily.reviewRemainingToday
                        )
                    } else {
                        SessionPolicy(newItemLimit = 0, reviewItemLimit = daily.reviewRemainingToday)
                    }
                    StudyMode.TYPING -> SessionPolicy(newItemLimit = 0, reviewItemLimit = daily.reviewRemainingToday)
                }
                context.engine.startSession(
                    StartStudySessionCommand(
                        SessionId(UUID.randomUUID().toString()), learnerId, requestedAt,
                        policy = dailyPolicy, installedPackageId = scope.installedPackageId, topicId = scope.topicId,
                        studyMode = mode,
                        includedContentIds = actionContentIds
                    )
                )
            }
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
            AndroidSessionEntry.QUICK_REVIEW -> when (val result = context.engine.startLearnedItemsReview(
                StartLearnedItemsReviewRequest(scope, requestedAt, PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW)
            )) {
                is StartLearnedItemsReviewResult.Accepted -> result.session
                else -> return AndroidStudyState.Failed("Quick Review is unavailable.")
            }
        } }
        return AndroidStartupTrace.measured("study_start_first_usable_state") {
            loadWithSnapshot(session.id.value, actionContentIds, daily, knownSession = session, deferHud = true)
        }
    }

    fun load(restoredSessionId: String? = null): AndroidStudyState =
        loadWithSnapshot(restoredSessionId, null, null)

    private fun loadWithSnapshot(
        restoredSessionId: String?,
        knownContentIds: Set<ContentId>?,
        knownDaily: DailyStudyBudgetSnapshot?,
        knownSession: StudySession? = null,
        deferHud: Boolean = false
    ): AndroidStudyState {
        if (knownSession == null) AndroidStartupTrace.measured("study_load_reconcile") { reconcileActiveSession() }
        val session = knownSession ?: AndroidStartupTrace.measured("study_load_session_lookup") {
            restoredSessionId?.let(::SessionId)?.let(context.engine::getSession)
                ?: context.engine.getActiveSession(learnerId)
        } ?: return home()
        if (session.status == SessionStatus.FINISHED) return completeExhaustedSession(session)
        val next = AndroidStartupTrace.measured("study_load_next_item") {
            context.engine.getNextSessionItem(session.id, Moment(now()))
        }
            ?: return completeExhaustedSession(session)
        val contentIds = knownContentIds
            ?: sessionContentSnapshot?.takeIf {
                it.sessionId == session.id && it.packageId == session.installedPackageId
            }?.contentIds
            ?: AndroidStartupTrace.measured("study_load_package_content_ids") {
                session.installedPackageId?.let(::packageContentIds)
                    ?: session.includedContentIds
            }
        sessionContentSnapshot = SessionContentSnapshot(session.id, session.installedPackageId, contentIds)
        currentItem = next
        if (next.session.policy.focusedPracticeKind == FocusedPracticeKind.DIFFICULT ||
            next.session.policy.focusedPracticeKind == FocusedPracticeKind.QUICK_REVIEW ||
            next.origin == SessionItemOrigin.NEW && next.item.content.id !in next.session.reviewedContentIds
        ) {
            val state = AndroidStartupTrace.measured("study_load_introduction_projection") {
                buildIntroduction(next, revealed = next.session.answerRevealed, packageContentIds = contentIds)
            }
            return if (deferHud) state else AndroidStartupTrace.measured("study_load_hud") {
                attachHud(state, next.session, contentIds, knownDaily)
            }
        }
        val plan = AndroidStartupTrace.measured("study_load_recall_plan") { createPlan(next) }
            ?: return AndroidStudyState.Failed("Không thể tạo kế hoạch ghi nhớ cho phiên học này.", session.id.value)
        val state = AndroidStartupTrace.measured("study_load_presentation") { present(plan) }
        return if (deferHud) state else AndroidStartupTrace.measured("study_load_hud") {
            attachHud(state, next.session, contentIds, knownDaily)
        }
    }

    fun refreshHud(state: AndroidStudyState.Runtime): AndroidStudyState {
        val item = currentItem ?: return state
        val stateSessionId = state.plan?.sessionId?.value ?: (state as? AndroidStudyState.Introduction)?.sessionId
        if (stateSessionId != item.session.id.value) return state
        return attachHud(state, item.session)
    }

    fun isDifficult(contentId: String): Boolean =
        difficultMarkers?.isMarked(ContentId(contentId)) ?: false

    fun toggleDifficult(contentId: String): Boolean =
        difficultMarkers?.toggle(ContentId(contentId)) ?: false

    fun saveQuickEdit(
        draft: vn.loi.learning.android.packageexperience.AndroidPackageQuickEditDraft
    ): Result<Unit> = runCatching {
        require(draft.question.isNotBlank()) { "Question is required." }
        require(draft.answer.isNotBlank()) { "Answer is required." }
        requireNotNull(context.contentBrowserEdit) { "Content editor is unavailable." }.updateTextFields(
            ContentId(draft.contentId),
            draft.question.trim(),
            draft.answer.trim(),
            draft.pronunciation.trim(),
            draft.partOfSpeech.trim(),
            draft.example.trim(),
            draft.translation.trim()
        )
    }

    fun updateDailyLimits(state: AndroidStudyState.Runtime, newLimit: Int, reviewLimit: Int): AndroidStudyState {
        val item = currentItem ?: return state
        return runCatching {
            context.engine.updateActiveSessionLimits(item.session.id, newLimit, reviewLimit)
            loadExact(item.session.id.value)
        }.getOrElse { error ->
            AndroidStartupTrace.write(false, "phase=study_update_limits_failed error=${error.message}")
            AndroidStudyState.Failed(error.message ?: "Không thể cập nhật giới hạn phiên học.", item.session.id.value)
        }
    }

    fun loadExact(sessionId: String): AndroidStudyState {
        reconcileActiveSession()
        val session = context.engine.getSession(SessionId(sessionId))
            ?: return AndroidStudyState.Failed("Phiên học không khả dụng. Vui lòng quay lại Thư viện và thử lại.", sessionId)
        if (session.status == SessionStatus.FINISHED) return completeExhaustedSession(session)
        val next = context.engine.getNextSessionItem(session.id, Moment(now()))
            ?: return completeExhaustedSession(session)
        currentItem = next
        if (next.session.policy.focusedPracticeKind == FocusedPracticeKind.DIFFICULT ||
            next.session.policy.focusedPracticeKind == FocusedPracticeKind.QUICK_REVIEW ||
            next.origin == SessionItemOrigin.NEW && next.item.content.id !in next.session.reviewedContentIds
        ) {
            return attachHud(buildIntroduction(next, revealed = next.session.answerRevealed), next.session)
        }
        val plan = createPlan(next)
            ?: return AndroidStudyState.Failed("Không thể tạo kế hoạch ghi nhớ cho phiên học này.", sessionId)
        return attachHud(present(plan), next.session)
    }

    private fun attachHud(
        state: AndroidStudyState,
        session: StudySession,
        knownContentIds: Set<ContentId>? = null,
        knownDaily: DailyStudyBudgetSnapshot? = null
    ): AndroidStudyState {
        val runtime = state as? AndroidStudyState.Runtime ?: return state
        val query = context.studyHeaderStatistics ?: return state
        val resolvedContentIds = knownContentIds ?: AndroidStartupTrace.measured("study_hud_package_content_ids") {
            session.installedPackageId?.let(::packageContentIds)
                ?: session.includedContentIds
        }
        val queue = AndroidStartupTrace.measured("study_hud_queue_progress") {
            context.engine.getStudyQueueProgress(session.id)
        } ?: return state
        val queueSnapshot = AndroidStartupTrace.measured("study_hud_queue_snapshot") {
            context.studyQueue.get(session.id)
        } ?: return state
        val scope = AndroidStartupTrace.measured("study_hud_statistics_scope") {
            resolveStatisticsScope(session, resolvedContentIds)
        } ?: return state
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
        val daily = knownDaily ?: AndroidStartupTrace.measured("study_hud_daily_budget") {
            currentScope()?.let { dailyBudget(it, contentIds = resolvedContentIds) }
        }
        val baseHud = runCatching { AndroidStartupTrace.measured("study_hud_statistics") {
            query.execute(scope, source, learnerId).toAndroidStudySessionHud(daily)
        } }
            .getOrNull() ?: return state
        val coverageCompleted = session.newItemsReviewed + session.reviewItemsReviewed
        val coverageTarget = queue.effectiveNewWorkload + queue.effectiveReviewWorkload
        val focusedKind = session.policy.focusedPracticeKind
        val skimStatus = when (focusedKind) {
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW -> "Quick Review"
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.LATEST_SESSION ->
                "Ôn từ vừa học · Vòng ${queueSnapshot.practiceRound}"
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT ->
                "Again / Hard · còn ${queueSnapshot.fixedPracticeMembership.size} từ"
            else -> if (session.studyMode == StudyMode.ADAPTIVE &&
                queueSnapshot.practiceLoopPolicy == PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED
            ) {
                "Adaptive · Continuous practice · Round ${queueSnapshot.practiceRound + 1}"
            } else if (queueSnapshot.practiceLoopPolicy != PracticeLoopPolicy.NONE) {
                "Skim · Round ${queueSnapshot.practiceRound + 1}"
            } else null
        } ?: if (runtime !is AndroidStudyState.Introduction &&
            session.studyMode == StudyMode.ADAPTIVE && coverageTarget > 0
        ) {
            val reinforcing = queueSnapshot.currentLearningItemId?.let(queueSnapshot.itemContentIds::get) in session.reviewedContentIds
            "Coverage ${minOf(coverageCompleted, coverageTarget)}/$coverageTarget" +
                if (reinforcing) " · Reinforce" else ""
        } else null
        val hud = baseHud.copy(
            skimStatus = skimStatus,
            focusedPractice = focusedKind !=
                vn.loi.learning.domain.study.session.model.FocusedPracticeKind.NONE
        )
        AndroidContinuousSkimTrace.selection(session, queueSnapshot, skimStatus)
        return when (runtime) {
            is AndroidStudyState.Introduction -> runtime.copy(hud = hud)
            is AndroidStudyState.Typing -> runtime.copy(hud = hud)
            is AndroidStudyState.MultipleChoice -> runtime.copy(hud = hud)
            is AndroidStudyState.Listening -> runtime.copy(hud = hud)
            is AndroidStudyState.ImageRecall -> runtime.copy(hud = hud)
            is AndroidStudyState.ExampleCompletion -> runtime.copy(hud = hud)
        }
    }

    private fun resolveStatisticsScope(
        session: StudySession,
        knownContentIds: Set<ContentId>? = null
    ): StudyStatisticsScope? {
        session.installedPackageId?.let { packageId ->
            val contentIds = knownContentIds ?: packageContentIds(packageId)
            return StudyStatisticsScope("package:${packageId.value}", contentIds)
        }
        return session.includedContentIds.takeIf { it.isNotEmpty() }
            ?.let { StudyStatisticsScope("session:${session.id.value}", it) }
    }

    fun revealIntroduction(state: AndroidStudyState.Introduction): AndroidStudyState {
        if (state.focusedPracticeKind == FocusedPracticeKind.DIFFICULT) {
            return state.copy(revealedStage = true)
        }
        if (state.focusedPracticeKind == FocusedPracticeKind.QUICK_REVIEW) return state.copy(revealedStage = true)
        val session = context.engine.completeContentIntroduction(
            sessionId = SessionId(state.sessionId),
            contentId = ContentId(state.contentId),
            learningItemId = LearningItemId(state.learningItemId)
        )
        currentItem = currentItem?.copy(session = session)
        return state.copy(revealedStage = true)
    }

    fun rateIntroduction(
        state: AndroidStudyState.Introduction,
        rating: ReviewRating,
        deferHud: Boolean = false
    ): AndroidStudyState {
        val queueOccurrence = if (state.focusedPracticeKind == FocusedPracticeKind.QUICK_REVIEW)
            context.studyQueue.get(SessionId(state.sessionId))?.let { "${it.practiceRound}:${it.practiceExposureSequence}" }
        else null
        val submissionKey = "${state.sessionId}:${state.learningItemId}:${queueOccurrence.orEmpty()}"
        if (submissionKey in submittedItems) return state
        submittedItems += submissionKey
        return try {
            val sessionId = SessionId(state.sessionId)
            val learningItemId = LearningItemId(state.learningItemId)
            val updatedSession = AndroidStartupTrace.measured("introduction_rating_prepare") {
                if (state.revealed) {
                    requireNotNull(context.engine.getSession(sessionId)) { "Study session is unavailable." }
                } else {
                    context.engine.completeContentIntroduction(
                        sessionId = sessionId,
                        contentId = ContentId(state.contentId),
                        learningItemId = learningItemId
                    )
                }
            }
            currentItem = currentItem?.copy(session = updatedSession)
            val reviewResult = AndroidStartupTrace.measured("introduction_rating_commit") {
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
            }
            currentItem = currentItem?.copy(session = reviewResult.session)
            val contentIds = sessionContentSnapshot?.takeIf { it.sessionId == sessionId }?.contentIds
            AndroidStartupTrace.measured("introduction_rating_next_state") {
                loadWithSnapshot(
                    restoredSessionId = state.sessionId,
                    knownContentIds = contentIds,
                    knownDaily = null,
                    knownSession = reviewResult.session,
                    deferHud = deferHud
                )
            }
        } catch (failure: RuntimeException) {
            submittedItems -= submissionKey
            throw failure
        }
    }

    private fun buildIntroduction(
        next: NextSessionItem,
        revealed: Boolean,
        packageContentIds: Set<ContentId>? = null
    ): AndroidStudyState.Introduction {
        val content = next.item.content
        val pronunciation = content.text.pronunciation
        val meaning = content.text.translatedText
        val projectedExample = LegacyExampleTranslationProjection.project(
            content.text.exampleText,
            content.text.exampleTranslation
        )
        val example = projectedExample.exampleText
        val translation = projectedExample.exampleTranslation
        val answer = content.text.primaryText
        val partOfSpeech = resolveIntroductionPartOfSpeech(content)
        val primaryAudio = content.media.primaryAudio?.let(resolveMedia)
        val meaningAudio = content.media.translatedAudio?.let(resolveMedia)
        val exampleEnglishAudio = content.media.exampleAudio?.let(resolveMedia)
        val exampleVietnameseAudio = content.media.exampleTranslatedAudio?.let(resolveMedia)
        val mediaImage = content.media.image?.let(resolveMedia)
        val latestEffectiveRating = context.engine.getContentLearningState(learnerId, content.id).latestEffectiveRating
        val currentPos = next.progress?.currentPosition
        val totalCount = next.progress?.totalItemCount
        val focusedPracticeQueue = if (
            next.session.policy.focusedPracticeKind == FocusedPracticeKind.QUICK_REVIEW ||
            next.session.policy.focusedPracticeKind == FocusedPracticeKind.DIFFICULT
        ) {
            context.studyQueue.get(next.session.id)
        } else null
        val presentationVisitId = focusedPracticeQueue?.let {
            "${next.session.id.value}:${it.practiceRound}:${it.practiceExposureSequence}"
        }
        val quickReviewQueue = focusedPracticeQueue.takeIf {
            next.session.policy.focusedPracticeKind == FocusedPracticeKind.QUICK_REVIEW
        }
        val packagePosition = AndroidStartupTrace.measured("study_introduction_package_position") {
            resolvePackagePosition(packageContentIds, content.id)
        }
        val title = AndroidStartupTrace.measured("study_introduction_context_title") {
            next.session.installedPackageId?.let(::resolvePackageName)
        }

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
            resolvedPromptAudio = primaryAudio,
            resolvedExpectedAnswerAudio = primaryAudio,
            resolvedMeaningAudio = meaningAudio,
            resolvedExampleEnglishAudio = exampleEnglishAudio,
            resolvedExampleVietnameseAudio = exampleVietnameseAudio,
            resolvedImage = mediaImage,
            currentPosition = currentPos,
            totalItems = totalCount,
            packagePosition = packagePosition?.position,
            packageTotal = packagePosition?.total,
            presentationVisitId = presentationVisitId,
            quickReviewPassPosition = quickReviewQueue?.practiceProgress?.position,
            quickReviewPoolSize = quickReviewQueue?.practiceProgress?.membershipSize,
            latestEffectiveRating = latestEffectiveRating,
            focusedPracticeKind = next.session.policy.focusedPracticeKind,
            contextTitle = title,
            runtimeIdentity = AndroidStudyRuntimeIdentity.from(next.session)
        )
    }

    private fun resolvePackagePosition(contentIds: Set<ContentId>?, contentId: ContentId): PackageStudyPosition? {
        return resolvePackageStudyPosition(contentIds.orEmpty().map { it.value }, contentId.value)
    }

    fun updateAnswer(state: AndroidStudyState.Runtime, answer: String): AndroidStudyState.Runtime = when (state) {
        is AndroidStudyState.Introduction -> state
        is AndroidStudyState.Typing -> {
            if (state.completed || state.revealed) state else state.copy(
                answer = answer,
                evaluation = typingEvaluator.evaluate(TypingRecallPrompt(state.plan.answerContract.canonicalAnswer), answer).status,
                attempt = state.attempt?.let { attempt ->
                    val prompt = TypingRecallPrompt(state.plan.answerContract.canonicalAnswer)
                    TypingAttemptTracker.update(
                        attempt, answer, typingEvaluator.evaluate(prompt, answer),
                        typingEvaluator.evaluateExpectedPrefix(prompt, answer),
                        TypingAttemptTimeSource.MONOTONIC.nowMillis()
                    )
                }
            )
        }
        is AndroidStudyState.Listening -> if (state.completed) state else state.copy(answer = answer)
        is AndroidStudyState.ImageRecall -> if (state.completed) state else state.copy(answer = answer)
        is AndroidStudyState.ExampleCompletion -> if (state.completed || state.revealed) state else state.copy(answer = answer)
        is AndroidStudyState.MultipleChoice -> state
    }

    fun submitTypingIfCorrect(state: AndroidStudyState.Typing): AndroidStudyState {
        val evaluation = typingEvaluator.evaluate(TypingRecallPrompt(state.plan.answerContract.canonicalAnswer), state.answer)
        if (!evaluation.isCorrect || state.completionPending || state.plan.planId in submittedPlans) {
            return state.copy(evaluation = evaluation.status)
        }
        AndroidTypingSuccessTrace.exactMatch(
            state.plan.planId.value,
            state.resolvedExpectedAnswerAudio
        )
        val item = currentItem ?: return AndroidStudyState.Failed("Study item is unavailable.")
        val attempt = state.attempt ?: return AndroidStudyState.Failed("Typing attempt is unavailable.")
        val metrics = attempt.snapshot(revealUsed = false)
        val decision = TypingAutomaticRatingResolver.decide(metrics)
        val execution = context.engine.executeRecall(
            RecallExecutionRequest(
                state.plan,
                typedSubmission(state.plan, state.answer),
                evaluationContext = strategyContext(item.session)
            )
        ) as? RecallExecutionResult.Completed
            ?: return AndroidStudyState.Failed("Shared recall execution rejected the Typing attempt.")
        pendingTypingCompletions[state.plan.planId] = PendingTypingCompletion(execution.result, decision.rating)
        return state.copy(
            evaluation = evaluation.status,
            automaticRating = decision,
            completionPending = true,
            completed = true,
            outcome = execution.result.outcome
        )
    }

    fun commitTypingRating(state: AndroidStudyState.Typing, manualRating: ReviewRating?): AndroidStudyState =
        commitTypingRatingInternal(state, manualRating, prepareNext = false)

    fun commitTypingRatingAndPrepareNext(
        state: AndroidStudyState.Typing,
        manualRating: ReviewRating?
    ): AndroidStudyState = commitTypingRatingInternal(state, manualRating, prepareNext = true)

    private fun commitTypingRatingInternal(
        state: AndroidStudyState.Typing,
        manualRating: ReviewRating?,
        prepareNext: Boolean
    ): AndroidStudyState {
        val tracePlanId = state.plan.planId.value
        AndroidTypingSuccessTrace.commitEvent("resolveRating", tracePlanId, "manual=${manualRating?.name ?: "NONE"}")
        val pending = pendingTypingCompletions[state.plan.planId] ?: return state
        if (state.plan.planId in submittedPlans) return state
        val item = currentItem ?: return AndroidStudyState.Failed("Study item is unavailable.")
        val strategyContext = strategyContext(item.session)
        val request = RecallLearningExecutionRequest(
            pending.result, item.session.id, item.item.learningItem.id, learnerId,
            item.item.content.id, strategyContext,
            manualRatingIntent = manualRating?.let { RecallManualRatingIntent(it, RatingSource.MANUAL_USER) },
            policy = RecallLearningExecutionPolicy(
                strongExactRating = pending.automaticRating,
                standardSuccessRating = pending.automaticRating,
                weakSuccessRating = pending.automaticRating
            )
        )
        AndroidTypingSuccessTrace.event("commitStart", tracePlanId, false, false)
        AndroidTypingSuccessTrace.commitEvent("executeRecallLearningStart", tracePlanId)
        AndroidTypingSuccessTrace.commitEvent("persistenceStart", tracePlanId)
        val learning = context.engine.executeRecallLearning(request)
        AndroidTypingSuccessTrace.commitEvent("schedulerEnd", tracePlanId)
        AndroidTypingSuccessTrace.commitEvent("persistenceEnd", tracePlanId)
        if (learning !is RecallLearningExecutionResult.Committed) {
            AndroidTypingSuccessTrace.commitEvent("complete", tracePlanId, "success=false result=${learning::class.simpleName}")
            return AndroidStudyState.Failed("Shared learning execution rejected the Typing rating.")
        }
        submittedPlans += state.plan.planId
        pendingTypingCompletions.remove(state.plan.planId)
        if (prepareNext) {
            AndroidTypingSuccessTrace.commitEvent("projectionRefreshStart", tracePlanId, "skipped=true combinedAdvance=true")
            AndroidTypingSuccessTrace.commitEvent("projectionRefreshEnd", tracePlanId, "skipped=true combinedAdvance=true")
            AndroidTypingSuccessTrace.event("commitEnd", tracePlanId, false, false)
            AndroidTypingSuccessTrace.commitEvent("complete", tracePlanId, "success=true")
            return prepareTypingNext(state.plan.sessionId.value, tracePlanId)
        }
        AndroidTypingSuccessTrace.commitEvent("projectionRefreshStart", tracePlanId)
        val committed = context.engine.getSession(item.session.id) ?: item.session
        val projected = attachHud(state.copy(manualRating = manualRating, completionPending = false), committed)
        AndroidTypingSuccessTrace.commitEvent("projectionRefreshEnd", tracePlanId)
        AndroidTypingSuccessTrace.event("commitEnd", tracePlanId, false, false)
        AndroidTypingSuccessTrace.commitEvent("complete", tracePlanId, "success=true")
        return projected
    }

    private fun prepareTypingNext(sessionId: String, tracePlanId: String): AndroidStudyState {
        AndroidTypingSuccessTrace.nextEvent("sessionReloadStart", tracePlanId)
        val session = context.engine.getSession(SessionId(sessionId))
            ?: return AndroidStudyState.Failed("Phiên học không khả dụng. Vui lòng quay lại Thư viện và thử lại.", sessionId)
        AndroidTypingSuccessTrace.nextEvent("sessionReloadEnd", tracePlanId)
        if (session.status == SessionStatus.FINISHED) return completeExhaustedSession(session)
        AndroidTypingSuccessTrace.nextEvent("queueAdvanceStart", tracePlanId)
        val next = context.engine.getNextSessionItem(session.id, Moment(now()))
            ?: return completeExhaustedSession(session)
        AndroidTypingSuccessTrace.nextEvent("queueAdvanceEnd", tracePlanId)
        currentItem = next
        AndroidTypingSuccessTrace.nextEvent("packageReadStart", tracePlanId)
        val isNewIntroduction = next.origin == SessionItemOrigin.NEW && next.item.content.id !in next.session.reviewedContentIds
        AndroidTypingSuccessTrace.nextEvent("packageReadEnd", tracePlanId)
        if (isNewIntroduction) {
            AndroidTypingSuccessTrace.nextEvent("stateProjectionStart", tracePlanId)
            val projected = attachHud(buildIntroduction(next, revealed = next.session.answerRevealed), next.session)
            AndroidTypingSuccessTrace.nextEvent("stateProjectionEnd", tracePlanId)
            AndroidTypingSuccessTrace.nextEvent("published", tracePlanId, "prepared=true")
            return projected
        }
        AndroidTypingSuccessTrace.nextEvent("planResolveStart", tracePlanId)
        val plan = createPlan(next)
            ?: return AndroidStudyState.Failed("Không thể tạo kế hoạch ghi nhớ cho phiên học này.", sessionId)
        AndroidTypingSuccessTrace.nextEvent("planResolveEnd", tracePlanId)
        AndroidTypingSuccessTrace.nextEvent("mediaResolveStart", tracePlanId)
        val presented = present(plan)
        AndroidTypingSuccessTrace.nextEvent("mediaResolveEnd", tracePlanId)
        AndroidTypingSuccessTrace.nextEvent("stateProjectionStart", tracePlanId)
        val projected = attachHud(presented, next.session)
        AndroidTypingSuccessTrace.nextEvent("stateProjectionEnd", tracePlanId)
        AndroidTypingSuccessTrace.nextEvent("published", tracePlanId, "prepared=true")
        return projected
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
        if (updatedState is AndroidStudyState.Typing) {
            val pending = submitTypingIfCorrect(updatedState)
            return if (pending is AndroidStudyState.Typing && pending.completionPending) {
                commitTypingRating(pending, null)
            } else pending
        }
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
        return if (state is AndroidStudyState.Introduction &&
            state.focusedPracticeKind == FocusedPracticeKind.QUICK_REVIEW
        ) {
            context.engine.advanceQuickReviewWithoutEvaluation(SessionId(state.sessionId))
            load(state.sessionId)
        } else if (state is AndroidStudyState.Introduction &&
            state.focusedPracticeKind == FocusedPracticeKind.DIFFICULT
        ) {
            context.engine.completePracticeItem(
                CompletePracticeItemCommand(
                    SessionId(state.sessionId), LearningItemId(state.learningItemId), PracticeRecallResult.REVEALED
                )
            )
            load(state.sessionId)
        } else if (state is AndroidStudyState.Introduction) load(state.sessionId)
        else if (state.completed && plan != null) load(plan.sessionId.value) else state
    }

    fun deferIntroduction(state: AndroidStudyState.Introduction): AndroidStudyState {
        context.studyQueue.deferCurrent(SessionId(state.sessionId))
        return load(state.sessionId)
    }

    fun pauseTyping(state: AndroidStudyState.Typing): AndroidStudyState =
        state.copy(attempt = state.attempt?.pause(TypingAttemptTimeSource.MONOTONIC.nowMillis()))

    fun resumeTyping(state: AndroidStudyState.Typing): AndroidStudyState =
        state.copy(attempt = state.attempt?.resume(TypingAttemptTimeSource.MONOTONIC.nowMillis()))

    fun overridePracticeRating(state: AndroidStudyState.Runtime, rating: ReviewRating): AndroidStudyState {
        val item = currentItem ?: return state
        if (item.session.policy.evaluationPolicy != SessionEvaluationPolicy.PRACTICE_ONLY) return state
        if (item.session.policy.focusedPracticeKind !=
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.NONE) return state
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

    fun isSessionItemUndoable(sessionId: String, learningItemId: String): Boolean {
        val session = context.engine.getSession(SessionId(sessionId)) ?: return false
        val undoable = session.undoableReview ?: return false
        return undoable.learningItemId.value == learningItemId
    }

    fun getUndoableSessionReviewRating(sessionId: String, learningItemId: String): ReviewRating? {
        val session = context.engine.getSession(SessionId(sessionId)) ?: return null
        val undoable = session.undoableReview ?: return null
        if (undoable.learningItemId.value != learningItemId) return null
        return context.reviewEventRepository?.findAll(learnerId)?.find { it.id == undoable.reviewEventId }?.rating
    }

    internal fun present(plan: RecallPlan): AndroidStudyState {
        val item = currentItem
        val content = item?.item?.content
        val pronunciation = content?.text?.pronunciation
        val partOfSpeech = content?.let(::resolveIntroductionPartOfSpeech)
        val meaning = content?.text?.translatedText
        val projectedExample = LegacyExampleTranslationProjection.project(
            content?.text?.exampleText,
            content?.text?.exampleTranslation
        )
        val example = projectedExample.exampleText
        val translation = projectedExample.exampleTranslation
        val promptAudio = content?.media?.primaryAudio?.let(resolveMedia)
        val expectedAnswerAudio = content?.media?.primaryAudio?.let(resolveMedia)
        val meaningAudio = content?.media?.translatedAudio?.let(resolveMedia)
        val exampleEnglishAudio = content?.media?.exampleAudio?.let(resolveMedia)
        val exampleVietnameseAudio = content?.media?.exampleTranslatedAudio?.let(resolveMedia)
        val mediaImage = content?.media?.image?.let(resolveMedia)
        val currentPos = item?.progress?.currentPosition
        val totalCount = item?.progress?.totalItemCount
        val title = item?.session?.installedPackageId?.let(::resolvePackageName)

        fun typingPresentation(sourceText: String): AndroidStudyState.Typing {
            val canonicalRatingTransitionEligible = item?.session?.policy?.evaluationPolicy ==
                SessionEvaluationPolicy.EVALUATIVE
            val previousCanonicalRating = if (canonicalRatingTransitionEligible) {
                requireNotNull(item).item.content.id.let { contentId ->
                    context.engine.getContentLearningState(learnerId, contentId).latestEffectiveRating
                }
            } else null
            return AndroidStudyState.Typing(
                plan, sourceText,
                partOfSpeech = partOfSpeech,
                attempt = item?.let { next ->
                    TypingAttemptState(
                        context = ExperienceRotationContext.from(next),
                        attemptGeneration = attemptSequence.incrementAndGet(),
                        startedAtMillis = TypingAttemptTimeSource.MONOTONIC.nowMillis(),
                        canonicalCodePointCount = plan.answerContract.canonicalAnswer.codePointCount(0, plan.answerContract.canonicalAnswer.length),
                        itemOrigin = next.origin,
                        learningStage = null,
                        previousRating = null,
                        itemPresentedAtEpochMillis = now(),
                        timingPolicy = TypingTimingPolicy.MEASURE_FROM_FIRST_INPUT
                    )
                },
                previousCanonicalRating = previousCanonicalRating,
                canonicalRatingTransitionEligible = canonicalRatingTransitionEligible,
                pronunciation = pronunciation, meaning = meaning, example = example, translation = translation,
                resolvedPromptAudio = promptAudio, resolvedExpectedAnswerAudio = expectedAnswerAudio,
                resolvedMeaningAudio = meaningAudio, resolvedExampleEnglishAudio = exampleEnglishAudio,
                resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                resolvedImage = mediaImage,
                currentPosition = currentPos, totalItems = totalCount, contextTitle = title
            )
        }

        return (when (val prompt = plan.prompt) {
            is RecallPrompt.Typing -> typingPresentation(prompt.sourceText)
            is RecallPrompt.ReverseTranslation -> typingPresentation(prompt.targetText)
            is RecallPrompt.MultipleChoice -> AndroidStudyState.MultipleChoice(
                plan, prompt.question, prompt.choices,
                pronunciation = pronunciation, partOfSpeech = partOfSpeech, meaning = meaning, example = example, translation = translation,
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
                    pronunciation = pronunciation, partOfSpeech = partOfSpeech, meaning = meaning, example = example, translation = translation,
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
                    answerAudioLoopEnabled = item?.session?.policy?.let { policy ->
                        policy.evaluationPolicy == SessionEvaluationPolicy.EVALUATIVE &&
                            policy.allowRepeatInSameSession && policy.newItemLimit == 0
                    } == true,
                    pronunciation = pronunciation, partOfSpeech = partOfSpeech, meaning = meaning, example = example, translation = translation,
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
                pronunciation = pronunciation, partOfSpeech = partOfSpeech, meaning = meaning, example = example, translation = translation,
                resolvedPromptAudio = promptAudio, resolvedExpectedAnswerAudio = expectedAnswerAudio,
                resolvedMeaningAudio = meaningAudio, resolvedExampleEnglishAudio = exampleEnglishAudio,
                resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                resolvedImage = mediaImage,
                currentPosition = currentPos, totalItems = totalCount, contextTitle = title
            )
            else -> AndroidStudyState.Failed("${plan.mode.wireId} is not available on Android.")
        }).withRuntimeIdentity(item?.session)
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
        val activePackageId = context.domainLibraryRepository?.findById(libraryId)?.activePackageId ?: return null
        val pkg = context.installedPackageRepository?.findById(activePackageId)
            ?.takeIf { it.libraryId == libraryId && it.state == PackageState.ACTIVE }
            ?: return null
        return LearnEntryScope(learnerId, pkg.id, pkg.topicId)
    }

    private fun dailyBudget(
        scope: LearnEntryScope,
        at: Moment = Moment(now()),
        contentIds: Set<ContentId>? = null
    ): DailyStudyBudgetSnapshot {
        val resolvedContentIds = contentIds ?: AndroidStartupTrace.measured("daily_budget_content_ids") {
            packageContentIds(scope.installedPackageId)
        }
        return AndroidStartupTrace.measured("daily_budget_calculation") {
            requireNotNull(context.dailyStudyBudget) { "Daily Study budget is unavailable." }
                .execute(learnerId, dailyLimits(), at, zoneId(), resolvedContentIds)
        }
    }

    private fun packageContentIds(packageId: vn.loi.learning.domain.library.model.InstalledPackageId): LinkedHashSet<ContentId> =
        context.packageContentQuery?.getContentIdsForPackage(packageId)
            .orEmpty().toCollection(linkedSetOf())

    private fun dailyUnavailableMessage(daily: DailyStudyBudgetSnapshot): String = when {
        daily.targetsComplete -> "Today's configured Study workload is complete."
        daily.newRemainingToday == 0 && daily.dueReviewCount == 0 ->
            "Today's NEW target is complete and no REVIEW work is due."
        daily.reviewRemainingToday == 0 && daily.eligibleNewContentCount == 0 ->
            "Today's REVIEW target is complete and no NEW content is available."
        else -> "No eligible Study content is currently available."
    }

    private fun completeExhaustedSession(session: StudySession): AndroidStudyState {
        val completed = if (session.status == SessionStatus.ACTIVE) {
            context.engine.finishSession(session.id, Moment(now()))
        } else session
        if (continuousSkimEnabled() &&
            completed.policy.evaluationPolicy == SessionEvaluationPolicy.EVALUATIVE &&
            completed.studyMode == StudyMode.ADAPTIVE
        ) {
            when (val practice = context.engine.startContinuousSkimPractice(
                StartContinuousSkimPracticeRequest(completed.id, Moment(now()))
            )) {
                is StartContinuousSkimPracticeResult.Accepted -> return loadExact(practice.session.id.value)
                StartContinuousSkimPracticeResult.NoItems -> Unit
            }
        }
        val daily = currentScope()?.let { dailyBudget(it) }
        return AndroidStudyState.Completion(
            sessionId = completed.id.value,
            canUndo = completed.undoableReview != null,
            dailyBudget = daily,
            modeFamily = androidCompletionModeFamily(completed),
            totalCompleted = completed.totalReviews,
            newCompleted = completed.newItemsReviewed,
            reviewCompleted = completed.reviewItemsReviewed
        )
    }

    private fun reconcileActiveSession(): StudySession? {
        val at = Moment(now())
        val active = context.activeStudySessionScopeReconciler?.reconcile(learnerId, at)
            ?: context.engine.getActiveSession(learnerId)
            ?: return null
        if (active.status != vn.loi.learning.domain.study.session.model.SessionStatus.ACTIVE) return null
        if (active.installedPackageId == null) return active
        val canonicalPackageId = currentScope()?.installedPackageId
        if (canonicalPackageId != null && active.installedPackageId == canonicalPackageId) return active
        context.engine.finishSession(active.id, at)
        return null
    }

    private fun strategyContext(session: StudySession) =
        if (session.policy.evaluationPolicy == SessionEvaluationPolicy.PRACTICE_ONLY)
            RecallStrategyContext.PRACTICE_ONLY else RecallStrategyContext.EVALUATIVE

    fun updateInsightsScope(scope: vn.loi.learning.android.dashboard.AndroidInsightsScope) {
        val packageId = when (scope) {
            is vn.loi.learning.android.dashboard.AndroidInsightsScope.AllPackages -> null
            is vn.loi.learning.android.dashboard.AndroidInsightsScope.SpecificPackage -> scope.packageId
        }
        onInsightsScopeChanged(packageId)
    }

    private fun queryForecastInsights(
        learnerId: LearnerId,
        scope: vn.loi.learning.android.dashboard.AndroidInsightsScope = vn.loi.learning.android.dashboard.AndroidInsightsScope.AllPackages,
        availableScopes: List<vn.loi.learning.android.dashboard.AndroidInsightsScopeOption> = emptyList()
    ) = vn.loi.learning.android.dashboard.AndroidForecastInsightsQueryService(
        memoryStateRepository = context.memoryStateRepository,
        reviewEventRepository = context.reviewEventRepository,
        packageContentQuery = context.packageContentQuery,
        learningItemRepository = context.learningItemRepository,
        now = now,
        zoneId = zoneId
    ).query(learnerId, scope, availableScopes)
}

internal fun androidCompletionModeFamily(session: StudySession): String = when (session.policy.focusedPracticeKind) {
    FocusedPracticeKind.QUICK_REVIEW -> "Quick Review"
    FocusedPracticeKind.DIFFICULT -> "Again / Hard"
    FocusedPracticeKind.LATEST_SESSION -> "Recent Review"
    FocusedPracticeKind.NONE -> when (session.studyMode) {
        StudyMode.LEARN_NEW -> "Learn New"
        StudyMode.TYPING -> "Typing"
        StudyMode.ADAPTIVE -> "Adaptive"
    }
}
