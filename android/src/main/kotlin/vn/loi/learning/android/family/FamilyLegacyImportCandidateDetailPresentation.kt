package vn.loi.learning.android.family

internal enum class CandidateDetailSection { WILL_IMPORT, NEEDS_REVIEW, BLOCKED, COMPARISON }

internal data class CandidateDetailFieldItem(
    val section: CandidateDetailSection,
    val label: String,
    val displayValue: String,
    val targetType: String? = null,
    val reason: String? = null,
    val existingValue: String? = null,
    val incomingValue: String? = null,
    val conflictType: LegacyConflictType? = null
)

internal data class CandidateDetailPresentation(
    val title: String,
    val status: String,
    val safeCount: Int,
    val reviewCount: Int,
    val blockedCount: Int,
    val conflictCount: Int,
    val duplicateReasons: List<String>,
    val items: List<CandidateDetailFieldItem>
) {
    fun section(section: CandidateDetailSection): List<CandidateDetailFieldItem> = items.filter { it.section == section }
}

internal object FamilyLegacyImportCandidateDetailPresentation {
    const val HIDDEN_VALUE = "Nội dung được ẩn"

    fun build(candidate: LegacyImportCandidate): CandidateDetailPresentation {
        val items = mutableListOf<CandidateDetailFieldItem>()
        candidate.fullName?.let { items += core("Họ và Tên", it, "Person.fullName") }
        candidate.birthDateSolar?.let { items += core("DOB", it.toString(), "Person.birthDateSolar") }
        candidate.note?.let { items += core("Ghi Chú", it, "Person.note") }
        candidate.fields.forEach { field ->
            when (field.disposition) {
                LegacyImportFieldDisposition.READY -> items += CandidateDetailFieldItem(
                    CandidateDetailSection.WILL_IMPORT,
                    field.label,
                    field.value.orEmpty(),
                    field.type?.name ?: "CUSTOM"
                )
                LegacyImportFieldDisposition.NEEDS_REVIEW -> items += CandidateDetailFieldItem(
                    CandidateDetailSection.NEEDS_REVIEW,
                    field.source.header,
                    field.value.orEmpty(),
                    reason = reviewReason(candidate, field.source)
                )
                LegacyImportFieldDisposition.BLOCKED -> items += CandidateDetailFieldItem(
                    CandidateDetailSection.BLOCKED,
                    field.source.header,
                    HIDDEN_VALUE,
                    reason = "Phát hiện dữ liệu đăng nhập hoặc thông tin xác thực nhạy cảm"
                )
                LegacyImportFieldDisposition.IGNORED -> Unit
            }
        }
        candidate.duplicate?.conflicts?.forEach { conflict ->
            val incoming = if (conflict.type == LegacyConflictType.SENSITIVE_BLOCKED) HIDDEN_VALUE
            else conflict.incoming.value ?: "—"
            items += CandidateDetailFieldItem(
                CandidateDetailSection.COMPARISON,
                conflict.incoming.label,
                conflictLabel(conflict.type),
                existingValue = conflict.existingValue ?: "—",
                incomingValue = incoming,
                conflictType = conflict.type
            )
        }
        return CandidateDetailPresentation(
            title = candidate.fullName ?: "Dòng ${candidate.rowIndex} chưa có tên",
            status = statusLabel(candidate.status),
            safeCount = items.count { it.section == CandidateDetailSection.WILL_IMPORT },
            reviewCount = items.count { it.section == CandidateDetailSection.NEEDS_REVIEW },
            blockedCount = items.count { it.section == CandidateDetailSection.BLOCKED },
            conflictCount = items.count { it.section == CandidateDetailSection.COMPARISON && it.conflictType == LegacyConflictType.CONFLICT },
            duplicateReasons = candidate.duplicate?.reasons.orEmpty(),
            items = items
        )
    }

    fun statusLabel(status: LegacyDuplicateStatus): String = when (status) {
        LegacyDuplicateStatus.NEW -> "SẴN SÀNG"
        LegacyDuplicateStatus.POSSIBLE_DUPLICATE -> "CÓ THỂ TRÙNG"
        LegacyDuplicateStatus.STRONG_DUPLICATE -> "TRÙNG CHẮC CHẮN"
        LegacyDuplicateStatus.REQUIRES_REVIEW -> "CẦN XEM LẠI"
    }

    fun conflictLabel(type: LegacyConflictType): String = when (type) {
        LegacyConflictType.SAME -> "GIỐNG NHAU"
        LegacyConflictType.NEW_VALUE -> "GIÁ TRỊ MỚI"
        LegacyConflictType.CONFLICT -> "XUNG ĐỘT"
        LegacyConflictType.EMPTY_INCOMING -> "CSV ĐỂ TRỐNG"
        LegacyConflictType.SENSITIVE_BLOCKED -> "NHẠY CẢM — ĐÃ CHẶN"
    }

    private fun core(label: String, value: String, target: String) = CandidateDetailFieldItem(
        CandidateDetailSection.WILL_IMPORT, label, value, target
    )

    private fun reviewReason(candidate: LegacyImportCandidate, column: LegacyColumn): String =
        when (candidate.issues.firstOrNull { it.column == column }?.reason) {
            LegacyImportReason.INVALID_PHONE -> "Số điện thoại không đủ tin cậy để tách hoặc chuẩn hóa"
            LegacyImportReason.INVALID_EMAIL -> "Định dạng email không hợp lệ"
            LegacyImportReason.INVALID_DATE -> "Ngày không hợp lệ hoặc không thể xác định chắc chắn"
            LegacyImportReason.NEEDS_REVIEW -> "Dữ liệu ghép, chưa phân loại hoặc cần xác nhận thủ công"
            else -> "Cần xác nhận thủ công trước khi nhập"
        }
}
