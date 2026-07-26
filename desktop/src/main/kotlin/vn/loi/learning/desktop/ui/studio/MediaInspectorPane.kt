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
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
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
    onUpdateDraftImageRef: ((String?) -> Unit)? = null,
    onUpdateDraftQuestionAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftAnswerAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftExampleAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftTranslationAudioRef: ((String?) -> Unit)? = null,
    onImportMediaFile: ((File, String) -> Unit)? = null,
    onOpenFullscreenImage: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCreating = uiState.isCreatingNewItem
    val activeDraft = if (isCreating || uiState.editingContentId != null) uiState.draftEdits else null
    val persistedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere

    val currentImageRef = if (activeDraft != null) activeDraft.imageRef else persistedItem?.imageRef
    val currentQuestionAudioRef = if (activeDraft != null) activeDraft.questionAudioRef else persistedItem?.questionAudioRef
    val currentAnswerAudioRef = if (activeDraft != null) activeDraft.answerAudioRef else persistedItem?.answerAudioRef
    val currentExampleAudioRef = if (activeDraft != null) activeDraft.exampleAudioRef else persistedItem?.exampleAudioRef
    val currentTranslationAudioRef = if (activeDraft != null) activeDraft.translationAudioRef else persistedItem?.translationAudioRef

    val hasImage = !currentImageRef.isNullOrBlank()

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
                        variant = if (hasImage) StatusBadgeVariant.Present else StatusBadgeVariant.Missing
                    )
                }

                Spacer(modifier = Modifier.height(LESpacing.sm))

                if (hasImage) {
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
                                reference = currentImageRef ?: "",
                                loader = thumbnailLoader
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentImageRef ?: "image.jpg",
                                style = LETypography.caption,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("Status: Attached", style = LETypography.caption, color = LEColors.textMuted)
                            Text("Asset: Resolved", style = LETypography.caption, color = LEColors.textMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(LESpacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                    ) {
                        LESecondaryButton(
                            text = "Replace",
                            onClick = {
                                pickFile("Select Image", listOf("png", "jpg", "jpeg", "webp")) { file ->
                                    if (onImportMediaFile != null) {
                                        onImportMediaFile(file, "image")
                                    } else {
                                        onUpdateDraftImageRef?.invoke(file.name)
                                    }
                                }
                            },
                            icon = LEIcons.Replace,
                            modifier = Modifier.weight(1f)
                        )
                        LESecondaryButton(
                            text = "Open",
                            onClick = { onOpenFullscreenImage?.invoke() },
                            icon = LEIcons.Open,
                            modifier = Modifier.weight(1f)
                        )
                        LESecondaryButton(
                            text = "Remove",
                            onClick = { onUpdateDraftImageRef?.invoke(null) },
                            icon = LEIcons.Remove,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                        LESecondaryButton(
                            text = "Browse Image File",
                            onClick = {
                                pickFile("Select Image", listOf("png", "jpg", "jpeg", "webp")) { file ->
                                    if (onImportMediaFile != null) {
                                        onImportMediaFile(file, "image")
                                    } else {
                                        onUpdateDraftImageRef?.invoke(file.name)
                                    }
                                }
                            },
                            icon = LEIcons.New,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LEDragDropTarget(
                            label = "or drag & drop image here",
                            hintText = "JPG, PNG, WEBP"
                        )
                    }
                }
            }

            // Audio Asset Cards (Question, Answer, Example, Translation)
            AudioAssetSlotCard(
                label = "Question Audio",
                slotName = "question",
                audioRef = currentQuestionAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftQuestionAudioRef,
                onImportMediaFile = onImportMediaFile
            )

            AudioAssetSlotCard(
                label = "Answer Audio",
                slotName = "answer",
                audioRef = currentAnswerAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftAnswerAudioRef,
                onImportMediaFile = onImportMediaFile
            )

            AudioAssetSlotCard(
                label = "Example Audio",
                slotName = "example",
                audioRef = currentExampleAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftExampleAudioRef,
                onImportMediaFile = onImportMediaFile
            )

            AudioAssetSlotCard(
                label = "Translation Audio",
                slotName = "translation",
                audioRef = currentTranslationAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftTranslationAudioRef,
                onImportMediaFile = onImportMediaFile
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
                    QualityItemRow(label = "Image", variant = if (hasImage) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Question Audio", variant = if (!currentQuestionAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Answer Audio", variant = if (!currentAnswerAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Example Audio", variant = if (!currentExampleAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Translation Audio", variant = if (!currentTranslationAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)

                    QualityItemRow(label = "IPA Format", variant = if (persistedItem?.pronunciation?.isNotBlank() == true) StatusBadgeVariant.Valid else StatusBadgeVariant.Missing)
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
private fun AudioAssetSlotCard(
    label: String,
    slotName: String,
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?,
    onUpdateDraftRef: ((String?) -> Unit)?,
    onImportMediaFile: ((File, String) -> Unit)?
) {
    val isPresent = !audioRef.isNullOrBlank()
    val isPlaying = if (isPresent && audioRef != null && playbackCoordinator != null) {
        playbackCoordinator.getButtonState(audioRef) is AudioButtonState.Playing
    } else false

    LEInspectorCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = LETypography.sectionTitle)
            LEStatusBadge(
                variant = if (isPresent) StatusBadgeVariant.Present else StatusBadgeVariant.Missing
            )
        }

        Spacer(modifier = Modifier.height(LESpacing.sm))

        if (isPresent && audioRef != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = audioRef,
                    style = LETypography.caption,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Audio Ready",
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
                    onClick = {
                        pickFile("Select Audio File", listOf("mp3", "wav", "aiff")) { file ->
                            if (onImportMediaFile != null) {
                                onImportMediaFile(file, slotName)
                            } else {
                                onUpdateDraftRef?.invoke(file.name)
                            }
                        }
                    },
                    icon = LEIcons.Replace,
                    modifier = Modifier.weight(1f)
                )
                LESecondaryButton(
                    text = if (isPlaying) "Stop" else "Preview",
                    onClick = {
                        if (isPlaying) {
                            playbackCoordinator?.stop() ?: onStopAudio?.invoke()
                        } else {
                            playbackCoordinator?.play(audioRef) ?: onPlayAudio?.invoke(audioRef)
                        }
                    },
                    icon = if (isPlaying) LEIcons.Stop else LEIcons.Play,
                    modifier = Modifier.weight(1f)
                )
                LESecondaryButton(
                    text = "Remove",
                    onClick = { onUpdateDraftRef?.invoke(null) },
                    icon = LEIcons.Remove,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                LESecondaryButton(
                    text = "Browse Audio File",
                    onClick = {
                        pickFile("Select Audio File", listOf("mp3", "wav", "aiff")) { file ->
                            if (onImportMediaFile != null) {
                                onImportMediaFile(file, slotName)
                            } else {
                                onUpdateDraftRef?.invoke(file.name)
                            }
                        }
                    },
                    icon = LEIcons.New,
                    modifier = Modifier.fillMaxWidth()
                )
                LEDragDropTarget(
                    label = "or drag & drop audio here",
                    hintText = "MP3, WAV, AIFF"
                )
            }
        }
    }
}

private fun pickFile(title: String, allowedExtensions: List<String>, onFileSelected: (File) -> Unit) {
    try {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isVisible = true
        val dir = dialog.directory
        val fileName = dialog.file
        if (dir != null && fileName != null) {
            val selected = File(dir, fileName)
            if (selected.exists() && selected.isFile) {
                val ext = selected.extension.lowercase()
                if (allowedExtensions.isEmpty() || ext in allowedExtensions) {
                    onFileSelected(selected)
                }
            }
        }
    } catch (_: Exception) {}
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
            color = LEColors.textPrimary,
            modifier = Modifier.padding(LESpacing.xs)
        )
    }
}
