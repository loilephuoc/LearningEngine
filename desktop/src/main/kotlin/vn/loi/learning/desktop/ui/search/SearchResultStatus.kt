package vn.loi.learning.desktop.ui.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SearchResultStatus(
    presentation: SearchResultStatusPresentation,
    modifier: Modifier = Modifier
) {
    Text(
        text = presentation.label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (presentation.isFiltered) FontWeight.SemiBold else FontWeight.Normal,
        modifier = modifier.semantics {
            contentDescription = presentation.contentDescription
            liveRegion = LiveRegionMode.Polite
        }
    )
}
