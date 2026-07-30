package vn.loi.learning.application.learningflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.application.learningobjective.LearningObjectivePolicy
import vn.loi.learning.application.learningstrategy.LearningStrategyPlanner
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateFactory

class LearningFlowEngineTest {
    private val planner = LearningFlowPlanner()
    private val controller = LearningFlowController()

    @Test
    fun `definition rejects empty duplicate and invalid terminal order`() {
        assertFailsWith<IllegalArgumentException> {
            LearningFlowDefinition.create(id(), rotation(), emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            LearningFlowDefinition.create(
                id(),
                rotation(),
                listOf(
                    LearningFlowStage.AnswerReveal(stage("same")),
                    LearningFlowStage.RatingReady(stage("same"))
                )
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LearningFlowDefinition.create(
                id(),
                rotation(),
                listOf(
                    LearningFlowStage.RatingReady(stage("terminal")),
                    LearningFlowStage.AnswerReveal(stage("reveal"))
                )
            )
        }
    }

    @Test
    fun `planner rotates primary and adds typing only after primary`() {
        val plan = plan(typing = true)
        val expected =
            listOf(
                LearningExperienceKind.IMAGE_RECALL,
                LearningExperienceKind.LISTENING_RECALL,
                LearningExperienceKind.PROMPT_RECALL
            )

        expected.forEachIndexed { ordinal, kind ->
            val definition = flow(plan, rotation(ordinal.toLong()))
            val experiences =
                definition.stages.filterIsInstance<LearningFlowStage.Experience>()
            assertEquals(kind, experiences.first().selection.selectedKind)
            assertEquals(
                LearningExperienceKind.TYPING_RECALL,
                experiences[1].selection.selectedKind
            )
            assertTrue(experiences.first().selection.reason.name == "ROUND_ROBIN")
            assertTrue(experiences[1].selection.reason.name == "USER_CHOICE")
        }
        assertEquals(
            flow(plan, rotation(1)),
            flow(plan, rotation(1))
        )
    }

    @Test
    fun `planner creates short safe flow without typing`() {
        val definition = flow(plan(typing = false), rotation())

        assertEquals(3, definition.stages.size)
        assertIs<LearningFlowStage.Experience>(definition.stages[0])
        assertIs<LearningFlowStage.AnswerReveal>(definition.stages[1])
        assertIs<LearningFlowStage.RatingReady>(definition.stages[2])
    }

    @Test
    fun `review typing flow starts at typing as stage one of one`() {
        val definition =
            flow(plan(typing = true, stage = LearningStage.REVIEW), rotation())
        val state = controller.initialize(definition)
        val current = assertIs<LearningFlowStage.Experience>(controller.current(definition, state))
        val progress = controller.progress(definition, state)

        assertEquals(LearningExperienceKind.TYPING_RECALL, current.selection.selectedKind)
        assertEquals(1, progress.currentExperienceNumber)
        assertEquals(1, progress.totalExperienceCount)
        assertEquals(3, definition.stages.size)
    }

    @Test
    fun `review items without typing retain image listening and prompt primary flows`() {
        listOf(
            LearningExperienceKind.IMAGE_RECALL,
            LearningExperienceKind.LISTENING_RECALL,
            LearningExperienceKind.PROMPT_RECALL
        ).forEach { kind ->
            val plan =
                LearningExperiencePlan(
                    options = LearningExperienceOptions.from(listOf(kind)),
                    capabilities =
                        LearningExperienceCapabilities(
                            hasPromptText = kind == LearningExperienceKind.PROMPT_RECALL,
                            hasPromptImage = kind == LearningExperienceKind.IMAGE_RECALL,
                            hasPromptAudio = kind == LearningExperienceKind.LISTENING_RECALL,
                            hasMeaning = true,
                            hasExample = false,
                            hasAnswerAudio = false,
                            hasExampleAudio = false
                        ),
                    context = LearningExperienceContext(false, LearningStage.REVIEW),
                    visibleSupportingRoles = emptySet()
                )
            val first =
                assertIs<LearningFlowStage.Experience>(
                    flow(plan, rotation()).stages.first()
                )

            assertEquals(kind, first.selection.selectedKind)
        }
    }

    @Test
    fun `controller advances ordered experiences then requests reveal and rating readiness`() {
        val definition = flow(plan(typing = true), rotation())
        var state = controller.initialize(definition)
        val first = controller.current(definition, state)
        assertEquals(1, controller.progress(definition, state).currentExperienceNumber)

        state =
            assertIs<LearningFlowTransition.StageAdvanced>(
                controller.completeCurrent(definition, state, first.id)
            ).state
        assertEquals(2, controller.progress(definition, state).currentExperienceNumber)

        val typing = controller.current(definition, state)
        val reveal =
            assertIs<LearningFlowTransition.AnswerRevealRequested>(
                controller.completeCurrent(definition, state, typing.id)
            )
        assertTrue(controller.progress(definition, reveal.state).isRevealPending)

        val ready =
            assertIs<LearningFlowTransition.RatingReady>(
                controller.confirmAnswerRevealed(definition, reveal.state)
            )
        assertTrue(controller.progress(definition, ready.state).isRatingReady)
        assertEquals(
            ready,
            controller.confirmAnswerRevealed(definition, ready.state)
        )
    }

    @Test
    fun `controller rejects stale and terminal completion without mutation`() {
        val definition = flow(plan(typing = false), rotation())
        val initial = controller.initialize(definition)
        assertEquals(
            initial,
            assertIs<LearningFlowTransition.Rejected>(
                controller.completeCurrent(definition, initial, stage("stale"))
            ).state
        )
        val reveal =
            assertIs<LearningFlowTransition.AnswerRevealRequested>(
                controller.completeCurrent(
                    definition,
                    initial,
                    controller.current(definition, initial).id
                )
            )
        val ready =
            controller.confirmAnswerRevealed(definition, reveal.state).state
        assertIs<LearningFlowTransition.Rejected>(
            controller.completeCurrent(
                definition,
                ready,
                controller.current(definition, ready).id
            )
        )
    }

    @Test
    fun `revealed reconstruction is rating ready without persisted stage`() {
        val definition = flow(plan(typing = true), rotation())
        val state = controller.initializeRevealed(definition)

        assertIs<LearningFlowStage.RatingReady>(controller.current(definition, state))
        assertTrue(controller.progress(definition, state).isRatingReady)
    }

    private fun plan(
        typing: Boolean,
        stage: LearningStage? = null
    ): LearningExperiencePlan =
        LearningExperiencePlan(
            options =
                LearningExperienceOptions.from(
                    buildList {
                        add(LearningExperienceKind.IMAGE_RECALL)
                        add(LearningExperienceKind.LISTENING_RECALL)
                        add(LearningExperienceKind.PROMPT_RECALL)
                        if (typing) add(LearningExperienceKind.TYPING_RECALL)
                    }
                ),
            capabilities =
                LearningExperienceCapabilities(
                    true, true, true, typing, false, false, false
                ),
            context = LearningExperienceContext(false, stage),
            visibleSupportingRoles = emptySet(),
            typingPrompt = if (typing) TypingRecallPrompt("answer") else null
        )

    private fun rotation(ordinal: Long = 0) =
        ExperienceRotationContext(
            SessionId("session"),
            LearningItemId("item"),
            ordinal
        )

    private fun flow(
        plan: LearningExperiencePlan,
        rotation: ExperienceRotationContext
    ): LearningFlowDefinition {
        return vn.loi.learning.application.learningstrategy.ProductBrainPlanner().planFlow(plan, rotation)
    }

    private fun id() = LearningFlowId("flow")
    private fun stage(value: String) = LearningFlowStageId(value)
}
