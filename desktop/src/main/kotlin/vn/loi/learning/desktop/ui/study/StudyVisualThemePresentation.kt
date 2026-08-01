package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LEBorderTokens
import vn.loi.learning.desktop.ui.theme.LEColors
import vn.loi.learning.desktop.ui.theme.LEElevationTokens

internal enum class StudyVisualFocusRole {
    QUESTION_CONTENT,
    ANSWER_CONTENT,
    MEANING,
    EXAMPLE,
    SCHEDULER,
    HEADER,
    METADATA,
    RATING_DOCK,
    RATING_ACTION
}

internal enum class StudyVisualEmphasis {
    PRIMARY,
    SECONDARY_PRIMARY,
    ACTION,
    SUPPORTING,
    EXPLANATORY,
    ORIENTATION,
    SECONDARY
}

internal enum class StudyContentTone { PRIMARY, SECONDARY, MUTED }

internal enum class StudyBorderProminence { NONE, SUBTLE, DEFAULT }

internal enum class StudyRestingElevation { FLAT, RAISED }

@Immutable
internal data class StudyVisualFocusPresentation(
    val role: StudyVisualFocusRole,
    val emphasis: StudyVisualEmphasis,
    val hierarchyWeight: Int,
    val surfaceVariant: LESurfaceVariant,
    val contentTone: StudyContentTone,
    val borderProminence: StudyBorderProminence,
    val restingElevation: StudyRestingElevation,
    val preservesSemanticRatingIdentity: Boolean = false,
    val selectedRatingPresentation: Boolean = false
)

internal object StudyVisualFocusResolver {
    @Suppress("UNUSED_PARAMETER")
    fun resolve(
        role: StudyVisualFocusRole,
        viewportClass: StudyViewportClass = StudyViewportClass.STANDARD
    ): StudyVisualFocusPresentation {
        // Responsive modes retain one semantic hierarchy; geometry remains in StudyVisualLayout.
        return when (role) {
            StudyVisualFocusRole.QUESTION_CONTENT,
            StudyVisualFocusRole.ANSWER_CONTENT -> presentation(
                role, StudyVisualEmphasis.PRIMARY, 700, LESurfaceVariant.ANSWER,
                StudyContentTone.PRIMARY, StudyBorderProminence.DEFAULT, StudyRestingElevation.RAISED
            )
            StudyVisualFocusRole.MEANING -> presentation(
                role, StudyVisualEmphasis.SECONDARY_PRIMARY, 600, LESurfaceVariant.MEANING,
                StudyContentTone.PRIMARY, StudyBorderProminence.SUBTLE, StudyRestingElevation.RAISED
            )
            StudyVisualFocusRole.RATING_DOCK -> presentation(
                role, StudyVisualEmphasis.ACTION, 500, LESurfaceVariant.RATING_DOCK,
                StudyContentTone.PRIMARY, StudyBorderProminence.SUBTLE, StudyRestingElevation.RAISED
            )
            StudyVisualFocusRole.RATING_ACTION -> presentation(
                role, StudyVisualEmphasis.ACTION, 500, LESurfaceVariant.RATING_DOCK,
                StudyContentTone.PRIMARY, StudyBorderProminence.NONE, StudyRestingElevation.FLAT,
                preservesSemanticRatingIdentity = true
            )
            StudyVisualFocusRole.EXAMPLE -> presentation(
                role, StudyVisualEmphasis.SUPPORTING, 400, LESurfaceVariant.EXAMPLE,
                StudyContentTone.SECONDARY, StudyBorderProminence.SUBTLE, StudyRestingElevation.FLAT
            )
            StudyVisualFocusRole.SCHEDULER -> presentation(
                role, StudyVisualEmphasis.EXPLANATORY, 300, LESurfaceVariant.SCHEDULER,
                StudyContentTone.SECONDARY, StudyBorderProminence.SUBTLE, StudyRestingElevation.FLAT
            )
            StudyVisualFocusRole.HEADER -> presentation(
                role, StudyVisualEmphasis.ORIENTATION, 200, LESurfaceVariant.STATISTICS,
                StudyContentTone.SECONDARY, StudyBorderProminence.SUBTLE, StudyRestingElevation.FLAT
            )
            StudyVisualFocusRole.METADATA -> presentation(
                role, StudyVisualEmphasis.SECONDARY, 100, LESurfaceVariant.SECONDARY,
                StudyContentTone.MUTED, StudyBorderProminence.NONE, StudyRestingElevation.FLAT
            )
        }
    }

    private fun presentation(
        role: StudyVisualFocusRole,
        emphasis: StudyVisualEmphasis,
        hierarchyWeight: Int,
        surfaceVariant: LESurfaceVariant,
        contentTone: StudyContentTone,
        borderProminence: StudyBorderProminence,
        restingElevation: StudyRestingElevation,
        preservesSemanticRatingIdentity: Boolean = false
    ) = StudyVisualFocusPresentation(
        role, emphasis, hierarchyWeight, surfaceVariant, contentTone, borderProminence,
        restingElevation, preservesSemanticRatingIdentity
    )
}

internal fun StudyVisualFocusPresentation.resolveBorder(
    borders: LEBorderTokens
): BorderStroke? = when (borderProminence) {
    StudyBorderProminence.NONE -> null
    StudyBorderProminence.SUBTLE -> borders.subtle
    StudyBorderProminence.DEFAULT -> borders.default
}

internal fun StudyVisualFocusPresentation.resolveElevation(
    elevation: LEElevationTokens
): Dp = when (restingElevation) {
    StudyRestingElevation.FLAT -> elevation.elevation0
    StudyRestingElevation.RAISED -> elevation.elevation1
}

internal fun StudyVisualFocusPresentation.resolveContentColor(colors: LEColors): Color =
    when (contentTone) {
        StudyContentTone.PRIMARY -> colors.textPrimary
        StudyContentTone.SECONDARY -> colors.textSecondary
        StudyContentTone.MUTED -> colors.textMuted
    }

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
    val answer = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.ANSWER_CONTENT).surfaceVariant
    val meaning = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.MEANING).surfaceVariant
    val example = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.EXAMPLE).surfaceVariant
    val scheduler = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.SCHEDULER).surfaceVariant
    val ratingDock = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.RATING_DOCK).surfaceVariant
}

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

internal enum class StudyExampleRowKind {
    ENGLISH,
    VIETNAMESE
}

@Immutable
internal data class StudyExampleInteractionStyle(
    val containerColor: Color,
    val contentColor: Color,
    val highlightColor: Color,
    val iconColor: Color,
    val borderColor: Color,
    val borderWidth: Dp
)

internal fun resolveStudyExampleInteractionStyle(
    colors: LEColors,
    borders: LEBorderTokens,
    kind: StudyExampleRowKind,
    enabled: Boolean,
    hovered: Boolean,
    pressed: Boolean,
    focused: Boolean,
    activeLoop: Boolean
): StudyExampleInteractionStyle {
    val container = when (kind) {
        StudyExampleRowKind.ENGLISH -> colors.surfacePrimary
        StudyExampleRowKind.VIETNAMESE -> colors.surfaceSecondary
    }
    val content = when (kind) {
        StudyExampleRowKind.ENGLISH -> colors.textPrimary
        StudyExampleRowKind.VIETNAMESE -> colors.textSecondary
    }
    val borderColor = when {
        focused -> colors.borderFocus
        activeLoop -> colors.accentPrimary
        hovered || pressed -> colors.borderMedium
        else -> colors.borderSubtle
    }
    return StudyExampleInteractionStyle(
        containerColor = container,
        contentColor = content,
        highlightColor = colors.danger,
        iconColor = when {
            !enabled -> colors.textMuted
            activeLoop -> colors.accentPrimary
            else -> colors.textSecondary
        },
        borderColor = borderColor,
        borderWidth = if (enabled && (focused || activeLoop)) borders.thick else borders.thin
    )
}
