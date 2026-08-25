package vn.loi.learning.android.family

import java.time.LocalDate
import kotlin.test.*
import org.junit.Test

class FamilyLegacyImportCandidateDetailPresentationTest {
    @Test fun `candidate detail groups every safe review blocked and comparison field`() {
        val candidate = duplicateCandidate()
        val presentation = FamilyLegacyImportCandidateDetailPresentation.build(candidate)

        assertEquals("TRÙNG CHẮC CHẮN", presentation.status)
        assertTrue(presentation.section(CandidateDetailSection.WILL_IMPORT).any { it.targetType == "Person.fullName" })
        assertTrue(presentation.section(CandidateDetailSection.WILL_IMPORT).any { it.targetType == PersonContactFieldType.PHONE.name })
        assertTrue(presentation.section(CandidateDetailSection.NEEDS_REVIEW).any { it.label == LegacyColumn.GMAIL.header && it.displayValue == "malformed email" })
        assertTrue(presentation.section(CandidateDetailSection.BLOCKED).any { it.label == LegacyColumn.W_PASS.header && it.displayValue == FamilyLegacyImportCandidateDetailPresentation.HIDDEN_VALUE })
        assertEquals(candidate.duplicate!!.conflicts.size, presentation.section(CandidateDetailSection.COMPARISON).size)
    }

    @Test fun `strong duplicate explains name DOB and phone and presents both comparison values`() {
        val presentation = FamilyLegacyImportCandidateDetailPresentation.build(duplicateCandidate())
        assertContains(presentation.duplicateReasons, "Trùng họ tên + ngày sinh")
        assertContains(presentation.duplicateReasons, "Trùng số điện thoại")
        val phone = presentation.section(CandidateDetailSection.COMPARISON)
            .single { it.label == "Điện thoại chính" }
        assertEquals("0901.222.333", phone.existingValue)
        assertEquals("0901222333", phone.incomingValue)
        assertEquals(LegacyConflictType.SAME, phone.conflictType)
        assertEquals("GIỐNG NHAU", phone.displayValue)
    }

    @Test fun `blocked credential raw value never enters presentation model`() {
        val rawSecret = "synthetic-password-value"
        val candidate = mappedCandidate(mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.W_PASS to rawSecret
        ))
        val presentation = FamilyLegacyImportCandidateDetailPresentation.build(candidate)
        assertFalse(presentation.toString().contains(rawSecret))
        val blocked = presentation.section(CandidateDetailSection.BLOCKED).single()
        assertEquals("W-pass", blocked.label)
        assertEquals("Nội dung được ẩn", blocked.displayValue)
        assertNull(blocked.incomingValue)
    }

    @Test fun `comparison presentation covers all conflict categories and masks sensitive incoming`() {
        val incoming = listOf(
            LegacyImportField(LegacyColumn.PRIMARY_PHONE, PersonContactFieldType.PHONE, "Điện thoại chính", "0901", "0901", disposition = LegacyImportFieldDisposition.READY),
            LegacyImportField(LegacyColumn.GMAIL, PersonContactFieldType.EMAIL, "Gmail", "new@example.test", "new@example.test", disposition = LegacyImportFieldDisposition.READY),
            LegacyImportField(LegacyColumn.PERMANENT_ADDRESS, PersonContactFieldType.ADDRESS, "Địa chỉ thường trú", "Incoming", disposition = LegacyImportFieldDisposition.READY),
            LegacyImportField(LegacyColumn.PID, null, "PID", disposition = LegacyImportFieldDisposition.IGNORED),
            LegacyImportField(LegacyColumn.W_PASS, null, "W-pass", disposition = LegacyImportFieldDisposition.BLOCKED)
        )
        val existing = listOf(
            PersonContactField("1", "p", PersonContactFieldType.PHONE, "Điện thoại chính", "0901", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            PersonContactField("2", "p", PersonContactFieldType.ADDRESS, "Địa chỉ thường trú", "Existing", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )
        val conflicts = FamilyLegacyImportDuplicateDetector.classify(incoming, existing)
        assertEquals(setOf(LegacyConflictType.SAME, LegacyConflictType.NEW_VALUE, LegacyConflictType.CONFLICT, LegacyConflictType.EMPTY_INCOMING, LegacyConflictType.SENSITIVE_BLOCKED), conflicts.map { it.type }.toSet())
        val candidate = LegacyImportCandidate(2, "Synthetic", null, null, incoming, emptyList(), LegacyDuplicateMatch("p", "Existing", LegacyDuplicateStatus.POSSIBLE_DUPLICATE, listOf("Trùng số điện thoại"), conflicts), LegacyDuplicateStatus.POSSIBLE_DUPLICATE)
        val presentation = FamilyLegacyImportCandidateDetailPresentation.build(candidate)
        assertEquals("Nội dung được ẩn", presentation.section(CandidateDetailSection.COMPARISON).single { it.conflictType == LegacyConflictType.SENSITIVE_BLOCKED }.incomingValue)
        assertTrue(presentation.section(CandidateDetailSection.COMPARISON).all { it.existingValue != null && it.incomingValue != null })
    }

    @Test fun `large candidate presents every field without truncation`() {
        val fields = (1..80).map { index ->
            LegacyImportField(LegacyColumn.INSURANCE, PersonContactFieldType.CUSTOM, "Trường $index", "Giá trị $index", disposition = LegacyImportFieldDisposition.READY)
        }
        val candidate = LegacyImportCandidate(2, "Synthetic Large", null, null, fields, emptyList(), null, LegacyDuplicateStatus.NEW)
        val presentation = FamilyLegacyImportCandidateDetailPresentation.build(candidate)
        assertEquals(81, presentation.section(CandidateDetailSection.WILL_IMPORT).size)
        assertEquals("Trường 80", presentation.section(CandidateDetailSection.WILL_IMPORT).last().label)
    }

    @Test fun `legitimate tax ID and identifiers appear in WILL_IMPORT presentation`() {
        val candidate = mappedCandidate(mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.TAX_ID to "8096977172",
            LegacyColumn.CCCD to "079065002954",
            LegacyColumn.SOCIAL_INSURANCE to "7936281812",
            LegacyColumn.BANK_ACCOUNT to "6180205143772"
        ))
        val presentation = FamilyLegacyImportCandidateDetailPresentation.build(candidate)
        val willImport = presentation.section(CandidateDetailSection.WILL_IMPORT)
        assertTrue(willImport.any { it.label == "Mã số thuế" && it.displayValue == "8096977172" })
        assertTrue(willImport.any { it.label == "Số CCCD" && it.displayValue == "079065002954" })
        assertTrue(willImport.any { it.label == "Số BHXH" && it.displayValue == "7936281812" })
        assertTrue(willImport.any { it.label == "Số tài khoản Bank" && it.displayValue == "6180205143772" })
        assertEquals(0, presentation.blockedCount)
    }

    @Test fun `mixed content with secret is blocked and never exposes raw secret in presentation`() {
        val secretValue = "super-secret-token"
        val candidate = mappedCandidate(mapOf(
            LegacyColumn.FULL_NAME to "Synthetic Person",
            LegacyColumn.TAX_ID to "8096977172 / password: $secretValue",
            LegacyColumn.W_PASS to "wpass-$secretValue",
            LegacyColumn.W_USER to "wuser-account",
            LegacyColumn.W_EMAIL to "wemail@example.test"
        ))
        val presentation = FamilyLegacyImportCandidateDetailPresentation.build(candidate)
        assertFalse(presentation.toString().contains(secretValue))
        assertEquals(4, presentation.blockedCount)
        assertTrue(presentation.section(CandidateDetailSection.BLOCKED).all { it.displayValue == FamilyLegacyImportCandidateDetailPresentation.HIDDEN_VALUE })
        assertTrue(presentation.section(CandidateDetailSection.WILL_IMPORT).none { it.label == "Mã số thuế" })
    }

    @Test fun `detail navigation presentation is read only and owns no repository sync authority`() {
        val before = FamilyLocalSnapshot(persons = listOf(Person("p", "Existing", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)))
        FamilyLegacyImportCandidateDetailPresentation.build(mappedCandidate(mapOf(LegacyColumn.FULL_NAME to "Synthetic")))
        assertEquals(before, before.copy())
        val source = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/family/FamilyLegacyImportPreview.kt"))
        assertFalse(source.contains("repository."))
        assertFalse(source.contains("upsertPerson"))
        assertFalse(source.contains("Supabase"))
        assertFalse(source.contains("Import now"))
    }

    private fun duplicateCandidate(): LegacyImportCandidate {
        val existing = Person("existing", "Synthetic Person", birthDateSolar = LocalDate.of(1990, 8, 15), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val existingFields = listOf(PersonContactField("phone", existing.id, PersonContactFieldType.PHONE, "Điện thoại chính", "0901.222.333", true, createdAtEpochMillis = 1, updatedAtEpochMillis = 1))
        return FamilyLegacyImportDuplicateDetector.attach(
            mappedCandidate(mapOf(
                LegacyColumn.FULL_NAME to "Synthetic Person",
                LegacyColumn.DOB to "15/08/1990",
                LegacyColumn.PRIMARY_PHONE to "0901222333",
                LegacyColumn.GMAIL to "malformed email",
                LegacyColumn.W_PASS to "synthetic-secret"
            )),
            FamilyLocalSnapshot(persons = listOf(existing), personContactFields = existingFields)
        )
    }

    private fun mappedCandidate(values: Map<LegacyColumn, String>): LegacyImportCandidate =
        FamilyLegacyImportMapper.map(LegacyPersonRow(2, values))
}
