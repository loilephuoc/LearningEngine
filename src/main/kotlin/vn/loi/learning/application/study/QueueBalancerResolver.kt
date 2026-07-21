package vn.loi.learning.application.study

import vn.loi.learning.domain.study.session.model.DifficultyBalancePolicyType

/**
 * Resolve DifficultyBalancePolicyType thành QueueBalancer tương ứng.
 */
class QueueBalancerResolver(
    private val noOpQueueBalancer:
    QueueBalancer =
        NoOpQueueBalancer(),
    private val difficultyQueueBalancer:
    QueueBalancer =
        DifficultyQueueBalancer()
) {

    fun resolve(
        type: DifficultyBalancePolicyType
    ): QueueBalancer =
        when (type) {
            DifficultyBalancePolicyType.NONE ->
                noOpQueueBalancer

            DifficultyBalancePolicyType
                .ALTERNATE_DIFFICULTY ->
                difficultyQueueBalancer
        }
}