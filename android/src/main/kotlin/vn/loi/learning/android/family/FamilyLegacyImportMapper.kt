package vn.loi.learning.android.family

internal object FamilyLegacyImportMapper {
    private val alwaysBlocked = setOf(LegacyColumn.W_EMAIL, LegacyColumn.W_USER, LegacyColumn.W_PASS)
    private val identifierColumns = setOf(
        LegacyColumn.CCCD, LegacyColumn.BANK_ACCOUNT, LegacyColumn.SOCIAL_INSURANCE,
        LegacyColumn.SOCIAL_INSURANCE_CARD, LegacyColumn.TAX_ID, LegacyColumn.STUDENT_CODE,
        LegacyColumn.PID, LegacyColumn.VINMEC_ID
    )

    fun map(row: LegacyPersonRow): LegacyImportCandidate {
        val issues = mutableListOf<LegacyImportIssue>()
        val fields = mutableListOf<LegacyImportField>()
        val fullName = row[LegacyColumn.FULL_NAME].trim().takeIf(String::isNotEmpty)
        if (fullName == null) issues += issue(row, LegacyColumn.FULL_NAME, LegacyImportSeverity.BLOCKED, LegacyImportReason.MISSING_NAME)

        val birthDate = row[LegacyColumn.DOB].trim().takeIf(String::isNotEmpty)?.let { raw ->
            FamilyLegacyImportNormalizer.parseDate(raw).also { parsed ->
                if (parsed == null) issues += issue(row, LegacyColumn.DOB, LegacyImportSeverity.WARNING, LegacyImportReason.INVALID_DATE)
            }
        }

        val rawNote = row[LegacyColumn.NOTE].trim()
        val note = when {
            rawNote.isEmpty() -> null
            FamilyLegacyImportNormalizer.containsSensitiveCredential(rawNote) -> {
                fields += blocked(LegacyColumn.NOTE)
                issues += issue(row, LegacyColumn.NOTE, LegacyImportSeverity.BLOCKED, LegacyImportReason.SENSITIVE_CREDENTIAL)
                null
            }
            else -> rawNote
        }

        LegacyColumn.entries.filterNot { it in setOf(LegacyColumn.FULL_NAME, LegacyColumn.DOB, LegacyColumn.NOTE) }
            .forEach { column -> mapField(row, column, fields, issues) }

        val provisional = LegacyImportCandidate(row.rowIndex, fullName, birthDate, note, fields, issues, null, LegacyDuplicateStatus.NEW)
        return provisional.copy(status = if (fullName == null || issues.any { it.severity in setOf(LegacyImportSeverity.WARNING, LegacyImportSeverity.BLOCKED) }) LegacyDuplicateStatus.REQUIRES_REVIEW else LegacyDuplicateStatus.NEW)
    }

    private fun mapField(
        row: LegacyPersonRow,
        column: LegacyColumn,
        fields: MutableList<LegacyImportField>,
        issues: MutableList<LegacyImportIssue>
    ) {
        val raw = row[column].trim()
        if (raw.isEmpty()) { fields += ignored(column); return }
        if (column in alwaysBlocked || FamilyLegacyImportNormalizer.containsSensitiveCredential(raw)) {
            fields += blocked(column)
            issues += issue(row, column, LegacyImportSeverity.BLOCKED, LegacyImportReason.SENSITIVE_CREDENTIAL)
            return
        }
        if (column == LegacyColumn.APP_VSSIS) {
            fields += review(column, raw)
            issues += issue(row, column, LegacyImportSeverity.WARNING, LegacyImportReason.NEEDS_REVIEW)
            return
        }
        val allZeroPlaceholder = column in identifierColumns || column in setOf(LegacyColumn.PRIMARY_PHONE, LegacyColumn.SECONDARY_PHONE)
        if (FamilyLegacyImportNormalizer.isPlaceholder(raw, allZeroPlaceholder)) {
            fields += ignored(column)
            issues += issue(row, column, LegacyImportSeverity.INFO, LegacyImportReason.PLACEHOLDER)
            return
        }
        when (column) {
            LegacyColumn.PRIMARY_PHONE, LegacyColumn.SECONDARY_PHONE -> mapPhones(row, column, raw, fields, issues)
            LegacyColumn.GMAIL, LegacyColumn.SECONDARY_EMAIL -> mapEmail(row, column, raw, fields, issues)
            LegacyColumn.CCCD_ISSUE_DATE -> mapIssueDate(row, column, raw, fields, issues)
            LegacyColumn.CCCD -> mapCccd(row, column, raw, fields, issues)
            else -> fields += ready(column, typeFor(column), labelFor(column), raw)
        }
    }

    private fun mapCccd(row: LegacyPersonRow, column: LegacyColumn, raw: String, fields: MutableList<LegacyImportField>, issues: MutableList<LegacyImportIssue>) {
        val digits = raw.filter(Char::isDigit)
        if (raw.any { !it.isDigit() && !it.isWhitespace() } || digits.length !in setOf(9, 12)) {
            fields += review(column, raw)
            issues += issue(row, column, LegacyImportSeverity.WARNING, LegacyImportReason.NEEDS_REVIEW)
        } else fields += ready(column, PersonContactFieldType.CUSTOM, "Số CCCD", raw)
    }

    private fun mapPhones(row: LegacyPersonRow, column: LegacyColumn, raw: String, fields: MutableList<LegacyImportField>, issues: MutableList<LegacyImportIssue>) {
        val parts = FamilyLegacyImportNormalizer.splitPhones(raw)
        if (parts == null || parts.any { !FamilyLegacyImportNormalizer.isReasonablePhone(it) }) {
            fields += review(column, raw)
            issues += issue(row, column, LegacyImportSeverity.WARNING, LegacyImportReason.INVALID_PHONE)
            return
        }
        parts.forEachIndexed { index, value ->
            fields += ready(
                column, PersonContactFieldType.PHONE,
                if (column == LegacyColumn.PRIMARY_PHONE && index == 0) "Điện thoại chính" else "Điện thoại phụ",
                value,
                FamilyLegacyImportNormalizer.normalizePhoneForComparison(value),
                column == LegacyColumn.PRIMARY_PHONE && index == 0
            )
        }
    }

    private fun mapEmail(row: LegacyPersonRow, column: LegacyColumn, raw: String, fields: MutableList<LegacyImportField>, issues: MutableList<LegacyImportIssue>) {
        if (!FamilyLegacyImportNormalizer.isReasonableEmail(raw)) {
            fields += review(column, raw)
            issues += issue(row, column, LegacyImportSeverity.WARNING, LegacyImportReason.INVALID_EMAIL)
            return
        }
        fields += ready(column, PersonContactFieldType.EMAIL, labelFor(column), raw, FamilyLegacyImportNormalizer.normalizeEmail(raw), column == LegacyColumn.GMAIL)
    }

    private fun mapIssueDate(row: LegacyPersonRow, column: LegacyColumn, raw: String, fields: MutableList<LegacyImportField>, issues: MutableList<LegacyImportIssue>) {
        val date = FamilyLegacyImportNormalizer.parseDate(raw)
        if (date == null) {
            fields += review(column, raw)
            issues += issue(row, column, LegacyImportSeverity.WARNING, LegacyImportReason.INVALID_DATE)
        } else fields += ready(column, PersonContactFieldType.CUSTOM, "Ngày cấp CCCD", date.toString())
    }

    private fun typeFor(column: LegacyColumn): PersonContactFieldType = when (column) {
        LegacyColumn.PERMANENT_ADDRESS, LegacyColumn.TEMPORARY_ADDRESS, LegacyColumn.OFFICE_ADDRESS -> PersonContactFieldType.ADDRESS
        LegacyColumn.JOB -> PersonContactFieldType.JOB_TITLE
        else -> PersonContactFieldType.CUSTOM
    }

    private fun labelFor(column: LegacyColumn): String = when (column) {
        LegacyColumn.GMAIL -> "Gmail"
        LegacyColumn.SECONDARY_EMAIL -> "Email phụ"
        LegacyColumn.PERMANENT_ADDRESS -> "Địa chỉ thường trú"
        LegacyColumn.TEMPORARY_ADDRESS -> "Địa chỉ tạm trú"
        LegacyColumn.OFFICE_ADDRESS -> "Địa chỉ cơ quan"
        LegacyColumn.JOB -> "Nghề nghiệp"
        LegacyColumn.CCCD -> "Số CCCD"
        LegacyColumn.BANK -> "Ngân hàng"
        LegacyColumn.BANK_ACCOUNT -> "Số tài khoản Bank"
        LegacyColumn.SOCIAL_INSURANCE -> "Số BHXH"
        LegacyColumn.SOCIAL_INSURANCE_CARD -> "Số thẻ BHXH"
        LegacyColumn.TAX_ID -> "Mã số thuế"
        LegacyColumn.STUDENT_CODE -> "Student Code (MSSV)"
        LegacyColumn.PID -> "PID"
        LegacyColumn.VINMEC_ID -> "ID Vinmec"
        LegacyColumn.INSURANCE -> "Bảo hiểm"
        else -> column.header
    }

    private fun ready(source: LegacyColumn, type: PersonContactFieldType, label: String, value: String, normalized: String? = null, primary: Boolean = false) =
        LegacyImportField(source, type, label, value, normalized, primary, LegacyImportFieldDisposition.READY)
    private fun review(source: LegacyColumn, value: String) = LegacyImportField(source, null, source.header, value = value, disposition = LegacyImportFieldDisposition.NEEDS_REVIEW)
    private fun blocked(source: LegacyColumn) = LegacyImportField(source, null, source.header, disposition = LegacyImportFieldDisposition.BLOCKED)
    private fun ignored(source: LegacyColumn) = LegacyImportField(source, null, source.header, disposition = LegacyImportFieldDisposition.IGNORED)
    private fun issue(row: LegacyPersonRow, column: LegacyColumn, severity: LegacyImportSeverity, reason: LegacyImportReason) =
        LegacyImportIssue(row.rowIndex, column, severity, reason)
}
