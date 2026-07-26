package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
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
        // 1. Answer term (Vocabulary Identity)
        VocabularyIdentityBlock(word = model.englishWord)

        // 2. Pronunciation Row (Inline Audio, IPA, POS)
        InlinePronunciationRow(
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

        // 4. Meaning Card (Vietnamese meaning & optional English definition)
        MeaningCard(
            meaning = model.vietnameseMeaning,
            definition = model.englishDefinition,
            meaningLabel = strings.meaningSceneLabel
        )

        // 5. Example Card (Dedicated example surface)
        if (model.examples.isNotEmpty()) {
            ExampleCard(
                examples = model.examples,
                exampleLabel = strings.exampleSceneLabel,
                audioController = audioController,
                strings = strings
            )
        }
    }
}

@Composable
fun VocabularyIdentityBlock(
    word: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = word,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = LEColors.primary,
        textAlign = TextAlign.Center,
        modifier = modifier.semantics { heading() }
    )
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
        if (audioPath != null) {
            CompactAudioReplayButton(
                path = audioPath,
                audioController = audioController,
                description = strings.promptAudioLabel,
                isPrimary = true
            )
        }

        if (!ipa.isNullOrBlank()) {
            val formattedIpa = if (ipa.startsWith("/") && ipa.endsWith("/")) ipa else "/$ipa/"
            Text(
                text = formattedIpa,
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!partOfSpeech.isNullOrBlank()) {
            LEStatusBadge(
                variant = StatusBadgeVariant.NotEvaluated,
                customText = partOfSpeech.lowercase()
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
    modifier: Modifier = Modifier
) {
    var audioState by remember(audioController) { mutableStateOf(audioController.state) }
    DisposableEffect(audioController) {
        val subscription = audioController.listen { audioState = it }
        onDispose(subscription::close)
    }

    val isPlaying = (audioState as? LearningContentAudioState.Playing)?.path == path
    val isStarting = (audioState as? LearningContentAudioState.Starting)?.path == path

    IconButton(
        onClick = { audioController.toggle(path) },
        modifier = modifier
            .size(32.dp)
            .semantics {
                contentDescription = when {
                    isStarting -> "Starting audio: $description"
                    isPlaying -> "Stop audio: $description"
                    else -> "Replay audio: $description" + (if (isPrimary) " [R]" else "")
                }
            }
    ) {
        Icon(
            imageVector = if (isPlaying) LEIcons.Stop else LEIcons.Audio,
            contentDescription = null,
            tint = if (isPlaying) LEColors.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
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
                .heightIn(max = 240.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun MeaningCard(
    meaning: String,
    definition: String? = null,
    meaningLabel: String = "Meaning",
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
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            Text(
                text = meaningLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = meaning,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!definition.isNullOrBlank()) {
                Text(
                    text = definition,
                    style = MaterialTheme.typography.bodyMedium,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)
                    ) {
                        Text(
                            text = example.englishText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!example.vietnameseTranslation.isNullOrBlank()) {
                            Text(
                                text = example.vietnameseTranslation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (example.audioPath != null) {
                        CompactAudioReplayButton(
                            path = example.audioPath,
                            audioController = audioController,
                            description = "${strings.exampleAudioLabel} ${index + 1}"
                        )
                    }
                }
            }
        }
    }
}
