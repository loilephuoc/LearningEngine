package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.search.HighlightedSearchText
import vn.loi.learning.desktop.ui.search.SearchEmptyStateCard
import vn.loi.learning.desktop.ui.search.SearchField
import vn.loi.learning.desktop.ui.search.SearchKeyboardAction
import vn.loi.learning.desktop.ui.search.SearchOptionGroup
import vn.loi.learning.desktop.ui.search.SearchKeyboardKey
import vn.loi.learning.desktop.ui.search.SearchRefinementBar
import vn.loi.learning.desktop.ui.search.SearchRefinementAction
import vn.loi.learning.desktop.ui.search.SearchResultStatus
import vn.loi.learning.desktop.ui.search.SearchScopeCard
import vn.loi.learning.desktop.ui.search.presentSearchKeyboardShortcuts
import vn.loi.learning.desktop.ui.search.resolveSearchKeyboardAction

@Composable
fun LessonBrowserCard(
    uiState: LessonBrowserUiState,
    onClose: () -> Unit,
    onSelectLesson: (String) -> Unit,
    onClearLessonSelection: () -> Unit,
    onStartStudy: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterChanged: (LessonBrowserFilter) -> Unit,
    onSortChanged: (LessonBrowserSort) -> Unit,
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
) {
    val searchFocusRequester = remember { FocusRequester() }
    val selected = uiState.selectedLesson
    val visibleLessons = remember(
        uiState.lessons,
        uiState.appliedQuery,
        uiState.filter,
        uiState.sort
    ) {
        projectLessons(uiState.lessons, uiState.appliedQuery, uiState.filter, uiState.sort)
    }
    val resetView = {
        onQueryChanged("")
        onFilterChanged(LessonBrowserFilter.ALL)
        onSortChanged(LessonBrowserSort.PACKAGE_ORDER)
    }

    Card(
        modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (selected != null) return@onPreviewKeyEvent false
                val key =
                    when (event.key) {
                        Key.F -> SearchKeyboardKey.F
                        Key.Escape -> SearchKeyboardKey.ESCAPE
                        else -> SearchKeyboardKey.OTHER
                    }
                when (
                    resolveSearchKeyboardAction(
                        key = key,
                        isKeyDown = event.type == KeyEventType.KeyDown,
                        controlPressed = event.isCtrlPressed,
                        hasQuery = uiState.query.isNotBlank(),
                        hasNonQueryRefinement =
                            uiState.filter != LessonBrowserFilter.ALL ||
                                uiState.sort != LessonBrowserSort.PACKAGE_ORDER
                    )
                ) {
                    SearchKeyboardAction.FOCUS_SEARCH -> {
                        searchFocusRequester.requestFocus()
                        true
                    }
                    SearchKeyboardAction.CLEAR_QUERY -> {
                        onClearQuery()
                        true
                    }
                    SearchKeyboardAction.RESET_VIEW -> {
                        resetView()
                        true
                    }
                    SearchKeyboardAction.NONE -> false
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selected == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = uiState.libraryName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(lessonBrowserSearchSummary(uiState).label)
                    }
                    OutlinedButton(onClick = onClose) { Text("Back") }
                }

                SearchField(
                    query = uiState.query,
                    label = "Search lessons",
                    summary = lessonBrowserSearchSummary(uiState),
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery,
                    focusRequester = searchFocusRequester,
                    keyboardPresentation = presentSearchKeyboardShortcuts("lessons"),
                    guidance = lessonBrowserSearchGuidance(uiState)
                )

                SearchResultStatus(lessonBrowserResultStatus(uiState))
                SearchScopeCard(lessonBrowserSearchScope(uiState))

                val refinements = uiState.refinementState()
                SearchRefinementBar(
                    presentation = lessonBrowserRefinementPresentation(uiState),
                    resetEnabled = !refinements.isDefault,
                    onAction = { action ->
                        when (action) {
                            SearchRefinementAction.CLEAR_QUERY -> onClearQuery()
                            SearchRefinementAction.RESET_FILTER -> onFilterChanged(LessonBrowserFilter.ALL)
                            SearchRefinementAction.RESET_SORT -> onSortChanged(LessonBrowserSort.PACKAGE_ORDER)
                        }
                    },
                    onReset = resetView
                )

                SearchOptionGroup(
                    presentation = lessonBrowserFilterPresentation(uiState),
                    onOptionSelected = { label ->
                        LessonBrowserFilter.entries.firstOrNull { it.label == label }?.let(onFilterChanged)
                    }
                )

                SearchOptionGroup(
                    presentation = lessonBrowserSortPresentation(uiState),
                    onOptionSelected = { label ->
                        LessonBrowserSort.entries.firstOrNull { it.label == label }?.let(onSortChanged)
                    }
                )

                if (visibleLessons.isEmpty()) {
                    SearchEmptyStateCard(
                        presentation = lessonBrowserEmptySearchPresentation(uiState),
                        onClearQuery = onClearQuery,
                        onResetView = resetView
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleLessons, key = LessonBrowserItem::id) { lesson ->
                            LessonRow(lesson, uiState.appliedQuery, thumbnailLoader) { onSelectLesson(lesson.id) }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = selected.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (selected.hasHierarchy) Text(selected.hierarchyPath)
                    Text(selected.primaryText)
                    selected.translatedText?.let { Text(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClearLessonSelection) { Text("Back") }
                        Button(onClick = { onStartStudy(selected.id) }) { Text("Start Study") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: LessonBrowserItem,
    query: String,
    thumbnailLoader: LessonThumbnailLoader,
    onOpen: () -> Unit
) {
    val accessibility = resolveLessonBrowserItemAccessibility(lesson)

    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = accessibility.contentDescription
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LessonThumbnail(lesson.imagePath, thumbnailLoader)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lesson.hasHierarchy) HighlightedSearchText(lesson.hierarchyPath, query)
            HighlightedSearchText(accessibility.title, query, fontWeight = FontWeight.SemiBold)
            if (lesson.primaryText != lesson.title) HighlightedSearchText(lesson.primaryText, query)
            lesson.translatedText?.let { HighlightedSearchText(it, query) }
            Text("${lesson.type} · ${lesson.learningItemCount} learning items")
            Button(onClick = onOpen) { Text("Open") }
            }
        }
    }
}
