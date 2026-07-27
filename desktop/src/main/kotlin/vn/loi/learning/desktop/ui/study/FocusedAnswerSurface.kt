package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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

@Composable
fun FocusedAnswerSurface(
    model: FocusedVocabularyAnswerModel,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    schedulerFeedback: StudySchedulerFeedback? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.runtime.LaunchedEffect(model.englishWord, model.primaryAudioPath) {
        if (model.primaryAudioPath != null) {
            audioController.playOnce(model.primaryAudioPath)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LESpacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = "Revealed answer: ${model.englishWord}. ${model.vietnameseMeaning}."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        // Approved answer hierarchy: identity, image, meaning, examples.
        VocabularyIdentitySurface(
            word = model.englishWord,
            ipa = model.ipa,
            partOfSpeech = model.partOfSpeech,
            audioPath = model.primaryAudioPath,
            audioController = audioController,
            strings = strings
        )

        // 3. Prompt Image (Centered, adaptive max height)
        if (model.imagePath != null) {
            VocabularyImageBlock(
                imagePath = model.imagePath,
                imageDescription = strings.imageDescription
            )
        }

        // 4. Meaning Card (Clickable when meaning audio exists)
        MeaningCard(
            meaning = model.vietnameseMeaning,
            definition = model.englishDefinition,
            meaningAudioPath = model.meaningAudioPath,
            meaningLabel = strings.meaningSceneLabel,
            audioController = audioController
        )

        // 5. Example Card (Dedicated EN / VI Audio Rows)
        if (model.examples.isNotEmpty()) {
            ExampleCard(
                examples = model.examples,
                exampleLabel = strings.exampleSceneLabel,
                audioController = audioController,
                strings = strings
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
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                audioController.toggleLoop(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.Spacebar) {
                    audioController.toggleLoop(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    BoxWithConstraints(baseModifier) {
        val wordSize = if (maxWidth < 600.dp) 42.sp else 52.sp
        Column(
            modifier = Modifier.fillMaxWidth().padding(LESpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            Text(
                text = word,
                fontSize = wordSize,
                lineHeight = 58.sp,
                fontWeight = FontWeight.Bold,
                color = LEColors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            InlinePronunciationRow(
                ipa = ipa,
                partOfSpeech = partOfSpeech,
                audioPath = audioPath,
                audioController = audioController,
                strings = strings
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
    modifier: Modifier = Modifier
) {
    val hasIpa = !ipa.isNullOrBlank()
    val hasPos = !partOfSpeech.isNullOrBlank()
    val hasAudio = audioPath != null

    if (!hasIpa && !hasPos && !hasAudio) return

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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            tint = if (isLooping) LEColors.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun VocabularyImageBlock(
    imagePath: Path,
    imageDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imagePath) {
        runCatching {
            Image.makeFromEncoded(Files.readAllBytes(imagePath)).toComposeImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = imageDescription,
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .heightIn(max = 340.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Fit
        )
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
            .clickable(interactionSource = interactionSource, indication = null) {
                audioController!!.playOnce(meaningAudioPath!!)
            }
            .onKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.Spacebar) {
                    audioController!!.playOnce(meaningAudioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        modifier = surfaceModifier,
        shape = LERadius.md,
        color = LEColors.studyMeaningSurface,
        border = LEBorder.subtle
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meaningLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (hasAudio) {
                    CompactAudioReplayButton(
                        path = meaningAudioPath!!,
                        audioController = audioController!!,
                        description = "Phát nghĩa tiếng Việt: $meaning",
                        loops = false
                    )
                }
            }
            Text(
                text = meaning,
                fontSize = 25.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!definition.isNullOrBlank()) {
                Text(
                    text = definition,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LERadius.md,
        color = LEColors.surfaceElevated,
        border = LEBorder.subtle
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            Text(
                text = exampleLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            examples.forEachIndexed { index, example ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(LESpacing.xs))
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    // English Example Row
                    EnglishExampleAudioRow(
                        englishText = example.englishText,
                        audioPath = example.englishAudioPath ?: example.audioPath,
                        audioController = audioController
                    )
                    // Vietnamese Translation Row
                    if (!example.vietnameseTranslation.isNullOrBlank()) {
                        VietnameseExampleAudioRow(
                            vietnameseTranslation = example.vietnameseTranslation,
                            audioPath = example.vietnameseAudioPath,
                            audioController = audioController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnglishExampleAudioRow(
    englishText: String,
    audioPath: Path?,
    audioController: LearningContentAudioController,
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
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                audioController.toggleLoop(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.Spacebar) {
                    audioController.toggleLoop(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "VÍ DỤ TIẾNG ANH",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = rowModifier.padding(vertical = LESpacing.xxs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = englishText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (hasAudio) {
                CompactAudioReplayButton(
                    path = audioPath!!,
                    audioController = audioController,
                    description = "Phát ví dụ tiếng Anh: $englishText"
                )
            }
        }
    }
}

@Composable
fun VietnameseExampleAudioRow(
    vietnameseTranslation: String,
    audioPath: Path?,
    audioController: LearningContentAudioController,
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
            .clickable(interactionSource = interactionSource, indication = null) {
                audioController.playOnce(audioPath!!)
            }
            .onKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.Spacebar) {
                    audioController.playOnce(audioPath!!)
                    true
                } else false
            }
            .focusable(interactionSource = interactionSource)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "BẢN DỊCH TIẾNG VIỆT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = rowModifier.padding(vertical = LESpacing.xxs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vietnameseTranslation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (hasAudio) {
                CompactAudioReplayButton(
                    path = audioPath!!,
                    audioController = audioController,
                    description = "Phát bản dịch tiếng Việt: $vietnameseTranslation",
                    loops = false
                )
            }
        }
    }
}
