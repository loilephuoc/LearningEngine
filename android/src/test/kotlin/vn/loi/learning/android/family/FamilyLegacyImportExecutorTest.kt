package vn.loi.learning.android.family

import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FamilyLegacyImportExecutorTest {

    @Test
    fun `prepareWrite preserves safe person core values`() {
        val entry = entry(
            rowIndex = 2,
            name = "Người thử nghiệm",
            birthDate = LocalDate.of(1990, 5, 20),
            note = "Ghi chú an toàn"
        )

        val write = FamilyLegacyImportExecutor.prepareWrite(entry)

        assertEquals("Người thử nghiệm", write.person.fullName)
        assertEquals(
            LocalDate.of(1990, 5, 20),
            write.person.birthDateSolar
        )
        assertEquals("Ghi chú an toàn", write.person.note)
    }

    @Test
    fun `prepareWrite creates only accepted fields`() {
        val accepted = listOf(
            readyField(
                source = LegacyColumn.PRIMARY_PHONE,
                type = PersonContactFieldType.PHONE,
                label = "Di động",
                value = "0901234567",
                isPrimary = true
            ),
            readyField(
                source = LegacyColumn.GMAIL,
                type = PersonContactFieldType.EMAIL,
                label = "Email",
                value = "person@example.com"
            )
        )

        val entry = entry(
            rowIndex = 2,
            acceptedFields = accepted
        )

        val write = FamilyLegacyImportExecutor.prepareWrite(entry)

        assertEquals(2, write.fields.size)
        assertEquals(
            PersonContactFieldType.PHONE,
            write.fields[0].type
        )
        assertTrue(write.fields[0].isPrimary)
        assertEquals(
            PersonContactFieldType.EMAIL,
            write.fields[1].type
        )
    }

    @Test
    fun `prepareWrite preserves field order deterministically`() {
        val entry = entry(
            rowIndex = 2,
            acceptedFields = listOf(
                readyField(
                    LegacyColumn.PRIMARY_PHONE,
                    PersonContactFieldType.PHONE,
                    "Điện thoại",
                    "0901234567"
                ),
                readyField(
                    LegacyColumn.GMAIL,
                    PersonContactFieldType.EMAIL,
                    "Email",
                    "person@example.com"
                ),
                readyField(
                    LegacyColumn.PERMANENT_ADDRESS,
                    PersonContactFieldType.ADDRESS,
                    "Thường trú",
                    "Địa chỉ thử nghiệm"
                )
            )
        )

        val write = FamilyLegacyImportExecutor.prepareWrite(entry)

        assertEquals(
            listOf(0, 1, 2),
            write.fields.map { it.sortOrder }
        )
    }

    @Test
    fun `executor writes each planned candidate exactly once`() {
        val plan = plan(
            entry(rowIndex = 2, name = "Người A"),
            entry(rowIndex = 3, name = "Người B")
        )

        val writes = mutableListOf<LegacyImportCandidateWrite>()

        val result = FamilyLegacyImportExecutor.execute(
            plan = plan,
            sink = FamilyLegacyImportWriteSink { write ->
                writes += write
                Result.success(Unit)
            }
        )

        assertEquals(2, writes.size)
        assertEquals(listOf(2, 3), writes.map { it.rowIndex })

        assertEquals(2, result.attemptedCandidates)
        assertEquals(2, result.successfulCandidates)
        assertEquals(0, result.failedCandidates)
        assertTrue(result.completedSuccessfully)
    }

    @Test
    fun `executor reports candidate failure and continues later candidates`() {
        val plan = plan(
            entry(rowIndex = 2, name = "Người A"),
            entry(rowIndex = 3, name = "Người B"),
            entry(rowIndex = 4, name = "Người C")
        )

        val attempted = mutableListOf<Int>()

        val result = FamilyLegacyImportExecutor.execute(
            plan = plan,
            sink = FamilyLegacyImportWriteSink { write ->
                attempted += write.rowIndex

                if (write.rowIndex == 3) {
                    Result.failure(
                        IllegalStateException("synthetic failure")
                    )
                } else {
                    Result.success(Unit)
                }
            }
        )

        assertEquals(listOf(2, 3, 4), attempted)
        assertEquals(3, result.attemptedCandidates)
        assertEquals(2, result.successfulCandidates)
        assertEquals(1, result.failedCandidates)
        assertFalse(result.completedSuccessfully)

        assertEquals(
            LegacyImportExecutionItemStatus.FAILED,
            result.items.single { it.rowIndex == 3 }.status
        )
    }

    @Test
    fun `executor does not retry failed candidate automatically`() {
        val plan = plan(
            entry(rowIndex = 2)
        )

        var calls = 0

        FamilyLegacyImportExecutor.execute(
            plan = plan,
            sink = FamilyLegacyImportWriteSink {
                calls += 1
                Result.failure(
                    IllegalStateException("synthetic failure")
                )
            }
        )

        assertEquals(1, calls)
    }

    @Test
    fun `empty plan performs zero writes`() {
        var calls = 0

        val result = FamilyLegacyImportExecutor.execute(
            plan = LegacyImportPlan(
                entries = emptyList(),
                skipped = emptyList(),
                totalCandidates = 0
            ),
            sink = FamilyLegacyImportWriteSink {
                calls += 1
                Result.success(Unit)
            }
        )

        assertEquals(0, calls)
        assertEquals(0, result.attemptedCandidates)
        assertEquals(0, result.successfulCandidates)
        assertEquals(0, result.failedCandidates)
        assertTrue(result.completedSuccessfully)
    }

    @Test
    fun `executor never sees blocked or review fields excluded by planner`() {
        val candidate = candidate(
            rowIndex = 2,
            fields = listOf(
                readyField(
                    LegacyColumn.PRIMARY_PHONE,
                    PersonContactFieldType.PHONE,
                    "Điện thoại",
                    "0901234567"
                ),
                LegacyImportField(
                    source = LegacyColumn.APP_VSSIS,
                    type = PersonContactFieldType.CUSTOM,
                    label = "AppVssis",
                    value = "review-value",
                    disposition =
                        LegacyImportFieldDisposition.NEEDS_REVIEW
                ),
                LegacyImportField(
                    source = LegacyColumn.W_PASS,
                    type = PersonContactFieldType.CUSTOM,
                    label = "W-pass",
                    value = null,
                    disposition =
                        LegacyImportFieldDisposition.BLOCKED
                )
            )
        )

        val preview = LegacyImportPreview(
            rowsRead = 1,
            candidates = listOf(candidate)
        )

        val plan = FamilyLegacyImportPlanner.build(preview)

        lateinit var captured: LegacyImportCandidateWrite

        FamilyLegacyImportExecutor.execute(
            plan,
            FamilyLegacyImportWriteSink {
                captured = it
                Result.success(Unit)
            }
        )

        assertEquals(1, captured.fields.size)
        assertEquals(
            "Điện thoại",
            captured.fields.single().label
        )
    }

    @Test
    fun `execution result counts only fields successfully written`() {
        val first = entry(
            rowIndex = 2,
            acceptedFields = listOf(
                readyField(
                    LegacyColumn.PRIMARY_PHONE,
                    PersonContactFieldType.PHONE,
                    "Điện thoại",
                    "0901234567"
                ),
                readyField(
                    LegacyColumn.GMAIL,
                    PersonContactFieldType.EMAIL,
                    "Email",
                    "a@example.com"
                )
            )
        )

        val second = entry(
            rowIndex = 3,
            acceptedFields = listOf(
                readyField(
                    LegacyColumn.PRIMARY_PHONE,
                    PersonContactFieldType.PHONE,
                    "Điện thoại",
                    "0912345678"
                )
            )
        )

        val result = FamilyLegacyImportExecutor.execute(
            plan(first, second),
            FamilyLegacyImportWriteSink { write ->
                if (write.rowIndex == 3) {
                    Result.failure(
                        IllegalStateException("synthetic")
                    )
                } else {
                    Result.success(Unit)
                }
            }
        )

        assertEquals(2, result.fieldsWritten)
        assertEquals(1, result.successfulCandidates)
        assertEquals(1, result.failedCandidates)
    }

    private fun plan(
        vararg entries: LegacyImportPlanEntry
    ): LegacyImportPlan {
        return LegacyImportPlan(
            entries = entries.toList(),
            skipped = emptyList(),
            totalCandidates = entries.size
        )
    }

    private fun entry(
        rowIndex: Int,
        name: String = "Người thử nghiệm",
        birthDate: LocalDate? = LocalDate.of(1990, 1, 2),
        note: String? = null,
        acceptedFields: List<LegacyImportField> = emptyList()
    ): LegacyImportPlanEntry {
        val candidate = candidate(
            rowIndex = rowIndex,
            name = name,
            birthDate = birthDate,
            note = note,
            fields = acceptedFields
        )

        return LegacyImportPlanEntry(
            rowIndex = rowIndex,
            candidate = candidate,
            acceptedFields = acceptedFields
        )
    }

    private fun candidate(
        rowIndex: Int,
        name: String = "Người thử nghiệm",
        birthDate: LocalDate? = LocalDate.of(1990, 1, 2),
        note: String? = null,
        fields: List<LegacyImportField> = emptyList()
    ): LegacyImportCandidate {
        return LegacyImportCandidate(
            rowIndex = rowIndex,
            fullName = name,
            birthDateSolar = birthDate,
            note = note,
            fields = fields,
            issues = emptyList(),
            duplicate = null,
            status = LegacyDuplicateStatus.NEW
        )
    }

    private fun readyField(
        source: LegacyColumn,
        type: PersonContactFieldType,
        label: String,
        value: String,
        isPrimary: Boolean = false
    ): LegacyImportField {
        return LegacyImportField(
            source = source,
            type = type,
            label = label,
            value = value,
            normalizedComparison = null,
            isPrimary = isPrimary,
            disposition = LegacyImportFieldDisposition.READY
        )
    }
}
