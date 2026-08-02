package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.session.RatingInventory

internal data class RatingInventoryItemPresentation(val label: String, val count: Int)

internal data class RatingInventoryPresentation(
    val items: List<RatingInventoryItemPresentation>,
    val total: Int
)

internal object RatingInventoryPresentationResolver {
    fun resolve(inventory: RatingInventory) = RatingInventoryPresentation(
        items = listOf(
            RatingInventoryItemPresentation("Again", inventory.againCount),
            RatingInventoryItemPresentation("Hard", inventory.hardCount),
            RatingInventoryItemPresentation("Good", inventory.goodCount),
            RatingInventoryItemPresentation("Easy", inventory.easyCount),
            RatingInventoryItemPresentation("Chưa đánh giá", inventory.neverReviewedCount)
        ),
        total = inventory.totalEligibleContentCount
    )
}
