package vn.loi.learning.desktop.ui.browser.posreview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vn.loi.learning.application.partofspeech.PartOfSpeechNormalizer
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.LEPrimaryButton
import vn.loi.learning.desktop.ui.designsystem.components.LESecondaryButton

@Composable
fun PosBatchReviewDialog(
    state: PosBatchReviewState,
    availableScopes: Map<PosReviewScope, Int>,
    onSelectScope: (PosReviewScope) -> Unit,
    onToggleRowSelection: (contentId: String) -> Unit,
    onToggleAllFiltered: () -> Unit,
    onClearSelection: () -> Unit,
    onUpdateRowNewPos: (contentId: String, newPos: String) -> Unit,
    onAnalyzePos: () -> Unit,
    onResetDrafts: () -> Unit,
    onBatchSetSelectedPos: (newPos: String) -> Unit,
    onBatchSetFilteredPos: (newPos: String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onStatusFilterChanged: (PosReviewRowStatus) -> Unit,
    onRequestUnlockSelected: () -> Unit,
    onCancelUnlockConfirmation: () -> Unit,
    onConfirmUnlockSelected: () -> Unit,
    onRequestApply: () -> Unit,
    onCancelApplyConfirmation: () -> Unit,
    onConfirmApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = LERadius.lg,
            color = LEColors.surface,
            tonalElevation = LEElevation.modal,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Part of Speech (POS) Batch Review",
                            style = LETypography.paneTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Audit, review, auto-analyze, and batch update Part of Speech. Learning progress remains untouched.",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !state.isSubmitting
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // Scope Selector Bar & Analyze Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LEColors.surfaceElevated)
                        .padding(horizontal = LESpacing.lg, vertical = LESpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scope:",
                            style = LETypography.fieldLabel,
                            color = LEColors.textSecondary
                        )
                        availableScopes.forEach { (scopeOption, count) ->
                            val isSelected = state.scope == scopeOption
                            Surface(
                                shape = LERadius.sm,
                                color = if (isSelected) LEColors.primary else LEColors.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LEColors.primary else LEColors.borderSubtle
                                ),
                                modifier = Modifier
                                    .clip(LERadius.sm)
                                    .clickable(enabled = !state.isSubmitting) { onSelectScope(scopeOption) }
                            ) {
                                Text(
                                    text = "${scopeOption.label} ($count)",
                                    style = LETypography.caption,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) LEColors.surface else LEColors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Top Action: Analyze POS and Reset Drafts
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.analyzedCount > 0 || state.changedCount > 0) {
                            TextButton(
                                onClick = onResetDrafts,
                                enabled = !state.isSubmitting,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = LEColors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Drafts", style = LETypography.caption, color = LEColors.textSecondary)
                            }
                        }

                        Surface(
                            shape = LERadius.sm,
                            color = LEColors.primary,
                            modifier = Modifier
                                .height(32.dp)
                                .clip(LERadius.sm)
                                .clickable(enabled = !state.isSubmitting) { onAnalyzePos() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = LEColors.surface,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Analyze POS",
                                    style = LETypography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = LEColors.surface
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // POS Audit Summary Strip & Analysis Result Indicators
                val auditScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LESpacing.lg, vertical = 6.dp)
                        .horizontalScroll(auditScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audit (${state.totalCount} items):",
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.textSecondary
                    )
                    if (state.confirmedCount > 0) {
                        AuditChip(label = "Confirmed", count = state.confirmedCount, color = LEColors.success)
                    }
                    state.auditSummary.canonicalCounts.forEach { (pos, count) ->
                        AuditChip(label = pos, count = count, color = LEColors.primary)
                    }
                    if (state.auditSummary.missingCount > 0) {
                        AuditChip(
                            label = "Missing",
                            count = state.auditSummary.missingCount,
                            color = LEColors.warning
                        )
                    }
                    state.auditSummary.customUnknownCounts.forEach { (pos, count) ->
                        AuditChip(label = "$pos (Custom)", count = count, color = LEColors.info)
                    }

                    if (state.analyzedCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analysis (${state.analyzedCount}):",
                            style = LETypography.caption,
                            fontWeight = FontWeight.Bold,
                            color = LEColors.primary
                        )
                        AuditChip(label = "Suggested Changes", count = state.changedCount, color = LEColors.primary)
                        AuditChip(label = "High Confidence", count = state.highConfidenceCount, color = LEColors.success)
                        AuditChip(label = "Review", count = state.reviewConfidenceCount, color = LEColors.warning)
                        AuditChip(label = "Uncertain / No Suggestion", count = state.uncertainConfidenceCount, color = LEColors.textSecondary)
                        if (state.legacySentenceCount > 0) {
                            AuditChip(
                                label = "Legacy SENTENCE Unchanged",
                                count = state.legacySentenceUnchangedCount,
                                color = if (state.legacySentenceUnchangedCount > 0) LEColors.warning else LEColors.success
                            )
                        }
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // Search & Filter & Batch Assign Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LESpacing.lg, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Field with visible, high-contrast text and clear button
                        PosReviewSearchField(
                            query = state.searchQuery,
                            onQueryChanged = onSearchQueryChanged,
                            modifier = Modifier.width(230.dp)
                        )

                        // Status Filter Dropdown
                        PosStatusFilterDropdown(
                            selected = state.statusFilter,
                            onSelected = onStatusFilterChanged
                        )

                        // Selection summary chip & clear button
                        if (state.selectedCount > 0) {
                            Surface(
                                color = LEColors.primarySoft,
                                shape = LERadius.xs,
                                border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.primary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "${state.selectedCount} selected",
                                    style = LETypography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = LEColors.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            TextButton(
                                onClick = onClearSelection,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Clear", style = LETypography.caption, color = LEColors.textSecondary)
                            }
                        }

                        // Unlock Selected POS button
                        if (state.canUnlockSelected) {
                            Surface(
                                color = LEColors.warningContainer,
                                shape = LERadius.xs,
                                border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.warning.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(LERadius.xs)
                                    .clickable(enabled = !state.isSubmitting) { onRequestUnlockSelected() }
                            ) {
                                Text(
                                    text = "Unlock Selected (${state.selectedConfirmedCount})",
                                    style = LETypography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = LEColors.warning,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Two distinct batch actions: Set Selected vs Set All Filtered
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Set Selected
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Set Selected (${state.selectedCount}) to:",
                                style = LETypography.caption,
                                fontWeight = if (state.canSetSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.canSetSelected) LEColors.textPrimary else LEColors.textMuted
                            )
                            PosQuickSetDropdown(
                                onSelectPos = onBatchSetSelectedPos,
                                enabled = state.canSetSelected
                            )
                        }

                        // Set All Filtered
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Set All Filtered (${state.filteredRows.size}) to:",
                                style = LETypography.caption,
                                fontWeight = if (state.canSetFiltered) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.canSetFiltered) LEColors.textPrimary else LEColors.textMuted
                            )
                            PosQuickSetDropdown(
                                onSelectPos = onBatchSetFilteredPos,
                                enabled = state.canSetFiltered
                            )
                        }
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // Table Header with Header Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LEColors.surfaceElevated)
                        .padding(horizontal = LESpacing.lg, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) {
                        TriStateCheckbox(
                            state = when {
                                state.isAllFilteredSelected -> ToggleableState.On
                                state.isSomeFilteredSelected -> ToggleableState.Indeterminate
                                else -> ToggleableState.Off
                            },
                            onClick = onToggleAllFiltered,
                            enabled = state.filteredRows.isNotEmpty() && !state.isSubmitting,
                            colors = CheckboxDefaults.colors(
                                checkedColor = LEColors.primary,
                                checkmarkColor = LEColors.surface,
                                uncheckedColor = LEColors.borderSubtle
                            )
                        )
                    }
                    Text("#", style = LETypography.caption, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Text("Question / Meaning", style = LETypography.caption, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Current POS", style = LETypography.caption, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                    Text("New POS", style = LETypography.caption, fontWeight = FontWeight.Bold, modifier = Modifier.width(170.dp))
                    Text("Confidence", style = LETypography.caption, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                    Text("Status", style = LETypography.caption, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // Table Rows (Virtualized LazyColumn)
                Box(modifier = Modifier.weight(1f)) {
                    if (state.filteredRows.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items matching filter",
                                style = LETypography.caption,
                                color = LEColors.textMuted
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(
                                items = state.filteredRows,
                                key = { _, item -> item.contentId }
                            ) { index, row ->
                                val isSelected = row.contentId in state.selectedRowIds
                                PosReviewRow(
                                    index = index + 1,
                                    row = row,
                                    isSelected = isSelected,
                                    onToggleSelection = { onToggleRowSelection(row.contentId) },
                                    onNewPosSelected = { newPos -> onUpdateRowNewPos(row.contentId, newPos) },
                                    enabled = !state.isSubmitting
                                )
                                HorizontalDivider(color = LEColors.borderSubtle.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // Bottom Action Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LEColors.surfaceElevated)
                        .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.md)) {
                            Text(
                                text = "Scope: ${state.totalCount} items",
                                style = LETypography.caption,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Changed: ${state.changedCount}",
                                style = LETypography.caption,
                                color = if (state.changedCount > 0) LEColors.primary else LEColors.textSecondary,
                                fontWeight = if (state.changedCount > 0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Unchanged: ${state.unchangedCount}",
                                style = LETypography.caption,
                                color = LEColors.textSecondary
                            )
                            Text(
                                text = "Missing: ${state.missingCount}",
                                style = LETypography.caption,
                                color = if (state.missingCount > 0) LEColors.warning else LEColors.textSecondary
                            )
                            Text(
                                text = "Custom: ${state.customCount}",
                                style = LETypography.caption,
                                color = if (state.customCount > 0) LEColors.info else LEColors.textSecondary
                            )
                        }
                        Text(
                            text = "LearningItem changes: 0  •  ReviewEvent changes: 0  •  FSRS changes: 0",
                            style = LETypography.caption,
                            color = LEColors.textMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                        LESecondaryButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            enabled = !state.isSubmitting
                        )
                        LEPrimaryButton(
                            text = if (state.changedCount > 0) "Apply ${state.changedCount} Changes" else "Apply Changes",
                            onClick = onRequestApply,
                            enabled = state.canApply
                        )
                    }
                }
            }
        }

        // Confirmation Alert Dialog (Apply)
        if (state.showConfirmApply) {
            AlertDialog(
                onDismissRequest = { if (!state.isSubmitting) onCancelApplyConfirmation() },
                title = { Text("Confirm Batch POS Update", style = LETypography.paneTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                        Text(
                            text = "This will update Part of Speech for ${state.changedCount} Content items and mark them as User Confirmed.",
                            style = LETypography.fieldValue
                        )
                        Text(
                            text = "Learning progress, scheduling (FSRS), and study history will not be changed.",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )
                    }
                },
                confirmButton = {
                    LEPrimaryButton(
                        text = "Confirm & Apply",
                        onClick = onConfirmApply,
                        enabled = !state.isSubmitting
                    )
                },
                dismissButton = {
                    LESecondaryButton(
                        text = "Back",
                        onClick = onCancelApplyConfirmation,
                        enabled = !state.isSubmitting
                    )
                }
            )
        }

        // Confirmation Alert Dialog (Unlock)
        if (state.showConfirmUnlock) {
            AlertDialog(
                onDismissRequest = { if (!state.isSubmitting) onCancelUnlockConfirmation() },
                title = { Text("Unlock Confirmed POS", style = LETypography.paneTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                        Text(
                            text = "Unlock ${state.selectedConfirmedCount} confirmed Content items?",
                            style = LETypography.fieldValue
                        )
                        Text(
                            text = "This removes confirmation protection and allows Auto Analyze to evaluate and suggest changes for them again.",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )
                    }
                },
                confirmButton = {
                    LEPrimaryButton(
                        text = "Unlock POS",
                        onClick = onConfirmUnlockSelected,
                        enabled = !state.isSubmitting
                    )
                },
                dismissButton = {
                    LESecondaryButton(
                        text = "Cancel",
                        onClick = onCancelUnlockConfirmation,
                        enabled = !state.isSubmitting
                    )
                }
            )
        }
    }
}

@Composable
private fun PosReviewSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = LERadius.sm,
        color = LEColors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = LEColors.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                cursorBrush = SolidColor(LEColors.primary),
                textStyle = LETypography.fieldValue.copy(
                    fontSize = 12.sp,
                    color = LEColors.textPrimary,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Filter rows (text / POS)...",
                                style = LETypography.fieldValue.copy(fontSize = 12.sp),
                                color = LEColors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChanged("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = LEColors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditChip(label: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = LERadius.xs,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = "$label: $count",
            style = LETypography.caption,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PosStatusFilterDropdown(
    selected: PosReviewRowStatus,
    onSelected: (PosReviewRowStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            color = LEColors.surface,
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Status: ${selected.label}", style = LETypography.caption)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PosReviewRowStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label, style = LETypography.caption) },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PosQuickSetDropdown(
    onSelectPos: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val catalog = remember { PartOfSpeechNormalizer.knownCatalog.sorted() }

    Box {
        Surface(
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) LEColors.primary else LEColors.borderSubtle),
            color = if (enabled) LEColors.surface else LEColors.surfaceElevated,
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = enabled) { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Select POS...",
                    style = LETypography.caption,
                    color = if (enabled) LEColors.primary else LEColors.textMuted
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (enabled) LEColors.primary else LEColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            catalog.forEach { pos ->
                DropdownMenuItem(
                    text = { Text(pos, style = LETypography.caption) },
                    onClick = {
                        onSelectPos(pos)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PosReviewRow(
    index: Int,
    row: PosReviewRowItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onNewPosSelected: (String) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    isSelected -> LEColors.primarySoft.copy(alpha = 0.45f)
                    row.isChanged -> LEColors.primarySoft.copy(alpha = 0.18f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = enabled) { onToggleSelection() }
            .padding(horizontal = LESpacing.lg, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = LEColors.primary,
                    checkmarkColor = LEColors.surface,
                    uncheckedColor = LEColors.borderSubtle
                )
            )
        }

        Text(
            text = "$index",
            style = LETypography.caption,
            color = LEColors.textMuted,
            modifier = Modifier.width(36.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.question,
                style = LETypography.fieldValue,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (row.translation.isNotBlank() || row.answer.isNotBlank()) {
                Text(
                    text = listOf(row.answer, row.translation).filter(String::isNotBlank).joinToString("  •  "),
                    style = LETypography.caption,
                    color = LEColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Current POS Badge
        Box(modifier = Modifier.width(140.dp)) {
            if (row.originalPos.isBlank()) {
                Surface(
                    color = LEColors.warningContainer,
                    shape = LERadius.xs
                ) {
                    Text(
                        text = "Missing",
                        style = LETypography.caption,
                        color = LEColors.warning,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Surface(
                    color = if (row.isCustomOrUnknown) LEColors.info.copy(alpha = 0.12f) else LEColors.surfaceElevated,
                    shape = LERadius.xs,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (row.isCustomOrUnknown) LEColors.info.copy(alpha = 0.4f) else LEColors.borderSubtle
                    )
                ) {
                    Text(
                        text = if (row.isCustomOrUnknown) "${row.originalPos} (Custom)" else row.originalPos,
                        style = LETypography.caption,
                        color = if (row.isCustomOrUnknown) LEColors.info else LEColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // New POS Dropdown
        Box(modifier = Modifier.width(170.dp)) {
            RowPosDropdown(
                currentPos = row.originalPos,
                selectedPos = row.newPos,
                onPosSelected = onNewPosSelected,
                enabled = enabled
            )
        }

        // Confidence / Analyzer Indicator
        Box(modifier = Modifier.width(90.dp)) {
            if (row.isManualOverride) {
                Surface(color = LEColors.info.copy(alpha = 0.15f), shape = LERadius.xs) {
                    Text(
                        text = "Manual",
                        style = LETypography.caption,
                        color = LEColors.info,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            } else {
                when (row.confidence) {
                    PosConfidence.HIGH -> Surface(color = LEColors.success.copy(alpha = 0.15f), shape = LERadius.xs) {
                        Text(
                            text = "High",
                            style = LETypography.caption,
                            color = LEColors.success,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    PosConfidence.MEDIUM -> Surface(color = LEColors.warningContainer, shape = LERadius.xs) {
                        Text(
                            text = "Review",
                            style = LETypography.caption,
                            color = LEColors.warning,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    PosConfidence.UNCERTAIN -> Surface(color = LEColors.surfaceElevated, shape = LERadius.xs) {
                        Text(
                            text = "Uncertain",
                            style = LETypography.caption,
                            color = LEColors.textMuted,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    null -> {}
                }
            }
        }

        // Status
        Box(modifier = Modifier.width(80.dp)) {
            when (row.status) {
                PosReviewRowStatus.CONFIRMED -> Surface(color = LEColors.success.copy(alpha = 0.15f), shape = LERadius.xs) {
                    Text(
                        text = "Confirmed",
                        style = LETypography.caption,
                        color = LEColors.success,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                PosReviewRowStatus.CHANGED -> Surface(color = LEColors.primary, shape = LERadius.xs) {
                    Text(
                        text = "Changed",
                        style = LETypography.caption,
                        color = LEColors.surface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                PosReviewRowStatus.SAME -> Text("Same", style = LETypography.caption, color = LEColors.textMuted)
                PosReviewRowStatus.MISSING -> Surface(color = LEColors.warningContainer, shape = LERadius.xs) {
                    Text(
                        text = "Missing",
                        style = LETypography.caption,
                        color = LEColors.warning,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                PosReviewRowStatus.CUSTOM -> Surface(color = LEColors.info.copy(alpha = 0.15f), shape = LERadius.xs) {
                    Text(
                        text = "Custom",
                        style = LETypography.caption,
                        color = LEColors.info,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun RowPosDropdown(
    currentPos: String,
    selectedPos: String,
    onPosSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val canonicalCatalog = remember { PartOfSpeechNormalizer.knownCatalog.sorted() }
    val options = remember(currentPos, selectedPos) {
        val list = mutableListOf<String>()
        val currentTrimmed = currentPos.trim()
        if (currentTrimmed.isNotBlank() && currentTrimmed !in canonicalCatalog) {
            list += currentTrimmed
        }
        val selectedTrimmed = selectedPos.trim()
        if (selectedTrimmed.isNotBlank() && selectedTrimmed !in canonicalCatalog && selectedTrimmed !in list) {
            list += selectedTrimmed
        }
        list += canonicalCatalog
        list
    }

    Box {
        Surface(
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            color = LEColors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = enabled) { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedPos.ifBlank { "(None)" },
                    style = LETypography.caption,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selectedPos.isBlank()) LEColors.textMuted else LEColors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = LEColors.textMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { pos ->
                DropdownMenuItem(
                    text = {
                        val isCustom = pos !in canonicalCatalog
                        Text(
                            text = if (isCustom) "$pos (Custom)" else pos,
                            style = LETypography.caption,
                            fontWeight = if (pos == selectedPos) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onPosSelected(pos)
                        expanded = false
                    }
                )
            }
        }
    }
}
