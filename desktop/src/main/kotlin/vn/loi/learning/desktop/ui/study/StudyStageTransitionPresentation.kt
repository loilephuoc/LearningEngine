package vn.loi.learning.desktop.ui.study

fun formatStudyStageTransition(
    beforeStage: String,
    afterStage: String
): String =
    "${formatStudyStageName(beforeStage)} → " +
        formatStudyStageName(afterStage)

private fun formatStudyStageName(
    stage: String
): String {
    val normalized =
        stage
            .trim()
            .lowercase()
            .split('_')
            .filter(String::isNotBlank)
            .joinToString(separator = " ")

    if (normalized.isBlank()) {
        return "Unknown"
    }

    return normalized.replaceFirstChar { character ->
        character.uppercase()
    }
}
