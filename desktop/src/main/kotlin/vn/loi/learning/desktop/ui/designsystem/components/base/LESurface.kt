package vn.loi.learning.desktop.ui.designsystem.components.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun LESurface(
    variant: LESurfaceVariant,
    modifier: Modifier = Modifier,
    contentPadding: Dp? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = resolveSurfaceStyle(LETheme.colors, variant)
    Surface(
        modifier = modifier,
        color = style.containerColor,
        contentColor = style.contentColor,
        shape = LETheme.shapes.radiusL,
        border = LETheme.borders.subtle,
        shadowElevation = LETheme.elevation.elevation1
    ) {
        Column(
            modifier = Modifier.padding(contentPadding ?: LETheme.spacing.space5),
            content = content
        )
    }
}
