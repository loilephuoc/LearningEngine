package vn.loi.learning.desktop.ui.reviewhistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.search.*
import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.DesktopLoadStateCard
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun ReviewHistoryScreen(
    uiState: ReviewHistoryUiState,
    onRetry: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterChanged: (ReviewHistoryFilter) -> Unit,
    onSortChanged: (ReviewHistorySort) -> Unit,
    onTabChanged: (ReviewCenterTab) -> Unit,
    hasActiveSession: Boolean,
    quickReviewPreparing: Boolean,
    onContinueSession: () -> Unit,
    onReviewAgainHard: () -> Unit,
    onReviewLearned: () -> Unit,
    onOpenStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ôn tập", style = LETheme.typography.headlinePane, fontWeight = FontWeight.Bold)
            Text("Ôn đúng phần bạn cần, hoặc xem lại lịch sử", color = LETheme.colors.textSecondary)
        }
        PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            Tab(uiState.selectedTab == ReviewCenterTab.QUICK_REVIEW, { onTabChanged(ReviewCenterTab.QUICK_REVIEW) }, text = { Text("Ôn nhanh") })
            Tab(uiState.selectedTab == ReviewCenterTab.HISTORY, { onTabChanged(ReviewCenterTab.HISTORY) }, text = { Text("Lịch sử") })
        }
        when (uiState.selectedTab) {
            ReviewCenterTab.QUICK_REVIEW -> QuickReview(hasActiveSession, quickReviewPreparing, onContinueSession, onReviewAgainHard, onReviewLearned, onOpenStudy)
            ReviewCenterTab.HISTORY -> History(uiState, onRetry, onQueryChanged, onClearQuery, onFilterChanged, onSortChanged)
        }
    }
}

@Composable private fun QuickReview(hasActiveSession: Boolean, preparing: Boolean, onContinue: () -> Unit, onAgainHard: () -> Unit, onLearned: () -> Unit, onOpenStudy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ôn nhanh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 820.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReviewAction("Tiếp tục phiên đang học", "Quay lại đúng phiên hiện tại", Icons.Outlined.PlayArrow, hasActiveSession, onContinue, Modifier.weight(1f))
                    ReviewAction("Ôn những từ Again / Hard", if (preparing) "Đang chuẩn bị..." else "Dùng lựa chọn chuẩn từ màn Học", Icons.Outlined.Replay, !preparing, onAgainHard, Modifier.weight(1f))
                    ReviewAction("Ôn lại tất cả đã học", if (preparing) "Đang chuẩn bị..." else "Dùng phạm vi đã học hiện có", Icons.Outlined.School, !preparing, onLearned, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReviewAction("Tiếp tục phiên đang học", "Quay lại đúng phiên hiện tại", Icons.Outlined.PlayArrow, hasActiveSession, onContinue)
                    ReviewAction("Ôn những từ Again / Hard", if (preparing) "Đang chuẩn bị..." else "Dùng lựa chọn chuẩn từ màn Học", Icons.Outlined.Replay, !preparing, onAgainHard)
                    ReviewAction("Ôn lại tất cả đã học", if (preparing) "Đang chuẩn bị..." else "Dùng phạm vi đã học hiện có", Icons.Outlined.School, !preparing, onLearned)
                }
            }
        }
        OutlinedButton(onClick = onOpenStudy) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Mở màn Học")
        }
    }
}

@Composable private fun ReviewAction(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, action: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = action,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 112.dp).alpha(if (enabled) 1f else .55f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = .22f) else MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, hoveredElevation = 3.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable private fun History(state: ReviewHistoryUiState, onRetry: () -> Unit, onQuery: (String) -> Unit, onClear: () -> Unit, onFilter: (ReviewHistoryFilter) -> Unit, onSort: (ReviewHistorySort) -> Unit) {
    val focus = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        val key = when (event.key) { Key.F -> SearchKeyboardKey.F; Key.Escape -> SearchKeyboardKey.ESCAPE; else -> SearchKeyboardKey.OTHER }
        when (resolveSearchKeyboardAction(key, event.type == KeyEventType.KeyDown, event.isCtrlPressed, state.query.isNotBlank(), state.filter != ReviewHistoryFilter.ALL || state.sort != ReviewHistorySort.NEWEST)) {
            SearchKeyboardAction.FOCUS_SEARCH -> { focus.requestFocus(); true }
            SearchKeyboardAction.CLEAR_QUERY -> { onClear(); true }
            SearchKeyboardAction.RESET_VIEW -> { onQuery(""); onFilter(ReviewHistoryFilter.ALL); onSort(ReviewHistorySort.NEWEST); true }
            SearchKeyboardAction.NONE -> false
        }
    }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DesktopLoadStateCard(state.loadState, "Lịch sử ôn tập", onRetry)
        if (state.loadState != DesktopLoadState.Loading) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Lịch sử", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("${state.visibleItems.size} kết quả")
            }
            SearchField(
                query = state.query,
                label = "Tìm trong lịch sử",
                summary = reviewHistorySearchSummary(state),
                onQueryChanged = onQuery,
                onClearQuery = onClear,
                focusRequester = focus,
                keyboardPresentation = null,
                guidance = null
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewHistoryFilter.entries.forEach { filter -> FilterChip(state.filter == filter, { onFilter(filter) }, { Text(if (filter == ReviewHistoryFilter.ALL) "Tất cả" else filter.label) }) }
                Spacer(Modifier.weight(1f))
                ReviewHistorySort.entries.forEach { sort -> FilterChip(state.sort == sort, { onSort(sort) }, { Text(sort.viLabel()) }) }
            }
            if (state.visibleItems.isEmpty()) Text("Không có lượt ôn phù hợp.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(state.visibleItems) { DenseRow(it, state.query) } }
        }
    }
}

private fun ReviewHistorySort.viLabel() = when (this) {
    ReviewHistorySort.NEWEST -> "Mới nhất"; ReviewHistorySort.OLDEST -> "Cũ nhất"; ReviewHistorySort.RATING -> "Rating"; ReviewHistorySort.RESPONSE_TIME -> "Thời gian trả lời"
}

@Composable private fun DenseRow(item: ReviewHistoryItemUi, query: String) {
    Card(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = resolveReviewHistoryItemContentDescription(item) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            HighlightedSearchText(item.reviewedAt, query, Modifier.weight(1.5f))
            Text(
                item.rating,
                modifier = Modifier.weight(.8f),
                fontWeight = FontWeight.SemiBold,
                color = ratingColor(item.rating)
            )
            Metric("Phản hồi", item.responseTime, query, Modifier.weight(1f))
            Metric("Stability", item.stability, query, Modifier.weight(1f))
            Metric("Difficulty", item.difficulty, query, Modifier.weight(1f))
        }
    }
}

@Composable private fun Metric(label: String, value: String, query: String, modifier: Modifier) { Column(modifier) { Text(label, style = MaterialTheme.typography.labelSmall); HighlightedSearchText(value, query) } }

@Composable private fun ratingColor(rating: String) = when (rating.uppercase()) {
    "AGAIN" -> MaterialTheme.colorScheme.error
    "HARD" -> MaterialTheme.colorScheme.tertiary
    "GOOD" -> MaterialTheme.colorScheme.primary
    "EASY" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurface
}
