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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.nio.file.Files
import org.jetbrains.skia.Image

@Composable
fun LearningSceneRenderer(
    scene: LearningScene,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    presentation: EffectiveStudyPresentation = EffectiveStudyPresentation.UNRESTRICTED,
    modifier: Modifier = Modifier
) {
    var audioState by remember(audioController) { mutableStateOf(audioController.state) }
    DisposableEffect(audioController) {
        val subscription = audioController.listen { audioState = it }
        onDispose(subscription::close)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = scene.instruction(strings),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SceneBlocks(
            blocks = scene.blocks.orderedFor(scene.type),
            sceneType = scene.type,
            audioState = audioState,
            strings = strings,
            audioController = audioController,
            presentation = presentation,
            answerRevealed = scene.context.answerRevealed,
            primary = true
        )
        scene.supportingScenes.filter { supporting ->
            when (supporting.type) {
                SceneType.MEANING -> presentation.showVietnameseMeaning
                SceneType.EXAMPLE ->
                    presentation.showEnglishExamples || presentation.showVietnameseExamples
                else -> true
            }
        }.forEach { supporting ->
            Text(
                text = supporting.instruction(strings),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            SceneBlocks(
                blocks = supporting.blocks,
                sceneType = supporting.type,
                audioState = audioState,
                strings = strings,
                audioController = audioController,
                presentation = presentation,
                answerRevealed = scene.context.answerRevealed,
                primary = false
            )
        }
    }
}

private fun LearningScene.instruction(strings: LearningContentRendererStrings): String =
    when (type) {
        SceneType.PROMPT -> strings.promptSceneInstruction
        SceneType.LISTENING -> strings.listeningSceneInstruction
        SceneType.IMAGE -> strings.imageSceneInstruction
        SceneType.MEANING -> strings.meaningSceneLabel
        SceneType.EXAMPLE -> strings.exampleSceneLabel
        SceneType.TYPING -> strings.typingSceneInstruction
    }

@Composable
private fun SceneBlocks(
    blocks: List<PresentedLearningBlock>,
    sceneType: SceneType,
    audioState: LearningContentAudioState,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    presentation: EffectiveStudyPresentation,
    answerRevealed: Boolean,
    primary: Boolean
) {
    val visibleBlocks =
        visibleStudySceneBlocks(blocks, sceneType, presentation, answerRevealed)
    val primaryAudio = visibleBlocks
        .filterIsInstance<PresentedLearningBlock.Audio>()
        .firstOrNull { it.role == PresentedAudioRole.PRIMARY_WORD }
    val hasInteractivePrimaryImage =
        primary && visibleBlocks.any { it is PresentedLearningBlock.Image } && primaryAudio != null
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        visibleBlocks.forEach { block ->
            when (block) {
                is PresentedLearningBlock.Text ->
                    MarkdownDocument(block.document, sceneType)

                is PresentedLearningBlock.Image -> {
                    val bitmap = remember(block.path) {
                        runCatching {
                            Image.makeFromEncoded(Files.readAllBytes(block.path)).toComposeImageBitmap()
                        }.getOrNull()
                    }
                    if (bitmap == null) {
                        Text(
                            strings.imageUnavailable,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics {
                                contentDescription = strings.imageUnavailable
                            }
                        )
                    } else {
                        VocabularyImageBlock(
                            imagePath = block.path,
                            imageDescription = block.description,
                            audioPath = primaryAudio?.path,
                            audioController = audioController,
                            loops = false,
                            modifier = Modifier.heightIn(max = 480.dp)
                        )
                    }
                }

                is PresentedLearningBlock.Audio -> {
                    if (hasInteractivePrimaryImage && block.role == PresentedAudioRole.PRIMARY_WORD) {
                        return@forEach
                    }
                    val starting =
                        (audioState as? LearningContentAudioState.Starting)?.path == block.path
                    val playing =
                        (audioState as? LearningContentAudioState.Playing)?.path == block.path
                    OutlinedButton(
                        modifier = Modifier.semantics {
                            contentDescription = when {
                                starting -> "${strings.startingAudio}: ${block.roleLabel}"
                                playing -> "${strings.stopAudio}: ${block.roleLabel}"
                                else -> "${strings.playAudio}: ${block.roleLabel}" +
                                    if (primary) " [R]" else ""
                            }
                        },
                        onClick = {
                            if (primary) audioController.playOnce(block.path) else audioController.toggle(block.path)
                        }
                    ) {
                        Text(
                            when {
                                starting -> "${strings.startingAudio}…"
                                playing -> "${strings.playingAudio}: ${block.roleLabel}"
                                else -> "${strings.playAudio}: ${block.roleLabel}"
                            }
                        )
                    }
                    val failure = audioState as? LearningContentAudioState.Failed
                    if (failure?.path == block.path) {
                        Text(
                            text = "${strings.audioPlaybackFailed}: ${block.roleLabel}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics {
                                contentDescription =
                                    "${strings.audioPlaybackFailed}: ${block.roleLabel}"
                            }
                        )
                    }
                }

                is PresentedLearningBlock.Unavailable ->
                    Text(
                        block.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { contentDescription = block.message }
                    )
            }
        }
    }
}

internal fun visibleStudySceneBlocks(
    blocks: List<PresentedLearningBlock>,
    sceneType: SceneType,
    presentation: EffectiveStudyPresentation,
    answerRevealed: Boolean
): List<PresentedLearningBlock> =
    blocks.filter { it.visibleFor(sceneType, presentation, answerRevealed) }

private fun PresentedLearningBlock.visibleFor(
    sceneType: SceneType,
    presentation: EffectiveStudyPresentation,
    answerRevealed: Boolean
): Boolean =
    when (this) {
        is PresentedLearningBlock.Audio ->
            when (role) {
                PresentedAudioRole.PRIMARY_WORD -> presentation.showPrimaryEnglish
                PresentedAudioRole.MEANING_TRANSLATION -> presentation.showVietnameseMeaning
                PresentedAudioRole.EXAMPLE_PRIMARY -> presentation.showEnglishExamples
                PresentedAudioRole.EXAMPLE_TRANSLATION -> presentation.showVietnameseExamples
                PresentedAudioRole.OTHER -> true
            }
        is PresentedLearningBlock.Text ->
            when (sceneType) {
                SceneType.MEANING -> presentation.showVietnameseMeaning
                SceneType.EXAMPLE ->
                    presentation.showEnglishExamples || presentation.showVietnameseExamples
                else ->
                    if (answerRevealed) presentation.showPrimaryEnglish
                    else presentation.showVietnameseMeaning
            }
        is PresentedLearningBlock.Image,
        is PresentedLearningBlock.Unavailable -> true
    }

private fun List<PresentedLearningBlock>.orderedFor(
    sceneType: SceneType
): List<PresentedLearningBlock> =
    when (sceneType) {
        SceneType.IMAGE ->
            filterIsInstance<PresentedLearningBlock.Image>() +
                filterNot { it is PresentedLearningBlock.Image }

        SceneType.LISTENING ->
            filterIsInstance<PresentedLearningBlock.Audio>() +
                filterNot { it is PresentedLearningBlock.Audio }

        else -> this
    }

@Composable
private fun MarkdownDocument(
    document: SafeMarkdownDocument,
    sceneType: SceneType
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        document.blocks.forEach { block ->
            when (block) {
                is SafeMarkdownBlock.Paragraph -> Text(
                    inlineMarkdown(block.text),
                    style = when (sceneType) {
                        SceneType.PROMPT,
                        SceneType.LISTENING,
                        SceneType.IMAGE -> MaterialTheme.typography.headlineLarge

                        SceneType.MEANING -> MaterialTheme.typography.titleLarge
                        SceneType.EXAMPLE,
                        SceneType.TYPING -> MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = if (
                        sceneType == SceneType.PROMPT ||
                        sceneType == SceneType.LISTENING ||
                        sceneType == SceneType.IMAGE
                    ) {
                        FontWeight.Bold
                    } else {
                        null
                    }
                )

                is SafeMarkdownBlock.Heading -> Text(
                    inlineMarkdown(block.text),
                    modifier = Modifier.semantics { heading() },
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
    val token = Regex(
        "(`[^`\\n]+`|\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__|" +
            "(?<!\\*)\\*[^*\\n]+\\*(?!\\*)|(?<!_)_[^_\\n]+_(?!_))"
    )
    token.findAll(value).forEach { match ->
        append(value.substring(index, match.range.first))
        val raw = match.value
        val content = if (raw.startsWith("**") || raw.startsWith("__")) {
            raw.drop(2).dropLast(2)
        } else {
            raw.drop(1).dropLast(1)
        }
        val start = length
        append(content)
        val style = when {
            raw.startsWith('`') -> SpanStyle(fontFamily = FontFamily.Monospace)
            raw.startsWith("**") || raw.startsWith("__") ->
                SpanStyle(fontWeight = FontWeight.Bold)
            else -> SpanStyle(fontStyle = FontStyle.Italic)
        }
        addStyle(style, start, length)
        index = match.range.last + 1
    }
    append(value.substring(index))
}
