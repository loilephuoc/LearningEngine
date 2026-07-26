package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePolicy
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.memory.model.LearningStage

class ZeroAnswerLeakageTest {

    private val policy = LearningExperiencePolicy()

    @Test
    fun `1 - IMAGE_RECALL prompt excludes target answer`() {
        val content = Content(
            id = ContentId("saucer-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "saucer",
                translatedText = "đĩa lót tách"
            ),
            media = ContentMedia(image = "saucer.png")
        )
        val uiState = StudyUiState(
            hasActiveSession = true,
            canReview = false, // before reveal
            learningStage = LearningStage.REVIEW,
            domainContent = content
        )

        val model = FocusedVocabularyAnswerResolver.resolve(uiState)
        assertEquals("saucer", model.englishWord)
        assertEquals("đĩa lót tách", model.vietnameseMeaning)

        val accessibility = resolveStudyContentAccessibility(uiState)
        assertFalse(accessibility.promptDescription.contains("saucer"))
        assertTrue(accessibility.promptDescription.contains("đĩa lót tách"))
    }

    @Test
    fun `2 - TYPING_RECALL prompt excludes target answer`() {
        val content = Content(
            id = ContentId("saucer-2"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "saucer",
                translatedText = "đĩa lót tách"
            ),
            media = ContentMedia(image = "saucer.png")
        )
        val uiState = StudyUiState(
            hasActiveSession = true,
            canReview = false,
            learningStage = LearningStage.REVIEW,
            domainContent = content
        )

        val accessibility = resolveStudyContentAccessibility(uiState)
        assertFalse(accessibility.promptDescription.contains("saucer"))
    }

    @Test
    fun `3 - Hidden answer is absent from accessibility semantics`() {
        val uiState = StudyUiState(
            contentText = "saucer",
            translationText = "đĩa lót tách",
            canReview = false,
            domainContent = Content(
                id = ContentId("test-3"),
                type = ContentType.WORD,
                text = ContentText(primaryText = "saucer", translatedText = "đĩa lót tách")
            )
        )
        val accessibility = resolveStudyContentAccessibility(uiState)
        assertFalse(accessibility.promptDescription.contains("saucer"))
        assertEquals(null, accessibility.answerDescription)
    }

    @Test
    fun `4 - Empty typing input remains empty`() {
        val state = TypingRecallUiState()
        assertEquals("", state.input)
        assertFalse(state.input.contains("saucer"))
    }

    @Test
    fun `5 - Unsafe prompt causes deterministic safe fallback`() {
        // Text-only item without prompt image and without meaning
        val content = vn.loi.learning.application.learningcontent.LearningContent(
            question = vn.loi.learning.application.learningcontent.LearningContentSection(
                listOf(vn.loi.learning.application.learningcontent.LearningContentBlock.Text("saucer", vn.loi.learning.domain.content.model.ContentTextFormat.PLAIN_TEXT))
            ),
            answer = vn.loi.learning.application.learningcontent.LearningContentSection(
                listOf(vn.loi.learning.application.learningcontent.LearningContentBlock.UnavailableAnswer)
            )
        )
        val plan = policy.plan(content, LearningExperienceContext(answerRevealed = false, stage = LearningStage.REVIEW))
        assertNotNull(plan)
        assertFalse(plan.options.orderedKinds.contains(LearningExperienceKind.TYPING_RECALL))
    }

    @Test
    fun `7 - After evaluation reveal, FocusedAnswerSurface receives target answer`() {
        val content = Content(
            id = ContentId("saucer-3"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "saucer",
                translatedText = "đĩa lót tách"
            )
        )
        val revealedState = StudyUiState(
            hasActiveSession = true,
            canReview = true,
            contentText = "saucer",
            translationText = "đĩa lót tách",
            domainContent = content
        )
        val model = FocusedVocabularyAnswerResolver.resolve(revealedState)
        assertEquals("saucer", model.englishWord)
        assertEquals("đĩa lót tách", model.vietnameseMeaning)

        val accessibility = resolveStudyContentAccessibility(revealedState)
        assertTrue(accessibility.promptDescription.contains("saucer"))
    }

    @Test
    fun `8 - Listening behavior remains unchanged`() {
        val plan = policy.plan(
            vn.loi.learning.application.learningcontent.LearningContent(
                question = vn.loi.learning.application.learningcontent.LearningContentSection(
                    listOf(
                        vn.loi.learning.application.learningcontent.LearningContentBlock.Audio(
                            vn.loi.learning.application.learningcontent.LocalLearningAssetReference.from("prompt.mp3")!!
                        )
                    )
                ),
                answer = vn.loi.learning.application.learningcontent.LearningContentSection(
                    listOf(vn.loi.learning.application.learningcontent.LearningContentBlock.Text("listen-test", vn.loi.learning.domain.content.model.ContentTextFormat.PLAIN_TEXT))
                )
            ),
            LearningExperienceContext(answerRevealed = false)
        )
        assertNotNull(plan)
        assertTrue(plan.options.orderedKinds.contains(LearningExperienceKind.LISTENING_RECALL))
    }

    @Test
    fun `9 - Resume does not leak answers`() {
        val content = Content(
            id = ContentId("resumed-1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "secret", translatedText = "bí mật")
        )
        val resumedState = StudyUiState(
            hasActiveSession = true,
            canReview = false,
            learningStage = LearningStage.REVIEW,
            domainContent = content
        )
        val accessibility = resolveStudyContentAccessibility(resumedState)
        assertFalse(accessibility.promptDescription.contains("secret"))
    }

    @Test
    fun `10 - Modern and legacy imported vocabulary obey concealment contract`() {
        val legacyContent = Content(
            id = ContentId("opd3-1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "table", translatedText = "bàn")
        )
        val state = StudyUiState(canReview = false, domainContent = legacyContent)
        val accessibility = resolveStudyContentAccessibility(state)
        assertFalse(accessibility.promptDescription.contains("table"))
    }
}
