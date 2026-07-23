package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertTrue

class LearningExperienceDependencyBoundaryTest {
    @Test
    fun `shared experience API exposes no Desktop Compose or filesystem types`() {
        val sharedTypes = listOf(
            LearningExperienceKind::class.java,
            LearningExperienceSupportingRole::class.java,
            LearningExperienceCapabilities::class.java,
            LearningExperienceContext::class.java,
            LearningExperienceOptions::class.java,
            LearningExperiencePlan::class.java,
            LearningExperiencePolicy::class.java,
            ExperienceSelectionReason::class.java,
            ExperienceSelectionRequest::class.java,
            ExperienceSelectionDecision::class.java,
            ExperienceSelectionResult::class.java,
            ExperienceSelectionStrategy::class.java,
            ExperienceSelectionEngine::class.java,
            RoundRobinExperienceStrategy::class.java
        )
        val exposedTypeNames = sharedTypes.flatMap { type ->
            buildList {
                add(type.name)
                type.declaredFields.forEach { field -> add(field.type.name) }
                type.declaredMethods.forEach { method ->
                    add(method.returnType.name)
                    method.parameterTypes.forEach { parameter -> add(parameter.name) }
                }
                type.declaredConstructors.forEach { constructor ->
                    constructor.parameterTypes.forEach { parameter -> add(parameter.name) }
                }
            }
        }

        assertTrue(
            exposedTypeNames.none { name ->
                name.startsWith("vn.loi.learning.desktop.") ||
                    name.startsWith("androidx.compose.") ||
                    name.startsWith("java.nio.file.")
            },
            "Shared experience API leaked a platform type: $exposedTypeNames"
        )
    }
}
