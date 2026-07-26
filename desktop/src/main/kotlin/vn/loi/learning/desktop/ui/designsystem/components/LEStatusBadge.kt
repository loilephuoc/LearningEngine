package vn.loi.learning.desktop.ui.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.*

enum class StatusBadgeVariant {
    Present,
    Missing,
    Valid,
    Warning,
    NotEvaluated
}

@Composable
fun LEStatusBadge(
    variant: StatusBadgeVariant,
    modifier: Modifier = Modifier,
    customText: String? = null
) {
    val (bgColor, textColor, defaultText, icon) = when (variant) {
        StatusBadgeVariant.Present -> Quad(LEColors.successContainer, LEColors.successText, "Present", LEIcons.Success)
        StatusBadgeVariant.Valid -> Quad(LEColors.successContainer, LEColors.successText, "Valid", LEIcons.Success)
        StatusBadgeVariant.Missing -> Quad(LEColors.dangerContainer, LEColors.dangerText, "Missing", LEIcons.Missing)
        StatusBadgeVariant.Warning -> Quad(LEColors.warningContainer, LEColors.warningText, "Warning", LEIcons.Warning)
        StatusBadgeVariant.NotEvaluated -> Quad(LEColors.surfaceElevated, LEColors.textMuted, "Not Evaluated", null)
    }

    Surface(
        color = bgColor,
        shape = LERadius.sm,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = LESpacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(LESpacing.xs))
            }
            Text(
                text = customText ?: defaultText,
                style = LETypography.caption,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
