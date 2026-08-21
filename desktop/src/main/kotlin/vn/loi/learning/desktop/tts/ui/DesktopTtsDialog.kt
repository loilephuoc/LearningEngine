package vn.loi.learning.desktop.tts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*
import vn.loi.learning.desktop.ui.studio.AudioPlayer
import vn.loi.learning.desktop.ui.studio.AudioPlayerState
import vn.loi.learning.desktop.ui.studio.DesktopAudioPlayer

@Composable
fun DesktopTtsDialog(
    target: TtsDialogTarget,
    packageName: String,
    ttsService: DesktopTtsAudioService,
    contentMediaStorage: ContentMediaStorage? = null,
    audioPlayer: AudioPlayer? = null,
    onApply: (contentId: String, field: TtsField, audioRef: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val localAudioPlayer = remember { audioPlayer ?: DesktopAudioPlayer() }
    val focusRequester = remember { FocusRequester() }

    var uiState by remember {
        mutableStateOf(
            DesktopTtsUiState(
                target = target,
                packageName = packageName
            )
        )
    }

    var previewJob by remember { mutableStateOf<Job?>(null) }
    var generateJob by remember { mutableStateOf<Job?>(null) }

    // Load available voices once on start
    LaunchedEffect(Unit) {
        uiState = uiState.copy(isLoadingVoices = true, voiceLoadError = null)
        try {
            val voices = withContext(Dispatchers.IO) { ttsService.listVoices() }
            val initialLangVoices = when (uiState.selectedLanguage) {
                TtsLanguage.ENGLISH -> voices.filter { it.isEnglish }
                TtsLanguage.VIETNAMESE -> voices.filter { it.isVietnamese }
            }
            val defaultVoice = ttsService.defaultVoiceFor(uiState.selectedLanguage.code, initialLangVoices)
                ?: initialLangVoices.firstOrNull()

            uiState = uiState.copy(
                allVoices = voices,
                selectedVoice = defaultVoice,
                isLoadingVoices = false
            )
        } catch (e: Exception) {
            uiState = uiState.copy(
                isLoadingVoices = false,
                voiceLoadError = "Could not load TTS voices: ${e.message ?: "Service unavailable"}"
            )
        }
    }

    // Monitor audio player state to synchronize playback UI
    LaunchedEffect(localAudioPlayer.state) {
        val playerState = localAudioPlayer.state
        when (playerState) {
            is AudioPlayerState.Playing -> {
                if (uiState.previewState is TtsPreviewState.Synthesizing) {
                    uiState = uiState.copy(previewState = TtsPreviewState.Playing)
                }
            }
            is AudioPlayerState.Idle -> {
                if (uiState.previewState is TtsPreviewState.Playing) {
                    uiState = uiState.copy(previewState = TtsPreviewState.Idle)
                }
                if (uiState.isPlayingGenerated) {
                    uiState = uiState.copy(isPlayingGenerated = false)
                }
            }
            is AudioPlayerState.Error -> {
                if (uiState.previewState is TtsPreviewState.Playing || uiState.previewState is TtsPreviewState.Synthesizing) {
                    uiState = uiState.copy(previewState = TtsPreviewState.Failed(playerState.message))
                }
                if (uiState.isPlayingGenerated) {
                    uiState = uiState.copy(isPlayingGenerated = false)
                }
            }
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
        onDispose {
            previewJob?.cancel()
            generateJob?.cancel()
            localAudioPlayer.stop()
            if (audioPlayer == null) {
                localAudioPlayer.release()
            }
        }
    }

    fun stopAllPlayback() {
        previewJob?.cancel()
        previewJob = null
        localAudioPlayer.stop()
        uiState = uiState.copy(
            previewState = if (uiState.previewState is TtsPreviewState.Playing || uiState.previewState is TtsPreviewState.Synthesizing) TtsPreviewState.Idle else uiState.previewState,
            isPlayingGenerated = false
        )
    }

    fun handlePreview() {
        val voice = uiState.selectedVoice ?: return
        val text = uiState.currentText
        if (text.isBlank()) return

        stopAllPlayback()
        uiState = uiState.copy(previewState = TtsPreviewState.Synthesizing)

        previewJob = coroutineScope.launch {
            try {
                val tempPreviewPath: Path = withContext(Dispatchers.IO) {
                    ttsService.preview(
                        text = text,
                        voice = voice,
                        rate = uiState.selectedRate.rateValue
                    )
                }
                uiState = uiState.copy(previewState = TtsPreviewState.Playing)
                localAudioPlayer.play(tempPreviewPath)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    previewState = TtsPreviewState.Failed(e.message ?: "Preview failed")
                )
            }
        }
    }

    fun handleGenerate() {
        val voice = uiState.selectedVoice ?: return
        val text = uiState.currentText
        if (text.isBlank() || uiState.isCurrentFieldAlreadyPopulated) return

        stopAllPlayback()
        uiState = uiState.copy(generationState = TtsGenerationState.Generating)

        generateJob = coroutineScope.launch {
            try {
                val asset = withContext(Dispatchers.IO) {
                    ttsService.generatePermanentAudio(
                        contentId = target.contentId,
                        packageName = packageName,
                        field = uiState.selectedField,
                        text = text,
                        voice = voice,
                        rate = uiState.selectedRate.rateValue,
                        language = uiState.selectedLanguage.code
                    )
                }
                uiState = uiState.copy(generationState = TtsGenerationState.Success(asset))
            } catch (e: Exception) {
                uiState = uiState.copy(
                    generationState = TtsGenerationState.Failed(e.message ?: "Audio generation failed")
                )
            }
        }
    }

    fun handlePlayGenerated() {
        val successState = uiState.generationState as? TtsGenerationState.Success ?: return
        val storage = contentMediaStorage ?: return
        val resolvedPath = storage.resolve(successState.asset.relativePath) ?: return

        stopAllPlayback()
        uiState = uiState.copy(isPlayingGenerated = true)
        localAudioPlayer.play(resolvedPath)
    }

    fun handleApply() {
        val successState = uiState.generationState as? TtsGenerationState.Success ?: return
        stopAllPlayback()
        onApply(target.contentId, uiState.selectedField, successState.asset.relativePath)
    }

    Dialog(
        onDismissRequest = {
            stopAllPlayback()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .widthIn(min = 540.dp, max = 640.dp)
                    .wrapContentHeight()
                    .testTag("desktop-tts-dialog")
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Escape -> {
                                stopAllPlayback()
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    },
                shape = LERadius.md,
                color = LEColors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
                shadowElevation = LEElevation.popup
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LESpacing.lg)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(LESpacing.md)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                        ) {
                            Icon(
                                imageVector = LEIcons.Audio,
                                contentDescription = null,
                                tint = LEColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "GENERATE AUDIO (TTS)",
                                style = LETypography.paneTitle,
                                color = LEColors.textPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                stopAllPlayback()
                                onDismiss()
                            },
                            modifier = Modifier.size(28.dp).testTag("tts-dialog-close-button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = LEColors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = LEColors.borderSubtle)

                    // Target Field Selector
                    Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)) {
                        Text(
                            text = "Target Field",
                            style = LETypography.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = LEColors.textSecondary
                        )

                        TtsFieldDropdown(
                            target = target,
                            selectedField = uiState.selectedField,
                            onFieldSelected = { field ->
                                stopAllPlayback()
                                val defaultLang = DesktopTtsUiState.defaultLanguageForField(field)
                                val defaultRegion = DesktopTtsUiState.defaultRegionForLanguage(defaultLang)
                                val langVoices = when (defaultLang) {
                                    TtsLanguage.ENGLISH -> uiState.allVoices.filter { it.isEnglish }
                                    TtsLanguage.VIETNAMESE -> uiState.allVoices.filter { it.isVietnamese }
                                }
                                val newVoice = ttsService.defaultVoiceFor(defaultLang.code, langVoices)
                                    ?: langVoices.firstOrNull()

                                uiState = uiState.copy(
                                    selectedField = field,
                                    selectedLanguage = defaultLang,
                                    selectedRegion = defaultRegion,
                                    selectedVoice = newVoice,
                                    generationState = TtsGenerationState.Idle
                                )
                            }
                        )
                    }

                    // Content Text Display (Read-Only)
                    Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)) {
                        Text(
                            text = "Text to Synthesize",
                            style = LETypography.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = LEColors.textSecondary
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = LEColors.surfaceElevated,
                            shape = LERadius.xs,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle)
                        ) {
                            Box(modifier = Modifier.padding(LESpacing.sm)) {
                                if (uiState.isTextBlank) {
                                    Text(
                                        text = "No text available for ${uiState.selectedField.displayName}",
                                        style = LETypography.caption,
                                        color = LEColors.warning,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = uiState.currentText,
                                        style = LETypography.fieldValueEmphasized,
                                        color = LEColors.textPrimary
                                    )
                                }
                            }
                        }

                        if (uiState.isCurrentFieldAlreadyPopulated) {
                            Text(
                                text = "⚠ This field already has audio (${target.audioRefFor(uiState.selectedField)}). Overwriting is disabled.",
                                style = LETypography.caption,
                                color = LEColors.warning
                            )
                        }
                    }

                    // Voice Configuration
                    LECard(
                        backgroundColor = LEColors.surfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(LESpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
                        ) {
                            Text(
                                text = "Voice Settings",
                                style = LETypography.caption,
                                fontWeight = FontWeight.Bold,
                                color = LEColors.textSecondary
                            )

                            // Language & Region row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                            ) {
                                // Language Dropdown
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)
                                ) {
                                    Text(
                                        text = "Language",
                                        style = LETypography.caption,
                                        color = LEColors.textMuted
                                    )
                                    TtsLanguageDropdown(
                                        selectedLanguage = uiState.selectedLanguage,
                                        onLanguageSelected = { lang ->
                                            stopAllPlayback()
                                            val defaultReg = DesktopTtsUiState.defaultRegionForLanguage(lang)
                                            val langVoices = when (lang) {
                                                TtsLanguage.ENGLISH -> uiState.allVoices.filter { it.isEnglish }
                                                TtsLanguage.VIETNAMESE -> uiState.allVoices.filter { it.isVietnamese }
                                            }
                                            val newVoice = ttsService.defaultVoiceFor(lang.code, langVoices)
                                                ?: langVoices.firstOrNull()

                                            uiState = uiState.copy(
                                                selectedLanguage = lang,
                                                selectedRegion = defaultReg,
                                                selectedVoice = newVoice,
                                                generationState = TtsGenerationState.Idle
                                            )
                                        }
                                    )
                                }

                                // Region Dropdown
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)
                                ) {
                                    Text(
                                        text = "Region",
                                        style = LETypography.caption,
                                        color = LEColors.textMuted
                                    )
                                    TtsRegionDropdown(
                                        availableRegions = uiState.availableRegions,
                                        selectedRegion = uiState.selectedRegion,
                                        onRegionSelected = { reg ->
                                            stopAllPlayback()
                                            val matchingVoices = uiState.availableVoicesForLanguage.filter {
                                                it.locale.equals(reg, ignoreCase = true)
                                            }
                                            val newVoice = ttsService.defaultVoiceFor(uiState.selectedLanguage.code, matchingVoices)
                                                ?: matchingVoices.firstOrNull()

                                            uiState = uiState.copy(
                                                selectedRegion = reg,
                                                selectedVoice = newVoice,
                                                generationState = TtsGenerationState.Idle
                                            )
                                        }
                                    )
                                }
                            }

                            // Gender & Voice row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                            ) {
                                // Gender Filter
                                Column(
                                    modifier = Modifier.weight(0.35f),
                                    verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)
                                ) {
                                    Text(
                                        text = "Gender",
                                        style = LETypography.caption,
                                        color = LEColors.textMuted
                                    )
                                    TtsGenderDropdown(
                                        selectedGender = uiState.selectedGender,
                                        onGenderSelected = { gender ->
                                            stopAllPlayback()
                                            val newFiltered = uiState.availableVoicesForLanguage
                                                .filter { it.locale.equals(uiState.selectedRegion, ignoreCase = true) }
                                                .filter { voice ->
                                                    when (gender) {
                                                        TtsGenderFilter.ALL -> true
                                                        TtsGenderFilter.FEMALE -> voice.gender?.equals("Female", ignoreCase = true) == true
                                                        TtsGenderFilter.MALE -> voice.gender?.equals("Male", ignoreCase = true) == true
                                                    }
                                                }
                                            val newVoice = if (uiState.selectedVoice in newFiltered) {
                                                uiState.selectedVoice
                                            } else {
                                                newFiltered.firstOrNull() ?: uiState.selectedVoice
                                            }

                                            uiState = uiState.copy(
                                                selectedGender = gender,
                                                selectedVoice = newVoice
                                            )
                                        }
                                    )
                                }

                                // Voice Selector
                                Column(
                                    modifier = Modifier.weight(0.65f),
                                    verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)
                                ) {
                                    Text(
                                        text = "Voice",
                                        style = LETypography.caption,
                                        color = LEColors.textMuted
                                    )
                                    TtsVoiceDropdown(
                                        voices = uiState.filteredVoices,
                                        selectedVoice = uiState.selectedVoice,
                                        isLoading = uiState.isLoadingVoices,
                                        onVoiceSelected = { voice ->
                                            stopAllPlayback()
                                            uiState = uiState.copy(
                                                selectedVoice = voice,
                                                generationState = TtsGenerationState.Idle
                                            )
                                        }
                                    )
                                }
                            }

                            // Speech Rate row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.width(180.dp),
                                    verticalArrangement = Arrangement.spacedBy(LESpacing.xxs)
                                ) {
                                    Text(
                                        text = "Speech Rate",
                                        style = LETypography.caption,
                                        color = LEColors.textMuted
                                    )
                                    TtsRateDropdown(
                                        selectedRate = uiState.selectedRate,
                                        onRateSelected = { rate ->
                                            stopAllPlayback()
                                            uiState = uiState.copy(
                                                selectedRate = rate,
                                                generationState = TtsGenerationState.Idle
                                            )
                                        }
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                // Preview Button
                                val isPreviewPlaying = uiState.previewState is TtsPreviewState.Playing
                                val isPreviewSynthesizing = uiState.previewState is TtsPreviewState.Synthesizing

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                                ) {
                                    if (isPreviewSynthesizing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = LEColors.primary
                                        )
                                    }

                                    LESecondaryButton(
                                        text = when {
                                            isPreviewSynthesizing -> "Loading..."
                                            isPreviewPlaying -> "■ Stop Preview"
                                            else -> "▶ Preview"
                                        },
                                        onClick = {
                                            if (isPreviewPlaying || isPreviewSynthesizing) {
                                                stopAllPlayback()
                                            } else {
                                                handlePreview()
                                            }
                                        },
                                        enabled = uiState.canPreview,
                                        modifier = Modifier.testTag("tts-preview-button")
                                    )
                                }
                            }

                            // Preview status / errors
                            when (val prev = uiState.previewState) {
                                is TtsPreviewState.Failed -> {
                                    Text(
                                        text = "Preview failed: ${prev.message}",
                                        style = LETypography.caption,
                                        color = LEColors.danger
                                    )
                                }
                                is TtsPreviewState.Playing -> {
                                    Text(
                                        text = "Playing preview (temporary audio)...",
                                        style = LETypography.caption,
                                        color = LEColors.primary
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }

                    // Generation & Verification Area
                    val isGenerating = uiState.generationState is TtsGenerationState.Generating
                    val generationSuccess = uiState.generationState as? TtsGenerationState.Success
                    val generationFailed = uiState.generationState as? TtsGenerationState.Failed

                    if (generationSuccess != null) {
                        Surface(
                            color = LEColors.success.copy(alpha = 0.12f),
                            shape = LERadius.xs,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.success.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(LESpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "✓ Generated successfully",
                                        style = LETypography.fieldValueEmphasized,
                                        color = LEColors.success
                                    )
                                    Text(
                                        text = "File: ${generationSuccess.asset.relativePath}",
                                        style = LETypography.caption,
                                        color = LEColors.textSecondary
                                    )
                                }

                                LESecondaryButton(
                                    text = if (uiState.isPlayingGenerated) "■ Stop Audio" else "▶ Play Generated Audio",
                                    onClick = {
                                        if (uiState.isPlayingGenerated) {
                                            stopAllPlayback()
                                        } else {
                                            handlePlayGenerated()
                                        }
                                    },
                                    modifier = Modifier.testTag("tts-play-generated-button")
                                )
                            }
                        }
                    }

                    if (generationFailed != null) {
                        Surface(
                            color = LEColors.danger.copy(alpha = 0.12f),
                            shape = LERadius.xs,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.danger.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(LESpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Audio generation failed: ${generationFailed.message}",
                                    style = LETypography.caption,
                                    color = LEColors.danger
                                )
                            }
                        }
                    }

                    // Action buttons (Cancel, Generate, Apply)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = LESpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LESecondaryButton(
                            text = "Cancel",
                            onClick = {
                                stopAllPlayback()
                                onDismiss()
                            },
                            modifier = Modifier.testTag("tts-dialog-cancel-button")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LEColors.primary
                                )
                            }

                            LESecondaryButton(
                                text = if (isGenerating) "Generating..." else "Generate",
                                onClick = ::handleGenerate,
                                enabled = uiState.canGenerate,
                                modifier = Modifier.testTag("tts-dialog-generate-button")
                            )

                            LEPrimaryButton(
                                text = "Apply",
                                onClick = ::handleApply,
                                enabled = uiState.canApply,
                                modifier = Modifier.testTag("tts-dialog-apply-button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TtsFieldDropdown(
    target: TtsDialogTarget,
    selectedField: TtsField,
    onFieldSelected: (TtsField) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = LERadius.xs,
            color = LEColors.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val hasAudio = target.hasAudioFor(selectedField)
                Text(
                    text = "${selectedField.displayName} ${if (hasAudio) "[Has Audio]" else "[Missing Audio]"}",
                    style = LETypography.fieldValue,
                    color = if (hasAudio) LEColors.textMuted else LEColors.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = LEColors.textMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TtsField.entries.forEach { field ->
                val hasAudio = target.hasAudioFor(field)
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = field.displayName,
                                fontWeight = if (field == selectedField) FontWeight.Bold else FontWeight.Normal,
                                color = if (field == selectedField) LEColors.primary else LEColors.textPrimary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = if (hasAudio) "Has Audio" else "Missing",
                                style = LETypography.caption,
                                color = if (hasAudio) LEColors.textMuted else LEColors.warning
                            )
                        }
                    },
                    onClick = {
                        onFieldSelected(field)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TtsLanguageDropdown(
    selectedLanguage: TtsLanguage,
    onLanguageSelected: (TtsLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = LERadius.xs,
            color = LEColors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedLanguage.displayName,
                    style = LETypography.fieldValue,
                    color = LEColors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = LEColors.textMuted
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TtsLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = lang.displayName,
                            fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                            color = if (lang == selectedLanguage) LEColors.primary else LEColors.textPrimary
                        )
                    },
                    onClick = {
                        onLanguageSelected(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TtsRegionDropdown(
    availableRegions: List<String>,
    selectedRegion: String,
    onRegionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = LERadius.xs,
            color = LEColors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = DesktopTtsUiState.regionDisplayName(selectedRegion),
                    style = LETypography.fieldValue,
                    color = LEColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = LEColors.textMuted
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableRegions.forEach { reg ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = DesktopTtsUiState.regionDisplayName(reg),
                            fontWeight = if (reg.equals(selectedRegion, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                            color = if (reg.equals(selectedRegion, ignoreCase = true)) LEColors.primary else LEColors.textPrimary
                        )
                    },
                    onClick = {
                        onRegionSelected(reg)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TtsGenderDropdown(
    selectedGender: TtsGenderFilter,
    onGenderSelected: (TtsGenderFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = LERadius.xs,
            color = LEColors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedGender.label,
                    style = LETypography.fieldValue,
                    color = LEColors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = LEColors.textMuted
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TtsGenderFilter.entries.forEach { gender ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = gender.label,
                            fontWeight = if (gender == selectedGender) FontWeight.Bold else FontWeight.Normal,
                            color = if (gender == selectedGender) LEColors.primary else LEColors.textPrimary
                        )
                    },
                    onClick = {
                        onGenderSelected(gender)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TtsVoiceDropdown(
    voices: List<TtsVoice>,
    selectedVoice: TtsVoice?,
    isLoading: Boolean,
    onVoiceSelected: (TtsVoice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = LERadius.xs,
            color = LEColors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable(enabled = !isLoading && voices.isNotEmpty()) { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        isLoading -> "Loading voices..."
                        selectedVoice != null -> "${selectedVoice.displayName} (${selectedVoice.gender ?: "Neutral"})"
                        voices.isEmpty() -> "No voices found"
                        else -> "Select a voice"
                    },
                    style = LETypography.fieldValue,
                    color = if (selectedVoice != null) LEColors.textPrimary else LEColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = LEColors.textMuted
                )
            }
        }

        if (voices.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                voices.forEach { voice ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = voice.displayName,
                                    fontWeight = if (voice.id == selectedVoice?.id) FontWeight.Bold else FontWeight.Normal,
                                    color = if (voice.id == selectedVoice?.id) LEColors.primary else LEColors.textPrimary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = voice.gender ?: "",
                                    style = LETypography.caption,
                                    color = LEColors.textMuted
                                )
                            }
                        },
                        onClick = {
                            onVoiceSelected(voice)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TtsRateDropdown(
    selectedRate: TtsRateOption,
    onRateSelected: (TtsRateOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = LERadius.xs,
            color = LEColors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedRate.label,
                    style = LETypography.fieldValue,
                    color = LEColors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = LEColors.textMuted
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TtsRateOption.entries.forEach { rate ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = rate.label,
                            fontWeight = if (rate == selectedRate) FontWeight.Bold else FontWeight.Normal,
                            color = if (rate == selectedRate) LEColors.primary else LEColors.textPrimary
                        )
                    },
                    onClick = {
                        onRateSelected(rate)
                        expanded = false
                    }
                )
            }
        }
    }
}
