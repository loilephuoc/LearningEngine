package vn.loi.learning.application.session

import kotlin.random.Random
import vn.loi.learning.domain.study.learning.model.LearningItemId

/** Platform-neutral deterministic Fisher-Yates ordering for a fixed practice membership. */
object PracticeRoundShuffler {
    fun shuffle(
        membership: List<LearningItemId>,
        seed: Long,
        round: Int,
        previousLast: LearningItemId? = null,
        previousOrder: List<LearningItemId>? = null
    ): List<LearningItemId> {
        require(membership.isNotEmpty())
        require(membership.distinct().size == membership.size)
        require(round > 0)
        val result = membership.toMutableList()
        val random = Random(seed xor (round.toLong() * -7046029254386353131L))
        for (index in result.lastIndex downTo 1) {
            val swapIndex = random.nextInt(index + 1)
            val value = result[index]
            result[index] = result[swapIndex]
            result[swapIndex] = value
        }
        if (result.size > 1 && result.first() == previousLast) result.swap(0, 1)
        if (result.size > 2 && result == previousOrder) result.swap(1, 2)
        return result
    }

    private fun <T> MutableList<T>.swap(first: Int, second: Int) {
        val value = this[first]
        this[first] = this[second]
        this[second] = value
    }
}
