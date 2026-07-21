package vn.loi.learning.desktop.ui.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DesktopLoadStateCard(
    state: DesktopLoadState,
    screenName: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentation =
        resolveDesktopLoadStatePresentation(
            state = state,
            screenName = screenName
        ) ?: return

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(
                    mergeDescendants = true
                ) {
                    liveRegion =
                        if (state is DesktopLoadState.Failed) {
                            LiveRegionMode.Assertive
                        } else {
                            LiveRegionMode.Polite
                        }

                    contentDescription =
                        presentation.contentDescription
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (state is DesktopLoadState.Failed) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state == DesktopLoadState.Loading) {
                CircularProgressIndicator()
            }

            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = presentation.description,
                style = MaterialTheme.typography.bodyMedium
            )

            presentation.actionLabel
                ?.let { actionLabel ->
                    Button(
                        onClick = onRetry
                    ) {
                        Text(actionLabel)
                    }
                }
        }
    }
}
