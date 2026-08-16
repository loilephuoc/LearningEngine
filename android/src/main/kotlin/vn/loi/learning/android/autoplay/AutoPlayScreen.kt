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

    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val itemCounts by viewModel.itemCounts.collectAsStateWithLifecycle()
    val packageTitle by viewModel.packageTitle.collectAsStateWithLifecycle()

    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

    when (val state = engineState) {
        is AutoPlayEngineState.Idle -> {
            AutoPlayConfigScreen(
                config = config,
                itemCounts = itemCounts,
                packageTitle = packageTitle,
                onSelectDirection = viewModel::selectDirection,
                onSelectSource = viewModel::selectSource,
                onUpdateFrontDelayMs = viewModel::updateFrontDelayMs,
                onUpdatePlayFrontAudio = viewModel::updatePlayFrontAudio,
                onUpdatePlayAnswerAudio = viewModel::updatePlayAnswerAudio,
                onUpdatePostAnswerDelayMs = viewModel::updatePostAnswerDelayMs,
                onUpdatePlayExampleEnglishAudio = viewModel::updatePlayExampleEnglishAudio,
                onUpdatePostExampleEnglishDelayMs = viewModel::updatePostExampleEnglishDelayMs,
                onUpdatePlayExampleVietnameseAudio = viewModel::updatePlayExampleVietnameseAudio,
                onUpdatePostExampleVietnameseDelayMs = viewModel::updatePostExampleVietnameseDelayMs,
                onUpdateKeepScreenOn = viewModel::updateKeepScreenOn,
                onStart = viewModel::startAutoPlay,
                onBack = onBack,
                modifier = modifier
            )
        }
        is AutoPlayEngineState.Empty -> {
            AutoPlayEmptyScreen(
                source = state.source,
                onChangeSource = viewModel::stop,
                onBack = onBack,
                modifier = modifier
            )
        }
        is AutoPlayEngineState.Running -> {
            AutoPlayPlayerScreen(
                state = state,
                hasPrevious = viewModel.engine.hasPrevious,
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
                    Icon(Icons.Default.Close, contentDescription = "Close full image")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoPlayConfigScreen(
    config: AutoPlayConfig,
    itemCounts: Map<AutoPlaySource, Int>,
    packageTitle: String?,
    onSelectDirection: (AutoPlayDirection) -> Unit,
    onSelectSource: (AutoPlaySource) -> Unit,
    onUpdateFrontDelayMs: (Long) -> Unit,
    onUpdatePlayFrontAudio: (Boolean) -> Unit,
    onUpdatePlayAnswerAudio: (Boolean) -> Unit,
    onUpdatePostAnswerDelayMs: (Long) -> Unit,
    onUpdatePlayExampleEnglishAudio: (Boolean) -> Unit,
    onUpdatePostExampleEnglishDelayMs: (Long) -> Unit,
    onUpdatePlayExampleVietnameseAudio: (Boolean) -> Unit,
    onUpdatePostExampleVietnameseDelayMs: (Long) -> Unit,
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val currentItemCount = itemCounts[config.source] ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Auto Play", style = MaterialTheme.typography.titleLarge)
                        packageTitle?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
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
                        label = if (currentItemCount > 0) "Start Auto Play ($currentItemCount items)" else "No items in selected source",
                        onClick = onStart,
                        enabled = currentItemCount > 0,
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

            // 2. Playback Direction Section (Prominent segmented cards)
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
                                Text("Play Front Audio", style = LearningTextRole.cardTitle)
                                Text(
                                    if (config.direction == AutoPlayDirection.VIETNAMESE_TO_ENGLISH) "Play Vietnamese answer audio on the front." else "Play English word audio on the front.",
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
                            label = "Front Display Duration",
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
                                Text("Play Answer Audio", style = LearningTextRole.cardTitle)
                                Text(
                                    if (config.direction == AutoPlayDirection.VIETNAMESE_TO_ENGLISH) "Play English word pronunciation after reveal." else "Play Vietnamese answer audio after reveal.",
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
                                label = "Delay after Answer Audio",
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
                                Text("Play English Example Audio", style = LearningTextRole.cardTitle)
                                Text("Play English example sentence audio if available", style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = config.playExampleEnglishAudio,
                                onCheckedChange = onUpdatePlayExampleEnglishAudio
                            )
                        }

                        if (config.playExampleEnglishAudio) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            AutoPlayDelayControl(
                                label = "Delay after English Example",
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
                                Text("Play Vietnamese Example Audio", style = LearningTextRole.cardTitle)
                                Text("Play Vietnamese example translation audio if available", style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = config.playExampleVietnameseAudio,
                                onCheckedChange = onUpdatePlayExampleVietnameseAudio
                            )
                        }

                        if (config.playExampleVietnameseAudio) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            AutoPlayDelayControl(
                                label = "Delay after Vietnamese Example",
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
                    Row(
                        Modifier.fillMaxWidth().padding(LearningSpacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Keep Screen On", style = LearningTextRole.cardTitle)
                            Text("Prevent screen from sleeping during active Auto Play", style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = config.keepScreenOn,
                            onCheckedChange = onUpdateKeepScreenOn
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
                    label = { Text("Custom seconds") },
                    placeholder = { Text("e.g. 1.5 or 2.75") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoPlayPlayerScreen(
    state: AutoPlayEngineState.Running,
    hasPrevious: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onStop)

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
                        Icon(Icons.Default.Close, contentDescription = "Exit Auto Play")
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
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { (state.currentIndex + 1).toFloat() / state.totalCount.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    )

                    // Playback Controls Row
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
                                contentDescription = "Previous item",
                                modifier = Modifier.size(32.dp),
                                tint = if (hasPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // Play / Pause Button
                        FilledIconButton(
                            onClick = { if (state.isPaused) onResume() else onPause() },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (state.isPaused) "Resume Auto Play" else "Pause Auto Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next item",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = onStop
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop and configure",
                                modifier = Modifier.size(32.dp)
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
                                        contentDescription = "Playing English word audio",
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
                                    contentDescription = "Playing Vietnamese answer audio",
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
                                                    contentDescription = "Playing English example audio",
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
                                                    contentDescription = "Playing Vietnamese example audio",
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
                title = { Text("Auto Play", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
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
                        Text("Back to Home")
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
                title = { Text("Auto Play", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
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
