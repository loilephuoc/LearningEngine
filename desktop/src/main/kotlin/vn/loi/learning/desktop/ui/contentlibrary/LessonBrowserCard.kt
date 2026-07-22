package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.search.SearchEmptyStateCard
import vn.loi.learning.desktop.ui.search.SearchField
import vn.loi.learning.desktop.ui.search.SearchRefinementBar

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
    modifier: Modifier = Modifier
) {
    val selected = uiState.selectedLesson
    val resetView = {
        onQueryChanged("")
        onFilterChanged(LessonBrowserFilter.ALL)
        onSortChanged(LessonBrowserSort.PACKAGE_ORDER)
    }

    Card(modifier.fillMaxWidth()) {
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
                    OutlinedButton(onClick = onClose) {
                        Text("Back")
                    }
                }

                SearchField(
                    query = uiState.query,
                    label = "Search lessons",
                    summary = lessonBrowserSearchSummary(uiState),
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery
                )

                val refinements = uiState.refinementState()
                SearchRefinementBar(
                    presentation = lessonBrowserRefinementPresentation(uiState),
                    resetEnabled = !refinements.isDefault,
                    onReset = resetView
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LessonBrowserFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { onFilterChanged(filter) },
                            label = { Text(filter.label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LessonBrowserSort.entries.forEach { sort ->
                        FilterChip(
                            selected = uiState.sort == sort,
                            onClick = { onSortChanged(sort) },
                            label = { Text(sort.label) }
                        )
                    }
                }

                if (uiState.visibleLessons.isEmpty()) {
                    SearchEmptyStateCard(
                        presentation = lessonBrowserEmptySearchPresentation(uiState),
                        onClearQuery = onClearQuery,
                        onResetView = resetView
                    )
                } else {
                    uiState.visibleLessons.forEach { lesson ->
                        LessonRow(lesson) {
                            onSelectLesson(lesson.id)
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
                    if (selected.hasHierarchy) {
                        Text(selected.hierarchyPath)
                    }
                    Text(selected.primaryText)
                    selected.translatedText?.let { translatedText ->
                        Text(translatedText)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClearLessonSelection) {
                            Text("Back")
                        }
                        Button(onClick = { onStartStudy(selected.id) }) {
                            Text("Start Study")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: LessonBrowserItem,
    onOpen: () -> Unit
) {
    val accessibility = resolveLessonBrowserItemAccessibility(lesson)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibility.contentDescription
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (lesson.hasHierarchy) {
                Text(lesson.hierarchyPath)
            }
            Text(accessibility.title, fontWeight = FontWeight.SemiBold)
            if (lesson.primaryText != lesson.title) {
                Text(lesson.primaryText)
            }
            lesson.translatedText?.let { translatedText ->
                Text(translatedText)
            }
            Text("${lesson.type} · ${lesson.learningItemCount} learning items")
            Button(onClick = onOpen) {
                Text("Open")
            }
        }
    }
}
