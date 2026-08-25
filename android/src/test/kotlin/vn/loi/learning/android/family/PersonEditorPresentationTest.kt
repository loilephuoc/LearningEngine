package vn.loi.learning.android.family

import java.time.LocalDate
import kotlin.test.*
import org.junit.Test

class PersonEditorPresentationTest {

    @Test
    fun `draft converts from and to domain model preserving exact values and stable id`() {
        val originalField = PersonContactField(
            id = "cf_stable_123",
            personId = "p1",
            type = PersonContactFieldType.PHONE,
            label = "Di động",
            value = "0772.098.666",
            isPrimary = true,
            sortOrder = 0,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val draft = PersonContactFieldDraft.from(originalField)
        assertEquals("cf_stable_123", draft.id)
        assertEquals(PersonContactFieldType.PHONE, draft.type)
        assertEquals("Di động", draft.label)
        assertEquals("0772.098.666", draft.value)
        assertTrue(draft.isPrimary)
        assertEquals(0, draft.sortOrder)

        val converted = draft.toDomain("p1", now = 2000L)!!
        assertEquals("cf_stable_123", converted.id)
        assertEquals("p1", converted.personId)
        assertEquals("0772.098.666", converted.value)
        assertEquals("Di động", converted.label)
        assertTrue(converted.isPrimary)
        assertEquals(2000L, converted.updatedAtEpochMillis)
    }

    @Test
    fun `blank value draft returns null domain field`() {
        val draft = PersonContactFieldDraft(
            id = "cf_blank",
            type = PersonContactFieldType.EMAIL,
            label = "Email",
            value = "   "
        )
        assertNull(draft.toDomain("p1"))
    }

    @Test
    fun `custom field draft with blank label defaults to fallback label`() {
        val draft = PersonContactFieldDraft(
            id = "cf_custom",
            type = PersonContactFieldType.CUSTOM,
            label = "   ",
            value = "079065002954"
        )
        val domain = draft.toDomain("p1")!!
        assertEquals("Thông tin", domain.label)
        assertEquals("079065002954", domain.value)
    }

    @Test
    fun `createDraftFromTemplate generates correct type and default label`() {
        val phoneTemplate = PersonEditorPresentation.FIELD_TEMPLATES.first { it.type == PersonContactFieldType.PHONE }
        val draftPhone = PersonEditorPresentation.createDraftFromTemplate(phoneTemplate, nextSortOrder = 2)
        assertEquals(PersonContactFieldType.PHONE, draftPhone.type)
        assertEquals("Điện thoại", draftPhone.label)
        assertEquals("", draftPhone.value)
        assertEquals(2, draftPhone.sortOrder)
        assertFalse(draftPhone.isPrimary)

        val customTemplate = PersonEditorPresentation.FIELD_TEMPLATES.first { it.title == "Trường tùy chỉnh" }
        val draftCustom = PersonEditorPresentation.createDraftFromTemplate(customTemplate, nextSortOrder = 3)
        assertEquals(PersonContactFieldType.CUSTOM, draftCustom.type)
        assertEquals("", draftCustom.label)
        assertEquals(3, draftCustom.sortOrder)
    }

    @Test
    fun `isDirty accurately detects unchanged vs modified state`() {
        val person = Person(
            id = "p1",
            fullName = "Lê Văn Tính",
            nickname = "Ba Tính",
            group = PersonGroup.FAMILY,
            relationshipLabel = "Ba",
            birthDateSolar = LocalDate.of(1965, 6, 12),
            note = "Ghi chú ban đầu",
            avatarRef = "content://media/1",
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val contactFields = listOf(
            PersonContactField(id = "cf1", personId = "p1", type = PersonContactFieldType.PHONE, label = "Di động", value = "0772098666", isPrimary = true, createdAtEpochMillis = 1000L, updatedAtEpochMillis = 1000L)
        )

        val drafts = contactFields.map(PersonContactFieldDraft::from)

        // 1. Exact initial values -> not dirty
        assertFalse(
            PersonEditorPresentation.isDirty(
                initialPerson = person,
                initialContactFields = contactFields,
                fullName = "Lê Văn Tính",
                nickname = "Ba Tính",
                relationshipLabel = "Ba",
                group = PersonGroup.FAMILY,
                birthDateSolar = LocalDate.of(1965, 6, 12),
                note = "Ghi chú ban đầu",
                avatarRef = "content://media/1",
                draftFields = drafts
            )
        )

        // 2. Modified name -> dirty
        assertTrue(
            PersonEditorPresentation.isDirty(
                initialPerson = person,
                initialContactFields = contactFields,
                fullName = "Lê Văn Tính Mới",
                nickname = "Ba Tính",
                relationshipLabel = "Ba",
                group = PersonGroup.FAMILY,
                birthDateSolar = LocalDate.of(1965, 6, 12),
                note = "Ghi chú ban đầu",
                avatarRef = "content://media/1",
                draftFields = drafts
            )
        )

        // 3. Added contact field -> dirty
        val extraDraft = PersonContactFieldDraft(id = "cf2", type = PersonContactFieldType.EMAIL, label = "Email", value = "test@example.com")
        assertTrue(
            PersonEditorPresentation.isDirty(
                initialPerson = person,
                initialContactFields = contactFields,
                fullName = "Lê Văn Tính",
                nickname = "Ba Tính",
                relationshipLabel = "Ba",
                group = PersonGroup.FAMILY,
                birthDateSolar = LocalDate.of(1965, 6, 12),
                note = "Ghi chú ban đầu",
                avatarRef = "content://media/1",
                draftFields = drafts + extraDraft
            )
        )

        // 4. Changed avatarRef -> dirty
        assertTrue(
            PersonEditorPresentation.isDirty(
                initialPerson = person,
                initialContactFields = contactFields,
                fullName = "Lê Văn Tính",
                nickname = "Ba Tính",
                relationshipLabel = "Ba",
                group = PersonGroup.FAMILY,
                birthDateSolar = LocalDate.of(1965, 6, 12),
                note = "Ghi chú ban đầu",
                avatarRef = null,
                draftFields = drafts
            )
        )
    }

    @Test
    fun `deleting one field preserves others and marks state dirty`() {
        val person = Person(id = "p1", fullName = "An", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val f1 = PersonContactField(id = "1", personId = "p1", type = PersonContactFieldType.PHONE, value = "0123", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val f2 = PersonContactField(id = "2", personId = "p1", type = PersonContactFieldType.EMAIL, value = "a@b.com", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val initialFields = listOf(f1, f2)

        val drafts = mutableListOf(PersonContactFieldDraft.from(f1), PersonContactFieldDraft.from(f2))
        drafts.removeAt(0) // Remove f1

        assertEquals(1, drafts.size)
        assertEquals("2", drafts[0].id)
        assertEquals("a@b.com", drafts[0].value)

        assertTrue(
            PersonEditorPresentation.isDirty(
                initialPerson = person,
                initialContactFields = initialFields,
                fullName = "An",
                nickname = "",
                relationshipLabel = "",
                group = PersonGroup.FAMILY,
                birthDateSolar = null,
                note = "",
                avatarRef = null,
                draftFields = drafts
            )
        )
    }

    @Test
    fun `prepareSavePayload constructs updated Person and contact fields correctly`() {
        val existing = Person(
            id = "p_existing",
            fullName = "Tên Cũ",
            group = PersonGroup.FRIEND,
            createdAtEpochMillis = 5000L,
            updatedAtEpochMillis = 5000L
        )

        val drafts = listOf(
            PersonContactFieldDraft(id = "cf1", type = PersonContactFieldType.PHONE, label = "Di động", value = "0772098666", isPrimary = true),
            PersonContactFieldDraft(id = "cf2", type = PersonContactFieldType.CUSTOM, label = "Số CCCD", value = "079065002954"),
            PersonContactFieldDraft(id = "cf3", type = PersonContactFieldType.ADDRESS, label = "Thường trú", value = "123 Lê Lợi, TP.HCM", isPrimary = true)
        )

        val (savedPerson, savedFields) = PersonEditorPresentation.prepareSavePayload(
            existingPerson = existing,
            fullName = "Tên Mới",
            nickname = "Nick",
            relationshipLabel = "Bạn thân",
            group = PersonGroup.FRIEND,
            birthDateSolar = LocalDate.of(1990, 1, 1),
            note = "Dòng 1\nDòng 2",
            avatarRef = "content://avatar/1",
            draftFields = drafts,
            now = 9000L
        )

        assertEquals("p_existing", savedPerson.id)
        assertEquals("Tên Mới", savedPerson.fullName)
        assertEquals("Nick", savedPerson.nickname)
        assertEquals("Bạn thân", savedPerson.relationshipLabel)
        assertEquals(PersonGroup.FRIEND, savedPerson.group)
        assertEquals(LocalDate.of(1990, 1, 1), savedPerson.birthDateSolar)
        assertEquals("Dòng 1\nDòng 2", savedPerson.note)
        assertEquals("content://avatar/1", savedPerson.avatarRef)
        assertEquals("0772098666", savedPerson.phone)
        assertEquals("123 Lê Lợi, TP.HCM", savedPerson.address)
        assertEquals(5000L, savedPerson.createdAtEpochMillis)
        assertEquals(9000L, savedPerson.updatedAtEpochMillis)

        assertEquals(3, savedFields.size)
        assertEquals(listOf("cf1", "cf2", "cf3"), savedFields.map { it.id })
        assertEquals(listOf(0, 1, 2), savedFields.map { it.sortOrder })
        assertEquals(listOf("0772098666", "079065002954", "123 Lê Lợi, TP.HCM"), savedFields.map { it.value })
    }

    @Test
    fun `prepareSavePayload supports 20 dynamic fields without loss`() {
        val drafts = (1..20).map { i ->
            PersonContactFieldDraft(
                id = "cf_$i",
                type = if (i <= 5) PersonContactFieldType.PHONE else if (i <= 10) PersonContactFieldType.EMAIL else PersonContactFieldType.CUSTOM,
                label = "Trường $i",
                value = "Giá trị tiếng Việt có dấu $i: TP. Hồ Chí Minh"
            )
        }

        val (savedPerson, savedFields) = PersonEditorPresentation.prepareSavePayload(
            existingPerson = null,
            fullName = "Nguyễn Văn Đầy Đủ Trường",
            nickname = "",
            relationshipLabel = "",
            group = PersonGroup.FAMILY,
            birthDateSolar = null,
            note = "Ghi chú dài\nNhiều dòng\n123456",
            avatarRef = null,
            draftFields = drafts,
            now = 1000L
        )

        assertEquals(20, savedFields.size)
        assertEquals("Nguyễn Văn Đầy Đủ Trường", savedPerson.fullName)
        assertEquals("Giá trị tiếng Việt có dấu 1: TP. Hồ Chí Minh", savedPerson.phone)
        assertEquals(0, savedFields.first().sortOrder)
        assertEquals(19, savedFields.last().sortOrder)
    }
}
