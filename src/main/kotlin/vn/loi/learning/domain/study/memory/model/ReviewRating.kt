package vn.loi.learning.domain.study.memory.model

enum class ReviewRating(val grade: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    companion object {
        fun fromGrade(grade: Int): ReviewRating =
            entries.firstOrNull { it.grade == grade }
                ?: throw IllegalArgumentException(
                    "Review grade must be between 1 and 4, but was $grade."
                )
    }
}