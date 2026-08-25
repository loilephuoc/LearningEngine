package vn.loi.learning.android.family

enum class DetailFieldActionType {
    NONE,
    DIAL,
    EMAIL,
    BROWSER
}

data class DetailFieldItem(
    val id: String,
    val type: PersonContactFieldType,
    val label: String,
    val value: String,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
    val actionType: DetailFieldActionType = DetailFieldActionType.NONE
)

data class DetailSection(
    val title: String,
    val fields: List<DetailFieldItem>
)

object PersonDetailPresentation {

    fun displayLabelForField(field: PersonContactField): String {
        val customLabel = field.label?.trim()
        if (!customLabel.isNullOrBlank()) {
            return customLabel
        }
        return when (field.type) {
            PersonContactFieldType.PHONE -> "Điện thoại"
            PersonContactFieldType.EMAIL -> "Email"
            PersonContactFieldType.ADDRESS -> "Địa chỉ"
            PersonContactFieldType.WEBSITE -> "Website"
            PersonContactFieldType.COMPANY -> "Công ty"
            PersonContactFieldType.JOB_TITLE -> "Chức vụ"
            PersonContactFieldType.CUSTOM -> "Thông tin"
        }
    }

    fun actionTypeForField(type: PersonContactFieldType, value: String): DetailFieldActionType {
        if (value.isBlank()) return DetailFieldActionType.NONE
        return when (type) {
            PersonContactFieldType.PHONE -> DetailFieldActionType.DIAL
            PersonContactFieldType.EMAIL -> DetailFieldActionType.EMAIL
            PersonContactFieldType.WEBSITE -> DetailFieldActionType.BROWSER
            else -> DetailFieldActionType.NONE
        }
    }

    fun toDetailFieldItem(field: PersonContactField): DetailFieldItem {
        return DetailFieldItem(
            id = field.id,
            type = field.type,
            label = displayLabelForField(field),
            value = field.value,
            isPrimary = field.isPrimary,
            sortOrder = field.sortOrder,
            actionType = actionTypeForField(field.type, field.value)
        )
    }

    fun groupAndSortFields(fields: List<PersonContactField>): List<DetailSection> {
        val activeFields = fields.filter { it.deletedAtEpochMillis == null && it.value.isNotBlank() }

        val contactFields = mutableListOf<DetailFieldItem>()
        val addressFields = mutableListOf<DetailFieldItem>()
        val workFields = mutableListOf<DetailFieldItem>()
        val otherFields = mutableListOf<DetailFieldItem>()

        for (field in activeFields) {
            val item = toDetailFieldItem(field)
            when (field.type) {
                PersonContactFieldType.PHONE,
                PersonContactFieldType.EMAIL,
                PersonContactFieldType.WEBSITE -> contactFields.add(item)

                PersonContactFieldType.ADDRESS -> addressFields.add(item)

                PersonContactFieldType.COMPANY,
                PersonContactFieldType.JOB_TITLE -> workFields.add(item)

                PersonContactFieldType.CUSTOM -> otherFields.add(item)
            }
        }

        val comparator = compareBy<DetailFieldItem> { it.sortOrder }.thenBy { it.id }

        val sections = mutableListOf<DetailSection>()

        if (contactFields.isNotEmpty()) {
            sections.add(DetailSection("LIÊN HỆ", contactFields.sortedWith(comparator)))
        }
        if (addressFields.isNotEmpty()) {
            sections.add(DetailSection("ĐỊA CHỈ", addressFields.sortedWith(comparator)))
        }
        if (workFields.isNotEmpty()) {
            sections.add(DetailSection("CÔNG VIỆC", workFields.sortedWith(comparator)))
        }
        if (otherFields.isNotEmpty()) {
            sections.add(DetailSection("THÔNG TIN KHÁC", otherFields.sortedWith(comparator)))
        }

        return sections
    }
}
