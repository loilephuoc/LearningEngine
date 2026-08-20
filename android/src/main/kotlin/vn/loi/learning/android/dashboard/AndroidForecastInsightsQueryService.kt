package vn.loi.learning.android.dashboard

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Pure calculation logic and query coordinator for FSRS Forecast & Retention Insights.
 *
 * Supports both AllPackages and SpecificPackage scopes.
 */
class AndroidForecastInsightsQueryService(
    private val memoryStateRepository: MemoryStateRepository?,
    private val reviewEventRepository: ReviewEventRepository?,
    private val packageContentQuery: InstalledPackageContentQueryService? = null,
    private val learningItemRepository: LearningItemRepository? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val zoneId: () -> ZoneId = ZoneId::systemDefault
) {

    fun query(
        learnerId: LearnerId,
        scope: AndroidInsightsScope = AndroidInsightsScope.AllPackages,
        availableScopes: List<AndroidInsightsScopeOption> = emptyList()
    ): AndroidForecastInsightsUiModel {
        val nowMillis = now()
        val currentZone = zoneId()

        val allStates = memoryStateRepository?.findAll().orEmpty()
        val allLearnerStates = allStates.filter { it.learnerId == learnerId }
        val allReviewEvents = reviewEventRepository?.findAll(learnerId).orEmpty()

        val scopedLearningItemIds = when (scope) {
            is AndroidInsightsScope.AllPackages -> null
            is AndroidInsightsScope.SpecificPackage -> resolvePackageLearningItemIds(scope.packageId)
        }

        val memoryStates = if (scopedLearningItemIds == null) {
            allLearnerStates
        } else {
            allLearnerStates.filter { it.learningItemId in scopedLearningItemIds }
        }

        val reviewEvents = if (scopedLearningItemIds == null) {
            allReviewEvents
        } else {
            allReviewEvents.filter { it.learningItemId in scopedLearningItemIds }
        }

        val forecast = computeForecast7Days(memoryStates, nowMillis, currentZone)
        val distribution = computeMemoryDistribution(memoryStates)
        val todayRatings = computeTodayRatings(reviewEvents, nowMillis, currentZone)

        return AndroidForecastInsightsUiModel(
            scope = scope,
            availableScopes = availableScopes,
            forecast7Days = forecast,
            memoryDistribution = distribution,
            todayRatings = todayRatings
        )
    }

    private fun resolvePackageLearningItemIds(packageId: String): Set<LearningItemId> {
        val query = packageContentQuery ?: return emptySet()
        val itemRepo = learningItemRepository ?: return emptySet()
        val contentIds = runCatching {
            query.getContentIdsForPackage(InstalledPackageId(packageId))
        }.getOrNull().orEmpty()
        if (contentIds.isEmpty()) return emptySet()
        return itemRepo.findByContentIds(contentIds).map { it.id }.toSet()
    }

    companion object {

        /**
         * Calculates 7-day review forecast buckets for Day 1 (Tomorrow) through Day 7 (+7d).
         * Overdue items and items due today are strictly excluded from the future forecast buckets.
         */
        fun computeForecast7Days(
            memoryStates: List<MemoryState>,
            nowMillis: Long,
            zoneId: ZoneId
        ): List<ForecastDayBucket> {
            val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

            return (1..7).map { offset ->
                val date = today.plusDays(offset.toLong())
                val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val nextDate = date.plusDays(1)
                val endMillis = nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

                val count = memoryStates.count { state ->
                    val due = state.dueAt.epochMillis
                    due in startMillis until endMillis
                }

                val label = if (offset == 1) "Tomorrow" else "+${offset}d"
                ForecastDayBucket(
                    dayIndex = offset,
                    date = date,
                    label = label,
                    count = count
                )
            }
        }

        /**
         * Calculates memory retention distribution across presentation stages:
         * - NEW
         * - LEARNING (including RELEARNING)
         * - YOUNG REVIEW (stability <= 21.0 days)
         * - RETAINED REVIEW (stability > 21.0 days, including MASTERED)
         *
         * NOTE: The 21.0 day threshold is a presentation-only metric.
         */
        fun computeMemoryDistribution(
            memoryStates: List<MemoryState>
        ): MemoryDistributionInsights {
            var newCount = 0
            var learningCount = 0
            var youngCount = 0
            var retainedCount = 0

            for (state in memoryStates) {
                when (state.stage) {
                    LearningStage.NEW -> newCount++
                    LearningStage.LEARNING,
                    LearningStage.RELEARNING -> learningCount++
                    LearningStage.REVIEW -> {
                        if (state.stabilityDays > 21.0) {
                            retainedCount++
                        } else {
                            youngCount++
                        }
                    }
                    LearningStage.MASTERED -> retainedCount++
                    LearningStage.SUSPENDED -> Unit
                }
            }

            val total = newCount + learningCount + youngCount + retainedCount
            return MemoryDistributionInsights(
                newCount = newCount,
                learningCount = learningCount,
                youngCount = youngCount,
                retainedCount = retainedCount,
                totalCount = total
            )
        }

        /**
         * Calculates rating breakdown for local today across all valid ReviewEvents
         * (including STANDARD_REVIEW, MANUAL_USER, etc.).
         * Time boundary: [startOfToday, startOfTomorrow)
         */
        fun computeTodayRatings(
            reviewEvents: List<ReviewEvent>,
            nowMillis: Long,
            zoneId: ZoneId
        ): TodayRatingsInsights {
            val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
            val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val startOfTomorrow = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            var again = 0
            var hard = 0
            var good = 0
            var easy = 0

            for (event in reviewEvents) {
                val reviewedAt = event.reviewedAt.epochMillis
                if (reviewedAt in startOfToday until startOfTomorrow) {
                    when (event.rating) {
                        ReviewRating.AGAIN -> again++
                        ReviewRating.HARD -> hard++
                        ReviewRating.GOOD -> good++
                        ReviewRating.EASY -> easy++
                    }
                }
            }

            val total = again + hard + good + easy
            return TodayRatingsInsights(
                againCount = again,
                hardCount = hard,
                goodCount = good,
                easyCount = easy,
                totalCount = total
            )
        }
    }
}
