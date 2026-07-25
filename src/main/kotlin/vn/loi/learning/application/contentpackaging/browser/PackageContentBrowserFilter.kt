package vn.loi.learning.application.contentpackaging.browser

enum class BrowserMediaFilter(val label: String) {
    ALL("All Media"),
    HAS_IMAGE("Has Image"),
    MISSING_IMAGE("Missing Image"),
    HAS_AUDIO("Has Audio"),
    MISSING_AUDIO("Missing Audio")
}

enum class BrowserSortOption(val label: String) {
    ORIGINAL_ORDER("Original Order"),
    QUESTION_ASC("Question A-Z"),
    LESSON_ASC("Lesson"),
    MEDIA_COMPLETENESS("Media Completeness")
}
