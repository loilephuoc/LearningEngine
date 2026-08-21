package vn.loi.learning.desktop.tts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.batch.BatchTtsJob
import vn.loi.learning.desktop.tts.batch.BatchTtsJobResult
import vn.loi.learning.desktop.tts.batch.BatchTtsJobStatus
import vn.loi.learning.desktop.tts.batch.BatchTtsRunner
import vn.loi.learning.desktop.tts.batch.BatchTtsScanner
import vn.loi.learning.desktop.tts.batch.BatchTtsSummary
import vn.loi.learning.desktop.tts.batch.BatchTtsTarget
import vn.loi.learning.desktop.tts.profile.TtsVoiceProfiles
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

/**
 * Step/Mode of the Batch TTS Dialog.
 */
enum class BatchTtsDialogStep {
    CONFIRMATION,
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
    initialProfiles: TtsVoiceProfiles = TtsVoiceProfiles(),
    onApply: (contentId: String, field: TtsField, audioRef: String) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(BatchTtsDialogStep.CONFIRMATION) }
    var availableVoices by remember { mutableStateOf<List<TtsVoice>>(emptyList()) }
    var isLoadingVoices by remember { mutableStateOf(true) }

    // Scanned targets
    val scannedTargets = remember(itemsToScan, targetField) {
        BatchTtsScanner.scanTargets(itemsToScan, targetField, missingOnly = true)
    }

    val englishTargetsCount = remember(scannedTargets) {
        scannedTargets.count { it.language == TtsLanguage.ENGLISH }
    }
    val vietnameseTargetsCount = remember(scannedTargets) {
        scannedTargets.count { it.language == TtsLanguage.VIETNAMESE }
    }

    // Selected voices
    var selectedEnglishVoice by remember { mutableStateOf<TtsVoice?>(null) }
    var selectedVietnameseVoice by remember { mutableStateOf<TtsVoice?>(null) }

    // Batch execution state
    var summary by remember { mutableStateOf(BatchTtsSummary.initial(scannedTargets.size)) }
    val runner = remember(ttsService) { BatchTtsRunner(ttsService, coroutineScope) }

    // Load voices
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
            // Error handling fallback
        } finally {
            isLoadingVoices = false
        }
    }

    fun startBatch(jobsToRun: List<BatchTtsJob>) {
        if (jobsToRun.isEmpty()) return
        currentStep = BatchTtsDialogStep.RUNNING
        summary = BatchTtsSummary.initial(jobsToRun.size)

        runner.runBatch(
            jobs = jobsToRun,
            packageName = packageName,
            onApply = onApply,
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
            targets = scannedTargets,
            englishVoice = enVoice,
            vietnameseVoice = viVoice
        )
        startBatch(jobs)
    }

    fun handleRetryFailed() {
        val failedJobs = summary.failedResults.map { it.job }
        if (failedJobs.isNotEmpty()) {
            startBatch(failedJobs)
        }
    }

    Dialog(
        onDismissRequest = {
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
                    .width(620.dp)
                    .heightIn(min = 360.dp, max = 680.dp)
                    .clip(LERadius.md)
                    .animateContentSize(),
                color = LEColors.surface,
                shape = LERadius.md,
                tonalElevation = LEElevation.popup,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // DIALOG HEADER
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
                                text = "Package: $packageName · ${itemsToScan.size} items",
                                style = LETypography.secondaryMetadata,
                                color = LEColors.textMuted
                            )
                        }

                        if (currentStep != BatchTtsDialogStep.RUNNING) {
                            LESecondaryButton(
                                text = "✕",
                                onClick = onDismiss
                            )
                        }
                    }

                    HorizontalDivider(color = LEColors.borderSubtle)

                    // DIALOG CONTENT BASED ON STEP
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(LESpacing.lg)) {
                        when (currentStep) {
                            BatchTtsDialogStep.CONFIRMATION -> {
                                ConfirmationStepContent(
                                    scannedTargets = scannedTargets,
                                    englishCount = englishTargetsCount,
                                    vietnameseCount = vietnameseTargetsCount,
                                    isLoadingVoices = isLoadingVoices,
                                    availableVoices = availableVoices,
                                    selectedEnglishVoice = selectedEnglishVoice,
                                    selectedVietnameseVoice = selectedVietnameseVoice,
                                    onEnglishVoiceChange = { selectedEnglishVoice = it },
                                    onVietnameseVoiceChange = { selectedVietnameseVoice = it }
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
                                    onRetryFailed = { handleRetryFailed() }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = LEColors.borderSubtle)

                    // DIALOG FOOTER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (currentStep) {
                            BatchTtsDialogStep.CONFIRMATION -> {
                                LESecondaryButton(
                                    text = "Cancel",
                                    onClick = onDismiss
                                )
                                Spacer(modifier = Modifier.width(LESpacing.sm))
                                LEPrimaryButton(
                                    text = if (scannedTargets.isEmpty()) "No Missing Targets" else "Generate (${scannedTargets.size})",
                                    onClick = { handleStartInitialBatch() },
                                    enabled = scannedTargets.isNotEmpty() && !isLoadingVoices && selectedEnglishVoice != null && selectedVietnameseVoice != null,
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
                                    LEPrimaryButton(
                                        text = "🔁 Retry Failed (${summary.failedCount})",
                                        onClick = { handleRetryFailed() }
                                    )
                                    Spacer(modifier = Modifier.width(LESpacing.sm))
                                }
                                LESecondaryButton(
                                    text = "Close",
                                    onClick = onDismiss
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
private fun ConfirmationStepContent(
    scannedTargets: List<BatchTtsTarget>,
    englishCount: Int,
    vietnameseCount: Int,
    isLoadingVoices: Boolean,
    availableVoices: List<TtsVoice>,
    selectedEnglishVoice: TtsVoice?,
    selectedVietnameseVoice: TtsVoice?,
    onEnglishVoiceChange: (TtsVoice) -> Unit,
    onVietnameseVoiceChange: (TtsVoice) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            Surface(
                color = LEColors.primarySoft,
                shape = LERadius.sm,
                border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.primary.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(LESpacing.md)) {
                    Text("Total Missing Targets", style = LETypography.caption, color = LEColors.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${scannedTargets.size}", style = LETypography.paneTitle, fontWeight = FontWeight.Bold, color = LEColors.primary)
                }
            }

            Surface(
                color = LEColors.surfaceElevated,
                shape = LERadius.sm,
                border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(LESpacing.md)) {
                    Text("English Targets (Q/A/Ex)", style = LETypography.caption, color = LEColors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$englishCount", style = LETypography.paneTitle, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                }
            }

            Surface(
                color = LEColors.surfaceElevated,
                shape = LERadius.sm,
                border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(LESpacing.md)) {
                    Text("Vietnamese Targets (Tr)", style = LETypography.caption, color = LEColors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$vietnameseCount", style = LETypography.paneTitle, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                }
            }
        }

        if (scannedTargets.isEmpty()) {
            Surface(
                color = LEColors.surfaceElevated,
                shape = LERadius.sm,
                modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.lg)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(LESpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✓ All audio is complete!", style = LETypography.fieldValueEmphasized, color = LEColors.success)
                    Text("No missing audio targets detected in the selected scope.", style = LETypography.caption, color = LEColors.textMuted)
                }
            }
        } else {
            Text("Voice Profiles Configuration", style = LETypography.sectionTitle)

            if (isLoadingVoices) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(LESpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LEColors.primary)
                    Text("Loading available voices...", style = LETypography.caption, color = LEColors.textMuted)
                }
            } else {
                // English Voice Dropdown
                VoiceSelectionRow(
                    label = "English Voice (Question, Answer, Example):",
                    currentVoice = selectedEnglishVoice,
                    candidateVoices = availableVoices.filter { it.language == "en" },
                    onSelect = onEnglishVoiceChange
                )

                // Vietnamese Voice Dropdown
                VoiceSelectionRow(
                    label = "Vietnamese Voice (Translation):",
                    currentVoice = selectedVietnameseVoice,
                    candidateVoices = availableVoices.filter { it.language == "vi" },
                    onSelect = onVietnameseVoiceChange
                )
            }
        }
    }
}

@Composable
private fun VoiceSelectionRow(
    label: String,
    currentVoice: TtsVoice?,
    candidateVoices: List<TtsVoice>,
    onSelect: (TtsVoice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = LETypography.caption, color = LEColors.textSecondary)

        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(LERadius.xs)
                    .clickable { expanded = true }
                    .border(1.dp, LEColors.borderSubtle, LERadius.xs),
                color = LEColors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentVoice?.let { "${it.displayName} (${it.locale}, ${it.gender})" } ?: "Select Voice",
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
                onDismissRequest = { expanded = false }
            ) {
                candidateVoices.forEach { voice ->
                    DropdownMenuItem(
                        text = {
                            Text("${voice.displayName} (${voice.locale}, ${voice.gender})", style = LETypography.caption)
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
            text = "Generating Audio",
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
                    Text("Currently Synthesizing:", style = LETypography.caption, color = LEColors.textMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "[${job.field.name}] \"${job.text}\"",
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
    onRetryFailed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        // Status header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            Text(
                text = when {
                    summary.isCancelled -> "⚠ Batch Cancelled"
                    summary.hasFailures -> "⚠ Batch Finished with Issues"
                    else -> "✓ Batch Completed Successfully"
                },
                style = LETypography.paneTitle,
                fontWeight = FontWeight.Bold,
                color = when {
                    summary.isCancelled -> LEColors.warning
                    summary.hasFailures -> LEColors.warning
                    else -> LEColors.success
                }
            )
        }

        // Statistics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            StatPill(label = "Total", value = "${summary.totalJobs}", color = LEColors.textPrimary)
            StatPill(label = "Success", value = "${summary.successCount}", color = LEColors.success)
            StatPill(label = "Skipped", value = "${summary.skippedCount}", color = LEColors.textMuted)
            StatPill(label = "Failed", value = "${summary.failedCount}", color = if (summary.failedCount > 0) LEColors.danger else LEColors.textMuted)
            if (summary.cancelledCount > 0) {
                StatPill(label = "Cancelled", value = "${summary.cancelledCount}", color = LEColors.warning)
            }
        }

        // Failure List (if any)
        if (summary.hasFailures) {
            Text("Failed Items (${summary.failedCount})", style = LETypography.sectionTitle, color = LEColors.danger)

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
                modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.md)
            ) {
                Column(modifier = Modifier.padding(LESpacing.md)) {
                    Text("All target items were generated and applied to the package.", style = LETypography.caption, color = LEColors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = LEColors.surfaceElevated,
        shape = LERadius.xs,
        border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle)
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
                text = "[${result.job.field.name}] ${result.job.text}",
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
