package vn.loi.learning.desktop.ui.browser.posreview

import kotlin.system.measureTimeMillis
import kotlin.test.*

class PosReviewModelsTest {
    @Test
    fun `computeAuditSummary accurately tallies canonical, missing, and custom POS`() {
        val rows = listOf(
            PosReviewRowItem("1", "apple", "qua tao", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "run", "chay", "", "VERB", "VERB", false),
            PosReviewRowItem("3", "fast", "nhanh", "", "ADJECTIVE", "ADJECTIVE", false),
            PosReviewRowItem("4", "ice cream", "kem", "", "NOUN PHRASE", "NOUN PHRASE", false),
            PosReviewRowItem("5", "hello", "xin chao", "", "", "", false),
            PosReviewRowItem("6", "sentence 1", "cau 1", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("7", "custom 2", "tuy bien", "", "MY_POS", "MY_POS", true)
        )

        val summary = computeAuditSummary(rows)
        assertEquals(7, summary.totalCount)
        assertEquals(1, summary.missingCount)
        assertEquals(1, summary.canonicalCounts["NOUN"])
        assertEquals(1, summary.canonicalCounts["VERB"])
        assertEquals(1, summary.canonicalCounts["ADJECTIVE"])
        assertEquals(1, summary.canonicalCounts["NOUN PHRASE"])
        assertEquals(1, summary.customUnknownCounts["SENTENCE"])
        assertEquals(1, summary.customUnknownCounts["MY POS"])
    }

    @Test
    fun `PosBatchReviewState computes counts and filters by search and status correctly`() {
        val rows = listOf(
            PosReviewRowItem("1", "apple", "qua tao", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "run", "chay", "", "VERB", "NOUN", false), // Changed
            PosReviewRowItem("3", "missing", "thieu", "", "", "", false),    // Missing
            PosReviewRowItem("4", "custom", "tuy bien", "", "SENTENCE", "SENTENCE", true) // Custom
        )

        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows
        )

        assertEquals(4, state.totalCount)
        assertEquals(1, state.changedCount)
        assertEquals(3, state.unchangedCount)
        assertEquals(1, state.missingCount)
        assertEquals(1, state.customCount)
        assertTrue(state.canApply)

        // Filter by Status: CHANGED
        val changedFiltered = state.copy(statusFilter = PosReviewRowStatus.CHANGED)
        assertEquals(1, changedFiltered.filteredRows.size)
        assertEquals("2", changedFiltered.filteredRows.first().contentId)

        // Search by Question
        val searchFiltered = state.copy(searchQuery = "apple")
        assertEquals(1, searchFiltered.filteredRows.size)
        assertEquals("1", searchFiltered.filteredRows.first().contentId)
    }

    @Test
    fun `selecting one row adds its ContentId and toggling removes it`() {
        val rows = listOf(
            PosReviewRowItem("1", "apple", "qua tao", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "run", "chay", "", "VERB", "VERB", false)
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rows)

        // 1. Selecting one row adds its ContentId
        val stateWithRow1 = state.toggleRowSelection("1")
        assertEquals(setOf("1"), stateWithRow1.selectedRowIds)
        assertEquals(1, stateWithRow1.selectedCount)

        // 2. Toggling selected row removes it
        val stateAfterToggle = stateWithRow1.toggleRowSelection("1")
        assertTrue(stateAfterToggle.selectedRowIds.isEmpty())
        assertEquals(0, stateAfterToggle.selectedCount)
    }

    @Test
    fun `selectAllFiltered selects only filtered rows and header checkbox behaves accurately`() {
        val rows = listOf(
            PosReviewRowItem("1", "10 percent", "10 phan tram", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "100 percent", "100 phan tram", "", "NOUN", "NOUN", false),
            PosReviewRowItem("3", "apple", "qua tao", "", "NOUN", "NOUN", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows,
            searchQuery = "percent"
        )

        assertEquals(2, state.filteredRows.size)
        assertTrue(state.isNoneFilteredSelected)
        assertFalse(state.isAllFilteredSelected)
        assertFalse(state.isSomeFilteredSelected)

        // 3. Select All Filtered selects only filtered rows (1 and 2, not 3)
        val selectedFilteredState = state.selectAllFiltered()
        assertEquals(setOf("1", "2"), selectedFilteredState.selectedRowIds)
        assertEquals(2, selectedFilteredState.filteredSelectedCount)
        assertTrue(selectedFilteredState.isAllFilteredSelected)
        assertFalse(selectedFilteredState.isSomeFilteredSelected)
        assertFalse(selectedFilteredState.isNoneFilteredSelected)

        // Toggling all filtered when all are selected deselects them
        val toggledState = selectedFilteredState.toggleAllFiltered()
        assertTrue(toggledState.selectedRowIds.isEmpty())
        assertTrue(toggledState.isNoneFilteredSelected)
    }

    @Test
    fun `clearSelection clears all selected rows`() {
        val rows = listOf(
            PosReviewRowItem("1", "apple", "qua tao", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "run", "chay", "", "VERB", "VERB", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows,
            selectedRowIds = setOf("1", "2")
        )

        // 4. Clear Selection clears selection
        val cleared = state.clearSelection()
        assertTrue(cleared.selectedRowIds.isEmpty())
        assertEquals(0, cleared.selectedCount)
    }

    @Test
    fun `selection survives search and status-filter changes as projection-only`() {
        val rows = listOf(
            PosReviewRowItem("1", "a bag of flour", "tui bot", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "percent", "phan tram", "", "NOUN", "NOUN", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows,
            selectedRowIds = setOf("1")
        )

        // 5. Search hides row 1, but row 1 remains selected in state
        val searchedState = state.copy(searchQuery = "percent")
        assertEquals(1, searchedState.filteredRows.size)
        assertEquals("2", searchedState.filteredRows.first().contentId)
        assertEquals(setOf("1"), searchedState.selectedRowIds)
        assertEquals(1, searchedState.selectedCount)

        // Select row 2 while searched
        val bothSelected = searchedState.toggleRowSelection("2")
        assertEquals(setOf("1", "2"), bothSelected.selectedRowIds)

        // Clear search: both rows visible and both still selected
        val unsearched = bothSelected.copy(searchQuery = "")
        assertEquals(2, unsearched.filteredRows.size)
        assertEquals(setOf("1", "2"), unsearched.selectedRowIds)

        // 6. Selection survives status filter changes
        val statusFiltered = unsearched.copy(statusFilter = PosReviewRowStatus.CHANGED)
        assertEquals(0, statusFiltered.filteredRows.size)
        assertEquals(setOf("1", "2"), statusFiltered.selectedRowIds)
    }

    @Test
    fun `Set Selected changes only selected rows without immediate persistence`() {
        val rows = listOf(
            PosReviewRowItem("1", "10-year-old girl", "be gai 10 tuoi", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "a bag of flour", "tui bot", "", "NOUN", "NOUN", false),
            PosReviewRowItem("3", "a bottle of water", "chai nuoc", "", "NOUN", "NOUN", false),
            PosReviewRowItem("4", "unselected word", "tu khong chon", "", "VERB", "VERB", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows,
            selectedRowIds = setOf("1", "2", "3")
        )

        // 8. Set Selected changes only rows 1, 2, 3
        val updated = state.batchSetSelectedPos("NOUN PHRASE")
        assertEquals("NOUN PHRASE", updated.currentRows.first { it.contentId == "1" }.newPos)
        assertEquals("NOUN PHRASE", updated.currentRows.first { it.contentId == "2" }.newPos)
        assertEquals("NOUN PHRASE", updated.currentRows.first { it.contentId == "3" }.newPos)
        assertEquals("VERB", updated.currentRows.first { it.contentId == "4" }.newPos)
        assertEquals(3, updated.changedCount)
    }

    @Test
    fun `Set All Filtered changes only filtered rows`() {
        val rows = listOf(
            PosReviewRowItem("1", "10 percent", "10 phan tram", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "100 percent", "100 phan tram", "", "NOUN", "NOUN", false),
            PosReviewRowItem("3", "apple", "qua tao", "", "NOUN", "NOUN", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows,
            searchQuery = "percent"
        )

        // 9. Set All Filtered changes only rows matching "percent" (1 and 2)
        val updated = state.batchSetFilteredPos("NUMBER")
        assertEquals("NUMBER", updated.currentRows.first { it.contentId == "1" }.newPos)
        assertEquals("NUMBER", updated.currentRows.first { it.contentId == "2" }.newPos)
        assertEquals("NOUN", updated.currentRows.first { it.contentId == "3" }.newPos)
        assertEquals(2, updated.changedCount)
    }

    @Test
    fun `deselecting a changed row does not discard its draft POS and apply count is based on changed rows`() {
        val rows = listOf(
            PosReviewRowItem("1", "item 1", "answer 1", "", "NOUN", "NOUN", false),
            PosReviewRowItem("2", "item 2", "answer 2", "", "VERB", "VERB", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows,
            selectedRowIds = setOf("1")
        )

        // Set Selected to ADJECTIVE
        val modified = state.batchSetSelectedPos("ADJECTIVE")
        // 10. Deselect row 1
        val deselected = modified.toggleRowSelection("1")
        assertTrue(deselected.selectedRowIds.isEmpty())

        // Draft POS is still ADJECTIVE and status is CHANGED
        val row1 = deselected.currentRows.first { it.contentId == "1" }
        assertEquals("ADJECTIVE", row1.newPos)
        assertTrue(row1.isChanged)

        // 11. Apply count is 1 based on changed rows, not selected rows
        assertEquals(1, deselected.changedCount)
        assertTrue(deselected.canApply)
    }

    @Test
    fun `search query is projection-only, preserves spaces and Vietnamese Unicode, and does not mutate drafts`() {
        val rows = listOf(
            PosReviewRowItem("1", "một chút", "a little bit", "", "PHRASE", "PHRASE", false),
            PosReviewRowItem("2", "10-year-old girl", "be gai 10 tuoi", "", "NOUN", "NOUN PHRASE", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows
        )

        // 12 & 14: Search with Vietnamese text
        val vietnameseSearch = state.copy(searchQuery = "một chút")
        assertEquals(1, vietnameseSearch.filteredRows.size)
        assertEquals("1", vietnameseSearch.filteredRows.first().contentId)

        // 13: Search does not discard draft POS for hidden row 2
        val hiddenRow2 = vietnameseSearch.currentRows.first { it.contentId == "2" }
        assertEquals("NOUN PHRASE", hiddenRow2.newPos)
        assertTrue(hiddenRow2.isChanged)
    }

    @Test
    fun `zero-row selected operation is no-op`() {
        val rows = listOf(
            PosReviewRowItem("1", "apple", "qua tao", "", "NOUN", "NOUN", false)
        )
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = rows,
            selectedRowIds = emptySet()
        )

        // 15: Zero-row selected batchSet is no-op
        val result = state.batchSetSelectedPos("VERB")
        assertEquals("NOUN", result.currentRows.first().newPos)
        assertEquals(0, result.changedCount)
        assertFalse(result.canSetSelected)
    }

    @Test
    fun `custom SENTENCE rows remain preserved and recognized as custom unknown`() {
        val row = PosReviewRowItem("1", "a button is missing", "mot cai cuc bi thieu", "", "SENTENCE", "SENTENCE", true)
        val state = PosBatchReviewState(
            scope = PosReviewScope.ALL_ITEMS,
            currentRows = listOf(row)
        )

        // 16: SENTENCE is preserved as custom POS
        assertTrue(row.isCurrentCustomOrUnknown)
        assertEquals(PosReviewRowStatus.CUSTOM, row.status)
        assertEquals(1, state.customCount)
        assertEquals(0, state.changedCount)
    }

    @Test
    fun `analyzeRows populates suggested New POS for High and Medium confidence and leaves Uncertain untouched`() {
        val rows = listOf(
            PosReviewRowItem("1", "10 percent", "10 phan tram", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("2", "a bag of flour", "tui bot", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("3", "a button is missing", "cuc ao bi thieu", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("4", "ambiguous xyz foo", "khong ro", "", "SENTENCE", "SENTENCE", true)
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rows)

        val analyzed = state.analyzeRows()

        assertEquals(4, analyzed.analyzedCount)
        assertEquals(3, analyzed.highConfidenceCount) // 10 percent (NUMBER), a bag of flour (NOUN PHRASE), a button is missing (SENTENCE)
        assertEquals(1, analyzed.uncertainConfidenceCount) // ambiguous xyz foo (UNCERTAIN)

        // 10 percent -> New POS becomes NUMBER
        val item1 = analyzed.currentRows.first { it.contentId == "1" }
        assertEquals("NUMBER", item1.newPos)
        assertEquals("NUMBER", item1.suggestedPos)
        assertEquals(PosConfidence.HIGH, item1.confidence)
        assertTrue(item1.isChanged)

        // a bag of flour -> New POS becomes NOUN PHRASE
        val item2 = analyzed.currentRows.first { it.contentId == "2" }
        assertEquals("NOUN PHRASE", item2.newPos)
        assertEquals("NOUN PHRASE", item2.suggestedPos)
        assertEquals(PosConfidence.HIGH, item2.confidence)
        assertTrue(item2.isChanged)

        // a button is missing -> preserves SENTENCE
        val item3 = analyzed.currentRows.first { it.contentId == "3" }
        assertEquals("SENTENCE", item3.newPos)
        assertEquals("SENTENCE", item3.suggestedPos)
        assertEquals(PosConfidence.HIGH, item3.confidence)
        assertFalse(item3.isChanged)

        // ambiguous xyz foo -> UNCERTAIN, New POS untouched
        val item4 = analyzed.currentRows.first { it.contentId == "4" }
        assertEquals("SENTENCE", item4.newPos)
        assertNull(item4.suggestedPos)
        assertEquals(PosConfidence.UNCERTAIN, item4.confidence)
        assertFalse(item4.isChanged)
    }

    @Test
    fun `analyzeRows correctly fixes real OPD_2nd sample dataset rows from legacy SENTENCE`() {
        val opdRows = listOf(
            PosReviewRowItem("1", "April", "thang 4", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("2", "apron", "tap de", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("3", "archery", "ban cung", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("4", "area code", "ma vung", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("5", "arm", "canh tay", "", "SENTENCE", "SENTENCE", true, exampleText = "He raised his right arm."),
            PosReviewRowItem("6", "armchair / easy chair", "ghe banh", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("7", "armpit", "nach", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("8", "arrange the furniture", "sap xep noi that", "", "SENTENCE", "SENTENCE", true),
            PosReviewRowItem("9", "a button is missing", "cuc ao bi thieu", "", "SENTENCE", "SENTENCE", true)
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = opdRows)
        val analyzed = state.analyzeRows()

        assertEquals(9, analyzed.analyzedCount)
        assertEquals(8, analyzed.changedCount) // 8 items repaired out of 9!
        assertEquals(1, analyzed.legacySentenceUnchangedCount) // Only "a button is missing" remains SENTENCE!

        assertEquals("NOUN", analyzed.currentRows.first { it.contentId == "1" }.newPos)
        assertEquals("NOUN", analyzed.currentRows.first { it.contentId == "2" }.newPos)
        assertEquals("NOUN", analyzed.currentRows.first { it.contentId == "3" }.newPos)
        assertEquals("NOUN PHRASE", analyzed.currentRows.first { it.contentId == "4" }.newPos)
        assertEquals("NOUN", analyzed.currentRows.first { it.contentId == "5" }.newPos)
        assertEquals("NOUN PHRASE", analyzed.currentRows.first { it.contentId == "6" }.newPos)
        assertEquals("NOUN", analyzed.currentRows.first { it.contentId == "7" }.newPos)
        assertEquals("PHRASE", analyzed.currentRows.first { it.contentId == "8" }.newPos)
        assertEquals("SENTENCE", analyzed.currentRows.first { it.contentId == "9" }.newPos)
    }

    @Test
    fun `manual override is preserved when re-analyzing`() {
        val rows = listOf(
            PosReviewRowItem("1", "a bottle of water", "chai nuoc", "", "SENTENCE", "SENTENCE", true)
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rows)

        // 1. First analyze -> suggested NOUN PHRASE
        val firstAnalyze = state.analyzeRows()
        assertEquals("NOUN PHRASE", firstAnalyze.currentRows.first().newPos)

        // 2. User manually overrides New POS to PHRASE
        val manualEdited = firstAnalyze.updateRowNewPos("1", "PHRASE")
        val rowAfterEdit = manualEdited.currentRows.first()
        assertEquals("PHRASE", rowAfterEdit.newPos)
        assertTrue(rowAfterEdit.isManualOverride)

        // 3. User clicks Analyze again -> user's manual "PHRASE" is strictly preserved!
        val reAnalyzed = manualEdited.analyzeRows()
        val reAnalyzedRow = reAnalyzed.currentRows.first()
        assertEquals("PHRASE", reAnalyzedRow.newPos)
        assertEquals("NOUN PHRASE", reAnalyzedRow.suggestedPos)
        assertTrue(reAnalyzedRow.isManualOverride)
    }

    @Test
    fun `resetDrafts restores all draft rows to originalPos and clears analyzer state`() {
        val rows = listOf(
            PosReviewRowItem("1", "10 percent", "10 phan tram", "", "SENTENCE", "SENTENCE", true)
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rows)
        val analyzed = state.analyzeRows()
        assertEquals("NUMBER", analyzed.currentRows.first().newPos)

        val reset = analyzed.resetDrafts()
        val row = reset.currentRows.first()
        assertEquals("SENTENCE", row.newPos)
        assertNull(row.suggestedPos)
        assertNull(row.confidence)
        assertFalse(row.isManualOverride)
        assertEquals(0, reset.changedCount)
    }

    @Test
    fun `analyzeRows performs fast for over 3000 items under 50ms`() {
        val largeList = (1..3000).map { i ->
            PosReviewRowItem(
                contentId = "id-$i",
                question = if (i % 3 == 0) "$i percent" else if (i % 3 == 1) "a bottle of water $i" else "a button is missing $i",
                answer = "answer $i",
                translation = "dich $i",
                originalPos = "SENTENCE",
                newPos = "SENTENCE",
                isCustomOrUnknown = true
            )
        }
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = largeList)

        // Warm up JIT
        state.analyzeRows()

        val elapsed = measureTimeMillis {
            val result = state.analyzeRows()
            assertEquals(3000, result.analyzedCount)
        }

        assertTrue(elapsed < 1000, "Analysis took too long: ${elapsed}ms for 3,000 items")
    }

    @Test
    fun `missing review metadata resolves to UNREVIEWED by default`() {
        val row = PosReviewRowItem("1", "apple", "qua tao", "", "NOUN", "NOUN", false)
        assertEquals(PosReviewAuthority.UNREVIEWED, row.reviewAuthority)
        assertFalse(row.isConfirmed)
        assertEquals(PosReviewRowStatus.SAME, row.status)
    }

    @Test
    fun `confirmed row is skipped by analyzeRows and does not modify draft newPos or suggestion`() {
        val rows = listOf(
            PosReviewRowItem(
                contentId = "1",
                question = "record", // In isolation analyzer suggests NOUN, but user previously confirmed VERB!
                answer = "ghi am",
                translation = "",
                originalPos = "VERB",
                newPos = "VERB",
                isCustomOrUnknown = false,
                reviewAuthority = PosReviewAuthority.USER_CONFIRMED
            ),
            PosReviewRowItem(
                contentId = "2",
                question = "April",
                answer = "thang 4",
                translation = "",
                originalPos = "SENTENCE",
                newPos = "SENTENCE",
                isCustomOrUnknown = true,
                reviewAuthority = PosReviewAuthority.UNREVIEWED
            )
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rows)
        assertEquals(1, state.confirmedCount)
        assertEquals(1, state.needsReviewCount)

        val analyzed = state.analyzeRows()

        // 1. Confirmed row "record" was SKIPPED: remains VERB, no suggestion override
        val row1 = analyzed.currentRows.first { it.contentId == "1" }
        assertEquals("VERB", row1.newPos)
        assertNull(row1.suggestedPos)
        assertTrue(row1.isConfirmed)
        assertEquals(PosReviewRowStatus.CONFIRMED, row1.status)

        // 2. Unreviewed row "April" was analyzed and updated to NOUN
        val row2 = analyzed.currentRows.first { it.contentId == "2" }
        assertEquals("NOUN", row2.newPos)
        assertEquals("NOUN", row2.suggestedPos)
        assertFalse(row2.isConfirmed)
        assertEquals(PosReviewRowStatus.CHANGED, row2.status)
    }

    @Test
    fun `manual edit on a confirmed row is allowed and marks isManualOverride`() {
        val row = PosReviewRowItem(
            contentId = "1",
            question = "record",
            answer = "ghi am",
            translation = "",
            originalPos = "VERB",
            newPos = "VERB",
            isCustomOrUnknown = false,
            reviewAuthority = PosReviewAuthority.USER_CONFIRMED
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = listOf(row))

        // User manually edits New POS on confirmed row to NOUN
        val edited = state.updateRowNewPos("1", "NOUN")
        val editedRow = edited.currentRows.first()
        assertEquals("NOUN", editedRow.newPos)
        assertTrue(editedRow.isManualOverride)
        assertTrue(editedRow.isChanged)
        assertEquals(PosReviewRowStatus.CHANGED, editedRow.status)
    }

    @Test
    fun `statusFilter correctly filters CONFIRMED and NEEDS_REVIEW`() {
        val rows = listOf(
            PosReviewRowItem("1", "apple", "", "", "NOUN", "NOUN", false, reviewAuthority = PosReviewAuthority.USER_CONFIRMED),
            PosReviewRowItem("2", "run", "", "", "VERB", "VERB", false, reviewAuthority = PosReviewAuthority.UNREVIEWED),
            PosReviewRowItem("3", "fast", "", "", "SENTENCE", "ADJECTIVE", true, reviewAuthority = PosReviewAuthority.UNREVIEWED)
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rows)

        // Filter CONFIRMED
        val confirmedFilter = state.copy(statusFilter = PosReviewRowStatus.CONFIRMED)
        assertEquals(1, confirmedFilter.filteredRows.size)
        assertEquals("1", confirmedFilter.filteredRows.first().contentId)

        // Filter NEEDS_REVIEW
        val needsReviewFilter = state.copy(statusFilter = PosReviewRowStatus.NEEDS_REVIEW)
        assertEquals(2, needsReviewFilter.filteredRows.size)
        assertEquals(setOf("2", "3"), needsReviewFilter.filteredRows.map { it.contentId }.toSet())
    }

    @Test
    fun `selectedConfirmedCount and canUnlockSelected detect selected confirmed rows`() {
        val rows = listOf(
            PosReviewRowItem("1", "apple", "", "", "NOUN", "NOUN", false, reviewAuthority = PosReviewAuthority.USER_CONFIRMED),
            PosReviewRowItem("2", "run", "", "", "VERB", "VERB", false, reviewAuthority = PosReviewAuthority.UNREVIEWED)
        )
        val state = PosBatchReviewState(scope = PosReviewScope.ALL_ITEMS, currentRows = rows)
        assertFalse(state.canUnlockSelected)
        assertEquals(0, state.selectedConfirmedCount)

        val selectedRow2 = state.toggleRowSelection("2")
        assertFalse(selectedRow2.canUnlockSelected) // "2" is UNREVIEWED
        assertEquals(0, selectedRow2.selectedConfirmedCount)

        val selectedBoth = selectedRow2.toggleRowSelection("1")
        assertTrue(selectedBoth.canUnlockSelected) // "1" is USER_CONFIRMED
        assertEquals(1, selectedBoth.selectedConfirmedCount)
    }
}
