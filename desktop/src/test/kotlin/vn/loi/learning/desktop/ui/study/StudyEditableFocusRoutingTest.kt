package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyEditableFocusRoutingTest {
    @Test
    fun `all recall text fields report focus to the shared Study keyboard context`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"))
        val itemCard = source.substringAfter("private fun StudyItemCard(").substringBefore("private fun MultipleChoicePanel(")
        val sharedInput = source.substringAfter("private fun RecallAnswerInputSurface(").substringBefore("private fun ExampleCompletionRecallPanel(")

        assertTrue(itemCard.contains("ListeningRecallPanel("))
        assertTrue(itemCard.contains("ImageRecallInputPanel("))
        assertTrue(itemCard.contains("ExampleCompletionRecallPanel("))
        assertTrue(itemCard.split("onFocusChanged = onTypingFocusChanged").size - 1 >= 3)
        assertTrue(sharedInput.contains(".onFocusChanged { onFocusChanged(it.isFocused) }"))
        assertTrue(source.contains("remember(uiState.currentLearningItemId)"))
    }

    @Test
    fun `typing focus routing has no static shortcut blacklist or focused audio workaround`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"))
        assertFalse(source.contains("blockedWhileTyping"))
        assertFalse(source.contains("if (typingInputFocused) audio"))
    }
}
