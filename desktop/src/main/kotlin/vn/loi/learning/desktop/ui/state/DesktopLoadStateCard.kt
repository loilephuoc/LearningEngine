package vn.loi.learning.desktop.ui.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButton
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme

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

    LESurface(
        variant = if (state is DesktopLoadState.Failed) {
            LESurfaceVariant.ERROR
        } else {
            LESurfaceVariant.SECONDARY
        },
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
    ) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space4)
        ) {
            if (state == DesktopLoadState.Loading) {
                CircularProgressIndicator()
            }

            Text(
                text = presentation.title,
                style = LETheme.typography.headlinePane,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = presentation.description,
                style = LETheme.typography.bodyDefinition
            )

            presentation.actionLabel
                ?.let { actionLabel ->
                    LEButton(
                        label = actionLabel,
                        onClick = onRetry
                    )
                }
        }
    }
}
