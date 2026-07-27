package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.LearningStage

class StudyLearningStageLabelTest {

    @Test
    fun `learning card label comes directly from durable learning stage`() {
        LearningStage.entries.forEach { stage ->
            assertEquals(stage.name, resolveLearningStageLabel(stage))
        }
    }

    @Test
    fun `missing learning stage has explicit unknown label`() {
        assertEquals("UNKNOWN", resolveLearningStageLabel(null))
    }
}
