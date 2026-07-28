package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.Files
import java.nio.file.Path
import org.jetbrains.skia.Image
import vn.loi.learning.desktop.ui.designsystem.LEBorder
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LEIcons
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.designsystem.components.LEStatusBadge
import vn.loi.learning.desktop.ui.designsystem.components.StatusBadgeVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun FocusedAnswerSurface(
    model: FocusedVocabularyAnswerModel,
    disclosure: FullAnswerDisclosure,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    schedulerFeedback: StudySchedulerFeedback? = null,
    typography: StudyTypographyPresentation =
        StudyTypographyPresentationResolver.resolve(
            vn.loi.learning.desktop.runtime.StudyTypographyPreferences(),
            viewportWidthDp = 0
        ),
    layout: StudyVisualLayout? = null,
    modifier: Modifier = Modifier
) {
    val traits = remember(model, disclosure, schedulerFeedback) {
        StudyVisualContentTraits(
            hasImage = disclosure.imageAvailable && model.imagePath != null,
            hasPronunciation = !disclosure.ipa.isNullOrBlank() || model.primaryAudioPath != null,
            hasPartOfSpeech = !disclosure.partOfSpeech.isNullOrBlank(),
            hasExamples = disclosure.examples.isNotEmpty(),
            hasSchedulerFeedback = schedulerFeedback != null
        )
    }
    val resolvedLayout = layout ?: remember(traits) {
        StudyVisualLayoutResolver.resolve(680, 800, traits)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = resolvedLayout.contentMaxWidthDp.dp)
            .padding(vertical = LESpacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Revealed answer: ${disclosure.englishWord}. ${disclosure.vietnameseMeaning}."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(resolvedLayout.sectionSpacingDp.dp)
    ) {
        // Approved answer hierarchy: identity, image, meaning, examples.
        VocabularyIdentitySurface(
            word = disclosure.englishWord,
            ipa = disclosure.ipa,
            partOfSpeech = disclosure.partOfSpeech,
            audioPath = model.primaryAudioPath,
            audioController = audioController,
            strings = strings,
            layout = resolvedLayout
        )

        // 3. Prompt Image (Centered, adaptive max height)
        if (disclosure.imageAvailable && model.imagePath != null) {
            VocabularyImageBlock(
                imagePath = model.imagePath,
                imageDescription = strings.imageDescription,
                audioPath = model.primaryAudioPath,
                audioController = audioController,
                loops = true,
                layout = resolvedLayout
            )
        }

        // 4. Meaning Card (Clickable when meaning audio exists)
        MeaningCard(
            meaning = disclosure.vietnameseMeaning,
            definition = disclosure.englishDefinition,
            meaningAudioPath = model.meaningAudioPath,
            meaningLabel = strings.meaningSceneLabel,
            audioController = audioController
        )

        // 5. Example Card (Dedicated EN / VI Audio Rows)
        if (disclosure.examples.isNotEmpty()) {
            ExampleCard(
                examples = disclosure.examples,
                exampleLabel = strings.exampleSceneLabel,
                audioController = audioController,
                strings = strings,
                typography = typography,
                englishTarget = disclosure.englishWord,
                vietnameseTarget = disclosure.vietnameseMeaning
            )
        }

        schedulerFeedback?.let {
            CompactSchedulerFeedback(feedback = it)
        }
    }
}

@Composable
fun VocabularyIdentitySurface(
    word: String,
    ipa: String?,
    partOfSpeech: String?,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    layout: StudyVisualLayout? = null,
    modifier: Modifier = Modifier
) {
    val hasAudio = audioPath != null
    val interactionSource = remember { MutableInteractionSource() }
    val isLooping = hasAudio && audioController.activeLoopPath == audioPath

    val baseModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = if (isLooping) "Dừng phát lặp từ tiếng Anh: $word" else "Phát lặp từ tiếng Anh: $word"
                stateDescription = if (isLooping) "Đang phát lặp" else "Chưa phát lặp"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController.toggleLoop(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController.toggleLoop(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    val presentation = rememberAudioInteractionPresentation(
        interactionSource, hasAudio, isLooping, LETheme.colors.surfacePrimary
    )
    Surface(
        modifier = baseModifier,
        shape = LETheme.shapes.radiusL,
        color = presentation.containerColor,
        border = presentation.border
    ) {
    val wordSize = (layout?.identityWordFontSizeSp ?: 52).sp
    val lineHeight = (layout?.identityWordLineHeightSp ?: 58).sp
    Column(
        modifier = Modifier.fillMaxWidth().padding(LESpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
    ) {
            Text(
                text = word,
                style = LETheme.typography.displayWord.copy(
                    fontSize = wordSize,
                    lineHeight = lineHeight,
                    color = if (isLooping) LETheme.colors.accentPrimary else LETheme.colors.textPrimary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            InlinePronunciationRow(
                ipa = ipa,
                partOfSpeech = partOfSpeech,
                audioPath = audioPath,
                audioController = audioController,
                strings = strings,
                layout = layout
            )
        }
    }
}

@Composable
fun InlinePronunciationRow(
    ipa: String?,
    partOfSpeech: String?,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    layout: StudyVisualLayout? = null,
    modifier: Modifier = Modifier
) {
    val hasIpa = !ipa.isNullOrBlank()
    val hasPos = !partOfSpeech.isNullOrBlank()
    val hasAudio = audioPath != null

    if (!hasIpa && !hasPos && !hasAudio) return

    val isStacked = layout?.metadataArrangement == MetadataArrangement.STACKED

    if (isStacked) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            if (hasAudio) {
                CompactAudioReplayButton(
                    path = audioPath!!,
                    audioController = audioController,
                    description = strings.promptAudioLabel,
                    isPrimary = true
                )
            }

            if (!ipa.isNullOrBlank()) {
                val formattedIpa = if (ipa.startsWith("/") && ipa.endsWith("/")) ipa else "/$ipa/"
                Text(
                    text = formattedIpa,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    style = LETheme.typography.metadataIpa
                )
            }

            if (!partOfSpeech.isNullOrBlank()) {
                LEStatusBadge(
                    variant = StatusBadgeVariant.NotEvaluated,
                    customText = partOfSpeech.uppercase()
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAudio) {
                CompactAudioReplayButton(
                    path = audioPath!!,
                    audioController = audioController,
                    description = strings.promptAudioLabel,
                    isPrimary = true
                )
            }

            if (!ipa.isNullOrBlank()) {
                val formattedIpa = if (ipa.startsWith("/") && ipa.endsWith("/")) ipa else "/$ipa/"
                Text(
                    text = formattedIpa,
                    fontSize = 22.sp,
                    fontStyle = FontStyle.Italic,
                    style = LETheme.typography.metadataIpa
                )
            }

            if (!partOfSpeech.isNullOrBlank()) {
                LEStatusBadge(
                    variant = StatusBadgeVariant.NotEvaluated,
                    customText = partOfSpeech.uppercase()
                )
            }
        }
    }
}

@Composable
fun CompactAudioReplayButton(
    path: Path,
    audioController: LearningContentAudioController,
    description: String,
    isPrimary: Boolean = false,
    loops: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isLooping = audioController.activeLoopPath == path

    IconButton(
        onClick = {
            if (loops) audioController.toggleLoop(path) else audioController.playOnce(path)
        },
        modifier = modifier
            .size(40.dp)
            .semantics {
                contentDescription = if (isLooping) "Stop loop: $description" else "Play audio: $description" + (if (isPrimary) " [R]" else "")
                stateDescription = if (isLooping) "Loop active" else "Loop inactive"
            }
    ) {
        Icon(
            imageVector = if (isLooping) LEIcons.Stop else LEIcons.Audio,
            contentDescription = null,
            tint = if (isLooping) LETheme.colors.accentPrimary else LETheme.colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun VocabularyImageBlock(
    imagePath: Path,
    imageDescription: String,
    audioPath: Path? = null,
    audioController: LearningContentAudioController? = null,
    loops: Boolean = false,
    layout: StudyVisualLayout? = null,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imagePath) {
        runCatching {
            Image.makeFromEncoded(Files.readAllBytes(imagePath)).toComposeImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        val maxW = (layout?.imageMaxWidthDp ?: 620).dp
        val maxH = (layout?.imageMaxHeightDp ?: 340).dp
        val enabled = audioPath != null && audioController != null
        val interactionSource = remember { MutableInteractionSource() }
        val isLooping = enabled && loops && audioController?.activeLoopPath == audioPath
        val presentation = rememberAudioInteractionPresentation(
            interactionSource = interactionSource,
            enabled = enabled,
            activeLoop = isLooping
        )
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = maxW)
                .heightIn(max = maxH)
                .audioPressable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    description = when {
                        isLooping -> "D\u1eebng ph\u00e1t l\u1eb7p t\u1eeb ti\u1ebfng Anh"
                        loops -> "Ph\u00e1t l\u1eb7p t\u1eeb ti\u1ebfng Anh"
                        else -> "Nghe t\u1eeb ti\u1ebfng Anh"
                    },
                    state = if (loops) {
                        if (isLooping) "\u0110ang ph\u00e1t l\u1eb7p" else "Ch\u01b0a ph\u00e1t l\u1eb7p"
                    } else null
                ) {
                    if (loops) {
                        audioController!!.toggleLoop(audioPath!!)
                    } else {
                        audioController!!.playOnce(audioPath!!)
                    }
                },
            shape = LETheme.shapes.radiusL,
            color = presentation.containerColor,
            border = presentation.border
        ) {
            Box {
                Image(
                    bitmap = bitmap,
                    contentDescription = imageDescription,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    contentScale = ContentScale.Fit
                )
                if (enabled) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(42.dp),
                        shape = LETheme.shapes.radiusPill,
                        color = LETheme.colors.accentSoft
                    ) {
                        Icon(
                            imageVector = if (isLooping) LEIcons.Stop else LEIcons.Audio,
                            contentDescription = null,
                            tint = presentation.iconColor,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeaningCard(
    meaning: String,
    definition: String? = null,
    meaningAudioPath: Path? = null,
    meaningLabel: String = "Meaning",
    audioController: LearningContentAudioController? = null,
    modifier: Modifier = Modifier
) {
    val hasAudio = meaningAudioPath != null && audioController != null
    val interactionSource = remember { MutableInteractionSource() }

    val surfaceModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Phát nghĩa tiếng Việt: $meaning"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController!!.playOnce(meaningAudioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController!!.playOnce(meaningAudioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    LESurface(
        variant = StudySurfaceRoles.meaning,
        modifier = surfaceModifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = LETheme.shapes.radiusM,
                color = if (hasAudio) LETheme.colors.accentSoft else LETheme.colors.surfaceSecondary,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (hasAudio) LEIcons.Audio else LEIcons.Help,
                    contentDescription = null,
                    tint = if (hasAudio) LETheme.colors.accentPrimary else LETheme.colors.textMuted,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = meaning,
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = LETheme.typography.meaningPrimary
                )
                if (!definition.isNullOrBlank()) {
                    Text(
                        text = definition,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        style = LETheme.typography.bodyDefinition
                    )
                }
            }
        }
    }
}

@Composable
fun ExampleCard(
    examples: List<FocusedExampleItem>,
    exampleLabel: String = "Examples",
    audioController: LearningContentAudioController,
    strings: LearningContentRendererStrings,
    typography: StudyTypographyPresentation,
    englishTarget: String,
    vietnameseTarget: String,
    modifier: Modifier = Modifier
) {
    LESurface(
        variant = StudySurfaceRoles.example,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            Text(
                text = exampleLabel.uppercase(),
                style = LETheme.typography.sectionTitle,
                color = LETheme.colors.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            examples.forEach { example ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LETheme.shapes.radiusM,
                    color = LETheme.colors.surfacePrimary,
                    border = LETheme.borders.subtle
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    // English Example Row
                    EnglishExampleAudioRow(
                        englishText = example.englishText,
                        target = englishTarget,
                        audioPath = example.englishAudioPath ?: example.audioPath,
                        audioController = audioController,
                        typography = typography
                    )
                    // Vietnamese Translation Row
                    if (
                        !example.vietnameseTranslation.isNullOrBlank()
                    ) {
                        VietnameseExampleAudioRow(
                            vietnameseTranslation = example.vietnameseTranslation,
                            target = vietnameseTarget,
                            audioPath = example.vietnameseAudioPath,
                            audioController = audioController,
                            typography = typography
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun EnglishExampleAudioRow(
    englishText: String,
    target: String,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    typography: StudyTypographyPresentation,
    modifier: Modifier = Modifier
) {
    val hasAudio = audioPath != null
    val interactionSource = remember { MutableInteractionSource() }
    val isLooping = hasAudio && audioController.activeLoopPath == audioPath

    val rowModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = if (isLooping) "Dừng phát lặp ví dụ tiếng Anh: $englishText" else "Phát lặp ví dụ tiếng Anh: $englishText"
                stateDescription = if (isLooping) "Loop active" else "Loop inactive"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController.toggleLoop(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController.toggleLoop(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    val presentation = rememberAudioInteractionPresentation(
        interactionSource, hasAudio, isLooping, LETheme.colors.surfacePrimary
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LETheme.shapes.radiusM,
        color = presentation.containerColor,
        border = presentation.border
    ) {
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAudio) {
                Icon(
                    if (isLooping) LEIcons.Stop else LEIcons.Audio,
                    contentDescription = null,
                    tint = presentation.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = highlightedExampleText(
                    text = englishText,
                    target = target,
                    language = ExampleTargetLanguage.ENGLISH,
                    highlightStyle = SpanStyle(
                        color = LEColors.danger,
                        fontWeight = FontWeight.Bold
                    )
                ),
                fontSize = typography.exampleEnglishFontSize.sp,
                lineHeight = typography.exampleEnglishLineHeight.sp,
                fontWeight = FontWeight.SemiBold,
                color = LETheme.colors.textPrimary,
                softWrap = typography.softWrap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VietnameseExampleAudioRow(
    vietnameseTranslation: String,
    target: String,
    audioPath: Path?,
    audioController: LearningContentAudioController,
    typography: StudyTypographyPresentation,
    modifier: Modifier = Modifier
) {
    val hasAudio = audioPath != null
    val interactionSource = remember { MutableInteractionSource() }

    val rowModifier = if (hasAudio) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Phát bản dịch tiếng Việt: $vietnameseTranslation"
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {
                audioController.playOnce(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                    audioController.playOnce(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    val presentation = rememberAudioInteractionPresentation(
        interactionSource, hasAudio, baseColor = LETheme.colors.surfaceSecondary
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LETheme.shapes.radiusM,
        color = presentation.containerColor,
        border = presentation.border
    ) {
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAudio) {
                Icon(
                    LEIcons.Audio,
                    contentDescription = null,
                    tint = LEColors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Spacer(Modifier.size(22.dp))
            }
            Text(
                text = highlightedExampleText(
                    text = vietnameseTranslation,
                    target = target,
                    language = ExampleTargetLanguage.VIETNAMESE,
                    highlightStyle = SpanStyle(
                        color = LEColors.danger,
                        fontWeight = FontWeight.Bold
                    )
                ),
                fontSize = typography.exampleVietnameseFontSize.sp,
                lineHeight = typography.exampleVietnameseLineHeight.sp,
                fontWeight = FontWeight.Normal,
                color = LETheme.colors.textSecondary,
                softWrap = typography.softWrap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
