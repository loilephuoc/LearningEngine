package vn.loi.learning.android.family

import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.junit.Test

class FamilyRepositoryTest {
    @Test
    fun `atomic repository roundtrips persons, events, reminder rules, tasks, checklist, and completions in v4`() = runTest {
        val dir = Files.createTempDirectory("family-v4-test")
        val file = dir.resolve("family-v1.json")
        val repository = JsonFamilyRepository(file)

        val p1 = Person(
            id = "p1",
            fullName = "Nguyễn An",
            nickname = "Cu Tí",
            group = PersonGroup.FAMILY,
            relationshipLabel = "Em trai",
            birthDateSolar = LocalDate.of(1992, 3, 4),
            phone = "0901234567",
            address = "Hà Nội",
            note = "Thích đá bóng",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 2
        )

        val customCategory = EventCategory(
            id = "cat_custom_alumni",
            name = "Họp lớp",
            builtInKey = null,
            builtIn = false,
            iconKey = "🎓",
            sortOrder = 10,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 2
        )

        val solarEvent = ImportantEvent(
            id = "e1",
            categoryId = "cat_wedding",
            title = "Kỷ niệm ngày cưới",
            relatedPersonId = "p1",
            relatedPersonName = "Nguyễn An",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2020, 11, 20),
            recurrence = RecurrenceType.YEARLY,
            note = "Nhớ đặt hoa",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 2
        )

        val task1 = Task(
            id = "t1",
            title = "Thanh toán Internet",
            description = "Gói 220k FPT",
            startAt = LocalDateTime.of(2026, 9, 1, 8, 0),
            dueAt = LocalDateTime.of(2026, 9, 15, 18, 0),
            recurrence = RecurrenceType.MONTHLY,
            status = TaskStatus.TODO,
            priority = TaskPriority.HIGH,
            relatedEventId = "e1",
            relatedPersonId = "p1",
            completedAt = null,
            note = "Chuyển khoản Vietcombank",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 2
        )

        val check1 = ChecklistItem("c1", "t1", "Mở app FPT", true, 1, 1, 2)
        val check2 = ChecklistItem("c2", "t1", "Quét mã QR", false, 2, 1, 2)

        val completion1 = TaskOccurrenceCompletion("comp1", "t1", LocalDateTime.of(2026, 8, 15, 18, 0), LocalDateTime.of(2026, 8, 14, 20, 0), 1, 2)

        val rule1 = ReminderRule("r1", ReminderTargetType.TASK, "t1", 3, ReminderOffsetUnit.DAY, 8, 0, true, 1, 2)
        val rule2 = ReminderRule("r2", ReminderTargetType.TASK, "t1", 0, ReminderOffsetUnit.DAY, 7, 0, true, 1, 2)

        repository.upsertPerson(p1)
        repository.upsertCategory(customCategory)
        repository.upsertEvent(solarEvent)
        repository.upsertTask(task1)
        repository.upsertChecklistItem(check1)
        repository.upsertChecklistItem(check2)
        repository.completeTaskOccurrence(completion1)
        repository.upsertReminderRule(rule1)
        repository.upsertReminderRule(rule2)

        val loaded = JsonFamilyRepository(file).snapshot.value
        assertEquals(4, loaded.schemaVersion)
        assertEquals(1, loaded.persons.size)
        assertEquals("Nguyễn An", loaded.persons.single().fullName)

        assertEquals(1, loaded.tasks.size)
        val loadedTask = loaded.tasks.single()
        assertEquals("t1", loadedTask.id)
        assertEquals("Thanh toán Internet", loadedTask.title)
        assertEquals(RecurrenceType.MONTHLY, loadedTask.recurrence)
        assertEquals(TaskPriority.HIGH, loadedTask.priority)

        assertEquals(2, loaded.checklistItems.size)
        assertEquals(1, loaded.taskOccurrenceCompletions.size)
        assertEquals("comp1", loaded.taskOccurrenceCompletions.single().id)

        assertEquals(2, loaded.reminderRules.size)
        assertEquals(ReminderTargetType.TASK, loaded.reminderRules.first().targetType)

        val savedContent = Files.readString(file)
        assertTrue(savedContent.contains("\"schemaVersion\": 4"))
        assertTrue(savedContent.contains("\"tasks\":"))
        assertTrue(savedContent.contains("\"checklistItems\":"))
        assertTrue(savedContent.contains("\"taskOccurrenceCompletions\":"))
        assertFalse(Files.exists(dir.resolve("family-v1.json.tmp")))
    }

    @Test
    fun `v3 snapshot migrates smoothly to v4 without losing data`() = runTest {
        val dir = Files.createTempDirectory("family-v3-migration")
        val file = dir.resolve("family-v1.json")

        val v3Json = """
            {
              "schemaVersion": 3,
              "persons": [
                {
                  "id": "p1",
                  "fullName": "Trần Sang",
                  "nickname": "Sang",
                  "group": "FRIEND",
                  "relationshipLabel": "Bạn đại học",
                  "birthDateSolar": "1995-06-12",
                  "phone": "0912345678",
                  "address": "Đà Nẵng",
                  "note": "Bạn thân",
                  "avatarRef": null,
                  "createdAtEpochMillis": 100,
                  "updatedAtEpochMillis": 200,
                  "deletedAtEpochMillis": null
                }
              ],
              "categories": [
                {
                  "id": "cat_wedding",
                  "name": "Ngày cưới",
                  "builtInKey": "WEDDING",
                  "builtIn": true,
                  "iconKey": "💍",
                  "sortOrder": 2,
                  "createdAtEpochMillis": 0,
                  "updatedAtEpochMillis": 0
                }
              ],
              "events": [
                {
                  "id": "e1",
                  "categoryId": "cat_wedding",
                  "title": "Kỷ niệm ngày cưới",
                  "calendarType": "SOLAR",
                  "solarDate": "2020-11-20",
                  "recurrence": "YEARLY",
                  "createdAtEpochMillis": 100,
                  "updatedAtEpochMillis": 200
                }
              ],
              "reminderRules": [
                {
                  "id": "r1",
                  "targetType": "PERSON_BIRTHDAY",
                  "targetId": "p1",
                  "amount": 0,
                  "unit": "DAY",
                  "remindHour": 7,
                  "remindMinute": 0,
                  "enabled": true,
                  "createdAtEpochMillis": 100,
                  "updatedAtEpochMillis": 200
                }
              ]
            }
        """.trimIndent()

        Files.writeString(file, v3Json)

        val repository = JsonFamilyRepository(file)
        val snapshot = repository.snapshot.value

        assertEquals(4, snapshot.schemaVersion)
        assertEquals(1, snapshot.persons.size)
        assertEquals("Trần Sang", snapshot.persons.single().fullName)
        assertEquals(1, snapshot.events.size)
        assertEquals(1, snapshot.reminderRules.size)
        assertEquals(0, snapshot.tasks.size)
        assertEquals(0, snapshot.checklistItems.size)
        assertEquals(0, snapshot.taskOccurrenceCompletions.size)
    }

    @Test
    fun `v2 snapshot migrates memorials and old reminder rules to v4 schema`() = runTest {
        val dir = Files.createTempDirectory("family-v2-migration")
        val file = dir.resolve("family-v1.json")

        val v2Json = """
            {
              "schemaVersion": 2,
              "persons": [
                {
                  "id": "p1",
                  "fullName": "Trần Sang",
                  "nickname": "Sang",
                  "group": "FRIEND",
                  "relationshipLabel": "Bạn đại học",
                  "birthDateSolar": "1995-06-12",
                  "phone": "0912345678",
                  "address": "Đà Nẵng",
                  "note": "Bạn thân",
                  "avatarRef": null,
                  "createdAtEpochMillis": 100,
                  "updatedAtEpochMillis": 200,
                  "deletedAtEpochMillis": null
                }
              ],
              "memorials": [
                {
                  "id": "m1",
                  "fullName": "Ông Ngoại",
                  "relationship": "Ông",
                  "lunarDay": 24,
                  "lunarMonth": 7,
                  "lunarLeapMonth": false,
                  "note": "Nhớ thắp hương",
                  "createdAtEpochMillis": 100,
                  "updatedAtEpochMillis": 200,
                  "deletedAtEpochMillis": null
                }
              ],
              "reminderRules": [
                {
                  "id": "r1",
                  "targetType": "BIRTHDAY",
                  "targetId": "p1",
                  "daysBefore": 3,
                  "remindHour": 8,
                  "remindMinute": 0,
                  "enabled": true,
                  "createdAtEpochMillis": 100,
                  "updatedAtEpochMillis": 200
                }
              ]
            }
        """.trimIndent()

        Files.writeString(file, v2Json)

        val repository = JsonFamilyRepository(file)
        val snapshot = repository.snapshot.value

        assertEquals(4, snapshot.schemaVersion)
        assertEquals(1, snapshot.persons.size)
        assertEquals(PersonGroup.FRIEND, snapshot.persons.single().group)
        assertEquals(1, snapshot.events.size)
        assertEquals("m1", snapshot.events.single().id)
        assertEquals("cat_memorial", snapshot.events.single().categoryId)
    }

    @Test
    fun `existing EVENT with only advance reminders gets default same-day rule without losing custom rules`() = runTest {
        val dir = Files.createTempDirectory("family-ongngoai-test")
        val file = dir.resolve("family-v1.json")

        // UAT scenario: Ong Ngoai memorial with 1 week before and 3 days before reminders
        val json = """
            {
              "schemaVersion": 4,
              "persons": [],
              "categories": [],
              "events": [
                {
                  "id": "m1",
                  "categoryId": "cat_memorial",
                  "title": "Ông Ngoại",
                  "calendarType": "LUNAR",
                  "lunarDay": 24,
                  "lunarMonth": 7,
                  "recurrence": "YEARLY",
                  "createdAtEpochMillis": 1000,
                  "updatedAtEpochMillis": 1000
                }
              ],
              "reminderRules": [
                {
                  "id": "r1",
                  "targetType": "EVENT",
                  "targetId": "m1",
                  "amount": 1,
                  "unit": "WEEK",
                  "remindHour": 8,
                  "remindMinute": 0,
                  "enabled": true,
                  "createdAtEpochMillis": 1000,
                  "updatedAtEpochMillis": 1000
                },
                {
                  "id": "r2",
                  "targetType": "EVENT",
                  "targetId": "m1",
                  "amount": 3,
                  "unit": "DAY",
                  "remindHour": 8,
                  "remindMinute": 0,
                  "enabled": true,
                  "createdAtEpochMillis": 1000,
                  "updatedAtEpochMillis": 1000
                }
              ],
              "tasks": [],
              "checklistItems": [],
              "taskOccurrenceCompletions": []
            }
        """.trimIndent()

        Files.writeString(file, json)

        val repository = JsonFamilyRepository(file)
        val snapshot = repository.snapshot.value

        assertEquals(3, snapshot.reminderRules.size)
        val sameDay = snapshot.reminderRules.firstOrNull { it.targetId == "m1" && it.amount == 0 && it.unit == ReminderOffsetUnit.DAY }
        assertNotNull(sameDay)
        assertEquals(7, sameDay.remindHour)
        assertEquals(0, sameDay.remindMinute)
        assertTrue(sameDay.enabled)

        // Custom advance rules are 100% preserved
        assertTrue(snapshot.reminderRules.any { it.id == "r1" && it.amount == 1 && it.unit == ReminderOffsetUnit.WEEK && it.remindHour == 8 })
        assertTrue(snapshot.reminderRules.any { it.id == "r2" && it.amount == 3 && it.unit == ReminderOffsetUnit.DAY && it.remindHour == 8 })

        // Adding a new rule "Trước 2 Tuần 09:00" and saving survives roundtrip
        val rNew = ReminderRule("r3", ReminderTargetType.EVENT, "m1", 2, ReminderOffsetUnit.WEEK, 9, 0, true, 2000, 2000)
        repository.setReminderRulesForTarget(ReminderTargetType.EVENT, "m1", snapshot.reminderRules + rNew)

        val reloaded = JsonFamilyRepository(file).snapshot.value
        assertEquals(4, reloaded.reminderRules.size)
        assertTrue(reloaded.reminderRules.any { it.amount == 0 && it.remindHour == 7 })
        assertTrue(reloaded.reminderRules.any { it.amount == 1 && it.remindHour == 8 })
        assertTrue(reloaded.reminderRules.any { it.amount == 3 && it.remindHour == 8 })
        assertTrue(reloaded.reminderRules.any { it.amount == 2 && it.remindHour == 9 })
    }

    @Test
    fun `existing EVENT with custom same-day 09-00 rule does not get duplicate 07-00 rule`() = runTest {
        val dir = Files.createTempDirectory("family-custom-sameday-test")
        val file = dir.resolve("family-v1.json")

        val json = """
            {
              "schemaVersion": 4,
              "persons": [],
              "categories": [],
              "events": [
                {
                  "id": "e1",
                  "categoryId": "cat_other",
                  "title": "Họp đại hội",
                  "calendarType": "SOLAR",
                  "solarDate": "2026-10-10",
                  "recurrence": "NONE",
                  "createdAtEpochMillis": 1000,
                  "updatedAtEpochMillis": 1000
                }
              ],
              "reminderRules": [
                {
                  "id": "r_custom",
                  "targetType": "EVENT",
                  "targetId": "e1",
                  "amount": 0,
                  "unit": "DAY",
                  "remindHour": 9,
                  "remindMinute": 30,
                  "enabled": true,
                  "createdAtEpochMillis": 1000,
                  "updatedAtEpochMillis": 1000
                }
              ],
              "tasks": [],
              "checklistItems": [],
              "taskOccurrenceCompletions": []
            }
        """.trimIndent()

        Files.writeString(file, json)

        val snapshot = JsonFamilyRepository(file).snapshot.value
        assertEquals(1, snapshot.reminderRules.size)
        val rule = snapshot.reminderRules.single()
        assertEquals(0, rule.amount)
        assertEquals(9, rule.remindHour)
        assertEquals(30, rule.remindMinute)
    }

    @Test
    fun `task checklist operations add, toggle, and tombstone items correctly`() = runTest {
        val file = Files.createTempDirectory("family-checklist-test").resolve("family-v1.json")
        val repository = JsonFamilyRepository(file)

        val t1 = Task("t1", "Chuẩn bị giỗ", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        repository.upsertTask(t1)

        val item1 = ChecklistItem("c1", "t1", "Mua hoa", false, 1, 1, 1)
        val item2 = ChecklistItem("c2", "t1", "Đặt bánh", false, 2, 1, 1)
        repository.setChecklistItemsForTask("t1", listOf(item1, item2))

        val loaded = repository.snapshot.value
        assertEquals(2, loaded.checklistItems.size)

        // Toggle item1 completed
        repository.upsertChecklistItem(item1.copy(completed = true, updatedAtEpochMillis = 2))
        assertEquals(true, repository.snapshot.value.checklistItems.first { it.id == "c1" }.completed)

        // Soft delete item2
        repository.deleteChecklistItem("c2", 99)
        assertEquals(99, repository.snapshot.value.checklistItems.first { it.id == "c2" }.deletedAtEpochMillis)
    }

    @Test
    fun `existing rule without repeatMode migrates to FOLLOW_TARGET and null occurrenceDate`() = runTest {
        val dir = Files.createTempDirectory("family-repeatmode-migration")
        val file = dir.resolve("family-v1.json")

        val json = """
            {
              "schemaVersion": 4,
              "persons": [],
              "categories": [],
              "events": [
                {
                  "id": "e1",
                  "categoryId": "cat_wedding",
                  "title": "Kỷ niệm",
                  "calendarType": "SOLAR",
                  "solarDate": "2026-11-20",
                  "recurrence": "YEARLY",
                  "createdAtEpochMillis": 1,
                  "updatedAtEpochMillis": 1
                }
              ],
              "reminderRules": [
                {
                  "id": "r1",
                  "targetType": "EVENT",
                  "targetId": "e1",
                  "amount": 1,
                  "unit": "WEEK",
                  "remindHour": 8,
                  "remindMinute": 0,
                  "enabled": true,
                  "createdAtEpochMillis": 1,
                  "updatedAtEpochMillis": 1
                }
              ],
              "tasks": [],
              "checklistItems": [],
              "taskOccurrenceCompletions": []
            }
        """.trimIndent()

        Files.writeString(file, json)

        val snapshot = JsonFamilyRepository(file).snapshot.value
        val r1 = snapshot.reminderRules.first { it.id == "r1" }
        assertEquals(ReminderRepeatMode.FOLLOW_TARGET, r1.repeatMode)
        assertNull(r1.occurrenceDate)
    }

    @Test
    fun `save and reload preserves repeatMode ONCE and occurrenceDate correctly`() = runTest {
        val dir = Files.createTempDirectory("family-once-test")
        val file = dir.resolve("family-v1.json")
        val repository = JsonFamilyRepository(file)

        val rOnce = ReminderRule(
            id = "r_once",
            targetType = ReminderTargetType.EVENT,
            targetId = "m1",
            amount = 2,
            unit = ReminderOffsetUnit.WEEK,
            remindHour = 9,
            remindMinute = 0,
            enabled = true,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
            repeatMode = ReminderRepeatMode.ONCE,
            occurrenceDate = LocalDate.of(2026, 9, 5)
        )

        repository.upsertReminderRule(rOnce)

        val loaded = JsonFamilyRepository(file).snapshot.value
        val loadedRule = loaded.reminderRules.single { it.id == "r_once" }
        assertEquals(ReminderRepeatMode.ONCE, loadedRule.repeatMode)
        assertEquals(LocalDate.of(2026, 9, 5), loadedRule.occurrenceDate)
        assertEquals(2, loadedRule.amount)
        assertEquals(ReminderOffsetUnit.WEEK, loadedRule.unit)
        assertEquals(9, loadedRule.remindHour)
    }
}
