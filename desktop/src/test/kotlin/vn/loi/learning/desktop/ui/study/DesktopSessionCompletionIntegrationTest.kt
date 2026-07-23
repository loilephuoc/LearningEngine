package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopSessionCompletionIntegrationTest {
    @Test
    fun `Facade through ViewModel completes persists recovers and clears session summary`() {
        val root = Files.createTempDirectory("desktop-alpha-04")
        try {
            val context = LearningApplicationFactory.createPersisted(root)
            registerSingleItem(context)
            val viewModel = StudyViewModel(StudyFacade(context))

            viewModel.startStudy()
            assertTrue(viewModel.uiState.hasActiveSession)
            viewModel.bootstrapSessionOverview("Medical Physics")
            viewModel.startFirstScene("What is inertia?", "Resistance to motion change")
            viewModel.submitSceneAttempt("Resistance to motion change", 1_100L)
            viewModel.completeAdaptiveSession()

            val completion = assertNotNull(viewModel.uiState.sessionCompletion)
            assertTrue(viewModel.uiState.sessionCompleted)
            assertTrue(completion.whatWasLearned.contains("durable recall"))
            assertTrue(completion.overallOutcome.contains("successful recall"))
            assertTrue(completion.schedulingGuidance.contains("scheduled"))
            assertNotNull(viewModel.uiState.schedulerFeedback)

            val persisted =
                assertNotNull(
                    context.engine.getLatestUndoableSession(LearnerId("default-learner"))
                )
            assertEquals(completion, persisted.completionSnapshot)
            assertEquals(1, persisted.totalReviews)

            val restarted = LearningApplicationFactory.createPersisted(root)
            val recovered = StudyFacade(restarted).load()
            assertTrue(recovered.sessionCompleted)
            assertEquals(completion, recovered.sessionCompletion)

            viewModel.startStudy()
            assertEquals(null, viewModel.uiState.sessionCompletion)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun registerSingleItem(
        context: vn.loi.learning.infrastructure.LearningApplicationContext
    ) {
        val content =
            Content(
                id = ContentId("medical-physics-content"),
                type = ContentType.SENTENCE,
                text = ContentText("What is inertia?", "Resistance to motion change"),
                metadata = ContentMetadata(lesson = "Medical Physics")
            )
        context.engine.registerContent(content)
        context.engine.registerLearningItem(
            LearningItem(
                id = LearningItemId("item-01"),
                contentId = content.id,
                mode = LearningMode.MEANING_RECOGNITION
            )
        )
    }
}
