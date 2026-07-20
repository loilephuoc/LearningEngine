package vn.loi.learning.domain.content.model

/**
 * Tập hợp các custom field của một Content.
 *
 * Mỗi ContentFieldId chỉ được xuất hiện một lần.
 */
data class ContentCustomFields(
    val fields: Set<ContentCustomField> = emptySet()
) {

    init {
        require(fields.map { it.id }.toSet().size == fields.size) {
            "Duplicate ContentFieldId is not allowed."
        }
    }

    fun isEmpty(): Boolean = fields.isEmpty()

    operator fun get(id: ContentFieldId): ContentCustomField? =
        fields.firstOrNull { it.id == id }
}
