package vn.loi.learning.android.family

import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FamilyLegacyImportPlanTest {

    @Test
    fun `NEW candidate includes only READY fields`() {
        val candidate = candidate(
            rowIndex = 2,
            status = LegacyDuplicateStatus.NEW,
            fields = listOf(
                field(
                    LegacyColumn.PRIMARY_PHONE,
                    LegacyImportFieldDisposition.READY,
                    "0901234567"
                ),
                field(
                    LegacyColumn.APP_VSSIS,
                    LegacyImportFieldDisposition.NEEDS_REVIEW,
                    "review-value"
                ),
                field(
                    LegacyColumn.W_PASS,
                    LegacyImportFieldDisposition.BLOCKED,
                    null
                ),
                field(
                    LegacyColumn.BANK_ACCOUNT,
                    LegacyImportFieldDisposition.IGNORED,
                    null
                )
            )
        )

        val plan = FamilyLegacyImportPlanner.build(
            preview(candidate)
        )

        assertEquals(1, plan.peopleToCreate)
        assertEquals(1, plan.safeFieldsToCreate)
        assertEquals(
            LegacyColumn.PRIMARY_PHONE,
            plan.entries.single().acceptedFields.single().source
        )

        assertEquals(1, plan.reviewFieldsSkipped)
        assertEquals(1, plan.blockedFieldsSkipped)
        assertEquals(1, plan.ignoredFieldsSkipped)
    }

    @Test
    fun `strong duplicate is never importable`() {
        val candidate = candidate(
            rowIndex = 7,
            status = LegacyDuplicateStatus.STRONG_DUPLICATE
        )

        val plan = FamilyLegacyImportPlanner.build(
            preview(candidate),
            selectedRowIndexes = setOf(7)
        )

        assertTrue(plan.entries.isEmpty())
        assertEquals(1, plan.duplicateCandidatesSkipped)
        assertEquals(
            LegacyImportSkipReason.STRONG_DUPLICATE,
            plan.skipped.single().reason
        )
    }

    @Test
    fun `possible duplicate is never importable`() {
        val candidate = candidate(
            rowIndex = 8,
            status = LegacyDuplicateStatus.POSSIBLE_DUPLICATE
        )

        val plan = FamilyLegacyImportPlanner.build(
            preview(candidate),
            selectedRowIndexes = setOf(8)
        )

        assertTrue(plan.entries.isEmpty())
        assertEquals(
            LegacyImportSkipReason.POSSIBLE_DUPLICATE,
            plan.skipped.single().reason
        )
    }

    @Test
    fun `review candidate is never importable`() {
        val candidate = candidate(
            rowIndex = 9,
            status = LegacyDuplicateStatus.REQUIRES_REVIEW
        )

        val plan = FamilyLegacyImportPlanner.build(
            preview(candidate),
            selectedRowIndexes = setOf(9)
        )

        assertTrue(plan.entries.isEmpty())
        assertEquals(1, plan.reviewCandidatesSkipped)
    }

    @Test
    fun `missing name is never importable`() {
        val candidate = LegacyImportCandidate(
            rowIndex = 10,
            fullName = "   ",
            birthDateSolar = null,
            note = null,
            fields = emptyList(),
            issues = emptyList(),
            duplicate = null,
            status = LegacyDuplicateStatus.NEW
        )

        val plan = FamilyLegacyImportPlanner.build(
            preview(candidate),
            selectedRowIndexes = setOf(10)
        )

        assertTrue(plan.entries.isEmpty())
        assertEquals(1, plan.invalidCandidatesSkipped)
    }

    @Test
    fun `selection limits eligible candidates`() {
        val first = candidate(
            rowIndex = 2,
            status = LegacyDuplicateStatus.NEW,
            name = "Người A"
        )
        val second = candidate(
            rowIndex = 3,
            status = LegacyDuplicateStatus.NEW,
            name = "Người B"
        )

        val preview = LegacyImportPreview(
            rowsRead = 2,
            candidates = listOf(first, second)
        )

        val plan = FamilyLegacyImportPlanner.build(
            preview,
            selectedRowIndexes = setOf(3)
        )

        assertEquals(1, plan.peopleToCreate)
        assertEquals(3, plan.entries.single().rowIndex)
    }

    @Test
    fun `null selection means all eligible NEW candidates`() {
        val readyA = candidate(
            rowIndex = 2,
            status = LegacyDuplicateStatus.NEW
        )
        val readyB = candidate(
            rowIndex = 3,
            status = LegacyDuplicateStatus.NEW
        )
        val duplicate = candidate(
            rowIndex = 4,
            status = LegacyDuplicateStatus.STRONG_DUPLICATE
        )

        val plan = FamilyLegacyImportPlanner.build(
            LegacyImportPreview(
                rowsRead = 3,
                candidates = listOf(
                    readyA,
                    readyB,
                    duplicate
                )
            )
        )

        assertEquals(2, plan.peopleToCreate)
        assertEquals(1, plan.duplicateCandidatesSkipped)
    }

    @Test
    fun `selectable rows contain only valid NEW candidates`() {
        val preview = LegacyImportPreview(
            rowsRead = 5,
            candidates = listOf(
                candidate(
                    rowIndex = 2,
                    status = LegacyDuplicateStatus.NEW,
                    name = "A"
                ),
                candidate(
                    rowIndex = 3,
                    status = LegacyDuplicateStatus.REQUIRES_REVIEW,
                    name = "B"
                ),
                candidate(
                    rowIndex = 4,
                    status = LegacyDuplicateStatus.POSSIBLE_DUPLICATE,
                    name = "C"
                ),
                candidate(
                    rowIndex = 5,
                    status = LegacyDuplicateStatus.STRONG_DUPLICATE,
                    name = "D"
                ),
                LegacyImportCandidate(
                    rowIndex = 6,
                    fullName = "",
                    birthDateSolar = null,
                    note = null,
                    fields = emptyList(),
                    issues = emptyList(),
                    duplicate = null,
                    status = LegacyDuplicateStatus.NEW
                )
            )
        )

        assertEquals(
            setOf(2),
            FamilyLegacyImportPlanner.selectableRowIndexes(preview)
        )
    }

    @Test
    fun `plan does not mutate source candidate`() {
        val originalFields = listOf(
            field(
                LegacyColumn.PRIMARY_PHONE,
                LegacyImportFieldDisposition.READY,
                "0901234567"
            ),
            field(
                LegacyColumn.W_PASS,
                LegacyImportFieldDisposition.BLOCKED,
                null
            )
        )

        val candidate = candidate(
            rowIndex = 2,
            status = LegacyDuplicateStatus.NEW,
            fields = originalFields
        )

        FamilyLegacyImportPlanner.build(
            preview(candidate)
        )

        assertEquals(originalFields, candidate.fields)
        assertEquals(2, candidate.fields.size)
    }

    @Test
    fun `empty explicit selection creates empty execution plan`() {
        val candidate = candidate(
            rowIndex = 2,
            status = LegacyDuplicateStatus.NEW
        )

        val plan = FamilyLegacyImportPlanner.build(
            preview(candidate),
            selectedRowIndexes = emptySet()
        )

        assertTrue(plan.entries.isEmpty())
        assertTrue(plan.isEmpty)
        assertEquals(0, plan.peopleToCreate)
    }


    @Test
    fun `batch import plan keeps exactly 10 NEW candidates and excludes duplicates and review rows`() {
        val readyCandidates = (2..11).map { rowIndex ->
            candidate(
                rowIndex = rowIndex,
                status = LegacyDuplicateStatus.NEW,
                name = "Người an toàn $rowIndex",
                fields = listOf(
                    field(
                        LegacyColumn.PRIMARY_PHONE,
                        LegacyImportFieldDisposition.READY,
                        "09000000$rowIndex"
                    )
                )
            )
        }

        val strongDuplicate = candidate(
            rowIndex = 12,
            status = LegacyDuplicateStatus.STRONG_DUPLICATE,
            name = "Người trùng chắc chắn"
        )

        val possibleDuplicate = candidate(
            rowIndex = 13,
            status = LegacyDuplicateStatus.POSSIBLE_DUPLICATE,
            name = "Người có thể trùng"
        )

        val reviewCandidates = (14..16).map { rowIndex ->
            candidate(
                rowIndex = rowIndex,
                status = LegacyDuplicateStatus.REQUIRES_REVIEW,
                name = "Người cần xem lại $rowIndex"
            )
        }

        val preview = LegacyImportPreview(
            rowsRead = 15,
            candidates =
                readyCandidates +
                        strongDuplicate +
                        possibleDuplicate +
                        reviewCandidates
        )

        val selectable =
            FamilyLegacyImportPlanner.selectableRowIndexes(preview)

        assertEquals(
            (2..11).toSet(),
            selectable
        )

        /*
         * Deliberately supply every row index, including duplicate/review rows,
         * to prove the planner still refuses to import them.
         */
        val plan = FamilyLegacyImportPlanner.build(
            preview = preview,
            selectedRowIndexes = (2..16).toSet()
        )

        assertEquals(10, plan.peopleToCreate)
        assertEquals(10, plan.safeFieldsToCreate)

        assertEquals(
            (2..11).toList(),
            plan.entries.map { it.rowIndex }
        )

        assertEquals(
            2,
            plan.duplicateCandidatesSkipped
        )

        assertEquals(
            3,
            plan.reviewCandidatesSkipped
        )

        assertEquals(
            0,
            plan.invalidCandidatesSkipped
        )

        assertTrue(
            plan.entries.all {
                it.candidate.status ==
                        LegacyDuplicateStatus.NEW
            }
        )

        assertTrue(
            plan.entries.none {
                it.rowIndex in setOf(
                    12,
                    13,
                    14,
                    15,
                    16
                )
            }
        )
    }


    private fun preview(
        candidate: LegacyImportCandidate
    ): LegacyImportPreview {
        return LegacyImportPreview(
            rowsRead = 1,
            candidates = listOf(candidate)
        )
    }

    private fun candidate(
        rowIndex: Int,
        status: LegacyDuplicateStatus,
        name: String = "Người thử nghiệm",
        fields: List<LegacyImportField> = emptyList()
    ): LegacyImportCandidate {
        return LegacyImportCandidate(
            rowIndex = rowIndex,
            fullName = name,
            birthDateSolar = LocalDate.of(1990, 1, 2),
            note = null,
            fields = fields,
            issues = emptyList(),
            duplicate = null,
            status = status
        )
    }

    private fun field(
        source: LegacyColumn,
        disposition: LegacyImportFieldDisposition,
        value: String?
    ): LegacyImportField {
        return LegacyImportField(
            source = source,
            type = when (source) {
                LegacyColumn.PRIMARY_PHONE,
                LegacyColumn.SECONDARY_PHONE ->
                    PersonContactFieldType.PHONE

                else ->
                    PersonContactFieldType.CUSTOM
            },
            label = source.header,
            value = value,
            disposition = disposition
        )
    }
}
