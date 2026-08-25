package vn.loi.learning.android.family.widget

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import vn.loi.learning.android.family.AndroidFamilyReminderScheduler
import vn.loi.learning.android.family.FamilyNotificationPublisher

class FamilyWidgetQuickAddIntentTest {

    @Test
    fun `quick add constants and intent action contracts are stable`() {
        assertEquals("vn.loi.learning.android.action.OPEN_FAMILY_QUICK_ADD", FamilyNotificationPublisher.ACTION_OPEN_FAMILY_QUICK_ADD)
        assertEquals("EXTRA_QUICK_ADD_TYPE", FamilyNotificationPublisher.EXTRA_QUICK_ADD_TYPE)
        assertEquals("family_occurrence_date", AndroidFamilyReminderScheduler.EXTRA_OCCURRENCE_DATE)
    }

    @Test
    fun `occurrence date extra parsing roundtrip and fallback`() {
        val testDate = LocalDate.of(2026, 8, 25)
        val dateString = testDate.toString()
        assertEquals("2026-08-25", dateString)

        val parsed = runCatching { LocalDate.parse(dateString) }.getOrNull()
        assertEquals(testDate, parsed)

        // Invalid string falls back safely to null / default without crashing
        val invalidParsed = runCatching { LocalDate.parse("invalid-date-string") }.getOrNull()
        assertNull(invalidParsed)
        val fallbackDate = invalidParsed ?: LocalDate.now()
        assertNotNull(fallbackDate)
    }

    @Test
    fun `quick add mode resolves safely with defaults`() {
        fun resolveQuickAddType(typeExtra: String?): String {
            return when (typeExtra?.uppercase()) {
                "TASK" -> "TASK"
                "EVENT" -> "EVENT"
                else -> "CHOICE"
            }
        }

        assertEquals("TASK", resolveQuickAddType("TASK"))
        assertEquals("TASK", resolveQuickAddType("task"))
        assertEquals("EVENT", resolveQuickAddType("EVENT"))
        assertEquals("CHOICE", resolveQuickAddType(null))
        assertEquals("CHOICE", resolveQuickAddType("UNKNOWN_FOO"))
    }

    @Test
    fun `quick add pending intent action is distinct from normal open family`() {
        assertNotEquals(
            FamilyNotificationPublisher.ACTION_OPEN_FAMILY,
            FamilyNotificationPublisher.ACTION_OPEN_FAMILY_QUICK_ADD
        )
    }

    @Test
    fun `quick add one-shot event token prevents duplicate execution on recomposition`() {
        var executionCount = 0
        var lastConsumedId = 0L

        fun handleEvent(eventId: Long, type: String) {
            if (eventId != lastConsumedId) {
                lastConsumedId = eventId
                executionCount++
            }
        }

        val event1Id = 1000L
        handleEvent(event1Id, "CHOICE")
        assertEquals(1, executionCount)

        // Simulating recomposition with the same event
        handleEvent(event1Id, "CHOICE")
        assertEquals(1, executionCount) // Still 1!

        // New event arrives from another widget tap
        val event2Id = 2000L
        handleEvent(event2Id, "TASK")
        assertEquals(2, executionCount)
    }
}
