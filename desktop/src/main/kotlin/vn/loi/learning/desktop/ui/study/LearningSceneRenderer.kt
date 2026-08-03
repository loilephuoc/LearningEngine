package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.Files
import org.jetbrains.skia.Image
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun LearningSceneRenderer(
    scene: LearningScene,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    presentation: EffectiveStudyPresentation = EffectiveStudyPresentation.UNRESTRICTED,
    layout: StudyVisualLayout,
    inventoryVisible: Boolean = false,
    manualSceneAudioInteraction: ManualSceneAudioInteraction = ManualSceneAudioInteraction.ALLOW,
    modifier: Modifier = Modifier,
    partOfSpeech: String? = null
) {
    var audioState by remember(audioController) { mutableStateOf(audioController.state) }
    DisposableEffect(audioController) {
        val subscription = audioController.listen { audioState = it }
        onDispose(subscription::close)
    }

    Column(
        modifier = modifier.widthIn(max = layout.contentMaxWidthDp.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val discoveryHero = StudyHeroPresentationResolver.resolve(StudySurfaceStage.DISCOVERY)
        if (shouldRenderPrimarySceneInstruction(scene.type)) {
            Text(
                text = scene.instruction(strings),
                style = MaterialTheme.typography.titleMedium,
                color = LETheme.colors.textSecondary
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color =
                        if (discoveryHero.usesAccentTone) LETheme.colors.accentSoft
                        else LETheme.colors.surfacePrimary,
                    shape = LETheme.shapes.radius2XL
                )
                .padding(
                    if (layout.viewportClass == StudyViewportClass.COMPACT) {
                        LETheme.spacing.space5
                    } else {
                        LETheme.spacing.space6
                    }
                )
        ) {
            SceneBlocks(
                blocks = scene.blocks.orderedFor(scene.type),
                sceneType = scene.type,
                audioState = audioState,
                strings = strings,
                audioController = audioController,
                partOfSpeech = partOfSpeech,
                presentation = presentation,
                layout = layout,
                manualSceneAudioInteraction = manualSceneAudioInteraction,
                typingFront = scene is TypingScene,
                inventoryVisible = inventoryVisible,
                primary = true
            )
        }
        scene.supportingScenes.filter { supporting ->
            when (supporting.type) {
                SceneType.MEANING -> presentation.showVietnameseMeaning
                SceneType.EXAMPLE ->
                    presentation.showEnglishExamples || presentation.showVietnameseExamples
                else -> true
            }
        }.forEach { supporting ->
            if (shouldRenderSupportingSceneHeading(scene.type, supporting.type)) {
                Text(
                    text = supporting.instruction(strings),
                    style = MaterialTheme.typography.labelLarge,
                    color = LETheme.colors.accentPrimary
                )
            }
            SceneBlocks(
                blocks = supporting.blocks,
                sceneType = supporting.type,
                audioState = audioState,
                strings = strings,
                audioController = audioController,
                partOfSpeech = partOfSpeech,
                presentation = presentation,
                layout = layout,
                manualSceneAudioInteraction = manualSceneAudioInteraction,
                typingFront = scene is TypingScene,
                inventoryVisible = inventoryVisible,
                primary = false
            )
        }
    }
}

enum class ManualSceneAudioInteraction {
    ALLOW,
    SUPPRESS
}

internal fun shouldRenderPrimarySceneInstruction(sceneType: SceneType): Boolean =
    sceneType != SceneType.TYPING

internal fun shouldRenderSupportingSceneHeading(
    parentType: SceneType,
    supportingType: SceneType
): Boolean = parentType != SceneType.TYPING || supportingType != SceneType.MEANING

private fun LearningScene.instruction(strings: LearningContentRendererStrings): String =
    when (type) {
        SceneType.PROMPT -> strings.promptSceneInstruction
        SceneType.LISTENING -> strings.listeningSceneInstruction
        SceneType.IMAGE -> strings.imageSceneInstruction
        SceneType.MEANING -> strings.meaningSceneLabel
        SceneType.EXAMPLE -> strings.exampleSceneLabel
        SceneType.TYPING -> strings.typingSceneInstruction
        SceneType.MULTIPLE_CHOICE -> strings.promptSceneInstruction
        SceneType.UNSUPPORTED_RECALL -> strings.flowPreparingAnswer
    }

@Composable
private fun SceneBlocks(
    blocks: List<PresentedLearningBlock>,
    sceneType: SceneType,
    audioState: LearningContentAudioState,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    partOfSpeech: String?,
    presentation: EffectiveStudyPresentation,
    layout: StudyVisualLayout,
    manualSceneAudioInteraction: ManualSceneAudioInteraction,
    typingFront: Boolean,
    inventoryVisible: Boolean,
    primary: Boolean
) {
    val visibleBlocks =
        visibleStudySceneBlocks(blocks, presentation)
    val primaryAudio = visibleBlocks
        .filterIsInstance<PresentedLearningBlock.Audio>()
        .firstOrNull { it.role == PresentedAudioRole.PRIMARY_WORD }
    val hasInteractivePrimaryImage =
        manualSceneAudioInteraction == ManualSceneAudioInteraction.ALLOW &&
            primary &&
            visibleBlocks.any { it is PresentedLearningBlock.Image } &&
            primaryAudio != null
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        visibleBlocks.forEach { block ->
            when (block) {
                is PresentedLearningBlock.Text -> {
                    if (
                        sceneType == SceneType.MEANING &&
                        block.role == PresentedTextRole.VIETNAMESE_MEANING
                    ) {
                        val typingMeaning =
                            TypingPresentationResolver.meaning(layout.viewportClass)
                        StudyMeaningPosGroup(
                            partOfSpeech = partOfSpeech,
                            centered = typingFront,
                            posFontSizeSp =
                                typingMeaning.posFontSizeSp.takeIf { typingFront }
                        ) {
                            MarkdownDocument(
                                document = block.document,
                                sceneType = sceneType,
                                paragraphFontSizeSp =
                                    typingMeaning.meaningFontSizeSp.takeIf { typingFront },
                                paragraphLineHeightSp =
                                    typingMeaning.meaningLineHeightSp.takeIf { typingFront },
                                textAlign =
                                    TextAlign.Center.takeIf { typingFront }
                            )
                        }
                    } else {
                        MarkdownDocument(block.document, sceneType)
                    }
                }

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
                            audioPath =
                                primaryAudio?.path?.takeIf {
                                    manualSceneAudioInteraction ==
                                        ManualSceneAudioInteraction.ALLOW
                                },
                            audioController = audioController,
                            loops = false,
                            layout = layout,
                            typingRequired = typingFront,
                            inventoryVisible = inventoryVisible
                        )
                    }
                }

                is PresentedLearningBlock.Audio -> {
                    if (!shouldRenderManualSceneAudio(manualSceneAudioInteraction)) {
                        return@forEach
                    }
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

internal fun shouldRenderManualSceneAudio(
    policy: ManualSceneAudioInteraction
): Boolean = policy == ManualSceneAudioInteraction.ALLOW

internal fun visibleStudySceneBlocks(
    blocks: List<PresentedLearningBlock>,
    presentation: EffectiveStudyPresentation
): List<PresentedLearningBlock> =
    blocks.filter { it.visibleFor(presentation) }

private fun PresentedLearningBlock.visibleFor(
    presentation: EffectiveStudyPresentation
): Boolean =
    when (this) {
        is PresentedLearningBlock.Audio ->
            when (role) {
                PresentedAudioRole.PRIMARY_WORD -> presentation.showPrimaryEnglishAudio
                PresentedAudioRole.MEANING_TRANSLATION -> presentation.showVietnameseMeaning
                PresentedAudioRole.EXAMPLE_PRIMARY -> presentation.showEnglishExamples
                PresentedAudioRole.EXAMPLE_TRANSLATION -> presentation.showVietnameseExamples
                PresentedAudioRole.OTHER -> true
            }
        is PresentedLearningBlock.Text ->
            when (role) {
                PresentedTextRole.PRIMARY_ENGLISH -> presentation.showPrimaryEnglish
                PresentedTextRole.VIETNAMESE_MEANING -> presentation.showVietnameseMeaning
                PresentedTextRole.ENGLISH_EXAMPLE -> presentation.showEnglishExamples
                PresentedTextRole.VIETNAMESE_EXAMPLE -> presentation.showVietnameseExamples
                PresentedTextRole.INSTRUCTION,
                PresentedTextRole.NEUTRAL -> true
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
    sceneType: SceneType,
    paragraphFontSizeSp: Int? = null,
    paragraphLineHeightSp: Int? = null,
    textAlign: TextAlign? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        document.blocks.forEach { block ->
            when (block) {
                is SafeMarkdownBlock.Paragraph -> Text(
                    inlineMarkdown(block.text),
                    style = (when (sceneType) {
                        SceneType.PROMPT,
                        SceneType.LISTENING,
                        SceneType.IMAGE,
                        SceneType.MULTIPLE_CHOICE -> MaterialTheme.typography.headlineLarge

                        SceneType.MEANING -> MaterialTheme.typography.titleLarge
                        SceneType.EXAMPLE,
                        SceneType.TYPING,
                        SceneType.UNSUPPORTED_RECALL -> MaterialTheme.typography.bodyLarge
                    }).let { base ->
                        base.copy(
                            fontSize = paragraphFontSizeSp?.sp ?: base.fontSize,
                            lineHeight = paragraphLineHeightSp?.sp ?: base.lineHeight
                        )
                    },
                    textAlign = textAlign,
                    softWrap = true,
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
