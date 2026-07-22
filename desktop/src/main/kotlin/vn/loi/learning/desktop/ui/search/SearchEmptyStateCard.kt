package vn.loi.learning.desktop.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SearchEmptyStateCard(
    presentation: SearchEmptyStatePresentation,
    onClearQuery: () -> Unit,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = presentation.contentDescription
                }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(presentation.message)

            if (presentation.showClearQuery || presentation.showResetView) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (presentation.showClearQuery) {
                        OutlinedButton(
                            onClick = onClearQuery,
                            modifier = Modifier.semantics {
                                contentDescription = presentation.clearQueryDescription
                            }
                        ) {
                            Text("Clear search")
                        }
                    }

                    if (presentation.showResetView) {
                        Button(
                            onClick = onResetView,
                            modifier = Modifier.semantics {
                                contentDescription = presentation.resetViewDescription
                            }
                        ) {
                            Text("Reset view")
                        }
                    }
                }
            }
        }
    }
}
