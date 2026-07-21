package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertSame
import vn.loi.learning.domain.study.session.model.DifficultyBalancePolicyType

class QueueBalancerResolverTest {

    private val noOpBalancer =
        QueueBalancer { candidates ->
            candidates
        }

    private val difficultyBalancer =
        QueueBalancer { candidates ->
            candidates.reversed()
        }

    private val resolver =
        QueueBalancerResolver(
            noOpQueueBalancer =
                noOpBalancer,
            difficultyQueueBalancer =
                difficultyBalancer
        )

    @Test
    fun `resolves no op queue balancer`() {
        assertSame(
            expected =
                noOpBalancer,
            actual =
                resolver.resolve(
                    DifficultyBalancePolicyType
                        .NONE
                )
        )
    }

    @Test
    fun `resolves difficulty queue balancer`() {
        assertSame(
            expected =
                difficultyBalancer,
            actual =
                resolver.resolve(
                    DifficultyBalancePolicyType
                        .ALTERNATE_DIFFICULTY
                )
        )
    }
}