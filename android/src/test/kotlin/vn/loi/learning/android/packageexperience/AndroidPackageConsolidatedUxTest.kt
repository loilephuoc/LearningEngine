package vn.loi.learning.android.packageexperience

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidPackageConsolidatedUxTest {
    private val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/packageexperience/PackageScreen.kt"))
    private val badge = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/components/PartOfSpeechBadge.kt"))
    private val audio = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/media/AndroidAudioController.kt"))

    @Test fun `row gives English priority compact POS uniform image and plain meaning`() {
        assertTrue(screen.contains("MaterialTheme.typography.titleLarge"))
        assertTrue(screen.contains("FontWeight.SemiBold"))
        assertTrue(screen.contains("PartOfSpeechBadge(presentation, compact = true)"))
        assertTrue(badge.contains("compact: Boolean = false"))
        assertTrue(screen.contains("Modifier.size(76.dp)"))
        assertTrue(screen.contains("ContentScale.Crop"))
        assertTrue(screen.contains("row.answer,\n                        style = MaterialTheme.typography.bodyMedium"))
    }

    @Test fun `audio distinguishes preparing and playing and clears visual before cleanup`() {
        assertTrue(screen.contains("audioController.replay(path, isLooping = false)"))
        val completion = audio.substringAfter("setOnCompletionListener").substringBefore("setOnErrorListener")
        assertTrue(completion.indexOf("onState(AndroidAudioState.Idle)") < completion.indexOf("close()"))
    }

    @Test fun `quick edit uses row long press while canonical failure remains open`() {
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        assertTrue(row.contains(".combinedClickable("))
        assertTrue(row.contains("onLongClick = onQuickEdit"))
        assertTrue(row.contains("CustomAccessibilityAction(\"Sửa từ\")"))
        assertFalse(row.contains("Icons.Default.MoreVert"))
        assertTrue(screen.contains("Icon(Icons.Default.MoreVert, contentDescription = \"Package operations\")"))
        assertTrue(screen.contains("PackageQuickEditDialog"))
        assertTrue(screen.contains("if (result.isSuccess) editing = null"))
        assertFalse(screen.contains("if (result.isFailure) editing = null"))
    }

    @Test fun `vocabulary metadata uses independent full width POS and IPA blocks`() {
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        assertFalse(row.contains("ListItem("))
        assertTrue(row.contains("PartOfSpeechBadge(presentation, compact = true)"))
        val ipa = row.substringAfter("normalizedIntroductionPronunciation").substringBefore("if (row.answer.isNotBlank())")
        assertTrue(ipa.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(ipa.contains("maxLines = 2"))
        assertTrue(ipa.contains("softWrap = true"))
        assertTrue(row.contains("row.audioRef?.let(onPlayAudio) ?: onClick()"))
        val meaning = row.substringAfter("if (row.answer.isNotBlank())").substringBefore("modifier = Modifier.width(76.dp)")
        assertFalse(meaning.contains("clickable"))
        assertFalse(meaning.contains("combinedClickable"))
        assertFalse(row.contains("IconButton(onClick = { onPlayAudio(reference) })"))
    }

    @Test fun `compact row keeps all text in one weighted stack beside fixed media`() {
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        val textStack = row.substringAfter("modifier = Modifier.weight(1f)")
            .substringBefore("modifier = Modifier.width(76.dp)")
        assertTrue(textStack.indexOf("row.question") < textStack.indexOf("partOfSpeechPresentation"))
        assertTrue(textStack.indexOf("partOfSpeechPresentation") < textStack.indexOf("normalizedIntroductionPronunciation"))
        assertTrue(textStack.indexOf("normalizedIntroductionPronunciation") < textStack.indexOf("row.answer.isNotBlank()"))
        val media = row.substringAfter("modifier = Modifier.width(76.dp)")
        assertTrue(media.contains("PackageThumbnail"))
        assertFalse(media.contains("IconButton"))
        assertFalse(textStack.contains("PackageThumbnail"))
    }

    @Test fun `English and compact POS share the top row while IPA and meaning remain plain blocks`() {
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        val top = row.substringAfter("verticalAlignment = Alignment.CenterVertically")
            .substringBefore("normalizedIntroductionPronunciation")
        assertTrue(top.contains("row.question"))
        assertTrue(top.contains("modifier = Modifier.weight(1f)"))
        assertTrue(top.contains("PartOfSpeechBadge(presentation, compact = true)"))
        val meaning = row.substringAfter("if (row.answer.isNotBlank())").substringBefore("modifier = Modifier.width(76.dp)")
        assertTrue(meaning.contains("maxLines = 2"))
        assertTrue(meaning.contains("modifier = Modifier.fillMaxWidth()"))
        assertFalse(meaning.contains("onToggleMeaning"))
    }

    @Test fun `package detail surfaces use canonical theme aware neutral and mint roles`() {
        val header = screen.substringAfter("private fun PackageHeader").substringBefore("private fun LearningPackageSelection")
        assertTrue(header.contains("containerColor = MaterialTheme.colorScheme.surface"))
        val search = screen.substringAfter("private fun PackageSearchField").substringBefore("private fun PackageContentRow")
        assertTrue(search.contains("focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant"))
        assertTrue(search.contains("focusedBorderColor = MaterialTheme.colorScheme.primary"))
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        assertTrue(row.contains("color = MaterialTheme.colorScheme.surface"))
        assertFalse(screen.contains("Purple"))
        assertFalse(screen.contains("Lavender"))
    }

    @Test fun `search owns IME selection immediately and remains one sticky authoritative field`() {
        val search = screen.substringAfter("private fun PackageSearchField").substringBefore("private fun PackageContentRow")
        assertTrue(search.contains("TextFieldValue.Saver"))
        assertTrue(search.contains("input = updated"))
        assertTrue(search.contains("onSearch(updated.text)"))
        assertTrue(search.contains("TextRange.Zero"))
        val body = screen.substringAfter("private fun PackageContentBody").substringBefore("private fun PackageThumbnail")
        assertTrue(body.contains("stickyHeader(\"search\")"))
        assertTrue(body.contains("Surface(color = MaterialTheme.colorScheme.background)"))
        assertTrue(body.indexOf("PackageSearchField(") == body.lastIndexOf("PackageSearchField("))
    }

    @Test fun `quick edit uses canonical theme surfaces without changing field authority`() {
        val dialog = screen.substringAfter("private fun PackageQuickEditDialog").substringBefore("operationFeedback")
        assertTrue(dialog.contains("containerColor = MaterialTheme.colorScheme.surface"))
        assertTrue(dialog.contains("focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant"))
        assertTrue(dialog.contains("focusedBorderColor = MaterialTheme.colorScheme.primary"))
        assertTrue(dialog.contains("cursorColor = MaterialTheme.colorScheme.primary"))
        listOf("Question", "Answer", "IPA / Pronunciation", "POS", "Example", "Translation").forEach {
            assertTrue(dialog.contains("\"$it\""), it)
        }
    }
}
