package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.Dp
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LEBorderTokens
import vn.loi.learning.desktop.ui.theme.LEElevationTokens

internal enum class StudySurfaceStage { DISCOVERY, UNDERSTANDING }

internal enum class StudySurfaceRole {
    HERO,
    HERO_SUPPORT,
    PRIMARY_SUPPORT,
    SECONDARY_PRIMARY,
    SUPPORTING,
    UTILITY,
    EXPLANATORY,
    ACTION
}

internal fun StudySurfacePresentation.resolveBorder(borders: LEBorderTokens): BorderStroke? =
    when (borderProminence) {
        StudyBorderProminence.NONE -> null
        StudyBorderProminence.SUBTLE -> borders.subtle
        StudyBorderProminence.DEFAULT -> borders.default
    }

internal fun StudySurfacePresentation.resolveElevation(elevation: LEElevationTokens): Dp =
    when (restingElevation) {
        StudyRestingElevation.FLAT -> elevation.elevation0
        StudyRestingElevation.RAISED -> elevation.elevation1
    }

internal enum class StudySurfaceLayer { CONTENT_STAGE, HERO_CONTENT, SUPPORTING_CONTENT, ACTION }

@Immutable
internal data class StudySurfacePresentation(
    val stage: StudySurfaceStage,
    val role: StudySurfaceRole,
    val layer: StudySurfaceLayer,
    val surfaceVariant: LESurfaceVariant,
    val borderProminence: StudyBorderProminence,
    val restingElevation: StudyRestingElevation,
    val hierarchyWeight: Int
)

internal object StudySurfacePresentationResolver {
    fun resolve(stage: StudySurfaceStage, role: StudySurfaceRole): StudySurfacePresentation {
        val allowed = when (stage) {
            StudySurfaceStage.DISCOVERY -> setOf(
                StudySurfaceRole.HERO,
                StudySurfaceRole.PRIMARY_SUPPORT,
                StudySurfaceRole.UTILITY,
                StudySurfaceRole.ACTION
            )
            StudySurfaceStage.UNDERSTANDING -> setOf(
                StudySurfaceRole.HERO,
                StudySurfaceRole.HERO_SUPPORT,
                StudySurfaceRole.SECONDARY_PRIMARY,
                StudySurfaceRole.SUPPORTING,
                StudySurfaceRole.UTILITY,
                StudySurfaceRole.EXPLANATORY,
                StudySurfaceRole.ACTION
            )
        }
        require(role in allowed) { "$role is not part of the $stage study stage" }
        return when (role) {
            StudySurfaceRole.HERO -> presentation(stage, role, StudySurfaceLayer.HERO_CONTENT, LESurfaceVariant.ANSWER, StudyBorderProminence.NONE, StudyRestingElevation.FLAT, 800)
            StudySurfaceRole.HERO_SUPPORT -> presentation(stage, role, StudySurfaceLayer.HERO_CONTENT, LESurfaceVariant.PRIMARY, StudyBorderProminence.NONE, StudyRestingElevation.RAISED, 700)
            StudySurfaceRole.PRIMARY_SUPPORT -> presentation(stage, role, StudySurfaceLayer.SUPPORTING_CONTENT, LESurfaceVariant.MEANING, StudyBorderProminence.SUBTLE, StudyRestingElevation.FLAT, 650)
            StudySurfaceRole.SECONDARY_PRIMARY -> presentation(stage, role, StudySurfaceLayer.SUPPORTING_CONTENT, LESurfaceVariant.MEANING, StudyBorderProminence.NONE, StudyRestingElevation.FLAT, 600)
            StudySurfaceRole.SUPPORTING -> presentation(stage, role, StudySurfaceLayer.SUPPORTING_CONTENT, LESurfaceVariant.EXAMPLE, StudyBorderProminence.NONE, StudyRestingElevation.FLAT, 400)
            StudySurfaceRole.UTILITY -> presentation(stage, role, StudySurfaceLayer.SUPPORTING_CONTENT, LESurfaceVariant.SECONDARY, StudyBorderProminence.NONE, StudyRestingElevation.FLAT, 200)
            StudySurfaceRole.EXPLANATORY -> presentation(stage, role, StudySurfaceLayer.SUPPORTING_CONTENT, LESurfaceVariant.SCHEDULER, StudyBorderProminence.NONE, StudyRestingElevation.FLAT, 300)
            StudySurfaceRole.ACTION -> presentation(stage, role, StudySurfaceLayer.ACTION, LESurfaceVariant.RATING_DOCK, StudyBorderProminence.SUBTLE, StudyRestingElevation.RAISED, 500)
        }
    }

    private fun presentation(
        stage: StudySurfaceStage,
        role: StudySurfaceRole,
        layer: StudySurfaceLayer,
        surfaceVariant: LESurfaceVariant,
        borderProminence: StudyBorderProminence,
        restingElevation: StudyRestingElevation,
        hierarchyWeight: Int
    ) = StudySurfacePresentation(stage, role, layer, surfaceVariant, borderProminence, restingElevation, hierarchyWeight)
}
