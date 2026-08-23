package vn.loi.learning.desktop.tts.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dedicated visual system and typography scale for Desktop Batch TTS dialogs and controls.
 * Ensures comfortable legibility and density at standard desktop viewing distances and 100% display scaling.
 */
object BatchTtsUiScale {

    // Typography
    val dialogTitle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp
    )

    val sectionHeading = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp
    )

    val subsectionHeading = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp
    )

    val controlPrimary = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    )

    val controlSecondary = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    )

    val previewText = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp
    )

    val previewSource = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    )

    val metricValue = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp
    )

    val metricValueLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 38.sp
    )

    val metricLabel = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    )

    val buttonLabel = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp
    )

    val buttonLabelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp
    )

    val body = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp
    )

    val badge = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp
    )

    // Component Dimensions
    val dialogWidth: Dp = 1060.dp
    val dialogHeight: Dp = 760.dp

    val pickerDialogWidth: Dp = 640.dp
    val pickerDialogHeight: Dp = 580.dp

    val buttonHeightSmall: Dp = 32.dp
    val buttonHeightStandard: Dp = 38.dp
    val buttonHeightPrimary: Dp = 44.dp

    val voiceAnchorHeight: Dp = 48.dp
    val fallbackRowMinHeight: Dp = 46.dp

    val cardPadding: Dp = 14.dp
    val sectionSpacing: Dp = 14.dp
    val itemSpacing: Dp = 8.dp
}
