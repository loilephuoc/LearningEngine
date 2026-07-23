package vn.loi.learning.application.learningexperience

class RoundRobinExperienceStrategy : ExperienceSelectionStrategy {
    override fun select(
        request: ExperienceSelectionRequest
    ): ExperienceSelectionDecision {
        val selectedIndex = Math.floorMod(
            request.ordinal,
            request.options.orderedKinds.size.toLong()
        ).toInt()
        return ExperienceSelectionDecision(
            selectedIndex = selectedIndex,
            reason = ExperienceSelectionReason.ROUND_ROBIN
        )
    }
}
