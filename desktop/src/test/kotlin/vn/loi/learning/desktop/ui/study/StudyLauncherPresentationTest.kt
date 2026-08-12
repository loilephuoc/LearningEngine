package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopContentWidthClass

class StudyLauncherPresentationTest {
    @Test
    fun `launcher uses shared narrow medium and wide responsive policy`() {
        assertEquals(DesktopContentWidthClass.NARROW, resolveStudyLauncherLayout(619).widthClass)
        assertEquals(1, resolveStudyLauncherLayout(619).primaryColumns)
        assertEquals(2, resolveStudyLauncherLayout(619).quickReviewColumns)
        assertEquals(DesktopContentWidthClass.MEDIUM, resolveStudyLauncherLayout(620).widthClass)
        assertEquals(2, resolveStudyLauncherLayout(999).primaryColumns)
        assertEquals(DesktopContentWidthClass.WIDE, resolveStudyLauncherLayout(1000).widthClass)
        assertEquals(3, resolveStudyLauncherLayout(1000).quickReviewColumns)
    }

    @Test
    fun `launcher groups existing actions without changing action identity or enabled state`() {
        val actions = StudyLearningAction.entries.map { action ->
            StudyLearningActionPresentation(action, action.name, "description", action != StudyLearningAction.REVIEW_LATEST_NEW,
                if (action == StudyLearningAction.BACK_TO_LIBRARY) LearningEntryActionPriority.NAVIGATION else LearningEntryActionPriority.ALTERNATIVE)
        }
        val sections = resolveStudyLauncherSections(presentation(actions))

        assertEquals(listOf(StudyLearningAction.CONTINUE, StudyLearningAction.START_NEW_CONFIGURED), sections.primary.map { it.action })
        assertEquals(listOf(StudyLearningAction.REVIEW_LATEST_NEW, StudyLearningAction.REVIEW_AGAIN_HARD, StudyLearningAction.REVIEW_ALL_LEARNED), sections.quickReview.map { it.action })
        assertFalse(sections.quickReview.first().enabled)
        assertEquals(listOf(StudyLearningAction.BACK_TO_LIBRARY), sections.navigation.map { it.action })
    }

    @Test
    fun `session details start collapsed and launcher omits long default descriptions`() {
        assertFalse(initialSessionDetailsExpanded())
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"))
        val launcher = source.substringAfter("private fun StudyIdleCard(").substringBefore("private fun StudyItemCard(")
        assertTrue(launcher.contains("AnimatedVisibility(visible = detailsExpanded)"))
        assertTrue(launcher.contains("enabled = active"))
        assertFalse(launcher.contains("text = presentation.description"))
        assertFalse(launcher.contains("text = action.description"))
        assertFalse(launcher.contains("Cách học"))
        assertFalse(launcher.contains("Lượt & Ôn"))
        assertFalse(launcher.contains("StudyWorkflowSelection"))
    }

    @Test
    fun `continue and new session actions dispatch directly to distinct callbacks`() {
        val calls = mutableListOf<String>()
        val callbacks = StudyLearningActionCallbacks(
            continueLearning = { calls += "continue" },
            startNewConfigured = { calls += "start-new" },
            reviewLatestNew = {},
            reviewAgainHard = {},
            reviewAllLearned = {},
            backToLibrary = {}
        )

        dispatchStudyLearningAction(StudyLearningAction.CONTINUE, callbacks)
        dispatchStudyLearningAction(StudyLearningAction.START_NEW_CONFIGURED, callbacks)

        assertEquals(listOf("continue", "start-new"), calls)
    }

    @Test
    fun `new session launcher has no confirmation state or modal`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"))
        val callbacks = screen.substringAfter("val learningActionCallbacks =").substringBefore("val onLearningAction:")

        assertTrue(callbacks.contains("startNewConfigured = onStartNewStudy"))
        assertFalse(screen.contains("StudyWorkflowSelection"))
        assertFalse(screen.contains("confirmStartNew"))
        assertFalse(screen.contains("Bắt đầu phiên mới?"))
    }

    @Test
    fun `new session remains protected by the view model in flight guard`() {
        val viewModel = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyViewModel.kt"))
        val updateSafely = viewModel.substringAfter("private fun updateSafely(")

        assertTrue(viewModel.contains("fun confirmStartNewConfiguredSession() = updateSafely("))
        assertTrue(updateSafely.contains("if (!actionInProgress)"))
        assertTrue(updateSafely.contains("actionInProgress = true"))
    }

    @Test
    fun `launcher source preserves Vietnamese Unicode output`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"))
        val launcher = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyLauncherPresentation.kt"))
        assertTrue(screen.contains("còn lại"))
        assertTrue(screen.contains("không khả dụng"))
        listOf("Ã", "áº", "á»", "Æ°").forEach { marker ->
            assertFalse(screen.substringAfter("private fun StudyIdleCard(").substringBefore("private fun StudyItemCard(").contains(marker))
            assertFalse(launcher.contains(marker))
        }
    }

    private fun presentation(actions: List<StudyLearningActionPresentation>) = StudyIdlePresentation(
        title = "Học",
        description = "description",
        actionLabel = "action",
        shortcutHint = "Enter",
        context = LearningEntryContextPresentation("Phạm vi", "Package"),
        actions = actions
    )
}
