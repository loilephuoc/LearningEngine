package vn.loi.learning.application.study

/**
 * Nhóm độ khó dùng trong quá trình cân bằng StudyQueue.
 */
enum class DifficultyBand {

    EASY,

    MEDIUM,

    HARD;

    companion object {

        private const val MEDIUM_THRESHOLD =
            4.0

        private const val HARD_THRESHOLD =
            7.0

        fun from(
            difficulty: Double
        ): DifficultyBand =
            when {
                difficulty <
                        MEDIUM_THRESHOLD ->
                    EASY

                difficulty <
                        HARD_THRESHOLD ->
                    MEDIUM

                else ->
                    HARD
            }
    }
}