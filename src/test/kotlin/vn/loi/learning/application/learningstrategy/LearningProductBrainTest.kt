package vn.loi.learning.application.learningstrategy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.flowtemplate.LearningFlowTemplate
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateFactory
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateSlot
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateStage
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.learningflow.LearningFlowInstantiationService
import vn.loi.learning.application.learningflow.LearningFlowPlanner
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.application.learningobjective.LearningObjectiveKind
import vn.loi.learning.application.learningobjective.LearningObjectivePolicy
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionId

class LearningProductBrainTest {
    private val objectivePolicy = LearningObjectivePolicy()
    private val strategyPlanner = LearningStrategyPlanner()
    private val templateFactory = LearningFlowTemplateFactory()
    private val productBrainPlanner = ProductBrainPlanner()
    private val instantiationService = LearningFlowInstantiationService()

    @Test
    fun `objective policy selects durable recall without changing experience plan`() {
        val plan = plan(typing = true)
        val original = plan.options.orderedKinds.toList()

        val objective = objectivePolicy.resolve(plan)

        assertEquals(LearningObjectiveKind.DURABLE_RECALL, objective.kind)
        assertEquals(original, plan.options.orderedKinds)
    }

    @Test
    fun `strategy planner determines optional typing eligibility without rotation`() {
        val planWithTyping = plan(typing = true)
        val planWithoutTyping = plan(typing = false)

        val strategyWithTyping = strategyPlanner.plan(objectivePolicy.resolve(planWithTyping), planWithTyping)
        val strategyWithoutTyping = strategyPlanner.plan(objectivePolicy.resolve(planWithoutTyping), planWithoutTyping)

        assertTrue(strategyWithTyping.includeOptionalTyping)
        assertTrue(!strategyWithoutTyping.includeOptionalTyping)
    }

    @Test
    fun `template owns sequence and is immutable from strategy list`() {
        val plan = plan(typing = true)
        val strategy = strategyPlanner.plan(objectivePolicy.resolve(plan), plan)
        val template = templateFactory.create(strategy)

        assertEquals(4, template.stages.size)
        assertIs<LearningFlowTemplateStage.Experience>(template.stages[0])
        assertEquals(
            LearningFlowTemplateSlot.ROTATED_PRIMARY,
            (template.stages[0] as LearningFlowTemplateStage.Experience).slot
        )
        assertIs<LearningFlowTemplateStage.Experience>(template.stages[1])
        assertEquals(
            LearningFlowTemplateSlot.OPTIONAL_TYPING,
            (template.stages[1] as LearningFlowTemplateStage.Experience).slot
        )
        assertIs<LearningFlowTemplateStage.AnswerReveal>(template.stages[2])
        assertIs<LearningFlowTemplateStage.RatingReady>(template.stages[3])

        assertFailsWith<UnsupportedOperationException> {
            (template.stages as MutableList<LearningFlowTemplateStage>).clear()
        }
    }

    @Test
    fun `equal inputs produce equal objective strategy and template`() {
        val plan = plan(typing = true)
        val objective = objectivePolicy.resolve(plan)

        val first = templateFactory.create(strategyPlanner.plan(objective, plan))
        val second = templateFactory.create(strategyPlanner.plan(objective, plan))

        assertEquals(first, second)
    }

    // --- ARCHITECTURE TESTS ---

    @Test
    fun `arch test - reusable templates contain no ExperienceSelectionResult`() {
        val plan = plan(typing = true)
        val template = productBrainPlanner.planTemplate(plan)

        template.stages.forEach { stage ->
            val fields = stage::class.java.declaredFields
            assertTrue(
                fields.none { it.type == ExperienceSelectionResult::class.java },
                "Template stage ${stage::class.java.simpleName} must not contain ExperienceSelectionResult"
            )
        }
    }

    @Test
    fun `arch test - templates are independent of item session rotation`() {
        val plan = plan(typing = true)
        val template = productBrainPlanner.planTemplate(plan)

        val fields = template::class.java.declaredFields + LearningFlowTemplateStage::class.java.declaredFields
        val fieldTypes = fields.map { it.type }

        assertTrue(SessionId::class.java !in fieldTypes)
        assertTrue(LearningItemId::class.java !in fieldTypes)
        assertTrue(ExperienceRotationContext::class.java !in fieldTypes)
    }

    @Test
    fun `arch test - equal template plus equal plan plus equal rotation produce equal runtime flow`() {
        val plan = plan(typing = true)
        val rotation = rotation(1)

        val flow1 = productBrainPlanner.planFlow(plan, rotation)
        val flow2 = productBrainPlanner.planFlow(plan, rotation)

        assertEquals(flow1, flow2)
    }

    @Test
    fun `arch test - changing rotation changes selected primary without changing template`() {
        val plan = plan(typing = true)
        val rot1 = rotation(1)
        val rot2 = rotation(2)

        val template1 = productBrainPlanner.planTemplate(plan)
        val template2 = productBrainPlanner.planTemplate(plan)
        assertEquals(template1, template2)

        val flow1 = productBrainPlanner.planFlow(plan, rot1)
        val flow2 = productBrainPlanner.planFlow(plan, rot2)

        val exp1 = (flow1.stages.first { it is LearningFlowStage.Experience } as LearningFlowStage.Experience).selection.selectedKind
        val exp2 = (flow2.stages.first { it is LearningFlowStage.Experience } as LearningFlowStage.Experience).selection.selectedKind

        assertNotEquals(exp1, exp2)
    }

    @Test
    fun `arch test - changing strategy changes template without changing resolver behavior`() {
        val planWithTyping = plan(typing = true)
        val planWithoutTyping = plan(typing = false)

        val templateWith = productBrainPlanner.planTemplate(planWithTyping)
        val templateWithout = productBrainPlanner.planTemplate(planWithoutTyping)

        assertNotEquals(templateWith, templateWithout)
        assertEquals(4, templateWith.stages.size)
        assertEquals(3, templateWithout.stages.size)
    }

    @Test
    fun `arch test - resolver never mutates templates`() {
        val plan = plan(typing = true)
        val template = productBrainPlanner.planTemplate(plan)
        val stagesBefore = template.stages.toList()

        instantiationService.instantiate(template, strategyPlanner.plan(objectivePolicy.resolve(plan), plan), plan, rotation(1))

        assertEquals(stagesBefore, template.stages)
    }

    @Test
    fun `arch test - LearningFlowPlanner does not regain Product Brain responsibilities`() {
        val methods = LearningFlowPlanner::class.java.declaredMethods
        val parameterTypes = methods.flatMap { it.parameterTypes.toList() }

        assertTrue(LearningExperiencePlan::class.java !in parameterTypes)
        assertTrue(LearningObjectivePolicy::class.java !in parameterTypes)
        assertTrue(LearningStrategyPlanner::class.java !in parameterTypes)
    }

    @Test
    fun `arch test - LearningFlowTemplateFactory has no LearningExperiencePlan dependency`() {
        val createMethod = LearningFlowTemplateFactory::class.java.declaredMethods.single { it.name == "create" }

        assertEquals(
            listOf(LearningStrategyDefinition::class.java),
            createMethod.parameterTypes.toList()
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
