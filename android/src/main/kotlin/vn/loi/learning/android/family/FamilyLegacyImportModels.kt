package vn.loi.learning.android.family

import java.time.LocalDate

internal enum class LegacyColumn(val header: String) {
    FULL_NAME("Họ và Tên"), DOB("DOB"), CCCD("Số CCCD"), CCCD_ISSUE_DATE("Ngày cấp"),
    PRIMARY_PHONE("Số điện thoại chính"), SECONDARY_PHONE("Số Điện Thoại Phụ"),
    GMAIL("Gmail"), SECONDARY_EMAIL("Email phụ"), BANK("Ngân Hàng"),
    BANK_ACCOUNT("Số Tài Khoản Bank"), PERMANENT_ADDRESS("Địa chỉ thường trú"),
    TEMPORARY_ADDRESS("Địa chỉ tạm trú"), JOB("Nghề Nghiệp"), SOCIAL_INSURANCE("Số BHXH"),
    SOCIAL_INSURANCE_CARD("Số thẻ BHXH"), TAX_ID("Mã số thuế"), OFFICE_ADDRESS("Địa chỉ cơ quan"),
    STUDENT_CODE("Student Code (MSSV)"), PID("PID"), VINMEC_ID("ID Vinmec"),
    W_EMAIL("W-email"), W_USER("W-user"), W_PASS("W-pass"), NOTE("Ghi Chú"),
    APP_VSSIS("AppVssis"), INSURANCE("Bảo hiểm")
}

internal data class LegacyPersonRow(val rowIndex: Int, val values: Map<LegacyColumn, String>) {
    operator fun get(column: LegacyColumn): String = values[column].orEmpty()
}

internal enum class LegacyImportSeverity { INFO, WARNING, BLOCKED }
internal enum class LegacyImportReason {
    INVALID_COLUMN_COUNT, MISSING_REQUIRED_HEADER, PLACEHOLDER, INVALID_PHONE, INVALID_EMAIL,
    INVALID_DATE, NEEDS_REVIEW, SENSITIVE_CREDENTIAL, MISSING_NAME
}

internal data class LegacyImportIssue(
    val rowIndex: Int,
    val column: LegacyColumn?,
    val severity: LegacyImportSeverity,
    val reason: LegacyImportReason
)

internal enum class LegacyImportFieldDisposition { READY, NEEDS_REVIEW, BLOCKED, IGNORED }

internal data class LegacyImportField(
    val source: LegacyColumn,
    val type: PersonContactFieldType?,
    val label: String,
    val value: String? = null,
    val normalizedComparison: String? = null,
    val isPrimary: Boolean = false,
    val disposition: LegacyImportFieldDisposition
)

internal enum class LegacyDuplicateStatus { NEW, POSSIBLE_DUPLICATE, STRONG_DUPLICATE, REQUIRES_REVIEW }
internal enum class LegacyConflictType { SAME, NEW_VALUE, CONFLICT, EMPTY_INCOMING, SENSITIVE_BLOCKED }

internal data class LegacyFieldConflict(
    val incoming: LegacyImportField,
    val type: LegacyConflictType,
    val existingValue: String? = null
)

internal data class LegacyDuplicateMatch(
    val personId: String,
    val displayName: String,
    val status: LegacyDuplicateStatus,
    val reasons: List<String>,
    val conflicts: List<LegacyFieldConflict>
) {
    val matchingFields: Int get() = conflicts.count { it.type == LegacyConflictType.SAME }
    val newFields: Int get() = conflicts.count { it.type == LegacyConflictType.NEW_VALUE }
    val conflictingFields: Int get() = conflicts.count { it.type == LegacyConflictType.CONFLICT }
}

internal data class LegacyImportCandidate(
    val rowIndex: Int,
    val fullName: String?,
    val birthDateSolar: LocalDate?,
    val note: String?,
    val fields: List<LegacyImportField>,
    val issues: List<LegacyImportIssue>,
    val duplicate: LegacyDuplicateMatch?,
    val status: LegacyDuplicateStatus
) {
    val readyFields: List<LegacyImportField> get() = fields.filter { it.disposition == LegacyImportFieldDisposition.READY }
    val blockedCount: Int get() = fields.count { it.disposition == LegacyImportFieldDisposition.BLOCKED }
    val ignoredCount: Int get() = fields.count { it.disposition == LegacyImportFieldDisposition.IGNORED }
}

internal data class LegacyImportPreview(
    val rowsRead: Int,
    val candidates: List<LegacyImportCandidate>,
    val fileIssues: List<LegacyImportIssue> = emptyList()
) {
    val ready: Int get() = candidates.count { it.status == LegacyDuplicateStatus.NEW }
    val possibleDuplicates: Int get() = candidates.count { it.status in setOf(LegacyDuplicateStatus.POSSIBLE_DUPLICATE, LegacyDuplicateStatus.STRONG_DUPLICATE) }
    val warnings: Int get() = fileIssues.count { it.severity == LegacyImportSeverity.WARNING } + candidates.sumOf { candidate -> candidate.issues.count { it.severity == LegacyImportSeverity.WARNING } }
    val sensitiveBlocked: Int get() = candidates.sumOf(LegacyImportCandidate::blockedCount)
    val invalidOrEmpty: Int get() = candidates.count { it.fullName.isNullOrBlank() }
}

internal data class LegacyCsvParseResult(
    val rows: List<LegacyPersonRow>,
    val issues: List<LegacyImportIssue>
)
