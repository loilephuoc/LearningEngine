package vn.loi.learning.desktop.notification

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopVocabularyReminderArchitectureTest {
    @Test
    fun `selector owns read sources only and notification production avoids mutation APIs`() {
        val sourceDirectory = File("src/main/kotlin/vn/loi/learning/desktop/notification")
        assertTrue(sourceDirectory.isDirectory)
        val sources = sourceDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        listOf(
            "ReviewSessionItemUseCase",
            "StudyQueueService",
            "StartStudySessionUseCase",
            "StartLatestCompletedNewItemsReviewUseCase",
            "StartDifficultItemsReviewUseCase",
            "StartLearnedItemsReviewUseCase",
            "StartContinuousSkimPracticeUseCase",
            "ReviewSessionItemCommand",
            "FsrsScheduler",
            ".append("
        ).forEach { forbidden ->
            assertFalse(sources.contains(forbidden), "Forbidden notification dependency: $forbidden")
        }
        val selector = File(sourceDirectory, "DesktopVocabularyReminderCandidateSelector.kt").readText()
        listOf(
            "DesktopInstalledPackageReadSource",
            "DesktopPackageContentReadSource",
            "DesktopContentReadSource",
            "DesktopLearningItemReadSource",
            "DesktopMemoryStateReadSource",
            "DesktopContentLearningStateReadSource"
        ).forEach { expected -> assertTrue(selector.contains(expected), expected) }
    }
}
