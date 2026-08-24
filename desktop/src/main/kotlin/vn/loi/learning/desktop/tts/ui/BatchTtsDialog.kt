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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfigurationLoader
import vn.loi.learning.desktop.runtime.DesktopRuntimeDirectoryResolver
import vn.loi.learning.desktop.runtime.FileDesktopRuntimeLogger
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsAudioParameters
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.batch.BatchTtsCheckpointRepository
import vn.loi.learning.desktop.tts.batch.BatchTtsEventLogger
import vn.loi.learning.desktop.tts.batch.BatchTtsJob
import vn.loi.learning.desktop.tts.batch.BatchTtsJobResult
import vn.loi.learning.desktop.tts.batch.BatchTtsJobStatus
import vn.loi.learning.desktop.tts.batch.BatchTtsLanguageRequirements
import vn.loi.learning.desktop.tts.batch.BatchTtsPlanIdentity
import vn.loi.learning.desktop.tts.batch.BatchTtsRunner
import vn.loi.learning.desktop.tts.batch.BatchTtsScanner
import vn.loi.learning.desktop.tts.batch.BatchTtsScopeScan
import vn.loi.learning.desktop.tts.batch.BatchTtsSummary
import vn.loi.learning.desktop.tts.batch.BatchTtsTarget
import vn.loi.learning.desktop.tts.batch.RuntimeBatchTtsEventLogger
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
    COMPLETED,
    APPLYING,
    APPLY_COMPLETED
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
    onApplyBatch: ((
        results: List<BatchTtsJobResult>,
        onProgress: (appliedCount: Int, totalCount: Int, failedCount: Int) -> Unit,
        onComplete: (appliedCount: Int, failedCount: Int) -> Unit
    ) -> Unit)? = null,
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
    var previewingVoiceId by remember { mutableStateOf<String?>(null) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    // Session-persistent Voice Picker Filter States
    val englishPickerFilterState = remember { VoicePickerFilterState() }
    val vietnamesePickerFilterState = remember { VoicePickerFilterState() }

    // Batch execution state
    var summary by remember { mutableStateOf(BatchTtsSummary.initial(scopeScan.totalValidTargets)) }
    val eventLogger = remember {
        val directories = DesktopRuntimeDirectoryResolver.resolve()
        val configFile = directories.config.resolve(DesktopRuntimeConfiguration.FILE_NAME)
        val config = runCatching { DesktopRuntimeConfigurationLoader.load(configFile) }.getOrDefault(DesktopRuntimeConfiguration())
        val logger = runCatching { FileDesktopRuntimeLogger.open(directories.logs, config) }.getOrNull()
        if (logger != null) RuntimeBatchTtsEventLogger(logger) else BatchTtsEventLogger.NoOp
    }
    val checkpointRepository = remember {
        BatchTtsCheckpointRepository(
            DesktopRuntimeDirectoryResolver.resolve().data.resolve("tts-checkpoints"),
            eventLogger = eventLogger
        )
    }
    var activeRunner by remember { mutableStateOf<BatchTtsRunner?>(null) }
    var isCancelling by remember { mutableStateOf(false) }
    var batchStartMessage by remember { mutableStateOf<String?>(null) }

    // Apply Phase States
    var applyProgress by remember { mutableStateOf(0f) }
    var applyCompletedCount by remember { mutableStateOf(0) }
    var applyTotalCount by remember { mutableStateOf(0) }
    var applyFailedCount by remember { mutableStateOf(0) }
    var applyStartTimeMillis by remember { mutableStateOf(0L) }

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
            previewingVoiceId = null
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
        previewingVoiceId = null
    }

    fun handlePreviewCandidateVoice(
        voice: TtsVoice,
        text: String,
        ratePercent: Int,
        pitchHz: Int,
        volumePercent: Int
    ) {
        stopAudio()
        if (text.isBlank()) return

        val rate = ratePercent
        val pitch = if (pitchHz >= 0) "+${pitchHz}Hz" else "${pitchHz}Hz"
        val volume = if (volumePercent >= 0) "+${volumePercent}%" else "${volumePercent}%"

        previewingVoiceId = voice.id
        if (voice.isEnglish) isPreviewingEn = true else isPreviewingVi = true

        previewJob = coroutineScope.launch {
            try {
                val previewPath = ttsService.preview(text, voice, rate, pitch, volume)
                localAudioPlayer.play(previewPath)
            } catch (_: Exception) {
                previewingVoiceId = null
                isPreviewingEn = false
                isPreviewingVi = false
            }
        }
    }

    fun handlePreview(text: String, language: TtsLanguage) {
        val voice = if (language == TtsLanguage.ENGLISH) selectedEnglishVoice else selectedVietnameseVoice
        val rate = if (language == TtsLanguage.ENGLISH) englishRate else vietnameseRate
        val pitchHz = if (language == TtsLanguage.ENGLISH) englishPitchHz else vietnamesePitchHz
        val volumePercent = if (language == TtsLanguage.ENGLISH) englishVolumePercent else vietnameseVolumePercent
        if (voice != null) {
            handlePreviewCandidateVoice(voice, text, rate, pitchHz, volumePercent)
        }
    }

    fun handleCancelBatch() {
        isCancelling = true
        activeRunner?.cancel()
    }

    fun startBatch(
        jobsToRun: List<BatchTtsJob>,
        mergedBaseResults: List<BatchTtsJobResult> = emptyList()
    ) {
        if (jobsToRun.isEmpty()) return
        val plan = BatchTtsPlanIdentity.create(packageName, jobsToRun, overwriteExisting)
        eventLogger.logPlanCreated(
            planId = plan.id,
            packageId = packageName,
            targetCount = jobsToRun.size,
            overwriteMode = overwriteExisting,
            selectedFields = jobsToRun.map { it.field.name }.distinct().sorted().joinToString(","),
            languageRequirements = languageRequirements.toString()
        )
        val store = checkpointRepository.storeFor(plan)
        val resumesExisting = store.load() != null
        val ownership = try {
            checkpointRepository.acquire(plan)
        } catch (_: IllegalStateException) {
            batchStartMessage = "Phiên tạo audio này đang chạy ở cửa sổ khác."
            return
        }
        batchStartMessage = if (resumesExisting) "Đã tìm thấy phiên tạo audio chưa hoàn tất. Đang tiếp tục…" else null
        val runner = BatchTtsRunner(
            ttsService,
            coroutineScope,
            checkpointStore = store,
            assetExists = { path -> contentMediaStorage?.exists(path) == true },
            checkpointOwnership = ownership,
            eventLogger = eventLogger
        )
        activeRunner = runner
        isCancelling = false
        stopAudio()
        currentStep = BatchTtsDialogStep.RUNNING
        summary = BatchTtsSummary.initial(jobsToRun.size + mergedBaseResults.size, skippedCount = 0).copy(
            jobResults = mergedBaseResults,
            successCount = mergedBaseResults.count { it.status == BatchTtsJobStatus.SUCCESS },
            completedJobs = mergedBaseResults.size
        )

        // Generate permanent MP3 assets without auto-applying to Content
        runner.runBatch(
            jobs = jobsToRun,
            packageName = packageName,
            batchId = plan.id,
            overwriteExisting = overwriteExisting,
            onApply = null, // Generate != Apply separation
            onProgress = { updatedSummary ->
                if (mergedBaseResults.isEmpty()) {
                    summary = updatedSummary
                } else {
                    val combinedResults = mergedBaseResults + updatedSummary.jobResults
                    summary = updatedSummary.copy(
                        totalJobs = mergedBaseResults.size + updatedSummary.totalJobs,
                        completedJobs = mergedBaseResults.size + updatedSummary.completedJobs,
                        successCount = mergedBaseResults.count { it.status == BatchTtsJobStatus.SUCCESS } + updatedSummary.successCount,
                        failedCount = updatedSummary.failedCount,
                        cancelledCount = updatedSummary.cancelledCount,
                        jobResults = combinedResults
                    )
                }
                if (updatedSummary.isFinished || updatedSummary.isCancelled) {
                    isCancelling = false
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
        val previousSuccesses = summary.successfulResults
        val failedResults = summary.failedResults
        if (failedResults.isEmpty()) return

        val failedTargets = failedResults.map { result ->
            BatchTtsTarget(
                contentId = result.job.contentId,
                field = result.job.field,
                text = result.job.text,
                language = result.job.language,
                isMissing = true,
                hasAudio = false,
                previousAudioRef = result.job.previousAudioRef
            )
        }

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

        val retryJobs = BatchTtsScanner.buildJobsWithStrategy(
            targets = failedTargets,
            englishStrategy = enStrategy,
            vietnameseStrategy = viStrategy,
            englishRate = englishRate,
            vietnameseRate = vietnameseRate,
            englishPitch = if (englishPitchHz >= 0) "+${englishPitchHz}Hz" else "${englishPitchHz}Hz",
            vietnamesePitch = if (vietnamesePitchHz >= 0) "+${vietnamesePitchHz}Hz" else "${vietnamesePitchHz}Hz",
            englishVolume = if (englishVolumePercent >= 0) "+${englishVolumePercent}%" else "${englishVolumePercent}%",
            vietnameseVolume = if (vietnameseVolumePercent >= 0) "+${vietnameseVolumePercent}%" else "${vietnameseVolumePercent}%"
        )

        if (retryJobs.isEmpty()) return

        startBatch(
            jobsToRun = retryJobs,
            mergedBaseResults = previousSuccesses
        )
    }

    fun handleApply() {
        stopAudio()
        currentStep = BatchTtsDialogStep.APPLYING
        applyCompletedCount = 0
        applyTotalCount = summary.successCount
        applyFailedCount = 0
        applyProgress = 0f
        applyStartTimeMillis = System.currentTimeMillis()

        eventLogger.logApplyStarted(packageName, summary.successCount)

        if (onApplyBatch != null) {
            onApplyBatch.invoke(
                summary.jobResults,
                { applied, total, failed ->
                    applyCompletedCount = applied
                    applyTotalCount = total
                    applyFailedCount = failed
                    applyProgress = if (total > 0) (applied.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 1f
                    eventLogger.logApplyProgress(packageName, applied, total, failed)
                },
                { finalApplied, finalFailed ->
                    val duration = System.currentTimeMillis() - applyStartTimeMillis
                    applyCompletedCount = finalApplied
                    applyFailedCount = finalFailed
                    applyProgress = 1f
                    currentStep = BatchTtsDialogStep.APPLY_COMPLETED
                    eventLogger.logApplyCompleted(packageName, finalApplied, finalFailed, duration)
                }
            )
        } else {
            applyCompletedCount = summary.successCount
            applyFailedCount = 0
            applyProgress = 1f
            currentStep = BatchTtsDialogStep.APPLY_COMPLETED
        }
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
            if (currentStep == BatchTtsDialogStep.APPLYING) {
                return@Dialog
            }
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
                        if (currentStep != BatchTtsDialogStep.APPLYING) {
                            stopAudio()
                            onDismiss()
                            true
                        } else false
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
                            style = BatchTtsUiScale.dialogTitle,
                            color = LEColors.textPrimary
                        )
                        Text(
                            text = scopeSubtitle,
                            style = BatchTtsUiScale.body,
                            color = LEColors.textMuted
                        )
                    }

                    if (currentStep != BatchTtsDialogStep.RUNNING && currentStep != BatchTtsDialogStep.APPLYING) {
                        LESecondaryButton(
                            text = "✕",
                            onClick = {
                                stopAudio()
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // CONTENT STEP
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(BatchTtsUiScale.cardPadding)) {
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
                                previewingVoiceId = previewingVoiceId,
                                englishFilterState = englishPickerFilterState,
                                vietnameseFilterState = vietnamesePickerFilterState,
                                onPreviewText = { text, lang -> handlePreview(text, lang) },
                                onPreviewCandidateVoice = { voice, text, rate, pitch, vol ->
                                    handlePreviewCandidateVoice(voice, text, rate, pitch, vol)
                                },
                                onStopPreview = { stopAudio() }
                            )
                        }
                        BatchTtsDialogStep.RUNNING -> {
                            RunningStepContent(
                                summary = summary,
                                isCancelling = isCancelling,
                                onCancel = { handleCancelBatch() }
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
                        BatchTtsDialogStep.APPLYING -> {
                            ApplyingStepContent(
                                completedCount = applyCompletedCount,
                                totalCount = applyTotalCount,
                                failedCount = applyFailedCount,
                                startTimeMillis = applyStartTimeMillis
                            )
                        }
                        BatchTtsDialogStep.APPLY_COMPLETED -> {
                            ApplyCompletedStepContent(
                                appliedCount = applyCompletedCount,
                                failedCount = applyFailedCount,
                                totalCount = applyTotalCount,
                                packageName = packageName
                            )
                        }
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                batchStartMessage?.let { message ->
                    Text(
                        message,
                        style = BatchTtsUiScale.body,
                        color = LEColors.warning,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = LESpacing.lg, vertical = LESpacing.xs)
                    )
                }

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
                                },
                                modifier = Modifier.height(BatchTtsUiScale.buttonHeightStandard)
                            )
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            LEPrimaryButton(
                                text = if (scopeScan.totalValidTargets == 0) "No Missing Targets" else "Generate (${scopeScan.totalValidTargets} Targets)",
                                onClick = { handleStartInitialBatch() },
                                enabled = scopeScan.hasTargets && !isLoadingVoices && languageRequirements.configurationsValid(selectedEnglishVoice, selectedVietnameseVoice),
                                icon = LEIcons.Audio,
                                modifier = Modifier.height(BatchTtsUiScale.buttonHeightStandard)
                            )
                        }
                        BatchTtsDialogStep.RUNNING -> {
                            LEDangerButton(
                                text = if (isCancelling) "Đang hủy..." else "■ Cancel Batch",
                                onClick = { handleCancelBatch() },
                                enabled = !isCancelling,
                                icon = LEIcons.Stop,
                                modifier = Modifier.height(BatchTtsUiScale.buttonHeightStandard)
                            )
                        }
                        BatchTtsDialogStep.COMPLETED -> {
                            if (summary.hasFailures) {
                                LESecondaryButton(
                                    text = "🔁 Retry Failed (${summary.failedCount})",
                                    onClick = { handleRetryFailed() },
                                    modifier = Modifier.height(BatchTtsUiScale.buttonHeightStandard)
                                )
                                Spacer(modifier = Modifier.width(LESpacing.sm))
                            }
                            LESecondaryButton(
                                text = "Discard",
                                onClick = {
                                    stopAudio()
                                    onDismiss()
                                },
                                modifier = Modifier.height(BatchTtsUiScale.buttonHeightStandard)
                            )
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            LEPrimaryButton(
                                text = "Apply (${summary.successCount} Targets)",
                                onClick = { handleApply() },
                                enabled = summary.successCount > 0,
                                icon = LEIcons.Save,
                                modifier = Modifier.height(BatchTtsUiScale.buttonHeightStandard)
                            )
                        }
                        BatchTtsDialogStep.APPLYING -> {
                            Text(
                                text = "Đang lưu trữ dữ liệu an toàn...",
                                style = BatchTtsUiScale.body,
                                color = LEColors.textMuted,
                                modifier = Modifier.padding(horizontal = LESpacing.md)
                            )
                        }
                        BatchTtsDialogStep.APPLY_COMPLETED -> {
                            LEPrimaryButton(
                                text = "Đóng",
                                onClick = {
                                    stopAudio()
                                    onDismiss()
                                },
                                icon = LEIcons.Success,
                                modifier = Modifier.height(BatchTtsUiScale.buttonHeightStandard)
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
    previewingVoiceId: String?,
    englishFilterState: VoicePickerFilterState,
    vietnameseFilterState: VoicePickerFilterState,
    onPreviewText: (text: String, language: TtsLanguage) -> Unit,
    onPreviewCandidateVoice: (voice: TtsVoice, text: String, rate: Int, pitchHz: Int, volumePercent: Int) -> Unit,
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
        Text("Audio Fields to Generate", style = BatchTtsUiScale.sectionHeading)

        Surface(
            color = LEColors.surfaceElevated,
            shape = LERadius.sm,
            border = BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(BatchTtsUiScale.cardPadding), verticalArrangement = Arrangement.spacedBy(BatchTtsUiScale.itemSpacing)) {
                // English Fields
                Text("English Fields (Prompt & Example)", style = BatchTtsUiScale.controlSecondary, color = LEColors.primary, fontWeight = FontWeight.Bold)
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
                        label = "Example",
                        missingCount = scopeScan.missingCountByField[TtsField.EXAMPLE] ?: 0,
                        checked = TtsField.EXAMPLE in selectedFields,
                        onCheckedChange = { onToggleField(TtsField.EXAMPLE) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = LEColors.borderSubtle.copy(alpha = 0.5f))

                // Vietnamese Fields
                Text("Vietnamese Fields (Answer & Translation)", style = BatchTtsUiScale.controlSecondary, color = LEColors.success, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
                ) {
                    FieldCheckbox(
                        label = "Answer",
                        missingCount = scopeScan.missingCountByField[TtsField.ANSWER] ?: 0,
                        checked = TtsField.ANSWER in selectedFields,
                        onCheckedChange = { onToggleField(TtsField.ANSWER) },
                        modifier = Modifier.weight(1f)
                    )
                    FieldCheckbox(
                        label = "Translation",
                        missingCount = scopeScan.missingCountByField[TtsField.TRANSLATION] ?: 0,
                        checked = TtsField.TRANSLATION in selectedFields,
                        onCheckedChange = { onToggleField(TtsField.TRANSLATION) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = overwriteExisting,
                onCheckedChange = onOverwriteExistingChange,
                colors = CheckboxDefaults.colors(checkedColor = LEColors.danger),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("Overwrite existing audio", style = BatchTtsUiScale.controlPrimary, color = LEColors.textPrimary)
                Text("Off by default. Replacement is limited to the selected fields and requires confirmation.", style = BatchTtsUiScale.controlSecondary, color = LEColors.textMuted)
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
                label = "English (Q/Ex)",
                value = "${scopeScan.englishTargetsCount}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Vietnamese (A/Tr)",
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
        Text("Voice Strategy & Preview", style = BatchTtsUiScale.sectionHeading)

        if (isLoadingVoices) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(LESpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LEColors.primary)
                Text("Loading available Edge TTS voices...", style = BatchTtsUiScale.body, color = LEColors.textMuted)
            }
        } else {
            val englishManaged = listOf(TtsField.QUESTION, TtsField.EXAMPLE).filter { it in selectedFields }
            val vietnameseManaged = listOf(TtsField.ANSWER, TtsField.TRANSLATION).filter { it in selectedFields }

            if (languageRequirements.requiresEnglish) VoiceStrategyCard(
                title = "English Voice Configuration",
                languageLabel = "English",
                managedFields = englishManaged,
                scopeScan = scopeScan,
                currentVoice = selectedEnglishVoice,
                candidateVoices = availableVoices.filter { it.isEnglish },
                filterState = englishFilterState,
                previewingVoiceId = previewingVoiceId,
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
                onPreviewCandidateVoice = onPreviewCandidateVoice,
                onStop = onStopPreview
            )

            if (languageRequirements.requiresVietnamese) VoiceStrategyCard(
                title = "Vietnamese Voice Configuration",
                languageLabel = "Vietnamese",
                managedFields = vietnameseManaged,
                scopeScan = scopeScan,
                currentVoice = selectedVietnameseVoice,
                candidateVoices = availableVoices.filter { it.isVietnamese },
                filterState = vietnameseFilterState,
                previewingVoiceId = previewingVoiceId,
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
                onPreviewCandidateVoice = onPreviewCandidateVoice,
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
        color = LEColors.surface,
        shape = LERadius.xs,
        border = BorderStroke(1.dp, if (checked) LEColors.primary else LEColors.borderSubtle),
        modifier = modifier
            .clip(LERadius.xs)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = LEColors.primary),
                    modifier = Modifier.size(20.dp)
                )
                Text(label, style = BatchTtsUiScale.controlPrimary, color = LEColors.textPrimary)
            }
            if (missingCount > 0) {
                Surface(
                    color = LEColors.primarySoft,
                    shape = LERadius.xs
                ) {
                    Text(
                        text = "$missingCount",
                        style = BatchTtsUiScale.controlSecondary,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "0",
                    style = BatchTtsUiScale.controlSecondary,
                    color = LEColors.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        color = LEColors.surfaceElevated,
        shape = LERadius.sm,
        border = BorderStroke(1.dp, if (highlight) LEColors.primary.copy(alpha = 0.3f) else LEColors.borderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(BatchTtsUiScale.cardPadding)) {
            Text(label, style = BatchTtsUiScale.metricLabel, color = if (highlight) LEColors.primary else if (isMuted) LEColors.textMuted else LEColors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = BatchTtsUiScale.metricValue, color = if (highlight) LEColors.primary else if (isMuted) LEColors.textMuted else LEColors.textPrimary)
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
    filterState: VoicePickerFilterState,
    previewingVoiceId: String?,
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
    onPreviewCandidateVoice: (voice: TtsVoice, text: String, rate: Int, pitchHz: Int, volumePercent: Int) -> Unit,
    onStop: () -> Unit
) {
    var showPrimaryVoicePicker by remember { mutableStateOf(false) }
    var selectedSampleField by remember(managedFields) { mutableStateOf(managedFields.firstOrNull()) }
    val effectiveSampleField = selectedSampleField?.takeIf { it in managedFields } ?: managedFields.firstOrNull()

    // Multi-target preview samples for the currently selected field
    val allSamples = remember(scopeScan, effectiveSampleField) {
        effectiveSampleField?.let { scopeScan.samplesFor(it) } ?: emptyList()
    }
    var sampleIndex by remember(effectiveSampleField) { mutableStateOf(0) }
    val clampedIndex = if (allSamples.isNotEmpty()) sampleIndex.coerceIn(0, allSamples.lastIndex) else 0
    val currentSample = allSamples.getOrNull(clampedIndex)

    val activeSampleText = currentSample?.text?.takeIf { it.isNotBlank() }
        ?: if (languageLabel.contains("English", ignoreCase = true) || languageLabel.contains("Anh", ignoreCase = true)) {
            "Hello! This is a preview of the selected text to speech voice."
        } else {
            "Xin chào! Đây là âm thanh nghe thử của giọng đọc được chọn."
        }

    Surface(
        color = LEColors.surfaceElevated,
        shape = LERadius.sm,
        border = BorderStroke(1.dp, LEColors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(BatchTtsUiScale.cardPadding), verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = BatchTtsUiScale.subsectionHeading, color = LEColors.textPrimary)
                Surface(
                    color = LEColors.primarySoft,
                    shape = LERadius.xs
                ) {
                    Text(
                        text = languageLabel,
                        style = BatchTtsUiScale.controlSecondary,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Primary Voice
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Primary Voice", style = BatchTtsUiScale.controlSecondary, fontWeight = FontWeight.Bold, color = LEColors.textSecondary)
                VoicePickerAnchor(
                    currentVoice = currentVoice,
                    onClick = { showPrimaryVoicePicker = true }
                )
                if (showPrimaryVoicePicker) {
                    SearchableVoicePickerDialog(
                        title = "Chọn giọng đọc chính ($languageLabel)",
                        currentVoice = currentVoice,
                        candidateVoices = candidateVoices,
                        filterState = filterState,
                        previewingVoiceId = previewingVoiceId,
                        onPreviewVoice = { voice ->
                            onPreviewCandidateVoice(voice, activeSampleText, currentRate, currentPitchHz, currentVolumePercent)
                        },
                        onStopPreview = onStop,
                        onSelectVoice = onVoiceSelect,
                        onDismiss = {
                            onStop()
                            showPrimaryVoicePicker = false
                        }
                    )
                }
            }

            // Fallback Voices with Execution Order
            FallbackVoiceEditor(
                languageLabel = languageLabel,
                primaryVoice = currentVoice,
                fallbackVoices = fallbackVoices,
                catalog = candidateVoices,
                filterState = filterState,
                previewingVoiceId = previewingVoiceId,
                activeSampleText = activeSampleText,
                currentRate = currentRate,
                currentPitchHz = currentPitchHz,
                currentVolumePercent = currentVolumePercent,
                onPreviewCandidateVoice = onPreviewCandidateVoice,
                onStop = onStop,
                onChange = onFallbackVoicesChange
            )

            // Numeric Audio Controls (Speed %, Pitch Hz, Volume %)
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

            // Multi-Target Preview Sentence Card
            Surface(
                color = LEColors.surface,
                shape = LERadius.xs,
                border = BorderStroke(1.dp, LEColors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(BatchTtsUiScale.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview Toolbar: Field Chips + [◀] Sample X / N [▶] + Preview Playback Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Field Chips or Label
                        if (managedFields.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                managedFields.forEach { field ->
                                    val isSelected = field == effectiveSampleField
                                    Surface(
                                        color = if (isSelected) LEColors.primarySoft else LEColors.surfaceElevated,
                                        shape = LERadius.xs,
                                        border = BorderStroke(1.dp, if (isSelected) LEColors.primary else LEColors.borderSubtle),
                                        modifier = Modifier.clickable { selectedSampleField = field }
                                    ) {
                                        Text(
                                            text = field.displayName,
                                            style = BatchTtsUiScale.controlSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) LEColors.primary else LEColors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else if (managedFields.isNotEmpty()) {
                            Text(
                                text = "${managedFields.first().displayName} Sample",
                                style = BatchTtsUiScale.controlPrimary,
                                color = LEColors.textSecondary
                            )
                        } else {
                            Text(
                                text = "No field selected",
                                style = BatchTtsUiScale.controlSecondary,
                                color = LEColors.textMuted
                            )
                        }

                        // Target Navigator and Preview Playback Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                        ) {
                            if (allSamples.size > 1) {
                                LESecondaryButton(
                                    text = "◀",
                                    onClick = { if (clampedIndex > 0) sampleIndex = clampedIndex - 1 },
                                    enabled = clampedIndex > 0,
                                    modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                                )
                                Text(
                                    text = "Sample ${clampedIndex + 1} / ${allSamples.size}",
                                    style = BatchTtsUiScale.badge,
                                    color = LEColors.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                LESecondaryButton(
                                    text = "▶",
                                    onClick = { if (clampedIndex < allSamples.lastIndex) sampleIndex = clampedIndex + 1 },
                                    enabled = clampedIndex < allSamples.lastIndex,
                                    modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                                )
                                Spacer(modifier = Modifier.width(LESpacing.xs))
                            } else if (allSamples.size == 1) {
                                Text(
                                    text = "Sample 1 / 1",
                                    style = BatchTtsUiScale.badge,
                                    color = LEColors.textMuted,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(LESpacing.xs))
                            }

                            if (isPreviewing) {
                                LEDangerButton(
                                    text = "■ Stop",
                                    onClick = onStop,
                                    icon = LEIcons.Stop,
                                    modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                                )
                            } else {
                                LESecondaryButton(
                                    text = "▶ Preview",
                                    onClick = { currentSample?.text?.let(onPreview) },
                                    icon = LEIcons.Audio,
                                    enabled = currentVoice != null && currentSample != null && currentSample.text.isNotBlank(),
                                    modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                                )
                            }
                        }
                    }

                    // Sample Text with Prominent Readability
                    if (currentSample != null) {
                        Text(
                            text = "\"${currentSample.text}\"",
                            style = BatchTtsUiScale.previewText,
                            color = LEColors.textPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Source: ${currentSample.displaySource} · Field: ${effectiveSampleField?.displayName ?: ""}",
                            style = BatchTtsUiScale.previewSource,
                            color = LEColors.textMuted
                        )
                    } else {
                        val fieldLabel = effectiveSampleField?.displayName ?: "selected field"
                        Text(
                            text = "No sample text available for $fieldLabel in current scope",
                            style = BatchTtsUiScale.previewSource,
                            color = LEColors.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FallbackVoiceEditor(
    languageLabel: String,
    primaryVoice: TtsVoice?,
    fallbackVoices: OrderedFallbackVoices,
    catalog: List<TtsVoice>,
    filterState: VoicePickerFilterState,
    previewingVoiceId: String?,
    activeSampleText: String,
    currentRate: Int,
    currentPitchHz: Int,
    currentVolumePercent: Int,
    onPreviewCandidateVoice: (voice: TtsVoice, text: String, rate: Int, pitchHz: Int, volumePercent: Int) -> Unit,
    onStop: () -> Unit,
    onChange: (OrderedFallbackVoices) -> Unit
) {
    var showAddFallbackPicker by remember { mutableStateOf(false) }
    val available = catalog.filter { candidate ->
        candidate.id != primaryVoice?.id && candidate.id !in fallbackVoices.ids
    }
    val canAdd = fallbackVoices.voices.size < OrderedFallbackVoices.MAX_FALLBACKS && available.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Fallback Voices — execution order",
                        style = BatchTtsUiScale.controlSecondary,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.textSecondary
                    )
                    Surface(
                        color = if (fallbackVoices.voices.size >= OrderedFallbackVoices.MAX_FALLBACKS) LEColors.primarySoft else LEColors.surfaceElevated,
                        shape = LERadius.xs
                    ) {
                        Text(
                            text = "${fallbackVoices.voices.size} / ${OrderedFallbackVoices.MAX_FALLBACKS}",
                            style = BatchTtsUiScale.controlSecondary,
                            fontWeight = FontWeight.Bold,
                            color = if (fallbackVoices.voices.size >= OrderedFallbackVoices.MAX_FALLBACKS) LEColors.primary else LEColors.textMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (fallbackVoices.voices.size >= OrderedFallbackVoices.MAX_FALLBACKS) {
                    Text(
                        text = "Maximum 3 fallback voices · 3 selected",
                        style = BatchTtsUiScale.previewSource,
                        fontWeight = FontWeight.Medium,
                        color = LEColors.primary
                    )
                } else if (available.isEmpty() && fallbackVoices.voices.isNotEmpty()) {
                    Text(
                        text = "No additional compatible voices available · ${fallbackVoices.voices.size} selected",
                        style = BatchTtsUiScale.previewSource,
                        color = LEColors.textMuted
                    )
                } else if (available.isEmpty() && fallbackVoices.voices.isEmpty()) {
                    Text(
                        text = "No additional compatible voices available",
                        style = BatchTtsUiScale.previewSource,
                        color = LEColors.textMuted
                    )
                } else {
                    Text(
                        text = "Maximum 3 fallback voices · ${fallbackVoices.voices.size} selected",
                        style = BatchTtsUiScale.previewSource,
                        color = LEColors.textMuted
                    )
                }
            }

            Box {
                LESecondaryButton(
                    text = "+ Add fallback voice",
                    onClick = { showAddFallbackPicker = true },
                    enabled = canAdd,
                    modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                )
                if (showAddFallbackPicker) {
                    SearchableVoicePickerDialog(
                        title = "Thêm giọng đọc dự phòng ($languageLabel)",
                        currentVoice = null,
                        candidateVoices = available,
                        filterState = filterState,
                        previewingVoiceId = previewingVoiceId,
                        onPreviewVoice = { voice ->
                            onPreviewCandidateVoice(voice, activeSampleText, currentRate, currentPitchHz, currentVolumePercent)
                        },
                        onStopPreview = onStop,
                        onSelectVoice = { voice ->
                            onChange(fallbackVoices.add(voice, primaryVoice))
                            showAddFallbackPicker = false
                        },
                        onDismiss = {
                            onStop()
                            showAddFallbackPicker = false
                        }
                    )
                }
            }
        }

        if (fallbackVoices.voices.isEmpty()) {
            Surface(
                color = LEColors.surface,
                shape = LERadius.xs,
                border = BorderStroke(1.dp, LEColors.borderSubtle.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No fallback voices configured (Single voice strategy)",
                    style = BatchTtsUiScale.controlSecondary,
                    color = LEColors.textMuted,
                    modifier = Modifier.padding(horizontal = LESpacing.md, vertical = 8.dp)
                )
            }
        } else {
            fallbackVoices.voices.forEachIndexed { index, voice ->
                Surface(
                    color = LEColors.surface,
                    shape = LERadius.xs,
                    border = BorderStroke(1.dp, LEColors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = LESpacing.md, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = BatchTtsUiScale.controlPrimary,
                            fontWeight = FontWeight.Bold,
                            color = LEColors.primary
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(
                                text = voice.displayName,
                                style = BatchTtsUiScale.controlPrimary,
                                color = LEColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${SearchableVoicePickerHelper.formatRegionDisplayName(voice.locale)} · ${voice.gender ?: "Neutral"}",
                                style = BatchTtsUiScale.controlSecondary,
                                color = LEColors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        LESecondaryButton(
                            text = "↑",
                            onClick = { onChange(fallbackVoices.move(voice.id, -1)) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        )
                        LESecondaryButton(
                            text = "↓",
                            onClick = { onChange(fallbackVoices.move(voice.id, 1)) },
                            enabled = index < fallbackVoices.voices.lastIndex,
                            modifier = Modifier.size(32.dp)
                        )
                        LEDangerButton(
                            text = "Remove",
                            onClick = { onChange(fallbackVoices.remove(voice.id)) },
                            modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningStepContent(
    summary: BatchTtsSummary,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    val progress = summary.progressPercent
    val elapsedSeconds = summary.elapsedMillis / 1000
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val elapsedStr = "%02d:%02d".format(minutes, seconds)

    val etaStr = summary.estimatedRemainingMillis?.let { etaMs ->
        val etaSec = etaMs / 1000
        if (etaSec < 60) "~${etaSec}s" else "~${etaSec / 60}m ${etaSec % 60}s"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(LESpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BatchTtsUiScale.sectionSpacing)
    ) {
        // Status operation banner
        Text(
            text = if (isCancelling || summary.isCancelled) "Đang hủy và dọn dẹp phiên tạo audio..." else (summary.currentOperation ?: "Đang tạo audio..."),
            style = BatchTtsUiScale.sectionHeading,
            color = if (isCancelling || summary.isCancelled) LEColors.warning else LEColors.primary
        )

        // Progress Numbers & Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${summary.completedJobs} / ${summary.totalJobs}",
                style = BatchTtsUiScale.metricValueLarge,
                color = LEColors.textPrimary
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = BatchTtsUiScale.sectionHeading,
                color = LEColors.textSecondary
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(LERadius.xs),
            color = if (isCancelling || summary.isCancelled) LEColors.warning else LEColors.primary,
            trackColor = LEColors.borderSubtle
        )

        // Elapsed time, ETA & Throughput
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thời gian: $elapsedStr",
                style = BatchTtsUiScale.body,
                color = LEColors.textSecondary
            )
            if (etaStr != null) {
                Text(
                    text = "Ước tính còn: $etaStr",
                    style = BatchTtsUiScale.body,
                    color = LEColors.primary
                )
            }
            if (summary.targetsPerSecond > 0.0) {
                Text(
                    text = "Tốc độ: %.1f mục/giây".format(summary.targetsPerSecond),
                    style = BatchTtsUiScale.body,
                    color = LEColors.textMuted
                )
            }
        }

        // Live Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            MetricCard(
                label = "Thành công",
                value = "${summary.successCount}",
                highlight = true,
                modifier = Modifier.weight(1f)
            )
            if (summary.fallbackRecoveredCount > 0) {
                MetricCard(
                    label = "Dự phòng",
                    value = "${summary.fallbackRecoveredCount}",
                    modifier = Modifier.weight(1f)
                )
            }
            MetricCard(
                label = "Bỏ qua",
                value = "${summary.skippedCount}",
                isMuted = true,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Thất bại",
                value = "${summary.failedCount}",
                isMuted = summary.failedCount == 0,
                modifier = Modifier.weight(1f)
            )
            if (summary.cancelledCount > 0) {
                MetricCard(
                    label = "Đã hủy",
                    value = "${summary.cancelledCount}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Current item with Runtime Voice Transparency
        summary.currentJob?.let { job ->
            Surface(
                color = LEColors.surfaceElevated,
                shape = LERadius.sm,
                border = BorderStroke(1.dp, LEColors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(BatchTtsUiScale.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mục đang xử lý: [${job.field.displayName}]",
                            style = BatchTtsUiScale.controlPrimary,
                            color = LEColors.primary
                        )
                        val candidateBadge = if (summary.isCurrentCandidateFallback) {
                            "Giọng dự phòng ${summary.currentCandidateIndex - 1}/${summary.totalCandidatesForCurrentJob - 1}"
                        } else {
                            "Giọng chính"
                        }
                        Surface(
                            color = if (summary.isCurrentCandidateFallback) LEColors.warningContainer else LEColors.primarySoft,
                            shape = LERadius.xs
                        ) {
                            Text(
                                text = candidateBadge,
                                style = BatchTtsUiScale.controlSecondary,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.isCurrentCandidateFallback) LEColors.warning else LEColors.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "\"${job.text}\"",
                        style = BatchTtsUiScale.previewText,
                        color = LEColors.textPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    HorizontalDivider(color = LEColors.borderSubtle.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Giọng chính cấu hình: ${job.requestedVoice.displayName}",
                            style = BatchTtsUiScale.controlSecondary,
                            color = LEColors.textMuted
                        )
                        Text(
                            text = "Giọng đang chạy: ${summary.currentVoiceName ?: job.requestedVoice.displayName}",
                            style = BatchTtsUiScale.controlSecondary,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.isCurrentCandidateFallback) LEColors.warning else LEColors.textSecondary
                        )
                    }
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
        modifier = Modifier.fillMaxSize().padding(LESpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BatchTtsUiScale.sectionSpacing)
    ) {
        // Status header
        val headerTitle = when {
            summary.isCancelled -> "⚠ Đã hủy tiến trình tạo audio"
            summary.hasFailures -> "⚠ Hoàn tất tạo audio có mục lỗi"
            else -> "✓ Hoàn tất tạo audio hàng loạt"
        }
        val headerColor = when {
            summary.isCancelled || summary.hasFailures -> LEColors.warning
            else -> LEColors.success
        }

        Text(
            text = headerTitle,
            style = BatchTtsUiScale.dialogTitle,
            color = headerColor
        )

        // Statistics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            MetricCard(
                label = "Tổng số mục",
                value = "${summary.totalJobs}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Thành công",
                value = "${summary.successCount}",
                highlight = summary.successCount > 0,
                modifier = Modifier.weight(1f)
            )
            if (summary.fallbackRecoveredCount > 0) {
                MetricCard(
                    label = "Khôi phục dự phòng",
                    value = "${summary.fallbackRecoveredCount}",
                    modifier = Modifier.weight(1f)
                )
            }
            MetricCard(
                label = "Thất bại",
                value = "${summary.failedCount}",
                isMuted = summary.failedCount == 0,
                modifier = Modifier.weight(1f)
            )
            if (summary.cancelledCount > 0) {
                MetricCard(
                    label = "Đã hủy",
                    value = "${summary.cancelledCount}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (summary.hasFailures) {
            Text("Danh sách mục lỗi (${summary.failedCount})", style = BatchTtsUiScale.sectionHeading, color = LEColors.danger)

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
                border = BorderStroke(1.dp, LEColors.borderSubtle),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.padding(BatchTtsUiScale.cardPadding), verticalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                    val summaryMessage = when {
                        summary.isCancelled ->
                            "Đã tạo và lưu thành công ${summary.successCount} mục audio trước khi hủy. Còn lại ${summary.cancelledCount} mục đã bị hủy."
                        else ->
                            "Đã tạo thành công tất cả ${summary.successCount} mục audio vào bộ nhớ lưu trữ."
                    }

                    Text(
                        text = summaryMessage,
                        style = BatchTtsUiScale.controlPrimary,
                        color = if (summary.isCancelled) LEColors.warning else LEColors.success
                    )

                    if (summary.fallbackRecoveredCount > 0) {
                        Text(
                            text = "${summary.fallbackRecoveredCount} mục đã được tạo thành công nhờ chuyển sang các giọng dự phòng trong chuỗi.",
                            style = BatchTtsUiScale.body,
                            color = LEColors.primary
                        )
                    }

                    if (summary.successCount > 0) {
                        Text(
                            text = "Nhấn 'Apply (${summary.successCount} Targets)' để liên kết các file audio đã tạo vào bài học của bạn. Thao tác này có thể Undo bất cứ lúc nào trong Content Studio.",
                            style = BatchTtsUiScale.body,
                            color = LEColors.textSecondary
                        )
                    }
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
            Text(label, style = BatchTtsUiScale.controlSecondary, color = LEColors.textMuted)
            Text(value, style = BatchTtsUiScale.controlSecondary, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun FailedJobRow(result: BatchTtsJobResult) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(BatchTtsUiScale.cardPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[${result.job.field.displayName}] ${result.job.text}",
                style = BatchTtsUiScale.controlPrimary,
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
                    style = BatchTtsUiScale.controlSecondary,
                    color = LEColors.danger,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        result.errorMessage?.let { msg ->
            Text(
                text = msg,
                style = BatchTtsUiScale.controlSecondary,
                color = LEColors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (result.attempts.isNotEmpty()) {
            Text(
                text = "Attempts (${result.attempts.size}): " + result.attempts.joinToString { "${it.voice.displayName} (${it.errorCategory?.displayLabel ?: "Failed"})" },
                style = BatchTtsUiScale.previewSource,
                color = LEColors.textMuted
            )
        }
    }
}

@Composable
private fun ApplyingStepContent(
    completedCount: Int,
    totalCount: Int,
    failedCount: Int,
    startTimeMillis: Long
) {
    val progress = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    val elapsedMillis = (System.currentTimeMillis() - startTimeMillis).coerceAtLeast(0L)
    val elapsedSeconds = elapsedMillis / 1000
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize().padding(LESpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(64.dp),
            color = LEColors.primary,
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(LESpacing.lg))
        Text(
            text = "Đang áp dụng audio đã tạo vào bài học...",
            style = BatchTtsUiScale.dialogTitle,
            color = LEColors.textPrimary
        )
        Spacer(modifier = Modifier.height(LESpacing.xs))
        Text(
            text = "$completedCount / $totalCount mục (${(progress * 100).toInt()}%)",
            style = BatchTtsUiScale.sectionHeading,
            color = LEColors.textSecondary
        )
        Spacer(modifier = Modifier.height(LESpacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(0.6f).height(10.dp).clip(LERadius.sm),
            color = LEColors.primary,
            trackColor = LEColors.surfaceSubtle
        )
        Spacer(modifier = Modifier.height(LESpacing.md))
        Row(
            horizontalArrangement = Arrangement.spacedBy(LESpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thời gian: $timeFormatted",
                style = BatchTtsUiScale.body,
                color = LEColors.textSecondary
            )
            Text(
                text = "Đã áp dụng: $completedCount",
                style = BatchTtsUiScale.body,
                color = LEColors.success
            )
            if (failedCount > 0) {
                Text(
                    text = "Lỗi: $failedCount",
                    style = BatchTtsUiScale.body,
                    color = LEColors.danger
                )
            }
        }
        Spacer(modifier = Modifier.height(LESpacing.lg))
        Text(
            text = "Vui lòng giữ ứng dụng mở trong quá trình lưu dữ liệu để đảm bảo an toàn.",
            style = BatchTtsUiScale.controlSecondary,
            color = LEColors.textMuted
        )
    }
}

@Composable
private fun ApplyCompletedStepContent(
    appliedCount: Int,
    failedCount: Int,
    totalCount: Int,
    packageName: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(LESpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (failedCount == 0) LEColors.successContainer else LEColors.warningContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (failedCount == 0) LEIcons.Success else LEIcons.Warning,
                contentDescription = null,
                tint = if (failedCount == 0) LEColors.success else LEColors.warning,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(LESpacing.lg))
        Text(
            text = if (failedCount == 0) "Áp dụng audio thành công!" else "Áp dụng audio hoàn tất với cảnh báo",
            style = BatchTtsUiScale.dialogTitle,
            color = LEColors.textPrimary
        )
        Spacer(modifier = Modifier.height(LESpacing.xs))
        Text(
            text = "Đã lưu $appliedCount / $totalCount mục audio vào gói nội dung '$packageName'.",
            style = BatchTtsUiScale.sectionHeading,
            color = LEColors.textSecondary
        )
        if (failedCount > 0) {
            Spacer(modifier = Modifier.height(LESpacing.xs))
            Text(
                text = "Không thể áp dụng $failedCount mục. Bạn có thể kiểm tra lại dữ liệu bài học.",
                style = BatchTtsUiScale.controlSecondary,
                color = LEColors.danger
            )
        }
        Spacer(modifier = Modifier.height(LESpacing.md))
        Surface(
            color = LEColors.surfaceElevated,
            shape = LERadius.sm,
            modifier = Modifier.fillMaxWidth(0.7f).padding(LESpacing.sm),
            border = BorderStroke(1.dp, LEColors.borderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(LESpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = LEIcons.Help,
                    contentDescription = null,
                    tint = LEColors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(LESpacing.sm))
                Text(
                    text = "Bạn có thể sử dụng nút 'Undo TTS' trong Content Studio bất cứ lúc nào nếu cần hoàn tác.",
                    style = BatchTtsUiScale.controlSecondary,
                    color = LEColors.textSecondary
                )
            }
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
                        style = BatchTtsUiScale.controlSecondary,
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
                                .height(34.dp)
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
                                        style = BatchTtsUiScale.controlPrimary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isDirty) {
                                        Text(
                                            text = "(Modified)",
                                            style = BatchTtsUiScale.controlSecondary,
                                            color = LEColors.warning,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text("▾", style = BatchTtsUiScale.controlSecondary, color = LEColors.textMuted)
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
                                                style = BatchTtsUiScale.controlPrimary,
                                                fontWeight = if (preset.id == selectedPreset.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (preset.id == selectedPreset.id) LEColors.primary else LEColors.textPrimary
                                            )
                                            if (preset.isBuiltIn) {
                                                Text(" (Default)", style = BatchTtsUiScale.controlSecondary, color = LEColors.textMuted)
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
                        modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                    )

                    LESecondaryButton(
                        text = "Save As...",
                        onClick = onSaveAsPreset,
                        modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                    )

                    LESecondaryButton(
                        text = "Duplicate",
                        onClick = onDuplicatePreset,
                        modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                    )

                    if (!selectedPreset.isBuiltIn) {
                        LESecondaryButton(
                            text = "Rename",
                            onClick = onRenamePreset,
                            modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                        )

                        LEDangerButton(
                            text = "Delete",
                            onClick = onDeletePreset,
                            modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                        )
                    }
                }

                LESecondaryButton(
                    text = "Reset Defaults",
                    onClick = onResetDefaults,
                    modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = "⚠ $errorMessage",
                    style = BatchTtsUiScale.previewSource,
                    color = LEColors.danger,
                    modifier = Modifier.padding(start = 4.dp)
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
