package vn.loi.learning.android.family

import java.time.LocalDate
import java.util.UUID

data class PersonContactFieldDraft(
    val id: String = UUID.randomUUID().toString(),
    val type: PersonContactFieldType,
    val label: String = "",
    val value: String = "",
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0
) {
    companion object {
        fun from(field: PersonContactField): PersonContactFieldDraft = PersonContactFieldDraft(
            id = field.id,
            type = field.type,
            label = field.label.orEmpty(),
            value = field.value,
            isPrimary = field.isPrimary,
            sortOrder = field.sortOrder
        )
    }

    fun toDomain(personId: String, now: Long = System.currentTimeMillis()): PersonContactField? {
        val trimmedVal = value.trim()
        if (trimmedVal.isBlank()) return null
        val trimmedLabel = label.trim()
        val finalLabel = when (type) {
            PersonContactFieldType.CUSTOM -> if (trimmedLabel.isNotBlank()) trimmedLabel else "Thông tin"
            else -> trimmedLabel.takeIf(String::isNotBlank)
        }
        return PersonContactField(
            id = id,
            personId = personId,
            type = type,
            label = finalLabel,
            value = trimmedVal,
            isPrimary = isPrimary,
            sortOrder = sortOrder,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
    }
}

enum class FieldTemplateCategory(val title: String) {
    CONTACT("Liên hệ"),
    ADDRESS("Địa chỉ"),
    WORK_PROFILE("Công việc & Hồ sơ"),
    OTHER("Khác")
}

data class FieldTemplate(
    val title: String,
    val type: PersonContactFieldType,
    val defaultLabel: String,
    val isCustom: Boolean = false,
    val category: FieldTemplateCategory = FieldTemplateCategory.OTHER
)

object PersonEditorPresentation {

    val FIELD_TEMPLATES: List<FieldTemplate> = listOf(
        // Liên hệ
        FieldTemplate("Điện thoại", PersonContactFieldType.PHONE, "Điện thoại", category = FieldTemplateCategory.CONTACT),
        FieldTemplate("Email", PersonContactFieldType.EMAIL, "Email", category = FieldTemplateCategory.CONTACT),
        FieldTemplate("Website", PersonContactFieldType.WEBSITE, "Website", category = FieldTemplateCategory.CONTACT),
        // Địa chỉ
        FieldTemplate("Thường trú", PersonContactFieldType.ADDRESS, "Thường trú", category = FieldTemplateCategory.ADDRESS),
        FieldTemplate("Tạm trú", PersonContactFieldType.ADDRESS, "Tạm trú", category = FieldTemplateCategory.ADDRESS),
        FieldTemplate("Địa chỉ", PersonContactFieldType.ADDRESS, "Địa chỉ", category = FieldTemplateCategory.ADDRESS),
        // Công việc & Hồ sơ
        FieldTemplate("Số CCCD", PersonContactFieldType.CUSTOM, "Số CCCD", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Ngày cấp CCCD", PersonContactFieldType.CUSTOM, "Ngày cấp", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Nơi cấp CCCD", PersonContactFieldType.CUSTOM, "Nơi cấp", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Ngân hàng", PersonContactFieldType.CUSTOM, "Ngân hàng", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Số tài khoản", PersonContactFieldType.CUSTOM, "Số tài khoản", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Nghề nghiệp / Chức vụ", PersonContactFieldType.JOB_TITLE, "Chức vụ", category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Công ty / Đơn vị", PersonContactFieldType.COMPANY, "Công ty", category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Mã số BHXH", PersonContactFieldType.CUSTOM, "Mã số BHXH", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Mã thẻ BHYT", PersonContactFieldType.CUSTOM, "Mã thẻ BHYT", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        FieldTemplate("Mã số thuế", PersonContactFieldType.CUSTOM, "Mã số thuế", isCustom = true, category = FieldTemplateCategory.WORK_PROFILE),
        // Tùy chỉnh
        FieldTemplate("Trường tùy chỉnh", PersonContactFieldType.CUSTOM, "", isCustom = true, category = FieldTemplateCategory.OTHER)
    )

    fun createDraftFromTemplate(template: FieldTemplate, nextSortOrder: Int): PersonContactFieldDraft {
        return PersonContactFieldDraft(
            id = UUID.randomUUID().toString(),
            type = template.type,
            label = template.defaultLabel,
            value = "",
            isPrimary = false,
            sortOrder = nextSortOrder
        )
    }

    fun isDirty(
        initialPerson: Person?,
        initialContactFields: List<PersonContactField>,
        fullName: String,
        nickname: String,
        relationshipLabel: String,
        group: PersonGroup,
        birthDateSolar: LocalDate?,
        note: String,
        avatarRef: String?,
        draftFields: List<PersonContactFieldDraft>
    ): Boolean {
        if (initialPerson == null) {
            return fullName.isNotBlank() ||
                nickname.isNotBlank() ||
                relationshipLabel.isNotBlank() ||
                group != PersonGroup.FAMILY ||
                birthDateSolar != null ||
                note.isNotBlank() ||
                avatarRef != null ||
                draftFields.any { it.value.isNotBlank() || it.label.isNotBlank() }
        }

        if (fullName.trim() != initialPerson.fullName.trim()) return true
        if (nickname.trim() != initialPerson.nickname.orEmpty().trim()) return true
        if (relationshipLabel.trim() != initialPerson.relationshipLabel.orEmpty().trim()) return true
        if (group != initialPerson.group) return true
        if (birthDateSolar != initialPerson.birthDateSolar) return true
        if (note.trim() != initialPerson.note.orEmpty().trim()) return true
        if (avatarRef != initialPerson.avatarRef) return true

        val initialActive = initialContactFields.filter { it.deletedAtEpochMillis == null }
        val activeDrafts = draftFields.filter { it.value.isNotBlank() }

        if (activeDrafts.size != initialActive.size) return true

        for (draft in activeDrafts) {
            val init = initialActive.firstOrNull { it.id == draft.id } ?: return true
            if (draft.type != init.type) return true
            if (draft.label.trim() != init.label.orEmpty().trim()) return true
            if (draft.value.trim() != init.value.trim()) return true
            if (draft.isPrimary != init.isPrimary) return true
        }

        return false
    }

    fun prepareSavePayload(
        existingPerson: Person?,
        fullName: String,
        nickname: String,
        relationshipLabel: String,
        group: PersonGroup,
        birthDateSolar: LocalDate?,
        note: String,
        avatarRef: String?,
        draftFields: List<PersonContactFieldDraft>,
        now: Long = System.currentTimeMillis()
    ): Pair<Person, List<PersonContactField>> {
        val personId = existingPerson?.id ?: UUID.randomUUID().toString()

        val validFields = draftFields.mapIndexedNotNull { index, draft ->
            draft.copy(sortOrder = index).toDomain(personId, now)
        }

        val primaryPhone = validFields.firstOrNull { it.type == PersonContactFieldType.PHONE && it.isPrimary }
            ?: validFields.firstOrNull { it.type == PersonContactFieldType.PHONE }
        val primaryAddress = validFields.firstOrNull { it.type == PersonContactFieldType.ADDRESS && it.isPrimary }
            ?: validFields.firstOrNull { it.type == PersonContactFieldType.ADDRESS }

        val person = Person(
            id = personId,
            fullName = fullName.trim(),
            nickname = nickname.trim().takeIf(String::isNotBlank),
            group = group,
            relationshipLabel = relationshipLabel.trim().takeIf(String::isNotBlank),
            birthDateSolar = birthDateSolar,
            phone = primaryPhone?.value,
            address = primaryAddress?.value,
            note = note.trim().takeIf(String::isNotBlank),
            avatarRef = avatarRef,
            createdAtEpochMillis = existingPerson?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
            deletedAtEpochMillis = existingPerson?.deletedAtEpochMillis
        )

        return person to validFields
    }
}
