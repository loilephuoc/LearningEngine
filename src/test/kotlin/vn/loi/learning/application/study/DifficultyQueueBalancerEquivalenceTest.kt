package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

class DifficultyQueueBalancerEquivalenceTest {

    private val balancer =
        DifficultyQueueBalancer()

    @Test
    fun `optimized implementation matches reference algorithm for every short band sequence`() {
        val sequences =
            generateBandSequences(
                maximumLength =
                    8
            )

        sequences.forEachIndexed {
                sequenceIndex,
                bands ->

            val candidates =
                bands.mapIndexed {
                        candidateIndex,
                        band ->

                    candidate(
                        suffix =
                            "$sequenceIndex-$candidateIndex",
                        band =
                            band
                    )
                }

            assertEquals(
                expected =
                    referenceBalance(
                        candidates
                    ),
                actual =
                    balancer.balance(
                        candidates
                    ),
                message =
                    "Unexpected ordering for bands $bands"
            )
        }
    }

    @Test
    fun `balances a large queue without losing or duplicating candidates`() {
        val candidates =
            List(
                size =
                    10_000
            ) { index ->
                candidate(
                    suffix =
                        "large-$index",
                    band =
                        when (
                            index % 3
                        ) {
                            0 ->
                                DifficultyBand.HARD

                            1 ->
                                DifficultyBand.HARD

                            else ->
                                DifficultyBand.EASY
                        }
                )
            }

        val result =
            balancer.balance(
                candidates
            )

        assertEquals(
            expected =
                candidates.size,
            actual =
                result.size
        )

        assertEquals(
            expected =
                candidates.toSet(),
            actual =
                result.toSet()
        )
    }

    private fun referenceBalance(
        orderedCandidates:
        List<SelectionCandidate>
    ): List<SelectionCandidate> {
        val remaining =
            orderedCandidates.toMutableList()

        val balanced =
            ArrayList<SelectionCandidate>(
                orderedCandidates.size
            )

        var previousBand:
                DifficultyBand? = null

        while (remaining.isNotEmpty()) {
            val firstBand =
                remaining.first()
                    .difficultyBand()

            val selectedIndex =
                when {
                    previousBand == null ->
                        0

                    firstBand !=
                            previousBand ->
                        0

                    else -> {
                        val alternativeIndex =
                            remaining.indexOfFirst {
                                    candidate ->

                                candidate
                                    .difficultyBand() !=
                                        previousBand
                            }

                        if (
                            alternativeIndex >=
                            0
                        ) {
                            alternativeIndex
                        } else {
                            0
                        }
                    }
                }

            val selected =
                remaining.removeAt(
                    selectedIndex
                )

            balanced.add(
                selected
            )

            previousBand =
                selected.difficultyBand()
        }

        return balanced
    }

    private fun generateBandSequences(
        maximumLength: Int
    ): List<List<DifficultyBand>> {
        val result =
            mutableListOf<List<DifficultyBand>>()

        fun generate(
            current:
            MutableList<DifficultyBand>,
            targetLength: Int
        ) {
            if (
                current.size ==
                targetLength
            ) {
                result.add(
                    current.toList()
                )

                return
            }

            DifficultyBand.entries.forEach { band ->
                current.add(
                    band
                )

                generate(
                    current =
                        current,
                    targetLength =
                        targetLength
                )

                current.removeAt(
                    current.lastIndex
                )
            }
        }

        for (
        length in
        0..maximumLength
        ) {
            generate(
                current =
                    mutableListOf(),
                targetLength =
                    length
            )
        }

        return result
    }

    private fun SelectionCandidate
            .difficultyBand(): DifficultyBand =
        DifficultyBand.from(
            memoryState
                .difficultyValue
                .value
        )

    private fun candidate(
        suffix: String,
        band: DifficultyBand
    ): SelectionCandidate {
        val itemId =
            LearningItemId(
                "equivalence-item-$suffix"
            )

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        itemId,
                    contentId =
                        ContentId(
                            "equivalence-content-$suffix"
                        ),
                    mode =
                        LearningMode
                            .MEANING_RECOGNITION
                ),
            memoryState =
                MemoryState(
                    learnerId =
                        LearnerId(
                            "equivalence-learner"
                        ),
                    learningItemId =
                        itemId,
                    stage =
                        LearningStage.REVIEW,
                    difficulty =
                        difficultyFor(
                            band
                        ),
                    stabilityDays =
                        10.0,
                    dueAt =
                        Moment(
                            1_000L
                        ),
                    lastReviewedAt =
                        Moment(
                            500L
                        ),
                    reviewCount =
                        1,
                    lapseCount =
                        0
                )
        )
    }

    private fun difficultyFor(
        band: DifficultyBand
    ): Double =
        when (band) {
            DifficultyBand.EASY ->
                2.0

            DifficultyBand.MEDIUM ->
                5.0

            DifficultyBand.HARD ->
                8.0
        }
}