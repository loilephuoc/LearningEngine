package vn.loi.learning.android.ui

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color

class AndroidMockupFoundationTest {
    private fun source(relative: String) = Files.readString(Path.of("src/main/kotlin").resolve(relative))

    @Test
    fun `Mockup 03 and 09 palettes are intentional green teal layered schemes`() {
        assertEquals(Color(0xFF167A5B), LearningEngineLightColors.primary)
        assertEquals(Color(0xFFF7F7F1), LearningEngineLightColors.background)
        assertEquals(Color(0xFF72DBB4), LearningEngineDarkColors.primary)
        assertEquals(Color(0xFF0C1516), LearningEngineDarkColors.background)
        assertFalse(LearningEngineLightColors.secondary == LearningEngineDarkColors.secondary)
    }

    @Test
    fun `mobile density typography shape and icon roles are centralized`() {
        assertEquals(16, LearningSpacing.screen.value.toInt())
        assertEquals(48, LearningSpacing.touchTarget.value.toInt())
        assertTrue(LearningTextRole.screenTitle.fontSize.value <= 24f)
        assertEquals(22, LearningIconSize.navigation.value.toInt())
        assertTrue(source("vn/loi/learning/android/ui/LearningEngineComponents.kt").contains("LearningEngineScreenShell"))
    }

    @Test
    fun `bottom navigation keeps destinations and replaces generic indicator`() {
        val navigation = source("vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        listOf("HOME", "LIBRARY", "STUDY", "REVIEW", "SETTINGS").forEach { assertTrue(navigation.contains(it)) }
        assertFalse(navigation.contains("NavigationBarItem("))
        assertTrue(navigation.contains("MaterialTheme.colorScheme.primaryContainer"))
        assertTrue(navigation.contains("selected = isSelected"))
    }

    @Test
    fun `top level destinations consume shared shell and compact settings rows`() {
        val navigation = source("vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        assertTrue(navigation.contains("LearningEngineScreenShell(\"Study\""))
        assertTrue(navigation.contains("LearningEngineScreenShell(\"Review\""))
        assertTrue(navigation.contains("LearningEngineScreenShell(\"Settings\""))
        assertTrue(navigation.contains("LearningEngineSettingsRow"))
        assertFalse(navigation.contains("Session Availability"))
        assertFalse(navigation.contains("Data Management"))
    }

    @Test
    fun `Home Library and Study runtime remove dominant legacy patterns`() {
        val study = source("vn/loi/learning/android/study/StudyScreen.kt")
        val library = source("vn/loi/learning/android/library/LibraryScreen.kt")
        assertTrue(study.contains("Keep your learning moving"))
        assertFalse(study.contains("Ready for your next step?"))
        assertTrue(library.contains("LearningTextRole.screenTitle"))
        assertTrue(library.contains("TextField("))
        assertTrue(study.contains("shape = LearningEngineShapes.small"))
        assertFalse(study.contains("targetState = state.revealed"))
    }

    @Test
    fun `screens do not introduce random production hex colors`() {
        listOf(
            "vn/loi/learning/android/study/StudyScreen.kt",
            "vn/loi/learning/android/library/LibraryScreen.kt",
            "vn/loi/learning/android/ui/AndroidRootNavigation.kt"
        ).forEach { assertFalse(Regex("Color\\(0x[0-9A-Fa-f]+\\)").containsMatchIn(source(it))) }
    }
}
