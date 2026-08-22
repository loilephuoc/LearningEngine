package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs
import org.junit.Test

class AndroidHomeStudyLandingCompositionTest {
    @Test
    fun `active session projection preserves canonical context and progress values`() {
        val state = home(
            canResume = true,
            action = AndroidHomePrimaryAction.Resume("session-1"),
            title = "A very long canonical package title that must remain learner visible",
            due = 7,
            reviewed = 12,
            accuracy = 83,
            active = 34,
            total = 50
        )

        val result = resolveLearningLandingPresentation(state)

        assertTrue(result.hasActiveSession)
        assertEquals(state.model.primaryAction, result.primaryAction)
        assertEquals(state.model.contextTitle, result.contextTitle)
        assertEquals(7, result.dueCount)
        assertEquals(12, result.reviewedToday)
        assertEquals(83, result.accuracyPercent)
        assertEquals(34f / 50f, result.learningProgress)
    }

    @Test
    fun `no session and zero due remain quiet without inventing availability`() {
        val state = home(
            canResume = false,
            action = AndroidHomePrimaryAction.StartLearning,
            title = "Current package",
            due = 0,
            reviewed = 0,
            accuracy = null,
            active = 0,
            total = 0
        )

        val result = resolveLearningLandingPresentation(state)

        assertFalse(result.hasActiveSession)
        assertEquals(0, result.dueCount)
        assertEquals(0f, result.learningProgress)
        assertTrue(result.hasContent)
    }

    @Test
    fun `Home and Study landing consume shared product components and canonical navigation`() {
        val homeSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        val landingSource = source("vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        val mainSource = source("vn/loi/learning/android/MainActivity.kt")
        val components = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")

        listOf("LearningEngineHeroCard", "LearningEngineStatTile", "LearningEngineActionCard").forEach {
            assertTrue(components.contains("fun $it"))
            assertTrue(homeSource.contains(it) || landingSource.contains(it))
        }
        assertFalse(homeSource.contains("item(\"navigation\")"))
        assertFalse(homeSource.contains("\"XP\""))
        assertFalse(homeSource.contains("\"Streak\""))
        assertFalse(landingSource.contains("Session Availability"))
        assertTrue(mainSource.contains("onReview = { navController.navigate(\"review\") }"))
        assertTrue(components.contains("maxLines = 2"))
        assertTrue(landingSource.contains("when (primaryAction)"))
        assertTrue(landingSource.contains("AndroidHomePrimaryAction.OpenLibrary -> onLibrary()"))
        assertFalse(landingSource.contains("presentation.hasContent -> onEvent(AndroidStudyEvent.Start"))
        listOf("R.string.study_mode_learn_new", "R.string.study_mode_adaptive", "R.string.study_mode_typing").forEach { resKey ->
            assertTrue(landingSource.contains(resKey))
        }
        assertFalse(landingSource.contains("AndroidHomePrimaryAction.StartLearning -> Unit"))
        assertTrue(landingSource.contains("enabled = home.availability.canStartTyping"))
        assertFalse(landingSource.contains("\"Review due\""))
        assertTrue(homeSource.contains("onStudyLauncher()"))
    }

    @Test
    fun `installed content without selected package exposes Library path instead of Start Study`() {
        val state = home(
            canResume = false,
            action = AndroidHomePrimaryAction.OpenLibrary,
            title = null,
            due = 8,
            reviewed = 4,
            accuracy = 75,
            active = 20,
            total = 20
        )

        val result = resolveLearningLandingPresentation(state)
        assertTrue(result.hasContent)
        assertEquals(20, result.activeMemoryCount)
        assertIs<AndroidHomePrimaryAction.OpenLibrary>(result.primaryAction)
        assertEquals(null, result.contextTitle)
    }

    @Test
    fun `Home due review remains conditional while Study launcher avoids a duplicate review module`() {
        val homeSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        val landingSource = source("vn/loi/learning/android/ui/AndroidRootNavigation.kt")

        assertTrue(homeSource.contains("state.availability.canStartReview && presentation.dueCount > 0"))
        assertFalse(landingSource.contains("home.availability.canStartReview && presentation.dueCount > 0"))
        assertFalse(homeSource.contains("StudyHeaderStatisticsQueryService"))
        assertFalse(landingSource.contains("context.engine"))
    }

    @Test
    fun `Study home identifies the canonical active package and provides a Library switch path`() {
        val source = source("vn/loi/learning/android/study/StudyScreen.kt")
        val card = source.substringAfter("private fun ActiveLearningPackageCard(").substringBefore("private fun ContinueLearningCard(")
        assertTrue(source.contains("item(\"active-package\")"))
        assertTrue(card.contains("model.activePackageName"))
        assertTrue(card.contains("model.activePackageContentCount"))
        assertTrue(card.contains("onClick = onLibrary"))
        assertTrue(card.contains("GÓI ĐANG HỌC"))
        assertTrue(card.contains("Đổi gói"))
    }

    private fun home(
        canResume: Boolean,
        action: AndroidHomePrimaryAction,
        title: String?,
        due: Int,
        reviewed: Int,
        accuracy: Int?,
        active: Int,
        total: Int
    ) = AndroidStudyState.Home(
        availability = AndroidSessionEntryAvailability(
            canStartReview = !canResume,
            canResume = canResume,
            canStartLatestSessionPractice = false,
            canStartDifficultPractice = false,
            canStartLearnedReview = false
        ),
        model = AndroidHomeUiModel(
            primaryAction = action,
            contextTitle = title,
            installedPackageCount = 1,
            dueCount = due,
            overdueCount = 0,
            reviewedToday = reviewed,
            accuracyPercent = accuracy,
            activeMemoryCount = active,
            totalMemoryCount = total
        )
    )

    private fun source(relative: String) = Files.readString(Path.of("src/main/kotlin").resolve(relative))
}
