package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.desktop.ui.designsystem.components.StatusBadgeVariant
import vn.loi.learning.domain.study.memory.model.LearningStage

class AuthoritativeStageBadgeProjectionTest {

    @Test
    fun `stage badge resolves corresponding status badge variant for all learning stages`() {
        assertEquals(StatusBadgeVariant.Present, resolveLearningStageBadgeVariant(LearningStage.NEW))
        assertEquals(StatusBadgeVariant.Warning, resolveLearningStageBadgeVariant(LearningStage.LEARNING))
        assertEquals(StatusBadgeVariant.Valid, resolveLearningStageBadgeVariant(LearningStage.REVIEW))
        assertEquals(StatusBadgeVariant.Warning, resolveLearningStageBadgeVariant(LearningStage.RELEARNING))
        assertEquals(StatusBadgeVariant.Valid, resolveLearningStageBadgeVariant(LearningStage.MASTERED))
        assertEquals(StatusBadgeVariant.Missing, resolveLearningStageBadgeVariant(LearningStage.SUSPENDED))
        assertEquals(StatusBadgeVariant.NotEvaluated, resolveLearningStageBadgeVariant(null))
    }

    @Test
    fun `stage label maps directly to authoritative stage name`() {
        assertEquals("NEW", resolveLearningStageLabel(LearningStage.NEW))
        assertEquals("LEARNING", resolveLearningStageLabel(LearningStage.LEARNING))
        assertEquals("REVIEW", resolveLearningStageLabel(LearningStage.REVIEW))
        assertEquals("RELEARNING", resolveLearningStageLabel(LearningStage.RELEARNING))
        assertEquals("MASTERED", resolveLearningStageLabel(LearningStage.MASTERED))
        assertEquals("SUSPENDED", resolveLearningStageLabel(LearningStage.SUSPENDED))
        assertEquals("UNKNOWN", resolveLearningStageLabel(null))
    }
}
