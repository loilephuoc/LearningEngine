package vn.loi.learning.desktop.ui.study

data class StudyWorkspaceStrings(
    val labels: Map<StudyActionControl, String>,
    val shortcutTemplate: (String, String) -> String,
    val statistics: StudyStatisticsStrings = StudyStatisticsStrings.ENGLISH,
    val previousRatingAccessibility: String = "This is the latest rating for the current review item.",
    val typingReviewedAgainMessage: String = "This answer will be reviewed again.",
    val typingContinueAgain: String = "Continue — Review Again",
    val typingRatingCurrentStatus: String = "Previous",
    val typingRatingUpcomingStatus: String = "Next",
    val typingRatingAvailableStatus: String = "Available",
    val typingProjectedRating: (String) -> String = { rating -> "Projected rating: $rating" },
    val typingTimerReady: String = "Ready",
    val typingTimerReadyAccessibility: String = "Typing timer ready. Start typing to begin.",
    val typingRatingTransitionAccessibility: (String, String) -> String = { previous, final ->
        "Previous rating $previous. Automatic rating $final."
    },
    val typingNewRatingLabel: String = "New",
    val typingTimerAccessibility: (Long, String, String, String?) -> String =
        { seconds, speed, rating, explanation ->
        "Typing time: $seconds seconds. Speed is in the $speed range. " +
            "Projected automatic rating is $rating." +
            explanation?.let { " $it" }.orEmpty()
    },
    val typingLegendAgain: String = "Reveal answer",
    val typingLegendHard: (String) -> String = { threshold -> "From $threshold" },
    val typingLegendGood: (String, String) -> String = { easy, hard ->
        "After $easy, before $hard"
    },
    val typingLegendGoodWithoutEasy: (String) -> String = { hard -> "Before $hard" },
    val typingLegendEasy: (String) -> String = { threshold -> "Up to $threshold" },
    val typingLegendEasyUnavailable: String = "Locked"
) {
    fun label(control: StudyActionControl): String = requireNotNull(labels[control])

    companion object {
        val ENGLISH = StudyWorkspaceStrings(
            labels = mapOf(
                StudyActionControl.RETRY_LOAD to "Retry Load",
                StudyActionControl.START_STUDY to "Start Study",
                StudyActionControl.START_GENERAL_STUDY to "Start General Study",
                StudyActionControl.REVEAL_ANSWER to "Reveal Answer",
                StudyActionControl.REVIEW_AGAIN to "Again",
                StudyActionControl.REVIEW_HARD to "Hard",
                StudyActionControl.REVIEW_GOOD to "Good",
                StudyActionControl.REVIEW_EASY to "Easy",
                StudyActionControl.UNDO_LATEST to "Undo latest rating",
                StudyActionControl.PAUSE_WORKSPACE to "Pause"
            ),
            shortcutTemplate = { label, shortcut -> "$label. Keyboard shortcut: $shortcut." }
        )
    }
}

data class StudyStatisticsStrings(
    val total: String,
    val new: String,
    val review: String,
    val due: String,
    val again: String,
    val hard: String,
    val good: String,
    val easy: String,
    val loading: String,
    val unavailable: String,
    val itemNoun: String,
    val sessionProgress: String,
    val remainingQueue: String,
    val dueNow: String,
    val learnedDistribution: String,
    val totalAccessibility: (Int) -> String,
    val newAccessibility: (Int, Int) -> String,
    val reviewAccessibility: (Int, Int) -> String,
    val dueAccessibility: (Int) -> String,
    val ratingAccessibility: (String, Int) -> String
) {
    companion object {
        val ENGLISH = StudyStatisticsStrings(
            "Total", "New", "Review", "Due", "Again", "Hard", "Good", "Easy",
            "Loading statistics", "Statistics unavailable", "items",
            "Session progress", "Remaining queue", "Due now", "Latest ratings",
            { count -> "$count learned items." },
            { completed, target -> "Completed $completed of the $target new-item session target." },
            { remaining, target -> "$remaining review items remain from the $target session target." },
            { count -> "$count items are due now." },
            { label, count -> "$count items have latest rating $label." }
        )
    }
}
