package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LessonBrowserCard(
    uiState: LessonBrowserUiState,
    onClose: () -> Unit,
    onSelectLesson: (String) -> Unit,
    onClearLessonSelection: () -> Unit,
    onStartStudy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLesson =
        uiState.selectedLesson

    Card(
        modifier =
            modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            if (selectedLesson == null) {
                LessonBrowserHeader(
                    uiState = uiState,
                    onClose = onClose
                )

                if (uiState.isEmpty) {
                    Text(
                        text =
                            "This library has no learning content.",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                } else {
                    uiState.lessons.forEach { lesson ->
                        LessonBrowserItemCard(
                            lesson = lesson,
                            onOpen = {
                                onSelectLesson(
                                    lesson.id
                                )
                            }
                        )
                    }
                }
            } else {
                LessonDetailCard(
                    lesson = selectedLesson,
                    onBack =
                        onClearLessonSelection,
                    onStartStudy = {
                        onStartStudy(
                            selectedLesson.id
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LessonBrowserHeader(
    uiState: LessonBrowserUiState,
    onClose: () -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = uiState.libraryName,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    buildString {
                        append(
                            uiState.lessonCount
                        )
                        append(" content item")

                        if (
                            uiState.lessonCount != 1
                        ) {
                            append("s")
                        }
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onClose
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun LessonBrowserItemCard(
    lesson: LessonBrowserItem,
    onOpen: () -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            if (lesson.hasHierarchy) {
                Text(
                    text =
                        lesson.hierarchyPath,
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            Text(
                text = lesson.title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )

            if (
                lesson.primaryText !=
                lesson.title
            ) {
                Text(
                    text =
                        lesson.primaryText,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }

            lesson.translatedText?.let {
                    translatedText ->

                Text(
                    text =
                        translatedText,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                text =
                    buildString {
                        append(lesson.type)
                        append(" · ")
                        append(
                            lesson.learningItemCount
                        )
                        append(" learning item")

                        if (
                            lesson.learningItemCount != 1
                        ) {
                            append("s")
                        }
                    },
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Button(
                onClick = onOpen
            ) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun LessonDetailCard(
    lesson: LessonBrowserItem,
    onBack: () -> Unit,
    onStartStudy: () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text =
                        "Learning Content",
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Text(
                    text = lesson.title,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                if (lesson.hasHierarchy) {
                    Text(
                        text =
                            lesson.hierarchyPath,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onBack
            ) {
                Text("Back")
            }
        }

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                )
        ) {
            Column(
                modifier =
                    Modifier.padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                lesson.group?.let { group ->
                    LessonDetailProperty(
                        label = "Group",
                        value = group
                    )
                }

                lesson.section?.let { section ->
                    LessonDetailProperty(
                        label = "Section",
                        value = section
                    )
                }

                lesson.lesson?.let {
                        lessonName ->

                    LessonDetailProperty(
                        label = "Lesson",
                        value = lessonName
                    )
                }

                LessonDetailProperty(
                    label = "Primary Text",
                    value =
                        lesson.primaryText
                )

                lesson.translatedText?.let {
                        translatedText ->

                    LessonDetailProperty(
                        label = "Translation",
                        value =
                            translatedText
                    )
                }

                LessonDetailProperty(
                    label = "Content Type",
                    value = lesson.type
                )

                LessonDetailProperty(
                    label = "Learning Items",
                    value =
                        lesson
                            .learningItemCount
                            .toString()
                )

                LessonDetailProperty(
                    label = "Content ID",
                    value = lesson.id
                )
            }
        }

        Button(
            onClick = onStartStudy,
            enabled =
                lesson.learningItemCount > 0
        ) {
            Text("Start Study")
        }

        if (
            lesson.learningItemCount == 0
        ) {
            Text(
                text =
                    "This content has no learning items.",
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LessonDetailProperty(
    label: String,
    value: String
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text = value,
            style =
                MaterialTheme
                    .typography
                    .bodyLarge
        )
    }
}