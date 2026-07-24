package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import vn.loi.learning.desktop.ui.search.SearchKeyboardKey
import vn.loi.learning.desktop.ui.search.SearchOptionGroup
import vn.loi.learning.desktop.ui.search.SearchRefinementAction
import vn.loi.learning.desktop.ui.search.SearchRefinementBar
import vn.loi.learning.desktop.ui.search.SearchResultStatus
import vn.loi.learning.desktop.ui.search.SearchScopeCard
import vn.loi.learning.desktop.ui.search.presentSearchKeyboardShortcuts
import vn.loi.learning.desktop.ui.search.resolveSearchKeyboardAction
import vn.loi.learning.domain.library.model.InstalledPackageId

data class LessonGroupHeader(
    val name: String,
    val sections: List<LessonSectionHeader>
)

data class LessonSectionHeader(
    val name: String,
    val lessons: List<LessonBrowserItem>
)

fun groupLessonsHierarchically(lessons: List<LessonBrowserItem>): List<LessonGroupHeader> {
    val groupMap = LinkedHashMap<String, LinkedHashMap<String, MutableList<LessonBrowserItem>>>()

    for (item in lessons) {
        val groupName = item.group?.trim()?.takeIf { it.isNotBlank() } ?: "General"
        val sectionName = item.section?.trim()?.takeIf { it.isNotBlank() } ?: "Other Lessons"

        groupMap
            .getOrPut(groupName) { LinkedHashMap() }
            .getOrPut(sectionName) { mutableListOf() }
            .add(item)
    }

    return groupMap.map { (groupName, sectionMap) ->
        LessonGroupHeader(
            name = groupName,
            sections = sectionMap.map { (sectionName, items) ->
                LessonSectionHeader(
                    name = sectionName,
                    lessons = items
                )
            }
        )
    }
}

@Composable
fun LessonBrowserCard(
    uiState: LessonBrowserUiState,
    onClose: () -> Unit,
    onSelectLesson: (String) -> Unit,
    onClearLessonSelection: () -> Unit,
    onStartStudy: (PackageLessonSelection) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterChanged: (LessonBrowserFilter) -> Unit,
    onSortChanged: (LessonBrowserSort) -> Unit,
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
) {
    val searchFocusRequester = remember { FocusRequester() }
    val visibleLessons = remember(
        uiState.lessons,
        uiState.appliedQuery,
        uiState.filter,
        uiState.sort
    ) {
        projectLessons(uiState.lessons, uiState.appliedQuery, uiState.filter, uiState.sort)
    }
    val hierarchicalGroups = remember(visibleLessons) {
        groupLessonsHierarchically(visibleLessons)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = uiState.libraryName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.lessonCount} lessons · ${uiState.totalLearningItemCount} learning items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.installedPackageId?.let { pkgId ->
                        Text(
                            text = "Package ID: ${pkgId.value}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                OutlinedButton(onClick = onClose) { Text("Back to Library") }
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

            if (uiState.isEmpty) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "This package contains no lessons or content items.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (visibleLessons.isEmpty()) {
                SearchEmptyStateCard(
                    presentation = lessonBrowserEmptySearchPresentation(uiState),
                    onClearQuery = onClearQuery,
                    onResetView = resetView
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    hierarchicalGroups.forEach { groupHeader ->
                        item(key = "group_${groupHeader.name}") {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Group: ${groupHeader.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        groupHeader.sections.forEach { sectionHeader ->
                            item(key = "section_${groupHeader.name}_${sectionHeader.name}") {
                                Text(
                                    text = "Section: ${sectionHeader.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }

                            items(sectionHeader.lessons, key = LessonBrowserItem::id) { lesson ->
                                val isSelected = lesson.id == uiState.selectedLessonId
                                LessonRow(
                                    lesson = lesson,
                                    query = uiState.appliedQuery,
                                    isSelected = isSelected,
                                    thumbnailLoader = thumbnailLoader,
                                    onSelect = { onSelectLesson(lesson.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Start Lesson Action Bar
            val selectedInView = uiState.selectedLessonInView
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedInView != null) {
                            Text(
                                text = "Selected: ${selectedInView.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedInView.learningItemCount == 0) {
                                Text(
                                    text = "No learning items available for this lesson.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = "${selectedInView.learningItemCount} learning items ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (uiState.selectedLessonId != null) {
                            Text(
                                text = "Selected lesson is hidden by search filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Select a lesson to start learning.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val sel = uiState.selectedLessonInView ?: return@Button
                            val pkgId = uiState.installedPackageId
                                ?: InstalledPackageId(uiState.libraryId)
                            onStartStudy(
                                PackageLessonSelection(
                                    installedPackageId = pkgId,
                                    lessonId = sel.id,
                                    packageName = uiState.libraryName,
                                    lessonTitle = sel.title
                                )
                            )
                        },
                        enabled = uiState.isStartEnabled
                    ) {
                        Text("Start Lesson")
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
    isSelected: Boolean,
    thumbnailLoader: LessonThumbnailLoader,
    onSelect: () -> Unit
) {
    val accessibility = resolveLessonBrowserItemAccessibility(lesson)

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility.contentDescription + if (isSelected) " (Selected)" else ""
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LessonThumbnail(lesson.imagePath, thumbnailLoader)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightedSearchText(accessibility.title, query, fontWeight = FontWeight.SemiBold)
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (lesson.hasHierarchy) HighlightedSearchText(lesson.hierarchyPath, query)
                if (lesson.primaryText != lesson.title) HighlightedSearchText(lesson.primaryText, query)
                lesson.translatedText?.let { HighlightedSearchText(it, query) }
                Text(
                    text = "${lesson.type} · ${lesson.learningItemCount} learning items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
