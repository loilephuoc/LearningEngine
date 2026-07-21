package vn.loi.learning.desktop.ui.reviewhistory

enum class ReviewHistoryFilter(val label: String) { ALL("All"), AGAIN("Again"), HARD("Hard"), GOOD("Good"), EASY("Easy");
    fun matches(item: ReviewHistoryItemUi): Boolean = this == ALL || item.rating.equals(label, true)
}
