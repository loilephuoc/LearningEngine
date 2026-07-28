package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typographic Role Tokens for Learning Engine 2.0 (PLE-028A Specification).
 * Defines cognitive roles for text components across Study, Studio, Dashboard and Settings.
 */
@Immutable
data class LETypography(
    val displayWord: TextStyle,
    val headlinePane: TextStyle,
    val sectionTitle: TextStyle,
    val meaningPrimary: TextStyle,
    val bodyDefinition: TextStyle,
    val exampleEnglish: TextStyle,
    val exampleVietnamese: TextStyle,
    val metadataIpa: TextStyle,
    val metadataPos: TextStyle,
    val meaningPos: TextStyle,
    val schedulerRatingLabel: TextStyle,
    val schedulerIntervalHint: TextStyle,
    val ratingAction: TextStyle,
    val shortcutBadge: TextStyle,
    val fieldLabel: TextStyle,
    val fieldValue: TextStyle,
    val fieldValueEmphasized: TextStyle,
    val secondaryMetadata: TextStyle,
    val caption: TextStyle,
    val statusText: TextStyle,
    val metricLabel: TextStyle,
    val metricValue: TextStyle,
    val metricSubtitle: TextStyle
)

/**
 * Factory function creating [LETypography] bound to active [LEColors].
 */
fun createLETypography(colors: LEColors): LETypography = LETypography(
    displayWord = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = colors.textPrimary
    ),
    headlinePane = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = colors.textPrimary
    ),
    sectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = colors.textPrimary
    ),
    meaningPrimary = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        color = colors.textPrimary
    ),
    bodyDefinition = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = colors.textSecondary
    ),
    exampleEnglish = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = colors.textPrimary
    ),
    exampleVietnamese = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = colors.textSecondary
    ),
    metadataIpa = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = colors.textMuted
    ),
    metadataPos = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = colors.accentPrimary
    ),
    meaningPos = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = colors.accentPrimary
    ),
    schedulerRatingLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = colors.textPrimary
    ),
    schedulerIntervalHint = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = colors.textSecondary
    ),
    ratingAction = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = colors.textPrimary
    ),
    shortcutBadge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 12.sp,
        color = colors.textPrimary
    ),
    fieldLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = colors.textSecondary
    ),
    fieldValue = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = colors.textPrimary
    ),
    fieldValueEmphasized = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = colors.textPrimary
    ),
    secondaryMetadata = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = colors.textSecondary
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        color = colors.textMuted
    ),
    statusText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = colors.textPrimary
    ),
    metricLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = colors.textSecondary
    ),
    metricValue = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        color = colors.textPrimary
    ),
    metricSubtitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = colors.textMuted
    )
)
