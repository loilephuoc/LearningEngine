package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test

class AndroidHomeExperienceTest {
    @Test
    fun `active session is always the primary action and preserves exact identity`() {
        val action = selectHomePrimaryAction("active-session-42", dueCount = 12, hasStudyScope = true)

        assertEquals("active-session-42", assertIs<AndroidHomePrimaryAction.Resume>(action).sessionId)
    }

    @Test
    fun `due review precedes start learning only when canonical scope exists`() {
        assertIs<AndroidHomePrimaryAction.ReviewDue>(selectHomePrimaryAction(null, 3, true))
        assertIs<AndroidHomePrimaryAction.OpenLibrary>(selectHomePrimaryAction(null, 3, false))
    }

    @Test
    fun `canonical study scope creates start learning and no content opens Library`() {
        assertIs<AndroidHomePrimaryAction.StartLearning>(selectHomePrimaryAction(null, 0, true))
        assertIs<AndroidHomePrimaryAction.OpenLibrary>(selectHomePrimaryAction(null, 0, false))
    }

    @Test
    fun `home model exposes only bounded immutable presentation values`() {
        val model = AndroidHomeUiModel(
            AndroidHomePrimaryAction.StartLearning, "Package", 1, 0, 0, 8, 75, 3, 4
        )

        assertTrue(model.hasContent)
        assertFalse(model.hasDueReview)
        assertEquals(0.75f, model.learningProgress)
    }

    @Test
    fun `home uses canonical dashboard and installed package projections`() {
        val source = source("vn/loi/learning/android/study/AndroidStudyFacade.kt")
        val home = source.substringAfter("fun home()").substringBefore("fun start(")

        assertTrue(home.contains("context.dashboard.query("))
        assertTrue(home.contains("context.installedPackages.query()"))
        assertFalse(home.contains("Repository"))
    }

    @Test
    fun `home is state hoisted and renders stable accessible sections`() {
        val source = source("vn/loi/learning/android/study/StudyScreen.kt")

        listOf("item(\"header\")", "item(\"hero\")", "item(\"stats\")", "item(\"due-review\")",
            "item(\"progress\")", "LearningEngineHeroCard", "LearningEngineStatTile",
            "LearningEngineActionCard", "LearningEngineProgress", "heading()"
        ).forEach { assertTrue(source.contains(it), it) }
        assertFalse(source.contains("Color("))
        assertFalse(source.contains("facade."))
        assertTrue(source.contains("LearningSpacing"))
    }

    @Test
    fun `empty and root failure remain actionable and non blank`() {
        val home = source("vn/loi/learning/android/study/StudyScreen.kt")
        val root = source("vn/loi/learning/android/MainActivity.kt")

        assertTrue(home.contains("LearningEngineEmptyState("))
        assertTrue(home.contains("actionLabel = \"Import package\""))
        assertTrue(root.contains("AndroidFeatureFailure(\"Learning overview unavailable\""))
        assertTrue(root.contains("AndroidStudyEvent.Retry"))
    }

    @Test
    fun `theme remains outside graph and home view model ownership`() {
        val activity = source("vn/loi/learning/android/MainActivity.kt")
        val viewModel = source("vn/loi/learning/android/study/AndroidStudyViewModel.kt")

        assertTrue(activity.indexOf("LearningEngineTheme(mode = themeMode)") < activity.indexOf("app.graph"))
        assertEquals(1, Regex("launchOperation\\(\"study_initial_load\"").findAll(viewModel).count())
        assertTrue(viewModel.contains("operationMutex.withLock"))
        assertTrue(viewModel.contains("facade.loadExact(it.sessionId)"))
    }

    private fun source(relative: String): String = Files.readString(Path.of("src/main/kotlin").resolve(relative))
}
