package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LEBorderTokens
import vn.loi.learning.desktop.ui.theme.LEColors

internal val studyRatingOrder: List<StudyActionControl> = listOf(
    StudyActionControl.REVIEW_AGAIN,
    StudyActionControl.REVIEW_HARD,
    StudyActionControl.REVIEW_GOOD,
    StudyActionControl.REVIEW_EASY
)

internal fun resolveStudyRatingVariant(control: StudyActionControl): LEButtonVariant =
    when (control) {
        StudyActionControl.REVIEW_AGAIN -> LEButtonVariant.RATING_AGAIN
        StudyActionControl.REVIEW_HARD -> LEButtonVariant.RATING_HARD
        StudyActionControl.REVIEW_GOOD -> LEButtonVariant.RATING_GOOD
        StudyActionControl.REVIEW_EASY -> LEButtonVariant.RATING_EASY
        else -> LEButtonVariant.PRIMARY
    }

internal object StudySurfaceRoles {
    val answer = LESurfaceVariant.ANSWER
    val meaning = LESurfaceVariant.MEANING
    val example = LESurfaceVariant.EXAMPLE
    val scheduler = LESurfaceVariant.SCHEDULER
    val ratingDock = LESurfaceVariant.RATING_DOCK
}

@Immutable
internal data class StudyPosBadgeStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

internal fun resolveStudyPosBadgeStyle(colors: LEColors): StudyPosBadgeStyle =
    StudyPosBadgeStyle(
        containerColor = colors.accentSoft,
        contentColor = colors.accentPrimary,
        borderColor = colors.borderMedium
    )

internal fun formatStudyPos(partOfSpeech: String): String = partOfSpeech.uppercase()

internal fun resolveStudyMeaningPos(partOfSpeech: String?): String? =
    partOfSpeech?.takeIf { it.isNotBlank() }

internal fun resolveStudyReadyStatusColor(colors: LEColors): Color = colors.textSecondary

@Immutable
internal data class StudyAnswerInteractionStyle(
    val containerColor: Color,
    val primaryContentColor: Color,
    val borderColor: Color,
    val borderWidth: Dp
)

internal fun resolveStudyAnswerInteractionStyle(
    colors: LEColors,
    borders: LEBorderTokens,
    enabled: Boolean,
    hovered: Boolean,
    pressed: Boolean,
    focused: Boolean,
    activeLoop: Boolean
): StudyAnswerInteractionStyle {
    // Hover/press intentionally retain the answer surface palette. This surface is clickable
    // only as an audio shortcut; changing its full background obscures learning content.
    val borderColor = when {
        focused -> colors.borderFocus
        activeLoop -> colors.accentPrimary
        hovered || pressed -> colors.borderMedium
        else -> colors.borderSubtle
    }
    return StudyAnswerInteractionStyle(
        containerColor = colors.surfacePrimary,
        primaryContentColor = if (activeLoop) colors.accentPrimary else colors.textPrimary,
        borderColor = borderColor,
        borderWidth = if (enabled && (focused || activeLoop)) borders.thick else borders.thin
    )
}
