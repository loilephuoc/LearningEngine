package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoldenAnswerLayoutTest {
    @Test
    fun `translation is exactly one centered audio and text row without heading`() {
        val meaning = source().substringAfter("fun MeaningCard(").substringBefore("fun ExampleCard(")

        assertTrue(meaning.contains("Row("))
        assertTrue(meaning.contains("Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)"))
        assertTrue(meaning.contains("verticalAlignment = Alignment.CenterVertically"))
        assertTrue(meaning.contains("imageVector = LEIcons.Audio"))
        assertTrue(meaning.contains("text = meaning"))
        assertTrue(meaning.contains("maxLines = 1"))
        assertTrue(meaning.contains("softWrap = false"))
        assertFalse(meaning.contains("meaningLabel"))
        assertFalse(meaning.contains("strings.meaningSceneLabel"))
    }

    @Test
    fun `answer image nearly fills stage and responds to accordion state`() {
        val answer = source()

        assertTrue(answer.contains("GoldenAnswerImageHeightResolver.heightDp"))
        assertTrue(answer.contains("examplesExpanded = examplesExpanded"))
        assertTrue(answer.contains("onExamplesExpandedChange = { examplesExpanded = it }"))
        assertTrue(answer.contains("resolvedLayout.contentMaxWidthDp * 0.98f"))
        assertFalse(answer.contains("minOf(signatureImageHeightDp, resolvedLayout.imageMaxHeightDp)"))
        assertTrue(answer.contains("contentScale = ContentScale.Fit"))
    }

    private fun source(): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/FocusedAnswerSurface.kt")
    )
}
