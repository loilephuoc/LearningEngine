package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidImmersiveStudyCompositionTest {
    private val screen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
    )
    private val components = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/ui/LearningEngineComponents.kt")
    )

    @Test
    fun `Introduction front is meaning and hero first without answer controls`() {
        assertTrue(screen.contains("state.meaning ?: \"Nghĩa tiếng Việt\""))
        assertTrue(screen.contains("Learning image, tap to discover"))
        assertTrue(screen.contains("Tap to discover the English word"))
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction && state.revealed)"))
    }

    @Test
    fun `revealed Introduction uses non-overlapping stage and persistent four way dock`() {
        assertFalse(screen.contains("label = \"introduction reveal\""))
        assertFalse(screen.contains("AnimatedContent(\n                targetState = state.revealed"))
        assertTrue(screen.contains("IntroductionRatingDock"))
        listOf("Again", "Hard", "Good", "Easy").forEach { rating ->
            assertTrue(screen.contains("RatingDockButton(\"$rating\""))
        }
        assertFalse(screen.contains("\"Expected Answer\""))
        assertFalse(screen.contains("\"Meaning\""))
        assertFalse(screen.contains("\"Example\""))
        assertFalse(screen.contains("\"Translation\""))
    }

    @Test
    fun `all canonical review modes remain in the immersive stage`() {
        listOf("Typing", "MultipleChoice", "Listening", "ImageRecall", "ExampleCompletion").forEach { mode ->
            assertTrue(screen.contains("is AndroidStudyState.$mode"))
        }
        assertTrue(screen.contains("AnswerField("))
        assertTrue(screen.contains("imePadding()"))
        assertTrue(screen.contains("defaultMinSize(minHeight = 56.dp)"))
    }

    @Test
    fun `HUD stays projected state and composition has no data authority`() {
        assertTrue(screen.contains("state.hud?.let { StudySessionHud(it) }"))
        assertFalse(screen.contains("StudyHeaderStatisticsQueryService"))
        assertFalse(screen.contains("Repository"))
        assertFalse(screen.contains("context.engine"))
    }

    @Test
    fun `study colors motion and image semantics remain token driven`() {
        assertFalse(Regex("Color\\(0x[0-9A-Fa-f]+\\)").containsMatchIn(screen))
        assertTrue(screen.contains("LearningMotion.standardMillis"))
        assertTrue(screen.contains("isReducedMotionEnabled()"))
        assertTrue(components.contains("interactionDescription: String"))
        assertTrue(components.contains("contentDescription = null"))
        assertTrue(screen.contains("defaultMinSize(minHeight = LearningSpacing.touchTarget)"))
    }
}
