package vn.loi.learning.android.family

import java.time.LocalDate

/**
 * V3.2-B execution layer.
 *
 * IMPORTANT:
 * - Consumes an already validated immutable LegacyImportPlan.
 * - Does NOT depend on FamilyRepository.
 * - Does NOT know about sync or Supabase.
 * - Candidate write is represented as ONE sink operation so the future
 *   repository adapter can define the real candidate-level atomicity.
 */
internal data class LegacyImportPersonWrite(
    val fullName: String,
    val birthDateSolar: LocalDate?,
    val note: String?
)

internal data class LegacyImportContactFieldWrite(
    val type: PersonContactFieldType,
    val label: String,
    val value: String,
    val isPrimary: Boolean,
    val sortOrder: Int
)

internal data class LegacyImportCandidateWrite(
    val rowIndex: Int,
    val person: LegacyImportPersonWrite,
    val fields: List<LegacyImportContactFieldWrite>
)

internal enum class LegacyImportExecutionItemStatus {
    SUCCESS,
    FAILED
}

internal data class LegacyImportExecutionItemResult(
    val rowIndex: Int,
    val status: LegacyImportExecutionItemStatus,
    val fieldsWritten: Int = 0
)

internal data class LegacyImportExecutionResult(
    val plannedCandidates: Int,
    val attemptedCandidates: Int,
    val successfulCandidates: Int,
    val failedCandidates: Int,
    val fieldsWritten: Int,
    val items: List<LegacyImportExecutionItemResult>
) {
    val completedSuccessfully: Boolean
        get() = failedCandidates == 0
}

/**
 * Candidate-level write boundary.
 *
 * The real repository integration will be implemented later.
 *
 * Contract:
 * - one call represents one candidate
 * - if the sink returns failure, executor reports that candidate failed
 * - executor never retries a candidate automatically
 */
internal fun interface FamilyLegacyImportWriteSink {
    fun writeCandidate(candidate: LegacyImportCandidateWrite): Result<Unit>
}

internal object FamilyLegacyImportExecutor {

    fun prepareWrite(
        entry: LegacyImportPlanEntry
    ): LegacyImportCandidateWrite {
        val candidate = entry.candidate

        val fields = entry.acceptedFields.mapIndexedNotNull { index, field ->
            val type = field.type ?: return@mapIndexedNotNull null
            val value = field.value?.takeIf { it.isNotBlank() }
                ?: return@mapIndexedNotNull null

            LegacyImportContactFieldWrite(
                type = type,
                label = field.label,
                value = value,
                isPrimary = field.isPrimary,
                sortOrder = index
            )
        }

        return LegacyImportCandidateWrite(
            rowIndex = entry.rowIndex,
            person = LegacyImportPersonWrite(
                fullName = entry.fullName,
                birthDateSolar = candidate.birthDateSolar,
                note = candidate.note
                    ?.takeIf { it.isNotBlank() }
            ),
            fields = fields
        )
    }

    fun execute(
        plan: LegacyImportPlan,
        sink: FamilyLegacyImportWriteSink
    ): LegacyImportExecutionResult {
        if (plan.entries.isEmpty()) {
            return LegacyImportExecutionResult(
                plannedCandidates = 0,
                attemptedCandidates = 0,
                successfulCandidates = 0,
                failedCandidates = 0,
                fieldsWritten = 0,
                items = emptyList()
            )
        }

        val results = mutableListOf<LegacyImportExecutionItemResult>()
        var successfulCandidates = 0
        var failedCandidates = 0
        var fieldsWritten = 0

        plan.entries.forEach { entry ->
            val write = prepareWrite(entry)

            val result = runCatching {
                sink.writeCandidate(write)
            }.fold(
                onSuccess = { it },
                onFailure = { Result.failure(it) }
            )

            if (result.isSuccess) {
                successfulCandidates += 1
                fieldsWritten += write.fields.size

                results += LegacyImportExecutionItemResult(
                    rowIndex = entry.rowIndex,
                    status = LegacyImportExecutionItemStatus.SUCCESS,
                    fieldsWritten = write.fields.size
                )
            } else {
                failedCandidates += 1

                results += LegacyImportExecutionItemResult(
                    rowIndex = entry.rowIndex,
                    status = LegacyImportExecutionItemStatus.FAILED,
                    fieldsWritten = 0
                )
            }
        }

        return LegacyImportExecutionResult(
            plannedCandidates = plan.entries.size,
            attemptedCandidates = results.size,
            successfulCandidates = successfulCandidates,
            failedCandidates = failedCandidates,
            fieldsWritten = fieldsWritten,
            items = results.toList()
        )
    }
}
