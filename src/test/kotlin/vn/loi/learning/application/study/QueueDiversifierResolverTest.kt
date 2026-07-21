package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertSame
import vn.loi.learning.domain.study.session.model.QueueDiversityPolicyType

class QueueDiversifierResolverTest {

    private val noOpDiversifier =
        QueueDiversifier { candidates ->
            candidates
        }

    private val contentDiversityDiversifier =
        QueueDiversifier { candidates ->
            candidates.reversed()
        }

    private val resolver =
        QueueDiversifierResolver(
            noOpDiversifier =
                noOpDiversifier,
            contentDiversityDiversifier =
                contentDiversityDiversifier
        )

    @Test
    fun `resolves no op diversifier`() {
        assertSame(
            expected =
                noOpDiversifier,
            actual =
                resolver.resolve(
                    QueueDiversityPolicyType
                        .NONE
                )
        )
    }

    @Test
    fun `resolves content diversity diversifier`() {
        assertSame(
            expected =
                contentDiversityDiversifier,
            actual =
                resolver.resolve(
                    QueueDiversityPolicyType
                        .CONTENT_DIVERSITY
                )
        )
    }
}