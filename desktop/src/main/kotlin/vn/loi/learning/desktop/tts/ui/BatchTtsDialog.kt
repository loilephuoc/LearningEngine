package vn.loi.learning.desktop.tts.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.runtime.DesktopRuntimeDirectoryResolver
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsAudioParameters
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.batch.BatchTtsJob
import vn.loi.learning.desktop.tts.batch.BatchTtsCheckpointStore
import vn.loi.learning.desktop.tts.batch.BatchTtsJobResult
import vn.loi.learning.desktop.tts.batch.BatchTtsJobStatus
import vn.loi.learning.desktop.tts.batch.BatchTtsLanguageRequirements
import vn.loi.learning.desktop.tts.batch.BatchTtsRunner
import vn.loi.learning.desktop.tts.batch.BatchTtsScanner
import vn.loi.learning.desktop.tts.batch.BatchTtsScopeScan
import vn.loi.learning.desktop.tts.batch.BatchTtsSummary
import vn.loi.learning.desktop.tts.preset.TtsLanguagePresetConfig
import vn.loi.learning.desktop.tts.preset.TtsPreset
import vn.loi.learning.desktop.tts.preset.TtsPresetRepository
import vn.loi.learning.desktop.tts.preset.TtsPresetStore
import vn.loi.learning.desktop.tts.profile.TtsVoiceProfiles
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LEElevation
import vn.loi.learning.desktop.ui.designsystem.LEIcons
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.designsystem.LETypography
import vn.loi.learning.desktop.ui.designsystem.components.LEDangerButton
import vn.loi.learning.desktop.ui.designsystem.components.LEPrimaryButton
import vn.loi.learning.desktop.ui.designsystem.components.LESecondaryButton
import vn.loi.learning.desktop.ui.studio.AudioPlayer
import vn.loi.learning.desktop.ui.studio.AudioPlayerState
import vn.loi.learning.desktop.ui.studio.DesktopAudioPlayer

/**
 * Step of the Batch TTS Dialog lifecycle.
 */
enum class BatchTtsDialogStep {
    CONFIG,
    RUNNING,
    COMPLETED
}

/**
 * Compose Desktop Dialog for Batch TTS Generation across selected items or entire package scope.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BatchTtsDialog(
    title: String = "Batch Generate Audio (TTS)",
    packageName: String,
    itemsToScan: List<PackageContentBrowserItem>,
    targetField: TtsField? = null,
    ttsService: DesktopTtsAudioService,
    contentMediaStorage: ContentMediaStorage? = null,
    presetRepository: TtsPresetRepository? = null,
    audioPlayer: AudioPlayer? = null,
    initialProfiles: TtsVoiceProfiles = TtsVoiceProfiles(),
    onApplyBatch: (results: List<BatchTtsJobResult>) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localAudioPlayer = remember { audioPlayer ?: DesktopAudioPlayer() }

    val resolvedPresetRepo = remember(presetRepository) {
        presetRepository ?: TtsPresetStore(DesktopRuntimeDirectoryResolver.resolve().config.resolve("tts-presets.json"))
    }

    var availablePresets by remember { mutableStateOf(resolvedPresetRepo.listPresets()) }
    var selectedPreset by remember { mutableStateOf(resolvedPresetRepo.getDefaultPreset()) }
    var isDirty by remember { mutableStateOf(false) }
    var presetErrorMessage by remember { mutableStateOf<String?>(null) }

    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var dialogInputName by remember { mutableStateOf("") }

    var currentStep by remember { mutableStateOf(BatchTtsDialogStep.CONFIG) }
    var overwriteExisting by remember { mutableStateOf(false) }
    var showOverwriteConfirmation by remember { mutableStateOf(false) }
    var availableVoices by remember { mutableStateOf<List<TtsVoice>>(emptyList()) }
    var isLoadingVoices by remember { mutableStateOf(true) }

    // Selected Audio Fields (Default: all 4 fields or specific field)
    var selectedFields by remember(targetField) {
        mutableStateOf(if (targetField != null) setOf(targetField) else selectedPreset.selectedFields)
    }

    // Scanned Scope analysis
    val scopeScan = remember(itemsToScan, selectedFields, overwriteExisting) {
        BatchTtsScanner.scanBatchScope(itemsToScan, selectedFields, overwriteExisting)
    }
    val languageRequirements = remember(scopeScan) { BatchTtsLanguageRequirements.from(scopeScan) }

    // Selected Voices and Numeric Parameters
    var selectedEnglishVoice by remember { mutableStateOf<TtsVoice?>(null) }
    var selectedVietnameseVoice by remember { mutableStateOf<TtsVoice?>(null) }
    var englishFallbacks by remember { mutableStateOf(OrderedFallbackVoices.EMPTY) }
    var vietnameseFallbacks by remember { mutableStateOf(OrderedFallbackVoices.EMPTY) }

    var englishRate by remember { mutableStateOf(selectedPreset.english.audioParameters.ratePercent) }
    var englishPitchHz by remember { mutableStateOf(selectedPreset.english.audioParameters.pitchHz) }
    var englishVolumePercent by remember { mutableStateOf(selectedPreset.english.audioParameters.volumePercent) }

    var vietnameseRate by remember { mutableStateOf(selectedPreset.vietnamese.audioParameters.ratePercent) }
    var vietnamesePitchHz by remember { mutableStateOf(selectedPreset.vietnamese.audioParameters.pitchHz) }
    var vietnameseVolumePercent by remember { mutableStateOf(selectedPreset.vietnamese.audioParameters.volumePercent) }

    // Advanced Voice Strategy
    var englishStrategyMode by remember { mutableStateOf(selectedPreset.english.strategyMode) }
    var vietnameseStrategyMode by remember { mutableStateOf(selectedPreset.vietnamese.strategyMode) }

    fun applyPreset(preset: TtsPreset) {
        selectedPreset = preset
        presetErrorMessage = null
        if (targetField == null) {
            selectedFields = preset.selectedFields
        }
        englishRate = preset.english.audioParameters.ratePercent
        englishPitchHz = preset.english.audioParameters.pitchHz
        englishVolumePercent = preset.english.audioParameters.volumePercent
        englishStrategyMode = preset.english.strategyMode

        vietnameseRate = preset.vietnamese.audioParameters.ratePercent
        vietnamesePitchHz = preset.vietnamese.audioParameters.pitchHz
        vietnameseVolumePercent = preset.vietnamese.audioParameters.volumePercent
        vietnameseStrategyMode = preset.vietnamese.strategyMode

        if (availableVoices.isNotEmpty()) {
            selectedEnglishVoice = availableVoices.firstOrNull { it.id == preset.english.primaryVoiceId }
                ?: availableVoices.firstOrNull { it.isEnglish }
            selectedVietnameseVoice = availableVoices.firstOrNull { it.id == preset.vietnamese.primaryVoiceId }
                ?: availableVoices.firstOrNull { it.isVietnamese }
            englishFallbacks = OrderedFallbackVoices.hydrate(preset.english.fallbackVoiceIds, availableVoices, TtsLanguage.ENGLISH, selectedEnglishVoice)
            vietnameseFallbacks = OrderedFallbackVoices.hydrate(preset.vietnamese.fallbackVoiceIds, availableVoices, TtsLanguage.VIETNAMESE, selectedVietnameseVoice)
        }
        isDirty = false
    }

    fun currentDraftPreset(): TtsPreset = TtsPreset(
        id = selectedPreset.id,
        name = selectedPreset.name,
        selectedFields = selectedFields,
        english = TtsLanguagePresetConfig(
            strategyMode = englishStrategyMode,
            primaryVoiceId = selectedEnglishVoice?.id.orEmpty(),
            fallbackVoiceIds = englishFallbacks.ids,
            candidateVoiceIds = listOfNotNull(selectedEnglishVoice?.id) + englishFallbacks.ids,
            audioParameters = TtsAudioParameters(
                ratePercent = englishRate,
                pitchHz = englishPitchHz,
                volumePercent = englishVolumePercent
            )
        ),
        vietnamese = TtsLanguagePresetConfig(
            strategyMode = vietnameseStrategyMode,
            primaryVoiceId = selectedVietnameseVoice?.id.orEmpty(),
            fallbackVoiceIds = vietnameseFallbacks.ids,
            candidateVoiceIds = listOfNotNull(selectedVietnameseVoice?.id) + vietnameseFallbacks.ids,
            audioParameters = TtsAudioParameters(
                ratePercent = vietnameseRate,
                pitchHz = vietnamesePitchHz,
                volumePercent = vietnameseVolumePercent
            )
        ),
        isBuiltIn = selectedPreset.isBuiltIn
    )

    fun handleSavePreset() {
        if (selectedPreset.isBuiltIn) {
            dialogInputName = "${selectedPreset.name} (Custom)"
            showSaveAsDialog = true
        } else {
            try {
                val saved = resolvedPresetRepo.savePreset(currentDraftPreset())
                availablePresets = resolvedPresetRepo.listPresets()
                selectedPreset = saved
                isDirty = false
                presetErrorMessage = null
            } catch (ex: Exception) {
                presetErrorMessage = ex.message ?: "Failed to save preset"
            }
        }
    }

    fun handleSaveAsPreset(name: String) {
        try {
            val created = resolvedPresetRepo.saveAsPreset(name, currentDraftPreset())
            availablePresets = resolvedPresetRepo.listPresets()
            selectedPreset = created
            isDirty = false
            showSaveAsDialog = false
            presetErrorMessage = null
        } catch (ex: Exception) {
            presetErrorMessage = ex.message ?: "Failed to create preset"
        }
    }

    fun handleDuplicatePreset() {
        try {
            val duplicated = resolvedPresetRepo.duplicatePreset(selectedPreset.id)
            availablePresets = resolvedPresetRepo.listPresets()
            selectedPreset = duplicated
            isDirty = false
            presetErrorMessage = null
        } catch (ex: Exception) {
            presetErrorMessage = ex.message ?: "Failed to duplicate preset"
        }
    }

    fun handleRenamePreset(newName: String) {
        try {
            val renamed = resolvedPresetRepo.renamePreset(selectedPreset.id, newName)
            availablePresets = resolvedPresetRepo.listPresets()
            selectedPreset = renamed
            showRenameDialog = false
            presetErrorMessage = null
        } catch (ex: Exception) {
            presetErrorMessage = ex.message ?: "Failed to rename preset"
        }
    }

    fun handleDeletePreset() {
        try {
            resolvedPresetRepo.deletePreset(selectedPreset.id)
            availablePresets = resolvedPresetRepo.listPresets()
            val nextPreset = resolvedPresetRepo.getDefaultPreset()
            applyPreset(nextPreset)
            showDeleteConfirmDialog = false
            presetErrorMessage = null
        } catch (ex: Exception) {
            presetErrorMessage = ex.message ?: "Failed to delete preset"
        }
    }

    fun handleResetToDefaults() {
        val defaultPreset = TtsPreset.createDefault()
        applyPreset(defaultPreset)
        isDirty = true
    }

    // Voice Preview States
    var isPreviewingEn by remember { mutableStateOf(false) }
    var isPreviewingVi by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    // Batch execution state
    var summary by remember { mutableStateOf(BatchTtsSummary.initial(scopeScan.totalValidTargets)) }
    val runner = remember(ttsService, contentMediaStorage, packageName) {
        val checkpointPath = DesktopRuntimeDirectoryResolver.resolve().data
            .resolve("tts-checkpoints")
            .resolve(packageName.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")
        BatchTtsRunner(
            ttsService,
            coroutineScope,
            checkpointStore = BatchTtsCheckpointStore(checkpointPath),
            assetExists = { path -> contentMediaStorage?.exists(path) == true }
        )
    }

    // Load available voices
    LaunchedEffect(Unit) {
        isLoadingVoices = true
        try {
            val voices = ttsService.listVoices()
            availableVoices = voices
            selectedEnglishVoice = voices.firstOrNull { it.id == selectedPreset.english.primaryVoiceId }
                ?: initialProfiles.resolveVoice(TtsLanguage.ENGLISH, voices)
                ?: ttsService.defaultVoiceFor("en", voices)
            selectedVietnameseVoice = voices.firstOrNull { it.id == selectedPreset.vietnamese.primaryVoiceId }
                ?: initialProfiles.resolveVoice(TtsLanguage.VIETNAMESE, voices)
                ?: ttsService.defaultVoiceFor("vi", voices)
            englishFallbacks = OrderedFallbackVoices.hydrate(selectedPreset.english.fallbackVoiceIds, voices, TtsLanguage.ENGLISH, selectedEnglishVoice)
            vietnameseFallbacks = OrderedFallbackVoices.hydrate(selectedPreset.vietnamese.fallbackVoiceIds, voices, TtsLanguage.VIETNAMESE, selectedVietnameseVoice)
        } catch (_: Exception) {
            // Safe fallback
        } finally {
            isLoadingVoices = false
        }
    }

    // Monitor audio player for preview state
    LaunchedEffect(localAudioPlayer.state) {
        if (localAudioPlayer.state is AudioPlayerState.Idle) {
            isPreviewingEn = false
            isPreviewingVi = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewJob?.cancel()
            localAudioPlayer.stop()
            if (audioPlayer == null) {
                localAudioPlayer.release()
            }
        }
    }

    fun stopAudio() {
        previewJob?.cancel()
        previewJob = null
        localAudioPlayer.stop()
        isPreviewingEn = false
        isPreviewingVi = false
    }

    fun handlePreview(text: String, language: TtsLanguage) {
        stopAudio()
        val voice = if (language == TtsLanguage.ENGLISH) selectedEnglishVoice else selectedVietnameseVoice
        val rate = if (language == TtsLanguage.ENGLISH) englishRate else vietnameseRate
        val pitch = if (language == TtsLanguage.ENGLISH) {
            if (englishPitchHz >= 0) "+${englishPitchHz}Hz" else "${englishPitchHz}Hz"
        } else {
            if (vietnamesePitchHz >= 0) "+${vietnamesePitchHz}Hz" else "${vietnamesePitchHz}Hz"
        }
        val volume = if (language == TtsLanguage.ENGLISH) {
            if (englishVolumePercent >= 0) "+${englishVolumePercent}%" else "${englishVolumePercent}%"
        } else {
            if (vietnameseVolumePercent >= 0) "+${vietnameseVolumePercent}%" else "${vietnameseVolumePercent}%"
        }

        if (voice == null || text.isBlank()) return

        if (language == TtsLanguage.ENGLISH) isPreviewingEn = true else isPreviewingVi = true

        previewJob = coroutineScope.launch {
            try {
                val previewPath = ttsService.preview(text, voice, rate, pitch, volume)
                localAudioPlayer.play(previewPath)
            } catch (_: Exception) {
                isPreviewingEn = false
                isPreviewingVi = false
            }
        }
    }

    fun startBatch(jobsToRun: List<BatchTtsJob>) {
        if (jobsToRun.isEmpty()) return
        stopAudio()
        currentStep = BatchTtsDialogStep.RUNNING
        summary = BatchTtsSummary.initial(jobsToRun.size)

        // Generate permanent MP3 assets without auto-applying to Content
        runner.runBatch(
            jobs = jobsToRun,
            packageName = packageName,
            batchId = "$packageName:${selectedFields.sortedBy { it.name }.joinToString(",")}",
            overwriteExisting = overwriteExisting,
            onApply = null, // Generate != Apply separation
            onProgress = { updatedSummary ->
                summary = updatedSummary
                if (updatedSummary.isFinished) {
                    currentStep = BatchTtsDialogStep.COMPLETED
                }
            }
        )
    }

    fun buildConfiguredJobs(): List<BatchTtsJob> {
        if (!languageRequirements.configurationsValid(selectedEnglishVoice, selectedVietnameseVoice)) return emptyList()
        val enVoice = selectedEnglishVoice
        val viVoice = selectedVietnameseVoice
        val enFallbacks = if (languageRequirements.requiresEnglish) englishFallbacks.voices else emptyList()
        val viFallbacks = if (languageRequirements.requiresVietnamese) vietnameseFallbacks.voices else emptyList()
        val enStrategy = enVoice?.takeIf { languageRequirements.requiresEnglish }?.let { primary -> VoiceStrategyConfig(
            mode = englishStrategyMode,
            primaryVoice = primary,
            fallbackVoices = enFallbacks,
            candidateVoices = listOf(primary) + enFallbacks,
            continueSequenceAcrossItems = true
        ) }
        val viStrategy = viVoice?.takeIf { languageRequirements.requiresVietnamese }?.let { primary -> VoiceStrategyConfig(
            mode = vietnameseStrategyMode,
            primaryVoice = primary,
            fallbackVoices = viFallbacks,
            candidateVoices = listOf(primary) + viFallbacks,
            continueSequenceAcrossItems = true
        ) }
        return BatchTtsScanner.buildJobsWithStrategy(
            targets = scopeScan.validTargets,
            englishStrategy = enStrategy,
            vietnameseStrategy = viStrategy,
            englishRate = englishRate,
            vietnameseRate = vietnameseRate,
            englishPitch = if (englishPitchHz >= 0) "+${englishPitchHz}Hz" else "${englishPitchHz}Hz",
            vietnamesePitch = if (vietnamesePitchHz >= 0) "+${vietnamesePitchHz}Hz" else "${vietnamesePitchHz}Hz",
            englishVolume = if (englishVolumePercent >= 0) "+${englishVolumePercent}%" else "${englishVolumePercent}%",
            vietnameseVolume = if (vietnameseVolumePercent >= 0) "+${vietnameseVolumePercent}%" else "${vietnameseVolumePercent}%"
        )
    }

    fun handleStartInitialBatch() {
        val jobs = buildConfiguredJobs()
        if (jobs.isEmpty()) return
        if (overwriteExisting) showOverwriteConfirmation = true else startBatch(jobs)
    }

    fun handleRetryFailed() {
        val failedJobs = summary.failedResults.map { it.job }
        if (failedJobs.isNotEmpty()) {
            startBatch(failedJobs)
        }
    }

    fun handleApply() {
        stopAudio()
        onApplyBatch(summary.jobResults)
        onDismiss()
    }

    val isEntirePackageScope = title.contains("All Missing", ignoreCase = true) || title.contains("Entire Package", ignoreCase = true)
    val scopeSubtitle = if (isEntirePackageScope) {
        "Package: $packageName · Scope: Entire Package (${itemsToScan.size} items)"
    } else {
        val count = itemsToScan.size
        "Package: $packageName · Scope: $count ${if (count == 1) "selected item" else "selected items"}"
    }

    Dialog(
        onDismissRequest = {
            stopAudio()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val workspace = BatchTtsWorkspaceLayout.resolve(maxWidth.value, maxHeight.value)
            Surface(
                modifier = Modifier
                .width(workspace.widthDp.dp)
                .height(workspace.heightDp.dp)
                .clip(LERadius.md)
                .border(1.dp, LEColors.borderSubtle, LERadius.md)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Escape) {
                        stopAudio()
                        onDismiss()
                        true
                    } else false
                },
            color = LEColors.surface,
            shape = LERadius.md,
            shadowElevation = 8.dp
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LEColors.surfaceElevated)
                        .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = LETypography.paneTitle,
                            fontWeight = FontWeight.Bold,
                            color = LEColors.textPrimary
                        )
                        Text(
                            text = scopeSubtitle,
                            style = LETypography.secondaryMetadata,
                            color = LEColors.textMuted
                        )
                    }

                    if (currentStep != BatchTtsDialogStep.RUNNING) {
                        LESecondaryButton(
                            text = "✕",
                            onClick = {
                                stopAudio()
                                onDismiss()
                            }
                        )
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // CONTENT STEP
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(LESpacing.lg)) {
                    when (currentStep) {
                        BatchTtsDialogStep.CONFIG -> {
                            ConfigStepContent(
                                availablePresets = availablePresets,
                                selectedPreset = selectedPreset,
                                isDirty = isDirty,
                                onSelectPreset = { preset -> applyPreset(preset) },
                                onSavePreset = { handleSavePreset() },
                                onSaveAsPreset = {
                                    dialogInputName = "${selectedPreset.name} (Copy)"
                                    showSaveAsDialog = true
                                },
                                onDuplicatePreset = { handleDuplicatePreset() },
                                onRenamePreset = {
                                    dialogInputName = selectedPreset.name
                                    showRenameDialog = true
                                },
                                onDeletePreset = { showDeleteConfirmDialog = true },
                                onResetDefaults = { handleResetToDefaults() },
                                presetErrorMessage = presetErrorMessage,
                                scopeScan = scopeScan,
                                languageRequirements = languageRequirements,
                                selectedFields = selectedFields,
                                onToggleField = { field ->
                                    selectedFields = if (field in selectedFields) {
                                        selectedFields - field
                                    } else {
                                        selectedFields + field
                                    }
                                    isDirty = true
                                },
                                overwriteExisting = overwriteExisting,
                                onOverwriteExistingChange = { overwriteExisting = it },
                                isLoadingVoices = isLoadingVoices,
                                availableVoices = availableVoices,
                                selectedEnglishVoice = selectedEnglishVoice,
                                selectedVietnameseVoice = selectedVietnameseVoice,
                                englishRate = englishRate,
                                englishPitchHz = englishPitchHz,
                                englishVolumePercent = englishVolumePercent,
                                vietnameseRate = vietnameseRate,
                                vietnamesePitchHz = vietnamesePitchHz,
                                vietnameseVolumePercent = vietnameseVolumePercent,
                                englishStrategyMode = englishStrategyMode,
                                vietnameseStrategyMode = vietnameseStrategyMode,
                                onEnglishVoiceChange = {
                                    selectedEnglishVoice = it
                                    englishFallbacks = OrderedFallbackVoices.hydrate(englishFallbacks.ids, availableVoices, TtsLanguage.ENGLISH, it)
                                    isDirty = true
                                },
                                onVietnameseVoiceChange = {
                                    selectedVietnameseVoice = it
                                    vietnameseFallbacks = OrderedFallbackVoices.hydrate(vietnameseFallbacks.ids, availableVoices, TtsLanguage.VIETNAMESE, it)
                                    isDirty = true
                                },
                                englishFallbacks = englishFallbacks,
                                vietnameseFallbacks = vietnameseFallbacks,
                                onEnglishFallbacksChange = { englishFallbacks = it; isDirty = true },
                                onVietnameseFallbacksChange = { vietnameseFallbacks = it; isDirty = true },
                                onEnglishRateChange = {
                                    englishRate = it
                                    isDirty = true
                                },
                                onEnglishPitchChange = {
                                    englishPitchHz = it
                                    isDirty = true
                                },
                                onEnglishVolumeChange = {
                                    englishVolumePercent = it
                                    isDirty = true
                                },
                                onVietnameseRateChange = {
                                    vietnameseRate = it
                                    isDirty = true
                                },
                                onVietnamesePitchChange = {
                                    vietnamesePitchHz = it
                                    isDirty = true
                                },
                                onVietnameseVolumeChange = {
                                    vietnameseVolumePercent = it
                                    isDirty = true
                                },
                                onEnglishStrategyChange = {
                                    englishStrategyMode = it
                                    isDirty = true
                                },
                                onVietnameseStrategyChange = {
                                    vietnameseStrategyMode = it
                                    isDirty = true
                                },
                                isPreviewingEn = isPreviewingEn,
                                isPreviewingVi = isPreviewingVi,
                                onPreviewText = { text, lang -> handlePreview(text, lang) },
                                onStopPreview = { stopAudio() }
                            )
                        }
                        BatchTtsDialogStep.RUNNING -> {
                            RunningStepContent(
                                summary = summary,
                                onCancel = { runner.cancel() }
                            )
                        }
                        BatchTtsDialogStep.COMPLETED -> {
                            CompletedStepContent(
                                summary = summary,
                                contentMediaStorage = contentMediaStorage,
                                localAudioPlayer = localAudioPlayer,
                                onRetryFailed = { handleRetryFailed() }
                            )
                        }
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // FOOTER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (currentStep) {
                        BatchTtsDialogStep.CONFIG -> {
                            LESecondaryButton(
                                text = "Cancel",
                                onClick = {
                                    stopAudio()
                                    onDismiss()
                                }
                            )
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            LEPrimaryButton(
                                text = if (scopeScan.totalValidTargets == 0) "No Missing Targets" else "Generate (${scopeScan.totalValidTargets} Targets)",
                                onClick = { handleStartInitialBatch() },
                                enabled = scopeScan.hasTargets && !isLoadingVoices && languageRequirements.configurationsValid(selectedEnglishVoice, selectedVietnameseVoice),
                                icon = LEIcons.Audio
                            )
                        }
                        BatchTtsDialogStep.RUNNING -> {
                            LEDangerButton(
                                text = "■ Cancel Batch",
                                onClick = { runner.cancel() },
                                icon = LEIcons.Stop
                            )
                        }
                        BatchTtsDialogStep.COMPLETED -> {
                            if (summary.hasFailures) {
                                LESecondaryButton(
                                    text = "🔁 Retry Failed (${summary.failedCount})",
                                    onClick = { handleRetryFailed() }
                                )
                                Spacer(modifier = Modifier.width(LESpacing.sm))
                            }
                            LESecondaryButton(
                                text = "Discard",
                                onClick = {
                                    stopAudio()
                                    onDismiss()
                                }
                            )
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            LEPrimaryButton(
                                text = "Apply (${summary.successCount} Targets)",
                                onClick = { handleApply() },
                                enabled = summary.successCount > 0,
                                icon = LEIcons.Save
                            )
                        }
                    }
                }
            }

            // MODALS / SUB-DIALOGS FOR PRESETS
            if (showSaveAsDialog) {
                PresetNameInputDialog(
                    title = "Save Preset As",
                    initialValue = dialogInputName,
                    confirmLabel = "Save Preset",
                    onConfirm = { name -> handleSaveAsPreset(name) },
                    onDismiss = { showSaveAsDialog = false }
                )
            }

            if (showRenameDialog) {
                PresetNameInputDialog(
                    title = "Rename Preset",
                    initialValue = dialogInputName,
                    confirmLabel = "Rename",
                    onConfirm = { name -> handleRenamePreset(name) },
                    onDismiss = { showRenameDialog = false }
                )
            }

            if (showDeleteConfirmDialog) {
                PresetDeleteConfirmDialog(
                    presetName = selectedPreset.name,
                    onConfirm = { handleDeletePreset() },
                    onDismiss = { showDeleteConfirmDialog = false }
                )
            }
            if (showOverwriteConfirmation) {
                ConfirmOverwriteDialog(
                    selectedFields = selectedFields,
                    onConfirm = {
                        showOverwriteConfirmation = false
                        buildConfiguredJobs().takeIf { it.isNotEmpty() }?.let(::startBatch)
                    },
                    onDismiss = { showOverwriteConfirmation = false }
                )
            }
            }
        }
    }
}

internal data class BatchTtsWorkspaceSize(val widthDp: Float, val heightDp: Float)

internal object BatchTtsWorkspaceLayout {
    private const val LARGE_FRACTION = 0.95f
    private const val STANDARD_FRACTION = 0.94f
    private const val COMPACT_FRACTION = 0.98f

    fun resolve(availableWidthDp: Float, availableHeightDp: Float): BatchTtsWorkspaceSize {
        require(availableWidthDp > 0f && availableHeightDp > 0f)
        val fraction = when {
            availableWidthDp >= 1440f && availableHeightDp >= 900f -> LARGE_FRACTION
            availableWidthDp < 720f || availableHeightDp < 560f -> COMPACT_FRACTION
            else -> STANDARD_FRACTION
        }
        return BatchTtsWorkspaceSize(availableWidthDp * fraction, availableHeightDp * fraction)
    }
}

@Composable
private fun ConfigStepContent(
    availablePresets: List<TtsPreset>,
    selectedPreset: TtsPreset,
    isDirty: Boolean,
    onSelectPreset: (TtsPreset) -> Unit,
    onSavePreset: () -> Unit,
    onSaveAsPreset: () -> Unit,
    onDuplicatePreset: () -> Unit,
    onRenamePreset: () -> Unit,
    onDeletePreset: () -> Unit,
    onResetDefaults: () -> Unit,
    presetErrorMessage: String?,
    scopeScan: BatchTtsScopeScan,
    languageRequirements: BatchTtsLanguageRequirements,
    selectedFields: Set<TtsField>,
    onToggleField: (TtsField) -> Unit,
    overwriteExisting: Boolean,
    onOverwriteExistingChange: (Boolean) -> Unit,
    isLoadingVoices: Boolean,
    availableVoices: List<TtsVoice>,
    selectedEnglishVoice: TtsVoice?,
    selectedVietnameseVoice: TtsVoice?,
    englishRate: Int,
    englishPitchHz: Int,
    englishVolumePercent: Int,
    vietnameseRate: Int,
    vietnamesePitchHz: Int,
    vietnameseVolumePercent: Int,
    englishStrategyMode: VoiceStrategyMode,
    vietnameseStrategyMode: VoiceStrategyMode,
    onEnglishVoiceChange: (TtsVoice) -> Unit,
    onVietnameseVoiceChange: (TtsVoice) -> Unit,
    englishFallbacks: OrderedFallbackVoices,
    vietnameseFallbacks: OrderedFallbackVoices,
    onEnglishFallbacksChange: (OrderedFallbackVoices) -> Unit,
    onVietnameseFallbacksChange: (OrderedFallbackVoices) -> Unit,
    onEnglishRateChange: (Int) -> Unit,
    onEnglishPitchChange: (Int) -> Unit,
    onEnglishVolumeChange: (Int) -> Unit,
    onVietnameseRateChange: (Int) -> Unit,
    onVietnamesePitchChange: (Int) -> Unit,
    onVietnameseVolumeChange: (Int) -> Unit,
    onEnglishStrategyChange: (VoiceStrategyMode) -> Unit,
    onVietnameseStrategyChange: (VoiceStrategyMode) -> Unit,
    isPreviewingEn: Boolean,
    isPreviewingVi: Boolean,
    onPreviewText: (text: String, language: TtsLanguage) -> Unit,
    onStopPreview: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        // Section 0: Reusable Preset Toolbar
        PresetToolbar(
            availablePresets = availablePresets,
            selectedPreset = selectedPreset,
            isDirty = isDirty,
            onSelectPreset = onSelectPreset,
            onSavePreset = onSavePreset,
            onSaveAsPreset = onSaveAsPreset,
            onDuplicatePreset = onDuplicatePreset,
            onRenamePreset = onRenamePreset,
            onDeletePreset = onDeletePreset,
            onResetDefaults = onResetDefaults,
            errorMessage = presetErrorMessage
        )

        // Section 1: Audio Fields to Generate
        Text("Audio Fields to Generate", style = LETypography.fieldValueEmphasized)

        Surface(
            color = LEColors.surfaceElevated,
            shape = LERadius.sm,
            border = BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(LESpacing.md), verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                // English Fields
                Text("English Fields", style = LETypography.caption, color = LEColors.primary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
                ) {
                    FieldCheckbox(
                        label = "Question",
                        missingCount = scopeScan.missingCountByField[TtsField.QUESTION] ?: 0,
                        checked = TtsField.QUESTION in selectedFields,
                        onCheckedChange = { onToggleField(TtsField.QUESTION) },
                        modifier = Modifier.weight(1f)
                    )
                    FieldCheckbox(
                        label = "Answer",
                        missingCount = scopeScan.missingCountByField[TtsField.ANSWER] ?: 0,
                        checked = TtsField.ANSWER in selectedFields,
                        onCheckedChange = { onToggleField(TtsField.ANSWER) },
                        modifier = Modifier.weight(1f)
                    )
                    FieldCheckbox(
                        label = "Example",
                        missingCount = scopeScan.missingCountByField[TtsField.EXAMPLE] ?: 0,
                        checked = TtsField.EXAMPLE in selectedFields,
                        onCheckedChange = { onToggleField(TtsField.EXAMPLE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = LEColors.borderSubtle.copy(alpha = 0.5f))

                // Vietnamese Fields
                Text("Vietnamese Fields", style = LETypography.caption, color = LEColors.success, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
                ) {
                    FieldCheckbox(
                        label = "Translation",
                        missingCount = scopeScan.missingCountByField[TtsField.TRANSLATION] ?: 0,
                        checked = TtsField.TRANSLATION in selectedFields,
                        onCheckedChange = { onToggleField(TtsField.TRANSLATION) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(2f))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = overwriteExisting,
                onCheckedChange = onOverwriteExistingChange,
                colors = CheckboxDefaults.colors(checkedColor = LEColors.danger)
            )
            Column {
                Text("Overwrite existing audio", style = LETypography.fieldValue, color = LEColors.textPrimary)
                Text("Off by default. Replacement is limited to the selected fields and requires confirmation.", style = LETypography.caption, color = LEColors.textMuted)
            }
        }

        // Section 2: Scan Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            MetricCard(
                label = "Total Targets",
                value = "${scopeScan.totalValidTargets}",
                highlight = true,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "English (Q/A/Ex)",
                value = "${scopeScan.englishTargetsCount}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Vietnamese (Tr)",
                value = "${scopeScan.vietnameseTargetsCount}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Existing Skipped",
                value = "${scopeScan.existingAudioSkippedCount}",
                isMuted = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Section 3: Voice & Rate Configurations with Strategy & Preview
        Text("Voice Strategy & Preview", style = LETypography.fieldValueEmphasized)

        if (isLoadingVoices) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(LESpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LEColors.primary)
                Text("Loading available Edge TTS voices...", style = LETypography.caption, color = LEColors.textMuted)
            }
        } else {
            if (languageRequirements.requiresEnglish) VoiceStrategyCard(
                title = "English Voice Configuration",
                languageLabel = "English (en-US)",
                managedFields = listOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE),
                scopeScan = scopeScan,
                currentVoice = selectedEnglishVoice,
                candidateVoices = availableVoices.filter { it.language == "en" },
                onVoiceSelect = onEnglishVoiceChange,
                fallbackVoices = englishFallbacks,
                onFallbackVoicesChange = onEnglishFallbacksChange,
                currentRate = englishRate,
                currentPitchHz = englishPitchHz,
                currentVolumePercent = englishVolumePercent,
                onRateChange = onEnglishRateChange,
                onPitchChange = onEnglishPitchChange,
                onVolumeChange = onEnglishVolumeChange,
                strategyMode = englishStrategyMode,
                onStrategyChange = onEnglishStrategyChange,
                isPreviewing = isPreviewingEn,
                onPreview = { text -> onPreviewText(text, TtsLanguage.ENGLISH) },
                onStop = onStopPreview
            )

            if (languageRequirements.requiresVietnamese) VoiceStrategyCard(
                title = "Vietnamese Voice Configuration",
                languageLabel = "Vietnamese (vi-VN)",
                managedFields = listOf(TtsField.TRANSLATION),
                scopeScan = scopeScan,
                currentVoice = selectedVietnameseVoice,
                candidateVoices = availableVoices.filter { it.language == "vi" },
                onVoiceSelect = onVietnameseVoiceChange,
                fallbackVoices = vietnameseFallbacks,
                onFallbackVoicesChange = onVietnameseFallbacksChange,
                currentRate = vietnameseRate,
                currentPitchHz = vietnamesePitchHz,
                currentVolumePercent = vietnameseVolumePercent,
                onRateChange = onVietnameseRateChange,
                onPitchChange = onVietnamesePitchChange,
                onVolumeChange = onVietnameseVolumeChange,
                strategyMode = vietnameseStrategyMode,
                onStrategyChange = onVietnameseStrategyChange,
                isPreviewing = isPreviewingVi,
                onPreview = { text -> onPreviewText(text, TtsLanguage.VIETNAMESE) },
                onStop = onStopPreview
            )
        }
    }
}

@Composable
private fun FieldCheckbox(
    label: String,
    missingCount: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (checked) LEColors.primarySoft else LEColors.surface,
        shape = LERadius.xs,
        border = BorderStroke(1.dp, if (checked) LEColors.primary.copy(alpha = 0.4f) else LEColors.borderSubtle),
        modifier = modifier.clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = LEColors.primary,
                        uncheckedColor = LEColors.borderSubtle
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = LETypography.caption, fontWeight = FontWeight.Medium)
            }
            Surface(
                color = if (missingCount > 0) LEColors.warningContainer else LEColors.surfaceElevated,
                shape = LERadius.xs
            ) {
                Text(
                    text = "$missingCount missing",
                    style = LETypography.caption,
                    color = if (missingCount > 0) LEColors.warning else LEColors.textMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    highlight: Boolean = false,
    isMuted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlight) LEColors.primarySoft else LEColors.surfaceElevated,
        shape = LERadius.sm,
        border = BorderStroke(1.dp, if (highlight) LEColors.primary.copy(alpha = 0.3f) else LEColors.borderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(LESpacing.sm)) {
            Text(label, style = LETypography.caption, color = if (highlight) LEColors.primary else if (isMuted) LEColors.textMuted else LEColors.textSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = LETypography.paneTitle, fontWeight = FontWeight.Bold, color = if (highlight) LEColors.primary else if (isMuted) LEColors.textMuted else LEColors.textPrimary)
        }
    }
}

@Composable
private fun VoiceStrategyCard(
    title: String,
    languageLabel: String,
    managedFields: List<TtsField>,
    scopeScan: BatchTtsScopeScan,
    currentVoice: TtsVoice?,
    candidateVoices: List<TtsVoice>,
    onVoiceSelect: (TtsVoice) -> Unit,
    fallbackVoices: OrderedFallbackVoices,
    onFallbackVoicesChange: (OrderedFallbackVoices) -> Unit,
    currentRate: Int,
    currentPitchHz: Int,
    currentVolumePercent: Int,
    onRateChange: (Int) -> Unit,
    onPitchChange: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    strategyMode: VoiceStrategyMode,
    onStrategyChange: (VoiceStrategyMode) -> Unit,
    isPreviewing: Boolean,
    onPreview: (text: String) -> Unit,
    onStop: () -> Unit
) {
    var selectedSampleField by remember(managedFields) { mutableStateOf(managedFields.first()) }
    val currentSample = scopeScan.sampleFor(selectedSampleField)

    Surface(
        color = LEColors.surfaceElevated,
        shape = LERadius.sm,
        border = BorderStroke(1.dp, LEColors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(LESpacing.md), verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = LETypography.fieldValue, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                Text(languageLabel, style = LETypography.caption, color = LEColors.textMuted)
            }

            FallbackVoiceEditor(
                primaryVoice = currentVoice,
                fallbackVoices = fallbackVoices,
                catalog = candidateVoices,
                onChange = onFallbackVoicesChange
            )

            // Row 1: Voice & Strategy Mode Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wide Voice Dropdown
                WideVoiceDropdown(
                    currentVoice = currentVoice,
                    candidateVoices = candidateVoices,
                    onSelect = onVoiceSelect,
                    modifier = Modifier.weight(0.6f)
                )

                // Strategy Mode Dropdown
                StrategyModeDropdown(
                    currentMode = strategyMode,
                    onSelectMode = onStrategyChange,
                    modifier = Modifier.weight(0.4f)
                )
            }

            // Row 2: Numeric Audio Controls (Speed %, Pitch Hz, Volume %)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                TtsNumericControl(
                    label = "Speed",
                    value = currentRate,
                    unit = "%",
                    min = -50,
                    max = 50,
                    step = 5,
                    onValueChange = onRateChange,
                    modifier = Modifier.weight(1f)
                )

                TtsNumericControl(
                    label = "Pitch",
                    value = currentPitchHz,
                    unit = "Hz",
                    min = -50,
                    max = 50,
                    step = 2,
                    onValueChange = onPitchChange,
                    modifier = Modifier.weight(1f)
                )

                TtsNumericControl(
                    label = "Volume",
                    value = currentVolumePercent,
                    unit = "%",
                    min = -50,
                    max = 50,
                    step = 5,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f)
                )
            }

            // Sample Text Area with Field Selector & Precise Attribution
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LERadius.xs)
                    .background(LEColors.surface)
                    .border(1.dp, LEColors.borderSubtle, LERadius.xs)
                    .padding(horizontal = LESpacing.sm, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (managedFields.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            managedFields.forEach { field ->
                                val isSelected = field == selectedSampleField
                                Surface(
                                    color = if (isSelected) LEColors.primarySoft else LEColors.surfaceElevated,
                                    shape = LERadius.xs,
                                    border = BorderStroke(1.dp, if (isSelected) LEColors.primary else LEColors.borderSubtle),
                                    modifier = Modifier.clickable { selectedSampleField = field }
                                ) {
                                    Text(
                                        text = field.displayName,
                                        style = LETypography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) LEColors.primary else LEColors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "${managedFields.first().displayName} Sample",
                            style = LETypography.caption,
                            fontWeight = FontWeight.Bold,
                            color = LEColors.textSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        if (currentSample != null) {
                            Text(
                                text = "Source: ${currentSample.displaySource}",
                                style = LETypography.caption,
                                color = LEColors.textMuted
                            )
                        }

                        if (isPreviewing) {
                            LEDangerButton(
                                text = "■ Stop",
                                onClick = onStop,
                                icon = LEIcons.Stop,
                                modifier = Modifier.height(28.dp)
                            )
                        } else {
                            LESecondaryButton(
                                text = "▶ Preview",
                                onClick = { currentSample?.text?.let(onPreview) },
                                icon = LEIcons.Audio,
                                enabled = currentVoice != null && currentSample != null && currentSample.text.isNotBlank(),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                if (currentSample != null) {
                    Text(
                        text = "\"${currentSample.text}\"",
                        style = LETypography.caption,
                        fontWeight = FontWeight.Medium,
                        color = LEColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "No sample text available for ${selectedSampleField.displayName} in current scope",
                        style = LETypography.caption,
                        color = LEColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackVoiceEditor(
    primaryVoice: TtsVoice?,
    fallbackVoices: OrderedFallbackVoices,
    catalog: List<TtsVoice>,
    onChange: (OrderedFallbackVoices) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val available = catalog.filter { candidate ->
        candidate.id != primaryVoice?.id && candidate.id !in fallbackVoices.ids
    }
    Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Fallback Voices — execution order", style = LETypography.fieldLabel, color = LEColors.textSecondary)
            Box {
                LESecondaryButton(
                    text = "+ Add fallback",
                    onClick = { expanded = true },
                    enabled = fallbackVoices.voices.size < OrderedFallbackVoices.MAX_FALLBACKS && available.isNotEmpty()
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    available.forEach { voice ->
                        DropdownMenuItem(
                            text = { Text(voice.displayName, style = LETypography.fieldValue) },
                            onClick = { onChange(fallbackVoices.add(voice, primaryVoice)); expanded = false }
                        )
                    }
                }
            }
        }
        if (fallbackVoices.voices.isEmpty()) {
            Text("Primary voice only", style = LETypography.caption, color = LEColors.textMuted)
        } else fallbackVoices.voices.forEachIndexed { index, voice ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                Text("${index + 1}.", style = LETypography.fieldValue, color = LEColors.primary)
                Text(voice.displayName, style = LETypography.fieldValue, modifier = Modifier.weight(1f))
                LESecondaryButton("↑", { onChange(fallbackVoices.move(voice.id, -1)) }, enabled = index > 0)
                LESecondaryButton("↓", { onChange(fallbackVoices.move(voice.id, 1)) }, enabled = index < fallbackVoices.voices.lastIndex)
                LEDangerButton("Remove", { onChange(fallbackVoices.remove(voice.id)) })
            }
        }
    }
}

@Composable
private fun StrategyModeDropdown(
    currentMode: VoiceStrategyMode,
    onSelectMode: (VoiceStrategyMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(LERadius.xs)
                .clickable { expanded = true }
                .border(1.dp, LEColors.borderSubtle, LERadius.xs),
            color = LEColors.surface
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMode.displayName,
                    style = LETypography.caption,
                    color = LEColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("▾", style = LETypography.caption, color = LEColors.textMuted)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VoiceStrategyMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName, style = LETypography.caption) },
                    onClick = {
                        expanded = false
                        onSelectMode(mode)
                    }
                )
            }
        }
    }
}

@Composable
private fun WideVoiceDropdown(
    currentVoice: TtsVoice?,
    candidateVoices: List<TtsVoice>,
    onSelect: (TtsVoice) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(LERadius.xs)
                .clickable { expanded = true }
                .border(1.dp, LEColors.borderSubtle, LERadius.xs),
            color = LEColors.surface
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentVoice?.let { "${it.displayName} (${it.locale}, ${it.gender ?: "Neutral"})" } ?: "Select Voice...",
                    style = LETypography.caption,
                    fontWeight = FontWeight.Medium,
                    color = if (currentVoice != null) LEColors.textPrimary else LEColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("▾", style = LETypography.caption, color = LEColors.textMuted)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 400.dp, max = 560.dp)
                .heightIn(max = 300.dp)
        ) {
            candidateVoices.forEach { voice ->
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                text = "${voice.displayName} (${voice.gender ?: "Neutral"})",
                                style = LETypography.caption,
                                fontWeight = if (voice.id == currentVoice?.id) FontWeight.Bold else FontWeight.Normal,
                                color = if (voice.id == currentVoice?.id) LEColors.primary else LEColors.textPrimary
                            )
                            Text(
                                text = "${voice.locale} · ${voice.id}",
                                style = LETypography.caption,
                                color = LEColors.textMuted
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(voice)
                    }
                )
            }
        }
    }
}

@Composable
private fun RateDropdown(
    currentRate: Int,
    onSelectRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val rateOptions = TtsRateOption.entries.map { it.rateValue to it.label }

    val label = rateOptions.firstOrNull { it.first == currentRate }?.second ?: if (currentRate == 0) "Normal" else "${if (currentRate > 0) "+$currentRate" else "$currentRate"}%"

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(LERadius.xs)
                .clickable { expanded = true }
                .border(1.dp, LEColors.borderSubtle, LERadius.xs),
            color = LEColors.surface
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = LETypography.caption,
                    color = LEColors.textPrimary,
                    maxLines = 1
                )
                Text("▾", style = LETypography.caption, color = LEColors.textMuted)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rateOptions.forEach { (rateVal, rateLabel) ->
                DropdownMenuItem(
                    text = { Text(rateLabel, style = LETypography.caption) },
                    onClick = {
                        expanded = false
                        onSelectRate(rateVal)
                    }
                )
            }
        }
    }
}

@Composable
private fun RunningStepContent(
    summary: BatchTtsSummary,
    onCancel: () -> Unit
) {
    val progress = if (summary.totalJobs > 0) summary.completedJobs.toFloat() / summary.totalJobs.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(LESpacing.sm))

        Text(
            text = "Synthesizing Audio Assets...",
            style = LETypography.fieldValueEmphasized,
            color = LEColors.primary
        )

        Text(
            text = "${summary.completedJobs} / ${summary.totalJobs}",
            style = LETypography.paneTitle,
            fontWeight = FontWeight.Bold,
            color = LEColors.textPrimary
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(LERadius.xs),
            color = LEColors.primary,
            trackColor = LEColors.borderSubtle
        )

        // Live Counters
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Success: ${summary.successCount}", style = LETypography.caption, color = LEColors.success, fontWeight = FontWeight.Bold)
            if (summary.fallbackRecoveredCount > 0) {
                Text("Fallback: ${summary.fallbackRecoveredCount}", style = LETypography.caption, color = LEColors.primary, fontWeight = FontWeight.Bold)
            }
            Text("Skipped: ${summary.skippedCount}", style = LETypography.caption, color = LEColors.textMuted)
            Text("Failed: ${summary.failedCount}", style = LETypography.caption, color = if (summary.failedCount > 0) LEColors.danger else LEColors.textMuted, fontWeight = FontWeight.Bold)
            if (summary.cancelledCount > 0) {
                Text("Cancelled: ${summary.cancelledCount}", style = LETypography.caption, color = LEColors.warning)
            }
        }

        // Current item
        summary.currentJob?.let { job ->
            Surface(
                color = LEColors.surfaceElevated,
                shape = LERadius.sm,
                modifier = Modifier.fillMaxWidth().padding(top = LESpacing.sm)
            ) {
                Column(modifier = Modifier.padding(LESpacing.sm)) {
                    Text("Synthesizing Target:", style = LETypography.caption, color = LEColors.textMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "[${job.field.displayName}] \"${job.text}\"",
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedStepContent(
    summary: BatchTtsSummary,
    contentMediaStorage: ContentMediaStorage?,
    localAudioPlayer: AudioPlayer,
    onRetryFailed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        // Status header
        Text(
            text = when {
                summary.isCancelled -> "⚠ Batch Cancelled"
                summary.hasFailures -> "⚠ Generation Finished with Issues"
                else -> "✓ Generation Completed (${summary.successCount} files created)"
            },
            style = LETypography.paneTitle,
            fontWeight = FontWeight.Bold,
            color = when {
                summary.isCancelled || summary.hasFailures -> LEColors.warning
                else -> LEColors.success
            }
        )

        // Statistics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            StatPill(label = "Total Targets", value = "${summary.totalJobs}", color = LEColors.textPrimary)
            StatPill(label = "Success", value = "${summary.successCount}", color = LEColors.success)
            if (summary.fallbackRecoveredCount > 0) {
                StatPill(label = "Fallback Recovered", value = "${summary.fallbackRecoveredCount}", color = LEColors.primary)
            }
            StatPill(label = "Failed", value = "${summary.failedCount}", color = if (summary.failedCount > 0) LEColors.danger else LEColors.textMuted)
            if (summary.cancelledCount > 0) {
                StatPill(label = "Cancelled", value = "${summary.cancelledCount}", color = LEColors.warning)
            }
        }

        if (summary.hasFailures) {
            Text("Failed Targets (${summary.failedCount})", style = LETypography.sectionTitle, color = LEColors.danger)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(LERadius.sm)
                    .border(1.dp, LEColors.borderSubtle, LERadius.sm)
                    .background(LEColors.surfaceElevated)
            ) {
                items(summary.failedResults) { result ->
                    FailedJobRow(result)
                    HorizontalDivider(color = LEColors.borderSubtle.copy(alpha = 0.4f))
                }
            }
        } else {
            Surface(
                color = LEColors.surfaceElevated,
                shape = LERadius.sm,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.padding(LESpacing.md), verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    Text(
                        text = "All audio files were synthesized and saved to package storage.",
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.success
                    )
                    if (summary.fallbackRecoveredCount > 0) {
                        Text(
                            text = "${summary.fallbackRecoveredCount} targets were successfully recovered via secondary fallback voices.",
                            style = LETypography.caption,
                            color = LEColors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Click 'Apply' to link these audio references to your content items atomically. You can immediately Undo this operation if needed.",
                        style = LETypography.caption,
                        color = LEColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Surface(
        color = LEColors.surfaceElevated,
        shape = LERadius.xs,
        border = BorderStroke(1.dp, LEColors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = LETypography.caption, color = LEColors.textMuted)
            Text(value, style = LETypography.caption, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun FailedJobRow(result: BatchTtsJobResult) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(LESpacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[${result.job.field.displayName}] ${result.job.text}",
                style = LETypography.caption,
                fontWeight = FontWeight.Bold,
                color = LEColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = LEColors.danger.copy(alpha = 0.12f),
                shape = LERadius.xs
            ) {
                Text(
                    text = result.errorCategory?.displayLabel ?: "Error",
                    style = LETypography.caption,
                    color = LEColors.danger,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        result.errorMessage?.let { msg ->
            Text(
                text = msg,
                style = LETypography.caption,
                color = LEColors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (result.attempts.isNotEmpty()) {
            Text(
                text = "Attempts (${result.attempts.size}): " + result.attempts.joinToString { "${it.voice.displayName} (${it.errorCategory?.displayLabel ?: "Failed"})" },
                style = LETypography.caption,
                color = LEColors.textMuted
            )
        }
    }
}

@Composable
private fun PresetToolbar(
    availablePresets: List<TtsPreset>,
    selectedPreset: TtsPreset,
    isDirty: Boolean,
    onSelectPreset: (TtsPreset) -> Unit,
    onSavePreset: () -> Unit,
    onSaveAsPreset: () -> Unit,
    onDuplicatePreset: () -> Unit,
    onRenamePreset: () -> Unit,
    onDeletePreset: () -> Unit,
    onResetDefaults: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Surface(
        color = LEColors.surfaceElevated,
        shape = LERadius.sm,
        border = BorderStroke(1.dp, LEColors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = LESpacing.md, vertical = LESpacing.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "Preset:",
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.textSecondary
                    )

                    // Preset Dropdown
                    Box {
                        Surface(
                            shape = LERadius.xs,
                            border = BorderStroke(1.dp, LEColors.borderSubtle),
                            color = LEColors.surface,
                            modifier = Modifier
                                .widthIn(min = 160.dp, max = 260.dp)
                                .height(32.dp)
                                .clip(LERadius.xs)
                                .clickable { expandedDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = selectedPreset.name,
                                        style = LETypography.caption,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isDirty) {
                                        Text(
                                            text = "(Modified)",
                                            style = LETypography.caption,
                                            color = LEColors.warning,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text("▾", style = LETypography.caption, color = LEColors.textMuted)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            availablePresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                preset.name,
                                                style = LETypography.caption,
                                                fontWeight = if (preset.id == selectedPreset.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (preset.id == selectedPreset.id) LEColors.primary else LEColors.textPrimary
                                            )
                                            if (preset.isBuiltIn) {
                                                Text(" (Default)", style = LETypography.caption, color = LEColors.textMuted)
                                            }
                                        }
                                    },
                                    onClick = {
                                        expandedDropdown = false
                                        onSelectPreset(preset)
                                    }
                                )
                            }
                        }
                    }

                    // Action buttons
                    LESecondaryButton(
                        text = "Save",
                        onClick = onSavePreset,
                        enabled = isDirty || selectedPreset.isBuiltIn,
                        modifier = Modifier.height(30.dp)
                    )

                    LESecondaryButton(
                        text = "Save As...",
                        onClick = onSaveAsPreset,
                        modifier = Modifier.height(30.dp)
                    )

                    LESecondaryButton(
                        text = "Duplicate",
                        onClick = onDuplicatePreset,
                        modifier = Modifier.height(30.dp)
                    )

                    if (!selectedPreset.isBuiltIn) {
                        LESecondaryButton(
                            text = "Rename",
                            onClick = onRenamePreset,
                            modifier = Modifier.height(30.dp)
                        )

                        LEDangerButton(
                            text = "Delete",
                            onClick = onDeletePreset,
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                LESecondaryButton(
                    text = "Reset Defaults",
                    onClick = onResetDefaults,
                    modifier = Modifier.height(30.dp)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = "⚠ $errorMessage",
                    style = LETypography.caption,
                    color = LEColors.danger,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PresetNameInputDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameText by remember { mutableStateOf(initialValue) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun tryConfirm() {
        val trimmed = nameText.trim()
        if (trimmed.isBlank()) {
            errorText = "Preset name cannot be blank."
            return
        }
        if (trimmed.length > TtsPreset.MAX_NAME_LENGTH) {
            errorText = "Name cannot exceed ${TtsPreset.MAX_NAME_LENGTH} characters."
            return
        }
        onConfirm(trimmed)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .clip(LERadius.md)
                .border(1.dp, LEColors.borderSubtle, LERadius.md)
                .onKeyEvent { event ->
                    if (event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else if (event.key == Key.Enter) {
                        tryConfirm()
                        true
                    } else false
                },
            color = LEColors.surface,
            shape = LERadius.md,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(LESpacing.lg), verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                Text(title, style = LETypography.sectionTitle, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        errorText = null
                    },
                    label = { Text("Preset Name", style = LETypography.caption) },
                    singleLine = true,
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(errorText!!, style = LETypography.caption, color = LEColors.danger)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LESecondaryButton(text = "Cancel", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(LESpacing.sm))
                    LEPrimaryButton(
                        text = confirmLabel,
                        onClick = { tryConfirm() },
                        enabled = nameText.isNotBlank()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PresetDeleteConfirmDialog(
    presetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(400.dp)
                .clip(LERadius.md)
                .border(1.dp, LEColors.borderSubtle, LERadius.md)
                .onKeyEvent { event ->
                    if (event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                },
            color = LEColors.surface,
            shape = LERadius.md,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(LESpacing.lg), verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                Text("Delete Preset", style = LETypography.sectionTitle, fontWeight = FontWeight.Bold, color = LEColors.danger)

                Text(
                    text = "Are you sure you want to delete preset '$presetName'? This action cannot be undone.",
                    style = LETypography.caption,
                    color = LEColors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LESecondaryButton(text = "Cancel", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(LESpacing.sm))
                    LEDangerButton(
                        text = "Delete Preset",
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmOverwriteDialog(
    selectedFields: Set<TtsField>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = LEColors.surface, shape = LERadius.md, shadowElevation = 8.dp) {
            Column(modifier = Modifier.width(440.dp).padding(LESpacing.lg), verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                Text("Confirm audio replacement", style = LETypography.sectionTitle, color = LEColors.danger)
                Text(
                    "Existing audio may be replaced only for: ${selectedFields.sortedBy { it.name }.joinToString { it.displayName }}. " +
                        "Old references remain unchanged if generation or storage fails.",
                    style = LETypography.fieldValue,
                    color = LEColors.textPrimary
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    LESecondaryButton("Cancel", onDismiss)
                    Spacer(Modifier.width(LESpacing.sm))
                    LEDangerButton("Replace selected fields", onConfirm)
                }
            }
        }
    }
}
