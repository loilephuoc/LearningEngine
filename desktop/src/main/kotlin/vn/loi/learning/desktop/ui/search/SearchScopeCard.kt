package vn.loi.learning.desktop.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun SearchScopeCard(
    presentation: SearchScopePresentation,
    modifier: Modifier = Modifier
) {
    LESurface(
        variant = LESurfaceVariant.SECONDARY,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = presentation.contentDescription
                },
    ) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space3)
        ) {
            Text(
                text = presentation.heading,
                style = LETheme.typography.sectionTitle,
                modifier = Modifier.semantics { heading() }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space3)) {
                presentation.fields.forEach { field ->
                    AssistChip(onClick = {}, label = { Text(field) })
                }
            }
            Text(
                text = presentation.activeSummary,
                style = LETheme.typography.secondaryMetadata
            )
        }
    }
}
