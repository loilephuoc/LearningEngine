package vn.loi.learning.android.packageexperience

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidPackageConsolidatedUxTest {
    private val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/packageexperience/PackageScreen.kt"))
    private val badge = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/components/PartOfSpeechBadge.kt"))
    private val presentationPolicy = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyPresentationPolicy.kt"))
    private val audio = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/media/AndroidAudioController.kt"))

    @Test fun `row gives English priority compact POS larger image and plain meaning`() {
        assertTrue(screen.contains("MaterialTheme.typography.titleLarge"))
        assertTrue(screen.contains("FontWeight.SemiBold"))
        assertTrue(screen.contains("PartOfSpeechBadge(presentation, compact = true)"))
        assertTrue(badge.contains("compact: Boolean = false"))
        assertTrue(screen.contains("Modifier.size(96.dp)"))
        assertTrue(screen.contains("ContentScale.Crop"))
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        assertTrue(row.substringAfter("row.answer,").contains("style = MaterialTheme.typography.bodyMedium"))
    }

    @Test fun `compact POS badge auto scales known labels without wrapping`() {
        assertTrue(badge.contains("maxLines = 1"))
        assertTrue(badge.contains("softWrap = false"))
        assertTrue(badge.contains("TextAutoSize.StepBased("))
        assertTrue(badge.contains("minFontSize = 9.sp"))
        assertTrue(badge.contains("maxFontSize = textStyle.fontSize"))
        listOf("NOUN", "VERB", "ADVERB", "ADJECTIVE", "PREPOSITION").forEach { label ->
            assertTrue(presentationPolicy.contains("\"$label\""))
        }
        assertTrue(screen.contains("PartOfSpeechBadge(presentation, compact = true)"))
        assertTrue(badge.indexOf("PartOfSpeechBadge") < badge.indexOf("TextAutoSize.StepBased"))
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

    @Test fun `vocabulary metadata keeps full width IPA and compact center POS`() {
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        assertFalse(row.contains("ListItem("))
        assertTrue(row.contains("PartOfSpeechBadge(presentation, compact = true)"))
        val ipa = row.substringAfter("normalizedIntroductionPronunciation").substringBefore("if (row.answer.isNotBlank())")
        assertTrue(ipa.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(ipa.contains("maxLines = 2"))
        assertTrue(ipa.contains("softWrap = true"))
        assertTrue(row.contains("row.audioRef?.let(onPlayAudio) ?: onClick()"))
        val meaning = row.substringAfter("if (row.answer.isNotBlank())").substringBefore("modifier = Modifier.width(68.dp)")
        assertFalse(meaning.contains("clickable"))
        assertFalse(meaning.contains("combinedClickable"))
        assertFalse(row.contains("IconButton(onClick = { onPlayAudio(reference) })"))
    }

    @Test fun `flat compact row uses text center controls and far right image zones`() {
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        val textStack = row.substringAfter("modifier = Modifier.weight(1f)")
            .substringBefore("modifier = Modifier.width(68.dp)")
        assertTrue(textStack.indexOf("row.question") < textStack.indexOf("normalizedIntroductionPronunciation"))
        assertTrue(textStack.indexOf("normalizedIntroductionPronunciation") < textStack.indexOf("row.answer.isNotBlank()"))
        val center = row.substringAfter("modifier = Modifier.width(68.dp)").substringBefore("modifier = Modifier.width(96.dp)")
        assertTrue(center.indexOf("PartOfSpeechBadge") < center.indexOf("IconButton"))
        assertTrue(center.contains("Icons.Filled.StarBorder"))
        val media = row.substringAfter("modifier = Modifier.width(96.dp)")
        assertTrue(media.contains("PackageThumbnail"))
        assertFalse(media.contains("IconButton"))
        assertFalse(textStack.contains("PackageThumbnail"))
        assertFalse(row.contains("Surface("))
    }

    @Test fun `English IPA and one line meaning remain in flexible left zone`() {
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        val left = row.substringAfter("modifier = Modifier.weight(1f)").substringBefore("modifier = Modifier.width(68.dp)")
        assertTrue(left.contains("row.question"))
        assertTrue(left.contains("normalizedIntroductionPronunciation"))
        val meaning = left.substringAfter("if (row.answer.isNotBlank())")
        assertTrue(meaning.contains("maxLines = 1"))
        assertTrue(meaning.contains("modifier = Modifier.fillMaxWidth()"))
        assertFalse(left.contains("PartOfSpeechBadge"))
    }

    @Test fun `package detail surfaces use canonical theme aware neutral and mint roles`() {
        val header = screen.substringAfter("private fun PackageHeader").substringBefore("private fun LearningPackageSelection")
        assertTrue(header.contains("containerColor = MaterialTheme.colorScheme.surface"))
        val search = screen.substringAfter("private fun PackageSearchField").substringBefore("private fun PackageContentRow")
        assertTrue(search.contains("focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant"))
        assertTrue(search.contains("focusedBorderColor = MaterialTheme.colorScheme.primary"))
        val row = screen.substringAfter("private fun PackageContentRow(").substringBefore("private fun PackageContentBody(")
        assertFalse(row.contains("Card("))
        assertFalse(row.contains("Surface("))
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
