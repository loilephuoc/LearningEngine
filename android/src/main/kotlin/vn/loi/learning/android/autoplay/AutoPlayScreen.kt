package vn.loi.learning.android.autoplay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.loi.learning.android.R
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.ui.*

@Composable
fun AutoPlayScreen(
    viewModel: AutoPlayViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.refreshSourceCounts()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.onHostActivityStop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val itemCounts by viewModel.itemCounts.collectAsStateWithLifecycle()
    val availablePackages by viewModel.availablePackages.collectAsStateWithLifecycle()
    val isPackageAvailable by viewModel.isPackageAvailable.collectAsStateWithLifecycle()
    val packageTitle by viewModel.packageTitle.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val remainingSleepMillis by viewModel.remainingSleepMillis.collectAsStateWithLifecycle()

    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

    when (val state = engineState) {
        is AutoPlayEngineState.Idle -> {
            AutoPlayConfigScreen(
                config = config,
                availablePackages = availablePackages,
                isPackageAvailable = isPackageAvailable,
                itemCounts = itemCounts,
                packageTitle = packageTitle,
                onSelectPackage = viewModel::selectPackage,
                onSelectDirection = viewModel::selectDirection,
                onSelectSource = viewModel::selectSource,
                onSelectPlaybackOrder = viewModel::selectPlaybackOrder,
                onUpdateFrontDelayMs = viewModel::updateFrontDelayMs,
                onUpdatePlayFrontAudio = viewModel::updatePlayFrontAudio,
                onUpdatePlayAnswerAudio = viewModel::updatePlayAnswerAudio,
                onUpdatePostAnswerDelayMs = viewModel::updatePostAnswerDelayMs,
                onUpdatePlayExampleEnglishAudio = viewModel::updatePlayExampleEnglishAudio,
                onUpdatePostExampleEnglishDelayMs = viewModel::updatePostExampleEnglishDelayMs,
                onUpdatePlayExampleVietnameseAudio = viewModel::updatePlayExampleVietnameseAudio,
                onUpdatePostExampleVietnameseDelayMs = viewModel::updatePostExampleVietnameseDelayMs,
                onUpdateKeepScreenOn = viewModel::updateKeepScreenOn,
                onUpdateBackgroundPlayback = viewModel::updateBackgroundPlayback,
                onUpdateSleepTimerMinutes = viewModel::updateSleepTimerMinutes,
                onStart = viewModel::startAutoPlay,
                onBack = onBack,
                modifier = modifier
            )
        }
        is AutoPlayEngineState.Empty -> {
            AutoPlayConfigScreen(
                config = config,
                availablePackages = availablePackages,
                isPackageAvailable = isPackageAvailable,
                itemCounts = itemCounts,
                packageTitle = packageTitle,
                onSelectPackage = viewModel::selectPackage,
                onSelectDirection = viewModel::selectDirection,
                onSelectSource = viewModel::selectSource,
                onSelectPlaybackOrder = viewModel::selectPlaybackOrder,
                onUpdateFrontDelayMs = viewModel::updateFrontDelayMs,
                onUpdatePlayFrontAudio = viewModel::updatePlayFrontAudio,
                onUpdatePlayAnswerAudio = viewModel::updatePlayAnswerAudio,
                onUpdatePostAnswerDelayMs = viewModel::updatePostAnswerDelayMs,
                onUpdatePlayExampleEnglishAudio = viewModel::updatePlayExampleEnglishAudio,
                onUpdatePostExampleEnglishDelayMs = viewModel::updatePostExampleEnglishDelayMs,
                onUpdatePlayExampleVietnameseAudio = viewModel::updatePlayExampleVietnameseAudio,
                onUpdatePostExampleVietnameseDelayMs = viewModel::updatePostExampleVietnameseDelayMs,
                onUpdateKeepScreenOn = viewModel::updateKeepScreenOn,
                onUpdateBackgroundPlayback = viewModel::updateBackgroundPlayback,
                onUpdateSleepTimerMinutes = viewModel::updateSleepTimerMinutes,
                onStart = viewModel::startAutoPlay,
                onBack = onBack,
                modifier = modifier
            )
        }
        is AutoPlayEngineState.Running -> {
            AutoPlayPlayerScreen(
                state = state,
                hasPrevious = state.currentIndex > 0,
                isMuted = isMuted,
                onToggleMute = viewModel::toggleMute,
                remainingSleepMillis = remainingSleepMillis,
                onUpdateSleepTimer = viewModel::updateSleepTimerMinutes,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onStop = viewModel::stop,
                onOpenFullscreenImage = { fullscreenImageUri = it },
                modifier = modifier
            )
        }
        is AutoPlayEngineState.Completed -> {
            AutoPlayCompletedScreen(
                totalCount = state.totalCount,
                config = state.config,
                onReplay = viewModel::replay,
                onChangeSource = viewModel::stop,
                onBack = onBack,
                modifier = modifier
            )
        }
    }

    val fullImage = fullscreenImageUri
    if (fullImage != null) {
        Dialog(onDismissRequest = { fullscreenImageUri = null }) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))) {
                LearningEngineImage(
                    imagePath = fullImage,
                    imageUnavailable = false,
                    onOpenFullscreen = {},
                    fillCanvas = true,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { fullscreenImageUri = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng ảnh toàn màn hình")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoPlayConfigScreen(
    config: AutoPlayConfig,
    availablePackages: List<AutoPlayPackageInfo>,
    isPackageAvailable: Boolean,
    itemCounts: Map<AutoPlaySource, Int>,
    packageTitle: String?,
    onSelectPackage: (String) -> Unit,
    onSelectDirection: (AutoPlayDirection) -> Unit,
    onSelectSource: (AutoPlaySource) -> Unit,
    onSelectPlaybackOrder: (AutoPlayPlaybackOrder) -> Unit,
    onUpdateFrontDelayMs: (Long) -> Unit,
    onUpdatePlayFrontAudio: (Boolean) -> Unit,
    onUpdatePlayAnswerAudio: (Boolean) -> Unit,
    onUpdatePostAnswerDelayMs: (Long) -> Unit,
    onUpdatePlayExampleEnglishAudio: (Boolean) -> Unit,
    onUpdatePostExampleEnglishDelayMs: (Long) -> Unit,
    onUpdatePlayExampleVietnameseAudio: (Boolean) -> Unit,
    onUpdatePostExampleVietnameseDelayMs: (Long) -> Unit,
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onUpdateBackgroundPlayback: (Boolean) -> Unit,
    onUpdateSleepTimerMinutes: (Double?) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val currentItemCount = itemCounts[config.source] ?: 0
    val startButtonEnabled = isPackageAvailable && currentItemCount > 0
    val startButtonLabel = when {
        !isPackageAvailable -> "Select an available package"
        currentItemCount > 0 -> "Start Auto Play ($currentItemCount items)"
        else -> "No items in selected source"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.autoplay_title), style = MaterialTheme.typography.titleLarge)
                        packageTitle?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = LearningElevation.raised,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium)) {
                    LearningEnginePrimaryButton(
                        label = startButtonLabel,
                        onClick = onStart,
                        enabled = startButtonEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.small),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
        ) {
            // 0. Learning Package Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Learning Package")
                if (!isPackageAvailable) {
                    Surface(
                        shape = LearningEngineShapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(LearningSpacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                "Selected package is no longer available. Please select a package below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                if (availablePackages.isEmpty()) {
                    Text(
                        "No active packages found in library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availablePackages.forEach { pkg ->
                        val selected = config.selectedPackageId == pkg.id && isPackageAvailable
                        Surface(
                            shape = LearningEngineShapes.medium,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (selected) 2.dp else 1.dp,
                            border = if (selected) CardDefaults.outlinedCardBorder() else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPackage(pkg.id) }
                                .semantics {
                                    stateDescription = if (selected) "Selected, ${pkg.name}" else "Not selected, ${pkg.name}"
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectPackage(pkg.id) }
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(pkg.name, style = LearningTextRole.cardTitle)
                                        Surface(
                                            shape = CircleShape,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "${pkg.totalItemCount} items",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Content Source Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Content Source")
                AutoPlaySource.entries.forEach { source ->
                    val selected = config.source == source
                    val count = itemCounts[source] ?: 0
                    Surface(
                        shape = LearningEngineShapes.medium,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (selected) 2.dp else 1.dp,
                        border = if (selected) CardDefaults.outlinedCardBorder() else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSource(source) }
                            .semantics {
                                stateDescription = if (selected) "Selected, $count items" else "Not selected, $count items"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectSource(source) }
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(source.displayName, style = LearningTextRole.cardTitle)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(source.description, style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 2. Playback Order Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Playback Order")
                AutoPlayPlaybackOrder.entries.forEach { order ->
                    val selected = config.playbackOrder == order
                    Surface(
                        shape = LearningEngineShapes.medium,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (selected) 2.dp else 1.dp,
                        border = if (selected) CardDefaults.outlinedCardBorder() else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlaybackOrder(order) }
                            .semantics {
                                stateDescription = if (selected) "Selected" else "Not selected"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(LearningSpacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectPlaybackOrder(order) }
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    order.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    order.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Playback Direction Section (Prominent segmented cards)
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Playback Direction")
                AutoPlayDirection.entries.forEach { dir ->
                    val selected = config.direction == dir
                    Surface(
                        shape = LearningEngineShapes.medium,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (selected) 2.dp else 1.dp,
                        border = if (selected) CardDefaults.outlinedCardBorder() else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDirection(dir) }
                            .semantics {
                                stateDescription = if (selected) "Selected" else "Not selected"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(LearningSpacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectDirection(dir) }
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    dir.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    dir.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Front Prompt Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Front Prompt")
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(LearningSpacing.medium), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.autoplay_front_audio_title), style = LearningTextRole.cardTitle)
                                Text(
                                    if (config.direction == AutoPlayDirection.VIETNAMESE_TO_ENGLISH) stringResource(R.string.autoplay_front_audio_desc_vi) else stringResource(R.string.autoplay_front_audio_desc_en),
                                    style = LearningTextRole.metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.playFrontAudio,
                                onCheckedChange = onUpdatePlayFrontAudio
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                        AutoPlayDelayControl(
                            label = stringResource(R.string.autoplay_front_display_duration),
                            currentDelayMs = config.frontDelayMs,
                            presetSeconds = listOf(1.0, 2.0, 3.0, 5.0, 8.0),
                            minDelayMs = AutoPlayConfig.MIN_FRONT_DELAY_MS,
                            maxDelayMs = AutoPlayConfig.MAX_DELAY_MS,
                            onUpdateDelayMs = onUpdateFrontDelayMs
                        )
                    }
                }
            }

            // 4. Revealed Answer Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Revealed Answer")
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(LearningSpacing.medium), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.autoplay_answer_audio_title), style = LearningTextRole.cardTitle)
                                Text(
                                    if (config.direction == AutoPlayDirection.VIETNAMESE_TO_ENGLISH) stringResource(R.string.autoplay_answer_audio_desc_vi) else stringResource(R.string.autoplay_answer_audio_desc_en),
                                    style = LearningTextRole.metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.playAnswerAudio,
                                onCheckedChange = onUpdatePlayAnswerAudio
                            )
                        }

                        if (config.playAnswerAudio) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            AutoPlayDelayControl(
                                label = stringResource(R.string.autoplay_post_answer_delay),
                                currentDelayMs = config.postAnswerDelayMs,
                                presetSeconds = listOf(0.0, 1.0, 2.0, 3.0, 5.0),
                                minDelayMs = 0L,
                                maxDelayMs = AutoPlayConfig.MAX_DELAY_MS,
                                onUpdateDelayMs = onUpdatePostAnswerDelayMs
                            )
                        }
                    }
                }
            }

            // 5. English Example Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("English Example")
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(LearningSpacing.medium), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.autoplay_example_en_title), style = LearningTextRole.cardTitle)
                                Text(stringResource(R.string.autoplay_example_en_desc), style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = config.playExampleEnglishAudio,
                                onCheckedChange = onUpdatePlayExampleEnglishAudio
                            )
                        }

                        if (config.playExampleEnglishAudio) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            AutoPlayDelayControl(
                                label = stringResource(R.string.autoplay_post_example_en_delay),
                                currentDelayMs = config.postExampleEnglishDelayMs,
                                presetSeconds = listOf(0.0, 1.0, 2.0, 3.0, 5.0),
                                minDelayMs = 0L,
                                maxDelayMs = AutoPlayConfig.MAX_DELAY_MS,
                                onUpdateDelayMs = onUpdatePostExampleEnglishDelayMs
                            )
                        }
                    }
                }
            }

            // 6. Vietnamese Example Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Vietnamese Example")
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(LearningSpacing.medium), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.autoplay_example_vi_title), style = LearningTextRole.cardTitle)
                                Text(stringResource(R.string.autoplay_example_vi_desc), style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = config.playExampleVietnameseAudio,
                                onCheckedChange = onUpdatePlayExampleVietnameseAudio
                            )
                        }

                        if (config.playExampleVietnameseAudio) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            AutoPlayDelayControl(
                                label = stringResource(R.string.autoplay_post_example_vi_delay),
                                currentDelayMs = config.postExampleVietnameseDelayMs,
                                presetSeconds = listOf(0.0, 1.0, 2.0, 3.0, 5.0),
                                minDelayMs = 0L,
                                maxDelayMs = AutoPlayConfig.MAX_DELAY_MS,
                                onUpdateDelayMs = onUpdatePostExampleVietnameseDelayMs
                            )
                        }
                    }
                }
            }

            // 7. Display Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Display")
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(LearningSpacing.medium), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.autoplay_keep_screen_on_title), style = LearningTextRole.cardTitle)
                                Text(stringResource(R.string.autoplay_keep_screen_on_desc), style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = config.keepScreenOn,
                                onCheckedChange = onUpdateKeepScreenOn
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.autoplay_background_playback_title), style = LearningTextRole.cardTitle)
                                Text(stringResource(R.string.autoplay_background_playback_desc), style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = config.backgroundPlayback,
                                onCheckedChange = onUpdateBackgroundPlayback
                            )
                        }
                    }
                }
            }

            // 8. Sleep Timer Section
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSectionHeader("Sleep Timer")
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(LearningSpacing.medium), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                        Text(stringResource(R.string.autoplay_sleep_timer_desc), style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        AutoPlaySleepTimerControl(
                            selectedMinutes = config.sleepTimerMinutes,
                            onSelectMinutes = onUpdateSleepTimerMinutes
                        )
                    }
                }
            }

            Spacer(Modifier.height(LearningSpacing.large))
        }
    }
}

@Composable
private fun AutoPlayDelayControl(
    label: String,
    currentDelayMs: Long,
    presetSeconds: List<Double>,
    minDelayMs: Long,
    maxDelayMs: Long,
    onUpdateDelayMs: (Long) -> Unit
) {
    val currentSeconds = currentDelayMs / 1000.0
    val matchedPreset = presetSeconds.firstOrNull { (it * 1000.0).toLong() == currentDelayMs }
    var isCustomActive by remember(currentDelayMs) { mutableStateOf(matchedPreset == null) }
    var customText by remember(currentDelayMs) { mutableStateOf(AutoPlayConfig.formatSeconds(currentSeconds)) }
    var isInputError by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${AutoPlayConfig.formatSeconds(currentSeconds)}s",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Responsive presets row with wrapping protection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetSeconds.forEach { sec ->
                val isSelected = !isCustomActive && matchedPreset == sec
                val labelText = "${AutoPlayConfig.formatSeconds(sec)}s"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isCustomActive = false
                            val ms = (sec * 1000.0).toLong()
                            onUpdateDelayMs(ms)
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Custom Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCustomActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isCustomActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1.3f)
                    .clickable {
                        isCustomActive = true
                    }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Expandable Custom Decimal Input
        if (isCustomActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { input ->
                        customText = input
                        val parsed = AutoPlayConfig.normalizeDecimalSeconds(input)
                        if (parsed != null) {
                            val ms = (parsed * 1000.0).toLong()
                            if (ms in minDelayMs..maxDelayMs) {
                                isInputError = false
                                onUpdateDelayMs(ms)
                            } else {
                                isInputError = true
                            }
                        } else {
                            isInputError = input.isNotBlank()
                        }
                    },
                    label = { Text(stringResource(R.string.autoplay_custom_seconds)) },
                    placeholder = { Text("Ví dụ: 1,5 hoặc 2,75") },
                    isError = isInputError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isInputError) {
                    Text(
                        text = "Enter a valid duration between ${minDelayMs / 1000.0}s and ${maxDelayMs / 1000.0}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutoPlaySleepTimerControl(
    selectedMinutes: Double?,
    onSelectMinutes: (Double?) -> Unit
) {
    val presets = listOf(null to "Off", 10.0 to "10m", 20.0 to "20m", 30.0 to "30m", 45.0 to "45m", 60.0 to "60m")
    val isCustom = selectedMinutes != null && presets.none { it.first == selectedMinutes }
    var isCustomActive by remember(selectedMinutes) { mutableStateOf(isCustom) }
    var customText by remember(selectedMinutes) {
        mutableStateOf(if (isCustom && selectedMinutes != null) AutoPlayConfig.formatMinutes(selectedMinutes) else "")
    }
    var isInputError by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEach { (mins, label) ->
                val selected = !isCustomActive && selectedMinutes == mins
                FilterChip(
                    selected = selected,
                    onClick = {
                        isCustomActive = false
                        isInputError = false
                        onSelectMinutes(mins)
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            FilterChip(
                selected = isCustomActive,
                onClick = {
                    isCustomActive = true
                    val parsed = AutoPlayConfig.normalizeDecimalMinutes(customText)
                    if (parsed != null && parsed in AutoPlayConfig.MIN_SLEEP_TIMER_MINUTES..AutoPlayConfig.MAX_SLEEP_TIMER_MINUTES) {
                        isInputError = false
                        onSelectMinutes(parsed)
                    } else {
                        isInputError = customText.isNotBlank()
                    }
                },
                label = { Text(stringResource(R.string.autoplay_custom)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        if (isCustomActive) {
            OutlinedTextField(
                value = customText,
                onValueChange = { input ->
                    customText = input
                    val parsed = AutoPlayConfig.normalizeDecimalMinutes(input)
                    if (parsed != null && parsed in AutoPlayConfig.MIN_SLEEP_TIMER_MINUTES..AutoPlayConfig.MAX_SLEEP_TIMER_MINUTES) {
                        isInputError = false
                        onSelectMinutes(parsed)
                    } else {
                        isInputError = input.isNotBlank()
                    }
                },
                label = { Text(stringResource(R.string.autoplay_custom_minutes)) },
                placeholder = { Text("Ví dụ: 0,5 hoặc 37,5") },
                isError = isInputError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (isInputError) {
                Text(
                    text = "Enter a duration between ${AutoPlayConfig.MIN_SLEEP_TIMER_MINUTES}m and ${AutoPlayConfig.MAX_SLEEP_TIMER_MINUTES}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatRemainingMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun AutoPlaySleepTimerDialog(
    currentMinutes: Double?,
    remainingMillis: Long?,
    onDismiss: () -> Unit,
    onSelectMinutes: (Double?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.autoplay_sleep_timer_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (remainingMillis != null && remainingMillis > 0L) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Stopping in ${formatRemainingMillis(remainingMillis)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                AutoPlaySleepTimerControl(
                    selectedMinutes = currentMinutes,
                    onSelectMinutes = onSelectMinutes
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoPlayPlayerScreen(
    state: AutoPlayEngineState.Running,
    hasPrevious: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    remainingSleepMillis: Long?,
    onUpdateSleepTimer: (Double?) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onStop)

    var showSleepTimerDialog by remember { mutableStateOf(false) }

    if (showSleepTimerDialog) {
        AutoPlaySleepTimerDialog(
            currentMinutes = state.config.sleepTimerMinutes,
            remainingMillis = remainingSleepMillis,
            onDismiss = { showSleepTimerDialog = false },
            onSelectMinutes = onUpdateSleepTimer
        )
    }

    val view = LocalView.current
    DisposableEffect(state.config.keepScreenOn) {
        val prev = view.keepScreenOn
        if (state.config.keepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = prev
        }
    }

    val item = state.item
    val isRevealed = state.stage != AutoPlayStage.FRONT_WAIT
    val direction = state.config.direction

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(state.config.source.displayName, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${state.currentIndex + 1} / ${state.totalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Close, contentDescription = "Thoát Tự động phát")
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (direction == AutoPlayDirection.VIETNAMESE_TO_ENGLISH) "VI → EN" else "EN → VI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (state.isPaused) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = LearningElevation.raised,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                ) {
                    // Secondary Control Row (Mute, Sleep Timer Badge, Stop)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleMute) {
                            Icon(
                                if (isMuted) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute Auto Play" else "Mute Auto Play",
                                tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            onClick = { showSleepTimerDialog = true },
                            shape = CircleShape,
                            color = if (remainingSleepMillis != null && remainingSleepMillis > 0L) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Hẹn giờ ngủ",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (remainingSleepMillis != null && remainingSleepMillis > 0L) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (remainingSleepMillis != null && remainingSleepMillis > 0L) "Sleep · ${formatRemainingMillis(remainingSleepMillis)}" else "Sleep Timer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (remainingSleepMillis != null && remainingSleepMillis > 0L) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onStop) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Dừng Tự động phát",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { (state.currentIndex + 1).toFloat() / state.totalCount.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    )

                    // Playback Controls Row (Previous, Play/Pause, Next)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            enabled = hasPrevious
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Mục trước",
                                modifier = Modifier.size(36.dp),
                                tint = if (hasPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // Play / Pause Button
                        FilledIconButton(
                            onClick = { if (state.isPaused) onResume() else onPause() },
                            modifier = Modifier.size(60.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (state.isPaused) "Resume Auto Play" else "Pause Auto Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Mục tiếp theo",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(LearningSpacing.screen),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Image (prominent)
                    if (!item.imagePath.isNullOrBlank()) {
                        LearningEngineImage(
                            imagePath = item.imagePath,
                            imageUnavailable = false,
                            onOpenFullscreen = onOpenFullscreenImage,
                            fillCanvas = true,
                            adaptiveFitBounds = LearningImageFitBounds(140, 220),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    // Part of Speech Tag
                    item.partOfSpeech?.let { pos ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = pos,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!isRevealed) {
                        // FRONT PRESENTATION
                        if (direction == AutoPlayDirection.VIETNAMESE_TO_ENGLISH) {
                            Text(
                                text = item.vietnameseMeaning,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = item.headword,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            item.ipa?.let { ipa ->
                                Text(
                                    text = ipa,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Revealing automatically...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // REVEALED PRESENTATION
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.headword,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                if (state.stage == AutoPlayStage.ANSWER_AUDIO && direction == AutoPlayDirection.VIETNAMESE_TO_ENGLISH) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Đang phát âm thanh từ tiếng Anh",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            item.ipa?.let { ipa ->
                                Text(
                                    text = ipa,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.vietnameseMeaning,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.stage == AutoPlayStage.ANSWER_AUDIO && direction == AutoPlayDirection.ENGLISH_TO_VIETNAMESE) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Đang phát âm thanh đáp án tiếng Việt",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // English Example & Vietnamese Example
                        if (!item.englishExample.isNullOrBlank() || !item.vietnameseExample.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // English Example
                                    if (!item.englishExample.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.englishExample,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (state.stage == AutoPlayStage.EXAMPLE_EN_AUDIO) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Đang phát âm thanh ví dụ tiếng Anh",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Vietnamese Example
                                    if (!item.vietnameseExample.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.vietnameseExample,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (state.stage == AutoPlayStage.EXAMPLE_VI_AUDIO) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Đang phát âm thanh ví dụ tiếng Việt",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoPlayCompletedScreen(
    totalCount: Int,
    config: AutoPlayConfig,
    onReplay: () -> Unit,
    onChangeSource: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tự động phát", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Về Trang chủ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(LearningSpacing.screen),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        "Auto Play Complete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Finished playback of all $totalCount items in ${config.source.displayName} (${config.direction.displayName}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    LearningEnginePrimaryButton(
                        label = "Play Again",
                        onClick = onReplay,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LearningEngineSecondaryButton(
                        label = "Change Source & Settings",
                        onClick = onChangeSource,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_back_home))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoPlayEmptyScreen(
    source: AutoPlaySource,
    onChangeSource: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.autoplay_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back_home))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(LearningSpacing.screen),
            contentAlignment = Alignment.Center
        ) {
            LearningEngineEmptyState(
                title = "No items available",
                detail = "There are no eligible items for ${source.displayName} in the active package.",
                actionLabel = "Change source",
                onAction = onChangeSource
            )
        }
    }
}
