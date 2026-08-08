package vn.loi.learning.android.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LearningSpacing {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 18.dp
    val section = 20.dp
    val screen = 16.dp
    val spacious = 28.dp
    val touchTarget = 48.dp
}

val LearningEngineShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object LearningElevation {
    val flat: Dp = 0.dp
    val card: Dp = 1.dp
    val raised: Dp = 3.dp
    val overlay: Dp = 6.dp
}

object LearningIconSize {
    val inline = 18.dp
    val navigation = 22.dp
    val action = 24.dp
    val card = 28.dp
    val hero = 40.dp
}

object LearningMotion {
    const val fastMillis = 120
    const val standardMillis = 240
    const val emphasizedMillis = 360
}

val LearningEngineTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 56.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

object LearningContentTypography {
    val vocabulary = LearningEngineTypography.headlineLarge
    val pronunciation = LearningEngineTypography.bodyMedium
    val meaning = LearningEngineTypography.titleLarge
    val example = LearningEngineTypography.bodyLarge
    val translation = LearningEngineTypography.bodyMedium
    val progressMetric = LearningEngineTypography.labelLarge
    val sectionTitle = LearningEngineTypography.titleLarge
}

object LearningTextRole {
    val brand = LearningEngineTypography.labelLarge
    val screenTitle = LearningEngineTypography.headlineMedium
    val sectionTitle = LearningEngineTypography.titleLarge
    val cardTitle = LearningEngineTypography.titleMedium
    val statistic = LearningEngineTypography.headlineSmall
    val metadata = LearningEngineTypography.bodyMedium
    val caption = LearningEngineTypography.labelMedium
    val navigation = LearningEngineTypography.labelMedium
}

enum class LearningStatusTone { SUCCESS, WARNING, ERROR, INFO, ACTIVE, DUE, OVERDUE, COMPLETED }
enum class LearningDifficultyTone { DIFFICULT, MEDIUM, EASY }

data class LearningFeedbackToken(
    val color: androidx.compose.ui.graphics.Color,
    val label: String,
    val iconDescription: String
)

fun LearningSemanticColors.feedback(tone: LearningDifficultyTone): LearningFeedbackToken = when (tone) {
    LearningDifficultyTone.DIFFICULT -> LearningFeedbackToken(difficult, "Difficult", "Needs more practice")
    LearningDifficultyTone.MEDIUM -> LearningFeedbackToken(medium, "Medium", "Developing confidence")
    LearningDifficultyTone.EASY -> LearningFeedbackToken(easy, "Easy", "Strong recall")
}
