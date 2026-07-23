package vn.loi.learning.application.learningexperience

enum class ExperienceSelectionReason {
    ROUND_ROBIN,
    USER_CHOICE
}

data class ExperienceSelectionRequest(
    val options: LearningExperienceOptions,
    val ordinal: Long
)

data class ExperienceSelectionDecision(
    val selectedIndex: Int,
    val reason: ExperienceSelectionReason
)

data class ExperienceSelectionResult(
    val selectedKind: LearningExperienceKind,
    val availableKinds: List<LearningExperienceKind>,
    val selectedIndex: Int,
    val reason: ExperienceSelectionReason
) {
    init {
        require(availableKinds.isNotEmpty()) {
            "Selection result must retain available experiences."
        }
        require(selectedIndex in availableKinds.indices) {
            "Selected experience index is outside available experiences."
        }
        require(availableKinds[selectedIndex] == selectedKind) {
            "Selected experience must match its available index."
        }
    }
}

fun interface ExperienceSelectionStrategy {
    fun select(request: ExperienceSelectionRequest): ExperienceSelectionDecision
}

class ExperienceSelectionEngine(
    private val strategy: ExperienceSelectionStrategy
) {
    fun select(request: ExperienceSelectionRequest): ExperienceSelectionResult {
        val decision = strategy.select(request)
        val available = request.options.orderedKinds
        require(decision.selectedIndex in available.indices) {
            "Selection strategy returned an unavailable experience index."
        }
        return ExperienceSelectionResult(
            selectedKind = available[decision.selectedIndex],
            availableKinds = available.toList(),
            selectedIndex = decision.selectedIndex,
            reason = decision.reason
        )
    }
}
