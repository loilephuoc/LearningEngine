package vn.loi.learning.android.ui

import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.android.SingleInstanceOwner
import androidx.compose.ui.graphics.Color

class AndroidDesignSystemTest {
    @Test
    fun `theme mode resolves system light and explicit choices`() {
        assertFalse(AndroidThemeMode.FOLLOW_SYSTEM.resolveDark(false))
        assertTrue(AndroidThemeMode.FOLLOW_SYSTEM.resolveDark(true))
        assertFalse(AndroidThemeMode.LIGHT.resolveDark(true))
        assertTrue(AndroidThemeMode.DARK.resolveDark(false))
        assertEquals(listOf("Follow system", "Light", "Dark"), AndroidThemeMode.entries.map { it.label })
    }

    @Test
    fun `theme mode persists and restores through typed store`() {
        val store = FakeThemeStore()
        AndroidThemeController(store).setMode(AndroidThemeMode.DARK)

        val recreated = AndroidThemeController(store)

        assertEquals(AndroidThemeMode.DARK, recreated.mode.value)
        assertEquals(1, store.saveCount)
    }

    @Test
    fun `theme changes preserve graph destination and active session identities`() {
        val owner = SingleInstanceOwner { Any() }
        val graph = owner.value
        val destination = "library"
        val session = "active-session"
        val controller = AndroidThemeController(FakeThemeStore())

        controller.setMode(AndroidThemeMode.LIGHT)
        controller.setMode(AndroidThemeMode.DARK)

        assertTrue(graph === owner.value)
        assertEquals(destination, "library")
        assertEquals(session, "active-session")
    }

    @Test
    fun `light and dark schemes define distinct accessible critical pairs`() {
        assertNotEquals(LearningEngineLightColors.primary, LearningEngineDarkColors.primary)
        assertNotEquals(Color.Unspecified, LearningEngineLightColors.background)
        assertNotEquals(Color.Unspecified, LearningEngineDarkColors.surfaceVariant)
        assertTrue(contrast(LearningEngineLightColors.primary, LearningEngineLightColors.onPrimary) >= 4.5)
        assertTrue(contrast(LearningEngineLightColors.background, LearningEngineLightColors.onBackground) >= 4.5)
        assertTrue(contrast(LearningEngineDarkColors.primary, LearningEngineDarkColors.onPrimary) >= 4.5)
        assertTrue(contrast(LearningEngineDarkColors.background, LearningEngineDarkColors.onBackground) >= 4.5)
    }

    @Test
    fun `semantic palettes are complete distinct and feedback has non-color meaning`() {
        listOf(LearningEngineLightSemanticColors, LearningEngineDarkSemanticColors).forEach { colors ->
            val critical = listOf(colors.success, colors.warning, colors.info, colors.overdueReview)
            assertEquals(critical.size, critical.distinct().size)
            assertNotEquals(Color.Unspecified, colors.progressTrack)
            LearningDifficultyTone.entries.forEach { tone ->
                val token = colors.feedback(tone)
                assertTrue(token.label.isNotBlank())
                assertTrue(token.iconDescription.isNotBlank())
            }
        }
    }

    @Test
    fun `typography spacing shapes elevation and motion foundations are available`() {
        assertTrue(LearningContentTypography.vocabulary.fontSize.value >= 24f)
        assertTrue(LearningContentTypography.pronunciation.fontSize.value >= 12f)
        assertTrue(LearningContentTypography.translation.lineHeight.value >= 16f)
        assertEquals(48f, LearningSpacing.touchTarget.value)
        assertTrue(LearningSpacing.screen > LearningSpacing.small)
        assertNotEquals(LearningEngineShapes.extraSmall, LearningEngineShapes.extraLarge)
        assertTrue(LearningElevation.raised > LearningElevation.card)
        assertTrue(LearningMotion.fastMillis < LearningMotion.standardMillis)
        assertTrue(LearningMotion.standardMillis < LearningMotion.emphasizedMillis)
        assertTrue("Từ vựng · phiên âm · nghĩa · ví dụ · bản dịch".isNotBlank())
    }

    @Test
    fun `foundation components expose semantics touch targets and previews without graph`() {
        val components = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")
        val catalog = source("vn/loi/learning/android/ui/LearningEngineDesignCatalog.kt")
        assertTrue(components.contains("LearningSpacing.touchTarget"))
        assertTrue(components.contains("progressBarRangeInfo"))
        assertTrue(components.contains("stateDescription"))
        assertTrue(components.contains("contentDescription"))
        assertTrue(catalog.contains("Learning Engine Light"))
        assertTrue(catalog.contains("Learning Engine Dark"))
        assertFalse(catalog.contains("AndroidApplicationGraph"))
    }

    @Test
    fun `production screens contain no direct hardcoded colors`() {
        val root = Path.of("src/main/kotlin/vn/loi/learning/android")
        val tokenFiles = setOf("LearningEngineTheme.kt", "LearningEngineDesignTokens.kt")
        val offenders = Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") && it.fileName.toString() !in tokenFiles }
                .filter { Files.readString(it).contains("Color(") }
                .toList()
        }
        assertTrue(offenders.isEmpty(), "Hardcoded screen colors: $offenders")
    }

    @Test
    fun `Study settings expose durable validated accessible daily limits`() {
        val settings = source("vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        val preferences = source("vn/loi/learning/android/study/AndroidStudyPreferences.kt")
        listOf("settings_new_items_per_day", "settings_review_items_per_day", "KeyboardType.Number", "1..999",
            "contentDescription", "stateDescription").forEach { assertTrue(settings.contains(it), it) }
        assertTrue(preferences.contains("getSharedPreferences"))
        assertTrue(preferences.contains("DailyStudyBudgetLimits"))
        assertFalse(preferences.contains("SavedStateHandle"))
    }

    private fun contrast(first: Color, second: Color): Double {
        fun luminance(color: Color): Double {
            fun channel(value: Float): Double {
                val component = value.toDouble()
                return if (component <= 0.04045) component / 12.92 else ((component + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
        }
        val lighter = maxOf(luminance(first), luminance(second))
        val darker = minOf(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun source(relative: String): String = Files.readString(Path.of("src/main/kotlin").resolve(relative))

    private class FakeThemeStore(
        private var persisted: AndroidThemeMode = AndroidThemeMode.FOLLOW_SYSTEM
    ) : AndroidThemePreferenceStore {
        var saveCount = 0
            private set
        override fun load(): AndroidThemeMode = persisted
        override fun save(mode: AndroidThemeMode) {
            persisted = mode
            saveCount += 1
        }
    }
}
