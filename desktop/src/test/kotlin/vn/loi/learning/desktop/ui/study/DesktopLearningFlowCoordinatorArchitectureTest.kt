package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.learningflow.LearningFlowController
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner

class DesktopLearningFlowCoordinatorArchitectureTest {
    @Test
    fun `arch test - Desktop depends only on ProductBrainPlanner and LearningFlowController`() {
        val primaryConstructor =
            DesktopLearningFlowCoordinator::class.java.constructors.first { it.parameterCount == 2 }

        assertEquals(
            listOf(
                ProductBrainPlanner::class.java,
                LearningFlowController::class.java
            ),
            primaryConstructor.parameterTypes.toList()
        )
    }
}
