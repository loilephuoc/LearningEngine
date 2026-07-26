package vn.loi.learning.desktop.ui.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.*

@Composable
fun LEDragDropTarget(
    label: String,
    modifier: Modifier = Modifier,
    hintText: String? = null,
    onBrowseClick: (() -> Unit)? = null
) {
    Surface(
        color = LEColors.surfaceElevated,
        shape = LERadius.sm,
        modifier = modifier
            .fillMaxWidth()
            .border(LEBorder.dashed, LERadius.sm)
            .clickable { onBrowseClick?.invoke() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LESpacing.sm, horizontal = LESpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = LETypography.caption,
                    color = LEColors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (hintText != null) {
                    Text(
                        text = hintText,
                        style = LETypography.caption,
                        color = LEColors.textMuted
                    )
                }
            }
        }
    }
}
