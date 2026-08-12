package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment

class ContentIntroductionPresentationTest {
    private val contentId = ContentId("content-a")

    @Test
    fun `unseen new content requires one introduction by content identity`() {
        assertEquals(
            ContentIntroductionState.REQUIRED,
            resolveContentIntroductionState(SessionItemOrigin.NEW, contentId, emptySet())
        )
        assertEquals(
            ContentIntroductionState.COMPLETED,
            resolveContentIntroductionState(
                SessionItemOrigin.NEW,
                contentId,
                setOf(contentId)
            )
        )
    }

    @Test
    fun `review content never enters introduction`() {
        assertEquals(
            ContentIntroductionState.NOT_APPLICABLE,
            resolveContentIntroductionState(SessionItemOrigin.REVIEW, contentId, emptySet())
        )
    }

    @Test
    fun `single visual recall reveals directly while typing policy remains mandatory`() {
        listOf(
            LearningExperienceKind.IMAGE_RECALL,
            LearningExperienceKind.LISTENING_RECALL,
            LearningExperienceKind.PROMPT_RECALL
        ).forEach { kind ->
            assertTrue(shouldRevealAnswerAfterIntroduction(kind, experienceCount = 1))
        }
        assertEquals(
            false,
            shouldRevealAnswerAfterIntroduction(
                LearningExperienceKind.TYPING_RECALL,
                experienceCount = 1
            )
        )
        assertEquals(
            false,
            shouldRevealAnswerAfterIntroduction(
                LearningExperienceKind.IMAGE_RECALL,
                experienceCount = 2
            )
        )
    }

    @Test
    fun `production screen binds introduction to one shot Vietnamese audio transition`() {
        val source = source("StudyScreen.kt")
        assertTrue(source.contains("uiState.contentIntroductionState"))
        assertTrue(source.contains("focusedAnswerModel.meaningAudioPath?.let(audioController::playOnce)"))
    }

    @Test
    fun `one view model action requests the atomic persisted reveal without simulating next twice`() {
        val viewModel = source("StudyViewModel.kt")
        val facade = source("StudyFacade.kt")

        assertTrue(viewModel.contains("facade.completeContentIntroduction("))
        assertEquals(1, Regex("""completeContentIntroduction\(""").findAll(viewModel).count())
        assertTrue(facade.contains("learningItemId = nextItem.item.learningItem.id.takeIf { revealAnswer }"))
        assertTrue(facade.contains("answerRevealed = revealAnswer"))
    }

    @Test
    fun `canonical introduction completion keeps current item and creates no review`() {
        val itemId = LearningItemId("item-introduction")
        val session = StudySession.start(
            id = SessionId("session-introduction"),
            learnerId = LearnerId("learner-introduction"),
            startedAt = Moment(1_000L),
            policy = SessionPolicy()
        ).presentItem(itemId, Moment(1_100L))

        val introduced = session.completeIntroductionAndReveal(contentId, itemId)

        assertEquals(setOf(contentId), introduced.introducedContentIds)
        assertEquals(itemId, introduced.currentLearningItemId)
        assertEquals(0, introduced.totalReviews)
        assertTrue(introduced.reviewedContentIds.isEmpty())
        assertTrue(introduced.answerRevealed)
    }

    @Test
    fun `screen selects discovery only from canonical introduction state`() {
        val screen = source("StudyScreen.kt")
        assertEquals(2, Regex("""val discoveryFrontVisible\s*=\s*uiState\.contentIntroductionState == ContentIntroductionState\.REQUIRED""").findAll(screen).count())
        assertTrue(screen.contains("if (discoveryFrontVisible) {\n                DiscoveryFrontSurface("))
        assertEquals(0, Regex("""shouldPresentNewItemDiscoveryFront""").findAll(screen).count())
    }

    private fun source(name: String): String {
        val relative = "src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"
        return java.nio.file.Files.readString(
            sequenceOf(
                java.nio.file.Path.of("desktop").resolve(relative),
                java.nio.file.Path.of(relative)
            ).first(java.nio.file.Files::exists)
        )
    }
}
