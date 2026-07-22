package vn.loi.learning.desktop.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SearchScopeCard(
    presentation: SearchScopePresentation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = presentation.contentDescription
                },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = presentation.heading,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presentation.fields.forEach { field ->
                    AssistChip(onClick = {}, label = { Text(field) })
                }
            }
            Text(
                text = presentation.activeSummary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
