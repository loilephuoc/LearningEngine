package vn.loi.learning.desktop.ui.browser.posreview

import vn.loi.learning.application.partofspeech.PartOfSpeechNormalizer

enum class PosReviewScope(val label: String) {
    ALL_ITEMS("All Items"),
    SELECTED_ITEMS("Selected Items"),
    CURRENT_SEARCH_RESULTS("Current Search Results"),
    CURRENT_FILTER_RESULTS("Current Filter Results")
}

enum class PosReviewAuthority {
    UNREVIEWED,
    USER_CONFIRMED
}

enum class PosReviewRowStatus(val label: String) {
    ALL("All"),
    NEEDS_REVIEW("Needs Review"),
    CONFIRMED("Confirmed"),
    CHANGED("Suggested Changes"),
    SAME("Same"),
    HIGH_CONFIDENCE("High Confidence"),
    REVIEW("Review"),
    UNCERTAIN("Uncertain / No Suggestion"),
    MISSING("Missing"),
    CUSTOM("Custom / Unknown")
}

data class PosReviewRowItem(
    val contentId: String,
    val question: String,
    val answer: String,
    val translation: String,
    val originalPos: String,
    val newPos: String,
    val isCustomOrUnknown: Boolean,
    val suggestedPos: String? = null,
    val confidence: PosConfidence? = null,
    val suggestionReason: PosSuggestionReason? = null,
    val isManualOverride: Boolean = false,
    val exampleText: String? = null,
    val pronunciation: String = "",
    val reviewAuthority: PosReviewAuthority = PosReviewAuthority.UNREVIEWED
) {
    val isChanged: Boolean get() = originalPos.trim() != newPos.trim()
    val isMissing: Boolean get() = newPos.isBlank()
    val isConfirmed: Boolean get() = reviewAuthority == PosReviewAuthority.USER_CONFIRMED
    val isCurrentCustomOrUnknown: Boolean
        get() {
            if (newPos.isBlank()) return false
            val canonical = PartOfSpeechNormalizer.canonicalize(newPos)
            return canonical == null || !canonical.known
        }
    val status: PosReviewRowStatus get() = when {
        isChanged -> PosReviewRowStatus.CHANGED
        isConfirmed -> PosReviewRowStatus.CONFIRMED
        confidence == PosConfidence.UNCERTAIN -> PosReviewRowStatus.UNCERTAIN
        isMissing -> PosReviewRowStatus.MISSING
        isCurrentCustomOrUnknown -> PosReviewRowStatus.CUSTOM
        else -> PosReviewRowStatus.SAME
    }
}

data class PosAuditSummary(
    val canonicalCounts: Map<String, Int>,
    val missingCount: Int,
    val customUnknownCounts: Map<String, Int>,
    val totalCount: Int
)

data class PosBatchReviewState(
    val scope: PosReviewScope,
    val currentRows: List<PosReviewRowItem>,
    val selectedRowIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val statusFilter: PosReviewRowStatus = PosReviewRowStatus.ALL,
    val showConfirmApply: Boolean = false,
    val showConfirmUnlock: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val auditSummary: PosAuditSummary by lazy {
        computeAuditSummary(currentRows)
    }

    val filteredRows: List<PosReviewRowItem> by lazy {
        currentRows.filter { row ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                row.question.contains(searchQuery, ignoreCase = true) ||
                    row.answer.contains(searchQuery, ignoreCase = true) ||
                    row.translation.contains(searchQuery, ignoreCase = true) ||
                    row.originalPos.contains(searchQuery, ignoreCase = true) ||
                    row.newPos.contains(searchQuery, ignoreCase = true) ||
                    (row.exampleText?.contains(searchQuery, ignoreCase = true) == true)
            }
            val matchesStatus = when (statusFilter) {
                PosReviewRowStatus.ALL -> true
                PosReviewRowStatus.NEEDS_REVIEW -> !row.isConfirmed
                PosReviewRowStatus.CONFIRMED -> row.isConfirmed
                PosReviewRowStatus.CHANGED -> row.isChanged
                PosReviewRowStatus.SAME -> !row.isChanged && !row.isMissing && !row.isCurrentCustomOrUnknown && row.confidence != PosConfidence.UNCERTAIN
                PosReviewRowStatus.HIGH_CONFIDENCE -> row.confidence == PosConfidence.HIGH
                PosReviewRowStatus.REVIEW -> row.confidence == PosConfidence.MEDIUM
                PosReviewRowStatus.UNCERTAIN -> row.confidence == PosConfidence.UNCERTAIN || (row.confidence == null && row.suggestedPos == null && row.isCurrentCustomOrUnknown)
                PosReviewRowStatus.MISSING -> row.isMissing
                PosReviewRowStatus.CUSTOM -> row.isCurrentCustomOrUnknown
            }
            matchesSearch && matchesStatus
        }
    }

    val changedCount: Int get() = currentRows.count { it.isChanged }
    val unchangedCount: Int get() = currentRows.count { !it.isChanged }
    val missingCount: Int get() = currentRows.count { it.isMissing }
    val customCount: Int get() = currentRows.count { it.isCurrentCustomOrUnknown }
    val totalCount: Int get() = currentRows.size

    val confirmedCount: Int get() = currentRows.count { it.isConfirmed }
    val needsReviewCount: Int get() = currentRows.count { !it.isConfirmed }

    val analyzedCount: Int get() = currentRows.count { it.confidence != null }
    val highConfidenceCount: Int get() = currentRows.count { it.confidence == PosConfidence.HIGH }
    val reviewConfidenceCount: Int get() = currentRows.count { it.confidence == PosConfidence.MEDIUM }
    val uncertainConfidenceCount: Int get() = currentRows.count { it.confidence == PosConfidence.UNCERTAIN }

    val legacySentenceCount: Int get() = currentRows.count { it.originalPos.trim().equals("SENTENCE", ignoreCase = true) }
    val legacySentenceUnchangedCount: Int get() = currentRows.count { it.originalPos.trim().equals("SENTENCE", ignoreCase = true) && !it.isChanged }
    val legacySentenceChangedCount: Int get() = currentRows.count { it.originalPos.trim().equals("SENTENCE", ignoreCase = true) && it.isChanged }

    val selectedCount: Int get() = selectedRowIds.size
    val filteredSelectedCount: Int get() = filteredRows.count { it.contentId in selectedRowIds }
    val isAllFilteredSelected: Boolean get() = filteredRows.isNotEmpty() && filteredSelectedCount == filteredRows.size
    val isSomeFilteredSelected: Boolean get() = filteredSelectedCount > 0 && filteredSelectedCount < filteredRows.size
    val isNoneFilteredSelected: Boolean get() = filteredSelectedCount == 0

    val selectedConfirmedCount: Int get() = selectedRowIds.count { id -> currentRows.any { it.contentId == id && it.isConfirmed } }
    val canSetSelected: Boolean get() = selectedRowIds.isNotEmpty() && !isSubmitting
    val canSetFiltered: Boolean get() = filteredRows.isNotEmpty() && !isSubmitting
    val canUnlockSelected: Boolean get() = selectedConfirmedCount > 0 && !isSubmitting
    val canApply: Boolean get() = changedCount > 0 && !isSubmitting

    fun toggleRowSelection(contentId: String): PosBatchReviewState {
        val next = if (contentId in selectedRowIds) selectedRowIds - contentId else selectedRowIds + contentId
        return copy(selectedRowIds = next)
    }

    fun selectAllFiltered(): PosBatchReviewState {
        val filteredIds = filteredRows.mapTo(hashSetOf()) { it.contentId }
        return copy(selectedRowIds = selectedRowIds + filteredIds)
    }

    fun deselectAllFiltered(): PosBatchReviewState {
        val filteredIds = filteredRows.mapTo(hashSetOf()) { it.contentId }
        return copy(selectedRowIds = selectedRowIds - filteredIds)
    }

    fun toggleAllFiltered(): PosBatchReviewState {
        return if (isAllFilteredSelected) deselectAllFiltered() else selectAllFiltered()
    }

    fun clearSelection(): PosBatchReviewState {
        return copy(selectedRowIds = emptySet())
    }

    fun updateRowNewPos(contentId: String, newPos: String): PosBatchReviewState {
        val updatedRows = currentRows.map { row ->
            if (row.contentId == contentId) {
                row.copy(newPos = newPos, isManualOverride = true)
            } else row
        }
        return copy(currentRows = updatedRows)
    }

    fun batchSetSelectedPos(newPos: String): PosBatchReviewState {
        if (selectedRowIds.isEmpty()) return this
        val updatedRows = currentRows.map { row ->
            if (row.contentId in selectedRowIds) {
                row.copy(newPos = newPos, isManualOverride = true)
            } else row
        }
        return copy(currentRows = updatedRows)
    }

    fun batchSetFilteredPos(newPos: String): PosBatchReviewState {
        if (filteredRows.isEmpty()) return this
        val filteredIds = filteredRows.mapTo(hashSetOf()) { it.contentId }
        val updatedRows = currentRows.map { row ->
            if (row.contentId in filteredIds) {
                row.copy(newPos = newPos, isManualOverride = true)
            } else row
        }
        return copy(currentRows = updatedRows)
    }

    fun analyzeRows(): PosBatchReviewState {
        val updatedRows = currentRows.map { row ->
            // Confirmed rows are protected: do not analyze or modify newPos unless user manually edits
            if (row.isConfirmed && !row.isManualOverride) {
                return@map row
            }

            val input = PosAnalysisInput(
                contentId = row.contentId,
                question = row.question,
                answer = row.answer,
                translation = row.translation,
                exampleText = row.exampleText,
                pronunciation = row.pronunciation,
                currentPos = row.originalPos
            )
            val suggestion = PosAnalyzer.analyze(input)

            if (row.isManualOverride) {
                // User manual edits are strictly preserved
                row.copy(
                    suggestedPos = suggestion.suggestedPos,
                    confidence = suggestion.confidence,
                    suggestionReason = suggestion.reasonCode
                )
            } else {
                val nextNewPos = when (suggestion.confidence) {
                    PosConfidence.HIGH, PosConfidence.MEDIUM -> suggestion.suggestedPos ?: row.originalPos
                    PosConfidence.UNCERTAIN -> row.newPos
                }
                row.copy(
                    newPos = nextNewPos,
                    suggestedPos = suggestion.suggestedPos,
                    confidence = suggestion.confidence,
                    suggestionReason = suggestion.reasonCode
                )
            }
        }
        return copy(currentRows = updatedRows)
    }

    fun resetDrafts(): PosBatchReviewState {
        val resetRows = currentRows.map { row ->
            row.copy(
                newPos = row.originalPos,
                isManualOverride = false,
                suggestedPos = null,
                confidence = null,
                suggestionReason = null
            )
        }
        return copy(currentRows = resetRows, selectedRowIds = emptySet())
    }
}

fun computeAuditSummary(rows: List<PosReviewRowItem>): PosAuditSummary {
    val canonicalCounts = mutableMapOf<String, Int>()
    var missingCount = 0
    val customCounts = mutableMapOf<String, Int>()

    for (row in rows) {
        val pos = row.newPos.trim()
        if (pos.isBlank()) {
            missingCount++
            continue
        }
        val canonical = PartOfSpeechNormalizer.canonicalize(pos)
        if (canonical != null && canonical.known) {
            canonicalCounts[canonical.value] = (canonicalCounts[canonical.value] ?: 0) + 1
        } else {
            val key = canonical?.value ?: pos
            customCounts[key] = (customCounts[key] ?: 0) + 1
        }
    }
    return PosAuditSummary(
        canonicalCounts = canonicalCounts.toSortedMap(),
        missingCount = missingCount,
        customUnknownCounts = customCounts.toSortedMap(),
        totalCount = rows.size
    )
}
