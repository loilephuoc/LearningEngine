package vn.loi.learning.android.family

import kotlin.test.*
import org.junit.Test

class PersonDetailPresentationTest {

    @Test
    fun `soft-deleted fields are excluded from grouped sections`() {
        val fields = listOf(
            PersonContactField(
                id = "cf1",
                personId = "p1",
                type = PersonContactFieldType.PHONE,
                value = "0772098666",
                createdAtEpochMillis = 100,
                updatedAtEpochMillis = 100
            ),
            PersonContactField(
                id = "cf2",
                personId = "p1",
                type = PersonContactFieldType.EMAIL,
                value = "test@example.com",
                createdAtEpochMillis = 100,
                updatedAtEpochMillis = 200,
                deletedAtEpochMillis = 200
            )
        )

        val sections = PersonDetailPresentation.groupAndSortFields(fields)
        assertEquals(1, sections.size)
        assertEquals("LIÊN HỆ", sections[0].title)
        assertEquals(1, sections[0].fields.size)
        assertEquals("0772098666", sections[0].fields[0].value)
    }

    @Test
    fun `fields with blank values are excluded`() {
        // PersonContactField requires non-blank value on creation, but we test the presentation filter logic
        val activeField = PersonContactField(
            id = "cf1",
            personId = "p1",
            type = PersonContactFieldType.PHONE,
            value = "0772098666",
            createdAtEpochMillis = 100,
            updatedAtEpochMillis = 100
        )
        val sections = PersonDetailPresentation.groupAndSortFields(listOf(activeField))
        assertEquals(1, sections.size)
        assertEquals("0772098666", sections[0].fields[0].value)
    }

    @Test
    fun `fields are grouped correctly into respective semantic categories`() {
        val fields = listOf(
            PersonContactField(id = "1", personId = "p", type = PersonContactFieldType.PHONE, value = "0772098666", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "2", personId = "p", type = PersonContactFieldType.EMAIL, value = "a@b.com", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "3", personId = "p", type = PersonContactFieldType.WEBSITE, value = "https://example.com", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "4", personId = "p", type = PersonContactFieldType.ADDRESS, value = "123 Đường A", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "5", personId = "p", type = PersonContactFieldType.JOB_TITLE, value = "Kỹ sư", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "6", personId = "p", type = PersonContactFieldType.COMPANY, value = "Công ty TNHH ABC", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "7", personId = "p", type = PersonContactFieldType.CUSTOM, label = "Số CCCD", value = "079065002954", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )

        val sections = PersonDetailPresentation.groupAndSortFields(fields)
        assertEquals(4, sections.size)

        assertEquals("LIÊN HỆ", sections[0].title)
        assertEquals(listOf("0772098666", "a@b.com", "https://example.com"), sections[0].fields.map { it.value })

        assertEquals("ĐỊA CHỈ", sections[1].title)
        assertEquals(listOf("123 Đường A"), sections[1].fields.map { it.value })

        assertEquals("CÔNG VIỆC", sections[2].title)
        assertEquals(listOf("Kỹ sư", "Công ty TNHH ABC"), sections[2].fields.map { it.value })

        assertEquals("THÔNG TIN KHÁC", sections[3].title)
        assertEquals(listOf("079065002954"), sections[3].fields.map { it.value })
    }

    @Test
    fun `fields within section respect sortOrder with deterministic id fallback`() {
        val fields = listOf(
            PersonContactField(id = "cf_c", personId = "p", type = PersonContactFieldType.PHONE, value = "333", sortOrder = 2, createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "cf_a", personId = "p", type = PersonContactFieldType.PHONE, value = "111", sortOrder = 0, createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "cf_b2", personId = "p", type = PersonContactFieldType.PHONE, value = "222_b", sortOrder = 1, createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField(id = "cf_b1", personId = "p", type = PersonContactFieldType.PHONE, value = "222_a", sortOrder = 1, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )

        val sections = PersonDetailPresentation.groupAndSortFields(fields)
        assertEquals(1, sections.size)
        val sortedValues = sections[0].fields.map { it.value }
        assertEquals(listOf("111", "222_a", "222_b", "333"), sortedValues)
    }

    @Test
    fun `primary field metadata is preserved`() {
        val field = PersonContactField(
            id = "cf1",
            personId = "p",
            type = PersonContactFieldType.PHONE,
            label = "Di động",
            value = "0772098666",
            isPrimary = true,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val item = PersonDetailPresentation.toDetailFieldItem(field)
        assertTrue(item.isPrimary)
        assertEquals("Di động", item.label)
        assertEquals("0772098666", item.value)
    }

    @Test
    fun `semantic actions match field types accurately`() {
        assertEquals(DetailFieldActionType.DIAL, PersonDetailPresentation.actionTypeForField(PersonContactFieldType.PHONE, "0772098666"))
        assertEquals(DetailFieldActionType.EMAIL, PersonDetailPresentation.actionTypeForField(PersonContactFieldType.EMAIL, "a@b.com"))
        assertEquals(DetailFieldActionType.BROWSER, PersonDetailPresentation.actionTypeForField(PersonContactFieldType.WEBSITE, "https://example.com"))
        assertEquals(DetailFieldActionType.NONE, PersonDetailPresentation.actionTypeForField(PersonContactFieldType.ADDRESS, "123 Main St"))
        assertEquals(DetailFieldActionType.NONE, PersonDetailPresentation.actionTypeForField(PersonContactFieldType.CUSTOM, "079123456"))
        assertEquals(DetailFieldActionType.NONE, PersonDetailPresentation.actionTypeForField(PersonContactFieldType.JOB_TITLE, "Driver"))
        assertEquals(DetailFieldActionType.NONE, PersonDetailPresentation.actionTypeForField(PersonContactFieldType.COMPANY, "Acme Inc"))
    }

    @Test
    fun `displayLabel uses custom label when present or falls back to default type label`() {
        val customField = PersonContactField(id = "1", personId = "p", type = PersonContactFieldType.CUSTOM, label = "Số CCCD", value = "079065", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        assertEquals("Số CCCD", PersonDetailPresentation.displayLabelForField(customField))

        val phoneWithCustomLabel = PersonContactField(id = "2", personId = "p", type = PersonContactFieldType.PHONE, label = "Điện thoại phụ", value = "0398679", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        assertEquals("Điện thoại phụ", PersonDetailPresentation.displayLabelForField(phoneWithCustomLabel))

        val defaultPhone = PersonContactField(id = "3", personId = "p", type = PersonContactFieldType.PHONE, label = null, value = "0772", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        assertEquals("Điện thoại", PersonDetailPresentation.displayLabelForField(defaultPhone))

        val defaultEmail = PersonContactField(id = "4", personId = "p", type = PersonContactFieldType.EMAIL, label = null, value = "a@b.com", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        assertEquals("Email", PersonDetailPresentation.displayLabelForField(defaultEmail))
    }

    @Test
    fun `empty sections are completely omitted from result`() {
        val fields = listOf(
            PersonContactField(id = "1", personId = "p", type = PersonContactFieldType.CUSTOM, label = "Số CCCD", value = "079065", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )
        val sections = PersonDetailPresentation.groupAndSortFields(fields)
        assertEquals(1, sections.size)
        assertEquals("THÔNG TIN KHÁC", sections[0].title)
    }

    @Test
    fun `exact clipboard value is preserved without reformatting`() {
        val rawValue = "0772.098.666 #ext 12"
        val field = PersonContactField(id = "1", personId = "p", type = PersonContactFieldType.PHONE, value = rawValue, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val item = PersonDetailPresentation.toDetailFieldItem(field)
        assertEquals(rawValue, item.value)
    }

    @Test
    fun `custom field has copy-only behavior with NONE action type`() {
        val customField = PersonContactField(id = "1", personId = "p", type = PersonContactFieldType.CUSTOM, label = "Số CCCD", value = "079065002954", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val item = PersonDetailPresentation.toDetailFieldItem(customField)
        assertEquals(DetailFieldActionType.NONE, item.actionType)
        assertEquals("079065002954", item.value)
    }
}
