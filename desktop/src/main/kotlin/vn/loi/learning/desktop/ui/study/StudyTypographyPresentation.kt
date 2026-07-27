package vn.loi.learning.desktop.ui.study

import kotlin.math.roundToInt
import vn.loi.learning.desktop.runtime.StudyTypographyPreferences

data class StudyTypographyPresentation(
    val exampleEnglishFontSize: Int,
    val exampleEnglishLineHeight: Int,
    val exampleVietnameseFontSize: Int,
    val exampleVietnameseLineHeight: Int,
    val softWrap: Boolean
)

object StudyTypographyPresentationResolver {
    fun resolve(
        preferences: StudyTypographyPreferences,
        viewportWidthDp: Int
    ): StudyTypographyPresentation {
        require(viewportWidthDp >= 0) { "viewportWidthDp must not be negative." }
        return StudyTypographyPresentation(
            exampleEnglishFontSize = preferences.exampleEnglishFontSize,
            exampleEnglishLineHeight = (preferences.exampleEnglishFontSize * 1.30).roundToInt(),
            exampleVietnameseFontSize = preferences.exampleVietnameseFontSize,
            exampleVietnameseLineHeight = (preferences.exampleVietnameseFontSize * 1.35).roundToInt(),
            softWrap = true
        )
    }
}

data class StudyTypographyPreview(
    val englishText: String,
    val vietnameseText: String,
    val typography: StudyTypographyPresentation
)

fun resolveStudyTypographyPreview(
    preferences: StudyTypographyPreferences,
    viewportWidthDp: Int
): StudyTypographyPreview =
    StudyTypographyPreview(
        englishText = "There are many homeless people.",
        vietnameseText = "Có rất nhiều người vô gia cư.",
        typography = StudyTypographyPresentationResolver.resolve(preferences, viewportWidthDp)
    )
