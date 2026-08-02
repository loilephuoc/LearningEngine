package vn.loi.learning.application.session

data class PracticeProgress(
    val round: Int,
    val position: Int,
    val membershipSize: Int
) {
    init {
        require(round > 0)
        require(position in 1..membershipSize)
    }
}
