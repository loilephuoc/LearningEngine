package vn.loi.learning.android.family

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

data class VietnameseLunarDate(val year: Int, val month: Int, val day: Int, val isLeapMonth: Boolean)

interface VietnameseLunarCalendar {
    fun solarToLunar(date: LocalDate): VietnameseLunarDate
    fun lunarToSolar(lunarYear: Int, lunarMonth: Int, lunarDay: Int, isLeapMonth: Boolean = false): LocalDate
}

/** Ho Ngoc Duc/Jean Meeus astronomical algorithm, fixed to Vietnam UTC+7. */
class AstronomicalVietnameseLunarCalendar : VietnameseLunarCalendar {
    override fun solarToLunar(date: LocalDate): VietnameseLunarDate {
        require(date.year in RANGE) { "Supported solar years are 1900..2100" }
        val dayNumber = jdFromDate(date.dayOfMonth, date.monthValue, date.year)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = getNewMoonDay(k + 1)
        if (monthStart > dayNumber) monthStart = getNewMoonDay(k)
        var a11 = getLunarMonth11(date.year)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = date.year
            a11 = getLunarMonth11(date.year - 1)
        } else {
            lunarYear = date.year + 1
            b11 = getLunarMonth11(date.year + 1)
        }
        val lunarDay = dayNumber - monthStart + 1
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var lunarMonth = diff + 11
        var leap = false
        if (b11 - a11 > 365) {
            val leapDiff = getLeapMonthOffset(a11)
            if (diff >= leapDiff) {
                lunarMonth = diff + 10
                if (diff == leapDiff) leap = true
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12
        if (lunarMonth >= 11 && diff < 4) lunarYear--
        return VietnameseLunarDate(lunarYear, lunarMonth, lunarDay, leap)
    }

    override fun lunarToSolar(lunarYear: Int, lunarMonth: Int, lunarDay: Int, isLeapMonth: Boolean): LocalDate {
        require(lunarYear in RANGE) { "Supported lunar years are 1900..2100" }
        require(lunarMonth in 1..12) { "Lunar month must be 1..12" }
        require(lunarDay in 1..30) { "Lunar day must be 1..30" }
        val a11: Int
        val b11: Int
        if (lunarMonth < 11) {
            a11 = getLunarMonth11(lunarYear - 1); b11 = getLunarMonth11(lunarYear)
        } else {
            a11 = getLunarMonth11(lunarYear); b11 = getLunarMonth11(lunarYear + 1)
        }
        var offset = lunarMonth - 11
        if (offset < 0) offset += 12
        if (b11 - a11 > 365) {
            val leapOffset = getLeapMonthOffset(a11)
            val leapMonth = (leapOffset - 2).let { if (it < 0) it + 12 else it }
            require(!isLeapMonth || lunarMonth == leapMonth) { "Lunar year $lunarYear has no leap month $lunarMonth" }
            if (isLeapMonth || offset >= leapOffset) offset++
        } else require(!isLeapMonth) { "Lunar year $lunarYear has no leap month" }
        val monthStart = getNewMoonDay(floor(0.5 + (a11 - 2415021.076998695) / 29.530588853).toInt() + offset)
        val result = jdToDate(monthStart + lunarDay - 1)
        val verified = solarToLunar(result)
        require(verified.year == lunarYear && verified.month == lunarMonth && verified.day == lunarDay && verified.isLeapMonth == isLeapMonth) {
            "Invalid lunar date $lunarDay/$lunarMonth/$lunarYear${if (isLeapMonth) " leap" else ""}"
        }
        return result
    }

    private fun jdFromDate(day: Int, month: Int, year: Int): Int {
        val a = (14 - month) / 12; val y = year + 4800 - a; val m = month + 12 * a - 3
        var jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        return jd
    }

    private fun jdToDate(jd: Int): LocalDate {
        val a: Int; val b: Int; val c: Int
        if (jd > 2299160) { a = jd + 32044; b = (4 * a + 3) / 146097; c = a - b * 146097 / 4 }
        else { b = 0; c = jd + 32082 }
        val d = (4 * c + 3) / 1461; val e = c - 1461 * d / 4; val m = (5 * e + 2) / 153
        val day = e - (153 * m + 2) / 5 + 1; val month = m + 3 - 12 * (m / 10); val year = b * 100 + d - 4800 + m / 10
        return LocalDate.of(year, month, day)
    }

    private fun getNewMoonDay(k: Int): Int = floor(newMoon(k) + 0.5 + TIME_ZONE / 24.0).toInt()
    private fun newMoon(k: Int): Double {
        val t = k / 1236.85; val t2 = t * t; val t3 = t2 * t; val dr = PI / 180
        var jd = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mp = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * m * dr)
        c1 -= 0.4068 * sin(mp * dr) + 0.0161 * sin(2 * mp * dr) + 0.0004 * sin(3 * mp * dr)
        c1 += 0.0104 * sin(2 * f * dr) - 0.0051 * sin((m + mp) * dr) - 0.0074 * sin((m - mp) * dr)
        c1 += 0.0004 * sin((2 * f + m) * dr) - 0.0004 * sin((2 * f - m) * dr) - 0.0006 * sin((2 * f + mp) * dr) + 0.0010 * sin((2 * f - mp) * dr) + 0.0005 * sin((2 * mp + m) * dr)
        val deltaT = if (t < -11) 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3 else -0.000278 + 0.000265 * t + 0.000262 * t2
        // The compact Meeus approximation can be tens of minutes early around
        // midnight in Vietnam. Keep the civil-day conversion on the UTC+7 side
        // of that documented approximation error.
        return jd + c1 - deltaT + NEW_MOON_PRECISION_CORRECTION_DAYS
    }

    private fun sunLongitude(jdn: Double): Double {
        val t = (jdn - 2451545.0) / 36525; val t2 = t * t; val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(2 * dr * m) + 0.000290 * sin(3 * dr * m)
        var l = (l0 + dl) * dr; l -= PI * 2 * floor(l / (PI * 2)); return l
    }

    private fun getSunLongitude(dayNumber: Int): Int = floor(sunLongitude(dayNumber - 0.5 - TIME_ZONE / 24.0) / PI * 6).toInt()
    private fun getLunarMonth11(year: Int): Int {
        val off = jdFromDate(31, 12, year) - 2415021
        val k = floor(off / 29.530588853).toInt(); var nm = getNewMoonDay(k)
        if (getSunLongitude(nm) >= 9) nm = getNewMoonDay(k - 1)
        return nm
    }
    private fun getLeapMonthOffset(a11: Int): Int {
        val k = floor(0.5 + (a11 - 2415021.076998695) / 29.530588853).toInt()
        var last = 0; var i = 1; var arc = getSunLongitude(getNewMoonDay(k + i))
        do { last = arc; i++; arc = getSunLongitude(getNewMoonDay(k + i)) } while (arc != last && i < 14)
        return i - 1
    }
    private companion object {
        const val TIME_ZONE = 7.0
        const val NEW_MOON_PRECISION_CORRECTION_DAYS = 1.0 / 24.0
        val RANGE = 1900..2100
    }
}
