package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidFocusedPracticeCompositionTest {
    @Test
    fun `Review Hub exposes both focused practice entries outside Learn New`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        )
        val reviewHub = source.substringAfter("fun ReviewHub(").substringBefore("fun SettingsScreen(")
        assertTrue(reviewHub.contains("Ôn từ vừa học"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.LATEST_SESSION"))
        assertTrue(reviewHub.contains("Ôn Again / Hard"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.DIFFICULT"))
        val studyLanding = source.substringAfter("fun StudyLanding(").substringBefore("fun ReviewHub(")
        assertFalse(studyLanding.contains("Ôn từ vừa học"))
        assertFalse(studyLanding.contains("Ôn Again / Hard"))
    }

    @Test
    fun `focused practice hides canonical scheduler rating controls`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        assertTrue(
            source.contains(
                "plan.provenance == RecallProvenance.PRACTICE && state.hud?.focusedPractice != true"
            )
        )
    }
}
