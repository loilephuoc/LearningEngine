package vn.loi.learning.domain.study.fsrs

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsConfiguration
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters
import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.memory.ForgettingCurve
import vn.loi.learning.domain.study.memory.FsrsForgettingCurve
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.memory.science.FsrsIntervalSolver
import vn.loi.learning.domain.study.memory.science.IntervalSolver
import vn.loi.learning.domain.study.scheduling.SchedulerDecision

/**
 * Triá»ƒn khai thuáº­t toÃ¡n FSRS-6.
 *
 * Há»— trá»£:
 * - khá»Ÿi táº¡o Difficulty vÃ  Stability á»Ÿ láº§n review Ä‘áº§u;
 * - same-day review;
 * - cáº­p nháº­t Difficulty;
 * - cáº­p nháº­t Stability khi nhá»› hoáº·c quÃªn;
 * - tÃ­nh interval theo desired retention;
 * - cáº­p nháº­t Ä‘áº§y Ä‘á»§ MemoryState.
 */
class DefaultFsrsAlgorithm(
    private val parameters: FsrsParameters =
        FsrsParameters.DEFAULT,
    private val desiredRetention: DesiredRetention =
        DesiredRetention.DEFAULT,
    private val forgettingCurve: ForgettingCurve =
        FsrsForgettingCurve(parameters),
    private val intervalSolver: IntervalSolver =
        FsrsIntervalSolver(parameters)
) : FsrsAlgorithm {

    /**
     * Khá»Ÿi táº¡o thuáº­t toÃ¡n tá»« má»™t cáº¥u hÃ¬nh FSRS thá»‘ng nháº¥t.
     *
     * Constructor nÃ y lÃ  Ä‘iá»ƒm vÃ o Ä‘Æ°á»£c Æ°u tiÃªn cho code má»›i.
     * Constructor chÃ­nh hiá»‡n táº¡i váº«n Ä‘Æ°á»£c giá»¯ láº¡i Ä‘á»ƒ khÃ´ng phÃ¡ vá»¡
     * cÃ¡c test vÃ  integration Ä‘Ã£ tá»“n táº¡i.
     */
    constructor(
        configuration: FsrsConfiguration
    ) : this(
        parameters = configuration.parameters,
        desiredRetention =
            configuration.desiredRetention,
        forgettingCurve =
            FsrsForgettingCurve(
                configuration.parameters
            ),
        intervalSolver =
            FsrsIntervalSolver(
                configuration.parameters
            )
    )

    override fun schedule(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision {

        require(currentState.stage != LearningStage.SUSPENDED) {
            "A suspended MemoryState cannot be scheduled."
        }

        currentState.lastReviewedAt?.let { lastReviewedAt ->
            require(reviewedAt >= lastReviewedAt) {
                "Review time must not be before lastReviewedAt."
            }
        }

        val nextFsrsState =
            if (currentState.reviewCount == 0) {
                FsrsState(
                    difficulty = initialDifficulty(rating),
                    stability = initialStability(rating)
                )
            } else {
                val currentFsrsState =
                    FsrsState(
                        difficulty = currentState.difficultyValue,
                        stability = currentState.stability
                    )

                val elapsedTime =
                    reviewedAt - requireNotNull(
                        currentState.lastReviewedAt
                    )

                val evolvedDifficulty =
                    nextDifficulty(
                        current = currentFsrsState.difficulty,
                        rating = rating
                    )

                val evolvedStability =
                    if (elapsedTime < TimeSpan.days(1L)) {
                        shortTermStability(
                            current = currentFsrsState.stability,
                            rating = rating
                        )
                    } else {
                        val retrievability =
                            forgettingCurve.calculate(
                                stability = currentFsrsState.stability,
                                elapsedTime = elapsedTime
                            )

                        nextStability(
                            difficulty = currentFsrsState.difficulty,
                            stability = currentFsrsState.stability,
                            retrievability = retrievability.value,
                            rating = rating
                        )
                    }

                FsrsState(
                    difficulty = evolvedDifficulty,
                    stability = evolvedStability
                )
            }

        val scheduledInterval =
            calculateInterval(nextFsrsState.stability)

        val nextState =
            currentState.copy(
                stage =
                    nextStage(
                        currentStage = currentState.stage,
                        rating = rating
                    ),
                difficulty = nextFsrsState.difficulty.value,
                stabilityDays = nextFsrsState.stability.days,
                dueAt = reviewedAt + scheduledInterval,
                lastReviewedAt = reviewedAt,
                reviewCount = currentState.reviewCount + 1,
                lapseCount =
                    currentState.lapseCount +
                            lapseIncrement(
                                currentStage = currentState.stage,
                                rating = rating
                            )
            )

        return SchedulerDecision(
            previousState = currentState,
            nextState = nextState,
            scheduledInterval = scheduledInterval
        )
    }

    /**
     * S0(G) = w[G - 1]
     */
    private fun initialStability(
        rating: ReviewRating
    ): Stability =
        Stability.of(
            parameters[rating.grade - 1]
                .coerceAtLeast(MINIMUM_STABILITY_DAYS)
        )

    /**
     * D0(G) = w4 - exp(w5 * (G - 1)) + 1
     */
    private fun initialDifficulty(
        rating: ReviewRating
    ): Difficulty {
        val value =
            parameters[4] -
                    exp(
                        parameters[5] *
                                (rating.grade - 1)
                    ) +
                    1.0

        return Difficulty.clamped(value)
    }

    /**
     * Same-day stability:
     *
     * S' = S * exp(w17 * (G - 3 + w18)) * S^(-w19)
     */
    private fun shortTermStability(
        current: Stability,
        rating: ReviewRating
    ): Stability {
        var multiplier =
            exp(
                parameters[17] *
                        (
                                rating.grade -
                                        3 +
                                        parameters[18]
                                )
            ) *
                    current.days.pow(
                        -parameters[19]
                    )

        if (
            rating == ReviewRating.GOOD ||
            rating == ReviewRating.EASY
        ) {
            multiplier =
                multiplier.coerceAtLeast(1.0)
        }

        return Stability.of(
            (current.days * multiplier)
                .coerceAtLeast(MINIMUM_STABILITY_DAYS)
        )
    }

    /**
     * Difficulty evolution vá»›i linear damping vÃ  mean reversion.
     */
    private fun nextDifficulty(
        current: Difficulty,
        rating: ReviewRating
    ): Difficulty {
        val easyInitialDifficulty =
            parameters[4] -
                    exp(
                        parameters[5] *
                                (ReviewRating.EASY.grade - 1)
                    ) +
                    1.0

        val deltaDifficulty =
            -parameters[6] *
                    (rating.grade - 3)

        val dampedDelta =
            (
                    Difficulty.MAX_VALUE -
                            current.value
                    ) *
                    deltaDifficulty /
                    (
                            Difficulty.MAX_VALUE -
                                    Difficulty.MIN_VALUE
                            )

        val changedDifficulty =
            current.value + dampedDelta

        val revertedDifficulty =
            parameters[7] *
                    easyInitialDifficulty +
                    (
                            1.0 -
                                    parameters[7]
                            ) *
                    changedDifficulty

        return Difficulty.clamped(
            revertedDifficulty
        )
    }

    private fun nextStability(
        difficulty: Difficulty,
        stability: Stability,
        retrievability: Double,
        rating: ReviewRating
    ): Stability {
        val value =
            when (rating) {
                ReviewRating.AGAIN ->
                    nextForgetStability(
                        difficulty = difficulty.value,
                        stability = stability.days,
                        retrievability = retrievability
                    )

                ReviewRating.HARD,
                ReviewRating.GOOD,
                ReviewRating.EASY ->
                    nextRecallStability(
                        difficulty = difficulty.value,
                        stability = stability.days,
                        retrievability = retrievability,
                        rating = rating
                    )
            }

        return Stability.of(
            value.coerceAtLeast(
                MINIMUM_STABILITY_DAYS
            )
        )
    }

    private fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double
    ): Double {
        val longTermStability =
            parameters[11] *
                    difficulty.pow(
                        -parameters[12]
                    ) *
                    (
                            (stability + 1.0).pow(
                                parameters[13]
                            ) -
                                    1.0
                            ) *
                    exp(
                        (1.0 - retrievability) *
                                parameters[14]
                    )

        val shortTermLimit =
            stability /
                    exp(
                        parameters[17] *
                                parameters[18]
                    )

        return minOf(
            longTermStability,
            shortTermLimit
        )
    }

    private fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: ReviewRating
    ): Double {
        val hardPenalty =
            if (rating == ReviewRating.HARD) {
                parameters[15]
            } else {
                1.0
            }

        val easyBonus =
            if (rating == ReviewRating.EASY) {
                parameters[16]
            } else {
                1.0
            }

        return stability *
                (
                        1.0 +
                                exp(parameters[8]) *
                                (11.0 - difficulty) *
                                stability.pow(
                                    -parameters[9]
                                ) *
                                (
                                        exp(
                                            (1.0 - retrievability) *
                                                    parameters[10]
                                        ) -
                                                1.0
                                        ) *
                                hardPenalty *
                                easyBonus
                        )
    }

    private fun calculateInterval(
        stability: Stability
    ): TimeSpan {
        val rawInterval =
            intervalSolver.solve(
                stability = stability,
                desiredRetention = desiredRetention
            )

        val roundedDays =
            rawInterval.toDays()
                .roundToLong()
                .coerceIn(
                    MINIMUM_INTERVAL_DAYS,
                    MAXIMUM_INTERVAL_DAYS
                )

        return TimeSpan.days(roundedDays)
    }

    private fun nextStage(
        currentStage: LearningStage,
        rating: ReviewRating
    ): LearningStage =
        when (rating) {
            ReviewRating.AGAIN ->
                when (currentStage) {
                    LearningStage.NEW,
                    LearningStage.LEARNING ->
                        LearningStage.LEARNING

                    LearningStage.REVIEW,
                    LearningStage.RELEARNING,
                    LearningStage.MASTERED ->
                        LearningStage.RELEARNING

                    LearningStage.SUSPENDED ->
                        error(
                            "Suspended state must be rejected before scheduling."
                        )
                }

            ReviewRating.HARD,
            ReviewRating.GOOD,
            ReviewRating.EASY ->
                LearningStage.REVIEW
        }

    private fun lapseIncrement(
        currentStage: LearningStage,
        rating: ReviewRating
    ): Int =
        if (
            rating == ReviewRating.AGAIN &&
            (
                    currentStage == LearningStage.REVIEW ||
                            currentStage == LearningStage.MASTERED
                    )
        ) {
            1
        } else {
            0
        }

    private companion object {

        const val MINIMUM_STABILITY_DAYS: Double =
            0.001

        const val MINIMUM_INTERVAL_DAYS: Long =
            1L

        const val MAXIMUM_INTERVAL_DAYS: Long =
            36_500L
    }
}
