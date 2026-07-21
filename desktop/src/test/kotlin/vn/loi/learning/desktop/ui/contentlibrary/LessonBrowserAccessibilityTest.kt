package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals

class LessonBrowserAccessibilityTest {
    @Test
    fun `browser header exposes library name and singular count`() {
        val accessibility =
            resolveLessonBrowserHeaderAccessibility(
                libraryName = "Physics",
                lessonCount = 1
            )

        assertEquals(
            "Physics. 1 content item.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `browser header clamps negative counts and normalizes blank names`() {
        val accessibility =
            resolveLessonBrowserHeaderAccessibility(
                libraryName = " ",
                lessonCount = -4
            )

        assertEquals(
            "Unnamed library",
            accessibility.libraryName
        )
        assertEquals(
            "0 content items",
            accessibility.countLabel
        )
    }

    @Test
    fun `lesson item exposes title hierarchy type and learning item count`() {
        val accessibility =
            resolveLessonBrowserItemAccessibility(
                LessonBrowserItem(
                    id = "lesson-1",
                    title = "Newton's Laws",
                    type = "Lesson",
                    group = "Physics",
                    section = "Mechanics",
                    lesson = "Newton's Laws",
                    primaryText = "Force and motion",
                    translatedText = null,
                    learningItemCount = 3
                )
            )

        assertEquals(
            "Newton's Laws. Physics → Mechanics → Newton's Laws. Lesson. 3 learning items.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `lesson item supplies stable fallbacks for blank content`() {
        val accessibility =
            resolveLessonBrowserItemAccessibility(
                LessonBrowserItem(
                    id = "lesson-2",
                    title = " ",
                    type = "",
                    group = null,
                    section = null,
                    lesson = null,
                    primaryText = "",
                    translatedText = null,
                    learningItemCount = -1
                )
            )

        assertEquals(
            "Untitled content. Unknown type. 0 learning items.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `lesson detail announces study availability`() {
        val available =
            resolveLessonDetailAccessibility(
                LessonBrowserItem(
                    id = "lesson-3",
                    title = "Vocabulary",
                    type = "Lesson",
                    group = null,
                    section = null,
                    lesson = null,
                    primaryText = "Words",
                    translatedText = null,
                    learningItemCount = 1
                )
            )

        val unavailable =
            resolveLessonDetailAccessibility(
                LessonBrowserItem(
                    id = "lesson-4",
                    title = "Empty lesson",
                    type = "Lesson",
                    group = null,
                    section = null,
                    lesson = null,
                    primaryText = "",
                    translatedText = null,
                    learningItemCount = 0
                )
            )

        assertEquals(
            "Learning Content. Vocabulary. 1 learning item available.",
            available.contentDescription
        )
        assertEquals(
            "Learning Content. Empty lesson. No learning items available.",
            unavailable.contentDescription
        )
    }

    @Test
    fun `detail property is exposed as label and value`() {
        val accessibility =
            resolveLessonDetailPropertyAccessibility(
                label = "Content Type",
                value = "Lesson"
            )

        assertEquals(
            "Content Type: Lesson.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank property text receives stable fallback wording`() {
        val accessibility =
            resolveLessonDetailPropertyAccessibility(
                label = "",
                value = " "
            )

        assertEquals(
            "Property",
            accessibility.label
        )
        assertEquals(
            "Unavailable",
            accessibility.value
        )
        assertEquals(
            "Property: Unavailable.",
            accessibility.contentDescription
        )
    }
}
