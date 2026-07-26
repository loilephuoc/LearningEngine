package vn.loi.learning.desktop.ui.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import vn.loi.learning.desktop.ui.designsystem.*

@Composable
fun LECard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LEColors.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            content = content
        )
    }
}

@Composable
fun LEInspectorCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceElevated),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.none),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            content = content
        )
    }
}
