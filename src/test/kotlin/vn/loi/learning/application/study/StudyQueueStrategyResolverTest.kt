package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertSame
import vn.loi.learning.domain.study.session.model.StudyQueueStrategyType

class StudyQueueStrategyResolverTest {

    private val reviewFirst =
        StudyQueueStrategy { candidates ->
            candidates
        }

    private val newFirst =
        StudyQueueStrategy { candidates ->
            candidates.reversed()
        }

    private val interleaved =
        StudyQueueStrategy { candidates ->
            candidates
        }

    private val adaptive =
        StudyQueueStrategy { candidates ->
            candidates
        }

    private val resolver =
        StudyQueueStrategyResolver(
            reviewFirstStrategy =
                reviewFirst,
            newFirstStrategy =
                newFirst,
            interleavedStrategy =
                interleaved,
            adaptiveStrategy =
                adaptive
        )

    @Test
    fun `resolves review first strategy`() {
        assertSame(
            expected =
                reviewFirst,
            actual =
                resolver.resolve(
                    StudyQueueStrategyType
                        .REVIEW_FIRST
                )
        )
    }

    @Test
    fun `resolves new first strategy`() {
        assertSame(
            expected =
                newFirst,
            actual =
                resolver.resolve(
                    StudyQueueStrategyType
                        .NEW_FIRST
                )
        )
    }

    @Test
    fun `resolves interleaved strategy`() {
        assertSame(
            expected =
                interleaved,
            actual =
                resolver.resolve(
                    StudyQueueStrategyType
                        .INTERLEAVED
                )
        )
    }

    @Test
    fun `resolves adaptive strategy`() {
        assertSame(
            expected =
                adaptive,
            actual =
                resolver.resolve(
                    StudyQueueStrategyType
                        .ADAPTIVE
                )
        )
    }
}