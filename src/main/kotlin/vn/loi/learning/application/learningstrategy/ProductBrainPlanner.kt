package vn.loi.learning.application.learningstrategy

import vn.loi.learning.application.flowtemplate.LearningFlowTemplate
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateFactory
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.LearningExperiencePolicy
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningflow.LearningFlowDefinition
import vn.loi.learning.application.learningflow.LearningFlowInstantiationService
import vn.loi.learning.application.learningobjective.LearningObjectivePolicy

import vn.loi.learning.application.decision.AdaptiveOutcome
import vn.loi.learning.application.decision.InstructionalDecisionEngine
import vn.loi.learning.application.scene.EvidenceReceipt
import vn.loi.learning.application.scene.LearningEvidence
import vn.loi.learning.application.scene.LearningSceneInput
import vn.loi.learning.application.scene.TypingRecallScene
import vn.loi.learning.application.session.bootstrap.ProductBrainSessionBootstrap
import vn.loi.learning.application.session.bootstrap.SessionOverview
import vn.loi.learning.application.session.bootstrap.SessionTimeline

/**
 * Pure orchestration boundary coordinating:
 * Experience Policy -> Objective Policy -> Strategy Planner -> Template Factory -> Instantiation Service -> Decision Engine
 */
class ProductBrainPlanner(
    private val experiencePolicy: LearningExperiencePolicy = LearningExperiencePolicy(),
    private val objectivePolicy: LearningObjectivePolicy = LearningObjectivePolicy(),
    private val strategyPlanner: LearningStrategyPlanner = LearningStrategyPlanner(),
    private val templateFactory: LearningFlowTemplateFactory = LearningFlowTemplateFactory(),
    private val instantiationService: LearningFlowInstantiationService = LearningFlowInstantiationService(),
    private val sessionBootstrap: ProductBrainSessionBootstrap = ProductBrainSessionBootstrap(),
    private val decisionEngine: InstructionalDecisionEngine = InstructionalDecisionEngine()
) {
    fun planExperience(
        content: LearningContent,
        context: LearningExperienceContext
    ): LearningExperiencePlan? = experiencePolicy.plan(content, context)

    fun planTemplate(experiencePlan: LearningExperiencePlan): LearningFlowTemplate {
        val objective = objectivePolicy.resolve(experiencePlan)
        val strategy = strategyPlanner.plan(objective, experiencePlan)
        return templateFactory.create(strategy)
    }

    fun planFlow(
        experiencePlan: LearningExperiencePlan,
        rotation: ExperienceRotationContext
    ): LearningFlowDefinition {
        val objective = objectivePolicy.resolve(experiencePlan)
        val strategy = strategyPlanner.plan(objective, experiencePlan)
        val template = templateFactory.create(strategy)
        return instantiationService.instantiate(template, strategy, experiencePlan, rotation)
    }

    fun bootstrapSession(
        learnerId: String,
        topicId: String,
        content: LearningContent? = null,
        itemCount: Int = 0,
        availableTimeMinutes: Int = 15
    ): SessionOverview {
        return sessionBootstrap.bootstrap(
            learnerId = learnerId,
            topicId = topicId,
            content = content,
            itemCount = itemCount,
            availableTimeMinutes = availableTimeMinutes
        )
    }

    fun selectFirstScene(
        promptText: String,
        expectedAnswer: String,
        learningItemId: String = "item-01",
        learnerId: String = "default-learner"
    ): TypingRecallScene {
        val scene = TypingRecallScene()
        val input = LearningSceneInput(
            sceneId = scene.sceneId,
            objective = "DURABLE_RECALL",
            promptText = promptText,
            expectedAnswer = expectedAnswer,
            learningItemId = learningItemId,
            learnerId = learnerId
        )
        scene.prepare(input)
        return scene
    }

    fun processEvidence(evidence: LearningEvidence): EvidenceReceipt {
        return EvidenceReceipt(
            evidenceId = evidence.evidenceId,
            status = "ACCEPTED"
        )
    }

    fun evaluateAndAdapt(
        evidence: LearningEvidence,
        timeline: SessionTimeline,
        currentDifficulty: Int = 1
    ): AdaptiveOutcome {
        val (decision, trace) = decisionEngine.evaluate(evidence, currentDifficulty)
        val updatedTimeline = timeline.updateWithDecision(decision.action)
        return AdaptiveOutcome(
            decision = decision,
            trace = trace,
            updatedTimeline = updatedTimeline,
            newDifficultyLevel = decision.newDifficultyLevel
        )
    }
}
