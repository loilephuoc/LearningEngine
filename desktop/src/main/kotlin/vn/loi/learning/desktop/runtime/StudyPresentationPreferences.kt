package vn.loi.learning.desktop.runtime

enum class StudyPresentationControlMode {
    ADAPTIVE,
    PREFERENCE_GUIDED,
    MANUAL
}

data class StudyPresentationPreferences(
    val controlMode: StudyPresentationControlMode = StudyPresentationControlMode.ADAPTIVE,
    val showEnglish: Boolean = true,
    val showVietnamese: Boolean = true,
    val autoplayEnglish: Boolean = true,
    val autoplayVietnamese: Boolean = false
)
