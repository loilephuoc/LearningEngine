package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionCompletionPresentationTest {
    @Test
    fun `completion always resolves one primary acknowledgement hierarchy`() {
        val presentation = resolve(actions())

        assertEquals(CompletionVisualRole.ACKNOWLEDGEMENT, presentation.acknowledgementRole)
        assertEquals(CompletionVisualRole.OUTCOME, presentation.outcomeRole)
        assertEquals(CompletionVisualRole.CONSEQUENCE, presentation.consequenceRole)
        assertEquals(CompletionVisualRole.ACTIONS, presentation.actionRole)
    }

    @Test
    fun `only first enabled continuation action becomes primary`() {
        val presentation = resolve(actions())
        val primary = presentation.actions.filter { it.priority == CompletionActionPriority.PRIMARY }

        assertEquals(1, primary.size)
        assertEquals(CompletionActionIdentity.CONTINUE, primary.single().identity)
        assertTrue(primary.single().enabled)
    }

    @Test
    fun `disabled continue yields enabled replay without promoting disabled action`() {
        val presentation = resolve(
            actions(continueEnabled = false, replayEnabled = true, reviewAllEnabled = true)
        )

        assertEquals(
            CompletionActionIdentity.REPLAY_LATEST,
            presentation.actions.single { it.priority == CompletionActionPriority.PRIMARY }.identity
        )
        assertFalse(
            presentation.actions.single { it.identity == CompletionActionIdentity.CONTINUE }.enabled
        )
    }

    @Test
    fun `no available continuation creates no fake primary`() {
        val presentation = resolve(
            actions(continueEnabled = false, replayEnabled = false, reviewAllEnabled = false)
        )

        assertTrue(presentation.actions.none { it.priority == CompletionActionPriority.PRIMARY })
        assertEquals(
            CompletionActionPriority.TERTIARY,
            presentation.actions.single {
                it.identity == CompletionActionIdentity.BACK_TO_LIBRARY
            }.priority
        )
    }

    @Test
    fun `Undo is recovery and Continuous Review is toggle never primary`() {
        val presentation = resolve(actions(), undo = true, continuous = true)

        assertEquals(
            CompletionActionPriority.RECOVERY,
            presentation.actions.single { it.identity == CompletionActionIdentity.UNDO }.priority
        )
        assertEquals(
            CompletionActionPriority.TOGGLE,
            presentation.actions.single {
                it.identity == CompletionActionIdentity.CONTINUOUS_REVIEW
            }.priority
        )
    }

    @Test
    fun `outcome summary contains projected facts and omits empty optional groups`() {
        val presentation = resolve(actions(), reviewed = 4, newItems = 0, reviewItems = 3)

        assertEquals(
            listOf(
                SessionCompletionOutcomeItem("Total reviews", 4),
                SessionCompletionOutcomeItem("Scheduled", 3)
            ),
            presentation.outcomeItems
        )
        assertTrue(presentation.outcomeItems.none { it.label == "New" })
    }

    @Test
    fun `resolver is deterministic and viewport theme independent`() {
        val first = resolve(actions(), backToLesson = true, undo = true, continuous = true)
        val second = resolve(actions(), backToLesson = true, undo = true, continuous = true)

        assertEquals(first, second)
    }

    @Test
    fun `learning action identity is typed and never derived from display text`() {
        StudyLearningAction.entries.forEach { action ->
            assertNotNull(action.toCompletionIdentity())
        }
        val source = source("SessionCompletionPresentation.kt")
        assertFalse(source.contains("label.contains"))
        assertFalse(source.contains("display"))
    }

    @Test
    fun `completion card renders semantic hierarchy with existing callbacks`() {
        val card = source("SessionCompletionCard.kt")
        val screen = source("StudyScreen.kt")

        assertTrue(card.contains("modifier = Modifier.semantics { heading() }"))
        assertTrue(card.contains("completionUiState.learningActions.chunked(2)"))
        assertTrue(card.contains("onLearningAction(action.action)"))
        assertTrue(card.contains("onContinueLearning(pkgId, contentId)"))
        assertTrue(card.contains("onContinuousReviewChanged"))
        assertTrue(card.contains("onClick = onUndo"))
        assertTrue(screen.contains("schedulerFeedback = uiState.schedulerFeedback"))
        assertTrue(screen.contains("!uiState.sessionCompleted"))
    }

    @Test
    fun `presentation has no learning authority or gamification metrics`() {
        val source = source("SessionCompletionPresentation.kt")
        listOf(
            "repository", "persistence", "scheduler", "FSRS", "queue", "streak", "points",
            "XP", "accuracy", "mastery", "excellent", "perfect"
        ).forEach { forbidden -> assertFalse(source.contains(forbidden, ignoreCase = true), forbidden) }
        assertNull(SessionCompletionUiState().recommendationReason)
    }

    private fun resolve(
        learningActions: List<StudyLearningActionPresentation>,
        reviewed: Int = 3,
        newItems: Int = 1,
        reviewItems: Int = 2,
        backToLesson: Boolean = false,
        undo: Boolean = false,
        continuous: Boolean = false
    ) = SessionCompletionPresentationResolver.resolve(
        state = SessionCompletionUiState(
            reviewedCount = reviewed,
            newItemsReviewed = newItems,
            reviewItemsReviewed = reviewItems,
            learningActions = learningActions
        ),
        actionsEnabled = true,
        backToLessonAvailable = backToLesson,
        undoAvailable = undo,
        continuousReviewAvailable = continuous
    )

    private fun actions(
        continueEnabled: Boolean = true,
        replayEnabled: Boolean = true,
        reviewAllEnabled: Boolean = true
    ) = listOf(
        action(StudyLearningAction.CONTINUE, continueEnabled),
        action(StudyLearningAction.REPLAY_LATEST, replayEnabled),
        action(StudyLearningAction.REVIEW_ALL_LEARNED, reviewAllEnabled),
        action(StudyLearningAction.BACK_TO_LIBRARY, true)
    )

    private fun action(action: StudyLearningAction, enabled: Boolean) =
        StudyLearningActionPresentation(action, action.name, "Existing action", enabled)

    private fun source(name: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
}
