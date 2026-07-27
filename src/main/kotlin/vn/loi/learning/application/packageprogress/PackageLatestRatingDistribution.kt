package vn.loi.learning.application.packageprogress

data class PackageLatestRatingDistribution(
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int
) {
    val ratedItemCount: Int
        get() = againCount + hardCount + goodCount + easyCount

    companion object {
        val EMPTY = PackageLatestRatingDistribution(0, 0, 0, 0)
    }
}
