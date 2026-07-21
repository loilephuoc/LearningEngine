package vn.loi.learning.desktop.ui.contentlibrary

enum class LessonBrowserFilter(val label: String) { ALL("All"), WITH_TRANSLATION("With translation"), WITHOUT_TRANSLATION("Without translation");
    fun matches(item: LessonBrowserItem): Boolean = when(this){ ALL->true; WITH_TRANSLATION->!item.translatedText.isNullOrBlank(); WITHOUT_TRANSLATION->item.translatedText.isNullOrBlank() }
}
