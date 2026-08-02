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
    val ratingConfirmationAccessibility: String = "Confirmed",
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
    val typingLegendEasyUnavailable: String = "Locked",
    val continuousReviewLabel: String = "Continue review automatically after completion",
    val continuousReviewAccessibility: String =
        "Continuous Review. Continue eligible general study sessions after restart.",
    val learningInsight: LearningInsightStrings = LearningInsightStrings.ENGLISH,
    val learningEntry: LearningEntryStrings = LearningEntryStrings.ENGLISH
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

data class LearningEntryStrings(
    val heading: String,
    val description: String,
    val currentContext: String,
    val generalScope: String,
    val lessonScope: String,
    val noContextTitle: String,
    val noContextDescription: String,
    val readiness: String,
    val resumable: String,
    val newAvailable: String,
    val reviewAvailable: String,
    val learnedAvailable: String,
    val resume: String,
    val resumeDescription: String,
    val continueStudy: String,
    val continueDescription: String,
    val startNewConfigured: String,
    val startNewConfiguredDescription: (Int, Int) -> String,
    val startNewConfiguredFallbackDescription: String,
    val reviewLatestNew: String,
    val reviewLatestNewAvailableDescription: (Int) -> String,
    val reviewLatestNewUnavailableDescription: String,
    val reviewAgainHard: String,
    val reviewAgainHardAvailableDescription: (Int) -> String,
    val reviewAgainHardUnavailableDescription: String,
    val reviewAll: String,
    val reviewAllAvailableDescription: (Int) -> String,
    val reviewAllUnavailableDescription: String,
    val backToLibrary: String,
    val backToLibraryDescription: String,
    val primaryActions: String,
    val alternativeActions: String,
    val managementActions: String
) {
    companion object {
        val ENGLISH = LearningEntryStrings(
            heading = "What would you like to learn?",
            description = "Confirm your current learning context, then choose how to begin.",
            currentContext = "Current learning context",
            generalScope = "Current package or topic",
            lessonScope = "Selected lesson",
            noContextTitle = "No active learning context",
            noContextDescription = "Choose an active package or lesson in Library before starting.",
            readiness = "Ready to learn",
            resumable = "Active session",
            newAvailable = "New",
            reviewAvailable = "Review",
            learnedAvailable = "Learned",
            resume = "Resume active session",
            resumeDescription = "Continue from the same item and queue in your active session.",
            continueStudy = "Continue learning",
            continueDescription = "Start ordinary Study with the current session configuration.",
            startNewConfigured = "Start new session with current settings",
            startNewConfiguredDescription = { newLimit, reviewLimit -> "Create a new session with up to $newLimit New and $reviewLimit Review items." },
            startNewConfiguredFallbackDescription = "Create a new session with the current New and Review limits.",
            reviewLatestNew = "Review New items from the latest session",
            reviewLatestNewAvailableDescription = { count -> "Review $count New items learned in the latest completed session." },
            reviewLatestNewUnavailableDescription = "The latest completed session has no New items to review.",
            reviewAgainHard = "Review Again / Hard items",
            reviewAgainHardAvailableDescription = { count -> "Review $count items whose latest rating is Again or Hard." },
            reviewAgainHardUnavailableDescription = "No items have a latest rating of Again or Hard.",
            reviewAll = "Review all learned",
            reviewAllAvailableDescription = { count -> "Review all $count learned items in the current scope." },
            reviewAllUnavailableDescription = "No learned items are available to review.",
            backToLibrary = "Back to Library",
            backToLibraryDescription = "Change package or lesson, import content, or manage Library.",
            primaryActions = "Primary study action",
            alternativeActions = "Alternative study modes",
            managementActions = "Content management"
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
