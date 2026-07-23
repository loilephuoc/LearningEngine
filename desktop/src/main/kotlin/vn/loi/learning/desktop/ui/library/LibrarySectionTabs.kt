package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibrarySectionTabs(
    selectedSection: LibrarySection,
    onSelectSection: (LibrarySection) -> Unit,
    counts: Map<LibrarySection, Int>,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedSection.ordinal,
        edgePadding = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        LibrarySection.entries.forEach { section ->
            val count = counts[section]
            val title = if (count != null && section != LibrarySection.OVERVIEW) {
                "${section.label} ($count)"
            } else {
                section.label
            }
            Tab(
                selected = selectedSection == section,
                onClick = { onSelectSection(section) },
                text = { Text(text = title) }
            )
        }
    }
}
