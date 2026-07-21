package vn.loi.learning.desktop.ui.contentlibrary

data class LessonBrowserHeaderAccessibility(
    val libraryName: String,
    val countLabel: String,
    val contentDescription: String
)

data class LessonBrowserItemAccessibility(
    val title: String,
    val contentDescription: String
)

data class LessonDetailAccessibility(
    val title: String,
    val contentDescription: String
)

data class LessonDetailPropertyAccessibility(
    val label: String,
    val value: String,
    val contentDescription: String
)

fun resolveLessonBrowserHeaderAccessibility(
    libraryName: String,
    lessonCount: Int
): LessonBrowserHeaderAccessibility {
    val normalizedName =
        libraryName.trim().ifBlank {
            "Unnamed library"
        }

    val normalizedCount =
        lessonCount.coerceAtLeast(0)

    val countLabel =
        if (normalizedCount == 1) {
            "1 content item"
        } else {
            "$normalizedCount content items"
        }

    return LessonBrowserHeaderAccessibility(
        libraryName = normalizedName,
        countLabel = countLabel,
        contentDescription =
            "$normalizedName. $countLabel."
    )
}

fun resolveLessonBrowserItemAccessibility(
    lesson: LessonBrowserItem
): LessonBrowserItemAccessibility {
    val normalizedTitle =
        lesson.title.trim().ifBlank {
            "Untitled content"
        }

    val hierarchy =
        lesson.hierarchyPath
            .trim()
            .takeIf {
                it.isNotBlank()
            }

    val normalizedType =
        lesson.type.trim().ifBlank {
            "Unknown type"
        }

    val normalizedItemCount =
        lesson.learningItemCount.coerceAtLeast(0)

    val countLabel =
        if (normalizedItemCount == 1) {
            "1 learning item"
        } else {
            "$normalizedItemCount learning items"
        }

    val parts =
        buildList {
            add(normalizedTitle)

            hierarchy?.let {
                add(it)
            }

            add(normalizedType)
            add(countLabel)
        }

    return LessonBrowserItemAccessibility(
        title = normalizedTitle,
        contentDescription =
            parts.joinToString(
                separator = ". ",
                postfix = "."
            )
    )
}

fun resolveLessonDetailAccessibility(
    lesson: LessonBrowserItem
): LessonDetailAccessibility {
    val normalizedTitle =
        lesson.title.trim().ifBlank {
            "Untitled content"
        }

    val normalizedItemCount =
        lesson.learningItemCount.coerceAtLeast(0)

    val availability =
        if (normalizedItemCount == 0) {
            "No learning items available."
        } else if (normalizedItemCount == 1) {
            "1 learning item available."
        } else {
            "$normalizedItemCount learning items available."
        }

    return LessonDetailAccessibility(
        title = normalizedTitle,
        contentDescription =
            "Learning Content. $normalizedTitle. $availability"
    )
}

fun resolveLessonDetailPropertyAccessibility(
    label: String,
    value: String
): LessonDetailPropertyAccessibility {
    val normalizedLabel =
        label.trim().ifBlank {
            "Property"
        }

    val normalizedValue =
        value.trim().ifBlank {
            "Unavailable"
        }

    return LessonDetailPropertyAccessibility(
        label = normalizedLabel,
        value = normalizedValue,
        contentDescription =
            "$normalizedLabel: $normalizedValue."
    )
}
