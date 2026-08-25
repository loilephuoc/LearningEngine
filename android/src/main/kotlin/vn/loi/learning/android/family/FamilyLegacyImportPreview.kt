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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FamilyLegacyImportPreviewScreen(preview: LegacyImportPreview, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<LegacyImportCandidate?>(null) }
    selected?.let { candidate ->
        BackHandler { selected = null }
        FamilyLegacyImportCandidateDetailScreen(candidate = candidate, onBack = { selected = null })
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xem trước dữ liệu AppSheet") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Chỉ xem trước — không ghi dữ liệu", fontWeight = FontWeight.Bold)
                        Text("Đã đọc: ${preview.rowsRead}")
                        Text("Sẵn sàng: ${preview.ready}")
                        Text("Có thể trùng: ${preview.possibleDuplicates}")
                        Text("Cảnh báo: ${preview.warnings}")
                        Text("Giá trị nhạy cảm bị chặn: ${preview.sensitiveBlocked}")
                        Text("Không hợp lệ/rỗng: ${preview.invalidOrEmpty}")
                    }
                }
            }
            items(preview.candidates, key = { it.rowIndex }) { candidate ->
                OutlinedCard(onClick = { selected = candidate }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(statusLabel(candidate.status), style = MaterialTheme.typography.labelMedium, color = statusColor(candidate.status))
                            Text(candidate.fullName ?: "Dòng ${candidate.rowIndex} chưa có tên", fontWeight = FontWeight.SemiBold)
                            Text("${candidate.readyFields.size} trường an toàn · ${candidate.issues.size} vấn đề", style = MaterialTheme.typography.bodySmall)
                            candidate.duplicate?.let { Text(it.reasons.joinToString(), style = MaterialTheme.typography.bodySmall) }
                        }
                        if (candidate.issues.isNotEmpty()) Icon(Icons.Default.Warning, contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyLegacyImportCandidateDetailScreen(candidate: LegacyImportCandidate, onBack: () -> Unit) {
    val presentation = remember(candidate) { FamilyLegacyImportCandidateDetailPresentation.build(candidate) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text(presentation.title, maxLines = 1); Text(presentation.status, style = MaterialTheme.typography.labelMedium) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại bản xem trước") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tổng quan", fontWeight = FontWeight.Bold)
                        Text("Sẽ nhập: ${presentation.safeCount} · Cần xem lại: ${presentation.reviewCount}")
                        Text("Bị chặn: ${presentation.blockedCount} · Xung đột: ${presentation.conflictCount}")
                        presentation.duplicateReasons.forEach { Text("• $it", color = MaterialTheme.colorScheme.tertiary) }
                        if (presentation.duplicateReasons.isNotEmpty()) Text("Không tự hợp nhất hoặc ghi đè.", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            detailSection("SẼ NHẬP", presentation.section(CandidateDetailSection.WILL_IMPORT))
            detailSection("CẦN XEM LẠI", presentation.section(CandidateDetailSection.NEEDS_REVIEW))
            detailSection("BỊ CHẶN", presentation.section(CandidateDetailSection.BLOCKED))
            detailSection("TRÙNG / XUNG ĐỘT", presentation.section(CandidateDetailSection.COMPARISON))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailSection(
    title: String,
    fields: List<CandidateDetailFieldItem>
) {
    if (fields.isEmpty()) return
    item { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
    items(fields) { field ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(field.label, fontWeight = FontWeight.SemiBold)
                if (field.section == CandidateDetailSection.COMPARISON) {
                    Text("Hiện tại: ${field.existingValue}")
                    Text("CSV: ${field.incomingValue}")
                    Text(field.displayValue, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                } else {
                    Text(field.displayValue)
                    field.targetType?.let { Text("Đích: $it", style = MaterialTheme.typography.bodySmall) }
                    field.reason?.let { Text("Lý do: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: LegacyDuplicateStatus) = when (status) {
    LegacyDuplicateStatus.NEW -> MaterialTheme.colorScheme.primary
    LegacyDuplicateStatus.POSSIBLE_DUPLICATE, LegacyDuplicateStatus.STRONG_DUPLICATE -> MaterialTheme.colorScheme.tertiary
    LegacyDuplicateStatus.REQUIRES_REVIEW -> MaterialTheme.colorScheme.error
}

private fun statusLabel(status: LegacyDuplicateStatus): String = when (status) {
    LegacyDuplicateStatus.NEW -> "SẴN SÀNG"
    LegacyDuplicateStatus.POSSIBLE_DUPLICATE -> "CÓ THỂ TRÙNG"
    LegacyDuplicateStatus.STRONG_DUPLICATE -> "TRÙNG CHẮC CHẮN"
    LegacyDuplicateStatus.REQUIRES_REVIEW -> "CẦN XEM LẠI"
}
