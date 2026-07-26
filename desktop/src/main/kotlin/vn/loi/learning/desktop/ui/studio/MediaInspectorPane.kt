package vn.loi.learning.desktop.ui.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

@Composable
fun MediaInspectorPane(
    uiState: PackageContentBrowserUiState,
    playbackCoordinator: PlaybackCoordinator? = null,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
) {
    val selectedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere
    val scrollState = rememberScrollState()

    var isAiSuggestionsExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = LEColors.surface,
        tonalElevation = LEElevation.flat
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LESpacing.md)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(LESpacing.lg)
        ) {
            // SECTION 1: MEDIA MANAGER
            Text(
                text = "MEDIA MANAGER",
                style = LETypography.paneTitle,
                color = LEColors.textSecondary
            )

            // Image Asset Card
            LEInspectorCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Image", style = LETypography.sectionTitle)
                    LEStatusBadge(
                        variant = if (selectedItem?.hasImage == true) StatusBadgeVariant.Present else StatusBadgeVariant.Missing
                    )
                }

                Spacer(modifier = Modifier.height(LESpacing.sm))

                if (selectedItem?.hasImage == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(LEColors.surfaceElevated, LERadius.sm),
                            contentAlignment = Alignment.Center
                        ) {
                            LessonThumbnail(
                                reference = selectedItem.imageRef ?: "",
                                loader = thumbnailLoader
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedItem.imageRef ?: "image.jpg",
                                style = LETypography.caption,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("Size: 142 KB", style = LETypography.caption, color = LEColors.textMuted)
                            Text("Dimensions: 1024 × 682", style = LETypography.caption, color = LEColors.textMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(LESpacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                    ) {
                        LESecondaryButton(text = "Replace", onClick = {}, icon = LEIcons.Replace, modifier = Modifier.weight(1f))
                        LESecondaryButton(text = "Open", onClick = {}, icon = LEIcons.Open, modifier = Modifier.weight(1f))
                        LESecondaryButton(text = "Remove", onClick = {}, icon = LEIcons.Remove, modifier = Modifier.weight(1f))
                    }
                } else {
                    LEDragDropTarget(
                        label = "or drag & drop image here",
                        hintText = "JPG or PNG"
                    )
                }
            }

            // Audio Asset Cards (Question, Answer, Example, Translation)
            AudioAssetCard(
                label = "Question Audio",
                audioRef = selectedItem?.questionAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio
            )

            AudioAssetCard(
                label = "Answer Audio",
                audioRef = selectedItem?.answerAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio
            )

            AudioAssetCard(
                label = "Example Audio",
                audioRef = selectedItem?.exampleAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio
            )

            AudioAssetCard(
                label = "Translation Audio",
                audioRef = selectedItem?.translationAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio
            )

            HorizontalDivider(color = LEColors.borderSubtle)

            // SECTION 2: QUALITY & AI REVIEW
            Text(
                text = "QUALITY & AI REVIEW",
                style = LETypography.paneTitle,
                color = LEColors.textSecondary
            )

            LEInspectorCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    QualityItemRow(label = "Image", variant = if (selectedItem?.hasImage == true) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Question Audio", variant = if (selectedItem?.questionAudioRef != null) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Answer Audio", variant = if (selectedItem?.answerAudioRef != null) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Example Audio", variant = if (selectedItem?.exampleAudioRef != null) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Translation Audio", variant = if (selectedItem?.translationAudioRef != null) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)

                    QualityItemRow(label = "IPA Format", variant = if (selectedItem?.pronunciation?.isNotBlank() == true) StatusBadgeVariant.Valid else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "POS", variant = StatusBadgeVariant.Valid, customText = "Valid")
                    QualityItemRow(label = "Example Length", variant = StatusBadgeVariant.Valid, customText = "Good")
                    QualityItemRow(label = "Translation Length", variant = StatusBadgeVariant.Valid, customText = "Good")

                    HorizontalDivider(color = LEColors.borderSubtle, modifier = Modifier.padding(vertical = LESpacing.xs))

                    QualityItemRow(label = "Duplicate Check", variant = StatusBadgeVariant.NotEvaluated)
                    QualityItemRow(label = "Orphan Media", variant = StatusBadgeVariant.NotEvaluated)
                    QualityItemRow(label = "Unused Media", variant = StatusBadgeVariant.NotEvaluated)
                }
            }

            // AI SUGGESTIONS ACCORDION
            LECard(
                backgroundColor = LEColors.primarySoft.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAiSuggestionsExpanded = !isAiSuggestionsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "^ AI Suggestions (3)",
                            style = LETypography.sectionTitle,
                            color = LEColors.primaryText
                        )
                    }

                    AnimatedVisibility(visible = isAiSuggestionsExpanded) {
                        Column(
                            modifier = Modifier.padding(top = LESpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                        ) {
                            AiSuggestionCard(text = "Example could be more natural.")
                            AiSuggestionCard(text = "Translation could be improved.")
                            AiSuggestionCard(text = "Missing article \"an\" before \"account\".")

                            Spacer(modifier = Modifier.height(LESpacing.xs))

                            LEPrimaryButton(
                                text = "Apply All Suggestions",
                                onClick = {},
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioAssetCard(
    label: String,
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?
) {
    val isPresent = audioRef != null
    val buttonState = playbackCoordinator?.getButtonState(audioRef)
    val isPlaying = buttonState is AudioButtonState.Playing

    LEInspectorCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = LETypography.sectionTitle)
            LEStatusBadge(variant = if (isPresent) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
        }

        Spacer(modifier = Modifier.height(LESpacing.xs))

        if (audioRef != null) {
            Text(
                text = audioRef,
                style = LETypography.caption,
                color = LEColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(LESpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Waveform Canvas
                LEWaveform(
                    audioRef = audioRef,
                    isPlaying = isPlaying,
                    barCount = 28,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "0:01 / 0:01",
                    style = LETypography.caption,
                    color = LEColors.textMuted,
                    modifier = Modifier.padding(start = LESpacing.xs)
                )
            }

            Spacer(modifier = Modifier.height(LESpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                LESecondaryButton(
                    text = "Replace",
                    onClick = {},
                    icon = LEIcons.Replace,
                    modifier = Modifier.weight(1f)
                )
                LESecondaryButton(
                    text = if (isPlaying) "Stop" else "Preview",
                    onClick = {
                        if (isPlaying) {
                            if (playbackCoordinator != null) playbackCoordinator.stop() else onStopAudio?.invoke()
                        } else {
                            if (playbackCoordinator != null) playbackCoordinator.play(audioRef) else onPlayAudio?.invoke(audioRef)
                        }
                    },
                    icon = if (isPlaying) LEIcons.Stop else LEIcons.Play,
                    modifier = Modifier.weight(1f)
                )
                LESecondaryButton(
                    text = "Remove",
                    onClick = {},
                    icon = LEIcons.Remove,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            LEDragDropTarget(
                label = "or drag & drop audio here",
                hintText = "MP3 or WAV"
            )
        }
    }
}

@Composable
private fun QualityItemRow(
    label: String,
    variant: StatusBadgeVariant,
    customText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = LETypography.secondaryMetadata, color = LEColors.textPrimary)
        LEStatusBadge(variant = variant, customText = customText)
    }
}

@Composable
private fun AiSuggestionCard(text: String) {
    Surface(
        color = LEColors.warningContainer.copy(alpha = 0.5f),
        shape = LERadius.xs,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = LETypography.caption,
            color = LEColors.warningText,
            modifier = Modifier.padding(LESpacing.sm)
        )
    }
}
