package vn.loi.learning.desktop.ui.reviewhistory
import kotlin.test.*
class ReviewHistoryFilterTest{@Test fun matches(){val i=ReviewHistoryItemUi("","Good","","","");assertTrue(ReviewHistoryFilter.ALL.matches(i));assertTrue(ReviewHistoryFilter.GOOD.matches(i));assertFalse(ReviewHistoryFilter.EASY.matches(i))}}
