package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningcontent.LearningTextRole
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.domain.study.session.model.SessionId

class DesktopRecallStudyModeResolverTest {
    private val planner = ProductBrainPlanner()
    private val rotation = ExperienceRotationContext(
        SessionId("routing-session"),
        LearningItemId("routing-item"),
        0
    )

    @Test
    fun `ProductBrain Typing primary resolves explicit Typing study intent`() {
        val intent = requireNotNull(
            DesktopRecallStudyModeResolver.resolve(
                planner,
                learningContent(),
                LearningExperienceContext(false, LearningStage.REVIEW),
                rotation
            )
        )

        assertEquals(StudyMode.TYPING, intent.studyMode)
        assertEquals(
            LearningExperienceKind.TYPING_RECALL,
            intent.definition.stages.filterIsInstance<LearningFlowStage.Experience>()
                .first().selection.selectedKind
        )
    }

    @Test
    fun `rotated primary retains adaptive recall authority`() {
        val intent = requireNotNull(
            DesktopRecallStudyModeResolver.resolve(
                planner,
                learningContent(),
                LearningExperienceContext(false, LearningStage.NEW),
                rotation
            )
        )

        assertEquals(StudyMode.ADAPTIVE, intent.studyMode)
        assertTrue(
            intent.definition.stages.filterIsInstance<LearningFlowStage.Experience>()
                .first().selection.selectedKind != LearningExperienceKind.TYPING_RECALL
        )
    }

    @Test
    fun `StudyFacade passes resolved study intent to canonical production request`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt")
        )
        val planCreation = source.substringAfter("private fun createProductionRecallPlan(")
            .substringBefore("private fun schedulerFeedbackFrom(")

        assertTrue(planCreation.contains("studyMode: StudyMode"))
        assertTrue(planCreation.contains("studyMode = studyMode"))
        assertTrue(source.contains("DesktopRecallStudyModeResolver.resolve("))
    }

    private fun learningContent() = LearningContent(
        question = LearningContentSection(
            listOf(
                LearningContentBlock.Text(
                    "quả táo",
                    ContentTextFormat.PLAIN_TEXT,
                    LearningTextRole.VIETNAMESE_MEANING
                ),
                LearningContentBlock.Image(
                    requireNotNull(LocalLearningAssetReference.from("apple.png"))
                )
            )
        ),
        answer = LearningContentSection(
            listOf(
                LearningContentBlock.Text(
                    "apple",
                    ContentTextFormat.PLAIN_TEXT,
                    LearningTextRole.PRIMARY_ENGLISH
                )
            )
        )
    )
}
