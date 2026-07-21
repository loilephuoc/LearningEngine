package vn.loi.learning.desktop.ui.reviewhistory
import kotlin.test.*
class ReviewHistorySearchPresentationTest{@Test fun emptyQuery(){val s=ReviewHistoryUiState(items=listOf(ReviewHistoryItemUi("d","Good","1s","1d","1")),query="x");assertTrue(reviewHistoryEmptySearchMessage(s).contains("x"));assertEquals(0,reviewHistorySearchSummary(s).visibleCount)}}
