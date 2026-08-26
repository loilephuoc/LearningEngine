package vn.loi.learning.android.family

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FamilyLegacyImportPreviewScreen(
    preview: LegacyImportPreview,
    repository: FamilyRepository,
    onBack: () -> Unit,
    onImportFinished: () -> Unit
) {
    var selectedCandidate by remember {
        mutableStateOf<LegacyImportCandidate?>(null)
    }

    val selectableRows = remember(preview) {
        FamilyLegacyImportPlanner.selectableRowIndexes(preview)
    }

    var selectedRowIndexes by remember(preview) {
        mutableStateOf(selectableRows)
    }

    var showImportConfirmation by remember {
        mutableStateOf(false)
    }

    var isImporting by remember {
        mutableStateOf(false)
    }

    var importError by remember {
        mutableStateOf<String?>(null)
    }

    var importedPeopleCount by remember {
        mutableStateOf<Int?>(null)
    }

    var importedFieldCount by remember {
        mutableStateOf(0)
    }

    val scope = rememberCoroutineScope()

    selectedCandidate?.let { candidate ->
        BackHandler {
            selectedCandidate = null
        }

        FamilyLegacyImportCandidateDetailScreen(
            candidate = candidate,
            onBack = {
                selectedCandidate = null
            }
        )

        return
    }

    val currentPlan = remember(
        preview,
        selectedRowIndexes
    ) {
        FamilyLegacyImportPlanner.build(
            preview = preview,
            selectedRowIndexes = selectedRowIndexes
        )
    }

    BackHandler(enabled = !isImporting) {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Xem trước dữ liệu AppSheet")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isImporting
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text =
                            "${selectedRowIndexes.size}/${selectableRows.size} người an toàn đã chọn",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (
                                !isImporting &&
                                currentPlan.peopleToCreate > 0
                            ) {
                                importError = null
                                showImportConfirmation = true
                            }
                        },
                        enabled =
                            !isImporting &&
                                    currentPlan.peopleToCreate > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color =
                                    MaterialTheme.colorScheme.onPrimary
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            Text("Đang nhập...")
                        } else {
                            Text(
                                "Nhập ${currentPlan.peopleToCreate} người"
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp),
            contentPadding =
                PaddingValues(vertical = 12.dp)
        ) {
            item {
                ElevatedCard(
                    Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Xem trước nhập dữ liệu",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Đã đọc: ${preview.rowsRead}"
                        )

                        Text(
                            "Sẵn sàng: ${preview.ready}"
                        )

                        Text(
                            "Có thể trùng: ${preview.possibleDuplicates}"
                        )

                        Text(
                            "Cảnh báo: ${preview.warnings}"
                        )

                        Text(
                            "Giá trị nhạy cảm bị chặn: ${preview.sensitiveBlocked}"
                        )

                        Text(
                            "Không hợp lệ/rỗng: ${preview.invalidOrEmpty}"
                        )

                        HorizontalDivider(
                            modifier =
                                Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            "Chỉ người ở trạng thái SẴN SÀNG mới có thể được chọn để nhập.",
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedRowIndexes =
                                selectableRows
                        },
                        enabled =
                            !isImporting &&
                                    selectableRows.isNotEmpty()
                    ) {
                        Text("Chọn tất cả an toàn")
                    }

                    TextButton(
                        onClick = {
                            selectedRowIndexes =
                                emptySet()
                        },
                        enabled =
                            !isImporting &&
                                    selectedRowIndexes.isNotEmpty()
                    ) {
                        Text("Bỏ chọn tất cả")
                    }
                }
            }

            items(
                preview.candidates,
                key = { it.rowIndex }
            ) { candidate ->
                val canImport =
                    candidate.rowIndex in selectableRows

                val checked =
                    candidate.rowIndex in
                            selectedRowIndexes

                OutlinedCard(
                    onClick = {
                        selectedCandidate =
                            candidate
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier =
                            Modifier.padding(14.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange =
                                if (
                                    canImport &&
                                    !isImporting
                                ) {
                                    { shouldSelect ->
                                        selectedRowIndexes =
                                            if (shouldSelect) {
                                                selectedRowIndexes +
                                                        candidate.rowIndex
                                            } else {
                                                selectedRowIndexes -
                                                        candidate.rowIndex
                                            }
                                    }
                                } else {
                                    null
                                },
                            enabled =
                                canImport &&
                                        !isImporting
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                statusLabel(
                                    candidate.status
                                ),
                                style =
                                    MaterialTheme.typography.labelMedium,
                                color =
                                    resolveStatusTone(
                                        candidate.status
                                    )
                            )

                            Text(
                                candidate.fullName
                                    ?: "Dòng ${candidate.rowIndex} chưa có tên",
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Text(
                                "${candidate.readyFields.size} trường an toàn · ${candidate.issues.size} vấn đề",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )

                            candidate.duplicate?.let {
                                Text(
                                    it.reasons.joinToString(),
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }

                            if (!canImport) {
                                Text(
                                    when (
                                        candidate.status
                                    ) {
                                        LegacyDuplicateStatus.REQUIRES_REVIEW ->
                                            "Không nhập tự động — cần xem lại"

                                        LegacyDuplicateStatus.POSSIBLE_DUPLICATE ->
                                            "Không nhập tự động — có thể trùng"

                                        LegacyDuplicateStatus.STRONG_DUPLICATE ->
                                            "Không nhập tự động — trùng chắc chắn"

                                        LegacyDuplicateStatus.NEW ->
                                            "Không đủ điều kiện nhập"
                                    },
                                    style =
                                        MaterialTheme.typography.labelSmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (
                            candidate.issues.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            importError?.let { message ->
                item {
                    ElevatedCard(
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.elevatedCardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.errorContainer
                            )
                    ) {
                        Text(
                            text = message,
                            modifier =
                                Modifier.padding(16.dp),
                            color =
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    if (showImportConfirmation) {
        val skippedFieldCount =
            currentPlan.blockedFieldsSkipped +
                    currentPlan.reviewFieldsSkipped +
                    currentPlan.ignoredFieldsSkipped

        AlertDialog(
            onDismissRequest = {
                if (!isImporting) {
                    showImportConfirmation = false
                }
            },
            title = {
                Text("Sắp nhập dữ liệu")
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${currentPlan.peopleToCreate} người mới"
                    )

                    Text(
                        "${currentPlan.safeFieldsToCreate} trường thông tin sẽ được ghi"
                    )

                    if (
                        currentPlan.reviewCandidatesSkipped >
                        0
                    ) {
                        Text(
                            "${currentPlan.reviewCandidatesSkipped} người cần xem lại — bỏ qua"
                        )
                    }

                    if (
                        currentPlan.duplicateCandidatesSkipped >
                        0
                    ) {
                        Text(
                            "${currentPlan.duplicateCandidatesSkipped} người trùng — bỏ qua"
                        )
                    }

                    if (
                        currentPlan.invalidCandidatesSkipped >
                        0
                    ) {
                        Text(
                            "${currentPlan.invalidCandidatesSkipped} dòng không hợp lệ — bỏ qua"
                        )
                    }

                    if (skippedFieldCount > 0) {
                        Text(
                            "$skippedFieldCount trường bị bỏ qua"
                        )
                    }

                    HorizontalDivider(
                        modifier =
                            Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        "Dữ liệu trùng và dữ liệu cần xem lại sẽ không được tự động hợp nhất hoặc ghi đè.",
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        "Các trường nhạy cảm bị chặn sẽ không được ghi.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirmation = false
                    },
                    enabled = !isImporting
                ) {
                    Text("HỦY")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isImporting) {
                            return@Button
                        }

                        showImportConfirmation = false
                        isImporting = true
                        importError = null

                        scope.launch {
                            runCatching {
                                FamilyLegacyImportRepositoryAdapter
                                    .commit(
                                        repository =
                                            repository,
                                        plan =
                                            currentPlan
                                    )
                            }.onSuccess { result ->
                                importedPeopleCount =
                                    result.peopleCreated

                                importedFieldCount =
                                    result.fieldsCreated
                            }.onFailure {
                                importError =
                                    "Không thể hoàn tất nhập dữ liệu. Vui lòng kiểm tra lại trước khi thử lại."
                            }

                            isImporting = false
                        }
                    },
                    enabled =
                        !isImporting &&
                                currentPlan.peopleToCreate > 0
                ) {
                    Text("NHẬP DỮ LIỆU")
                }
            }
        )
    }

    importedPeopleCount?.let { count ->
        AlertDialog(
            onDismissRequest = {
                // Không trở lại preview cũ sau khi đã import,
                // vì preview này được tạo trước khi repository thay đổi.
                onImportFinished()
            },
            title = {
                Text("Nhập dữ liệu hoàn tất")
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Đã nhập $count người."
                    )

                    Text(
                        "Đã tạo $importedFieldCount trường thông tin."
                    )

                    Text(
                        "Danh bạ đã được cập nhật. Nếu mở lại cùng tệp CSV, dữ liệu vừa nhập sẽ được kiểm tra trùng với dữ liệu hiện có.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onImportFinished
                ) {
                    Text("XEM DANH BẠ")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyLegacyImportCandidateDetailScreen(
    candidate: LegacyImportCandidate,
    onBack: () -> Unit
) {
    val presentation =
        remember(candidate) {
            FamilyLegacyImportCandidateDetailPresentation
                .build(candidate)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            presentation.title,
                            maxLines = 1
                        )

                        Text(
                            presentation.status,
                            style =
                                MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                "Quay lại bản xem trước"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp),
            contentPadding =
                PaddingValues(vertical = 12.dp)
        ) {
            item {
                ElevatedCard(
                    Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Tổng quan",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            "Sẽ nhập: ${presentation.safeCount} · Cần xem lại: ${presentation.reviewCount}"
                        )

                        Text(
                            "Bị chặn: ${presentation.blockedCount} · Xung đột: ${presentation.conflictCount}"
                        )

                        presentation
                            .duplicateReasons
                            .forEach {
                                Text(
                                    "• $it",
                                    color =
                                        MaterialTheme.colorScheme.tertiary
                                )
                            }

                        if (
                            presentation
                                .duplicateReasons
                                .isNotEmpty()
                        ) {
                            Text(
                                "Không tự hợp nhất hoặc ghi đè.",
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            detailSection(
                "SẼ NHẬP",
                presentation.section(
                    CandidateDetailSection.WILL_IMPORT
                )
            )

            detailSection(
                "CẦN XEM LẠI",
                presentation.section(
                    CandidateDetailSection.NEEDS_REVIEW
                )
            )

            detailSection(
                "BỊ CHẶN",
                presentation.section(
                    CandidateDetailSection.BLOCKED
                )
            )

            detailSection(
                "TRÙNG / XUNG ĐỘT",
                presentation.section(
                    CandidateDetailSection.COMPARISON
                )
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope
        .detailSection(
    title: String,
    fields: List<CandidateDetailFieldItem>
) {
    if (fields.isEmpty()) return

    item {
        Text(
            title,
            style =
                MaterialTheme.typography.titleSmall,
            fontWeight =
                FontWeight.Bold
        )
    }

    items(fields) { field ->
        OutlinedCard(
            Modifier.fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier.padding(14.dp),
                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    field.label,
                    fontWeight =
                        FontWeight.SemiBold
                )

                if (
                    field.section ==
                    CandidateDetailSection.COMPARISON
                ) {
                    Text(
                        "Hiện tại: ${field.existingValue}"
                    )

                    Text(
                        "CSV: ${field.incomingValue}"
                    )

                    Text(
                        field.displayValue,
                        color =
                            MaterialTheme.colorScheme.tertiary,
                        fontWeight =
                            FontWeight.Bold
                    )
                } else {
                    Text(
                        field.displayValue
                    )

                    field.targetType?.let {
                        Text(
                            "Đích: $it",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    field.reason?.let {
                        Text(
                            "Lý do: $it",
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveStatusTone(
    status: LegacyDuplicateStatus
) = when (status) {
    LegacyDuplicateStatus.NEW ->
        MaterialTheme.colorScheme.primary

    LegacyDuplicateStatus.POSSIBLE_DUPLICATE,
    LegacyDuplicateStatus.STRONG_DUPLICATE ->
        MaterialTheme.colorScheme.tertiary

    LegacyDuplicateStatus.REQUIRES_REVIEW ->
        MaterialTheme.colorScheme.error
}

private fun statusLabel(
    status: LegacyDuplicateStatus
): String = when (status) {
    LegacyDuplicateStatus.NEW ->
        "SẴN SÀNG"

    LegacyDuplicateStatus.POSSIBLE_DUPLICATE ->
        "CÓ THỂ TRÙNG"

    LegacyDuplicateStatus.STRONG_DUPLICATE ->
        "TRÙNG CHẮC CHẮN"

    LegacyDuplicateStatus.REQUIRES_REVIEW ->
        "CẦN XEM LẠI"
}