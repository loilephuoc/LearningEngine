package vn.loi.learning.android.family

import java.time.LocalDate
import kotlin.test.*
import org.junit.Test

class FamilyLegacyImportTest {
    @Test fun `placeholder detection is field safe`() {
        assertTrue(FamilyLegacyImportNormalizer.isPlaceholder("  "))
        assertTrue(FamilyLegacyImportNormalizer.isPlaceholder("000"))
        assertTrue(FamilyLegacyImportNormalizer.isPlaceholder("000000000000"))
        assertTrue(FamilyLegacyImportNormalizer.isPlaceholder("NoT"))
        assertFalse(FamilyLegacyImportNormalizer.isPlaceholder("079065002954"))
        assertFalse(FamilyLegacyImportNormalizer.isPlaceholder("A0B", allZeroIsPlaceholder = true))
    }

    @Test fun `phones preserve display and split only confident separators`() {
        assertEquals(listOf("0772.098.666"), FamilyLegacyImportNormalizer.splitPhones("0772.098.666"))
        assertEquals(listOf("0398 111 222", "0903.444.555"), FamilyLegacyImportNormalizer.splitPhones("0398 111 222 / 0903.444.555"))
        assertNull(FamilyLegacyImportNormalizer.splitPhones("0901234567 / note"))
        assertEquals("0772098666", FamilyLegacyImportNormalizer.normalizePhoneForComparison("0772.098.666"))
    }

    @Test fun `email validation is practical and invalid value stays review only`() {
        assertTrue(FamilyLegacyImportNormalizer.isReasonableEmail("person@example.test"))
        assertFalse(FamilyLegacyImportNormalizer.isReasonableEmail("person at example"))
        val candidate = mapOf(LegacyColumn.FULL_NAME to "Synthetic Person", LegacyColumn.GMAIL to "bad email").candidate()
        assertTrue(candidate.fields.single { it.source == LegacyColumn.GMAIL }.disposition == LegacyImportFieldDisposition.NEEDS_REVIEW)
        assertTrue(candidate.issues.any { it.reason == LegacyImportReason.INVALID_EMAIL })
    }

    @Test fun `dates accept legacy strings and Excel serial while rejecting impossible dates`() {
        assertEquals(LocalDate.of(2020, 2, 29), FamilyLegacyImportNormalizer.parseDate("29/02/2020"))
        assertEquals(LocalDate.of(2020, 2, 29), FamilyLegacyImportNormalizer.parseDate("29-02-2020"))
        assertEquals(LocalDate.of(1900, 1, 1), FamilyLegacyImportNormalizer.parseDate("2"))
        assertNull(FamilyLegacyImportNormalizer.parseDate("31/02/2020"))
        val candidate = mapOf(LegacyColumn.FULL_NAME to "Synthetic Person", LegacyColumn.CCCD_ISSUE_DATE to "01/03/2021").candidate()
        assertEquals("2021-03-01", candidate.readyFields.single { it.source == LegacyColumn.CCCD_ISSUE_DATE }.value)
    }

    @Test fun `credential columns and credential content in wrong column are blocked without values`() {
        val candidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.W_PASS to "synthetic-secret",
            LegacyColumn.W_USER to "synthetic-user",
            LegacyColumn.NOTE to "login username + password: synthetic"
        ).candidate()
        assertEquals(3, candidate.blockedCount)
        assertTrue(candidate.fields.filter { it.disposition == LegacyImportFieldDisposition.BLOCKED }.all { it.value == null })
        assertNull(candidate.note)
        assertTrue(candidate.issues.filter { it.reason == LegacyImportReason.SENSITIVE_CREDENTIAL }.all { it.severity == LegacyImportSeverity.BLOCKED })
    }

    @Test fun `personal and private identifiers are not classified as sensitive credentials`() {
        val candidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.TAX_ID to "8096977172",
            LegacyColumn.CCCD to "079065002954",
            LegacyColumn.SOCIAL_INSURANCE to "7936281812",
            LegacyColumn.SOCIAL_INSURANCE_CARD to "DN47936281812",
            LegacyColumn.BANK_ACCOUNT to "6180205143772",
            LegacyColumn.PRIMARY_PHONE to "0901234567",
            LegacyColumn.STUDENT_CODE to "20120001",
            LegacyColumn.PID to "PID12345",
            LegacyColumn.VINMEC_ID to "VM99999"
        ).candidate()

        assertEquals(0, candidate.blockedCount)
        assertEquals(LegacyDuplicateStatus.NEW, candidate.status)
        assertEquals("8096977172", candidate.readyFields.single { it.source == LegacyColumn.TAX_ID }.value)
        assertEquals("079065002954", candidate.readyFields.single { it.source == LegacyColumn.CCCD }.value)
        assertEquals("7936281812", candidate.readyFields.single { it.source == LegacyColumn.SOCIAL_INSURANCE }.value)
        assertEquals("6180205143772", candidate.readyFields.single { it.source == LegacyColumn.BANK_ACCOUNT }.value)
        assertEquals("0901234567", candidate.readyFields.single { it.source == LegacyColumn.PRIMARY_PHONE }.value)
    }

    @Test fun `content level credential detection blocks explicit authentication patterns and mixed cells`() {
        val mixedTaxCandidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.TAX_ID to "8096977172 / password: synthetic-secret"
        ).candidate()
        assertEquals(1, mixedTaxCandidate.blockedCount)
        assertTrue(mixedTaxCandidate.readyFields.none { it.source == LegacyColumn.TAX_ID })
        val blockedTax = mixedTaxCandidate.fields.single { it.source == LegacyColumn.TAX_ID }
        assertEquals(LegacyImportFieldDisposition.BLOCKED, blockedTax.disposition)
        assertNull(blockedTax.value)

        val pwdCandidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.NOTE to "pwd: synthetic-secret"
        ).candidate()
        assertEquals(1, pwdCandidate.blockedCount)
        assertNull(pwdCandidate.note)

        val userPassCandidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.NOTE to "user: myuser pass: synthetic-secret"
        ).candidate()
        assertEquals(1, userPassCandidate.blockedCount)
        assertNull(userPassCandidate.note)

        val fbPassCandidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.NOTE to "FacebookPass: synthetic-secret"
        ).candidate()
        assertEquals(1, fbPassCandidate.blockedCount)
        assertNull(fbPassCandidate.note)

        val vneidPassCandidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.NOTE to "VNeID pass: synthetic-secret"
        ).candidate()
        assertEquals(1, vneidPassCandidate.blockedCount)
        assertNull(vneidPassCandidate.note)
    }

    @Test fun `core identity and flexible fields follow the 26 column contract`() {
        val candidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.DOB to "15/08/1990",
            LegacyColumn.NOTE to "Safe synthetic note",
            LegacyColumn.CCCD to "012345678901",
            LegacyColumn.BANK to "Synthetic Bank",
            LegacyColumn.BANK_ACCOUNT to "0123000456",
            LegacyColumn.PERMANENT_ADDRESS to "Synthetic address",
            LegacyColumn.JOB to "Synthetic job",
            LegacyColumn.INSURANCE to "Synthetic plan"
        ).candidate()
        assertEquals("Synthetic Person", candidate.fullName)
        assertEquals(LocalDate.of(1990, 8, 15), candidate.birthDateSolar)
        assertEquals("Safe synthetic note", candidate.note)
        assertEquals(PersonContactFieldType.CUSTOM, candidate.readyFields.single { it.source == LegacyColumn.CCCD }.type)
        assertEquals(PersonContactFieldType.CUSTOM, candidate.readyFields.single { it.source == LegacyColumn.BANK }.type)
        assertEquals(PersonContactFieldType.ADDRESS, candidate.readyFields.single { it.source == LegacyColumn.PERMANENT_ADDRESS }.type)
        assertEquals(PersonContactFieldType.JOB_TITLE, candidate.readyFields.single { it.source == LegacyColumn.JOB }.type)
        assertEquals(PersonContactFieldType.CUSTOM, candidate.readyFields.single { it.source == LegacyColumn.INSURANCE }.type)
    }

    @Test fun `AppVssis is review only and all credential columns remain excluded`() {
        val candidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.APP_VSSIS to "synthetic metadata",
            LegacyColumn.W_EMAIL to "synthetic@example.test",
            LegacyColumn.W_USER to "synthetic-user",
            LegacyColumn.W_PASS to "synthetic-secret"
        ).candidate()
        assertEquals(LegacyImportFieldDisposition.NEEDS_REVIEW, candidate.fields.single { it.source == LegacyColumn.APP_VSSIS }.disposition)
        assertEquals(3, candidate.blockedCount)
        assertTrue(candidate.readyFields.none { it.source in setOf(LegacyColumn.W_EMAIL, LegacyColumn.W_USER, LegacyColumn.W_PASS, LegacyColumn.APP_VSSIS) })
        assertEquals(LegacyDuplicateStatus.REQUIRES_REVIEW, candidate.status)
    }

    @Test fun `merged CCCD content is retained for review and never auto mapped`() {
        val candidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.CCCD to "012345678901 / 01-01-2020 / synthetic authority"
        ).candidate()
        assertEquals(LegacyImportFieldDisposition.NEEDS_REVIEW, candidate.fields.single { it.source == LegacyColumn.CCCD }.disposition)
        assertTrue(candidate.readyFields.none { it.source == LegacyColumn.CCCD })
        assertEquals(LegacyDuplicateStatus.REQUIRES_REVIEW, candidate.status)
    }

    @Test fun `duplicate signals are deterministic and name alone never auto merges`() {
        val existing = Person("person-existing", "Synthetic Person", birthDateSolar = LocalDate.of(1990, 8, 15), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val fields = listOf(
            PersonContactField("cccd", existing.id, PersonContactFieldType.CUSTOM, "Số CCCD", "012345678901", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField("phone", existing.id, PersonContactFieldType.PHONE, "Điện thoại chính", "0901.222.333", true, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )
        val snapshot = FamilyLocalSnapshot(persons = listOf(existing), personContactFields = fields)
        val byNameDob = mapOf(LegacyColumn.FULL_NAME to "synthetic person", LegacyColumn.DOB to "15/08/1990").candidate(snapshot)
        assertEquals(LegacyDuplicateStatus.STRONG_DUPLICATE, byNameDob.status)
        assertContains(byNameDob.duplicate!!.reasons, "Trùng họ tên + ngày sinh")
        val byCccd = mapOf(LegacyColumn.FULL_NAME to "Other", LegacyColumn.CCCD to "012345678901").candidate(snapshot)
        assertEquals(LegacyDuplicateStatus.STRONG_DUPLICATE, byCccd.status)
        val byPhone = mapOf(LegacyColumn.FULL_NAME to "Other", LegacyColumn.PRIMARY_PHONE to "0901222333").candidate(snapshot)
        assertEquals(LegacyDuplicateStatus.POSSIBLE_DUPLICATE, byPhone.status)
        val nameOnly = mapOf(LegacyColumn.FULL_NAME to "Synthetic Person").candidate(snapshot)
        assertEquals(LegacyDuplicateStatus.NEW, nameOnly.status)
    }

    @Test fun `conflicts distinguish same new differing empty and sensitive`() {
        val existing = listOf(PersonContactField("phone", "p", PersonContactFieldType.PHONE, "Điện thoại chính", "0901.222.333", true, createdAtEpochMillis = 1, updatedAtEpochMillis = 1))
        val candidate = mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.PRIMARY_PHONE to "0901222333",
            LegacyColumn.GMAIL to "new@example.test",
            LegacyColumn.W_PASS to "synthetic-secret"
        ).candidate()
        val conflicts = FamilyLegacyImportDuplicateDetector.classify(candidate.fields, existing)
        assertEquals(LegacyConflictType.SAME, conflicts.single { it.incoming.source == LegacyColumn.PRIMARY_PHONE }.type)
        assertEquals(LegacyConflictType.NEW_VALUE, conflicts.single { it.incoming.source == LegacyColumn.GMAIL }.type)
        assertEquals(LegacyConflictType.SENSITIVE_BLOCKED, conflicts.single { it.incoming.source == LegacyColumn.W_PASS }.type)
        assertTrue(conflicts.any { it.type == LegacyConflictType.EMPTY_INCOMING })

        val differing = mapOf(LegacyColumn.FULL_NAME to "Synthetic", LegacyColumn.PRIMARY_PHONE to "0909999999").candidate()
        assertEquals(LegacyConflictType.CONFLICT, FamilyLegacyImportDuplicateDetector.classify(differing.fields, existing).single { it.incoming.source == LegacyColumn.PRIMARY_PHONE }.type)
    }

    @Test fun `CSV parser requires all 26 named columns and supports quoted commas`() {
        val values = LegacyColumn.entries.associateWith { "" }.toMutableMap()
        values[LegacyColumn.FULL_NAME] = "Synthetic, Person"
        val parsed = FamilyLegacyCsvParser.parse(csv(values))
        assertTrue(parsed.issues.isEmpty())
        assertEquals(1, parsed.rows.size)
        assertEquals("Synthetic, Person", parsed.rows.single()[LegacyColumn.FULL_NAME])
        val missing = FamilyLegacyCsvParser.parse("Họ và Tên,DOB\nSynthetic,01/01/2000")
        assertTrue(missing.rows.isEmpty())
        assertTrue(missing.issues.all { it.reason == LegacyImportReason.MISSING_REQUIRED_HEADER })
    }

    @Test fun `dry run is pure and has no repository sync or Supabase authority`() {
        val before = FamilyLocalSnapshot(persons = listOf(Person("p", "Existing", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)))
        val csv = csv(mapOf(LegacyColumn.FULL_NAME to "Synthetic Incoming"))
        val preview = FamilyLegacyImportPreviewService.preview(csv, before)
        assertEquals(1, preview.rowsRead)
        assertEquals(before, before.copy())
        val source = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/family/FamilyLegacyImportDuplicateDetector.kt"))
        assertFalse(source.contains("FamilyRepository"))
        assertFalse(source.contains("FamilySync"))
        assertFalse(source.contains("Supabase"))
    }

    private fun Map<LegacyColumn, String>.candidate(snapshot: FamilyLocalSnapshot = FamilyLocalSnapshot()): LegacyImportCandidate =
        FamilyLegacyImportDuplicateDetector.attach(FamilyLegacyImportMapper.map(LegacyPersonRow(2, this)), snapshot)

    private fun csv(values: Map<LegacyColumn, String>): String {
        fun escape(value: String) = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
        return LegacyColumn.entries.joinToString(",") { escape(it.header) } + "\n" +
            LegacyColumn.entries.joinToString(",") { escape(values[it].orEmpty()) }
    }
}
