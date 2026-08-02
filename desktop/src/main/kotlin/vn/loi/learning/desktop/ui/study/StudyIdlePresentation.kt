package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.session.LearnedItemsReviewAvailability
import vn.loi.learning.application.session.LatestCompletedNewItemsAvailability
import vn.loi.learning.application.session.DifficultItemsReviewAvailability

enum class LearningEntryActionPriority {
    PRIMARY,
    ALTERNATIVE,
    NAVIGATION
}

enum class LearningEntryReadinessId {
    ACTIVE_SESSION,
    ACTIVE_REMAINING,
    ACTIVE_NEW_REMAINING,
    ACTIVE_REVIEW_REMAINING,
    CONFIGURED_NEW,
    CONFIGURED_REVIEW,
    NEW,
    REVIEW,
    LEARNED
}

data class LearningEntryContextPresentation(
    val scopeLabel: String,
    val title: String
)

data class LearningEntryReadinessPresentation(
    val id: LearningEntryReadinessId,
    val label: String,
    val value: String
)

data class StudyIdlePresentation(
    val title: String,
    val description: String,
    val actionLabel: String,
    val shortcutHint: String,
    val context: LearningEntryContextPresentation,
    val readiness: List<LearningEntryReadinessPresentation> = emptyList(),
    val actions: List<StudyLearningActionPresentation> = emptyList()
) {
    val primaryAction: StudyLearningActionPresentation?
        get() = actions.singleOrNull { it.priority == LearningEntryActionPriority.PRIMARY }
}

enum class StudyLearningAction {
    CONTINUE,
    START_NEW_CONFIGURED,
    REVIEW_LATEST_NEW,
    REVIEW_AGAIN_HARD,
    REVIEW_ALL_LEARNED,
    BACK_TO_LIBRARY
}

data class StudyLearningActionPresentation(
    val action: StudyLearningAction,
    val label: String,
    val description: String,
    val enabled: Boolean,
    val priority: LearningEntryActionPriority = LearningEntryActionPriority.ALTERNATIVE,
    val supportingCount: Int? = null
)

data class StudyLearningActionCallbacks(
    val continueLearning: () -> Unit,
    val startNewConfigured: () -> Unit = {},
    val reviewLatestNew: () -> Unit,
    val reviewAgainHard: () -> Unit,
    val reviewAllLearned: () -> Unit,
    val backToLibrary: () -> Unit
)

fun dispatchStudyLearningAction(
    action: StudyLearningAction,
    callbacks: StudyLearningActionCallbacks
) {
    when (action) {
        StudyLearningAction.CONTINUE -> callbacks.continueLearning()
        StudyLearningAction.START_NEW_CONFIGURED -> callbacks.startNewConfigured()
        StudyLearningAction.REVIEW_LATEST_NEW -> callbacks.reviewLatestNew()
        StudyLearningAction.REVIEW_AGAIN_HARD -> callbacks.reviewAgainHard()
        StudyLearningAction.REVIEW_ALL_LEARNED -> callbacks.reviewAllLearned()
        StudyLearningAction.BACK_TO_LIBRARY -> callbacks.backToLibrary()
    }
}

fun resolveStudyLearningActions(
    uiState: StudyUiState,
    strings: LearningEntryStrings = LearningEntryStrings.ENGLISH,
    continueEnabled: Boolean = uiState.hasActiveSession || uiState.activeInstalledPackageId != null,
    replayEnabled: Boolean? = null
): List<StudyLearningActionPresentation> {
    val availability = uiState.learnEntryReviewAvailability
    val latest = availability?.latestCompletedNewItems as? LatestCompletedNewItemsAvailability.Available
    val difficult = availability?.difficultItems as? DifficultItemsReviewAvailability.Available
    val learned = availability?.learnedItems as? LearnedItemsReviewAvailability.Available
    return listOf(
        StudyLearningActionPresentation(
            action = StudyLearningAction.CONTINUE,
            label = if (uiState.hasActiveSession) strings.resume else strings.continueStudy,
            description = if (uiState.hasActiveSession) strings.resumeDescription else strings.continueDescription,
            enabled = continueEnabled,
            priority = if (continueEnabled) LearningEntryActionPriority.PRIMARY else LearningEntryActionPriority.ALTERNATIVE
        ),
        StudyLearningActionPresentation(
            action = StudyLearningAction.START_NEW_CONFIGURED,
            label = strings.startNewConfigured,
            description = uiState.nextSessionConfiguration?.let {
                strings.startNewConfiguredDescription(it.newLimit, it.reviewLimit)
            } ?: strings.startNewConfiguredFallbackDescription,
            enabled = continueEnabled,
            priority = LearningEntryActionPriority.ALTERNATIVE
        ),
        StudyLearningActionPresentation(
            action = StudyLearningAction.REVIEW_LATEST_NEW,
            label = strings.reviewLatestNew,
            description = latest?.let { strings.reviewLatestNewAvailableDescription(it.itemCount) }
                ?: strings.reviewLatestNewUnavailableDescription,
            enabled = latest != null && (replayEnabled ?: true),
            priority = LearningEntryActionPriority.ALTERNATIVE,
            supportingCount = latest?.itemCount
        ),
        StudyLearningActionPresentation(
            action = StudyLearningAction.REVIEW_AGAIN_HARD,
            label = strings.reviewAgainHard,
            description = difficult?.let {
                strings.reviewAgainHardAvailableDescription(it.itemCount)
            } ?: strings.reviewAgainHardUnavailableDescription,
            enabled = difficult != null,
            priority = LearningEntryActionPriority.ALTERNATIVE,
            supportingCount = difficult?.itemCount
        ),
        StudyLearningActionPresentation(
            action = StudyLearningAction.REVIEW_ALL_LEARNED,
            label = strings.reviewAll,
            description = learned?.let { strings.reviewAllAvailableDescription(it.totalLearnedCount) }
                ?: strings.reviewAllUnavailableDescription,
            enabled = learned != null,
            priority = LearningEntryActionPriority.ALTERNATIVE,
            supportingCount = learned?.sessionItemCount
        ),
        StudyLearningActionPresentation(
            action = StudyLearningAction.BACK_TO_LIBRARY,
            label = strings.backToLibrary,
            description = strings.backToLibraryDescription,
            enabled = true,
            priority = LearningEntryActionPriority.NAVIGATION
        )
    ).filter { it.action != StudyLearningAction.START_NEW_CONFIGURED || uiState.hasActiveSession }
}

fun resolveStudyIdlePresentation(
    uiState: StudyUiState,
    strings: LearningEntryStrings = LearningEntryStrings.ENGLISH
): StudyIdlePresentation? {
    if (
        (uiState.hasActiveSession && !uiState.learnEntryChooserVisible) ||
        uiState.sessionCompleted ||
        uiState.loadError != null
    ) {
        return null
    }

    val hasLearningScope = uiState.hasActiveSession || uiState.activeInstalledPackageId != null
    val actions = resolveStudyLearningActions(
        uiState = uiState,
        strings = strings,
        continueEnabled = hasLearningScope && !uiState.actionInProgress,
        replayEnabled = if (uiState.actionInProgress) false else null
    )
    val primary = actions.singleOrNull { it.priority == LearningEntryActionPriority.PRIMARY }
    return StudyIdlePresentation(
        title = if (hasLearningScope) strings.heading else strings.noContextTitle,
        description = if (hasLearningScope) strings.description else strings.noContextDescription,
        actionLabel = primary?.label ?: strings.backToLibrary,
        shortcutHint = if (primary != null) "Enter or Space" else "F5",
        context = LearningEntryContextPresentation(
            scopeLabel = if (uiState.isLessonStudy) strings.lessonScope else strings.generalScope,
            title = if (hasLearningScope) uiState.studyTitle else strings.noContextTitle
        ),
        readiness = resolveLearningEntryReadiness(uiState, strings),
        actions = if (hasLearningScope) {
            actions
        } else {
            actions.filter { it.action == StudyLearningAction.BACK_TO_LIBRARY }
        }
    )
}

private fun resolveLearningEntryReadiness(
    uiState: StudyUiState,
    strings: LearningEntryStrings
): List<LearningEntryReadinessPresentation> = buildList {
    if (uiState.hasActiveSession) {
        uiState.activeSessionQueueSummary?.let {
            add(LearningEntryReadinessPresentation(LearningEntryReadinessId.ACTIVE_SESSION, strings.resumable, "${it.completed} / ${it.total}"))
            add(LearningEntryReadinessPresentation(LearningEntryReadinessId.ACTIVE_REMAINING, "Remaining", it.remaining.toString()))
            add(LearningEntryReadinessPresentation(LearningEntryReadinessId.ACTIVE_NEW_REMAINING, "New remaining", it.newRemaining.toString()))
            add(LearningEntryReadinessPresentation(LearningEntryReadinessId.ACTIVE_REVIEW_REMAINING, "Review remaining", it.reviewRemaining.toString()))
        }
    }
    uiState.nextSessionConfiguration?.let {
        add(LearningEntryReadinessPresentation(LearningEntryReadinessId.CONFIGURED_NEW, "Next session · New maximum", it.newLimit.toString()))
        add(LearningEntryReadinessPresentation(LearningEntryReadinessId.CONFIGURED_REVIEW, "Next session · Review maximum", it.reviewLimit.toString()))
    }
    val learned = uiState.learnEntryReviewAvailability?.learnedItems as? LearnedItemsReviewAvailability.Available
    learned?.let {
        add(LearningEntryReadinessPresentation(LearningEntryReadinessId.LEARNED, strings.learnedAvailable, it.totalLearnedCount.toString()))
    }
}
