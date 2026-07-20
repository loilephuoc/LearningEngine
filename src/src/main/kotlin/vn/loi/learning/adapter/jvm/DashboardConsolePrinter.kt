package vn.loi.learning.adapter.jvm

import vn.loi.learning.application.learningdashboard.LearningDashboardSnapshot

/**
 * Adapter hiển thị Learning Dashboard trên console JVM.
 *
 * Printer này chỉ chuyển immutable Dashboard snapshot
 * thành văn bản để hiển thị.
 *
 * Nó không:
 * - truy cập repository;
 * - gọi LearningEngine;
 * - query Dashboard;
 * - đọc đồng hồ hệ thống;
 * - tính lại analytics;
 * - thay đổi domain state.
 */
class DashboardConsolePrinter {

    fun print(
        snapshot: LearningDashboardSnapshot
    ) {
        println()
        println("=== LEARNING DASHBOARD ===")

        printActivity(snapshot)
        printMemory(snapshot)
        printScheduling(snapshot)
        printRetention(snapshot)
        printForecast(snapshot)

        println("==========================")
    }

    private fun printActivity(
        snapshot: LearningDashboardSnapshot
    ) {
        println()
        println("--- ACTIVITY ---")
        println(
            "Has review activity: ${snapshot.hasActivity}"
        )
    }

    private fun printMemory(
        snapshot: LearningDashboardSnapshot
    ) {
        println()
        println("--- MEMORY ---")
        println(
            "Total memories: ${snapshot.memory.totalMemories}"
        )
        println(
            "Active memories: ${snapshot.memory.activeMemories}"
        )
    }

    private fun printScheduling(
        snapshot: LearningDashboardSnapshot
    ) {
        val dueStatistics =
            snapshot.scheduling.dueStatistics

        println()
        println("--- SCHEDULING ---")
        println(
            "Due memories: ${dueStatistics.dueCount}"
        )
        println(
            "Due now: ${dueStatistics.dueNowCount}"
        )
        println(
            "Overdue: ${dueStatistics.overdueCount}"
        )
    }

    private fun printRetention(
        snapshot: LearningDashboardSnapshot
    ) {
        val statistics =
            snapshot.retention.statistics

        println()
        println("--- RETENTION ---")
        println(
            "Evaluated memories: " +
                    statistics.evaluatedMemoryCount
        )

        val averageRetrievability =
            statistics.averageRetrievability

        if (averageRetrievability == null) {
            println("Average retrievability: N/A")
            return
        }

        println(
            "Average retrievability: %.2f%%".format(
                averageRetrievability.value * 100.0
            )
        )
    }

    private fun printForecast(
        snapshot: LearningDashboardSnapshot
    ) {
        val forecast =
            snapshot.forecast.forecast

        println()
        println("--- FORECAST ---")

        if (!forecast.hasBuckets) {
            println("No forecast buckets.")
            return
        }

        forecast.buckets.forEachIndexed { index, bucket ->
            println(
                "Bucket ${index + 1}: " +
                        "(${bucket.windowStart.epochMillis}, " +
                        "${bucket.windowEnd.epochMillis}] " +
                        "-> ${bucket.dueCount} due"
            )
        }

        println(
            "Total forecast due: ${forecast.totalDueCount}"
        )
    }
}