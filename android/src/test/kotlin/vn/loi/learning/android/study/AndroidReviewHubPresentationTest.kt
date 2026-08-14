package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidReviewHubPresentationTest {
    private val source = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt")
    ).substringAfter("fun ReviewHub(").substringBefore("fun SettingsScreen(")

    @Test
    fun `hub differentiates all canonical review modes without duplicate Quick Review entry`() {
        val actionList = source.substringAfter("val actions = listOf(").substringBefore("actions.forEach")
        assertEquals(1, Regex("AndroidSessionEntry\\.QUICK_REVIEW").findAll(actionList).count())
        listOf(
            "AndroidSessionEntry.QUICK_REVIEW",
            "AndroidSessionEntry.LATEST_SESSION",
            "AndroidSessionEntry.DIFFICULT",
            "AndroidSessionEntry.LEARNED"
        ).forEach { assertTrue(source.contains(it)) }
        assertTrue(source.contains("NHANH · KHÔNG GIỚI HẠN"))
        assertTrue(source.contains("Vuốt lên để lướt"))
        assertTrue(source.contains("Again / Hard / Good / Easy"))
        assertTrue(source.contains("Phiên ôn hữu hạn có đánh giá"))
    }

    @Test
    fun `hub preserves eligibility authority and canonical entry callbacks`() {
        assertTrue(source.contains("home.availability.canStartLearnedReview"))
        assertTrue(source.contains("home.availability.canStartLatestSessionPractice"))
        assertTrue(source.contains("home.availability.canStartDifficultPractice"))
        assertTrue(source.contains("onEvent(AndroidStudyEvent.Start(action.entry))"))
        assertTrue(source.contains("enabled = action.available"))
        assertFalse(source.contains("context.engine"))
        assertFalse(source.contains("ReviewEvent"))
    }

    @Test
    fun `hub exposes compact accessible availability without decorative icon speech`() {
        assertTrue(source.contains("LearningEngineCompactCard("))
        assertTrue(source.contains("defaultMinSize(minHeight = LearningSpacing.touchTarget)"))
        assertTrue(source.contains("stateDescription = if (action.available) \"Available\" else \"Unavailable\""))
        assertTrue(source.contains("contentDescription = null"))
        assertTrue(source.contains("LearningEngineSecondaryButton("))
    }
}
