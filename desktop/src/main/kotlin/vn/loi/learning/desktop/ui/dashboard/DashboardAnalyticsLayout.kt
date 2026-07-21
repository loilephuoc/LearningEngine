package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Responsive layout dùng chung cho các Dashboard analytics section.
 *
 * Cửa sổ rộng:
 * - visualization bên trái;
 * - metric summary bên phải.
 *
 * Cửa sổ hẹp:
 * - visualization phía trên;
 * - metric summary phía dưới.
 *
 * Không dùng IntrinsicSize vì các thành phần BoxWithConstraints
 * bên trong được xây dựng trên SubcomposeLayout.
 */
@Composable
fun DashboardAnalyticsLayout(
    visualization: @Composable (Modifier) -> Unit,
    metrics: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        if (maxWidth >= 1_000.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                visualization(
                    Modifier.weight(1.45f)
                )

                metrics(
                    Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                visualization(
                    Modifier.fillMaxWidth()
                )

                metrics(
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}