package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ContinuousReviewScopeProjectionTest {
    @Test
    fun `completion switch projects only the exact durable scope`() {
        val context = LearningApplicationFactory.createInMemory()
        val packageA = InstalledPackageId("package-a")
        context.engine.enableContinuousReview(
            LearnerId("default-learner"), packageA, TopicId("topic-a"), Moment(1L)
        )
        val facade = StudyFacade(context)

        val scopeA = facade.projectContinuousReview(
            StudyUiState(
                sessionCompleted = true,
                activeInstalledPackageId = packageA,
                topicId = "topic-a"
            )
        )
        val scopeB = facade.projectContinuousReview(
            StudyUiState(
                sessionCompleted = true,
                activeInstalledPackageId = InstalledPackageId("package-b"),
                topicId = "topic-b"
            )
        )

        assertTrue(scopeA.continuousReviewEnabled)
        assertFalse(scopeB.continuousReviewEnabled)
    }

    @Test
    fun `nullable topic match remains exact`() {
        val context = LearningApplicationFactory.createInMemory()
        val packageId = InstalledPackageId("package")
        context.engine.enableContinuousReview(
            LearnerId("default-learner"), packageId, null, Moment(1L)
        )
        val facade = StudyFacade(context)

        assertTrue(
            facade.projectContinuousReview(
                StudyUiState(
                    sessionCompleted = true,
                    activeInstalledPackageId = packageId,
                    topicId = null
                )
            ).continuousReviewEnabled
        )
        assertFalse(
            facade.projectContinuousReview(
                StudyUiState(
                    sessionCompleted = true,
                    activeInstalledPackageId = packageId,
                    topicId = "topic"
                )
            ).continuousReviewEnabled
        )
    }
}
