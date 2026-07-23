package vn.loi.learning.application.learningflow

import kotlin.test.Test
import kotlin.test.assertTrue

class LearningFlowDependencyBoundaryTest {
    @Test
    fun `flow API exposes no platform persistence scheduler or filesystem types`() {
        val names =
            listOf(
                LearningFlowStageId::class.java,
                LearningFlowId::class.java,
                LearningFlowStage::class.java,
                LearningFlowDefinition::class.java,
                LearningFlowState::class.java,
                LearningFlowProgress::class.java,
                LearningFlowTransition::class.java,
                LearningFlowPlanner::class.java,
                LearningFlowController::class.java
            ).flatMap { type ->
                type.declaredFields.map { it.type.name } +
                    type.declaredMethods.flatMap {
                        listOf(it.returnType.name) + it.parameterTypes.map(Class<*>::getName)
                    }
            }

        assertTrue(
            names.none {
                it.startsWith("androidx.compose") ||
                    it.startsWith("java.awt") ||
                    it.startsWith("javax.swing") ||
                    it.startsWith("java.nio.file") ||
                    it.contains(".desktop.") ||
                    it.contains(".infrastructure.") ||
                    it.contains(".scheduling.")
            },
            "Flow API leaked a forbidden dependency: $names"
        )
    }
}
