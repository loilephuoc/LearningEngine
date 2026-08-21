package vn.loi.learning.desktop.tts.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import java.nio.file.Path
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.batch.BatchTtsJob
import vn.loi.learning.desktop.tts.batch.BatchTtsJobResult
import vn.loi.learning.desktop.tts.batch.BatchTtsJobStatus
import vn.loi.learning.desktop.tts.batch.BatchTtsRunner
import vn.loi.learning.desktop.tts.batch.BatchTtsScanner
import vn.loi.learning.desktop.tts.batch.BatchTtsScopeScan
import vn.loi.learning.desktop.tts.batch.BatchTtsSummary
import vn.loi.learning.desktop.tts.profile.TtsVoiceProfiles
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
    audioPlayer: AudioPlayer? = null,
    initialProfiles: TtsVoiceProfiles = TtsVoiceProfiles(),
    onApplyBatch: (results: List<BatchTtsJobResult>) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localAudioPlayer = remember { audioPlayer ?: DesktopAudioPlayer() }

    var currentStep by remember { mutableStateOf(BatchTtsDialogStep.CONFIG) }
    var availableVoices by remember { mutableStateOf<List<TtsVoice>>(emptyList()) }
    var isLoadingVoices by remember { mutableStateOf(true) }

    // Selected Audio Fields (Default: all 4 fields or specific field)
    var selectedFields by remember(targetField) {
        mutableStateOf(if (targetField != null) setOf(targetField) else TtsField.entries.toSet())
    }

    // Scanned Scope analysis
    val scopeScan = remember(itemsToScan, selectedFields) {
        BatchTtsScanner.scanBatchScope(itemsToScan, selectedFields)
    }

    // Selected Voices and Rates
    var selectedEnglishVoice by remember { mutableStateOf<TtsVoice?>(null) }
    var selectedVietnameseVoice by remember { mutableStateOf<TtsVoice?>(null) }
    var englishRate by remember { mutableStateOf(initialProfiles.english.rate) }
    var vietnameseRate by remember { mutableStateOf(initialProfiles.vietnamese.rate) }

    // Voice Preview States
    var isPreviewingEn by remember { mutableStateOf(false) }
    var isPreviewingVi by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    // Batch execution state
    var summary by remember { mutableStateOf(BatchTtsSummary.initial(scopeScan.totalValidTargets)) }
    val runner = remember(ttsService) { BatchTtsRunner(ttsService, coroutineScope) }

    // Load available voices
    LaunchedEffect(Unit) {
        isLoadingVoices = true
        try {
            val voices = ttsService.listVoices()
            availableVoices = voices
            selectedEnglishVoice = initialProfiles.resolveVoice(TtsLanguage.ENGLISH, voices)
                ?: ttsService.defaultVoiceFor("en", voices)
            selectedVietnameseVoice = initialProfiles.resolveVoice(TtsLanguage.VIETNAMESE, voices)
                ?: ttsService.defaultVoiceFor("vi", voices)
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

    fun handlePreview(language: TtsLanguage) {
        stopAudio()
        val voice = if (language == TtsLanguage.ENGLISH) selectedEnglishVoice else selectedVietnameseVoice
        val rate = if (language == TtsLanguage.ENGLISH) englishRate else vietnameseRate
        val text = if (language == TtsLanguage.ENGLISH) scopeScan.representativeEnglishText else scopeScan.representativeVietnameseText

        if (voice == null || text.isNullOrBlank()) return

        if (language == TtsLanguage.ENGLISH) isPreviewingEn = true else isPreviewingVi = true

        previewJob = coroutineScope.launch {
            try {
                val previewPath = ttsService.preview(text, voice, rate)
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
            onApply = null, // Generate != Apply separation
            onProgress = { updatedSummary ->
                summary = updatedSummary
                if (updatedSummary.isFinished) {
                    currentStep = BatchTtsDialogStep.COMPLETED
                }
            }
        )
    }

    fun handleStartInitialBatch() {
        val enVoice = selectedEnglishVoice ?: return
        val viVoice = selectedVietnameseVoice ?: return
        val jobs = BatchTtsScanner.buildJobs(
            targets = scopeScan.validTargets,
            englishVoice = enVoice,
            vietnameseVoice = viVoice,
            englishRate = englishRate,
            vietnameseRate = vietnameseRate
        )
        startBatch(jobs)
    }

    fun handleRetryFailed() {
        val failedJobs = summary.failedResults.map { it.job }
        if (failedJobs.isNotEmpty()) {
            startBatch(failedJobs)
        }
    }

    fun handleApply() {
        stopAudio()
        val successfulResults = summary.successfulResults
        if (successfulResults.isNotEmpty()) {
            onApplyBatch(successfulResults)
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            stopAudio()
            if (currentStep == BatchTtsDialogStep.RUNNING) {
                runner.cancel()
            }
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .onKeyEvent { event ->
                    if (event.key == Key.Escape) {
                        stopAudio()
                        if (currentStep == BatchTtsDialogStep.RUNNING) {
                            runner.cancel()
                        }
                        onDismiss()
                        true
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(660.dp)
                    .heightIn(min = 400.dp, max = 720.dp)
                    .clip(LERadius.md)
                    .animateContentSize(),
                color = LEColors.surface,
                shape = LERadius.md,
                tonalElevation = LEElevation.popup,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, LEColors.borderSubtle)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = title,
                                style = LETypography.paneTitle,
                                color = LEColors.textPrimary
                            )
                            Text(
                                text = "Package: $packageName · Scope: ${itemsToScan.size} selected items",
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
                                    scopeScan = scopeScan,
                                    selectedFields = selectedFields,
                                    onToggleField = { field ->
                                        selectedFields = if (field in selectedFields) {
                                            selectedFields - field
                                        } else {
                                            selectedFields + field
                                        }
                                    },
                                    isLoadingVoices = isLoadingVoices,
                                    availableVoices = availableVoices,
                                    selectedEnglishVoice = selectedEnglishVoice,
                                    selectedVietnameseVoice = selectedVietnameseVoice,
                                    englishRate = englishRate,
                                    vietnameseRate = vietnameseRate,
                                    onEnglishVoiceChange = { selectedEnglishVoice = it },
                                    onVietnameseVoiceChange = { selectedVietnameseVoice = it },
                                    onEnglishRateChange = { englishRate = it },
                                    onVietnameseRateChange = { vietnameseRate = it },
                                    isPreviewingEn = isPreviewingEn,
                                    isPreviewingVi = isPreviewingVi,
                                    onPreviewEn = { handlePreview(TtsLanguage.ENGLISH) },
                                    onPreviewVi = { handlePreview(TtsLanguage.VIETNAMESE) },
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
                                    enabled = scopeScan.hasTargets && !isLoadingVoices && selectedEnglishVoice != null && selectedVietnameseVoice != null,
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
            }
        }
    }
}

@Composable
private fun ConfigStepContent(
    scopeScan: BatchTtsScopeScan,
    selectedFields: Set<TtsField>,
    onToggleField: (TtsField) -> Unit,
    isLoadingVoices: Boolean,
    availableVoices: List<TtsVoice>,
    selectedEnglishVoice: TtsVoice?,
    selectedVietnameseVoice: TtsVoice?,
    englishRate: Int,
    vietnameseRate: Int,
    onEnglishVoiceChange: (TtsVoice) -> Unit,
    onVietnameseVoiceChange: (TtsVoice) -> Unit,
    onEnglishRateChange: (Int) -> Unit,
    onVietnameseRateChange: (Int) -> Unit,
    isPreviewingEn: Boolean,
    isPreviewingVi: Boolean,
    onPreviewEn: () -> Unit,
    onPreviewVi: () -> Unit,
    onStopPreview: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        // Section 1: Audio Fields to Generate
        Text("Audio Fields to Generate", style = LETypography.sectionTitle)

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

        // Section 3: Voice & Rate Configurations with Preview
        Text("Voice Profiles & Preview", style = LETypography.sectionTitle)

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
            // English Voice & Rate Row with Preview
            VoiceConfigCard(
                title = "English Voice Configuration",
                languageLabel = "English (US preferred)",
                currentVoice = selectedEnglishVoice,
                candidateVoices = availableVoices.filter { it.language == "en" },
                onVoiceSelect = onEnglishVoiceChange,
                currentRate = englishRate,
                onRateChange = onEnglishRateChange,
                isPreviewing = isPreviewingEn,
                onPreview = onPreviewEn,
                onStop = onStopPreview
            )

            // Vietnamese Voice & Rate Row with Preview
            VoiceConfigCard(
                title = "Vietnamese Voice Configuration",
                languageLabel = "Vietnamese (vi-VN)",
                currentVoice = selectedVietnameseVoice,
                candidateVoices = availableVoices.filter { it.language == "vi" },
                onVoiceSelect = onVietnameseVoiceChange,
                currentRate = vietnameseRate,
                onRateChange = onVietnameseRateChange,
                isPreviewing = isPreviewingVi,
                onPreview = onPreviewVi,
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
private fun VoiceConfigCard(
    title: String,
    languageLabel: String,
    currentVoice: TtsVoice?,
    candidateVoices: List<TtsVoice>,
    onVoiceSelect: (TtsVoice) -> Unit,
    currentRate: Int,
    onRateChange: (Int) -> Unit,
    isPreviewing: Boolean,
    onPreview: () -> Unit,
    onStop: () -> Unit
) {
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
                Text(title, style = LETypography.caption, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                Text(languageLabel, style = LETypography.caption, color = LEColors.textMuted)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed Wide Voice Dropdown
                WideVoiceDropdown(
                    currentVoice = currentVoice,
                    candidateVoices = candidateVoices,
                    onSelect = onVoiceSelect,
                    modifier = Modifier.weight(0.6f)
                )

                // Speech Rate Selector
                RateDropdown(
                    currentRate = currentRate,
                    onSelectRate = onRateChange,
                    modifier = Modifier.weight(0.22f)
                )

                // Preview Button
                if (isPreviewing) {
                    LEDangerButton(
                        text = "■ Stop",
                        onClick = onStop,
                        icon = LEIcons.Stop,
                        modifier = Modifier.weight(0.18f)
                    )
                } else {
                    LESecondaryButton(
                        text = "Preview",
                        onClick = onPreview,
                        icon = LEIcons.Audio,
                        enabled = currentVoice != null,
                        modifier = Modifier.weight(0.18f)
                    )
                }
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

        // Bug Fix: Explicit min-width and max-height prevents narrow vertical column wrapping
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

    val rateOptions = listOf(
        -15 to "Slow (-15%)",
        0 to "Normal (0%)",
        15 to "Fast (+15%)",
        30 to "Very Fast (+30%)"
    )

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
    var playingRelativePath by remember { mutableStateOf<String?>(null) }

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
    }
}
