package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.session.RatingInventory

internal enum class RatingInventoryKind { AGAIN, HARD, GOOD, EASY, NEVER_REVIEWED, TOTAL }
internal enum class RatingInventoryColorRole { AGAIN, HARD, GOOD, EASY, NEUTRAL, EMPHASIS }

internal data class RatingInventoryItemPresentation(
    val kind: RatingInventoryKind,
    val label: String,
    val count: Int,
    val colorRole: RatingInventoryColorRole = kind.toColorRole()
)

internal fun RatingInventoryKind.toColorRole(): RatingInventoryColorRole = when (this) {
    RatingInventoryKind.AGAIN -> RatingInventoryColorRole.AGAIN
    RatingInventoryKind.HARD -> RatingInventoryColorRole.HARD
    RatingInventoryKind.GOOD -> RatingInventoryColorRole.GOOD
    RatingInventoryKind.EASY -> RatingInventoryColorRole.EASY
    RatingInventoryKind.NEVER_REVIEWED -> RatingInventoryColorRole.NEUTRAL
    RatingInventoryKind.TOTAL -> RatingInventoryColorRole.EMPHASIS
}

internal data class RatingInventoryPresentation(
    val items: List<RatingInventoryItemPresentation>,
    val total: Int
)

internal object RatingInventoryPresentationResolver {
    fun resolve(inventory: RatingInventory) = RatingInventoryPresentation(
        items = listOf(
            RatingInventoryItemPresentation(RatingInventoryKind.AGAIN, "Again", inventory.againCount),
            RatingInventoryItemPresentation(RatingInventoryKind.HARD, "Hard", inventory.hardCount),
            RatingInventoryItemPresentation(RatingInventoryKind.GOOD, "Good", inventory.goodCount),
            RatingInventoryItemPresentation(RatingInventoryKind.EASY, "Easy", inventory.easyCount),
            RatingInventoryItemPresentation(
                RatingInventoryKind.NEVER_REVIEWED,
                "Chưa đánh giá",
                inventory.neverReviewedCount
            ),
            RatingInventoryItemPresentation(
                RatingInventoryKind.TOTAL,
                "Tổng",
                inventory.totalEligibleContentCount
            )
        ),
        total = inventory.totalEligibleContentCount
    )
}
