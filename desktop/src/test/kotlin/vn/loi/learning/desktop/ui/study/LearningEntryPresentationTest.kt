package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.application.packageprogress.StudyPackageLearningStatistics
import vn.loi.learning.application.packageprogress.StudySessionProgressStatistics
import vn.loi.learning.application.session.LearnEntryReviewAvailability
import vn.loi.learning.application.session.LearnedItemsReviewAvailability
import vn.loi.learning.application.session.LatestCompletedSessionAvailability
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

class LearningEntryPresentationTest {
    @Test
    fun `current projected title and lesson role form context without technical ids`() {
        val presentation = resolveStudyIdlePresentation(scopedState(isLesson = true, title = "Travel Basics"))!!

        assertEquals("Travel Basics", presentation.context.title)
        assertEquals("Selected lesson", presentation.context.scopeLabel)
        assertFalse(presentation.context.title.contains("pkg"))
    }

    @Test
    fun `active resumable session owns the only primary action`() {
        val presentation = resolveStudyIdlePresentation(
            scopedState().copy(hasActiveSession = true, learnEntryChooserVisible = true)
        )!!

        assertEquals(StudyLearningAction.CONTINUE, presentation.primaryAction?.action)
        assertEquals("Resume active session", presentation.primaryAction?.label)
        assertEquals(1, presentation.actions.count { it.priority == LearningEntryActionPriority.PRIMARY })
    }

    @Test
    fun `ordinary scoped study owns primary when no active session exists`() {
        val presentation = resolveStudyIdlePresentation(scopedState())!!

        assertEquals("Continue learning", presentation.primaryAction?.label)
    }

    @Test
    fun `busy state exposes no enabled primary`() {
        val presentation = resolveStudyIdlePresentation(scopedState().copy(actionInProgress = true))!!

        assertNull(presentation.primaryAction)
        assertTrue(presentation.actions.none { it.enabled && it.priority == LearningEntryActionPriority.PRIMARY })
    }

    @Test
    fun `replay and Review All remain alternatives when available`() {
        val presentation = resolveStudyIdlePresentation(scopedState(availability = availableReviews()))!!

        val replay = presentation.actions.single { it.action == StudyLearningAction.REPLAY_LATEST }
        val reviewAll = presentation.actions.single { it.action == StudyLearningAction.REVIEW_ALL_LEARNED }
        assertTrue(replay.enabled)
        assertTrue(reviewAll.enabled)
        assertEquals(LearningEntryActionPriority.ALTERNATIVE, replay.priority)
        assertEquals(LearningEntryActionPriority.ALTERNATIVE, reviewAll.priority)
    }

    @Test
    fun `Library is typed navigation and never primary`() {
        val library = resolveStudyIdlePresentation(scopedState())!!.actions.single {
            it.action == StudyLearningAction.BACK_TO_LIBRARY
        }

        assertEquals(LearningEntryActionPriority.NAVIGATION, library.priority)
    }

    @Test
    fun `no scope creates valid no-work presentation without study action`() {
        val presentation = resolveStudyIdlePresentation(StudyUiState())!!

        assertNull(presentation.primaryAction)
        assertTrue(presentation.actions.none { it.action == StudyLearningAction.CONTINUE })
        assertEquals(listOf(StudyLearningAction.BACK_TO_LIBRARY), presentation.actions.filter { it.enabled }.map { it.action })
    }

    @Test
    fun `readiness uses projected New Review and learned facts`() {
        val presentation = resolveStudyIdlePresentation(
            scopedState(availability = availableReviews()).copy(headerStatistics = availableStatistics())
        )!!

        assertEquals(
            mapOf(
                LearningEntryReadinessId.NEW to "3",
                LearningEntryReadinessId.REVIEW to "4",
                LearningEntryReadinessId.LEARNED to "12"
            ),
            presentation.readiness.associate { it.id to it.value }
        )
    }

    @Test
    fun `resolver is deterministic and independent of viewport or theme`() {
        val state = scopedState(availability = availableReviews()).copy(headerStatistics = availableStatistics())

        assertEquals(resolveStudyIdlePresentation(state), resolveStudyIdlePresentation(state))
    }

    @Test
    fun `null lesson and topic remain safe general context`() {
        val presentation = resolveStudyIdlePresentation(scopedState(isLesson = false))!!

        assertEquals("Current package or topic", presentation.context.scopeLabel)
    }

    @Test
    fun `typed error remains owned by existing recovery presentation`() {
        assertNull(resolveStudyIdlePresentation(scopedState().copy(loadError = "sanitized", failureKind = StudyFailureKind.PREPARATION)))
    }

    @Test
    fun `dispatch preserves every existing callback identity`() {
        val calls = mutableListOf<String>()
        val callbacks = StudyLearningActionCallbacks(
            continueLearning = { calls += "continue" },
            replayLatestCompletedSession = { calls += "replay" },
            reviewAllLearned = { calls += "review-all" },
            backToLibrary = { calls += "library" }
        )
        StudyLearningAction.entries.forEach { dispatchStudyLearningAction(it, callbacks) }

        assertEquals(listOf("continue", "replay", "review-all", "library"), calls)
    }

    @Test
    fun `English and Vietnamese localization contain complete learning entry vocabulary`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt"))
        val strings = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyWorkspaceStrings.kt"))

        assertTrue(strings.contains("What would you like to learn?"))
        assertTrue(strings.contains("Alternative study modes"))
        assertTrue(source.contains("Bạn muốn học gì?"))
        assertTrue(source.contains("Cách học thay thế"))
    }

    @Test
    fun `Compose renders semantic hierarchy and forwards existing callbacks only`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"))

        assertTrue(screen.contains("presentation.context.title"))
        assertTrue(screen.contains("presentation.readiness.forEach"))
        assertTrue(screen.contains("presentation.primaryAction"))
        assertTrue(screen.contains("onLearningAction(action.action)"))
        assertTrue(screen.contains("onBackToLibrary?.invoke()"))
        assertFalse(screen.contains("idle.actionLabel =="))
    }

    @Test
    fun `presentation owns no learning planning persistence or gamification`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyIdlePresentation.kt"))
        listOf("Repository", "Facade", "startSession(", "queueService", "mastery", "streak", "score", "XP").forEach {
            assertFalse(source.contains(it, ignoreCase = true), "Forbidden authority or invented metric: $it")
        }
    }

    private fun scopedState(
        isLesson: Boolean = false,
        title: String = "All learning items",
        availability: LearnEntryReviewAvailability? = null
    ) = StudyUiState(
        activeInstalledPackageId = InstalledPackageId("pkg"),
        studyTitle = title,
        isLessonStudy = isLesson,
        learnEntryReviewAvailability = availability
    )

    private fun availableReviews() = LearnEntryReviewAvailability(
        LatestCompletedSessionAvailability.Available(SessionId("done"), 3),
        LearnedItemsReviewAvailability.Available(totalLearnedCount = 12, sessionItemCount = 5)
    )

    private fun availableStatistics() = StudyHeaderStatisticsState.Available(
        StudyHeaderStatistics(
            session = StudySessionProgressStatistics("session", 2, 5, 5, 1, 5, 5),
            packageLearning = StudyPackageLearningStatistics("scope", Moment(1), 12, 4, 3, 3, 3, 3, null)
        )
    )
}
