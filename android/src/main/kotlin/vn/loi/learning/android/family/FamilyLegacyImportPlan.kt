package vn.loi.learning.android.family

/**
 * V3.2 import planning layer.
 *
 * IMPORTANT:
 * - Pure logic only.
 * - No FamilyRepository reference.
 * - No sync.
 * - No Supabase.
 * - No persistent mutation.
 *
 * The UI/executor must build a complete immutable plan BEFORE any write.
 */
internal enum class LegacyImportSkipReason {
    MISSING_NAME,
    REQUIRES_REVIEW,
    POSSIBLE_DUPLICATE,
    STRONG_DUPLICATE
}

internal data class LegacyImportPlanEntry(
    val rowIndex: Int,
    val candidate: LegacyImportCandidate,
    val acceptedFields: List<LegacyImportField>
) {
    val fullName: String
        get() = requireNotNull(candidate.fullName).trim()

    val safeFieldCount: Int
        get() = acceptedFields.size

    val blockedFieldCount: Int
        get() = candidate.fields.count {
            it.disposition == LegacyImportFieldDisposition.BLOCKED
        }

    val reviewFieldCount: Int
        get() = candidate.fields.count {
            it.disposition == LegacyImportFieldDisposition.NEEDS_REVIEW
        }

    val ignoredFieldCount: Int
        get() = candidate.fields.count {
            it.disposition == LegacyImportFieldDisposition.IGNORED
        }
}

internal data class LegacySkippedImportCandidate(
    val rowIndex: Int,
    val displayName: String?,
    val reason: LegacyImportSkipReason
)

internal data class LegacyImportPlan(
    val entries: List<LegacyImportPlanEntry>,
    val skipped: List<LegacySkippedImportCandidate>,
    val totalCandidates: Int
) {
    val peopleToCreate: Int
        get() = entries.size

    val safeFieldsToCreate: Int
        get() = entries.sumOf { it.safeFieldCount }

    val blockedFieldsSkipped: Int
        get() = entries.sumOf { it.blockedFieldCount }

    val reviewFieldsSkipped: Int
        get() = entries.sumOf { it.reviewFieldCount }

    val ignoredFieldsSkipped: Int
        get() = entries.sumOf { it.ignoredFieldCount }

    val skippedCandidates: Int
        get() = skipped.size

    val duplicateCandidatesSkipped: Int
        get() = skipped.count {
            it.reason == LegacyImportSkipReason.POSSIBLE_DUPLICATE ||
                it.reason == LegacyImportSkipReason.STRONG_DUPLICATE
        }

    val reviewCandidatesSkipped: Int
        get() = skipped.count {
            it.reason == LegacyImportSkipReason.REQUIRES_REVIEW
        }

    val invalidCandidatesSkipped: Int
        get() = skipped.count {
            it.reason == LegacyImportSkipReason.MISSING_NAME
        }

    val isEmpty: Boolean
        get() = entries.isEmpty()
}

/**
 * Builds the immutable import plan from an already parsed/mapped preview.
 *
 * [selectedRowIndexes]&#58;  * - null -> all eligible NEW candidates are selected.
 * - non-null -> only NEW candidates whose rowIndex is contained in the set.
 *
 * Non-importable candidates are always skipped, even if their row index is
 * accidentally supplied in [selectedRowIndexes].
 */
internal object FamilyLegacyImportPlanner {

    fun build(
        preview: LegacyImportPreview,
        selectedRowIndexes: Set<Int>? = null
    ): LegacyImportPlan {
        val entries = mutableListOf<LegacyImportPlanEntry>()
        val skipped = mutableListOf<LegacySkippedImportCandidate>()

        preview.candidates.forEach { candidate ->
            val name = candidate.fullName?.trim()

            if (name.isNullOrEmpty()) {
                skipped += LegacySkippedImportCandidate(
                    rowIndex = candidate.rowIndex,
                    displayName = candidate.fullName,
                    reason = LegacyImportSkipReason.MISSING_NAME
                )
                return@forEach
            }

            when (candidate.status) {
                LegacyDuplicateStatus.NEW -> {
                    val selected =
                        selectedRowIndexes == null ||
                            candidate.rowIndex in selectedRowIndexes

                    if (!selected) {
                        return@forEach
                    }

                    val acceptedFields = candidate.fields.filter {
                        it.disposition == LegacyImportFieldDisposition.READY &&
                            !it.value.isNullOrBlank()
                    }

                    entries += LegacyImportPlanEntry(
                        rowIndex = candidate.rowIndex,
                        candidate = candidate,
                        acceptedFields = acceptedFields
                    )
                }

                LegacyDuplicateStatus.REQUIRES_REVIEW -> {
                    skipped += LegacySkippedImportCandidate(
                        rowIndex = candidate.rowIndex,
                        displayName = name,
                        reason = LegacyImportSkipReason.REQUIRES_REVIEW
                    )
                }

                LegacyDuplicateStatus.POSSIBLE_DUPLICATE -> {
                    skipped += LegacySkippedImportCandidate(
                        rowIndex = candidate.rowIndex,
                        displayName = name,
                        reason = LegacyImportSkipReason.POSSIBLE_DUPLICATE
                    )
                }

                LegacyDuplicateStatus.STRONG_DUPLICATE -> {
                    skipped += LegacySkippedImportCandidate(
                        rowIndex = candidate.rowIndex,
                        displayName = name,
                        reason = LegacyImportSkipReason.STRONG_DUPLICATE
                    )
                }
            }
        }

        return LegacyImportPlan(
            entries = entries.toList(),
            skipped = skipped.toList(),
            totalCandidates = preview.candidates.size
        )
    }

    fun selectableRowIndexes(
        preview: LegacyImportPreview
    ): Set<Int> {
        return preview.candidates
            .asSequence()
            .filter { candidate ->
                candidate.status == LegacyDuplicateStatus.NEW &&
                    !candidate.fullName.isNullOrBlank()
            }
            .map { it.rowIndex }
            .toSet()
    }
}
