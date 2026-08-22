package vn.loi.learning.android.packageexperience

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.android.R
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.study.normalizedIntroductionPronunciation
import vn.loi.learning.android.study.partOfSpeechPresentation
import vn.loi.learning.android.study.components.PartOfSpeechBadge
import vn.loi.learning.android.ui.*

/**
 * Canonical Package Experience screen.
 *
 * Composable contract:
 * - State hoisted; no ViewModel, no repository in composable.
 * - Material 3 foundation components only.
 * - Light/Dark via MaterialTheme (LearningEngineTheme).
 * - WindowInsets applied.
 * - Touch targets ≥ 48 dp.
 * - Semantic headings and content descriptions.
 * - Lazy stable keys — no per-row media load, no per-row archive parse.
 */
@Composable
fun PackageScreen(
    state: AndroidPackageContentState,
    operationState: AndroidPackageOperationState = AndroidPackageOperationState.Idle,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStudyPackage: () -> Unit,
    onContinueLearning: () -> Unit,
    onSelectLearningPackage: () -> Unit = {},
    onOpenContent: (String) -> Unit = {},
    onSaveQuickEdit: (AndroidPackageQuickEditDraft, (Result<Unit>) -> Unit) -> Unit = { _, callback -> callback(Result.failure(IllegalStateException("Editor unavailable"))) },
    onExport: () -> Unit = {},
    onVerify: () -> Unit = {},
    onUninstall: () -> Unit = {},
    resolveMedia: (String) -> String? = { null },
    onDismissOperation: () -> Unit = {},
    onSetFsrsFilter: (AndroidFsrsFilter) -> Unit = {},
    onToggleDifficultFilter: () -> Unit = {},
    onSetLessonFilter: (String?) -> Unit = {},
    onSetMediaFilter: (BrowserMediaFilter) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onToggleDifficult: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, onRefresh) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Scaffold(
        topBar = {
            PackageTopBar(
                title = when (state) {
                    is AndroidPackageContentState.Content -> androidDisplayTitle(state.header.title)
                    is AndroidPackageContentState.Empty -> androidDisplayTitle(state.header.title)
                    else -> "Package"
                },
                isActive = when (state) {
                    is AndroidPackageContentState.Content -> state.header.isActivePackage
                    is AndroidPackageContentState.Empty -> state.header.isActivePackage
                    else -> false
                },
                packageId = when (state) {
                    is AndroidPackageContentState.Content -> state.header.packageId
                    is AndroidPackageContentState.Empty -> state.header.packageId
                    else -> null
                },
                onBack = onBack,
                onExport = onExport,
                onVerify = onVerify,
                onUninstall = onUninstall
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            when (state) {
                AndroidPackageContentState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LearningEngineLoadingState("Loading package")
                    }
                }

                is AndroidPackageContentState.Failure -> {
                    Box(
                        Modifier.fillMaxSize().padding(LearningSpacing.screen),
                        contentAlignment = Alignment.Center
                    ) {
                        LearningEngineErrorState(
                            title = "Package unavailable",
                            message = state.message,
                            onRetry = if (state.recoverable) onBack else null
                        )
                    }
                }

                is AndroidPackageContentState.Empty -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = LearningSpacing.screen,
                            vertical = LearningSpacing.section
                        ),
                        verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
                    ) {
                        item("header") {
                            PackageHeader(state.header)
                        }
                        item("learning-package") {
                            LearningPackageSelection(state.header, onSelectLearningPackage)
                        }
                        item("empty") {
                            LearningEngineEmptyState(
                                title = "No content",
                                detail = "This package has no content yet."
                            )
                        }
                        // Operation feedback
                        operationFeedback(operationState, onDismissOperation)
                    }
                }

                is AndroidPackageContentState.Content -> {
                    PackageContentBody(
                        state = state,
                        operationState = operationState,
                        onSearch = onSearch,
                        onClearSearch = onClearSearch,
                        onStudyPackage = onStudyPackage,
                        onContinueLearning = onContinueLearning,
                        onSelectLearningPackage = onSelectLearningPackage,
                        onOpenContent = onOpenContent,
                        onSaveQuickEdit = onSaveQuickEdit,
                        resolveMedia = resolveMedia,
                        onDismissOperation = onDismissOperation,
                        onSetFsrsFilter = onSetFsrsFilter,
                        onToggleDifficultFilter = onToggleDifficultFilter,
                        onSetLessonFilter = onSetLessonFilter,
                        onSetMediaFilter = onSetMediaFilter,
                        onClearFilters = onClearFilters,
                        onToggleDifficult = onToggleDifficult
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageTopBar(
    title: String,
    isActive: Boolean,
    packageId: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onVerify: () -> Unit,
    onUninstall: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (isActive) {
                    Text(
                        "Current learning package",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại thư viện")
            }
        },
        actions = {
            if (packageId != null) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Thao tác với gói")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Xuất gói") },
                            onClick = { showMenu = false; onExport() }
                        )
                        DropdownMenuItem(
                            text = { Text("Xác minh tệp gói") },
                            onClick = { showMenu = false; onVerify() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Gỡ cài đặt", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; confirmUninstall = true }
                        )
                    }
                }
            }
        }
    )

    if (confirmUninstall) {
        AlertDialog(
            onDismissRequest = { confirmUninstall = false },
            title = { Text("Gỡ cài đặt gói?") },
            text = { Text("Thao tác này sẽ xóa \"$title\" và dữ liệu học cục bộ của gói. Không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = { confirmUninstall = false; onUninstall() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Gỡ cài đặt") }
            },
            dismissButton = {
                TextButton(onClick = { confirmUninstall = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun PackageHeader(header: AndroidPackageHeaderModel) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(LearningSpacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        androidDisplayTitle(header.title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics {
                            contentDescription = header.title
                        }
                    )
                    Text(
                        "v${header.version}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineStatusBadge(
                    label = if (header.state == "ACTIVE") "Available" else header.state.lowercase().replaceFirstChar(Char::uppercase),
                    tone = LearningStatusTone.INFO
                )
                Text(
                    "${header.contentCount} content${if (header.contentCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LearningPackageSelection(header: AndroidPackageHeaderModel, onSelect: () -> Unit) {
    if (!header.isActivePackage && header.state == "ACTIVE") {
        LearningEngineSecondaryButton(
            label = "Use for Study",
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = LearningSpacing.touchTarget)
        )
    }
}

@Composable
private fun PackageCtaRow(
    cta: AndroidPackageCta,
    onStudyPackage: () -> Unit,
    onContinueLearning: () -> Unit
) {
    when (cta) {
        is AndroidPackageCta.ContinueLearning -> {
            LearningEnginePrimaryButton(
                label = stringResource(R.string.library_continue_learning),
                onClick = onContinueLearning,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .semantics { contentDescription = "Tiếp tục phiên học hiện tại" }
            )
        }
        AndroidPackageCta.ContinuePackage -> {
            LearningEnginePrimaryButton(
                label = stringResource(R.string.library_continue_package),
                onClick = onStudyPackage,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .semantics { contentDescription = "Tiếp tục học gói này" }
            )
        }
        AndroidPackageCta.StudyPackage -> {
            LearningEnginePrimaryButton(
                label = stringResource(R.string.library_study_package),
                onClick = onStudyPackage,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .semantics { contentDescription = "Bắt đầu phiên học cho gói này" }
            )
        }
        AndroidPackageCta.NoContent -> {
            // No CTA when there's no content
        }
    }
}

@Composable
private fun PackageSearchField(
    query: String,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(query, TextRange(query.length)))
    }
    LaunchedEffect(query) {
        if (query != input.text) input = TextFieldValue(query, TextRange(query.length))
    }
    OutlinedTextField(
        value = input,
        onValueChange = { updated -> input = updated; onSearch(updated.text) },
        label = { Text(stringResource(R.string.library_search_placeholder)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        trailingIcon = {
            if (input.text.isNotEmpty()) {
                IconButton(
                    onClick = { input = TextFieldValue("", TextRange.Zero); onClear() },
                    modifier = Modifier.defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.library_clear_search))
                }
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .semantics { contentDescription = "Tìm kiếm trong gói này" }
    )
}

@Composable
private fun PackageFiltersRow(
    filterSpec: AndroidPackageFilterSpec,
    availableLessons: List<String>,
    onSetFsrsFilter: (AndroidFsrsFilter) -> Unit,
    onToggleDifficultFilter: () -> Unit,
    onSetLessonFilter: (String?) -> Unit,
    onSetMediaFilter: (BrowserMediaFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    var fsrsMenuExpanded by remember { mutableStateOf(false) }
    var lessonMenuExpanded by remember { mutableStateOf(false) }
    var mediaMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. FSRS Filter Dropdown
        Box {
            FilterChip(
                selected = filterSpec.fsrsFilter != AndroidFsrsFilter.ALL,
                onClick = { fsrsMenuExpanded = true },
                label = { Text("FSRS: ${filterSpec.fsrsFilter.label}") },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics {
                    contentDescription = "FSRS filter, ${filterSpec.fsrsFilter.label}"
                }
            )
            DropdownMenu(
                expanded = fsrsMenuExpanded,
                onDismissRequest = { fsrsMenuExpanded = false }
            ) {
                AndroidFsrsFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (filter == filterSpec.fsrsFilter) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(filter.label)
                            }
                        },
                        onClick = {
                            fsrsMenuExpanded = false
                            onSetFsrsFilter(filter)
                        }
                    )
                }
            }
        }

        // 2. Difficult Filter Chip
        FilterChip(
            selected = filterSpec.difficultOnly,
            onClick = onToggleDifficultFilter,
            label = { Text("★ Difficult") },
            leadingIcon = if (filterSpec.difficultOnly) {
                { Icon(Icons.Filled.Star, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary) }
            } else null,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics {
                contentDescription = "Marked difficult, ${if (filterSpec.difficultOnly) "enabled" else "disabled"}"
            }
        )

        // 3. Lesson Filter Dropdown (if multiple lessons available)
        if (availableLessons.size > 1) {
            Box {
                FilterChip(
                    selected = filterSpec.selectedLesson != null,
                    onClick = { lessonMenuExpanded = true },
                    label = { Text(filterSpec.selectedLesson?.let { "Lesson: $it" } ?: "All lessons") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics {
                        contentDescription = "Lesson filter, ${filterSpec.selectedLesson ?: "All lessons"}"
                    }
                )
                DropdownMenu(
                    expanded = lessonMenuExpanded,
                    onDismissRequest = { lessonMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (filterSpec.selectedLesson == null) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Tất cả bài học")
                            }
                        },
                        onClick = {
                            lessonMenuExpanded = false
                            onSetLessonFilter(null)
                        }
                    )
                    availableLessons.forEach { lesson ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (filterSpec.selectedLesson == lesson) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(lesson)
                                }
                            },
                            onClick = {
                                lessonMenuExpanded = false
                                onSetLessonFilter(lesson)
                            }
                        )
                    }
                }
            }
        }

        Box {
            FilterChip(
                selected = filterSpec.mediaFilter != BrowserMediaFilter.ALL,
                onClick = { mediaMenuExpanded = true },
                label = { Text(filterSpec.mediaFilter.label) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics {
                    contentDescription = "Media filter, ${filterSpec.mediaFilter.label}"
                }
            )
            DropdownMenu(expanded = mediaMenuExpanded, onDismissRequest = { mediaMenuExpanded = false }) {
                BrowserMediaFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.label) },
                        onClick = {
                            mediaMenuExpanded = false
                            onSetMediaFilter(filter)
                        }
                    )
                }
            }
        }

        // 4. Clear Filters Action (if any filter is active)
        if (filterSpec.isFiltered) {
            AssistChip(
                onClick = onClearFilters,
                label = { Text("Xóa bộ lọc") },
                leadingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics {
                    contentDescription = "Xóa tất cả bộ lọc"
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PackageContentRow(
    row: AndroidPackageContentRow,
    resolveMedia: (String) -> String?,
    onQuickEdit: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onToggleDifficult: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { row.audioRef?.let { onPlayAudio(it) } },
                        onLongClick = onQuickEdit
                    )
                    .semantics {
                        contentDescription = buildString {
                            append(row.question)
                            if (row.answer.isNotEmpty()) { append(", "); append(row.answer) }
                            if (row.hasImage) append(", has image")
                            if (row.hasAudio) append(", has audio")
                            append(", status ${row.fsrsStatus.label}")
                            if (row.isDifficult) append(", marked difficult")
                            append(". Chạm để nghe từ. Nhấn giữ để sửa.")
                        }
                        customActions = buildList {
                            row.audioRef?.let { reference ->
                                add(CustomAccessibilityAction("Nghe từ") { onPlayAudio(reference); true })
                            }
                            add(CustomAccessibilityAction("Mở chi tiết") { onClick(); true })
                            add(CustomAccessibilityAction("Sửa từ") { onQuickEdit(); true })
                        }
                        role = Role.Button
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    row.question,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                normalizedIntroductionPronunciation(row.partOfSpeech, row.pronunciation)?.let { pronunciation ->
                    Text(
                        pronunciation,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (row.answer.isNotBlank()) {
                    Text(
                        row.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (row.imageRef != null) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = onQuickEdit
                        )
                        .semantics {
                            contentDescription = "Mở chi tiết cho ${row.question}"
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PackageThumbnail(row.imageRef, resolveMedia)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 96.dp)
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = onQuickEdit
                        )
                        .semantics {
                            contentDescription = "Mở chi tiết cho ${row.question}"
                            role = Role.Button
                        }
                )
            }
        }

        // Overlays outside Row width allocation
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(if (row.imageRef != null) 96.dp else 48.dp)
        ) {
            partOfSpeechPresentation(row.partOfSpeech)?.let { presentation ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    PartOfSpeechBadge(presentation, compact = true)
                }
            }
            IconButton(
                onClick = onToggleDifficult,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(48.dp)
                    .semantics {
                        contentDescription = if (row.isDifficult) "Unmark as difficult" else "Mark as difficult"
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (row.isDifficult) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (row.isDifficult) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PackageContentBody(
    state: AndroidPackageContentState.Content,
    operationState: AndroidPackageOperationState,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStudyPackage: () -> Unit,
    onContinueLearning: () -> Unit,
    onSelectLearningPackage: () -> Unit,
    onOpenContent: (String) -> Unit,
    resolveMedia: (String) -> String?,
    onSaveQuickEdit: (AndroidPackageQuickEditDraft, (Result<Unit>) -> Unit) -> Unit,
    onDismissOperation: () -> Unit,
    onSetFsrsFilter: (AndroidFsrsFilter) -> Unit,
    onToggleDifficultFilter: () -> Unit,
    onSetLessonFilter: (String?) -> Unit,
    onSetMediaFilter: (BrowserMediaFilter) -> Unit,
    onClearFilters: () -> Unit,
    onToggleDifficult: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val audioController = remember { AndroidAudioController() }
    var editing by remember { mutableStateOf<AndroidPackageContentRow?>(null) }
    DisposableEffect(audioController) { onDispose(audioController::close) }
    val playWordAudio: (String) -> Unit = { reference ->
        resolveMedia(reference)?.let { path ->
            audioController.stop()
            audioController.replay(path, isLooping = false) { }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = LearningSpacing.screen,
            vertical = LearningSpacing.section
        ),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
    ) {
        // Package header
        item("header") {
            PackageHeader(state.header)
        }

        item("learning-package") {
            LearningPackageSelection(state.header, onSelectLearningPackage)
        }

        // Primary CTA
        item("cta") {
            PackageCtaRow(
                cta = state.cta,
                onStudyPackage = onStudyPackage,
                onContinueLearning = onContinueLearning
            )
        }

        // Operation feedback
        operationFeedback(operationState, onDismissOperation)

        stickyHeader("search") {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = LearningSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
                ) {
                    PackageSearchField(state.query, onSearch, onClearSearch)
                    PackageFiltersRow(
                        filterSpec = state.filterSpec,
                        availableLessons = state.availableLessons,
                        onSetFsrsFilter = onSetFsrsFilter,
                        onToggleDifficultFilter = onToggleDifficultFilter,
                        onSetLessonFilter = onSetLessonFilter,
                        onSetMediaFilter = onSetMediaFilter,
                        onClearFilters = onClearFilters
                    )
                    Text(
                        "${state.visibleRows.size} of ${state.allRows.size} items",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        }

        // Empty search/filter result
        if (state.visibleRows.isEmpty()) {
            item("no-results") {
                if (state.filterSpec.isFiltered) {
                    val detailMsg = buildString {
                        if (state.filterSpec.difficultOnly && state.filterSpec.fsrsFilter == AndroidFsrsFilter.DUE) {
                            append("No difficult words are due")
                        } else if (state.filterSpec.difficultOnly) {
                            append("No difficult words found")
                        } else if (state.filterSpec.fsrsFilter != AndroidFsrsFilter.ALL) {
                            append("No words in ${state.filterSpec.fsrsFilter.label} status")
                        } else {
                            append("No words match the selected filters")
                        }
                        if (state.filterSpec.selectedLesson != null) {
                            append(" in ${state.filterSpec.selectedLesson}")
                        }
                        if (state.filterSpec.query.isNotBlank()) {
                            append(" matching \"${state.filterSpec.query}\"")
                        }
                        append(".")
                    }
                    LearningEngineEmptyState(
                        title = "No matching words",
                        detail = detailMsg,
                        actionLabel = "Clear filters",
                        onAction = onClearFilters
                    )
                } else {
                    LearningEngineEmptyState(
                        title = "No content",
                        detail = "This package has no content yet."
                    )
                }
            }
        }

        // Content rows — stable keys, immutable rows, no media load per row
        items(
            items = state.visibleRows,
            key = { "content-${it.contentId}" }
        ) { row ->
            PackageContentRow(
                row = row,
                resolveMedia = resolveMedia,
                onQuickEdit = { editing = row },
                onPlayAudio = playWordAudio,
                onToggleDifficult = { onToggleDifficult(row.contentId) },
                onClick = { onOpenContent(row.contentId) }
            )
        }
    }
    editing?.let { row ->
        PackageQuickEditDialog(row, onDismiss = { editing = null }, onSave = { draft, callback ->
            onSaveQuickEdit(draft) { result ->
                callback(result)
                if (result.isSuccess) editing = null
            }
        })
    }
}

@Composable
private fun PackageThumbnail(reference: String, resolveMedia: (String) -> String?) {
    val targetPixels = with(LocalDensity.current) { 96.dp.roundToPx() }
    val bitmap by produceState<android.graphics.Bitmap?>(null, reference, targetPixels) {
        value = withContext(Dispatchers.IO) {
            val path = resolveMedia(reference) ?: return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val sample = thumbnailSampleSize(bounds.outWidth, bounds.outHeight, targetPixels, targetPixels)
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                inSampleSize = sample
                inScaled = false
            })
        }
    }
    bitmap?.let {
        Surface(Modifier.size(96.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
            Image(
                it.asImageBitmap(),
                "Content image",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium
            )
        }
    }
}

internal fun thumbnailSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) return 1
    var sample = 1
    while (
        sourceWidth / (sample * 2) >= targetWidth &&
        sourceHeight / (sample * 2) >= targetHeight
    ) {
        sample *= 2
    }
    return sample
}

@Composable
private fun PackageQuickEditDialog(
    row: AndroidPackageContentRow,
    onDismiss: () -> Unit,
    onSave: (AndroidPackageQuickEditDraft, (Result<Unit>) -> Unit) -> Unit
) {
    var question by remember(row.contentId) { mutableStateOf(row.question) }
    var answer by remember(row.contentId) { mutableStateOf(row.answer) }
    var pronunciation by remember(row.contentId) { mutableStateOf(row.pronunciation) }
    var pos by remember(row.contentId) { mutableStateOf(row.partOfSpeech) }
    var example by remember(row.contentId) { mutableStateOf(row.example.orEmpty()) }
    var translation by remember(row.contentId) { mutableStateOf(row.translation.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Sửa từ") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Question" to question, "Answer" to answer, "IPA / Pronunciation" to pronunciation,
                    "POS" to pos, "Example" to example, "Translation" to translation
                ).forEachIndexed { index, pair ->
                    OutlinedTextField(
                        pair.second,
                        { value -> when(index){0->question=value;1->answer=value;2->pronunciation=value;3->pos=value;4->example=value;else->translation=value} },
                        label={Text(pair.first)}, enabled=!saving,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(enabled = !saving, onClick = {
            saving = true; error = null
            onSave(AndroidPackageQuickEditDraft(row.contentId, question, answer, pronunciation, pos, example, translation)) {
                saving = false; error = it.exceptionOrNull()?.message
            }
        }) { Text(if (saving) "Saving…" else "Save") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Hủy") } }
    )
}

/** Extension to add operation feedback item to a LazyListScope. */
private fun androidx.compose.foundation.lazy.LazyListScope.operationFeedback(
    operationState: AndroidPackageOperationState,
    onDismiss: () -> Unit
) {
    when (operationState) {
        is AndroidPackageOperationState.Pending -> {
            item("op-pending") {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = LearningSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(operationState.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is AndroidPackageOperationState.Succeeded -> {
            item("op-success") {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = LearningSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
                ) {
                    Text(
                        operationState.message,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    ) { Text("Đóng") }
                }
            }
        }
        is AndroidPackageOperationState.Failed -> {
            item("op-failed") {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = LearningSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
                ) {
                    Text(
                        operationState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Assertive }
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    ) { Text("Đóng") }
                }
            }
        }
        AndroidPackageOperationState.Idle -> { /* nothing */ }
    }
}
