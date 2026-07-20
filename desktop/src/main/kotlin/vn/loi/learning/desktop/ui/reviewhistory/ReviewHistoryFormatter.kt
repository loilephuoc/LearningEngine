package vn.loi.learning.desktop.ui.reviewhistory

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReviewHistoryFormatter {

    private val formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun formatEpochMillis(
        epochMillis: Long
    ): String =
        formatter.format(
            Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
        )

    fun formatResponseTime(
        milliseconds: Long?
    ): String =
        milliseconds?.let {
            "%.2f s".format(it / 1000.0)
        } ?: "--"

    fun formatDouble(
        value: Double
    ): String =
        "%.2f".format(value)
}