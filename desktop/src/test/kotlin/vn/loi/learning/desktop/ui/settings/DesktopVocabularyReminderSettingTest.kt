package vn.loi.learning.desktop.ui.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopVocabularyReminderSettingTest {
    @Test
    fun `settings surface exposes compact configuration and production preview controls`() {
        val source = File("src/main/kotlin/vn/loi/learning/desktop/ui/settings/DesktopVocabularyReminderSetting.kt").readText()
        listOf(
            "Desktop Vocabulary Reminder", "Configure ›", "Select package", "Content mode",
            "Show next vocabulary every", "Seconds", "Minutes", "Active from (HH:mm)", "Active until (HH:mm)",
            "Popup duration (1.5–60 seconds)", "Play pronunciation when popup appears",
            "Popup position", "Move monitor", "Reset position",
            "Preview notification", "Pause reminders",
            "Marked difficult", "30 minutes", "1 hour", "Today", "Resume now", "Apply", "Cancel",
            "selectedContainerColor = MaterialTheme.colorScheme.primary",
            "selectedLabelColor = MaterialTheme.colorScheme.onPrimary"
        ).forEach { assertTrue(source.contains(it), it) }
        listOf("speaker", "pronunciation playback", "Good", "Easy", "Submit").forEach {
            assertFalse(source.contains(it), "Out-of-scope control: $it")
        }
    }
}
