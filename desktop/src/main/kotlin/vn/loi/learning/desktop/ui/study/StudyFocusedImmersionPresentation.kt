package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable

internal enum class FocusedImmersionLayer {
    WORKSPACE,
    LEARNING_STAGE,
    HERO,
    UNDERSTANDING,
    DECISION
}

internal enum class FocusedImmersionContentRole {
    DISCOVERY_PROMPT,
    HERO_IDENTITY,
    KNOWLEDGE,
    READING,
    EXPLANATION,
    DECISION
}

internal enum class FocusedImmersionDepth { CANVAS, STAGE, HERO, FLOATING_DECISION }

@Immutable
internal data class StudyCanvasPresentation(
    val orderedLayers: List<FocusedImmersionLayer>,
    val centralStageMaxWidthDp: Int,
    val stageDepth: FocusedImmersionDepth,
    val stageUsesExpansiveShape: Boolean,
    val separatesWorkspaceAndStageTone: Boolean
)

internal object StudyCanvasPresentationResolver {
    fun resolve(viewportClass: StudyViewportClass): StudyCanvasPresentation =
        StudyCanvasPresentation(
            orderedLayers = listOf(
                FocusedImmersionLayer.WORKSPACE,
                FocusedImmersionLayer.LEARNING_STAGE,
                FocusedImmersionLayer.HERO,
                FocusedImmersionLayer.UNDERSTANDING,
                FocusedImmersionLayer.DECISION
            ),
            centralStageMaxWidthDp =
                when (viewportClass) {
                    StudyViewportClass.COMPACT -> StudyVisualLayoutResolver.COMPACT_MAX_WIDTH_DP
                    StudyViewportClass.STANDARD -> 720
                    StudyViewportClass.WIDE -> 1040
                },
            stageDepth = FocusedImmersionDepth.STAGE,
            stageUsesExpansiveShape = true,
            separatesWorkspaceAndStageTone = true
        )
}

@Immutable
internal data class StudyHeroPresentation(
    val role: FocusedImmersionContentRole,
    val depth: FocusedImmersionDepth,
    val usesAccentTone: Boolean,
    val usesExpansiveShape: Boolean,
    val focal: Boolean
)

internal object StudyHeroPresentationResolver {
    fun resolve(stage: StudySurfaceStage): StudyHeroPresentation =
        StudyHeroPresentation(
            role =
                if (stage == StudySurfaceStage.DISCOVERY) {
                    FocusedImmersionContentRole.DISCOVERY_PROMPT
                } else {
                    FocusedImmersionContentRole.HERO_IDENTITY
                },
            depth = FocusedImmersionDepth.HERO,
            usesAccentTone = true,
            usesExpansiveShape = true,
            focal = true
        )
}

@Immutable
internal data class StudyContentRhythmPresentation(
    val meaningRole: FocusedImmersionContentRole,
    val examplesRole: FocusedImmersionContentRole,
    val schedulerRole: FocusedImmersionContentRole,
    val examplesReadAsContinuousContent: Boolean,
    val schedulerRemainsExplanatory: Boolean
)

internal object StudyContentRhythmPresentationResolver {
    fun resolve(): StudyContentRhythmPresentation =
        StudyContentRhythmPresentation(
            meaningRole = FocusedImmersionContentRole.KNOWLEDGE,
            examplesRole = FocusedImmersionContentRole.READING,
            schedulerRole = FocusedImmersionContentRole.EXPLANATION,
            examplesReadAsContinuousContent = true,
            schedulerRemainsExplanatory = true
        )
}

@Immutable
internal data class StudyDecisionAreaPresentation(
    val role: FocusedImmersionContentRole,
    val depth: FocusedImmersionDepth,
    val groupedChoices: Boolean,
    val preservesSemanticRatings: Boolean,
    val usesExpansiveShape: Boolean
)

internal object StudyDecisionAreaPresentationResolver {
    fun resolve(): StudyDecisionAreaPresentation =
        StudyDecisionAreaPresentation(
            role = FocusedImmersionContentRole.DECISION,
            depth = FocusedImmersionDepth.FLOATING_DECISION,
            groupedChoices = true,
            preservesSemanticRatings = true,
            usesExpansiveShape = true
        )
}

@Immutable
internal data class StudyItemArrivalPresentation(
    val alpha: Float,
    val translationFraction: Float
)

internal object StudyImmersionMotionResolver {
    fun itemArrival(progress: Float): StudyItemArrivalPresentation {
        val bounded = progress.coerceIn(0f, 1f)
        return StudyItemArrivalPresentation(
            alpha = bounded,
            translationFraction = 1f - bounded
        )
    }
}
