package vn.loi.learning.domain.study.memory.model

/**
 * Một thời điểm tuyệt đối, biểu diễn bằng số mili-giây tính từ Unix Epoch.
 *
 * Không dùng java.time.Instant trực tiếp trong Domain để giữ Core dễ chuyển
 * sang Kotlin Multiplatform và iPhone trong tương lai.
 */
@JvmInline
value class Moment(val epochMillis: Long) : Comparable<Moment> {

    init {
        require(epochMillis >= 0L) {
            "Moment epochMillis must not be negative."
        }
    }

    override fun compareTo(other: Moment): Int =
        epochMillis.compareTo(other.epochMillis)

    operator fun plus(duration: TimeSpan): Moment =
        Moment(Math.addExact(epochMillis, duration.millis))

    operator fun minus(other: Moment): TimeSpan {
        require(this >= other) {
            "Cannot calculate a negative TimeSpan."
        }

        return TimeSpan(epochMillis - other.epochMillis)
    }

    override fun toString(): String = epochMillis.toString()
}