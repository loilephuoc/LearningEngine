package vn.loi.learning.desktop.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.*

@Composable
fun LESearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search content..."
) {
    var rawText by remember(query.isEmpty()) { mutableStateOf(query) }

    Surface(
        shape = LERadius.md,
        color = LEColors.surface,
        border = LEBorder.subtle,
        tonalElevation = LEElevation.flat,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.md, vertical = LESpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = LEIcons.Search,
                contentDescription = "Search",
                tint = LEColors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(LESpacing.sm))
            TextField(
                value = rawText,
                onValueChange = { newText ->
                    rawText = newText
                    onQueryChanged(newText)
                },
                placeholder = {
                    Text(
                        placeholderText,
                        style = LETypography.secondaryMetadata,
                        color = LEColors.textMuted
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LETypography.fieldValue,
                modifier = Modifier.weight(1f)
            )
            if (rawText.isNotBlank()) {
                LEIconButton(
                    icon = LEIcons.Remove,
                    onClick = {
                        rawText = ""
                        onClearQuery()
                    },
                    contentDescription = "Clear search"
                )
            }
        }
    }
}

@Composable
fun LEFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = LERadius.sm,
        color = if (selected) LEColors.primarySoft else LEColors.surfaceElevated,
        border = if (selected) BorderStroke(1.dp, LEColors.primary) else LEBorder.subtle,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = LETypography.caption,
            color = if (selected) LEColors.primaryText else LEColors.textSecondary,
            modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = LESpacing.xs)
        )
    }
}
