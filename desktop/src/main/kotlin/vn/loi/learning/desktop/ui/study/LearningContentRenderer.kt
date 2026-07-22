package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import java.nio.file.Files

@Composable
fun LearningContentRenderer(
    presentation: LearningContentPresentation,
    strings: LearningContentRendererStrings,
    modifier: Modifier = Modifier
) {
    val audioPlayer = remember { JavaSoundLearningContentAudioPlayer() }
    var audioRevision by remember { mutableStateOf(0) }
    val contentIdentity = presentation.sections.hashCode()

    DisposableEffect(contentIdentity) {
        audioPlayer.stop()
        onDispose { audioPlayer.stop() }
    }
    DisposableEffect(Unit) { onDispose(audioPlayer::close) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        presentation.sections.forEach { section ->
            if (section.kind != LearningSectionKind.QUESTION) {
                Text(
                    text = when (section.kind) {
                        LearningSectionKind.ANSWER -> strings.answerLabel
                        LearningSectionKind.EXAMPLE -> strings.exampleLabel
                        LearningSectionKind.QUESTION -> error("Question has no section label")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            section.blocks.forEach { block ->
                when (block) {
                    is PresentedLearningBlock.Text -> MarkdownDocument(block.document)
                    is PresentedLearningBlock.Image -> {
                        val bitmap = remember(block.path) {
                            runCatching {
                                Image.makeFromEncoded(Files.readAllBytes(block.path)).toComposeImageBitmap()
                            }.getOrNull()
                        }
                        if (bitmap == null) {
                            Text(strings.imageUnavailable, color = MaterialTheme.colorScheme.error)
                        } else {
                            Image(
                                bitmap = bitmap,
                                contentDescription = block.description,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    is PresentedLearningBlock.Audio -> {
                        val playing = audioPlayer.playing == block.path
                        @Suppress("UNUSED_EXPRESSION") audioRevision
                        OutlinedButton(onClick = {
                            audioPlayer.toggle(block.path)
                            audioRevision++
                        }) {
                            Text(if (playing) strings.stopAudio else strings.playAudio)
                        }
                    }
                    is PresentedLearningBlock.Unavailable ->
                        Text(block.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun MarkdownDocument(document: SafeMarkdownDocument) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        document.blocks.forEach { block ->
            when (block) {
                is SafeMarkdownBlock.Paragraph -> Text(inlineMarkdown(block.text))
                is SafeMarkdownBlock.Heading -> Text(
                    inlineMarkdown(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold
                )
                is SafeMarkdownBlock.ListItem -> Text(
                    (if (block.ordered) "${block.number ?: 1}. " else "• ") + block.text,
                    modifier = Modifier.padding(start = 12.dp)
                )
                is SafeMarkdownBlock.Code -> Text(
                    block.text,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal fun inlineMarkdown(value: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    val token = Regex("(`[^`\\n]+`|\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__|(?<!\\*)\\*[^*\\n]+\\*(?!\\*)|(?<!_)_[^_\\n]+_(?!_))")
    token.findAll(value).forEach { match ->
        append(value.substring(index, match.range.first))
        val raw = match.value
        val content = if (raw.startsWith("**") || raw.startsWith("__")) raw.drop(2).dropLast(2)
        else raw.drop(1).dropLast(1)
        val start = length
        append(content)
        val style = when {
            raw.startsWith('`') -> SpanStyle(fontFamily = FontFamily.Monospace)
            raw.startsWith("**") || raw.startsWith("__") -> SpanStyle(fontWeight = FontWeight.Bold)
            else -> SpanStyle(fontStyle = FontStyle.Italic)
        }
        addStyle(style, start, length)
        index = match.range.last + 1
    }
    append(value.substring(index))
}
