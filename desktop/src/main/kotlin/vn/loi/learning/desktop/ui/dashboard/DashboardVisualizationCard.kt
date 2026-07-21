package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Container dùng chung cho các Dashboard visualization.
 *
 * Khi có dữ liệu:
 * - giữ chiều cao tối thiểu đủ cho chart;
 * - dùng padding và spacing rộng hơn.
 *
 * Khi không có dữ liệu:
 * - dùng layout compact;
 * - tránh tạo khoảng trống dọc không cần thiết.
 */
@Composable
fun DashboardVisualizationCard(
    title: String,
    hasData: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val contentPadding =
        if (hasData) {
            20.dp
        } else {
            16.dp
        }

    val contentSpacing =
        if (hasData) {
            16.dp
        } else {
            8.dp
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (hasData) {
                        Modifier.defaultMinSize(
                            minHeight = 220.dp
                        )
                    } else {
                        Modifier
                    }
                ),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainer
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement =
                Arrangement.spacedBy(contentSpacing)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            content()
        }
    }
}