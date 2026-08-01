package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme

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
    val accessibility =
        resolveDashboardVisualizationAccessibility(
            title = title,
            hasData = hasData
        )

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

    LESurface(
        variant = LESurfaceVariant.SECONDARY,
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
                )
                .semantics {
                    contentDescription =
                        accessibility.contentDescription
                },
        contentPadding = LETheme.spacing.space0,
        border = null,
        shadowElevation = LETheme.elevation.elevation0
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
                text = accessibility.title,
                modifier =
                    Modifier.semantics {
                        heading()
                    },
                style = LETheme.typography.sectionTitle,
                color = LETheme.colors.textPrimary
            )

            content()
        }
    }
}
