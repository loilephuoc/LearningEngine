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
    ).substringAfter("fun ReviewHub(").substringBefore("@Composable\nprivate fun QuickReviewInsightsCard(")
    private val fullSource = Files.readString(
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
        assertTrue(source.contains("R.string.review_quick_badge"))
        assertTrue(source.contains("R.string.review_quick_desc"))
        assertTrue(source.contains("R.string.review_difficult_desc"))
        assertTrue(source.contains("R.string.review_learned_desc"))
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
        assertTrue(source.contains("clickable(enabled = action.available)"))
        assertFalse(source.contains("LearningEngineSecondaryButton("))
        assertFalse(source.contains("review_action_switch"))
        assertFalse(source.contains("review_action_start"))
    }

    @Test
    fun `runtime-only Quick Review summary is compact accessible and delegates difficult entry`() {
        assertTrue(fullSource.contains("quickReviewSummary?.takeIf { it.totalExposures > 0 }"))
        assertTrue(fullSource.contains("R.string.review_recent_summary_title"))
        assertTrue(fullSource.contains("${'$'}{summary.totalExposures} lượt · Lướt ${'$'}{summary.skipped}"))
        assertTrue(fullSource.contains("${'$'}ratingAgain ${'$'}{summary.again} · ${'$'}ratingHard ${'$'}{summary.hard}"))
        assertTrue(fullSource.contains("clearAndSetSemantics { contentDescription = semanticSummary }"))
        assertTrue(fullSource.contains("AndroidStudyEvent.Start(AndroidSessionEntry.DIFFICULT)"))
        assertTrue(fullSource.contains("difficultAvailable = home.availability.canStartDifficultPractice"))
        assertFalse(fullSource.contains("studyQueue"))
    }
}
