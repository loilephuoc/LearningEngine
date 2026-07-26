package vn.loi.learning.desktop.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.*

@Composable
fun LEPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = LERadius.sm,
        colors = ButtonDefaults.buttonColors(
            containerColor = LEColors.primary,
            contentColor = LEColors.textOnPrimary
        ),
        contentPadding = PaddingValues(horizontal = LESpacing.md, vertical = LESpacing.sm),
        modifier = modifier.height(34.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.height(16.dp).width(16.dp))
                Spacer(modifier = Modifier.width(LESpacing.xs))
            }
            Text(text, style = LETypography.statusText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LESecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = LERadius.sm,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LEColors.textPrimary
        ),
        border = LEBorder.subtle,
        contentPadding = PaddingValues(horizontal = LESpacing.md, vertical = LESpacing.sm),
        modifier = modifier.height(34.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.height(16.dp).width(16.dp))
                Spacer(modifier = Modifier.width(LESpacing.xs))
            }
            Text(text, style = LETypography.statusText)
        }
    }
}

@Composable
fun LEDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = LERadius.sm,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LEColors.danger
        ),
        border = BorderStroke(1.dp, LEColors.danger.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = LESpacing.md, vertical = LESpacing.sm),
        modifier = modifier.height(34.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.height(16.dp).width(16.dp))
                Spacer(modifier = Modifier.width(LESpacing.xs))
            }
            Text(text, style = LETypography.statusText)
        }
    }
}

@Composable
fun LEIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(32.dp).width(32.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) LEColors.textPrimary else LEColors.textMuted,
            modifier = Modifier.height(18.dp).width(18.dp)
        )
    }
}
