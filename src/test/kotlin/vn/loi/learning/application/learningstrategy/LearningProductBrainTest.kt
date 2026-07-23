package vn.loi.learning.application.learningstrategy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateFactory
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateStage
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.learningobjective.LearningObjectiveKind
import vn.loi.learning.application.learningobjective.LearningObjectivePolicy
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.application.learningflow.LearningFlowPlanner
import vn.loi.learning.application.flowtemplate.LearningFlowTemplate

class LearningProductBrainTest {
    private val objectivePolicy = LearningObjectivePolicy()
    private val strategyPlanner = LearningStrategyPlanner()
    private val templateFactory = LearningFlowTemplateFactory()

    @Test
    fun `objective policy selects durable recall without changing experience plan`() {
        val plan = plan(typing = true)
        val original = plan.options.orderedKinds.toList()

        val objective = objectivePolicy.resolve(plan)

        assertEquals(LearningObjectiveKind.DURABLE_RECALL, objective.kind)
        assertEquals(original, plan.options.orderedKinds)
    }

    @Test
    fun `strategy owns rotated primary and optional typing decision`() {
        val plan = plan(typing = true)
        val objective = objectivePolicy.resolve(plan)

        val strategy = strategyPlanner.plan(objective, plan, rotation(1))

        assertEquals(
            listOf(
                LearningExperienceKind.LISTENING_RECALL,
                LearningExperienceKind.TYPING_RECALL
            ),
            strategy.orderedExperiences.map { it.selectedKind }
        )
        assertEquals("ROUND_ROBIN", strategy.orderedExperiences[0].reason.name)
        assertEquals("USER_CHOICE", strategy.orderedExperiences[1].reason.name)
    }

    @Test
    fun `strategy without typing retains one rotated primary`() {
        val plan = plan(typing = false)
        val strategy =
            strategyPlanner.plan(
                objectivePolicy.resolve(plan),
                plan,
                rotation(2)
            )

        assertEquals(
            listOf(LearningExperienceKind.PROMPT_RECALL),
            strategy.orderedExperiences.map { it.selectedKind }
        )
    }

    @Test
    fun `template owns sequence and is immutable from strategy list`() {
        val plan = plan(typing = true)
        val strategy =
            strategyPlanner.plan(objectivePolicy.resolve(plan), plan, rotation())
        val template = templateFactory.create(strategy)

        assertEquals(4, template.stages.size)
        assertIs<LearningFlowTemplateStage.Experience>(template.stages[0])
        assertIs<LearningFlowTemplateStage.Experience>(template.stages[1])
        assertIs<LearningFlowTemplateStage.AnswerReveal>(template.stages[2])
        assertIs<LearningFlowTemplateStage.RatingReady>(template.stages[3])
        assertTrue(strategy.orderedExperiences !== template.stages)
        assertFailsWith<UnsupportedOperationException> {
            (template.stages as MutableList<LearningFlowTemplateStage>).clear()
        }
    }

    @Test
    fun `equal inputs produce equal objective strategy and template`() {
        val plan = plan(typing = true)
        val rotation = rotation(Long.MAX_VALUE)
        val objective = objectivePolicy.resolve(plan)

        val first = templateFactory.create(strategyPlanner.plan(objective, plan, rotation))
        val second = templateFactory.create(strategyPlanner.plan(objective, plan, rotation))

        assertEquals(first, second)
    }

    @Test
    fun `flow planner architecture accepts template rather than product inputs`() {
        val instantiate =
            LearningFlowPlanner::class.java.declaredMethods.single {
                it.name == "instantiate"
            }

        assertEquals(
            listOf(
                LearningFlowTemplate::class.java,
                ExperienceRotationContext::class.java
            ),
            instantiate.parameterTypes.toList()
        )
        assertTrue(
            LearningFlowPlanner::class.java.declaredMethods.none {
                LearningExperiencePlan::class.java in it.parameterTypes
            }
        )
    }

    private fun plan(typing: Boolean) =
        LearningExperiencePlan(
            LearningExperienceOptions.from(
                buildList {
                    add(LearningExperienceKind.IMAGE_RECALL)
                    add(LearningExperienceKind.LISTENING_RECALL)
                    add(LearningExperienceKind.PROMPT_RECALL)
                    if (typing) add(LearningExperienceKind.TYPING_RECALL)
                }
            ),
            LearningExperienceCapabilities(true, true, true, typing, false, false, false),
            LearningExperienceContext(false),
            emptySet(),
            if (typing) TypingRecallPrompt("answer") else null
        )

    private fun rotation(ordinal: Long = 0) =
        ExperienceRotationContext(
            SessionId("session"),
            LearningItemId("item"),
            ordinal
        )
}
