package vn.loi.learning.android.packageexperience

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onOpenContent: (String) -> Unit = {},
    onExport: () -> Unit = {},
    onVerify: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onDismissOperation: () -> Unit = {}
) {
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
                        onOpenContent = onOpenContent,
                        onDismissOperation = onDismissOperation
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
                        "Active learning package",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library")
            }
        },
        actions = {
            if (packageId != null) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Package operations")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export package") },
                            onClick = { showMenu = false; onExport() }
                        )
                        DropdownMenuItem(
                            text = { Text("Verify package file") },
                            onClick = { showMenu = false; onVerify() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Uninstall", color = MaterialTheme.colorScheme.error) },
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
            title = { Text("Uninstall package?") },
            text = { Text("This removes \"$title\" and its local learning data. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { confirmUninstall = false; onUninstall() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Uninstall") }
            },
            dismissButton = {
                TextButton(onClick = { confirmUninstall = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PackageHeader(header: AndroidPackageHeaderModel) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
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
                    label = header.state.lowercase().replaceFirstChar(Char::uppercase),
                    tone = if (header.isActivePackage) LearningStatusTone.ACTIVE else LearningStatusTone.INFO
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
private fun PackageCtaRow(
    cta: AndroidPackageCta,
    onStudyPackage: () -> Unit,
    onContinueLearning: () -> Unit
) {
    when (cta) {
        is AndroidPackageCta.ContinueLearning -> {
            LearningEnginePrimaryButton(
                label = "Continue Learning",
                onClick = onContinueLearning,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .semantics { contentDescription = "Continue your current learning session" }
            )
        }
        AndroidPackageCta.ContinuePackage -> {
            LearningEnginePrimaryButton(
                label = "Continue Package",
                onClick = onStudyPackage,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .semantics { contentDescription = "Continue studying this package" }
            )
        }
        AndroidPackageCta.StudyPackage -> {
            LearningEnginePrimaryButton(
                label = "Study Package",
                onClick = onStudyPackage,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .semantics { contentDescription = "Start a study session for this package" }
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
    OutlinedTextField(
        value = query,
        onValueChange = onSearch,
        label = { Text("Search content") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { keyboard?.hide(); onClear() },
                    modifier = Modifier.defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .semantics { contentDescription = "Search within this package" }
    )
}

@Composable
private fun PackageContentRow(
    row: AndroidPackageContentRow,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                row.question,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                if (row.answer.isNotBlank()) {
                    Text(
                        row.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val lessonLabel = buildString {
                    row.group?.let { append(it); append(" › ") }
                    row.section?.let { append(it); append(" › ") }
                    append(row.lesson)
                }
                Text(
                    lessonLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.hasImage) Icon(Icons.Default.Image, contentDescription = "Has image", Modifier.size(16.dp))
                if (row.hasAudio) Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Has audio", Modifier.size(16.dp))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(row.question)
                    if (row.answer.isNotBlank()) { append(", "); append(row.answer) }
                    append(", lesson "); append(row.lesson)
                    if (row.hasImage) append(", has image")
                    if (row.hasAudio) append(", has audio")
                }
                role = Role.Button
            }
    )
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PackageContentBody(
    state: AndroidPackageContentState.Content,
    operationState: AndroidPackageOperationState,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStudyPackage: () -> Unit,
    onContinueLearning: () -> Unit,
    onOpenContent: (String) -> Unit,
    onDismissOperation: () -> Unit
) {
    val listState = rememberLazyListState()

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

        // Search field
        item("search") {
            PackageSearchField(
                query = state.query,
                onSearch = onSearch,
                onClear = onClearSearch
            )
        }

        // Result count
        item("count") {
            Text(
                "${state.visibleRows.size} of ${state.allRows.size} items",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

        // Empty search result
        if (state.visibleRows.isEmpty() && state.query.isNotEmpty()) {
            item("no-results") {
                LearningEngineEmptyState(
                    title = "No results",
                    detail = "Try a different search term."
                )
            }
        }

        // Content rows — stable keys, immutable rows, no media load per row
        items(
            items = state.visibleRows,
            key = { "content-${it.contentId}" }
        ) { row ->
            PackageContentRow(
                row = row,
                onClick = { onOpenContent(row.contentId) }
            )
        }
    }
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
                    ) { Text("Dismiss") }
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
                    ) { Text("Dismiss") }
                }
            }
        }
        AndroidPackageOperationState.Idle -> { /* nothing */ }
    }
}
