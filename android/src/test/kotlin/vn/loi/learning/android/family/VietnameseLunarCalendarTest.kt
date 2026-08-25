package vn.loi.learning.android.family

import java.time.LocalDate
import kotlin.test.*
import org.junit.Test

class VietnameseLunarCalendarTest {
    private val calendar = AstronomicalVietnameseLunarCalendar()

    @Test fun `known Tet fixtures use Vietnam lunar semantics`() {
        listOf(LocalDate.of(2024, 2, 10), LocalDate.of(2025, 1, 29), LocalDate.of(2026, 2, 17)).forEach { solar ->
            assertEquals(VietnameseLunarDate(solar.year, 1, 1, false), calendar.solarToLunar(solar))
            assertEquals(solar, calendar.lunarToSolar(solar.year, 1, 1))
        }
    }

    @Test fun `leap lunar month is explicit and validated`() {
        val solar = LocalDate.of(2023, 3, 22)
        assertEquals(VietnameseLunarDate(2023, 2, 1, true), calendar.solarToLunar(solar))
        assertEquals(solar, calendar.lunarToSolar(2023, 2, 1, true))
        assertFailsWith<IllegalArgumentException> { calendar.lunarToSolar(2024, 2, 1, true) }
    }

    @Test fun `future and range samples roundtrip`() {
        listOf(LocalDate.of(1900, 6, 15), LocalDate.of(2050, 7, 9), LocalDate.of(2075, 11, 21), LocalDate.of(2099, 4, 3), LocalDate.of(2100, 1, 10)).forEach { solar ->
            val lunar = calendar.solarToLunar(solar)
            assertEquals(solar, calendar.lunarToSolar(lunar.year, lunar.month, lunar.day, lunar.isLeapMonth), solar.toString())
        }
        assertFailsWith<IllegalArgumentException> { calendar.solarToLunar(LocalDate.of(1899, 12, 31)) }
    }
}
